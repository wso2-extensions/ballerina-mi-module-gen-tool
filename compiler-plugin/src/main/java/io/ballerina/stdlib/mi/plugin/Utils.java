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

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.*;
import io.ballerina.compiler.syntax.tree.*;
import io.ballerina.stdlib.mi.plugin.connectorModel.DataType;
import io.ballerina.stdlib.mi.plugin.model.Connector;
import io.ballerina.stdlib.mi.plugin.model.ModelElement;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
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
     * These are private utility functions used in the generateXml method
     */
    private static String readFile(String fileName) throws IOException {
        InputStream inputStream = Utils.class.getClassLoader().getResourceAsStream(fileName);
        assert inputStream != null;
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        StringBuilder xmlContent = new StringBuilder();
        int character;
        while ((character = reader.read()) != -1) {
            xmlContent.append((char) character);
        }
        reader.close();
        return xmlContent.toString();
    }

    /**
     * These are private utility functions used in the generateXml method
     */
    private static void writeFile(String fileName, String content) throws IOException {
        FileWriter myWriter = new FileWriter(fileName);
        myWriter.write(content);
        myWriter.close();
    }

    /**
     * Generate file (XML/JSON) using the provided template and model element.
     *
     * @param templateName Name of the template file
     * @param outputName   Name of the output file
     * @param element      Model element(connector/component) to be used in the template
     * @param extension    Extension of the file to be generated (e.g., "xml" or "json")
     * @Note: This method generates the files needed for the connector, which uses the ReadXml and WriteXml methods.
     */
    private static void generateFile(String templateName, String outputName, ModelElement element, String extension) {
        try {
            Handlebars handlebar = new Handlebars();
            String templateFileName = String.format("%s.%s", templateName, extension);
            String content = readFile(templateFileName);
            Template template = handlebar.compileInline(content);
            String output = template.apply(element);

            String outputFileName = String.format("%s.%s", outputName, extension);
            writeFile(outputFileName, output);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void generateFileForConnector(String templatePath, String templateName, String outputName, io.ballerina.stdlib.mi.plugin.connectorModel.ModelElement element, String extension) {
        try {
            Handlebars handlebar = new Handlebars();
            handlebar.registerHelper("eq", (context, options) -> context != null && context.equals(options.param(0)));
            String templateFileName = String.format("%s/%s.%s", templatePath, templateName, extension);
            String content = readFile(templateFileName);
            Template template = handlebar.compileInline(content);
            //TODO: Apply description in connector template
            // TODO: Apply description and display name for function component xml
            String output = template.apply(element);

            String outputFileName = String.format("%s.%s", outputName, extension);
            writeFile(outputFileName, output);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Existing methods can now call the new generic method
    public static void generateXml(String templateName, String outputName, ModelElement element) {
        generateFile(templateName, outputName, element, "xml");
    }

    public static void generateJson(String templateName, String outputName, ModelElement element) {
        generateFile(templateName, outputName, element, "json");
    }

    public static void generateXmlForConnector(String templatePath, String templateName, String outputName, io.ballerina.stdlib.mi.plugin.connectorModel.ModelElement element) {
        generateFileForConnector(templatePath, templateName, outputName, element, "xml");
    }

    public static void generateJsonForConnector(String templatePath, String templateName, String outputName, io.ballerina.stdlib.mi.plugin.connectorModel.ModelElement element) {
        generateFileForConnector(templatePath, templateName, outputName, element, "json");
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

    /**
     * Copy resources from the JAR file to the destination directory.
     *
     * @param classLoader Class loader to load resources
     * @param destination Destination directory
     * @param jarPath     Path to the JAR file
     * @throws IOException        If an I/O error occurs
     * @throws URISyntaxException If the URI is invalid
     * @Note : This method is used to copy the resources(icons,jar files, mediator jar) to the Constants.CONNECTOR
     * directory
     */
    public static void copyResources(ClassLoader classLoader, Path destination, URI jarPath, String org,
                                     String module, String moduleVersion)
            throws IOException, URISyntaxException {
        URI uri = URI.create("jar:" + jarPath.toString());
        try (FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap())) {
            copyMediatorClasses(classLoader, fs, destination, org, module, moduleVersion);
            copyIcons(classLoader, fs, destination);
            copyResources(classLoader, fs, destination, Connector.LIB_PATH, ".jar");
        }
    }

    /**
     * This is mediator class copy private utility method
     */
    //TODO: check and change this method and other related methods
    private static void copyMediatorClasses(ClassLoader classLoader, FileSystem fs, Path destination, String org,
                                            String module, String moduleVersion)
            throws IOException {
        List<Path> paths = Files.walk(fs.getPath("mediator-classes"))
                .filter(f -> f.toString().contains(".class"))
                .toList();

        for (Path path : paths) {
            Path relativePath = fs.getPath("mediator-classes").relativize(path);
            Path outputPath = destination.resolve(relativePath.toString());
            Files.createDirectories(outputPath.getParent()); // Create parent directories if they don't exist
            InputStream inputStream = getFileFromResourceAsStream(classLoader, path.toString());
            if (path.getFileName().toString().contains("ModuleInfo.class")) {
                updateConstants(inputStream, outputPath.toString(), org, module, moduleVersion);
            } else {
                Files.copy(inputStream, outputPath);
            }
            inputStream.close();
        }
    }

    private static void updateConstants(InputStream inputStream, String outputPath, String org, String module,
                                        String moduleVersion) throws IOException {
        ClassReader classReader = new ClassReader(inputStream.readAllBytes());
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        ClassVisitor classVisitor = new ClassVisitor(Opcodes.ASM7, classWriter) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                                             String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                return new MethodVisitor(Opcodes.ASM7, mv) {
                    @Override
                    public void visitLdcInsn(Object value) {
                        if ("BALLERINA_ORG_NAME".equals(value)) {
                            super.visitLdcInsn(org);
                        } else if ("BALLERINA_MODULE_NAME".equals(value)) {
                            super.visitLdcInsn(module);
                        } else if ("BALLERINA_MODULE_VERSION".equals(value)) {
                            super.visitLdcInsn(moduleVersion);
                        } else {
                            super.visitLdcInsn(value);
                        }
                    }
                };
            }
        };

        classReader.accept(classVisitor, ClassReader.SKIP_DEBUG);
        byte[] modifiedBytecode = classWriter.toByteArray();

        FileOutputStream fos = new FileOutputStream(outputPath);
        fos.write(modifiedBytecode);
        fos.close();
    }

    /**
     * This is a private utility method to copy icons
     */
    private static void copyIcons(ClassLoader classLoader, FileSystem fs, Path destination) throws IOException {
        Connector connector = Connector.getConnector();
        if (connector.getIconPath() == null) {
            copyResources(classLoader, fs, destination, Connector.ICON_FOLDER, ".png");
            return;
        }

        Path iconPath = destination.getParent().resolve(connector.getIconPath()).normalize();
        if (!Files.exists(iconPath)) {
            copyResources(classLoader, fs, destination, Connector.ICON_FOLDER, ".png");
            return;
        }

        List<Path> paths = Files.walk(iconPath)
                .filter(f -> f.toString().contains(".png"))
                .toList();

        if (paths.size() != 2) {
            copyResources(classLoader, fs, destination, Connector.ICON_FOLDER, ".png");
            return;
        }
        copyIcons(destination, paths);
    }

    /**
     * This is a private utility method to copy icons with separating the small and large icons
     */
    private static void copyIcons(Path destination, List<Path> paths) throws IOException {
        Path smallOutputPath = destination.resolve(Connector.ICON_FOLDER).resolve(Connector.SMALL_ICON_NAME);
        Path largeOutputPath = destination.resolve(Connector.ICON_FOLDER).resolve(Connector.LARGE_ICON_NAME);
        Path smallIconPath;
        Path largeIconPath;
        Files.createDirectories(smallOutputPath.getParent());
        if (Files.size(paths.get(0)) > Files.size(paths.get(1))) {
            smallIconPath = paths.get(1);
            largeIconPath = paths.get(0);
        } else {
            smallIconPath = paths.get(0);
            largeIconPath = paths.get(1);
        }

        copyIconToDestination(smallIconPath, smallOutputPath);
        copyIconToDestination(largeIconPath, largeOutputPath);
    }

    /**
     * This is a private utility method to copy png when input and output path given
     */
    private static void copyIconToDestination(Path iconPath, Path destination) throws IOException {
        InputStream inputStream = Files.newInputStream(iconPath);
        Files.copy(inputStream, destination);
    }

    /**
     * This is mediator class copy private utility method
     */
    private static void copyResources(ClassLoader classLoader, FileSystem fs, Path destination, String resourceFolder,
                                      String fileExtension) throws IOException {
        List<Path> paths = Files.walk(fs.getPath(resourceFolder))
                .filter(f -> f.toString().contains(fileExtension))
                .toList();
        for (Path path : paths) {
            copyResource(classLoader, path, destination);
        }
    }

    /**
     * This is a private utility method without the specific file extension
     */
    private static void copyResource(ClassLoader classLoader, Path path, Path destination) throws IOException {
        Path outputPath = destination.resolve(path.toString());
        Files.createDirectories(outputPath.getParent());
        InputStream inputStream = getFileFromResourceAsStream(classLoader, path.toString());
        Files.copy(inputStream, outputPath);
    }

    /**
     * These are private utility functions used in the moveResources method
     */
    private static InputStream getFileFromResourceAsStream(ClassLoader classLoader, String fileName) {
        InputStream inputStream = classLoader.getResourceAsStream(fileName);
        if (inputStream == null) {
            throw new IllegalArgumentException("file not found " + fileName);
        } else {
            return inputStream;
        }
    }

    /**
     * This is a private utility function for
     */
    //TODO
    public static String getDocFromMetadata(MetadataNode optionalMetadataNode) {
        StringBuilder doc = new StringBuilder();
        MarkdownDocumentationNode docLines = optionalMetadataNode.documentationString().isPresent() ?
                (MarkdownDocumentationNode) optionalMetadataNode.documentationString().get() : null;
        if (docLines != null) {
            for (Node docLine : docLines.documentationLines()) {
                if (docLine instanceof MarkdownDocumentationLineNode) {
                    doc.append(!((MarkdownDocumentationLineNode) docLine).documentElements().isEmpty() ?
                            getDocLineString(((MarkdownDocumentationLineNode) docLine).documentElements()) : "\n");
                } else if (docLine instanceof MarkdownCodeBlockNode) {
                    doc.append(getDocCodeBlockString((MarkdownCodeBlockNode) docLine));
                } else {
                    break;
                }
            }
        }

        return doc.toString();
    }

    //TODO
    public static String getDocLineString(NodeList<Node> documentElements) {
        if (documentElements.isEmpty()) {
            return null;
        }
        StringBuilder doc = new StringBuilder();
        for (Node docNode : documentElements) {
            doc.append(docNode.toString());
        }

        return doc.toString();
    }

    //TODO
    public static String getDocCodeBlockString(MarkdownCodeBlockNode markdownCodeBlockNode) {
        StringBuilder doc = new StringBuilder();

        doc.append(markdownCodeBlockNode.startBacktick().toString());
        markdownCodeBlockNode.langAttribute().ifPresent(langAttribute -> doc.append(langAttribute.toString()));

        for (MarkdownCodeLineNode codeLineNode : markdownCodeBlockNode.codeLines()) {
            doc.append(codeLineNode.codeDescription().toString());
        }

        doc.append(markdownCodeBlockNode.endBacktick().toString());
        return doc.toString();
    }

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
