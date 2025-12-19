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

            // Generate synapse name.
            // For resource functions:
            //   httpMethod + PascalCase(literal path segments) + By<ParamName> for each path parameter.
            // For all others, fall back to type-based heuristics in Utils.generateSynapseName(..).
            String synapseName;
            if (functionType == FunctionType.RESOURCE && methodSymbol instanceof ResourceMethodSymbol resourceMethod) {
                try {
                    String httpMethod = methodSymbol.getName().orElse("resource").toLowerCase();
                    PathSegmentList resourcePath = (PathSegmentList) resourceMethod.resourcePath();
                    java.util.List<PathSegment> pathSegments = resourcePath.list();

                    java.util.List<String> literalSegments = new java.util.ArrayList<>();
                    java.util.List<String> pathParamNames = new java.util.ArrayList<>();

                    for (PathSegment segment : pathSegments) {
                        String sig = segment.signature();
                        if (sig == null || sig.isEmpty()) {
                            continue;
                        }

                        if (sig.startsWith("[") && sig.endsWith("]")) {
                            // Path parameter segment. Extract the parameter name:
                            // e.g. "[string userId]" -> "userId", "[userId]" -> "userId"
                            String inside = sig.substring(1, sig.length() - 1).trim();
                            String paramName = inside;
                            int lastSpace = inside.lastIndexOf(' ');
                            if (lastSpace >= 0 && lastSpace + 1 < inside.length()) {
                                paramName = inside.substring(lastSpace + 1);
                            }
                            if (!paramName.isEmpty()) {
                                pathParamNames.add(paramName);
                            }
                        } else {
                            // Literal path segment, e.g. "users", "threads", "trash"
                            literalSegments.add(sig);
                        }
                    }

                    StringBuilder nameBuilder = new StringBuilder();
                    nameBuilder.append(httpMethod);

                    // Add PascalCase literal segments
                    for (String lit : literalSegments) {
                        nameBuilder.append(toPascalCaseSegment(lit));
                    }

                    // Add By<ParamName> for each path parameter
                    for (String paramName : pathParamNames) {
                        if (paramName.isEmpty()) {
                            continue;
                        }
                        nameBuilder.append("By");
                        nameBuilder.append(Character.toUpperCase(paramName.charAt(0)));
                        if (paramName.length() > 1) {
                            nameBuilder.append(paramName.substring(1));
                        }
                    }

                    synapseName = nameBuilder.toString();
                } catch (Throwable e) {
                    // If anything goes wrong while reading the path, ignore and fall back to type-based heuristics
                    printStream.println("WARN: Unable to derive name from resource path for method "
                            + methodSymbol.getName().orElse("<unknown>") + ": " + e.getMessage());
                    synapseName = Utils.generateSynapseName(methodSymbol, functionType);
                }
            } else {
                synapseName = Utils.generateSynapseName(methodSymbol, functionType);
            }
            
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
            
            // Try to extract operationId from @openapi:ResourceInfo annotation if present
            Optional<String> operationIdOpt = Optional.empty();
            try {
                // Get source content from module to extract operationId
                // Try to access documents through module's document API
                Collection<io.ballerina.projects.Document> documents = module.documentIds().stream()
                        .map(module::document)
                        .toList();
                for (io.ballerina.projects.Document doc : documents) {
                    String sourceContent = doc.textDocument().toString();
                    operationIdOpt = Utils.getOpenApiOperationId(methodSymbol, sourceContent);
                    if (operationIdOpt.isPresent()) {
                        System.out.println("Found operationId: " + operationIdOpt.get() + " for method: " + methodSymbol.getName().orElse("<unknown>"));
                        break; // Found operationId, no need to check other documents
                    }
                }
            } catch (Exception e) {
                // If source content access fails, continue without operationId
                System.out.println("Error extracting operationId for method: " + methodSymbol.getName().orElse("<unknown>") + " - " + e.getMessage());
            }
            
            Component component = new Component(finalSynapseName, Utils.getDocString(methodSymbol.documentation().get()), functionType, Integer.toString(i), List.of(), List.of(), returnType);
            
            // Store operationId as a parameter if found
            if (operationIdOpt.isPresent()) {
                Param operationIdParam = new Param("operationId", operationIdOpt.get());
                component.setParam(operationIdParam);
            }
//            String componentIndex = Integer.toString(connection.getComponents().size());
//            Component component = new Component(functionName,
//                    GeneratorUtils.getDocFromMetadata(functionDefinition.metadata()),
//                    functionType, componentIndex, pathParams, queryParams, returnParam !=null ? returnParam.typeName : null);
//            int noOfQueryParams = queryParams.size();
//            int noOfPathParams = pathParams.size();
//            Param querySizeParam = new Param(QUERY_PARAM_SIZE, Integer.toString(noOfQueryParams));
//            Param pathSizeParam = new Param(PATH_PARAM_SIZE, Integer.toString(noOfPathParams));
//            Param functionNameParam = new Param(METHOD_NAME, component.getName());
//            component.setParam(querySizeParam);
//            component.setParam(pathSizeParam);
//            component.setParam(functionNameParam);

            Optional<List<ParameterSymbol>> params = methodSymbol.typeDescriptor().params();
            if (params.isPresent()) {
                List<ParameterSymbol> parameterSymbols = params.get();
                int paramIndex = 0;
                for (ParameterSymbol parameterSymbol : parameterSymbols) {
                    Optional<FunctionParam> functionParam = ParamFactory.createFunctionParam(parameterSymbol, paramIndex);
                    if (functionParam.isPresent()) {
                        component.setFunctionParam(functionParam.get());
                    } else {
                        // Skip the function if any parameter type is unsupported
//                        printStream.println("Skipping function '" + functionName + "' due to unsupported parameter type.");
                        continue methodLoop;
                    }
                    paramIndex++;
                }
                Param sizeParam = new Param("paramSize", Integer.toString(paramIndex));
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

    /**
     * Convert a path segment (e.g. "users", "user-threads") to PascalCase ("Users", "UserThreads").
     */
    private static String toPascalCaseSegment(String segment) {
        if (segment == null || segment.isEmpty()) {
            return "";
        }
        String[] parts = segment.split("[-_\\s]+");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            sb.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                sb.append(part.substring(1).toLowerCase());
            }
        }
        return sb.toString();
    }
}
