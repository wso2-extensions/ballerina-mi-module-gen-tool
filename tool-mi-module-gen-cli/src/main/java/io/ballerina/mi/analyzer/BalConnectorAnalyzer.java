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
import io.ballerina.compiler.api.symbols.*;
import io.ballerina.compiler.api.symbols.resourcepath.PathSegmentList;
import io.ballerina.compiler.api.symbols.resourcepath.util.PathSegment;
import io.ballerina.compiler.syntax.tree.*;
import io.ballerina.mi.model.Component;
import io.ballerina.mi.model.Connection;
import io.ballerina.mi.model.Connector;
import io.ballerina.mi.model.FunctionType;
import io.ballerina.mi.model.param.FunctionParam;
import io.ballerina.mi.model.param.Param;
import io.ballerina.mi.model.param.RecordFunctionParam;
import io.ballerina.mi.model.param.ResourcePathSegment;
import io.ballerina.mi.util.Constants;
import io.ballerina.mi.util.Utils;
import io.ballerina.projects.*;
import io.ballerina.projects.Module;
import io.ballerina.projects.Package;
import org.ballerinalang.diagramutil.connector.models.connector.types.PathParamType;

import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class BalConnectorAnalyzer implements Analyzer {
    private static final String METHOD_NAME = "MethodName";
    private static final String PATH_PARAM_SIZE = "PathParamSize";
    private static final String QUERY_PARAM_SIZE = "QueryParamSize";

    private final PrintStream printStream = System.out;

    @Override
    public void analyze(Package compilePackage) {

        PackageDescriptor descriptor = compilePackage.descriptor();
        Connector connector = Connector.getConnector(descriptor);

        // Extract icon from the bala package docs folder
        extractConnectorIcon(compilePackage, connector);

        for (Module module : compilePackage.modules()) {
            analyzeModule(compilePackage, module);
        }
    }

    /**
     * Extracts the connector icon from the bala package docs folder.
     * The icon is typically located at {sourceRoot}/docs/icon.png
     *
     * @param compilePackage The compiled package
     * @param connector The connector to set the icon path on
     */
    private void extractConnectorIcon(Package compilePackage, Connector connector) {
        try {
            Path sourceRoot = compilePackage.project().sourceRoot();
            Path docsIconPath = sourceRoot.resolve("docs").resolve("icon.png");

            if (Files.exists(docsIconPath)) {
                connector.setIconPath(docsIconPath.toString());
                printStream.println("Found connector icon at: " + docsIconPath);
            } else {
                printStream.println("No connector icon found in docs folder. Using default icon.");
            }
        } catch (Exception e) {
            printStream.println("Warning: Could not extract connector icon: " + e.getMessage());
        }
    }

    private void analyzeModule(Package compilePackage, Module module) {
        // Skip sub-modules - only process root-level module clients
        // Sub-modules have non-empty moduleNamePart (e.g., "OAS" in googleapis.gmail.OAS)
        // Root modules have null moduleNamePart
        if (module.moduleName().moduleNamePart() != null && !module.moduleName().moduleNamePart().isEmpty()) {
            printStream.println("Skipping sub-module: " + module.moduleName());
            return;
        }

        SemanticModel semanticModel = compilePackage.getCompilation().getSemanticModel(module.moduleId());
        List<Symbol> moduleSymbols = semanticModel.moduleSymbols();
        List<Symbol> classSymbols = moduleSymbols.stream().filter((s) -> s instanceof BallerinaClassSymbol).toList();

        // Extract default values from syntax trees
        Map<String, Map<String, Map<String, String>>> classMethodDefaultValues = extractDefaultValues(module);

        for (Symbol classSymbol : classSymbols) {
            String className = classSymbol.getName().orElse("");
            Map<String, Map<String, String>> methodDefaultValues = classMethodDefaultValues.getOrDefault(className, Map.of());
            analyzeClass(compilePackage, module, (ClassSymbol) classSymbol, methodDefaultValues);
        }
    }

    /**
     * Extracts default values for all method parameters from the module's syntax trees.
     * Returns a map of className -> (functionName -> (paramName -> defaultValue))
     */
    private Map<String, Map<String, Map<String, String>>> extractDefaultValues(Module module) {
        Map<String, Map<String, Map<String, String>>> result = new HashMap<>();

        for (DocumentId docId : module.documentIds()) {
            Document document = module.document(docId);
            SyntaxTree syntaxTree = document.syntaxTree();
            ModulePartNode modulePartNode = syntaxTree.rootNode();

            for (ModuleMemberDeclarationNode member : modulePartNode.members()) {
                if (member instanceof ClassDefinitionNode classNode) {
                    String className = classNode.className().text();
                    Map<String, Map<String, String>> functionDefaults = new HashMap<>();

                    for (Node classMember : classNode.members()) {
                        if (classMember instanceof FunctionDefinitionNode funcNode) {
                            String functionName = funcNode.functionName().text();
                            // Normalize init method name to match Constants.INIT_FUNCTION_NAME
                            if (functionName.equals(Constants.INIT_FUNCTION_NAME)) {
                                functionName = Constants.INIT_FUNCTION_NAME;
                            }
                            Map<String, String> paramDefaults = new HashMap<>();
                            extractFunctionDefaultValues(funcNode, paramDefaults);
                            if (!paramDefaults.isEmpty()) {
                                functionDefaults.put(functionName, paramDefaults);
                            }
                        }
                    }

                    if (!functionDefaults.isEmpty()) {
                        result.put(className, functionDefaults);
                    }
                }
            }
        }

        return result;
    }

    /**
     * Extracts default values from a function's parameters.
     */
    private void extractFunctionDefaultValues(FunctionDefinitionNode funcNode, Map<String, String> paramDefaults) {
        FunctionSignatureNode signature = funcNode.functionSignature();
        SeparatedNodeList<ParameterNode> parameters = signature.parameters();

        for (ParameterNode paramNode : parameters) {
            if (paramNode instanceof DefaultableParameterNode defaultableParam) {
                String paramName = defaultableParam.paramName().map(Token::text).orElse("");
                String defaultValue = defaultableParam.expression().toSourceCode().trim();
                // Clean up the default value (remove quotes for simple strings if needed)
                paramDefaults.put(paramName, defaultValue);
            }
        }
    }

    private void analyzeClass(Package compilePackage, Module module, ClassSymbol classSymbol,
                              Map<String, Map<String, String>> defaultValues) {
        SemanticModel semanticModel = compilePackage.getCompilation().getSemanticModel(module.moduleId());

        if (!isClientClass(classSymbol) || classSymbol.getName().isEmpty()) {
            return;
        }
        String clientClassName = classSymbol.getName().get();
        ModuleSymbol moduleSymbol = classSymbol.getModule().orElseThrow(() -> new IllegalStateException("Client class is outside the module"));
        String moduleName = moduleSymbol.getName().orElseThrow(() -> new IllegalStateException("Module name not defined"));
        String connectionType = String.format("%s_%s", moduleName, clientClassName);

        // Replace dots with underscores in connectionType if module name has dots
        if (moduleName.contains(".")) {
            connectionType = connectionType.replace(".", "_");
        }

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

        int i = 0;
        Map<String, MethodSymbol> allMethods = new HashMap<>(classSymbol.methods());
        classSymbol.initMethod().ifPresent(methodSymbol -> allMethods.put(Constants.INIT_FUNCTION_NAME, methodSymbol));

        // Track generated synapse names to handle duplicates
        Map<String, Integer> synapseNameCount = new HashMap<>();

        int totalOperations = 0;
        int skippedOperations = 0;

        for (Map.Entry<String, MethodSymbol> methodEntry : allMethods.entrySet()) {
            MethodSymbol methodSymbol = methodEntry.getValue();
            List<Qualifier> qualifierList = methodSymbol.qualifiers();
            if (!(Utils.containsToken(qualifierList, Qualifier.PUBLIC) ||
                    Utils.containsToken(qualifierList, Qualifier.REMOTE) ||
                    Utils.containsToken(qualifierList, Qualifier.RESOURCE))) {
                continue;
            }

            totalOperations++;

            String functionName = methodSymbol.getName().get();
            FunctionType functionType = Utils.getFunctionType(methodSymbol);
            // For init methods, use Constants.INIT_FUNCTION_NAME as key since we store it that way
            String functionKey = (functionType == FunctionType.INIT) ? Constants.INIT_FUNCTION_NAME : functionName;

            String returnType = Utils.getReturnTypeName(methodSymbol);
            String docString = methodSymbol.documentation().map(Utils::getDocString).orElse("");
            Component component = new Component(functionName, docString, functionType, Integer.toString(i),
                    List.of(), List.of(), returnType);

            // Get default values for this specific function
            Map<String, String> functionParamDefaults = defaultValues.getOrDefault(functionKey, Map.of());

            // Prepare context for synapse name generation
            SynapseNameContext.Builder contextBuilder = SynapseNameContext.builder().module(module);

            // Extract operationId from @openapi:ResourceInfo annotation if present using syntax tree API
            Optional<String> operationIdOpt = Optional.empty();
            try {
                operationIdOpt = Utils.getOpenApiOperationId(methodSymbol, module, semanticModel);
                if (operationIdOpt.isPresent()) {
                    printStream.println("Found operationId: " + operationIdOpt.get() + " for method: " + methodSymbol.getName().orElse("<unknown>"));
                }
            } catch (Exception e) {
                // If syntax tree access fails, continue without operationId
                printStream.println("Error extracting operationId for method: " + methodSymbol.getName().orElse("<unknown>") + " - " + e.getMessage());
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

            // Replace dots with underscores in synapse name if connector module name has dots
            if (connector.getModuleName().contains(".")) {
                synapseName = synapseName.replace(".", "_");
            }

            // NOTE: Defer duplicate-name bookkeeping until after parameter validation
            // to avoid reserving names for methods that will be skipped
            String finalSynapseName = synapseName;

            // Extract path parameters and path segments from resource path (for resource functions)
            List<PathParamType> pathParams = new ArrayList<>();
            Set<String> pathParamNames = new HashSet<>();
            List<ResourcePathSegment> resourcePathSegments = new ArrayList<>();

            if (functionType == FunctionType.RESOURCE && methodSymbol instanceof ResourceMethodSymbol resourceMethod) {
                try {
                    PathSegmentList resourcePath = (PathSegmentList) resourceMethod.resourcePath();
                    List<PathSegment> segments = resourcePath.list();

                    for (PathSegment segment : segments) {
                        String sig = segment.signature();
                        if (sig != null && sig.startsWith("[") && sig.endsWith("]")) {
                            // This is a path parameter segment (e.g., "[string userId]")
                            String inside = sig.substring(1, sig.length() - 1).trim();
                            String paramName = inside;
                            String paramType = "string"; // default type
                            int lastSpace = inside.lastIndexOf(' ');
                            if (lastSpace >= 0 && lastSpace + 1 < inside.length()) {
                                paramType = inside.substring(0, lastSpace).trim();
                                paramName = inside.substring(lastSpace + 1);
                            }
                            if (!paramName.isEmpty()) {
                                pathParamNames.add(paramName);
                                // Add as a dynamic path segment
                                resourcePathSegments.add(new ResourcePathSegment(paramName, paramType));
                            }
                        } else if (sig != null && !sig.isEmpty()) {
                            // This is a static path segment (e.g., "users", "drafts")
                            // Remove any leading quote (for escaped Ballerina keywords like 'import)
                            String staticSegment = sig.startsWith("'") ? sig.substring(1) : sig;
                            resourcePathSegments.add(new ResourcePathSegment(staticSegment));
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

            // Collect all parameter names (path params, function params) to check for conflicts
            Set<String> allParamNames = new HashSet<>(pathParamNames);
            if (params.isPresent()) {
                List<ParameterSymbol> parameterSymbols = params.get();
                for (ParameterSymbol parameterSymbol : parameterSymbols) {
                    Optional<String> paramNameOpt = parameterSymbol.getName();
                    if (paramNameOpt.isPresent()) {
                        allParamNames.add(paramNameOpt.get());
                    }
                }
            }

            // Check if synapse name conflicts with any parameter name and make it unique if needed
            Optional<String> methodNameOpt = methodSymbol.getName();
            if (allParamNames.contains(synapseName) ||
                    (methodNameOpt.isPresent() && allParamNames.contains(methodNameOpt.get()))) {
                // Add a suffix to make the synapse name unique and avoid conflicts
                synapseName = synapseName + "_operation";
            }

            // NOW handle duplicate names by appending numeric suffix
            // (All parameters are validated, so this method will not be skipped)
            if (synapseNameCount.containsKey(synapseName)) {
                int count = synapseNameCount.get(synapseName) + 1;
                synapseNameCount.put(synapseName, count);
                finalSynapseName = synapseName + count;
            } else {
                synapseNameCount.put(synapseName, 0);
                finalSynapseName = synapseName;
            }

            component = new Component(finalSynapseName, docString, functionType, Integer.toString(i), pathParams, List.of(), returnType);

            // For resource functions, store the accessor and path segments for invocation
            if (functionType == FunctionType.RESOURCE) {
                component.setResourceAccessor(functionName); // functionName is the accessor (get, post, etc.)
                component.setResourcePathSegments(resourcePathSegments);
            }

            // Store operationId as a parameter if found and mark the component
            if (operationIdOpt.isPresent()) {
                Param operationIdParam = new Param("operationId", operationIdOpt.get());
                component.setParam(operationIdParam);
                component.setHasOperationId(true);
            }


            // Now add all function parameters (we keep them all, synapse name is made unique instead)
            int paramIndex = 0;
            boolean shouldSkipOperation = false;
            if (params.isPresent()) {
                List<ParameterSymbol> parameterSymbols = params.get();
                for (ParameterSymbol parameterSymbol : parameterSymbols) {
                    Optional<FunctionParam> functionParam = ParamFactory.createFunctionParam(parameterSymbol, paramIndex);
                    if (functionParam.isEmpty()) {
                        String paramType = parameterSymbol.typeDescriptor().typeKind().getName();
                        printStream.println("Skipping function '" + functionName +
                                "' due to unsupported parameter type: " + paramType);
                        skippedOperations++;
                        shouldSkipOperation = true;
                        break; // Exit parameter loop, skip this entire operation
                    }

                    FunctionParam param = functionParam.get();
                    String paramName = parameterSymbol.getName().orElse("");
                    if (functionParamDefaults.containsKey(paramName)) {
                        String defaultValue = functionParamDefaults.get(paramName);
                        if (defaultValue.startsWith("\"") && defaultValue.endsWith("\"")) {
                            defaultValue = defaultValue.substring(1, defaultValue.length() - 1);
                        } else if ("()".equals(defaultValue)) {
                            // Convert Ballerina nil to empty string for UI schema
                            defaultValue = "";
                        }
                        param.setDefaultValue(defaultValue);
                        param.setRequired(false);
                    }
                    component.setFunctionParam(param);
                    paramIndex++;
                }

                // Only add component if we didn't skip due to unsupported parameter
                if (!shouldSkipOperation) {
                    Param sizeParam = new Param("paramSize", Integer.toString(paramIndex));
                    Param functionNameParam = new Param("paramFunctionName", component.getName());
                    component.setParam(sizeParam);
                    component.setParam(functionNameParam);
                }
            }

            // Only add the component if it wasn't skipped
            if (!shouldSkipOperation) {
                if (functionType == FunctionType.INIT) {
                    // objectTypeName is only needed on Connection, not on Component
                    // to avoid duplication in generated XML
                    connection.setInitComponent(component);
                } else {
                    connection.setComponent(component);
                }
                i++;
            }
        }

        // Abort generation only if no operations exist
        if (totalOperations == 0) {
            String message = String.format("WARNING: No operations found in class '%s'. Artifact generation will be skipped.", clientClassName);
            printStream.println(message);
            connector.setGenerationAborted(true, message);
            return;
        }

        if (skippedOperations > 0) {
            String message = String.format("WARNING: %d out of %d operations were skipped due to " +
                            "unsupported parameter types for '%s'.",
                    skippedOperations, totalOperations, clientClassName);
            printStream.println(message);
        }

        connector.setConnection(connection);
    }

    private boolean isClientClass(ClassSymbol classSymbol) {
        return classSymbol.qualifiers().contains(Qualifier.PUBLIC) && classSymbol.qualifiers().contains(Qualifier.CLIENT);
    }

    /**
     * Counts the number of expanded parameters. For RecordFunctionParam, counts its fields recursively.
     * For other params, returns 1.
     */
    private int countExpandedParams(FunctionParam param) {
        if (param instanceof RecordFunctionParam recordParam && !recordParam.getRecordFieldParams().isEmpty()) {
            int count = 0;
            for (FunctionParam fieldParam : recordParam.getRecordFieldParams()) {
                count += countExpandedParams(fieldParam);
            }
            return count;
        }
        return 1;
    }
}
