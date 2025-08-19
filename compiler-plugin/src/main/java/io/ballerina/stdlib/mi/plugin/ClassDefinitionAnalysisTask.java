package io.ballerina.stdlib.mi.plugin;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.impl.symbols.*;
import io.ballerina.compiler.api.symbols.*;
import io.ballerina.compiler.syntax.tree.*;
import io.ballerina.projects.PackageDescriptor;
import io.ballerina.projects.plugins.AnalysisTask;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.stdlib.mi.plugin.connectorModel.*;
import org.ballerinalang.diagramutil.connector.generator.GeneratorUtils;
import org.ballerinalang.diagramutil.connector.models.connector.Type;
import org.ballerinalang.diagramutil.connector.models.connector.types.PathParamType;

import java.util.*;

import static io.ballerina.stdlib.mi.plugin.Utils.getDocFromMetadata;

enum FunctionType {
    FUNCTION,
    REMOTE,
    INIT,
    RESOURCE
}

public class ClassDefinitionAnalysisTask implements AnalysisTask<SyntaxNodeAnalysisContext> {
    private static final String METHOD_NAME = "MethodName";
    private static final String SIZE = "Size";

    @Override
    public void perform(SyntaxNodeAnalysisContext syntaxNodeAnalysisContext) {
        SemanticModel semanticModel = syntaxNodeAnalysisContext.compilation().getSemanticModel(syntaxNodeAnalysisContext.moduleId());

        //TODO: Handle multiple clients - multiple connection types
        // multiple connector objects, getting the connection name. can there be two clients with the same object type names?
        //TODO: Handle no client libraries public methods
        ClassDefinitionNode classDefinition = (ClassDefinitionNode) syntaxNodeAnalysisContext.node();
        // The class should be a public client class
        if (classDefinition.visibilityQualifier().isEmpty() || !classDefinition.visibilityQualifier().get()
                .kind().equals(SyntaxKind.PUBLIC_KEYWORD) ||
                !containsToken(classDefinition.classTypeQualifiers(), SyntaxKind.CLIENT_KEYWORD)
        ) {
            return;
        }
        // TODO: Add diagnostics
        //TODO: Get default values
        // classDefinition.members().get(1).internalNode().functionSignature.parameters.get(2).expression.toString()

        // Access the client class symbol
        Optional<Symbol> optionalClassSymbol = semanticModel.symbol(classDefinition);
        if (optionalClassSymbol.isEmpty()) {
            return;
        }
        if (!(optionalClassSymbol.get() instanceof ClassSymbol clientClassSymbol)) return;

        // Process the client class
        //TODO: pass descriptor details when creating a connector
        Connector connector = Connector.getConnector();
        PackageDescriptor descriptor = syntaxNodeAnalysisContext.currentPackage().manifest().descriptor();
        setModuleInfo(descriptor, connector);

        //TODO: Test connector description
        Optional<MetadataNode> optionalMetadataNode = classDefinition.metadata();
        if (optionalMetadataNode.isEmpty()) {
            connector.setDescription(String.format("Ballerina %s connector", connector.getModuleName()));
        } else {
            connector.setDescription(getDocFromMetadata(classDefinition.metadata().get()));
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
            //TODO: Handle path params
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

            String[] qualifierArr = new String[qualifierList.size()];
            for (int i = 0; i < qualifierList.size(); i++) {
                qualifierArr[i] = qualifierList.get(i).toString().trim();
            }

            Component component = new Component(functionName, pathParams, queryParams, returnParam,
                    qualifierArr, GeneratorUtils.getDocFromMetadata(functionDefinition.metadata()));
            if (functionName.equals("init")) {
                component.setIsConfig(true);
                component.setObjectTypeName(clientClassSymbol.getName().get().toString());
            }
            component.setParent(connector);

            //TODO: count in path params
            int noOfParams = queryParams.size();
            Param sizeParam = new Param(SIZE, Integer.toString(noOfParams));
            Param functionNameParam = new Param(METHOD_NAME, component.getName());
            component.setParam(sizeParam);
            component.setParam(functionNameParam);

            connector.setComponent(component);
        }


        // Access the init method symbol
        //TODO: Check and remove
//        BallerinaMethodSymbol initMethodSymbol = (BallerinaMethodSymbol) clientClassSymbol.initMethod().get();
////        if (initMethodSymbol.isEmpty()) {
////            return;
////        }
//
//
//        Optional<String> initName = initMethodSymbol.getName();
//        if (initName.isEmpty()) {
//            return;
//        }
//        // TODO: get description from functionSymbol.documentation()
//        Component configComponent = new Component(initName.get());
//        configComponent.setIsConfig(true);
//        configComponent.setParent(connector);
//        configComponent.setObjectTypeName(clientClassSymbol.getName().get());
//        // TODO: IMPLEMENT parameter handling logic for init - trying init() with no params for now
//        setParameters(syntaxNodeAnalysisContext, initMethodSymbol, configComponent);
//        connector.setComponent(configComponent);
//
//
////        Access client class methods
//        Map<String, MethodSymbol> methods = clientClassSymbol.methods();
//        for (Map.Entry<String, MethodSymbol> entry : methods.entrySet()) {
//            MethodSymbol methodSymbol = entry.getValue();
//
//            List<Qualifier> qualifiers = methodSymbol.qualifiers();
//            // TODO: These are not made use of yet
//            FunctionType functionType;
//            if (qualifiers.contains(Qualifier.REMOTE)) {
//                functionType = FunctionType.REMOTE;
//            } else if (qualifiers.contains(Qualifier.RESOURCE)) {
//                functionType = FunctionType.RESOURCE;
//            } else if (qualifiers.contains(Qualifier.PUBLIC)) {
//                functionType = FunctionType.FUNCTION;
//            } else {
//                continue;
//            }
//
//            Optional<String> methodName = methodSymbol.getName();
//            if (methodName.isEmpty()) return;
//            // TODO: Handled only for remote functions
//            Component methodComponent = new Component(methodName.get());
//            setParameters(syntaxNodeAnalysisContext, methodSymbol, methodComponent);
//            connector.setComponent(methodComponent);
//        }
    }

