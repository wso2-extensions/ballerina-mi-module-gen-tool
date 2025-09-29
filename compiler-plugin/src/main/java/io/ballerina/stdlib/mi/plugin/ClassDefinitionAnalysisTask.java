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

package io.ballerina.stdlib.mi.plugin;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.ClassSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.syntax.tree.ClassDefinitionNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.FunctionSignatureNode;
import io.ballerina.compiler.syntax.tree.MetadataNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.projects.PackageDescriptor;
import io.ballerina.projects.PackageReadmeMd;
import io.ballerina.projects.plugins.AnalysisTask;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.stdlib.mi.plugin.connectorModel.*;
import org.ballerinalang.diagramutil.connector.generator.GeneratorUtils;
import org.ballerinalang.diagramutil.connector.models.connector.Type;
import org.ballerinalang.diagramutil.connector.models.connector.types.PathParamType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static io.ballerina.stdlib.mi.plugin.Utils.getDocFromMetadata;

public class ClassDefinitionAnalysisTask implements AnalysisTask<SyntaxNodeAnalysisContext> {
    private static final String METHOD_NAME = "MethodName";
    private static final String PATH_PARAM_SIZE = "PathParamSize";
    private static final String QUERY_PARAM_SIZE = "QueryParamSize";

    @Override
    public void perform(SyntaxNodeAnalysisContext syntaxNodeAnalysisContext) {
        SemanticModel semanticModel = syntaxNodeAnalysisContext.compilation()
                .getSemanticModel(syntaxNodeAnalysisContext.moduleId());

        //TODO: Handle no client libraries public methods
        ClassDefinitionNode classDefinition = (ClassDefinitionNode) syntaxNodeAnalysisContext.node();
        // The class should be a public client class
        if (classDefinition.visibilityQualifier().isEmpty() || !classDefinition.visibilityQualifier().get()
                .kind().equals(SyntaxKind.PUBLIC_KEYWORD) ||
                !containsToken(classDefinition.classTypeQualifiers(), SyntaxKind.CLIENT_KEYWORD)
        ) {
            return;
        }

        // Access the client class symbol
        Optional<Symbol> optionalClassSymbol = semanticModel.symbol(classDefinition);
        if (optionalClassSymbol.isEmpty()) {
            return;
        }
        if (!(optionalClassSymbol.get() instanceof ClassSymbol clientClassSymbol)) return;

        //Define the connection type
        String clientClassName = clientClassSymbol.getName().get();
        String moduleName = syntaxNodeAnalysisContext.currentPackage().module(syntaxNodeAnalysisContext.moduleId()).moduleName().toString();
        String connectionType = String.format("%s_%s", moduleName, clientClassName);

        PackageDescriptor descriptor = syntaxNodeAnalysisContext.currentPackage().manifest().descriptor();
        Connector connector = Connector.getConnector(descriptor);
        Connection connection = new Connection(connector, connectionType, clientClassName, Integer.toString(connector.getConnections().size()));

        // Get the connector description
        Optional<PackageReadmeMd> connectorReadMe = syntaxNodeAnalysisContext.currentPackage().readmeMd();
        if (connectorReadMe.isPresent() && !connectorReadMe.get().content().isEmpty()) {
            connector.setDescription(connectorReadMe.get().content());
        } else {
            connector.setDescription(String.format("Ballerina %s connector", connector.getModuleName()));
        }

        // Get the connection description
        Optional<MetadataNode> optionalMetadataNode = classDefinition.metadata();
        if (optionalMetadataNode.isEmpty()) {
            connection.setDescription(String.format("%s connection for Ballerina %s connector", connectionType,
                    connector.getModuleName()));
        } else {
            connection.setDescription(getDocFromMetadata(optionalMetadataNode.get()));
        }

        /**
         * TODO: DO: Get the icon - from central or context or from the docs dir inside the bala - small and large icon
         * syntaxNodeAnalysisContext.currentPackage().project().sourceRoot()
         */

        for (Node member : classDefinition.members()) {
            if (!(member instanceof FunctionDefinitionNode functionDefinition)) {
                continue;
            }
            NodeList<Token> qualifierList = functionDefinition.qualifierList();
            if (!(containsToken(qualifierList, SyntaxKind.PUBLIC_KEYWORD) ||
                    containsToken(qualifierList, SyntaxKind.REMOTE_KEYWORD) ||
                    containsToken(qualifierList, SyntaxKind.RESOURCE_KEYWORD))) {
                continue;
            }

            String functionName = functionDefinition.functionName().text();
            FunctionSignatureNode functionSignature = functionDefinition.functionSignature();
            FunctionType functionType = FunctionType.FUNCTION;
            if (functionName.equals(Constants.INIT_FUNCTION_NAME)) {
                functionType = FunctionType.INIT;
            } else if (containsToken(qualifierList, SyntaxKind.REMOTE_KEYWORD)) {
                functionType = FunctionType.REMOTE;
            } else if (containsToken(qualifierList, SyntaxKind.RESOURCE_KEYWORD)) {
                functionType = FunctionType.RESOURCE;
            }
            List<PathParamType> pathParams = new ArrayList<>(GeneratorUtils.getPathParameters(
                    functionDefinition.relativeResourcePath()));
            List<Type> queryParams = new ArrayList<>(GeneratorUtils.getFunctionParameters(
                    functionSignature.parameters(),
                    functionDefinition.metadata(), semanticModel));
            Type returnParam = null;
            if (functionSignature.returnTypeDesc().isPresent()) {
                returnParam = GeneratorUtils.getReturnParameter(functionSignature.returnTypeDesc().get(),
                        functionDefinition.metadata(), semanticModel);
            }

            String componentIndex = Integer.toString(connection.getComponents().size());
            Component component = new Component(functionName,
                    GeneratorUtils.getDocFromMetadata(functionDefinition.metadata()),
                    functionType, componentIndex, pathParams, queryParams, returnParam);
            int noOfQueryParams = queryParams.size();
            int noOfPathParams = pathParams.size();
            Param querySizeParam = new Param(QUERY_PARAM_SIZE, Integer.toString(noOfQueryParams));
            Param pathSizeParam = new Param(PATH_PARAM_SIZE, Integer.toString(noOfPathParams));
            Param functionNameParam = new Param(METHOD_NAME, component.getName());
            component.setParam(querySizeParam);
            component.setParam(pathSizeParam);
            component.setParam(functionNameParam);
            if (functionType == FunctionType.INIT) {
                component.setObjectTypeName(clientClassSymbol.getName().get());
                connection.setInitComponent(component);
            } else {
                connection.setComponent(component);
            }
        }
        connector.setConnection(connection);
    }

