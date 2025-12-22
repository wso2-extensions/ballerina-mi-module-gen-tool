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

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.impl.symbols.BallerinaClassSymbol;
import io.ballerina.compiler.api.impl.symbols.BallerinaUnionTypeSymbol;
import io.ballerina.compiler.api.symbols.*;
import io.ballerina.compiler.api.symbols.resourcepath.PathSegmentList;
import io.ballerina.compiler.api.symbols.resourcepath.util.PathSegment;
import io.ballerina.mi.connectorModel.*;
import io.ballerina.mi.util.Constants;
import io.ballerina.mi.util.Utils;
import io.ballerina.projects.Module;
import io.ballerina.projects.Package;
import io.ballerina.projects.PackageDescriptor;
import io.ballerina.projects.PackageReadmeMd;
import org.ballerinalang.diagramutil.connector.models.connector.types.PathParamType;

import java.io.PrintStream;
import java.util.*;

public class BalConnectorAnalyzer implements Analyzer {
    private static final String METHOD_NAME = "MethodName";
    private static final String PATH_PARAM_SIZE = "PathParamSize";
    private static final String QUERY_PARAM_SIZE = "QueryParamSize";

    private final PrintStream printStream = System.out;

    @Override
    public void analyze(Package compilePackage) {

        PackageDescriptor descriptor = compilePackage.descriptor();
        Connector.getConnector(descriptor);
        for (Module module : compilePackage.modules()) {
            analyzeModule(compilePackage, module);
        }
    }

    private void analyzeModule(Package compilePackage, Module module) {
        SemanticModel semanticModel = compilePackage.getCompilation().getSemanticModel(module.moduleId());
        List<Symbol> moduleSymbols = semanticModel.moduleSymbols();
        List<Symbol> classSymbols = moduleSymbols.stream().filter((s) -> s instanceof BallerinaClassSymbol).toList();
        for (Symbol classSymbol : classSymbols) {
            analyzeClass(compilePackage, module, (ClassSymbol) classSymbol);
        }
    }

