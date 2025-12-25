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
import io.ballerina.compiler.syntax.tree.*;
import io.ballerina.mi.connectorModel.*;
import io.ballerina.mi.util.Constants;
import io.ballerina.mi.util.Utils;
import io.ballerina.projects.Document;
import io.ballerina.projects.DocumentId;
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

        // Extract default values from syntax trees
        Map<String, Map<String, Map<String, String>>> classMethodDefaultValues = extractDefaultValues(module);

        for (Symbol classSymbol : classSymbols) {
            String className = classSymbol.getName().orElse("");
            Map<String, Map<String, String>> methodDefaultValues = classMethodDefaultValues.getOrDefault(className, Map.of());
            analyzeClass(compilePackage, (ClassSymbol) classSymbol, methodDefaultValues);
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

    private void analyzeClass(Package compilePackage, ClassSymbol classSymbol, Map<String, Map<String, String>> defaultValues) {

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

        methodLoop:
        for (Map.Entry<String, MethodSymbol> methodEntry : allMethods.entrySet()) {
            MethodSymbol methodSymbol = methodEntry.getValue();
            List<Qualifier> qualifierList = methodSymbol.qualifiers();
            if (!(Utils.containsToken(qualifierList, Qualifier.PUBLIC) ||
                    Utils.containsToken(qualifierList, Qualifier.REMOTE) ||
                    Utils.containsToken(qualifierList, Qualifier.RESOURCE))) {
                continue;
            }

            String functionName = methodSymbol.getName().get();
            FunctionType functionType = Utils.getFunctionType(methodSymbol);
            // For init methods, use Constants.INIT_FUNCTION_NAME as key since we store it that way
            String functionKey = (functionType == FunctionType.INIT) ? Constants.INIT_FUNCTION_NAME : functionName;

            String returnType = Utils.getReturnTypeName(methodSymbol);
            Component component = new Component(functionName, Utils.getDocString(methodSymbol.documentation().get()), functionType, Integer.toString(i), List.of(), List.of(), returnType);

            // Get default values for this specific function
            Map<String, String> functionParamDefaults = defaultValues.getOrDefault(functionKey, Map.of());

            Optional<List<ParameterSymbol>> params = methodSymbol.typeDescriptor().params();
            if (params.isPresent()) {
                List<ParameterSymbol> parameterSymbols = params.get();
                int paramIndex = 0;
                for (ParameterSymbol parameterSymbol : parameterSymbols) {
                    Optional<FunctionParam> functionParam = ParamFactory.createFunctionParam(parameterSymbol, paramIndex);
                    if (functionParam.isPresent()) {
                        FunctionParam param = functionParam.get();
                        // Set default value if available for this function parameter
                        String paramName = parameterSymbol.getName().orElse("");
                        if (functionParamDefaults.containsKey(paramName)) {
                            String defaultValue = functionParamDefaults.get(paramName);
                            // Clean up the default value - remove surrounding quotes for string literals
                            if (defaultValue.startsWith("\"") && defaultValue.endsWith("\"")) {
                                defaultValue = defaultValue.substring(1, defaultValue.length() - 1);
                            }
                            param.setDefaultValue(defaultValue);
                            param.setRequired(false);
                        }
                        component.setFunctionParam(param);
                        // Count expanded params for records, otherwise count 1
                        paramIndex += countExpandedParams(param);
                    } else {
                        // Skip the function if any parameter type is unsupported
                        String paramType = parameterSymbol.typeDescriptor().typeKind().getName();
                        printStream.println("Skipping function '" + functionName +
                                "' due to unsupported parameter type: " + paramType);
                        continue methodLoop;
                    }
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
