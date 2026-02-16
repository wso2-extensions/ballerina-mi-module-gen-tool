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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import io.ballerina.compiler.api.symbols.*;
import io.ballerina.compiler.syntax.tree.*;
import io.ballerina.mi.model.FunctionType;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Utils {
    private static final Gson PRETTY_GSON = new GsonBuilder()
            .setPrettyPrinting().disableHtmlEscaping().create();

    /**
     * These are utility functions used when generating XML and JSON content
     */
    public static String readFile(String fileName) throws IOException {
        try (InputStream inputStream = Utils.class.getClassLoader()
                .getResourceAsStream(fileName.replace("\\", "/"))) {
            if (inputStream == null) {
                throw new IOException("Resource not found: " + fileName);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * These are private utility functions used in the generateXml method
     */
    public static void writeFile(String fileName, String content) throws IOException {
        // Auto-format JSON files for consistent, readable output
        String outputContent = content;
        if (fileName.endsWith(".json")) {
            outputContent = formatJson(content);
        }
        try (FileWriter myWriter = new FileWriter(fileName)) {
            myWriter.write(outputContent);
        }
    }

    /**
     * Format a JSON string with consistent 2-space indentation.
     * This ensures all generated JSON files have proper, readable formatting.
     *
     * @param json The JSON string to format (may have inconsistent indentation)
     * @return The formatted JSON string with consistent 2-space indentation
     */
    public static String formatJson(String json) {
        try {
            JsonElement jsonElement = JsonParser.parseString(json);
            return PRETTY_GSON.toJson(jsonElement);
        } catch (Exception e) {
            // If parsing fails, return the original content
            return json;
        }
    }

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
        // Prefer the native 'zip' command — it runs in a separate process and avoids
        // exhausting the JVM's memory when the Ballerina compiler/runtime has already
        // consumed most of the system's physical memory via off-heap allocations
        // (Netty direct buffers, NIO channels, etc.).
        if (tryNativeZip(sourceDirPath, zipFilePath)) {
            return;
        }
        // Fallback: Java-based ZIP
        zipFolderJava(sourceDirPath, zipFilePath);
    }

    private static boolean tryNativeZip(Path sourceDirPath, String zipFilePath) throws IOException {
        try {
            Path zipPath = Paths.get(zipFilePath).toAbsolutePath();
            // Delete existing zip if present so 'zip' doesn't append
            Files.deleteIfExists(zipPath);

            ProcessBuilder pb = new ProcessBuilder(
                    "zip", "-r", "-q", zipPath.toString(), "."
            );
            pb.directory(sourceDirPath.toFile());
            pb.redirectErrorStream(true);
            Process process = pb.start();
            // Drain output to prevent process blocking
            try (InputStream is = process.getInputStream()) {
                is.readAllBytes();
            }
            int exitCode = process.waitFor();
            if (exitCode == 0 && Files.exists(zipPath)) {
                return true;
            }
            System.out.println("Native zip exited with code " + exitCode + ", falling back to Java ZIP.");
            return false;
        } catch (Exception e) {
            // 'zip' not available or failed — fall back to Java
            return false;
        }
    }

    private static void zipFolderJava(Path sourceDirPath, String zipFilePath) throws IOException {
        try (ZipOutputStream outputStream = new ZipOutputStream(Files.newOutputStream(Paths.get(zipFilePath)))) {
            Files.walkFileTree(sourceDirPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    String fileName = file.getFileName().toString();
                    if (shouldSkipFile(fileName)) {
                        return FileVisitResult.CONTINUE;
                    }

                    Path targetFile = sourceDirPath.relativize(file);
                    outputStream.putNextEntry(new ZipEntry(targetFile.toString()));
                    Files.copy(file, outputStream);
                    outputStream.closeEntry();
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    String dirName = dir.getFileName().toString();
                    if (shouldSkipDirectory(dirName)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }

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
     * Check if a file should be skipped during ZIP creation.
     * Skips macOS-specific files that cause extraction issues in MI.
     *
     * @param fileName The name of the file to check
     * @return true if the file should be skipped, false otherwise
     */
    private static boolean shouldSkipFile(String fileName) {
        // Skip .DS_Store files (macOS Finder metadata)
        if (".DS_Store".equals(fileName)) {
            return true;
        }
        // Skip AppleDouble resource fork files (._* files)
        if (fileName.startsWith("._")) {
            return true;
        }
        return false;
    }

    /**
     * Check if a directory should be skipped during ZIP creation.
     * Skips macOS-specific directories that cause extraction issues in MI.
     *
     * @param dirName The name of the directory to check
     * @return true if the directory should be skipped, false otherwise
     */
    private static boolean shouldSkipDirectory(String dirName) {
        // Skip __MACOSX directory (created by macOS archiver)
        if ("__MACOSX".equals(dirName)) {
            return true;
        }
        return false;
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

    public static String getDocString(Documentation documentation) {
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
            case SINGLETON -> Constants.STRING;  // Singleton types (e.g., "1", "2" in enums) are treated as strings
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

    /**
     * Extract operationId from @openapi:ResourceInfo annotation if present.
     * 
     * This method checks for the @openapi:ResourceInfo annotation and extracts the operationId field value
     * using the syntax tree API to properly parse annotation values.
     * 
     * @param functionSymbol The function symbol (MethodSymbol or FunctionSymbol) to check for annotations
     * @param module The module containing the function (used to access syntax trees)
     * @param semanticModel The semantic model to match function symbols to syntax tree nodes
     * @return Optional containing the operationId value if found, empty otherwise
     */
    public static Optional<String> getOpenApiOperationId(FunctionSymbol functionSymbol, io.ballerina.projects.Module module, 
                                                         io.ballerina.compiler.api.SemanticModel semanticModel) {
        // First check if the annotation exists
        if (!hasOpenApiResourceInfoAnnotation(functionSymbol)) {
            return Optional.empty();
        }
        
        // Extract operationId using syntax tree
        try {
            // Search through module documents for the function
            for (io.ballerina.projects.DocumentId docId : module.documentIds()) {
                io.ballerina.projects.Document document = module.document(docId);
                SyntaxTree syntaxTree = document.syntaxTree();
                
                Optional<String> operationId = extractOperationIdFromSyntaxTree(syntaxTree, functionSymbol, semanticModel);
                if (operationId.isPresent()) {
                    return operationId;
                }
            }
        } catch (Exception e) {
            // If syntax tree access fails, return empty
        }
        
        return Optional.empty();
    }
    
    /**
     * Extract operationId from syntax tree by finding the function and its @openapi:ResourceInfo annotation.
     * Uses the reference pattern from MetaInfoMapperImpl to properly extract annotation field values.
     * Matches function nodes to the specific function symbol using SemanticModel.
     */
    private static Optional<String> extractOperationIdFromSyntaxTree(SyntaxTree syntaxTree, FunctionSymbol targetFunctionSymbol,
                                                                     io.ballerina.compiler.api.SemanticModel semanticModel) {
        Node rootNode = syntaxTree.rootNode();
        
        // Find all function definitions in the syntax tree and match to our target symbol
        FunctionDefinitionNodeVisitor visitor = new FunctionDefinitionNodeVisitor(targetFunctionSymbol, semanticModel);
        rootNode.accept(visitor);
        
        return visitor.getOperationId();
    }
    
    /**
     * Visitor class to find function definition nodes and extract operationId from annotations.
     * Follows the pattern from MetaInfoMapperImpl for extracting annotation field values.
     * Uses SemanticModel to match function definition nodes to the target function symbol.
     */
    private static class FunctionDefinitionNodeVisitor extends NodeVisitor {
        private final FunctionSymbol targetFunctionSymbol;
        private final io.ballerina.compiler.api.SemanticModel semanticModel;
        private Optional<String> operationId = Optional.empty();
        
        public FunctionDefinitionNodeVisitor(FunctionSymbol targetFunctionSymbol, 
                                              io.ballerina.compiler.api.SemanticModel semanticModel) {
            this.targetFunctionSymbol = targetFunctionSymbol;
            this.semanticModel = semanticModel;
        }
        
        @Override
        public void visit(FunctionDefinitionNode functionDefinitionNode) {
            // Skip if we already found the operationId
            if (operationId.isPresent()) {
                return;
            }
            
            // Use SemanticModel to get the symbol for this function definition node
            // and check if it matches our target function symbol
            Optional<io.ballerina.compiler.api.symbols.Symbol> nodeSymbolOpt = semanticModel.symbol(functionDefinitionNode);
            if (nodeSymbolOpt.isEmpty() || !nodeSymbolOpt.get().equals(targetFunctionSymbol)) {
                // Continue visiting child nodes
                visitSyntaxNode(functionDefinitionNode);
                return;
            }
            
            // This is our target function - extract operationId from annotations
            // Check metadata for annotations - following MetaInfoMapperImpl pattern
            Optional<MetadataNode> optMetadata = functionDefinitionNode.metadata();
            if (optMetadata.isPresent()) {
                MetadataNode metadataNode = optMetadata.get();
                NodeList<AnnotationNode> annotations = metadataNode.annotations();
                
                // Look for @openapi:ResourceInfo annotation
                for (AnnotationNode annotation : annotations) {
                    if (annotation.annotReference().kind() == SyntaxKind.QUALIFIED_NAME_REFERENCE) {
                        QualifiedNameReferenceNode ref = (QualifiedNameReferenceNode) annotation.annotReference();
                        String annotationName = ref.modulePrefix().text() + ":" + ref.identifier().text();
                        
                        if ("openapi:ResourceInfo".equals(annotationName)) {
                            Optional<MappingConstructorExpressionNode> optExpressionNode = annotation.annotValue();
                            if (optExpressionNode.isPresent()) {
                                MappingConstructorExpressionNode mappingConstructorExpressionNode = optExpressionNode.get();
                                SeparatedNodeList<MappingFieldNode> fields = mappingConstructorExpressionNode.fields();
                                
                                // Extract operationId field value - following MetaInfoMapperImpl pattern
                                for (MappingFieldNode field : fields) {
                                    if (field instanceof SpecificFieldNode specificField) {
                                        String fieldName = specificField.fieldName().toSourceCode().trim().replaceAll("\"", "");
                                        
                                        if ("operationId".equals(fieldName)) {
                                            Optional<ExpressionNode> valueExpr = specificField.valueExpr();
                                            if (valueExpr.isPresent()) {
                                                ExpressionNode expressionNode = valueExpr.get();
                                                String expressionStr = expressionNode.toSourceCode().trim();
                                                
                                                // Extract string value - remove quotes
                                                if (!expressionStr.isBlank()) {
                                                    String fieldValue = expressionStr.replaceAll("\"", "").trim();
                                                    if (!fieldValue.isEmpty()) {
                                                        operationId = Optional.of(fieldValue);
                                                        return; // Found operationId, stop searching
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // Continue visiting child nodes
            visitSyntaxNode(functionDefinitionNode);
        }
        
        public Optional<String> getOperationId() {
            return operationId;
        }
    }
    
    /**
     * Check if a function has @openapi:ResourceInfo annotation.
     * 
     * @param functionSymbol The function symbol to check
     * @return true if the annotation is present, false otherwise
     */
    public static boolean hasOpenApiResourceInfoAnnotation(FunctionSymbol functionSymbol) {
        List<AnnotationSymbol> annotations = functionSymbol.annotations();
        for (AnnotationSymbol annotationSymbol : annotations) {
            Optional<String> annotationName = annotationSymbol.getName();
            if (annotationName.isEmpty()) {
                continue;
            }
            
            if (!annotationName.get().equals("ResourceInfo")) {
                continue;
            }
            
            Optional<ModuleSymbol> moduleOpt = annotationSymbol.getModule();
            if (moduleOpt.isEmpty()) {
                continue;
            }
            
            Optional<String> moduleNameOpt = moduleOpt.get().getName();
            if (moduleNameOpt.isPresent() && moduleNameOpt.get().equals("openapi")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Generate synapse name for resource functions when a path-based resource hint is available.
     * Falls back to the default generation if no path hint is provided.
     */
    public static String generateSynapseName(FunctionSymbol functionSymbol, FunctionType functionType,
                                             String pathResourceHint) {
        if (functionType != FunctionType.RESOURCE || pathResourceHint == null || pathResourceHint.isBlank()) {
            return generateSynapseName(functionSymbol, functionType);
        }

        String httpMethod = functionSymbol.getName().orElse("resource").toLowerCase();
        String pascalHint = toPascalCase(pathResourceHint);
        return sanitizeXmlName(httpMethod + pascalHint);
    }

      /**
     * Convert a technical name like "getAllCountriesAndProvincesStatus" or
     * "postUsersThreadsTrashByUserIdById" into a human-friendly phrase:
     * "get all countries and provinces status", "post users threads trash by user id by id".
     */
    public static String humanizeName(String name) {
        if (name == null || name.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        char prev = 0;

        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == '_' || c == '-' || c == ' ') {
                // Normalize any separators to a single space
                if (sb.length() > 0 && sb.charAt(sb.length() - 1) != ' ') {
                    sb.append(' ');
                }
            } else if (Character.isUpperCase(c)) {
                // Insert a space before an upper-case letter when following a lower-case letter or digit
                if (i > 0 && (Character.isLowerCase(prev) || Character.isDigit(prev)) && sb.charAt(sb.length() - 1) != ' ') {
                    sb.append(' ');
                }
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
            prev = c;
        }

        // Collapse multiple spaces and trim
        return sb.toString().trim().replaceAll("\\s+", " ");
    }

    /**
     * Generate synapse name for functions.
     * <ul>
     *   <li>Non-resource functions: use the function name directly.</li>
     *   <li>Resource functions: derive a meaningful name using HTTP method + resource hint from parameters
     *       (e.g., queries/requests types) and fall back to &lt;method&gt;Resource.</li>
     * </ul>
     *
     * @param functionSymbol The function symbol (MethodSymbol or FunctionSymbol)
     * @param functionType   The type of function (RESOURCE, REMOTE, FUNCTION, etc.)
     * @return Generated synapse name
     */
    public static String generateSynapseName(FunctionSymbol functionSymbol, FunctionType functionType) {
        if (functionType != FunctionType.RESOURCE) {
            // For non-resource functions, use the function name directly
            return functionSymbol.getName().orElse("unknown");
        }

        // For resource functions in client connectors, the symbol name is typically the HTTP method ("get", "post", ...)
        String httpMethod = functionSymbol.getName().orElse("resource").toLowerCase(Locale.ROOT);

        // Try to infer a resource name from parameter types (e.g., ListAssistantsQueries -> UsersThreadsTrash)
        String resourceHint = inferResourceNameFromParams(functionSymbol);
        // If params don't give a good hint, fall back to the return type
        if (resourceHint == null || resourceHint.isEmpty()) {
            resourceHint = inferResourceNameFromReturnType(functionSymbol);
        }
        if (resourceHint != null && !resourceHint.isEmpty()) {
            String pascalHint = toPascalCase(resourceHint);
            return sanitizeXmlName(httpMethod + pascalHint);
        }

        // Fallback: use HTTP method with "Resource" suffix
        return sanitizeXmlName(httpMethod + "Resource");
    }

    /**
     * Try to infer a meaningful resource name using parameter types of a resource function.
     * <p>
     * Heuristics (tuned for connector-style clients):
     * <ul>
     *   <li>Look for parameters whose type names end with common suffixes such as
     *       "Queries", "Query", "Request", "Response", "Params", "Options".</li>
     *   <li>Strip the suffix and derive a combined CamelCase resource name from the remaining tokens,
     *       e.g., "GmailUsersThreadsTrashQueries" -> "UsersThreadsTrash", "ListAssistantsQueries" -> "Assistants".</li>
     *   <li>If nothing meaningful is found, return an empty string.</li>
     * </ul>
     */
    private static String inferResourceNameFromParams(FunctionSymbol functionSymbol) {
        Optional<List<ParameterSymbol>> paramsOpt = functionSymbol.typeDescriptor().params();
        if (paramsOpt.isEmpty()) {
            return "";
        }

        List<ParameterSymbol> params = paramsOpt.get();
        for (ParameterSymbol parameterSymbol : params) {
            TypeSymbol typeSymbol = getActualTypeSymbol(parameterSymbol.typeDescriptor());
            // Only consider named types; avoid using full signatures which can be extremely long
            Optional<String> optTypeName = typeSymbol.getName();
            if (optTypeName.isEmpty()) {
                continue;
            }
            String typeName = optTypeName.get();

            String base = stripCommonTypeSuffixes(typeName);
            if (base.isEmpty()) {
                continue;
            }

            String resourceName = buildResourceNameFromCamelTokens(base);
            if (!resourceName.isEmpty()) {
                return resourceName;
            }
        }

        return "";
    }

    /**
     * Infer a resource name from the return type of a resource function.
     * <p>
     * This is especially useful for DELETE-like operations where there may be only headers
     * and no rich payload/query types to infer from. For example:
     * <ul>
     *   <li>return type "DeleteModelResponse" -> "Model"</li>
     *   <li>return type "MailThread" -> "MailThread"</li>
     * </ul>
     */
    private static String inferResourceNameFromReturnType(FunctionSymbol functionSymbol) {
        Optional<TypeSymbol> retOpt = functionSymbol.typeDescriptor().returnTypeDescriptor();
        if (retOpt.isEmpty()) {
            return "";
        }

        TypeSymbol retSymbol = getActualTypeSymbol(retOpt.get());

        // If union (e.g., T|error), pick the non-error member
        if (retSymbol.typeKind() == TypeDescKind.UNION && retSymbol instanceof UnionTypeSymbol unionTypeSymbol) {
            for (TypeSymbol member : unionTypeSymbol.memberTypeDescriptors()) {
                if (member.typeKind() != TypeDescKind.ERROR) {
                    retSymbol = getActualTypeSymbol(member);
                    break;
                }
            }
        }

        Optional<String> optName = retSymbol.getName();
        if (optName.isEmpty()) {
            return "";
        }
        String typeName = optName.get();

        String base = stripCommonTypeSuffixes(typeName);
        if (base.isEmpty()) {
            return "";
        }

        return buildResourceNameFromCamelTokens(base);
    }

    /**
     * Strip well-known trailing suffixes from type names to get a more generic base.
     * Example: "ListAssistantsQueries" -> "ListAssistants".
     */
    private static String stripCommonTypeSuffixes(String typeName) {
        String[] suffixes = {"Queries", "Query", "Request", "Response", "Params", "Options", "Payload"};
        for (String suffix : suffixes) {
            if (typeName.endsWith(suffix) && typeName.length() > suffix.length()) {
                return typeName.substring(0, typeName.length() - suffix.length());
            }
        }
        return typeName;
    }

    /**
     * Build a resource name from CamelCase tokens in a type name.
     * <p>
     * Examples:
     * <ul>
     *   <li>"ListAssistants" -> "Assistants"</li>
     *   <li>"GmailUsersThreadsTrash" -> "UsersThreadsTrash"</li>
     *   <li>"GmailUsersDraftsList" -> "UsersDrafts"</li>
     * </ul>
     */
    private static String buildResourceNameFromCamelTokens(String str) {
        if (str == null || str.isEmpty()) {
            return "";
        }

        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        char[] chars = str.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (i > 0 && Character.isUpperCase(c)
                    && (Character.isLowerCase(chars[i - 1]) || Character.isDigit(chars[i - 1]))) {
                tokens.add(current.toString());
                current.setLength(0);
            }
            current.append(c);
        }
        if (current.length() > 0) {
            tokens.add(current.toString());
        }

        if (tokens.isEmpty()) {
            return "";
        }

        // Heuristic:
        // - If 3+ tokens, drop the first one (often org/module prefix like "Gmail", "OpenAI", "List").
        // - If 2 tokens and the first is a generic prefix (List, Get, Create, Update, Delete),
        //   drop the first.
        // - Drop a trailing generic token (List, Collection, Items) if present.
        List<String> working = new ArrayList<>(tokens);

        if (working.size() >= 3) {
            working = working.subList(1, working.size());
        } else if (working.size() == 2 && isGenericPrefix(working.get(0))) {
            working = working.subList(1, working.size());
        }

        if (working.size() > 1 && isGenericSuffix(working.get(working.size() - 1))) {
            working = working.subList(0, working.size() - 1);
        }

        if (working.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (String t : working) {
            if (t.isEmpty()) {
                continue;
            }
            sb.append(t);
        }
        return sb.toString();
    }

    private static boolean isGenericPrefix(String token) {
        String lower = token.toLowerCase(Locale.ROOT);
        return lower.equals("list") || lower.equals("get") || lower.equals("create")
                || lower.equals("update") || lower.equals("delete");
    }

    private static boolean isGenericSuffix(String token) {
        String lower = token.toLowerCase(Locale.ROOT);
        return lower.equals("list") || lower.equals("collection") || lower.equals("items");
    }

    /**
 * Convert a string to PascalCase.
 * Example: "user-profiles" -> "UserProfiles", "userId" -> "UserId"
 */
/**
 * Sanitizes a parameter name by removing leading single quotes from quoted identifiers
 * and replacing dots with underscores for Synapse compatibility.
 * 
 * <p>In Ballerina, a single quote (') is used before a variable or identifier name to create
 * a quoted identifier. This allows using reserved keywords (like if, while, string, int) as
 * names for variables, functions, or fields. For example, 'start is a valid quoted identifier.</p>
 * 
 * <p>However, when generating XML and JSON files for MI connectors, these quoted identifiers
 * can cause issues:
 * <ul>
 *   <li>XML attribute names cannot start with a single quote (e.g., name="'start" is invalid)</li>
 *   <li>JSON property names with leading quotes may cause parsing issues</li>
 *   <li>Synapse template parameters with dots are not correctly stored/retrieved (e.g., auth.token fails)</li>
 * </ul>
 * </p>
 * 
 * <p>This method sanitizes parameter names by:
 * <ol>
 *   <li>Removing the leading quote(s) for use in XML/JSON generation</li>
 *   <li>Replacing dots with underscores for Synapse template parameter compatibility</li>
 * </ol>
 * The original name is preserved for display purposes.</p>
 *
 * @param paramName the parameter name to sanitize (may be a quoted identifier like 'start or contain dots like auth.token)
 * @return the sanitized parameter name with leading quotes removed and dots replaced with underscores
 */
public static String sanitizeParamName(String paramName) {
    if (paramName == null) {
        return "";
    }
    String sanitized = paramName;
    // Remove leading single quotes from quoted identifiers
    // In Ballerina, 'start is a quoted identifier, we need 'start -> start for XML/JSON
    while (sanitized.startsWith("'")) {
        sanitized = sanitized.substring(1);
    }
    // Replace dots with underscores for Synapse template parameter compatibility
    // Synapse/MI cannot correctly handle parameter names with dots (e.g., auth.token)
    sanitized = sanitized.replace(".", "_");
    // If the name is empty after sanitization, use a default
    if (sanitized.isEmpty()) {
        sanitized = "param";
    }
    return sanitized;
}

    private static String toPascalCase(String str) {
        if (str == null || str.isEmpty()) {
            return "";
        }

        // Handle hyphenated or underscore-separated words
        String[] parts = str.split("[-_\\s]+");
        StringBuilder result = new StringBuilder();

        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            // Capitalize first letter, lowercase rest
            result.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                result.append(part.substring(1).toLowerCase());
            }
        }

        return result.toString();
    }

    /**
     * Sanitize a string to be a valid XML name.
     * XML names must start with a letter or underscore, and contain only letters, digits, underscores, hyphens, and periods.
     */
    public static String sanitizeXmlName(String name) {
        if (name == null || name.isEmpty()) {
            return "resource";
        }

        StringBuilder sanitized = new StringBuilder();
        boolean firstChar = true;

        for (char c : name.toCharArray()) {
            if (firstChar) {
                // First character must be a letter or underscore
                if (Character.isLetter(c) || c == '_') {
                    sanitized.append(c);
                    firstChar = false;
                } else if (Character.isDigit(c)) {
                    // If starts with digit, prefix with underscore
                    sanitized.append('_').append(c);
                    firstChar = false;
                }
                // Skip invalid first characters
            } else {
                // Subsequent characters can be letters, digits, underscores, hyphens, periods
                if (Character.isLetterOrDigit(c) || c == '_' || c == '-' || c == '.') {
                    sanitized.append(c);
                } else {
                    // Replace invalid characters with underscore
                    sanitized.append('_');
                }
            }
        }

        // Ensure result is not empty
        if (sanitized.length() == 0) {
            return "resource";
        }

        return sanitized.toString();
    }
}