    private void analyzeClass(Package compilePackage, Module module, ClassSymbol classSymbol) {
        SemanticModel semanticModel = compilePackage.getCompilation().getSemanticModel(module.moduleId());

        if (!isClientClass(classSymbol) || classSymbol.getName().isEmpty()) {
            return;
        }
        String clientClassName = classSymbol.getName().get();
        ModuleSymbol moduleSymbol = classSymbol.getModule().orElseThrow(() -> new IllegalStateException("Client class is outside the module"));
        String moduleName = moduleSymbol.getName().orElseThrow(() -> new IllegalStateException("Module name not defined"));
        String connectionType = String.format("%s_%s", moduleName, clientClassName);

        Connector connector = Connector.getConnector();
        Connection connection = new Connection(connector, connectionType, clientClassName, Integer.toString(connector.getConnections().size()));

        // Get the connector description
        Optional<PackageReadmeMd> connectorReadMe = compilePackage.readmeMd();
        if (connectorReadMe.isPresent() && !connectorReadMe.get().content().isEmpty()) {
            connector.setDescription(connectorReadMe.get().content());
        } else {
            connector.setDescription(String.format("Ballerina %s connector", connector.getModuleName()));
        }

        // Get the connection description
        Optional<Documentation> optionalMetadataNode = classSymbol.documentation();
        if (optionalMetadataNode.isEmpty()) {
            connection.setDescription(String.format("%s connection for Ballerina %s connector", connectionType,
                    connector.getModuleName()));
        } else {
            connection.setDescription(Utils.getDocString(optionalMetadataNode.get()));
        }

        /**
         * TODO: DO: Get the icon - from central or context or from the docs dir inside the bala - small and large icon
         * syntaxNodeAnalysisContext.currentPackage().project().sourceRoot()
         */

        int i = 0;
        Map<String, MethodSymbol> allMethods = new HashMap<>(classSymbol.methods());
        classSymbol.initMethod().ifPresent(methodSymbol -> allMethods.put(Constants.INIT_FUNCTION_NAME, methodSymbol));

        // Track generated synapse names to handle duplicates
        Map<String, Integer> synapseNameCount = new HashMap<>();

        methodLoop:
        for (Map.Entry<String, MethodSymbol> methodEntry : allMethods.entrySet()) {
            MethodSymbol methodSymbol = methodEntry.getValue();
            List<Qualifier> qualifierList = methodSymbol.qualifiers();
            if (!(Utils.containsToken(qualifierList, Qualifier.PUBLIC) ||
                    Utils.containsToken(qualifierList, Qualifier.REMOTE) ||
                    Utils.containsToken(qualifierList, Qualifier.RESOURCE))) {
                continue;
            }

            FunctionType functionType = Utils.getFunctionType(methodSymbol);

            // Prepare context for synapse name generation
            SynapseNameContext.Builder contextBuilder = SynapseNameContext.builder().module(module);
            
            // Extract operationId from @openapi:ResourceInfo annotation if present using syntax tree API
            Optional<String> operationIdOpt = Optional.empty();
            try {
                operationIdOpt = Utils.getOpenApiOperationId(methodSymbol, module, semanticModel);
                if (operationIdOpt.isPresent()) {
                    System.out.println("Found operationId: " + operationIdOpt.get() + " for method: " + methodSymbol.getName().orElse("<unknown>"));
                }
            } catch (Exception e) {
                // If syntax tree access fails, continue without operationId
                System.out.println("Error extracting operationId for method: " + methodSymbol.getName().orElse("<unknown>") + " - " + e.getMessage());
            }
            
            // Add operationId to context if found
            operationIdOpt.ifPresent(contextBuilder::operationId);
            
            // Extract resource path segments if this is a resource function
            if (functionType == FunctionType.RESOURCE && methodSymbol instanceof ResourceMethodSymbol resourceMethod) {
                try {
                    PathSegmentList resourcePath = (PathSegmentList) resourceMethod.resourcePath();
                    contextBuilder.resourcePathSegments(resourcePath.list());
                } catch (Exception e) {
                    // If path extraction fails, continue without path segments
                }
            }
            
            SynapseNameContext context = contextBuilder.build();
            
            // Use priority-based synapse name generation
            SynapseNameGeneratorManager nameGenerator = new SynapseNameGeneratorManager();
            Optional<String> synapseNameOpt = nameGenerator.generateSynapseName(methodSymbol, functionType, context);
            
            // Fallback to default if no generator succeeded
            String synapseName = synapseNameOpt.orElseGet(() -> Utils.generateSynapseName(methodSymbol, functionType));
            
            // Handle duplicate names by appending numeric suffix
            String finalSynapseName = synapseName;
            if (synapseNameCount.containsKey(synapseName)) {
                int count = synapseNameCount.get(synapseName) + 1;
                synapseNameCount.put(synapseName, count);
                finalSynapseName = synapseName + count;
            } else {
                synapseNameCount.put(synapseName, 0);
            }

            String returnType = Utils.getReturnTypeName(methodSymbol);
            String docString = methodSymbol.documentation().map(Utils::getDocString).orElse("");
            
            // Extract path parameters from resource path segments (for resource functions)
            List<PathParamType> pathParams = new ArrayList<>();
            Set<String> pathParamNames = new HashSet<>();
            
            if (functionType == FunctionType.RESOURCE && methodSymbol instanceof ResourceMethodSymbol resourceMethod) {
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
            Optional<List<ParameterSymbol>> params = methodSymbol.typeDescriptor().params();
            Map<String, ParameterSymbol> paramMap = new HashMap<>();
            if (params.isPresent()) {
                for (ParameterSymbol paramSymbol : params.get()) {
                    paramSymbol.getName().ifPresent(name -> paramMap.put(name, paramSymbol));
                }
            }

            /*
             * Create PathParamType objects for identified path parameters.
             * In some generated connectors, the path parameter name used in the resource path segment
             * does not have a matching function parameter (for example, when the path param is only
             * used in the path and not re-declared as a separate argument).
             *
             * Previously, such path parameters were silently dropped which resulted in:
             *   - No <property name="pathParam*".../> entries in the Synapse template
             *   - Missing input fields for those path params in the JSON UI schema
             *
             * To avoid losing path parameters, we now:
             *   - Use the actual parameter type when a matching function parameter exists
             *   - Fall back to treating the path parameter as a string when there is no match
             */
            for (String pathParamName : pathParamNames) {
                ParameterSymbol paramSymbol = paramMap.get(pathParamName);
                String paramTypeName;
                if (paramSymbol != null) {
                    paramTypeName = Utils.getParamTypeName(Utils.getActualTypeKind(paramSymbol.typeDescriptor()));
                    // If we cannot resolve a concrete MI type, also fall back to string
                    if (paramTypeName == null) {
                        paramTypeName = Constants.STRING;
                    }
                } else {
                    // No matching parameter symbol - assume string path parameter
                    paramTypeName = Constants.STRING;
                }

                PathParamType pathParam = new PathParamType();
                pathParam.name = pathParamName;
                pathParam.typeName = paramTypeName;
                pathParams.add(pathParam);
            }
            
            Component component = new Component(finalSynapseName, docString, functionType, Integer.toString(i), pathParams, List.of(), returnType);
            
            // Store operationId as a parameter if found
            if (operationIdOpt.isPresent()) {
                Param operationIdParam = new Param("operationId", operationIdOpt.get());
                component.setParam(operationIdParam);
            }

            // Extract regular function parameters (non-path parameters)
            int functionParamIndex = 0;
            if (params.isPresent()) {
                List<ParameterSymbol> parameterSymbols = params.get();
                for (ParameterSymbol parameterSymbol : parameterSymbols) {
                    // Skip path parameters as they are handled separately
                    Optional<String> paramNameOpt = parameterSymbol.getName();
                    if (paramNameOpt.isPresent() && !pathParamNames.contains(paramNameOpt.get())) {
                        Optional<FunctionParam> functionParam = ParamFactory.createFunctionParam(parameterSymbol, functionParamIndex);
                        if (functionParam.isPresent()) {
                            component.setFunctionParam(functionParam.get());
                            functionParamIndex++;
                        } else {
                            // Skip the function if any parameter type is unsupported
                            String paramType = parameterSymbol.typeDescriptor().typeKind().getName();
                            continue methodLoop;
                        }
                    }
                }
                Param sizeParam = new Param("paramSize", Integer.toString(functionParamIndex));
                Param functionNameParam = new Param("paramFunctionName", component.getName());
                component.setParam(sizeParam);
                component.setParam(functionNameParam);
            }
            if (functionType == FunctionType.INIT) {
                component.setObjectTypeName(classSymbol.getName().get());
                connection.setInitComponent(component);
            } else {
                connection.setComponent(component);
            }
            i++;
        }
        connector.setConnection(connection);
    }

    private boolean isClientClass(ClassSymbol classSymbol) {

        return classSymbol.qualifiers().contains(Qualifier.PUBLIC) && classSymbol.qualifiers().contains(Qualifier.CLIENT);
    }
}