    static boolean containsToken(NodeList<Token> nodeList, SyntaxKind kind) {
        for (Node node: nodeList) {
            if (node.kind() == kind) {
                return true;
            }
        }
        return false;
    }

    private static void setModuleInfo(PackageDescriptor descriptor, Connector connector) {
        String orgName = descriptor.org().value();
        String moduleName = descriptor.name().value();
        String version = String.valueOf(descriptor.version().value().major());

        if (connector.getOrgName() == null) {
            connector.setOrgName(orgName);
        }
        if (connector.getModuleName() == null) {
            connector.setModuleName(moduleName);
        }
        if (connector.getModuleVersion() == null) {
            connector.setModuleVersion(version);
        }

         // TODO: set description with clientClassSymbol.documentation().flatMap(Documentation::description)
    }

    private static void setParameters(SyntaxNodeAnalysisContext context, MethodSymbol methodSymbol,
                                      Component component) {
        int noOfParams = 0;
        Optional<List<ParameterSymbol>> methodParams = methodSymbol.typeDescriptor().params();
        if (methodParams.isPresent()) {
            List<ParameterSymbol> parameterSymbols = methodParams.get();
            noOfParams = parameterSymbols.size();

            for (int i = 0; i < noOfParams; i++) {
                ParameterSymbol parameterSymbol = parameterSymbols.get(i);
                String paramType = getParamTypeName(parameterSymbol);

                if (paramType != null) {
                    // TODO: FIX-even if params are null the noOfParams is not reduced
                    Optional<String> optParamName = parameterSymbol.getName();
                    if (optParamName.isPresent()) {
                        FunctionParam param = new FunctionParam(Integer.toString(i), optParamName.get(), paramType);
                        // TODO: COMPLETE for other datatypes and for union
                        if ((TypeDescKind.UNION.getName().equals(paramType)) &&
                                (parameterSymbol.typeDescriptor() instanceof BallerinaUnionTypeSymbol unionSymbol)) {
                            // TODO: Members of a union can have records and other types. This assumes members are primitive types
                            param.setDataType(new UnionDataType(unionSymbol));
                        }
                        if (TypeDescKind.RECORD.getName().equals(paramType)) {
                            BallerinaTypeReferenceTypeSymbol typeRefSymbol = (BallerinaTypeReferenceTypeSymbol) parameterSymbol.typeDescriptor();
                            BallerinaRecordTypeSymbol recordSymbol = (BallerinaRecordTypeSymbol) typeRefSymbol.typeDescriptor();
                            param.setDataType(new RecordDataType(recordSymbol, Arrays.stream(recordSymbol.getBType().getQualifiedTypeName().split(":")).toList().get(2)));

                        }
                        //TODO: Getting default values - not needed for calling methods, only needed if needed for the UI
                        component.addBalFuncParams(param);
                    }

                }
                //TODO: handle null param type - this means that data type is not supported
            }
        }
        Param sizeParam = new Param(SIZE, Integer.toString(noOfParams));
        Param functionNameParam = new Param(METHOD_NAME, component.getName());
        component.setParam(sizeParam);
        component.setParam(functionNameParam);

        // Get the function return type
        //TODO: Break into a separate function
        Optional<TypeSymbol> optReturnTypeSymbol = methodSymbol.typeDescriptor().returnTypeDescriptor();
        if (optReturnTypeSymbol.isEmpty()) {
            component.setBalFuncReturnType(new FunctionReturn(TypeDescKind.NIL.getName()));
        } else {
            FunctionReturn funcReturn = getFunctionReturn(optReturnTypeSymbol.get());
            component.setBalFuncReturnType(funcReturn);
            //TODO: Consider below condition
//            if (returnType != null) {
//                component.setBalFuncReturnType(returnType);
//            }
        }
    }