    static boolean containsToken(NodeList<Token> nodeList, SyntaxKind kind) {
        for (Node node: nodeList) {
            if (node.kind() == kind) {
                return true;
            }
        }
        return false;
    }

//    //TODO: Remove
//    private static void setParameters(SyntaxNodeAnalysisContext context, MethodSymbol methodSymbol,
//                                      Component component) {
//        int noOfParams = 0;
//        Optional<List<ParameterSymbol>> methodParams = methodSymbol.typeDescriptor().params();
//        if (methodParams.isPresent()) {
//            List<ParameterSymbol> parameterSymbols = methodParams.get();
//            noOfParams = parameterSymbols.size();
//
//            for (int i = 0; i < noOfParams; i++) {
//                ParameterSymbol parameterSymbol = parameterSymbols.get(i);
//                String paramType = getParamTypeName(parameterSymbol);
//
//                if (paramType != null) {
//                    // TODO: FIX-even if params are null the noOfParams is not reduced
//                    Optional<String> optParamName = parameterSymbol.getName();
//                    if (optParamName.isPresent()) {
//                        FunctionParam param = new FunctionParam(Integer.toString(i), optParamName.get(), paramType);
//                        // TODO: COMPLETE for other datatypes and for union
//                        if ((TypeDescKind.UNION.getName().equals(paramType)) &&
//                                (parameterSymbol.typeDescriptor() instanceof BallerinaUnionTypeSymbol unionSymbol)) {
//                            // TODO: Members of a union can have records and other types. This assumes members are primitive types
//                            param.setDataType(new UnionDataType(unionSymbol));
//                        }
//                        if (TypeDescKind.RECORD.getName().equals(paramType)) {
//                            BallerinaTypeReferenceTypeSymbol typeRefSymbol = (BallerinaTypeReferenceTypeSymbol) parameterSymbol.typeDescriptor();
//                            BallerinaRecordTypeSymbol recordSymbol = (BallerinaRecordTypeSymbol) typeRefSymbol.typeDescriptor();
//                            param.setDataType(new RecordDataType(recordSymbol, Arrays.stream(recordSymbol.getBType().getQualifiedTypeName().split(":")).toList().get(2)));
//
//                        }
//                        //TODO: Getting default values - not needed for calling methods, only needed if needed for the UI
//                        component.addBalFuncParams(param);
//                    }
//
//                }
//                //TODO: handle null param type - this means that data type is not supported
//            }
//        }
//        Param sizeParam = new Param(SIZE, Integer.toString(noOfParams));
//        Param functionNameParam = new Param(METHOD_NAME, component.getName());
//        component.setParam(sizeParam);
//        component.setParam(functionNameParam);
//
//        // Get the function return type
//        //TODO: Break into a separate function
//        Optional<TypeSymbol> optReturnTypeSymbol = methodSymbol.typeDescriptor().returnTypeDescriptor();
//        if (optReturnTypeSymbol.isEmpty()) {
//            component.setBalFuncReturnType(new FunctionReturn(TypeDescKind.NIL.getName()));
//        } else {
//            FunctionReturn funcReturn = getFunctionReturn(optReturnTypeSymbol.get());
//            component.setBalFuncReturnType(funcReturn);
//            //TODO: Consider below condition
////            if (returnType != null) {
////                component.setBalFuncReturnType(returnType);
////            }
//        }
//    }

