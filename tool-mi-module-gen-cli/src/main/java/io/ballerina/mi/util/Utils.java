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

package io.ballerina.mi.util;

import io.ballerina.compiler.api.symbols.*;
import io.ballerina.mi.connectorModel.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Utils {
    /**
     * These are utility functions used when generating XML and JSON content
     */
    public static String readFile(String fileName) throws IOException {
        InputStream inputStream = Utils.class.getClassLoader().getResourceAsStream(fileName.replace("\\", "/"));
        assert inputStream != null;
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        StringBuilder fileContent = new StringBuilder();
        int character;
        while ((character = reader.read()) != -1) {
            fileContent.append((char) character);
        }
        reader.close();
        return fileContent.toString();
    }

    /**
     * These are private utility functions used in the generateXml method
     */
    public static void writeFile(String fileName, String content) throws IOException {
        FileWriter myWriter = new FileWriter(fileName);
        myWriter.write(content);
        myWriter.close();
    }

//    /**
//     * This is a private utility function for
//     */
//    //TODO
//    public static String getDocFromMetadata(MetadataNode optionalMetadataNode) {
//        StringBuilder doc = new StringBuilder();
//        MarkdownDocumentationNode docLines = optionalMetadataNode.documentationString().isPresent() ?
//                (MarkdownDocumentationNode) optionalMetadataNode.documentationString().get() : null;
//        if (docLines != null) {
//            for (Node docLine : docLines.documentationLines()) {
//                if (docLine instanceof MarkdownDocumentationLineNode) {
//                    doc.append(!((MarkdownDocumentationLineNode) docLine).documentElements().isEmpty() ?
//                            getDocLineString(((MarkdownDocumentationLineNode) docLine).documentElements()) : "\n");
//                } else if (docLine instanceof MarkdownCodeBlockNode) {
//                    doc.append(getDocCodeBlockString((MarkdownCodeBlockNode) docLine));
//                } else {
//                    break;
//                }
//            }
//        }
//
//        return doc.toString();
//    }

//    //TODO
//    public static String getDocLineString(NodeList<Node> documentElements) {
//        if (documentElements.isEmpty()) {
//            return null;
//        }
//        StringBuilder doc = new StringBuilder();
//        for (Node docNode : documentElements) {
//            doc.append(docNode.toString());
//        }
//
//        return doc.toString();
//    }

//    //TODO
//    public static String getDocCodeBlockString(MarkdownCodeBlockNode markdownCodeBlockNode) {
//        StringBuilder doc = new StringBuilder();
//
//        doc.append(markdownCodeBlockNode.startBacktick().toString());
//        markdownCodeBlockNode.langAttribute().ifPresent(langAttribute -> doc.append(langAttribute.toString()));
//
//        for (MarkdownCodeLineNode codeLineNode : markdownCodeBlockNode.codeLines()) {
//            doc.append(codeLineNode.codeDescription().toString());
//        }
//
//        doc.append(markdownCodeBlockNode.endBacktick().toString());
//        return doc.toString();
//    }

    public static boolean containsToken(List<Qualifier> qualifiers, Qualifier kind) {
        for (Qualifier qualifier : qualifiers) {
            if (qualifier == kind) {
                return true;
            }
        }
        return false;
    }

    /**
     * Zip a folder and its contents.
     *
     * @param sourceDirPath Path to the source directory
     * @param zipFilePath   Path to the output ZIP file
     * @throws IOException If an I/O error occurs
     * @Note: This method is used to zip the Annotations. CONNECTOR directory and create a zip file using the module
     * name and Annotations.ZIP_FILE_SUFFIX
     */
    public static void zipFolder(Path sourceDirPath, String zipFilePath) throws IOException {
        try (ZipOutputStream outputStream = new ZipOutputStream(Files.newOutputStream(Paths.get(zipFilePath)))) {
            Files.walkFileTree(sourceDirPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Path targetFile = sourceDirPath.relativize(file);
                    outputStream.putNextEntry(new ZipEntry(targetFile.toString()));
                    Files.copy(file, outputStream);
                    outputStream.closeEntry();
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    if (!dir.equals(sourceDirPath)) {
                        Path targetDir = sourceDirPath.relativize(dir);
                        outputStream.putNextEntry(new ZipEntry(targetDir + "/"));
                        outputStream.closeEntry();
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    /**
     * Delete a directory and all its contents.
     *
     * @param dirPath Path to the directory to be deleted
     * @throws IOException If an I/O error occurs
     * @Note : This method is used to delete the intermediate Annotations.CONNECTOR directory
     */
    public static void deleteDirectory(Path dirPath) throws IOException {
        Path directory = dirPath;
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

//    private static void updateConstants(InputStream inputStream, String outputPath, String org, String module,
//                                        String moduleVersion) throws IOException {
//        ClassReader classReader = new ClassReader(inputStream.readAllBytes());
//        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
//        ClassVisitor classVisitor = new ClassVisitor(Opcodes.ASM7, classWriter) {
//            @Override
//            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
//                                             String[] exceptions) {
//                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
//                return new MethodVisitor(Opcodes.ASM7, mv) {
//                    @Override
//                    public void visitLdcInsn(Object value) {
//                        if ("BALLERINA_ORG_NAME".equals(value)) {
//                            super.visitLdcInsn(org);
//                        } else if ("BALLERINA_MODULE_NAME".equals(value)) {
//                            super.visitLdcInsn(module);
//                        } else if ("BALLERINA_MODULE_VERSION".equals(value)) {
//                            super.visitLdcInsn(moduleVersion);
//                        } else {
//                            super.visitLdcInsn(value);
//                        }
//                    }
//                };
//            }
//        };
//
//        classReader.accept(classVisitor, ClassReader.SKIP_DEBUG);
//        byte[] modifiedBytecode = classWriter.toByteArray();
//
//        FileOutputStream fos = new FileOutputStream(outputPath);
//        fos.write(modifiedBytecode);
//        fos.close();
//    }

    public static String getDocString(Documentation documentation) {
        //TODO: get the full documenation
        return documentation.description().orElse("");
    }

    public static FunctionType getFunctionType(FunctionSymbol functionSymbol) {

        List<Qualifier> qualifierList = functionSymbol.qualifiers();
        String functionName = functionSymbol.getName().orElse("");
        if (functionName.equals(Constants.INIT_FUNCTION_NAME)) {
            return FunctionType.INIT;
        } else if (containsToken(qualifierList, Qualifier.REMOTE)) {
            return FunctionType.REMOTE;
        } else if (containsToken(qualifierList, Qualifier.RESOURCE)) {
            return FunctionType.RESOURCE;
        } else {
            return FunctionType.FUNCTION;
        }
    }

    public static String getParamTypeName(TypeDescKind typeKind) {
        return switch (typeKind) {
            case INT, INT_SIGNED8, INT_SIGNED16, INT_SIGNED32, INT_UNSIGNED8, INT_UNSIGNED16, INT_UNSIGNED32 ->
                    Constants.INT;
            case STRING, STRING_CHAR -> Constants.STRING;
            case BOOLEAN, FLOAT, DECIMAL, XML, JSON, ARRAY, RECORD, MAP, UNION, NIL -> typeKind.getName();
            default -> null;
        };
    }

    /**
     * Get the actual TypeDescKind by resolving type references recursively.
     */
    public static TypeDescKind getActualTypeKind(TypeSymbol typeSymbol) {
        TypeDescKind typeKind = typeSymbol.typeKind();
        // System.err.println("DEBUG: getActualTypeKind: " + typeKind + " for symbol: " + typeSymbol.getName().orElse("anon"));
        if (typeKind == TypeDescKind.TYPE_REFERENCE) {
            if (typeSymbol instanceof io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol typeRef) {
                TypeDescKind resolved = getActualTypeKind(typeRef.typeDescriptor());
                // System.err.println("DEBUG: Resolved TYPE_REFERENCE to: " + resolved);
                return resolved;
            }
        }
        return typeKind;
    }

    /**
     * Get the actual TypeSymbol by resolving type references recursively.
     */
    public static TypeSymbol getActualTypeSymbol(TypeSymbol typeSymbol) {
        if (typeSymbol.typeKind() == TypeDescKind.TYPE_REFERENCE) {
            if (typeSymbol instanceof io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol typeRef) {
                return getActualTypeSymbol(typeRef.typeDescriptor());
            }
        }
        return typeSymbol;
    }

    public static String getReturnTypeName(FunctionSymbol functionSymbol) {
        Optional<TypeSymbol> functionTypeDescKind = functionSymbol.typeDescriptor().returnTypeDescriptor();
        TypeDescKind typeKind = TypeDescKind.NIL;
        if (functionTypeDescKind.isPresent()) {
            typeKind = getActualTypeKind(functionTypeDescKind.get());
        }
        return switch (typeKind) {
            case NIL, BOOLEAN, INT, STRING, FLOAT, DECIMAL, XML, JSON, ANY, ARRAY, MAP, RECORD, UNION -> typeKind.getName();
            default -> null;
        };
    }

    // Commented out: Syntax tree-dependent documentation extraction methods
    // These methods required classes from io.ballerina.compiler.syntax.tree which are not available
//    /**
//     * This is a private utility function for
//     */
//    //TODO
//    public static String getDocFromMetadata(MetadataNode optionalMetadataNode) {
//        StringBuilder doc = new StringBuilder();
//        MarkdownDocumentationNode docLines = optionalMetadataNode.documentationString().isPresent() ?
//                (MarkdownDocumentationNode) optionalMetadataNode.documentationString().get() : null;
//        if (docLines != null) {
//            for (Node docLine : docLines.documentationLines()) {
//                if (docLine instanceof MarkdownDocumentationLineNode) {
//                    doc.append(!((MarkdownDocumentationLineNode) docLine).documentElements().isEmpty() ?
//                            getDocLineString(((MarkdownDocumentationLineNode) docLine).documentElements()) : "\n");
//                } else if (docLine instanceof MarkdownCodeBlockNode) {
//                    doc.append(getDocCodeBlockString((MarkdownCodeBlockNode) docLine));
//                } else {
//                    break;
//                }
//            }
//        }
//
//        return doc.toString();
//    }
//
//    //TODO
//    public static String getDocLineString(NodeList<Node> documentElements) {
//        if (documentElements.isEmpty()) {
//            return null;
//        }
//        StringBuilder doc = new StringBuilder();
//        for (Node docNode : documentElements) {
//            doc.append(docNode.toString());
//        }
//
//        return doc.toString();
//    }
//
//    //TODO
//    public static String getDocCodeBlockString(MarkdownCodeBlockNode markdownCodeBlockNode) {
//        StringBuilder doc = new StringBuilder();
//
//        doc.append(markdownCodeBlockNode.startBacktick().toString());
//        markdownCodeBlockNode.langAttribute().ifPresent(langAttribute -> doc.append(langAttribute.toString()));
//
//        for (MarkdownCodeLineNode codeLineNode : markdownCodeBlockNode.codeLines()) {
//            doc.append(codeLineNode.codeDescription().toString());
//        }
//
//        doc.append(markdownCodeBlockNode.endBacktick().toString());
//        return doc.toString();
//    }

//    private static void processFunctionSymbol(SemanticModel semanticModel, FunctionSymbol functionSymbol, Documentable documentable, int packageId,
//                                              FunctionType functionType, String packageName,
//                                              TypeSymbol errorTypeSymbol, Package resolvedPackage) {
//        //Get function description
//        String description = documentable.documentation().flatMap(Documentation::description).orElse(String.format("Function for %s", functionSymbol.getName()));
//        Map<String, String> documentationMap = functionSymbol.documentation().map(Documentation::parameterMap)
//                .orElse(Map.of());
//
//    }

//    public static List<DataType> getFunctionParameters(SeparatedNodeList<ParameterNode> parameterNodes,
//                                                       Optional<MetadataNode> optionalMetadataNode,
//                                                       SemanticModel semanticModel) {
//        List<DataType> parameters = new ArrayList<>();
//        Optional<DataType> optionalParam;
//        DataType param;
//
//        for (ParameterNode parameterNode : parameterNodes) {
//            switch (parameterNode.kind()) {
//                case REQUIRED_PARAM:
//                    RequiredParameterNode requiredParameterNode = (RequiredParameterNode) parameterNode;
//                    optionalParam = fromSyntaxNode(requiredParameterNode.typeName(), semanticModel);
//                    if (optionalParam.isEmpty()) {
//                        continue;
//                    }
//            }
//        }
//
//        return parameters;
//    }

//    public static Optional<DataType> fromSyntaxNode(Node node, SemanticModel semanticModel) {
//        Optional<DataType> type = Optional.empty();
//        switch (node.kind()) {
//            case SIMPLE_NAME_REFERENCE:
//            case QUALIFIED_NAME_REFERENCE:
//                Optional<Symbol> optSymbol = Optional.empty();
//                try {
//                    optSymbol = semanticModel.symbol(node);
//                } catch (NullPointerException ignored) {
//                }
//                if (optSymbol != null && optSymbol.isPresent()) {
//                    Symbol symbol = optSymbol.get();
//                    type = Optional.of(fromSemanticSymbol(symbol));
//                    clearVisitedTypeMap();
//                }
//                break;
//            case OPTIONAL_TYPE_DESC:
//                OptionalTypeDescriptorNode optionalTypeDescriptorNode = (OptionalTypeDescriptorNode) node;
//                type = fromSyntaxNode(optionalTypeDescriptorNode.typeDescriptor(), semanticModel);
//                if (type.isPresent()) {
//                    Type optionalType = type.get();
//                    optionalType.optional = true;
//                    type = Optional.of(optionalType);
//                }
//                break;
//            case UNION_TYPE_DESC:
//                UnionType unionType = new UnionType();
//                flattenUnionNode(node, semanticModel, unionType.members);
//                type = Optional.of(unionType);
//                break;
//            case INTERSECTION_TYPE_DESC:
//                IntersectionType intersectionType = new IntersectionType();
//                flattenIntersectionNode(node, semanticModel, intersectionType.members);
//                type = Optional.of(intersectionType);
//                break;
//            case ARRAY_TYPE_DESC:
//                ArrayTypeDescriptorNode arrayTypeDescriptorNode = (ArrayTypeDescriptorNode) node;
//                Optional<Type> syntaxNode = fromSyntaxNode(arrayTypeDescriptorNode.memberTypeDesc(), semanticModel);
//                if (syntaxNode.isPresent()) {
//                    type = Optional.of(new ArrayType(syntaxNode.get()));
//                }
//                break;
//            case STREAM_TYPE_DESC:
//                StreamTypeDescriptorNode streamNode = (StreamTypeDescriptorNode) node;
//                StreamTypeParamsNode streamParams = streamNode.streamTypeParamsNode().isPresent() ?
//                        (StreamTypeParamsNode) streamNode.streamTypeParamsNode().get() : null;
//                Optional<Type> leftParam = Optional.empty();
//                Optional<Type> rightParam = Optional.empty();
//                if (streamParams != null) {
//                    leftParam = fromSyntaxNode(streamParams.leftTypeDescNode(), semanticModel);
//                    if (streamParams.rightTypeDescNode().isPresent()) {
//                        rightParam = fromSyntaxNode(streamParams.rightTypeDescNode().get(), semanticModel);
//                    }
//                }
//                type = Optional.of(new StreamType(leftParam, rightParam));
//                break;
//            case RECORD_TYPE_DESC:
//                RecordTypeDescriptorNode recordNode = (RecordTypeDescriptorNode) node;
//                List<Type> fields = new ArrayList<>();
//                recordNode.fields().forEach(node1 -> {
//                    Optional<Type> optionalType = fromSyntaxNode(node1, semanticModel);
//                    optionalType.ifPresent(fields::add);
//                });
//
//                Optional<Type> restType = recordNode.recordRestDescriptor().isPresent() ?
//                        fromSyntaxNode(recordNode.recordRestDescriptor().get().typeName(), semanticModel) :
//                        Optional.empty();
//                type = Optional.of(new RecordType(fields, restType));
//                break;
//            case RECORD_FIELD:
//                RecordFieldNode recordField = (RecordFieldNode) node;
//                type = fromSyntaxNode(recordField.typeName(), semanticModel);
//                if (type.isPresent()) {
//                    Type recordType = type.get();
//                    recordType.name = recordField.fieldName().text();
//                    type = Optional.of(recordType);
//                }
//                break;
//            case MAP_TYPE_DESC:
//                MapTypeDescriptorNode mapNode = (MapTypeDescriptorNode) node;
//                Optional<Type> mapStNode = fromSyntaxNode(mapNode.mapTypeParamsNode().typeNode(), semanticModel);
//                if (mapStNode.isPresent()) {
//                    type = Optional.of(new MapType(mapStNode.get()));
//                }
//                break;
//            case TABLE_TYPE_DESC:
//                TableTypeDescriptorNode tableTypeNode = (TableTypeDescriptorNode) node;
//                Optional<Symbol> optTableTypeSymbol = Optional.empty();
//                TableTypeSymbol tableTypeSymbol = null;
//                List<String> keySpecifiers = null;
//                try {
//                    optTableTypeSymbol = semanticModel.symbol(tableTypeNode);
//                } catch (NullPointerException ignored) {
//                }
//                if (optTableTypeSymbol != null && optTableTypeSymbol.isPresent()) {
//                    tableTypeSymbol = (TableTypeSymbol) optTableTypeSymbol.get();
//                }
//                if (tableTypeSymbol != null) {
//                    keySpecifiers = tableTypeSymbol.keySpecifiers();
//                }
//                if (tableTypeNode.keyConstraintNode().isEmpty()) {
//                    break;
//                }
//                Node keyConstraint = tableTypeNode.keyConstraintNode().get();
//                Optional<Type> tableStNode = fromSyntaxNode(tableTypeNode.rowTypeParameterNode(), semanticModel);
//                Optional<Type> constraintStNode = fromSyntaxNode(keyConstraint, semanticModel);
//                if (tableStNode.isPresent() && constraintStNode.isPresent()) {
//                    type = Optional.of(new TableType(tableStNode.get(), keySpecifiers, constraintStNode.get()));
//                }
//                break;
//            default:
//                if (node instanceof BuiltinSimpleNameReferenceNode builtinSimpleNameReferenceNode) {
//                    type = Optional.of(new PrimitiveType(builtinSimpleNameReferenceNode.name().text()));
//                } else {
//                    type = Optional.of(new PrimitiveType(node.toSourceCode()));
//                }
//                break;
//        }
//
//        return type;
//    }

//    public static DataType fromSemanticSymbol(Symbol symbol) {
//        DataType type = null;
//        if (symbol instanceof TypeReferenceTypeSymbol) {
//            TypeReferenceTypeSymbol typeReferenceTypeSymbol = (TypeReferenceTypeSymbol) symbol;
//            type = getEnumType(typeReferenceTypeSymbol, symbol);
//        } else if (symbol instanceof RecordTypeSymbol) {
//            RecordTypeSymbol recordTypeSymbol = (RecordTypeSymbol) symbol;
//            String typeName = ((BallerinaRecordTypeSymbol) recordTypeSymbol).getBType().toString();
//            VisitedType visitedType = getVisitedType(typeName);
//            if (visitedType != null) {
//                return geAlreadyVisitedType(symbol, typeName, visitedType, false);
//            } else {
//                if (typeName.contains("record {")) {
//                    type = getRecordType(recordTypeSymbol);
//                } else {
//                    visitedTypeMap.put(typeName, new VisitedType());
//                    type = getRecordType(recordTypeSymbol);
//                    completeVisitedTypeEntry(typeName, type);
//                }
//            }
//        } else if (symbol instanceof ArrayTypeSymbol) {
//            ArrayTypeSymbol arrayTypeSymbol = (ArrayTypeSymbol) symbol;
//            type = new ArrayType(fromSemanticSymbol(arrayTypeSymbol.memberTypeDescriptor()));
//        } else if (symbol instanceof MapTypeSymbol) {
//            MapTypeSymbol mapTypeSymbol = (MapTypeSymbol) symbol;
//            type = new MapType(fromSemanticSymbol(mapTypeSymbol.typeParam()));
//        } else if (symbol instanceof TableTypeSymbol) {
//            TableTypeSymbol tableTypeSymbol = (TableTypeSymbol) symbol;
//            TypeSymbol keyConstraint = null;
//            if (tableTypeSymbol.keyConstraintTypeParameter().isPresent()) {
//                keyConstraint = tableTypeSymbol.keyConstraintTypeParameter().get();
//            }
//            type = new TableType(fromSemanticSymbol(tableTypeSymbol.rowTypeParameter()),
//                    tableTypeSymbol.keySpecifiers(), fromSemanticSymbol(keyConstraint));
//        } else if (symbol instanceof UnionTypeSymbol) {
//            UnionTypeSymbol unionSymbol = (UnionTypeSymbol) symbol;
//            String typeName = ((BallerinaUnionTypeSymbol) unionSymbol).getBType().toString();
//            VisitedType visitedType = getVisitedType(typeName);
//            if (visitedType != null) {
//                return geAlreadyVisitedType(symbol, typeName, visitedType, true);
//            } else {
//                visitedTypeMap.put(typeName, new VisitedType());
//                type = getUnionType(unionSymbol);
//                completeVisitedTypeEntry(typeName, type);
//            }
//        } else if (symbol instanceof ErrorTypeSymbol) {
//            ErrorTypeSymbol errSymbol = (ErrorTypeSymbol) symbol;
//            ErrorType errType = new ErrorType();
//            if (errSymbol.detailTypeDescriptor() instanceof TypeReferenceTypeSymbol) {
//                errType.detailType = fromSemanticSymbol(errSymbol.detailTypeDescriptor());
//            }
//            type = errType;
//        } else if (symbol instanceof IntersectionTypeSymbol) {
//            IntersectionTypeSymbol intersectionTypeSymbol = (IntersectionTypeSymbol) symbol;
//            String typeName = ((BallerinaIntersectionTypeSymbol) intersectionTypeSymbol).getBType().toString();
//            VisitedType visitedType = getVisitedType(typeName);
//            if (visitedType != null) {
//                return geAlreadyVisitedType(symbol, typeName, visitedType, false);
//            } else {
//                visitedTypeMap.put(typeName, new VisitedType());
//                type = getIntersectionType(intersectionTypeSymbol);
//                completeVisitedTypeEntry(typeName, type);
//            }
//        } else if (symbol instanceof StreamTypeSymbol) {
//            StreamTypeSymbol streamTypeSymbol = (StreamTypeSymbol) symbol;
//            type = fromSemanticSymbol(streamTypeSymbol.typeParameter());
//        } else if (symbol instanceof ObjectTypeSymbol) {
//            ObjectTypeSymbol objectTypeSymbol = (ObjectTypeSymbol) symbol;
//            ObjectType objectType = new ObjectType();
//            objectTypeSymbol.fieldDescriptors().forEach((typeName, typeSymbol) -> {
//                Type semanticSymbol = fromSemanticSymbol(typeSymbol);
//                if (semanticSymbol != null) {
//                    objectType.fields.add(semanticSymbol);
//                }
//            });
//            objectTypeSymbol.typeInclusions().forEach(typeSymbol -> {
//                Type semanticSymbol = fromSemanticSymbol(typeSymbol);
//                if (semanticSymbol != null) {
//                    objectType.fields.add(new InclusionType(semanticSymbol));
//                }
//            });
//            type = objectType;
//        } else if (symbol instanceof TypeSymbol) {
//            String typeName = ((TypeSymbol) symbol).signature();
//            if (typeName.startsWith("\"") && typeName.endsWith("\"")) {
//                typeName = typeName.substring(1, typeName.length() - 1);
//            }
//            type = new PrimitiveType(typeName);
//        }
//        return type;
//    }
}
