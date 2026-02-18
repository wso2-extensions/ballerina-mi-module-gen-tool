/*
 * Copyright (c) 2025, WSO2 LLC. (https://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
 
package io.ballerina.mi.analyzer;

import io.ballerina.compiler.api.symbols.*;
import io.ballerina.compiler.api.symbols.resourcepath.PathSegmentList;
import io.ballerina.compiler.api.symbols.resourcepath.util.PathSegment;
import io.ballerina.mi.model.Component;
import io.ballerina.mi.model.Connection;
import io.ballerina.mi.model.Connector;
import io.ballerina.mi.model.FunctionType;
import io.ballerina.mi.model.param.Param;
import io.ballerina.mi.util.Constants;
import io.ballerina.mi.util.Utils;
import io.ballerina.projects.Package;
import org.ballerinalang.diagramutil.connector.models.connector.types.PathParamType;

import java.io.PrintStream;
import java.util.*;

public class BalModuleAnalyzer implements Analyzer {

    private final PrintStream printStream;

    public BalModuleAnalyzer() {
        this.printStream = System.out;
    }

    @Override
    public void analyze(Package compilePackage) {

        Connector connector = Connector.getConnector(compilePackage.descriptor());
        connector.setBalModule(true);
        // Get all symbols from the module and filter for functions
        Collection<Symbol> allSymbols = compilePackage.getCompilation().getSemanticModel(compilePackage.getDefaultModule().moduleId()).moduleSymbols();
        for (Symbol symbol : allSymbols) {
            if (symbol.kind() == SymbolKind.FUNCTION && symbol instanceof FunctionSymbol functionSymbol) {
                analyzeFunctionForMIOperation(functionSymbol, connector);
            }
        }
    }

    private void analyzeFunctionForMIOperation(FunctionSymbol functionSymbol, Connector connector) {
        // Check if function has @mi:Operation annotation
        List<AnnotationSymbol> annotations = functionSymbol.annotations();

        boolean hasOperationAnnotation = false;
        for (AnnotationSymbol annotationSymbol : annotations) {
            Optional<String> annotationName = annotationSymbol.getName();
            if (annotationName.isPresent() && annotationName.get().equals("Operation")) {
                hasOperationAnnotation = true;
                break;
            }
        }

        if (!hasOperationAnnotation) {
            return;
        }

        Optional<String> functionName = functionSymbol.getName();
        if (functionName.isEmpty()) {
            return;
        }

        printStream.println("Found MI operation: " + functionName.get());

        Connection connection = new Connection(connector, null, null, null);
        // Create component
        Optional<Documentation> documentation = functionSymbol.documentation();
        String documentationString = documentation.map(Utils::getDocString).orElse("");
        FunctionType functionType = Utils.getFunctionType(functionSymbol);
        String returnTypeName = Utils.getReturnTypeName(functionSymbol);

        // Generate synapse name based on function type (handles resource functions)
        String synapseName = Utils.generateSynapseName(functionSymbol, functionType);
        
        // Replace dots with underscores in synapse name if connector module name has dots
        if (connector.getModuleName().contains(".")) {
            synapseName = synapseName.replace(".", "_");
        }

        // Extract path parameters from resource path segments (for resource functions)
        List<PathParamType> pathParams = new ArrayList<>();
        Set<String> pathParamNames = new HashSet<>();
        
        if (functionType == FunctionType.RESOURCE && functionSymbol instanceof ResourceMethodSymbol resourceMethod) {
            try {
                PathSegmentList resourcePath = (PathSegmentList) resourceMethod.resourcePath();
                List<PathSegment> segments = resourcePath.list();
                
                for (PathSegment segment : segments) {
                    String sig = segment.signature();
                    if (sig != null && sig.startsWith("[") && sig.endsWith("]")) {
                        // This is a path parameter segment
                        String inside = sig.substring(1, sig.length() - 1).trim();
                        String paramName = inside;
                        int lastSpace = inside.lastIndexOf(' ');
                        if (lastSpace >= 0 && lastSpace + 1 < inside.length()) {
                            paramName = inside.substring(lastSpace + 1);
                        }
                        if (!paramName.isEmpty()) {
                            pathParamNames.add(paramName);
                        }
                    }
                }
            } catch (Exception e) {
                // If path extraction fails, continue without path parameters
            }
        }
        
        // Now match path parameter names with actual function parameters to get types
        Optional<List<ParameterSymbol>> params = functionSymbol.typeDescriptor().params();
        Map<String, ParameterSymbol> paramMap = new HashMap<>();
        if (params.isPresent()) {
            for (ParameterSymbol paramSymbol : params.get()) {
                paramSymbol.getName().ifPresent(name -> paramMap.put(name, paramSymbol));
            }
        }

        /*
         * Create PathParamType objects for identified path parameters.
         * Similar to the connector analyzer, there can be cases where the name used in
         * the resource path segment does not have a directly matching function parameter.
         * Previously we dropped such path parameters, which meant they did not appear in
         * the generated Synapse template or JSON schema.
         *
         * To fix this, we:
         *   - Use the concrete parameter type when a matching function parameter exists
         *   - Fall back to treating the path parameter as a string when there is no match
         */
        for (String pathParamName : pathParamNames) {
            ParameterSymbol paramSymbol = paramMap.get(pathParamName);
            String paramTypeName;
            if (paramSymbol != null) {
                paramTypeName = Utils.getParamTypeName(Utils.getActualTypeKind(paramSymbol.typeDescriptor()));
                if (paramTypeName == null) {
                    paramTypeName = Constants.STRING;
                }
            } else {
                paramTypeName = Constants.STRING;
            }

            PathParamType pathParam = new PathParamType();
            pathParam.name = pathParamName;
            pathParam.typeName = paramTypeName;
            pathParams.add(pathParam);
        }

        // Collect all parameter names (path params, function params) to check for conflicts
        Set<String> allParamNames = new HashSet<>(pathParamNames);
        int noOfParams = 0;
        int functionParamIndex = 0;
        if (params.isPresent()) {
            List<ParameterSymbol> parameterSymbols = params.get();
            noOfParams = parameterSymbols.size();

            for (ParameterSymbol paramSymbol : parameterSymbols) {
                Optional<String> paramNameOpt = paramSymbol.getName();
                if (paramNameOpt.isPresent()) {
                    allParamNames.add(paramNameOpt.get());
                }
            }
        }

        // Check if synapse name conflicts with any parameter name and make it unique if needed
        String finalSynapseName = synapseName;
        if (allParamNames.contains(synapseName) || allParamNames.contains(functionName.get())) {
            // Add a suffix to make the synapse name unique and avoid conflicts
            finalSynapseName = synapseName + "_operation";
        }

        Component component = new Component(finalSynapseName, documentationString, functionType, "0", pathParams, Collections.emptyList(), returnTypeName);

        // Now add all function parameters (we keep them all, synapse name is made unique instead)
        if (params.isPresent()) {
            List<ParameterSymbol> parameterSymbols = params.get();
            for (ParameterSymbol paramSymbol : parameterSymbols) {
                // Skip path parameters as they are handled separately
                Optional<String> paramNameOpt = paramSymbol.getName();
                if (paramNameOpt.isPresent() && !pathParamNames.contains(paramNameOpt.get())) {
                    ParamFactory.createFunctionParam(paramSymbol, functionParamIndex).ifPresent(component::setFunctionParam);
                    functionParamIndex++;
                }
            }
        }

        Param sizeParam = new Param("paramSize", Integer.toString(noOfParams));
        Param functionNameParam = new Param("paramFunctionName", component.getName());
        component.setParam(sizeParam);
        component.setParam(functionNameParam);

        connection.setComponent(component);
        connector.setConnection(connection);
    }
}