    //TODO: remove
//    private static FunctionReturn getFunctionReturn(TypeSymbol returnTypeSymbol) {
//        String returnType = getReturnTypeName(returnTypeSymbol.typeKind());
//        FunctionReturn funcReturn = new FunctionReturn(returnType);
//
//        // TODO: Below only handles union and record types
//        if ((TypeDescKind.UNION.getName().equals(returnType)) &&
//                (returnTypeSymbol instanceof BallerinaUnionTypeSymbol unionSymbol)) {
//            funcReturn.setDataType(new UnionDataType(unionSymbol));
//        } else if ((TypeDescKind.RECORD.getName().equals(returnType)) &&
//                (returnTypeSymbol instanceof BallerinaRecordTypeSymbol recordSymbol)) {
//            funcReturn.setDataType(new RecordDataType(recordSymbol, Arrays.stream(recordSymbol.getBType().getQualifiedTypeName().split(":")).toList().get(2)));
//        }
//        return funcReturn;
//    }

    //TODO: remove
//    private static String getParamTypeName(ParameterSymbol paramSymbol) {
//        TypeDescKind typeKind = paramSymbol.typeDescriptor().typeKind();
//        return switch (typeKind) {
//            case BOOLEAN, INT, STRING, FLOAT, DECIMAL, XML, JSON, RECORD, UNION -> typeKind.getName();
//            case TYPE_REFERENCE -> {
//                BallerinaTypeReferenceTypeSymbol typeRefSymbol = (BallerinaTypeReferenceTypeSymbol) paramSymbol.typeDescriptor();
//
//                if (typeRefSymbol.typeDescriptor().typeKind().getName().equals(TypeDescKind.RECORD.getName())) {
//                    yield typeRefSymbol.typeDescriptor().typeKind().getName();
//                } else {
//                    yield null;
//                }
//            }
//            default -> null;
//        };
//    }

    //TODO: remove
//    private static String getReturnTypeName(TypeDescKind typeKind) {
//        return switch (typeKind) {
//            case NIL, BOOLEAN, INT, STRING, FLOAT, DECIMAL, XML, JSON, ANY, UNION, RECORD -> typeKind.getName();
//            default -> null;
//        };
//    }
}