    private static FunctionReturn getFunctionReturn(TypeSymbol returnTypeSymbol) {
        String returnType = getReturnTypeName(returnTypeSymbol.typeKind());
        FunctionReturn funcReturn = new FunctionReturn(returnType);

        // TODO: Below only handles union and record types
        if ((TypeDescKind.UNION.getName().equals(returnType)) &&
                (returnTypeSymbol instanceof BallerinaUnionTypeSymbol unionSymbol)) {
            funcReturn.setDataType(new UnionDataType(unionSymbol));
        } else if ((TypeDescKind.RECORD.getName().equals(returnType)) &&
                (returnTypeSymbol instanceof BallerinaRecordTypeSymbol recordSymbol)) {
            funcReturn.setDataType(new RecordDataType(recordSymbol, Arrays.stream(recordSymbol.getBType().getQualifiedTypeName().split(":")).toList().get(2)));
        }
        return funcReturn;
    }

    private static String getParamTypeName(ParameterSymbol paramSymbol) {
        TypeDescKind typeKind = paramSymbol.typeDescriptor().typeKind();
        return switch (typeKind) {
            case BOOLEAN, INT, STRING, FLOAT, DECIMAL, XML, JSON, RECORD, UNION -> typeKind.getName();
            case TYPE_REFERENCE -> {
                BallerinaTypeReferenceTypeSymbol typeRefSymbol = (BallerinaTypeReferenceTypeSymbol) paramSymbol.typeDescriptor();

                if (typeRefSymbol.typeDescriptor().typeKind().getName().equals(TypeDescKind.RECORD.getName())) {
                    yield typeRefSymbol.typeDescriptor().typeKind().getName();
                } else {
                    yield null;
                }
            }
            default -> null;
        };
    }

    private static String getReturnTypeName(TypeDescKind typeKind) {
        return switch (typeKind) {
            case NIL, BOOLEAN, INT, STRING, FLOAT, DECIMAL, XML, JSON, ANY, UNION, RECORD -> typeKind.getName();
            default -> null;
        };
    }
}
