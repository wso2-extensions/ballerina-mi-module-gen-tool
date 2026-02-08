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

package io.ballerina.mi;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import io.ballerina.mi.connectorModel.*;
import io.ballerina.mi.connectorModel.attributeModel.Attribute;
import io.ballerina.mi.connectorModel.attributeModel.AttributeGroup;
import io.ballerina.mi.connectorModel.attributeModel.Combo;
import io.ballerina.mi.connectorModel.attributeModel.Element;
import io.ballerina.mi.connectorModel.attributeModel.Table;
import io.ballerina.mi.util.Constants;
import io.ballerina.mi.util.JsonTemplateBuilder;
import io.ballerina.mi.util.Utils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import io.ballerina.compiler.api.symbols.ArrayTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import org.ballerinalang.diagramutil.connector.models.connector.Type;
import org.ballerinalang.diagramutil.connector.models.connector.types.PathParamType;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static io.ballerina.mi.util.Constants.*;
import static io.ballerina.mi.util.Constants.RECORD;

public class ConnectorSerializer {

    private static final String CONFIG_TEMPLATE_PATH = "balConnector" + File.separator + "config";
    private static final String FUNCTION_TEMPLATE_PATH = "balConnector" + File.separator + "functions";

    // Cached Handlebars instances and compiled templates to avoid OOM on large connectors
    private static Handlebars cachedConnectorHandlebars;
    private static final Map<String, Template> connectorTemplateCache = new ConcurrentHashMap<>();
    private static Handlebars cachedSimpleHandlebars;
    private static final Map<String, Template> simpleTemplateCache = new ConcurrentHashMap<>();

    private final PrintStream printStream;
    private final Path sourcePath;
    private final Path targetPath;

    public ConnectorSerializer(Path sourcePath, Path targetPath) {
        this.sourcePath = sourcePath;
        this.targetPath = targetPath;
        this.printStream = System.out;
    }

    private static synchronized Handlebars getConnectorHandlebars() {
        if (cachedConnectorHandlebars != null) return cachedConnectorHandlebars;
        cachedConnectorHandlebars = new Handlebars();
        registerConnectorHelpers(cachedConnectorHandlebars);
        return cachedConnectorHandlebars;
    }

    private static Template getConnectorTemplate(String templateFilePath) throws IOException {
        Template cached = connectorTemplateCache.get(templateFilePath);
        if (cached != null) return cached;
        String content = Utils.readFile(templateFilePath);
        Template template = getConnectorHandlebars().compileInline(content);
        connectorTemplateCache.put(templateFilePath, template);
        return template;
    }

    private static synchronized Handlebars getSimpleHandlebars() {
        if (cachedSimpleHandlebars != null) return cachedSimpleHandlebars;
        cachedSimpleHandlebars = new Handlebars();
        return cachedSimpleHandlebars;
    }

    private static Template getSimpleTemplate(String templateFileName) throws IOException {
        Template cached = simpleTemplateCache.get(templateFileName);
        if (cached != null) return cached;
        String content = Utils.readFile(templateFileName);
        Template template = getSimpleHandlebars().compileInline(content);
        simpleTemplateCache.put(templateFileName, template);
        return template;
    }

    static void clearCaches() {
        connectorTemplateCache.clear();
        simpleTemplateCache.clear();
        cachedConnectorHandlebars = null;
        cachedSimpleHandlebars = null;
    }

    private static void registerConnectorHelpers(Handlebars handlebar) {
        handlebar.registerHelper("eq", (first, options) -> {
            Object second = options.param(0);
            if (first == null && second == null) {
                return options.fn();
            }
            if (first != null && first.equals(second)) {
                return options.fn();
            }
            return options.inverse();
        });
        handlebar.registerHelper("not", (context, options) -> {
            if (context instanceof Boolean booleanContext) {
                return !booleanContext;
            }
            return true;
        });
        handlebar.registerHelper("escapeChars", (context, options) -> {
            if (context == null) return "";
            String value = context.toString().replaceAll("^\"(.*)\"$", "$1");
            if (value.equals("()")) {
                return "";
            }
            return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\b", "\\b")
                    .replace("\f", "\\f")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t")
                    .replace("\u0000", "\\u0000");
        });
        handlebar.registerHelper("sanitizeParamName", (context, options) -> {
            if (context == null) return "";
            return Utils.sanitizeParamName(context.toString());
        });
        handlebar.registerHelper("checkFuncType", (context, options) -> {
            FunctionType functionType = (FunctionType) context;
            return functionType.toString().equals(options.param(0));
        });
        handlebar.registerHelper("writeConfigXmlProperties", (context, options) -> {
            Connection connection = (Connection) context;
            StringBuilder result = new StringBuilder();
            List<Type> initParams = connection.getInitComponent() != null ? connection.getInitComponent().getQueryParams() : List.of();
            for (int i = 0; i < initParams.size(); i++) {
                writeConfigXmlProperty(initParams.get(i), i, connection.getConnectionType(), result);
            }
            return new Handlebars.SafeString(result.toString());
        });
        handlebar.registerHelper("writeComponentXmlProperties", (context, options) -> {
            Component component = (Component) context;
            StringBuilder result = new StringBuilder();
            List<PathParamType> pathParams = component.getPathParams();
            for (int i = 0; i < pathParams.size(); i++) {
                writeComponentXmlPathProperty(pathParams.get(i), i, result, i == 0);
            }
            List<Type> queryParams = component.getQueryParams();
            for (int i = 0; i < queryParams.size(); i++) {
                writeComponentXmlQueryProperty(queryParams.get(i), i, result);
            }
            // This helper is only used in functions_template.xml, so always append returnType
            boolean hasPreviousProperties = !pathParams.isEmpty() || !queryParams.isEmpty();
            String indent = hasPreviousProperties ? "        " : "";
            result.append(String.format("%s<property name=\"returnType\" value=\"%s\"/>\n",
                    indent, component.getReturnType()));
            return new Handlebars.SafeString(result.toString());
        });
        handlebar.registerHelper("writeConfigXmlParameters", (context, options) -> {
            @SuppressWarnings("unchecked")
            List<FunctionParam> functionParams = (List<FunctionParam>) context;
            if (functionParams == null) {
                return new Handlebars.SafeString("");
            }
            StringBuilder result = new StringBuilder();
            boolean[] isFirst = {true};
            Set<String> processedParams = new HashSet<>();
            for (FunctionParam functionParam : functionParams) {
                writeXmlParameterElements(functionParam, result, isFirst, processedParams);
            }
            String output = result.toString();
            if (output.endsWith("\n    ")) {
                output = output.substring(0, output.length() - 5);
            }
            return new Handlebars.SafeString(output);
        });
        handlebar.registerHelper("writeConfigXmlParamProperties", (context, options) -> {
            Connection connection = (Connection) context;
            StringBuilder result = new StringBuilder();
            if (connection.getInitComponent() != null) {
                List<FunctionParam> functionParams = connection.getInitComponent().getFunctionParams();
                int[] indexHolder = {0};
                boolean[] isFirst = {true};
                for (FunctionParam functionParam : functionParams) {
                    writeXmlParamProperties(functionParam, connection.getConnectionType().toUpperCase(), result, indexHolder, isFirst);
                }
            }
            String output = result.toString();
            if (!output.isEmpty() && !output.endsWith("\n")) {
                output = output + "\n";
            }
            return new Handlebars.SafeString(output);
        });
        handlebar.registerHelper("writeComponentXmlParameters", (context, options) -> {
            Component component = (Component) context;
            StringBuilder result = new StringBuilder();
            if (component.getPathParams() != null) {
                for (PathParamType param : component.getPathParams()) {
                    result.append(String.format("    <parameter name=\"%s\" description=\"\"/>\n", param.name));
                }
            }
            if (component.getQueryParams() != null) {
                for (Type param : component.getQueryParams()) {
                    result.append(String.format("    <parameter name=\"%s\" description=\"\"/>\n", param.name));
                }
            }
            String output = result.toString();
            if (output.isEmpty()) {
                return new Handlebars.SafeString("");
            }
            output = "\n" + output;
            if (output.endsWith("\n")) {
                output = output.substring(0, output.length() - 1);
            }
            return new Handlebars.SafeString(output);
        });
        handlebar.registerHelper("writeFunctionRecordXmlParameters", (context, options) -> {
            FunctionParam functionParam = (FunctionParam) context;
            StringBuilder result = new StringBuilder();
            if (functionParam instanceof RecordFunctionParam recordParam && !recordParam.getRecordFieldParams().isEmpty()) {
                boolean[] isFirst = {true};
                Set<String> processedParams = new HashSet<>();
                for (FunctionParam fieldParam : recordParam.getRecordFieldParams()) {
                    writeXmlParameterElements(fieldParam, result, isFirst, processedParams);
                }
            } else {
                String sanitizedParamName = Utils.sanitizeParamName(functionParam.getValue());
                String description = functionParam.getDescription() != null ? functionParam.getDescription() : "";
                result.append(String.format("<parameter name=\"%s\" description=\"%s\"/>",
                        sanitizedParamName, escapeXml(description)));
            }
            return new Handlebars.SafeString(result.toString());
        });
        handlebar.registerHelper("writeFunctionRecordXmlProperties", (context, options) -> {
            FunctionParam functionParam = (FunctionParam) context;
            int paramIndex = options.param(0) instanceof Integer ? (Integer) options.param(0) : Integer.parseInt(options.param(0).toString());
            StringBuilder result = new StringBuilder();
            if (functionParam instanceof RecordFunctionParam recordParam && !recordParam.getRecordFieldParams().isEmpty()) {
                int[] fieldIndexHolder = {0};
                String recordParamName = recordParam.getValue();
                for (FunctionParam fieldParam : recordParam.getRecordFieldParams()) {
                    writeFunctionRecordFieldProperties(fieldParam, recordParamName, result, fieldIndexHolder);
                }
            }
            return new Handlebars.SafeString(result.toString());
        });
        handlebar.registerHelper("writeConfigJsonProperties", (context, options) -> {
            Component component = (Component) context;
            JsonTemplateBuilder builder = new JsonTemplateBuilder();
            List<FunctionParam> functionParams = component.getFunctionParams();

            List<FunctionParam> basicParams = new ArrayList<>();
            List<FunctionParam> advancedParams = new ArrayList<>();

            List<FunctionParam> flattenedParams = new ArrayList<>();
            for (FunctionParam param : functionParams) {
                if (param instanceof RecordFunctionParam recordParam && !recordParam.getRecordFieldParams().isEmpty()) {
                    flattenedParams.addAll(recordParam.getRecordFieldParams());
                } else {
                    flattenedParams.add(param);
                }
            }

            for (FunctionParam param : flattenedParams) {
                boolean isAdvanced = false;
                if (!param.isRequired() || (param.getDefaultValue() != null && !param.getDefaultValue().isEmpty())) {
                    isAdvanced = true;
                }
                if (isAdvanced) {
                    advancedParams.add(param);
                } else {
                    basicParams.add(param);
                }
            }

            if (!basicParams.isEmpty()) {
                writeAttributeGroup("Basic", basicParams, advancedParams.isEmpty(), builder);
            }
            if (!advancedParams.isEmpty()) {
                writeAttributeGroup("Advanced", advancedParams, true, builder, true);
            }

            return new Handlebars.SafeString(builder.build());
        });
        handlebar.registerHelper("writeComponentJsonProperties", (context, options) -> {
            Component component = (Component) context;
            JsonTemplateBuilder builder = new JsonTemplateBuilder();

            List<PathParamType> pathParams = component.getPathParams();
            int totalPathParams = pathParams.size();
            for (int i = 0; i < totalPathParams; i++) {
                PathParamType pathParam = pathParams.get(i);
                writeJsonAttributeForPathParam(pathParam, i, totalPathParams, builder);
                if (i < totalPathParams - 1) {
                    builder.addSeparator(ATTRIBUTE_SEPARATOR);
                }
            }

            List<FunctionParam> functionParams = component.getFunctionParams();
            List<FunctionParam> flattenedParams = new ArrayList<>();
            for (FunctionParam param : functionParams) {
                if (param instanceof RecordFunctionParam recordParam && !recordParam.getRecordFieldParams().isEmpty()) {
                    flattenedParams.addAll(recordParam.getRecordFieldParams());
                } else {
                    flattenedParams.add(param);
                }
            }

            List<FunctionParam> requiredParams = new ArrayList<>();
            List<FunctionParam> advancedParams = new ArrayList<>();
            for (FunctionParam param : flattenedParams) {
                boolean isAdvanced = !param.isRequired()
                        || (param.getDefaultValue() != null && !param.getDefaultValue().isEmpty());
                if (isAdvanced) {
                    advancedParams.add(param);
                } else {
                    requiredParams.add(param);
                }
            }

            boolean hasPathParams = totalPathParams > 0;
            boolean hasRequiredParams = !requiredParams.isEmpty();
            boolean hasAdvancedParams = !advancedParams.isEmpty();

            if (hasPathParams && (hasRequiredParams || hasAdvancedParams)) {
                builder.addSeparator(ATTRIBUTE_SEPARATOR);
            }

            int totalRequired = requiredParams.size();
            for (int i = 0; i < totalRequired; i++) {
                writeJsonAttributeForFunctionParam(requiredParams.get(i), i, totalRequired, builder, false, true, null);
            }

            if (hasAdvancedParams) {
                if (hasRequiredParams) {
                    builder.addSeparator(ATTRIBUTE_SEPARATOR);
                }
                writeAttributeGroup("Advanced", advancedParams, true, builder, true);
            }

            return new Handlebars.SafeString(builder.build());
        });
        handlebar.registerHelper("writeConfigDependency", (context, options) -> {
            Connector connector = (Connector) context;
            if (!connector.isBalModule()) {
                return new Handlebars.SafeString("<dependency component=\"config\"/>");
            }
            return new Handlebars.SafeString("");
        });
        handlebar.registerHelper("uppercase", (context, options) -> {
            if (context == null) {
                return "";
            }
            return context.toString().toUpperCase();
        });
        handlebar.registerHelper("arrayElementType", (context, options) -> {
            if (!(context instanceof FunctionParam functionParam)) {
                return "";
            }
            // Use pre-computed value if available (after clearTypeSymbols)
            if (functionParam instanceof ArrayFunctionParam arrayParam) {
                String cached = arrayParam.getArrayElementTypeName();
                if (cached != null) return cached;
            }
            // Fallback to TypeSymbol (before clearTypeSymbols)
            TypeSymbol typeSymbol = functionParam.getTypeSymbol();
            if (typeSymbol == null) {
                return "";
            }
            TypeSymbol actualTypeSymbol = Utils.getActualTypeSymbol(typeSymbol);
            if (!(actualTypeSymbol instanceof ArrayTypeSymbol arrayTypeSymbol)) {
                return "";
            }
            TypeSymbol memberType = arrayTypeSymbol.memberTypeDescriptor();
            TypeDescKind memberKind = Utils.getActualTypeKind(memberType);
            String elementType = Utils.getParamTypeName(memberKind);
            return elementType != null ? elementType : "";
        });
        handlebar.registerHelper("mapValueType", (context, options) -> {
            if (!(context instanceof MapFunctionParam mapParam)) {
                return "";
            }
            // Use pre-computed value
            String cached = mapParam.getValueTypeName();
            if (cached != null) return cached;
            // Fallback to TypeSymbol
            TypeSymbol valueType = mapParam.getValueTypeSymbol();
            if (valueType == null) {
                return "";
            }
            TypeDescKind valueKind = Utils.getActualTypeKind(valueType);
            String valueTypeName = Utils.getParamTypeName(valueKind);
            return valueTypeName != null ? valueTypeName : "";
        });
        handlebar.registerHelper("isMapOfRecord", (context, options) -> {
            if (!(context instanceof MapFunctionParam mapParam)) {
                return false;
            }
            // Use pre-computed value
            TypeDescKind valueKind = mapParam.getValueTypeKind();
            if (valueKind != null) return valueKind == TypeDescKind.RECORD;
            // Fallback to TypeSymbol
            TypeSymbol valueType = mapParam.getValueTypeSymbol();
            if (valueType == null) {
                return false;
            }
            return Utils.getActualTypeKind(valueType) == TypeDescKind.RECORD;
        });
        handlebar.registerHelper("mapRecordFieldNames", (context, options) -> {
            if (!(context instanceof MapFunctionParam mapParam)) {
                return "";
            }
            List<FunctionParam> valueFields = mapParam.getValueFieldParams();
            if (valueFields == null || valueFields.isEmpty()) {
                return "";
            }
            return valueFields.stream()
                    .map(FunctionParam::getValue)
                    .collect(Collectors.joining(","));
        });
        handlebar.registerHelper("unwrapOptional", ((context, options) -> {
            if (context instanceof Optional<?> optional) {
                if (optional.isPresent()) {
                    return optional.get();
                }
            }
            return "";
        }));
        handlebar.registerHelper("capitalize", ((context, options) -> {
            if (context instanceof String) {
                return new Handlebars.SafeString(StringUtils.capitalize((String) context));
            }
            return "";
        }));
        handlebar.registerHelper("sanitizeModuleName", ((context, options) -> {
            if (context == null) {
                return "";
            }
            String moduleName = context.toString();
            return new Handlebars.SafeString(moduleName.replace(".", "_"));
        }));
    }

    private static final int BATCH_SIZE = 50;

    public void serialize(Connector connector) {

        // Pre-compute TypeSymbol-derived values and release heavy compiler references
        // to allow the Ballerina semantic model to be garbage collected before serialization
        connector.clearTypeSymbols();

        try {
            Path destinationPath = targetPath.resolve("generated");
            if (Files.exists(destinationPath)) {
                FileUtils.cleanDirectory(destinationPath.toFile());
            } else {
                Files.createDirectories(destinationPath);
            }

            File connectorFolder = new File(destinationPath.toUri());
            if (!connectorFolder.exists()) {
                connectorFolder.mkdir();
            }

            // Phase 1: Generate aggregate XML files (lightweight, covers all components in one file)
            System.out.println("Generating aggregate XML files...");
            connector.generateInstanceXml(connectorFolder);
            connector.generateFunctionsXml(connectorFolder, Constants.FUNCTION_TEMPLATE_PATH, "functions");
            if (!connector.isBalModule()) {
                connector.generateConfigInstanceXml(connectorFolder, CONFIG_TEMPLATE_PATH, "config");
                connector.generateConfigTemplateXml(connectorFolder, CONFIG_TEMPLATE_PATH, "config");
            }

            // Phase 2: Generate per-connection config JSON
            for (Connection connection : connector.getConnections()) {
                if (connection.getInitComponent() != null) {
                    connection.getInitComponent().generateUIJson(connectorFolder, CONFIG_TEMPLATE_PATH,
                            connection.getConnectionType());
                }
            }

            // Phase 3: Generate per-component files (XML template + JSON UI schema) in batches
            int totalComponents = connector.getComponents().size();
            int processed = 0;
            for (Connection connection : connector.getConnections()) {
                for (Component component : connection.getComponents()) {
                    component.generateTemplateXml(connectorFolder, FUNCTION_TEMPLATE_PATH, "functions");
                    component.generateUIJson(connectorFolder, FUNCTION_TEMPLATE_PATH, component.getName());
                    processed++;
                    if (processed % BATCH_SIZE == 0) {
                        System.out.println("Processed " + processed + "/" + totalComponents + " components...");
                        System.gc();
                    }
                }
            }
            if (processed % BATCH_SIZE != 0) {
                System.out.println("Processed " + processed + "/" + totalComponents + " components.");
            }

            // Copy resources and JARs before clearing connector metadata
            URI jarPath = getClass().getProtectionDomain().getCodeSource().getLocation().toURI();
            copyResources(getClass().getClassLoader(), destinationPath, jarPath, connector.getOrgName(),
                    connector.getModuleName(), connector.getMajorVersion());

            if (connector.isBalModule()) {
                Files.copy(targetPath.resolve("bin").resolve(connector.getModuleName() + ".jar"), destinationPath.resolve(Connector.LIB_PATH).resolve(connector.getModuleName() + ".jar"));
            } else {
                Path generatedArtifactPath = Paths.get(System.getProperty(Constants.CONNECTOR_TARGET_PATH));
                Files.copy(generatedArtifactPath, destinationPath.resolve(Connector.LIB_PATH).resolve(generatedArtifactPath.getFileName()));
            }

            // Extract zip path before clearing connector
            String zipFilePath = targetPath.resolve(connector.getZipFileName()).toString();

            // Aggressively free ALL memory before ZIP packaging:
            // 1. Clear the Connector model (connections, components, nested FunctionParam trees)
            connector.clearComponentData();
            Connector.reset();
            // 2. Clear Handlebars engines and compiled template caches
            clearCaches();
            JsonTemplateBuilder.clearCache();
            // 3. Request GC with finalization to reclaim as much as possible
            System.gc();
            System.runFinalization();
            System.gc();

            Runtime rt = Runtime.getRuntime();
            long usedMB = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
            long maxMB = rt.maxMemory() / (1024 * 1024);
            System.out.println("Packaging connector ZIP... (heap: " + usedMB + "MB used / " + maxMB + "MB max)");
            Utils.zipFolder(destinationPath, zipFilePath);
            System.out.println("Connector ZIP created successfully.");
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
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
            String templateFileName = String.format("%s.%s", templateName, extension);
            Template template = getSimpleTemplate(templateFileName);
            String output = template.apply(element);

            String outputFileName = String.format("%s.%s", outputName, extension);
            Utils.writeFile(outputFileName, output);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void generateFileForConnector(String templatePath, String templateName, String outputName,
                                                 io.ballerina.mi.connectorModel.ModelElement element,
                                                 String extension) {
        try {
            String templateFileName = String.format("%s/%s.%s", templatePath, templateName, extension);
            Template template = getConnectorTemplate(templateFileName);
            String output = template.apply(element);
            
            // Sanitize filename: config files use exact connectionType (no sanitization),
            // function files apply PascalCase conversion
            boolean isConfigFile = templatePath.equals(CONFIG_TEMPLATE_PATH);
            String outputFileName;
            if (isConfigFile) {
                // For config files, use the filename as-is (connectionType already has dots replaced)
                // This ensures the filename matches connectionName exactly for UI lookup
                // Extract just the filename part to avoid any path issues
                int lastSeparator = Math.max(outputName.lastIndexOf('/'), outputName.lastIndexOf('\\'));
                String filename = (lastSeparator >= 0) ? outputName.substring(lastSeparator + 1) : outputName;
                outputFileName = String.format("%s.%s", filename, extension);
                // Preserve directory path
                if (lastSeparator >= 0) {
                    String directory = outputName.substring(0, lastSeparator + 1);
                    outputFileName = directory + outputFileName;
                }
            } else {
                // For function files, apply full sanitization with PascalCase
                String sanitizedOutputName = sanitizeFileName(outputName, false);
                outputFileName = String.format("%s.%s", sanitizedOutputName, extension);
            }
            Utils.writeFile(outputFileName, output);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Sanitize filename by replacing dots with underscores.
     * Optionally converts to PascalCase for function files (not config files).
     * Preserves the directory path structure.
     * 
     * @param filePath The full file path including directory and filename
     * @param isConfigFile If true, only replace dots (no PascalCase). If false, also apply PascalCase.
     * @return The sanitized file path
     */
    private static String sanitizeFileName(String filePath, boolean isConfigFile) {
        if (filePath == null || filePath.isEmpty()) {
            return filePath;
        }
        
        // Extract the directory path and filename
        int lastSeparatorIndex = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
        String filename;
        String directory = "";
        
        if (lastSeparatorIndex < 0) {
            // No directory path, just filename
            filename = filePath;
        } else {
            // Split into directory and filename
            directory = filePath.substring(0, lastSeparatorIndex + 1);
            filename = filePath.substring(lastSeparatorIndex + 1);
        }
        
        // Replace dots with underscores
        String sanitizedFilename = filename.replace(".", "_");
        
        // Only apply PascalCase for function files, not config files
        // Config files must match connectionType exactly for UI to find them
        if (!isConfigFile) {
            // If the filename doesn't contain underscores, preserve it as-is (likely already camelCase)
            // Only apply transformation if there are underscores (module names with dots converted to underscores)
            if (sanitizedFilename.contains("_")) {
                // Convert to PascalCase: split by underscore, capitalize each word except keep first word lowercase
                // Example: zoom_meetings_Client -> zoom_Meetings_Client
                String[] parts = sanitizedFilename.split("_");
                if (parts.length > 0) {
                    StringBuilder pascalCase = new StringBuilder();
                    // Keep first part lowercase (module name)
                    pascalCase.append(parts[0].toLowerCase());
                    // Convert remaining parts to PascalCase
                    for (int i = 1; i < parts.length; i++) {
                        if (!parts[i].isEmpty()) {
                            pascalCase.append("_");
                            pascalCase.append(StringUtils.capitalize(parts[i].toLowerCase()));
                        }
                    }
                    sanitizedFilename = pascalCase.toString();
                }
            }
            // If no underscores, preserve the original camelCase (e.g., "processStringOrInt" stays as-is)
        }
        
        return directory + sanitizedFilename;
    }

    private static void writeComponentXmlPathProperty(PathParamType parameter, int index, StringBuilder result, boolean isFirstPathParam) {
        // Handlebars adds 8 spaces indentation only to the first line of helper output
        // So the first pathParam line doesn't need indentation, but all subsequent lines do
        if (isFirstPathParam) {
            result.append(String.format("<property name=\"pathParam%d\" value=\"%s\"/>\n", index, parameter.name));
        } else {
            result.append(String.format("        <property name=\"pathParam%d\" value=\"%s\"/>\n", index, parameter.name));
        }
        // All pathParamType lines need indentation (they're not the first line)
        result.append(String.format("        <property name=\"pathParamType%d\" value=\"%s\"/>\n", index, parameter.typeName));
    }

    private static void writeComponentXmlQueryProperty(Type parameter, int index, StringBuilder result) {
        switch (parameter.typeName) {
            case STRING:
            case INT, DECIMAL, FLOAT:
            case ENUM, ARRAY:
            case BOOLEAN, MAP:
                result.append(String.format("<property name=\"queryParam%d\" value=\"%s\"/>\n", index,
                        parameter.name));
                result.append(String.format("<property name=\"queryParamType%d\" value=\"%s\"/>\n", index,
                        parameter.typeName));
                break;
            case UNION:
                result.append(String.format("<property name=\"queryParam%d\" value=\"%s\"/>\n", index,
                        parameter.name));
                result.append(String.format("<property name=\"queryParamType%d\" value=\"%s\"/>\n", index,
                        parameter.typeName));
                result.append(String.format("<property name=\"queryParamDataType%d\" value=\"%s\"/>\n", index,
                        String.format("%s_%s", parameter.name, "dataType")));
                break;
            case RECORD:
                result.append(String.format("<property name=\"queryParam%d\" value=\"%s\"/>\n", index,
                        parameter.name));
                result.append(String.format("<property name=\"queryParamType%d\" value=\"%s\"/>\n", index,
                        parameter.typeName));
                if (null != parameter.getTypeInfo()) {
                    result.append(String.format("<property name=\"queryParamRecordName%d\" value=\"%s\"/>\n", index,
                            parameter.typeInfo.name));
                    result.append(String.format("<property name=\"queryParamRecordModule%d\" value=\"%s\"/>\n", index,
                            parameter.typeInfo.moduleName));
                    result.append(String.format("<property name=\"queryParamRecordOrg%d\" value=\"%s\"/>\n", index,
                            parameter.typeInfo.orgName));
                    result.append(String.format("<property name=\"queryParamRecordVersion%d\" value=\"%s\"/>\n", index,
                            parameter.typeInfo.version));
                }
                break;
        }
    }

    private static void writeConfigXmlProperty(Type parameter, int index, String connectionType, StringBuilder result) {
        switch (parameter.typeName) {
            case STRING:
            case INT, DECIMAL, FLOAT:
            case BOOLEAN, MAP:
                result.append(String.format("<property name=\"%s_param%d\" value=\"%s\"/>\n", connectionType, index,
                        parameter.name));
                result.append(String.format("<property name=\"%s_paramType%d\" value=\"%s\"/>\n", connectionType, index,
                        parameter.typeName));
                break;
            case RECORD:
                result.append(String.format("<property name=\"%s_param%d\" value=\"%s\"/>\n", connectionType, index,
                        parameter.name));
                result.append(String.format("<property name=\"%s_paramType%d\" value=\"%s\"/>\n", connectionType,
                        index, parameter.typeName));
                result.append(String.format("<property name=\"%s_recordName%d\" value=\"%s\"/>\n", connectionType,
                        index, parameter.typeInfo.name));
                result.append(String.format("<property name=\"%s_recordModule%d\" value=\"%s\"/>\n", connectionType,
                        index, parameter.typeInfo.moduleName));
                result.append(String.format("<property name=\"%s_recordOrg%d\" value=\"%s\"/>\n", connectionType,
                        index, parameter.typeInfo.orgName));
                result.append(String.format("<property name=\"%s_recordVersion%d\" value=\"%s\"/>\n", connectionType,
                        index, parameter.typeInfo.version));
                //TODO: Generate properties for record fields
//                List<Type> recFields = ((RecordType) parameter).fields;
//                for (int i = 0; i < recFields.size(); i++) {
//                    writeConfigXmlProperty(recFields.get(i), i, connectionType, result);
//                }
                break;
            case UNION:
                result.append(String.format("<property name=\"%s_param%d\" value=\"%s\"/>\n", connectionType, index,
                        parameter.name));
                result.append(String.format("<property name=\"%s_paramType%d\" value=\"%s\"/>\n", connectionType, index,
                        parameter.typeName));
                result.append(String.format("<property name=\"%s_dataType%d\" value=\"%s\"/>\n", connectionType, index,
                        String.format("%s_%s", parameter.name, "dataType")));
                break;
//            case INTERSECTION:
            case ENUM:
                result.append(String.format("<property name=\"%s_param%d\" value=\"%s\"/>\n", connectionType, index,
                        parameter.name));
                result.append(String.format("<property name=\"%s_paramType%d\" value=\"%s\"/>\n", connectionType, index,
                        parameter.typeName));
                break;
            case ARRAY:
                //TODO: Generate properties for array
        }
    }

    private static void writeJsonAttributeForFunctionParam(FunctionParam functionParam, int index, int paramLength,
                                                           JsonTemplateBuilder builder,
                                                           boolean isCombo, boolean expandRecords) throws IOException {
        writeJsonAttributeForFunctionParam(functionParam, index, paramLength, builder, isCombo, expandRecords, null);
    }

    private static void writeJsonAttributeForFunctionParam(FunctionParam functionParam, int index, int paramLength,
                                                           JsonTemplateBuilder builder,
                                                           boolean isCombo, boolean expandRecords, String groupName) throws IOException {
        String paramType = functionParam.getParamType();
        String originalParamValue = functionParam.getValue();
        String displayParamValue = originalParamValue;

        
        // For display purposes, remove group prefix if we're in a group context
        // For nested fields like "http1Settings.proxy.host" in "Proxy" group, 
        // remove everything up to and including the group name segment
        if (groupName != null && !groupName.isEmpty() && displayParamValue != null) {
            displayParamValue = removeGroupPrefix(displayParamValue, groupName);
        }
        
        // The 'name' attribute must match the XML parameter name (full path like "server.host")
        String sanitizedParamName = Utils.sanitizeParamName(originalParamValue);
        
        // Display name is the human-friendly version (last segment only)
        String displayName = displayParamValue;
        if (displayName.contains(".")) {
             displayName = displayName.substring(displayName.lastIndexOf('.') + 1);
        }
        
        // Apply sanitizeParamName to displayName as well to remove leading quotes
        // (e.g., 'limit -> limit) to avoid breaking Synapse XML parsing
        displayName = Utils.sanitizeParamName(displayName);

        
        String defaultValue = functionParam.getDefaultValue() != null ? functionParam.getDefaultValue() : "";
        switch (paramType) {
            case STRING, XML:
                Attribute stringAttr = new Attribute(sanitizedParamName, displayName, INPUT_TYPE_STRING_OR_EXPRESSION,
                        defaultValue, functionParam.isRequired(), functionParam.getDescription(), "",
                        "", isCombo);
                stringAttr.setEnableCondition(functionParam.getEnableCondition());
                builder.addFromTemplate(ATTRIBUTE_TEMPLATE_PATH, stringAttr);
                break;
            case MAP:
                if (functionParam instanceof MapFunctionParam mapParam && mapParam.isRenderAsTable()) {
                    writeMapAsTable(mapParam, builder, isCombo);
                } else {
                    // Fall back to JSON input for complex maps
                    Attribute mapAttr = new Attribute(sanitizedParamName, displayName,
                        INPUT_TYPE_STRING_OR_EXPRESSION, defaultValue, functionParam.isRequired(),
                        functionParam.getDescription() != null ? functionParam.getDescription() : "Expecting JSON object with key-value pairs",
                        JSON, "", isCombo);
                    mapAttr.setEnableCondition(functionParam.getEnableCondition());
                    builder.addFromTemplate(ATTRIBUTE_TEMPLATE_PATH, mapAttr);
                }
                break;
            case ARRAY:
                if (functionParam instanceof ArrayFunctionParam arrayParam && arrayParam.isRenderAsTable()) {
                    if (arrayParam.is2DArray()) {
                        writeNestedArrayAsTable(arrayParam, builder, isCombo);
                    } else if (arrayParam.isUnionArray()) {
                        writeUnionArrayAsTable(arrayParam, builder, isCombo);
                    } else {
                        writeArrayAsTable(arrayParam, builder, isCombo);
                    }
                } else {
                    // Fall back to JSON input for complex arrays
                    Attribute arrayAttr = new Attribute(sanitizedParamName, displayName,
                        INPUT_TYPE_STRING_OR_EXPRESSION, defaultValue, functionParam.isRequired(),
                        functionParam.getDescription() != null ? functionParam.getDescription() : "Expecting JSON array",
                        JSON, "", isCombo);
                    arrayAttr.setEnableCondition(functionParam.getEnableCondition());
                    builder.addFromTemplate(ATTRIBUTE_TEMPLATE_PATH, arrayAttr);
                }
                break;
            case JSON:
                // JSON parameters get a concise hint and validation
                String jsonHelpTip = functionParam.getDescription();
                if (jsonHelpTip == null || jsonHelpTip.isEmpty()) {
                    jsonHelpTip = "Expecting JSON object";
                }
                // Apply regex validation for both required and optional fields
                //  use a pattern that allows empty string OR valid values
                String jsonMatchPattern = functionParam.isRequired() ? "" : JSON_OBJECT_REGEX_OPTIONAL;
                String validationType = functionParam.isRequired() ? JSON : VALIDATE_TYPE_REGEX;
                Attribute jsonAttr = new Attribute(sanitizedParamName, displayName, INPUT_TYPE_STRING_OR_EXPRESSION,
                        defaultValue, functionParam.isRequired(), jsonHelpTip, validationType,
                        jsonMatchPattern, isCombo);
                jsonAttr.setEnableCondition(functionParam.getEnableCondition());
                builder.addFromTemplate(ATTRIBUTE_TEMPLATE_PATH, jsonAttr);
                break;
            case RECORD:
                if (expandRecords) {
                    writeRecordFields(functionParam, builder, expandRecords, groupName);
                } else {
                    // Generate concise hint and validation for non-expanded records
                    String recordHelpTip = functionParam.getDescription();
                    if (recordHelpTip == null || recordHelpTip.isEmpty()) {
                        recordHelpTip = "Expecting JSON object";
                    }
                    // For non-expanded records (input via UI), we use "json" validation type
                    // This enables the JSON editor in the UI
                    Attribute recordAttr = new Attribute(functionParam.getValue(), displayName,
                            INPUT_TYPE_STRING_OR_EXPRESSION, defaultValue, functionParam.isRequired(),
                            recordHelpTip, JSON, "", isCombo);
                    recordAttr.setEnableCondition(functionParam.getEnableCondition());
                    builder.addFromTemplate(ATTRIBUTE_TEMPLATE_PATH, recordAttr);
                }
                break;
            case INT:
                // Apply regex validation for both required and optional fields.
                // Optional fields use a pattern that allows empty string OR valid values
                String intMatchPattern = functionParam.isRequired() ? INTEGER_REGEX : INTEGER_REGEX_OPTIONAL;
                Attribute intAttr = new Attribute(functionParam.getValue(), displayName, INPUT_TYPE_STRING_OR_EXPRESSION,
                        defaultValue, functionParam.isRequired(), functionParam.getDescription(), VALIDATE_TYPE_REGEX,
                        intMatchPattern, isCombo);
                intAttr.setEnableCondition(functionParam.getEnableCondition());
                builder.addFromTemplate(ATTRIBUTE_TEMPLATE_PATH, intAttr);
                break;
            case DECIMAL, FLOAT:
                // Apply regex validation for both required and optional fields.
                // Optional fields use a pattern that allows empty string OR valid values
                String decMatchPattern = functionParam.isRequired() ? DECIMAL_REGEX : DECIMAL_REGEX_OPTIONAL;
                Attribute decAttr = new Attribute(functionParam.getValue(), displayName,
                        INPUT_TYPE_STRING_OR_EXPRESSION, defaultValue, functionParam.isRequired(),
                        functionParam.getDescription(), VALIDATE_TYPE_REGEX, decMatchPattern, isCombo);
                decAttr.setEnableCondition(functionParam.getEnableCondition());
                builder.addFromTemplate(ATTRIBUTE_TEMPLATE_PATH, decAttr);
                break;
            case BOOLEAN:
                Attribute boolAttr = new Attribute(functionParam.getValue(), displayName, INPUT_TYPE_BOOLEAN,
                        defaultValue, functionParam.isRequired(), functionParam.getDescription(), "",
                        "", isCombo);
                boolAttr.setEnableCondition(functionParam.getEnableCondition());
                builder.addFromTemplate(ATTRIBUTE_TEMPLATE_PATH, boolAttr);
                break;
            case UNION:
                // Ensure the functionParam is actually a UnionFunctionParam instance
                if (!(functionParam instanceof UnionFunctionParam unionFunctionParam)) {
                    throw new IllegalArgumentException("FunctionParam with paramType 'union' must be an instance of UnionFunctionParam for parameter: " + functionParam.getValue());
                }
                // Gather the data types in the union
                if (!unionFunctionParam.getUnionMemberParams().isEmpty()) {
                    List<FunctionParam> unionMembers = unionFunctionParam.getUnionMemberParams();
                    List<FunctionParam> validMembers = new ArrayList<>();
                    for (FunctionParam member : unionMembers) {
                        // Skip nested unions that have no members (would produce no output)
                        if (member instanceof UnionFunctionParam nestedUnion && nestedUnion.getUnionMemberParams().isEmpty()) {
                            continue;
                        }
                        validMembers.add(member);
                    }

                    // Determine if we should show the combo box
                    boolean showCombo = validMembers.size() > 1;

                    // If groupName is not provided but we're in a group context, detect it from the field path
                    String effectiveGroupName = groupName;
                    if (effectiveGroupName == null && originalParamValue != null && originalParamValue.contains(".")) {
                        String immediateParent = getImmediateParentSegment(originalParamValue);
                        if (immediateParent != null) {
                            effectiveGroupName = immediateParent;
                        }
                    } else if (!showCombo && validMembers.size() == 1) {
                         // Even if not strictly a group, if we are skipping combo, we might want to ensure proper naming
                         // But logic below handles effectiveGroupName fine.
                    }

                    if (showCombo) {
                        Combo comboField = getComboField(unionFunctionParam, functionParam.getValue(),
                                functionParam.getDescription(), effectiveGroupName);
                        builder.addFromTemplate(COMBO_TEMPLATE_PATH, comboField);

                        // Add separator after combo field if there are valid members
                        if (!validMembers.isEmpty()) {
                            builder.addSeparator(ATTRIBUTE_SEPARATOR);
                        }
                    } else if (validMembers.size() == 1) {
                        // If only one option, we skip the combo.
                        // We must update the member's enable condition to remove dependency on the (missing) combo.
                        // Instead, it should inherit the Union's parent condition directly.
                        FunctionParam singleMember = validMembers.get(0);
                        singleMember.setEnableCondition(functionParam.getEnableCondition());
                    }

                    // Aggregate record members to write them together (prevents duplicate groups)
                    List<FunctionParam> recordMembers = new ArrayList<>();
                    List<FunctionParam> simpleMembers = new ArrayList<>();
                    
                    for (int i = 0; i < validMembers.size(); i++) {
                        FunctionParam member = validMembers.get(i);
                        if (member.getParamType().equals(RECORD) && expandRecords) {
                            recordMembers.add(member);
                        } else {
                            simpleMembers.add(member);
                        }
                    }

                    // Write simple members first
                    for (int i = 0; i < simpleMembers.size(); i++) {
                        writeJsonAttributeForFunctionParam(simpleMembers.get(i), index, paramLength, builder, true, expandRecords, effectiveGroupName);
                        if (i < simpleMembers.size() - 1 || !recordMembers.isEmpty()) {
                            builder.addSeparator(ATTRIBUTE_SEPARATOR);
                        }
                    }
                    
                    // Write aggregated record fields
                    if (!recordMembers.isEmpty()) {
                        // Create a virtual RecordFunctionParam to hold all fields from all record members
                        RecordFunctionParam virtualRecordParam = new RecordFunctionParam(
                            Integer.toString(index), 
                            functionParam.getValue(), 
                            RECORD
                        );
                        // Inherit the enable condition from the union param itself if needed
                        virtualRecordParam.setEnableCondition(functionParam.getEnableCondition());
                        
                        // Collect all fields
                        for (FunctionParam member : recordMembers) {
                            if (member instanceof RecordFunctionParam recordParam) {
                                String memberCondition = member.getEnableCondition();
                                // If we skipped combo, memberCondition is already reset to parent condition (above).
                                // If we have combo, memberCondition includes "combo == X".
                                
                                for (FunctionParam field : recordParam.getRecordFieldParams()) {
                                    // Propagate member condition to field
                                    String fieldCondition = field.getEnableCondition();
                                    String mergedCondition = mergeEnableConditions(memberCondition, fieldCondition);
                                    field.setEnableCondition(mergedCondition);
                                    
                                    virtualRecordParam.addRecordFieldParam(field);
                                }
                            }
                        }
                        
                        // Write combined fields - this will group them correctly under specialized groups (e.g. "Auth")
                        writeRecordFields(virtualRecordParam, builder, true, effectiveGroupName);
                    }
                }
                break;
            default:
                throw new IllegalArgumentException("Unsupported parameter type '" + paramType + "' for parameter: " + functionParam.getValue());
        }
        // Don't add separator for combo members (they're handled by parent union logic)
        if (!isCombo) {
            builder.addConditionalSeparator((index < paramLength - 1), ATTRIBUTE_SEPARATOR);
        }
    }

    /**
     * Write JSON attribute for a path parameter.
     * Path parameters are converted to input elements in the UI schema.
     */
    private static void writeJsonAttributeForPathParam(PathParamType pathParam, int index, int paramLength,
                                                      JsonTemplateBuilder builder) throws IOException {
        String paramType = pathParam.typeName;
        String sanitizedParamName = Utils.sanitizeParamName(pathParam.name);
        String displayName = pathParam.name;
        // Apply sanitizeParamName to displayName to remove leading quotes
        // (e.g., 'limit -> limit) to avoid breaking Synapse XML parsing
        displayName = Utils.sanitizeParamName(displayName);
        String description = ""; // PathParamType doesn't have documentation field
        
        switch (paramType) {
            case STRING, XML, JSON, MAP, RECORD, ARRAY:
                Attribute stringAttr = new Attribute(sanitizedParamName, displayName, INPUT_TYPE_STRING_OR_EXPRESSION,
                        "", true, description, "", "", false);
                builder.addFromTemplate(ATTRIBUTE_TEMPLATE_PATH, stringAttr);
                break;
            case INT:
                Attribute intAttr = new Attribute(sanitizedParamName, displayName, INPUT_TYPE_STRING_OR_EXPRESSION,
                        "", true, description, VALIDATE_TYPE_REGEX, INTEGER_REGEX, false);
                builder.addFromTemplate(ATTRIBUTE_TEMPLATE_PATH, intAttr);
                break;
            case DECIMAL, FLOAT:
                Attribute decAttr = new Attribute(sanitizedParamName, displayName,
                        INPUT_TYPE_STRING_OR_EXPRESSION, "", true, description, 
                        VALIDATE_TYPE_REGEX, DECIMAL_REGEX, false);
                builder.addFromTemplate(ATTRIBUTE_TEMPLATE_PATH, decAttr);
                break;
            case BOOLEAN:
                Attribute boolAttr = new Attribute(sanitizedParamName, displayName, INPUT_TYPE_BOOLEAN,
                        "", true, description, "", "", false);
                builder.addFromTemplate(ATTRIBUTE_TEMPLATE_PATH, boolAttr);
                break;
            default:
                // Default to string for unknown types
                Attribute defaultAttr = new Attribute(sanitizedParamName, displayName, INPUT_TYPE_STRING_OR_EXPRESSION,
                        "", true, description, "", "", false);
                builder.addFromTemplate(ATTRIBUTE_TEMPLATE_PATH, defaultAttr);
                break;
        }
    }

    private static String getTypeNameOrFallback(FunctionParam member, String fallback) {
        // Prefer displayTypeName (set during analysis), then resolvedTypeName (from TypeSymbol.getName()),
        // then try TypeSymbol directly as fallback
        String name = member.getDisplayTypeName();
        if (name != null && !name.isEmpty()) return name;
        name = member.getResolvedTypeName();
        if (name != null && !name.isEmpty()) return name;
        TypeSymbol ts = member.getTypeSymbol();
        if (ts != null) return ts.getName().orElse(fallback);
        return fallback;
    }

    private static Combo getComboField(UnionFunctionParam unionFunctionParam, String paramName, String helpTip, String groupName) {
        List<FunctionParam> unionMembers = unionFunctionParam.getUnionMemberParams();
        StringJoiner unionJoiner = new StringJoiner(",", "[", "]");
        for (int i = 0; i < unionMembers.size(); i++) {
            FunctionParam member = unionMembers.get(i);
            String comboItem;
            if (member.getParamType().equals(RECORD)) {
                comboItem = getTypeNameOrFallback(member, "Record" + i);
            } else if (member.getParamType().equals(UNION)) {
                comboItem = getTypeNameOrFallback(member, "Union" + i);
            } else {
                comboItem = member.getParamType();
            }
            unionJoiner.add("\"" + comboItem + "\"");
        }
        String unionComboValues = unionJoiner.toString();

        // Always set the first member as default for all combo fields.
        // enableCondition controls visibility - hidden fields won't be serialized to XML.
        FunctionParam firstMember = unionMembers.getFirst();
        String defaultValue;
        if (firstMember.getParamType().equals(RECORD)) {
            defaultValue = getTypeNameOrFallback(firstMember, RECORD);
        } else if (firstMember.getParamType().equals(UNION)) {
            defaultValue = getTypeNameOrFallback(firstMember, UNION);
        } else {
            defaultValue = firstMember.getParamType();
        }
        String enableCondition = unionFunctionParam.getEnableCondition();
        // Combo field for selecting the data type - sanitize the parameter name
        // Keep full qualified name for the combo name (for enable conditions matching)
        String sanitizedParamName = Utils.sanitizeParamName(paramName);
        String comboName = String.format("%s%s", sanitizedParamName, "DataType");
        
        // For displayName, remove group prefix if in a group context
        // Note: Utils.sanitizeParamName is already applied in the format string above,
        // which removes leading quotes (e.g., 'limit -> limit) to avoid breaking Synapse
        String comboDisplayName = comboName;
        if (groupName != null && !groupName.isEmpty() && paramName != null) {
            // Remove group prefix from paramName first, then add "DataType"
            String displayParamName = removeGroupPrefix(paramName, groupName);
            // For deeply nested fields, just take the last segment for display
            if (displayParamName.contains(".")) {
                displayParamName = displayParamName.substring(displayParamName.lastIndexOf('.') + 1);
            }
            comboDisplayName = String.format("%s%s", Utils.sanitizeParamName(displayParamName), "DataType");
        }
        
        return new Combo(comboName, comboDisplayName, INPUT_TYPE_COMBO, unionComboValues, defaultValue,
                unionFunctionParam.isRequired(), enableCondition, helpTip);
    }

    /**
     * Counts the total number of parameters after expanding record fields.
     */
    private static int countExpandedParams(List<FunctionParam> functionParams) {
        int count = 0;
        for (FunctionParam param : functionParams) {
            if (param instanceof RecordFunctionParam recordParam && !recordParam.getRecordFieldParams().isEmpty()) {
                count += countExpandedParams(recordParam.getRecordFieldParams());
            } else {
                count++;
            }
        }
        return count;
    }

    /**
     * Writes XML property elements for function params, expanding record fields with proper indexing.
     * For record parameters, writes the record parameter itself with type="record" and then
     * writes the record fields with the record parameter name prefix.
     */
    private static void writeXmlParamProperties(FunctionParam functionParam, String connectionType,
                                                 StringBuilder result, int[] indexHolder, boolean[] isFirst) {
        if (functionParam instanceof RecordFunctionParam recordParam && !recordParam.getRecordFieldParams().isEmpty()) {
            // First, write the record parameter itself with type="record"
            if (!isFirst[0]) {
                result.append("\n        ");
            }
            result.append(String.format("<property name=\"%s_param%d\" value=\"%s\"/>",
                    connectionType, indexHolder[0], recordParam.getValue()));
            result.append(String.format("\n        <property name=\"%s_paramType%d\" value=\"%s\"/>",
                    connectionType, indexHolder[0], RECORD));
            result.append(String.format("\n        <property name=\"%s_param%d_recordName\" value=\"%s\"/>",
                    connectionType, indexHolder[0], recordParam.getRecordName()));
            isFirst[0] = false;
            int recordParamIndex = indexHolder[0];
            indexHolder[0]++;

            // Then, write record fields with the record parameter name prefix
            int[] fieldIndexHolder = {0};
            String recordParamName = recordParam.getValue();
            for (FunctionParam fieldParam : recordParam.getRecordFieldParams()) {
                writeRecordFieldParamProperties(fieldParam, connectionType, recordParamName, result, fieldIndexHolder);
            }
        } else {
            // Generate param and paramType properties for non-record parameters
            if (!isFirst[0]) {
                result.append("\n        ");
            }
            result.append(String.format("<property name=\"%s_param%d\" value=\"%s\"/>",
                    connectionType, indexHolder[0], functionParam.getValue()));
            result.append(String.format("\n        <property name=\"%s_paramType%d\" value=\"%s\"/>",
                    connectionType, indexHolder[0], functionParam.getParamType()));
            isFirst[0] = false;
            indexHolder[0]++;
        }
    }

    /**
     * Writes XML property elements for record field parameters with the record parameter name prefix.
     * This allows the runtime to identify which fields belong to which record parameter.
     * Recursively expands nested records to their leaf fields.
     * IMPORTANT: Keep the full qualified field path (e.g., "http1Settings.proxy.host") in the value
     * so the runtime can reconstruct the nested record structure. Only remove the prefix for UI display
     * in JSON schema, not in XML properties.
     */
    private static void writeRecordFieldParamProperties(FunctionParam fieldParam, String connectionType,
                                                        String recordParamName, StringBuilder result,
                                                        int[] fieldIndexHolder) {
        writeRecordFieldParamPropertiesWithUnionMember(fieldParam, connectionType, recordParamName, result,
                fieldIndexHolder, null);
    }

    /**
     * Writes XML properties for record field parameters, with support for tracking union member types.
     * When a field belongs to a union member record, the unionMemberType parameter indicates which
     * union member type this field belongs to, enabling runtime reconstruction of the correct record type.
     *
     * @param fieldParam The field parameter to write
     * @param connectionType The connection type prefix
     * @param recordParamName The record parameter name
     * @param result The StringBuilder to append to
     * @param fieldIndexHolder The current field index (mutable)
     * @param unionMemberType The union member type name if this field belongs to a union member record, null otherwise
     */
    private static void writeRecordFieldParamPropertiesWithUnionMember(FunctionParam fieldParam, String connectionType,
                                                        String recordParamName, StringBuilder result,
                                                        int[] fieldIndexHolder, String unionMemberType) {
        // If this is a nested record, recursively expand its fields
        if (fieldParam instanceof RecordFunctionParam nestedRecordParam && !nestedRecordParam.getRecordFieldParams().isEmpty()) {
            // Recursively expand nested record fields, preserving union member context
            for (FunctionParam nestedFieldParam : nestedRecordParam.getRecordFieldParams()) {
                writeRecordFieldParamPropertiesWithUnionMember(nestedFieldParam, connectionType, recordParamName,
                        result, fieldIndexHolder, unionMemberType);
            }
        } else if (fieldParam instanceof UnionFunctionParam unionFieldParam) {
            // Write the union field with the record parameter name prefix
            result.append("\n        ");
            // Keep full qualified path for runtime reconstruction (e.g., "http1Settings.proxy.host")
            String fieldValue = fieldParam.getValue();
            // Use pattern: {connectionType}_{recordParamName}_param{fieldIndex}
            result.append(String.format("<property name=\"%s_%s_param%d\" value=\"%s\"/>",
                    connectionType, recordParamName, fieldIndexHolder[0], fieldValue));
            result.append(String.format("\n        <property name=\"%s_%s_paramType%d\" value=\"%s\"/>",
                    connectionType, recordParamName, fieldIndexHolder[0], fieldParam.getParamType()));
            // Add dataType property for union type selector - use sanitized param name to match combo field
            // Use full qualified path for the dataType property name to ensure it matches enable conditions
            String sanitizedParamName = Utils.sanitizeParamName(fieldValue);
            result.append(String.format("\n        <property name=\"%s_%s_dataType%d\" value=\"%s\"/>",
                    connectionType, recordParamName, fieldIndexHolder[0],
                    String.format("%s_%s", sanitizedParamName, "DataType")));

            // If this union field is inside another union member, add the unionMember property
            if (unionMemberType != null) {
                result.append(String.format("\n        <property name=\"%s_%s_unionMember%d\" value=\"%s\"/>",
                        connectionType, recordParamName, fieldIndexHolder[0], unionMemberType));
            }

            fieldIndexHolder[0]++;

            // Now recursively write the fields of each union member that is a record type
            // This allows runtime to know what fields belong to which union member
            for (FunctionParam memberParam : unionFieldParam.getUnionMemberParams()) {
                if (memberParam instanceof RecordFunctionParam recordMemberParam) {
                    // Get the union member type name (e.g., "BearerTokenConfig", "OAuth2RefreshTokenGrantConfig")
                    String memberTypeName = recordMemberParam.getDisplayTypeName();
                    if (memberTypeName == null || memberTypeName.isEmpty()) {
                        memberTypeName = recordMemberParam.getRecordName();
                    }
                    // Recursively write record fields with the union member type context
                    for (FunctionParam recordField : recordMemberParam.getRecordFieldParams()) {
                        writeRecordFieldParamPropertiesWithUnionMember(recordField, connectionType, recordParamName,
                                result, fieldIndexHolder, memberTypeName);
                    }
                }
                // For primitive union members (string, int, etc.), no additional fields to write
            }
        } else {
            // Write the leaf field with the record parameter name prefix
            result.append("\n        ");
            // Keep full qualified path for runtime reconstruction (e.g., "http1Settings.proxy.host")
            String fieldValue = fieldParam.getValue();
            // Use pattern: {connectionType}_{recordParamName}_param{fieldIndex}
            result.append(String.format("<property name=\"%s_%s_param%d\" value=\"%s\"/>",
                    connectionType, recordParamName, fieldIndexHolder[0], fieldValue));
            result.append(String.format("\n        <property name=\"%s_%s_paramType%d\" value=\"%s\"/>",
                    connectionType, recordParamName, fieldIndexHolder[0], fieldParam.getParamType()));

            // If this field belongs to a union member record, add the unionMember property
            if (unionMemberType != null) {
                result.append(String.format("\n        <property name=\"%s_%s_unionMember%d\" value=\"%s\"/>",
                        connectionType, recordParamName, fieldIndexHolder[0], unionMemberType));
            }

            fieldIndexHolder[0]++;
        }
    }

    /**
     * Writes XML property elements for function record field parameters without connectionType prefix.
     * Used for regular function parameters (not config/init).
     * Pattern: {recordParamName}_param{fieldIndex} / {recordParamName}_paramType{fieldIndex}
     */
    private static void writeFunctionRecordFieldProperties(FunctionParam fieldParam, String recordParamName,
                                                            StringBuilder result, int[] fieldIndexHolder) {
        if (fieldParam instanceof RecordFunctionParam nestedRecordParam && !nestedRecordParam.getRecordFieldParams().isEmpty()) {
            for (FunctionParam nestedFieldParam : nestedRecordParam.getRecordFieldParams()) {
                writeFunctionRecordFieldProperties(nestedFieldParam, recordParamName, result, fieldIndexHolder);
            }
        } else if (fieldParam instanceof UnionFunctionParam unionFieldParam) {
            String fieldValue = fieldParam.getValue();
            result.append(String.format("\n        <property name=\"%s_param%d\" value=\"%s\"/>",
                    recordParamName, fieldIndexHolder[0], fieldValue));
            result.append(String.format("\n        <property name=\"%s_paramType%d\" value=\"%s\"/>",
                    recordParamName, fieldIndexHolder[0], fieldParam.getParamType()));
            String sanitizedParamName = Utils.sanitizeParamName(fieldValue);
            result.append(String.format("\n        <property name=\"%s_dataType%d\" value=\"%s\"/>",
                    recordParamName, fieldIndexHolder[0],
                    String.format("%s_%s", sanitizedParamName, "DataType")));
            fieldIndexHolder[0]++;

            for (FunctionParam memberParam : unionFieldParam.getUnionMemberParams()) {
                if (memberParam instanceof RecordFunctionParam recordMemberParam) {
                    String memberTypeName = recordMemberParam.getDisplayTypeName();
                    if (memberTypeName == null || memberTypeName.isEmpty()) {
                        memberTypeName = recordMemberParam.getRecordName();
                    }
                    for (FunctionParam recordField : recordMemberParam.getRecordFieldParams()) {
                        writeFunctionRecordFieldPropertiesWithUnionMember(recordField, recordParamName,
                                result, fieldIndexHolder, memberTypeName);
                    }
                }
            }
        } else {
            String fieldValue = fieldParam.getValue();
            result.append(String.format("\n        <property name=\"%s_param%d\" value=\"%s\"/>",
                    recordParamName, fieldIndexHolder[0], fieldValue));
            result.append(String.format("\n        <property name=\"%s_paramType%d\" value=\"%s\"/>",
                    recordParamName, fieldIndexHolder[0], fieldParam.getParamType()));
            fieldIndexHolder[0]++;
        }
    }

    /**
     * Writes XML property elements for function record field parameters with union member tracking.
     * Used for fields within union member records in function parameters.
     */
    private static void writeFunctionRecordFieldPropertiesWithUnionMember(FunctionParam fieldParam, String recordParamName,
                                                                          StringBuilder result, int[] fieldIndexHolder,
                                                                          String unionMemberType) {
        if (fieldParam instanceof RecordFunctionParam nestedRecordParam && !nestedRecordParam.getRecordFieldParams().isEmpty()) {
            for (FunctionParam nestedFieldParam : nestedRecordParam.getRecordFieldParams()) {
                writeFunctionRecordFieldPropertiesWithUnionMember(nestedFieldParam, recordParamName,
                        result, fieldIndexHolder, unionMemberType);
            }
        } else if (fieldParam instanceof UnionFunctionParam unionFieldParam) {
            String fieldValue = fieldParam.getValue();
            result.append(String.format("\n        <property name=\"%s_param%d\" value=\"%s\"/>",
                    recordParamName, fieldIndexHolder[0], fieldValue));
            result.append(String.format("\n        <property name=\"%s_paramType%d\" value=\"%s\"/>",
                    recordParamName, fieldIndexHolder[0], fieldParam.getParamType()));
            String sanitizedParamName = Utils.sanitizeParamName(fieldValue);
            result.append(String.format("\n        <property name=\"%s_dataType%d\" value=\"%s\"/>",
                    recordParamName, fieldIndexHolder[0],
                    String.format("%s_%s", sanitizedParamName, "DataType")));
            if (unionMemberType != null) {
                result.append(String.format("\n        <property name=\"%s_unionMember%d\" value=\"%s\"/>",
                        recordParamName, fieldIndexHolder[0], unionMemberType));
            }
            fieldIndexHolder[0]++;

            for (FunctionParam memberParam : unionFieldParam.getUnionMemberParams()) {
                if (memberParam instanceof RecordFunctionParam recordMemberParam) {
                    String memberTypeName = recordMemberParam.getDisplayTypeName();
                    if (memberTypeName == null || memberTypeName.isEmpty()) {
                        memberTypeName = recordMemberParam.getRecordName();
                    }
                    for (FunctionParam recordField : recordMemberParam.getRecordFieldParams()) {
                        writeFunctionRecordFieldPropertiesWithUnionMember(recordField, recordParamName,
                                result, fieldIndexHolder, memberTypeName);
                    }
                }
            }
        } else {
            String fieldValue = fieldParam.getValue();
            result.append(String.format("\n        <property name=\"%s_param%d\" value=\"%s\"/>",
                    recordParamName, fieldIndexHolder[0], fieldValue));
            result.append(String.format("\n        <property name=\"%s_paramType%d\" value=\"%s\"/>",
                    recordParamName, fieldIndexHolder[0], fieldParam.getParamType()));
            if (unionMemberType != null) {
                result.append(String.format("\n        <property name=\"%s_unionMember%d\" value=\"%s\"/>",
                        recordParamName, fieldIndexHolder[0], unionMemberType));
            }
            fieldIndexHolder[0]++;
        }
    }

    /**
     * Writes XML parameter elements for function params, expanding record fields as separate parameters.
     * Also expands UnionFunctionParams into their discriminant (DataType) and member fields.
     */
    private static void writeXmlParameterElements(FunctionParam functionParam, StringBuilder result, boolean[] isFirst, Set<String> processedParams) {
        if (functionParam instanceof RecordFunctionParam recordParam && !recordParam.getRecordFieldParams().isEmpty()) {
            // Expand record fields as separate parameters
            for (FunctionParam fieldParam : recordParam.getRecordFieldParams()) {
                writeXmlParameterElements(fieldParam, result, isFirst, processedParams);
            }
        } else if (functionParam instanceof UnionFunctionParam unionParam) {
            // Expand Union: Add DataType param and expand members

            // 1. DataType parameter
            String sanitizedParamName = Utils.sanitizeParamName(unionParam.getValue());
            String dataTypeParamName = sanitizedParamName + "_DataType";
            
            if (!processedParams.contains(dataTypeParamName)) {
                String description = functionParam.getDescription() != null ? functionParam.getDescription() : "";
                if (!isFirst[0]) {
                    result.append("\n    ");
                }
                result.append(String.format("<parameter name=\"%s\" description=\"%s\"/>",
                        dataTypeParamName, escapeXml(description)));
                isFirst[0] = false;
                processedParams.add(dataTypeParamName);
            }

            // 2. Expand all union members properties
            for (FunctionParam member : unionParam.getUnionMemberParams()) {
                // If member is Record, expand its fields
                if (member instanceof RecordFunctionParam memberRecord) {
                    for (FunctionParam field : memberRecord.getRecordFieldParams()) {
                        writeXmlParameterElements(field, result, isFirst, processedParams);
                    }
                } else if (member instanceof UnionFunctionParam) {
                    // Nested union? Recurse
                    writeXmlParameterElements(member, result, isFirst, processedParams);
                } else {
                    // Simple member? This is tricky. 
                    // Usually Union member params are not named 'auth', they are the fields inside the type.
                    // If it's a simple type union e.g. string|int, we might treat it as a single parameter?
                    // But writeXmlParameterElements(member) would try to write parameter named after member value?
                    // For now, assume auth is Record|Record union (which is the case here).
                    writeXmlParameterElements(member, result, isFirst, processedParams);
                }
            }
        } else {
            // Generate single parameter element
            String sanitizedParamName = Utils.sanitizeParamName(functionParam.getValue());
            
            // Deduplicate: Don't write if already written
            if (processedParams.contains(sanitizedParamName)) {
                return;
            }
            
            String description = functionParam.getDescription() != null ? functionParam.getDescription() : "";
            if (!isFirst[0]) {
                result.append("\n    ");
            }
            result.append(String.format("<parameter name=\"%s\" description=\"%s\"/>",
                    sanitizedParamName, escapeXml(description)));
            isFirst[0] = false;
            processedParams.add(sanitizedParamName);
        }
    }

    /**
     * Escapes XML special characters for use in XML attribute values.
     * Note: Single quotes (') are NOT escaped because:
     * 1. XML attributes in our templates are enclosed in double quotes, so single quotes are safe
     * 2. &apos; is not universally supported (XML 1.0 doesn't include it, only XML 1.1)
     * 3. Ballerina uses single quotes in syntax (e.g., 'string' for singleton types) which should remain as-is
     * 
     * @param value The string value to escape
     * @return The escaped string safe for XML attribute values
     */
    private static String escapeXml(String value) {
        if (value == null) return "";
        // Escape in order: & first (to avoid double-escaping), then <, >, and "
        // Do NOT escape single quotes - they're safe in double-quoted attributes
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    /**
     * Writes record fields as separate UI elements instead of a single opaque text field.
     * Only used for config (init) parameters where expandRecords is true.
     * If the param is a RecordFunctionParam with extracted fields, each field is rendered
     * as its own input element. Nested record types are grouped into attributeGroups.
     * Otherwise, falls back to a single stringOrExpression field.
     */
    private static void writeRecordFields(FunctionParam functionParam, JsonTemplateBuilder builder,
                                          boolean expandRecords, String parentGroupName) throws IOException {
        if (functionParam instanceof RecordFunctionParam recordParam && !recordParam.getRecordFieldParams().isEmpty()) {
            List<FunctionParam> recordFields = recordParam.getRecordFieldParams();
            
            // Group fields by their immediate parent path segment for nested types
            java.util.Map<String, java.util.List<FunctionParam>> groupedFields = new java.util.LinkedHashMap<>();
            java.util.List<FunctionParam> topLevelFields = new java.util.ArrayList<>();
            
            for (FunctionParam fieldParam : recordFields) {
                String fieldName = fieldParam.getValue();
                String immediateParent = getImmediateParentSegment(fieldName);
                
                if (immediateParent != null && !immediateParent.isEmpty()) {
                    // This is a nested field - group it
                    groupedFields.computeIfAbsent(immediateParent, k -> new java.util.ArrayList<>()).add(fieldParam);
                } else {
                    // This is a top-level field (direct child of the record)
                    topLevelFields.add(fieldParam);
                }
            }
            
            // Write top-level fields first (not grouped)
            for (int i = 0; i < topLevelFields.size(); i++) {
                FunctionParam fieldParam = topLevelFields.get(i);
                
                // Propagate parent's enable condition to the field
                String parentCondition = functionParam.getEnableCondition();
                if (parentCondition != null && !parentCondition.isEmpty()) {
                    String currentCondition = fieldParam.getEnableCondition();
                    String mergedCondition = mergeEnableConditions(parentCondition, currentCondition);
                    fieldParam.setEnableCondition(mergedCondition);
                }
                
                writeJsonAttributeForFunctionParam(fieldParam, i, topLevelFields.size(), builder, false, expandRecords, parentGroupName);
            }
            
            // Write grouped nested fields
            int groupIndex = 0;
            int totalGroups = groupedFields.size();
            for (java.util.Map.Entry<String, java.util.List<FunctionParam>> groupEntry : groupedFields.entrySet()) {
                String groupName = groupEntry.getKey();
                java.util.List<FunctionParam> groupFields = groupEntry.getValue();
                
                // Add separator before group if there were top-level fields or previous groups
                if (!topLevelFields.isEmpty() || groupIndex > 0) {
                    builder.addSeparator(ATTRIBUTE_SEPARATOR);
                }
                
                // Create attributeGroup for this nested type with Title Case name
                String displayGroupName = camelCaseToTitleCase(groupName);

                // Check for duplicate group name (flatten if matches parent)
                if (parentGroupName != null && displayGroupName.equals(parentGroupName)) {
                    // Flatten the group - duplicate of parent
                    for (int i = 0; i < groupFields.size(); i++) {
                        FunctionParam fieldParam = groupFields.get(i);
                        
                        // Propagate parent's enable condition to the field
                        String parentCondition = functionParam.getEnableCondition();
                        if (parentCondition != null && !parentCondition.isEmpty()) {
                            String currentCondition = fieldParam.getEnableCondition();
                            String mergedCondition = mergeEnableConditions(parentCondition, currentCondition);
                            fieldParam.setEnableCondition(mergedCondition);
                        }

                        // Use groupName to strip the prefix
                        writeJsonAttributeForFunctionParam(fieldParam, i, groupFields.size(), builder, false, expandRecords, groupName);
                        
                        // Add separator if not last element
                         if (i < groupFields.size() - 1) {
                            builder.addSeparator(ATTRIBUTE_SEPARATOR);
                        }
                    }
                } else {
                    AttributeGroup attributeGroup = new AttributeGroup(displayGroupName);

                    // Propagate parent's enable condition to the group itself
                    // This ensures the group header is hidden when its parent union member is not selected
                    String parentCondition = functionParam.getEnableCondition();
                    if (parentCondition != null && !parentCondition.isEmpty()) {
                        attributeGroup.setEnableCondition(parentCondition);
                    }

                    builder.addFromTemplate(ATTRIBUTE_GROUP_TEMPLATE_PATH, attributeGroup);

                    // Write fields within this group
                    // The attributeGroup template ends with "elements": [ and a newline
                    // Add proper indentation (18 spaces) to align with the attribute template's content indentation
                    for (int i = 0; i < groupFields.size(); i++) {
                        FunctionParam fieldParam = groupFields.get(i);

                        // Propagate parent's enable condition to the field
                        // (parentCondition is already set above for the group)
                        if (parentCondition != null && !parentCondition.isEmpty()) {
                            String currentCondition = fieldParam.getEnableCondition();
                            String mergedCondition = mergeEnableConditions(parentCondition, currentCondition);
                            fieldParam.setEnableCondition(mergedCondition);
                        }



                        // Add indentation before the first attribute in the group
                        if (i == 0) {
                            builder.addSeparator("                  ");  // 18 spaces to align with template content
                        }
                        // Pass groupName to remove prefix from displayName
                        writeJsonAttributeForFunctionParam(fieldParam, i, groupFields.size(), builder, false, expandRecords, groupName);


                    }

                    // Close the attributeGroup - close elements array, value object, and attributeGroup object
                    // Match the indentation format from the expected output
                    builder.addSeparator("\n                    ]");
                    builder.addSeparator("\n                  }");
                }
                
                // Close attributeGroup with comma if there are more groups, otherwise just close it
                // The comma should be on the same line as the closing brace: },{
                // The next attributeGroup template starts with {, so we get },{ on the same line
                if (groupIndex < totalGroups - 1) {
                    // Add closing brace and comma on same line, then newline
                    builder.addSeparator("\n                },");
                } else {
                    // Only close if we created a group (not flattened). 
                    // Actually, the separator loop above handles "between groups". 
                    // But if we flattened, we didn't open a group brace.
                    // Wait, this loop structure separates "groups" with comma.
                    // If we flattened, we just added attributes.
                    // If the NEXT thing is a group, we need a comma.
                    // But `writeJsonAttributeForFunctionParam` adds separator internally?
                    // No, `writeJsonAttributeForFunctionParam` adds separator conditionally based on index.
                    
                    // ISSUE: If I flatten, I am injecting raw attributes.
                    // The "wrapper" logic (lines 1031-1035) assumes we emitted a JSON object (AttributeGroup) that needs a comma after it.
                    // But if we flattened, we emitted a series of objects, separated by commas (in loop).
                    // The last object in flattened group does NOT have a comma.
                    // If there is another group coming, we need a comma.
                    // The separators for attributes are handled by `writeJsonAttributeForFunctionParam`?
                    // No, line 705: `builder.addConditionalSeparator((index < paramLength - 1), ATTRIBUTE_SEPARATOR);`
                    // In my flattened loop above, I call it with `groupFields.size()`.
                    // So the last element will NOT have a comma.
                    // So if `groupIndex < totalGroups - 1`, we DO need a comma.
                    // But `builder.addSeparator("\n                },")` adds `},`. 
                    // But if we flattened, we don't have a closing brace `}`.
                    
                    // Logic fix:
                    if (!(parentGroupName != null && displayGroupName.equals(parentGroupName))) { 
                        // Only close brace if we didn't flatten
                        if (groupIndex < totalGroups - 1) {
                           builder.addSeparator("\n                },");
                        } else {
                           builder.addSeparator("\n                }");
                        }
                    } else {
                         // We flattened. If there are more groups, we need a comma.
                         if (groupIndex < totalGroups - 1) {
                            builder.addSeparator(ATTRIBUTE_SEPARATOR);
                         }
                    }
                }
                
                groupIndex++;
            }
        } else {
            // Fallback: treat as single stringOrExpression field if no field info available
            Attribute recordAttr = new Attribute(functionParam.getValue(), functionParam.getValue(),
                    INPUT_TYPE_STRING_OR_EXPRESSION, "", functionParam.isRequired(),
                    functionParam.getDescription(), "", "", false);
            recordAttr.setEnableCondition(functionParam.getEnableCondition());
            builder.addFromTemplate(ATTRIBUTE_TEMPLATE_PATH, recordAttr);
        }
    }
    
    /**
     * Writes a map parameter as a table UI with key and value columns.
     * For simple value types, creates a 2-column table (key, value).
     * For record value types, creates a multi-column table (key, field1, field2, ...).
     */
    private static void writeMapAsTable(MapFunctionParam mapParam, JsonTemplateBuilder builder, boolean isCombo)
            throws IOException {

        String paramName = Utils.sanitizeParamName(mapParam.getValue());
        String displayName = mapParam.getValue();
        if (displayName.contains(".")) {
            displayName = displayName.substring(displayName.lastIndexOf('.') + 1);
        }
        displayName = Utils.sanitizeParamName(displayName);

        List<Element> tableColumns = new ArrayList<>();

        // First column: key (always required for maps)
        Attribute keyColumn = new Attribute(
            "key",
            "Key",
            INPUT_TYPE_STRING_OR_EXPRESSION,
            "",
            true,
            "Key for the map entry",
            "",
            "",
            false
        );
        tableColumns.add(keyColumn);

        // Value column(s) based on map value type
        if (mapParam.getValueFieldParams() != null && !mapParam.getValueFieldParams().isEmpty()) {
            // Complex value (record) - create column for each field
            for (FunctionParam valueField : mapParam.getValueFieldParams()) {
                Attribute column = createAttributeForMapValueField(valueField);
                tableColumns.add(column);
            }
        } else {
            // Simple value (string, int, etc.)
            Attribute valueColumn = createSimpleValueColumn(mapParam);
            tableColumns.add(valueColumn);
        }

        String description = mapParam.getDescription() != null ?
            mapParam.getDescription() :
            "Configure " + displayName + " entries";

        // Set tableKey and tableValue to actual element names
        String tableKey = tableColumns.isEmpty() ? "" : tableColumns.get(0).getName();
        String tableValue = tableColumns.size() < 2 ? tableKey : tableColumns.get(tableColumns.size() - 1).getName();

        Table table = new Table(
            paramName,
            displayName,
            displayName,
            description,
            tableKey,
            tableValue,
            tableColumns,
            mapParam.getEnableCondition(),
            mapParam.isRequired()
        );

        builder.addFromTemplate(TABLE_TEMPLATE_PATH, table);
    }

    /**
     * Writes an array parameter as a table UI with element columns.
     * For simple element types, creates a single-column table.
     * For record element types, creates a multi-column table (field1, field2, ...).
     */
    private static void writeArrayAsTable(ArrayFunctionParam arrayParam, JsonTemplateBuilder builder, boolean isCombo)
            throws IOException {

        String paramName = Utils.sanitizeParamName(arrayParam.getValue());
        String displayName = arrayParam.getValue();
        if (displayName.contains(".")) {
            displayName = displayName.substring(displayName.lastIndexOf('.') + 1);
        }
        displayName = Utils.sanitizeParamName(displayName);

        List<Element> tableColumns = new ArrayList<>();

        // For arrays, we don't have a key column - just value column(s)
        if (arrayParam.getElementFieldParams() != null && !arrayParam.getElementFieldParams().isEmpty()) {
            // Complex element (record) - create column for each field
            for (FunctionParam elementField : arrayParam.getElementFieldParams()) {
                Attribute column = createAttributeForElementField(elementField);
                tableColumns.add(column);
            }
        } else {
            // Simple element (string, int, etc.)
            Attribute column = createSimpleElementColumn(arrayParam);
            tableColumns.add(column);
        }

        String description = arrayParam.getDescription() != null ?
            arrayParam.getDescription() :
            "Configure " + displayName + " entries";

        // Set tableKey and tableValue to actual element names
        String tableKey = tableColumns.isEmpty() ? "" : tableColumns.get(0).getName();
        String tableValue = tableColumns.size() < 2 ? tableKey : tableColumns.get(tableColumns.size() - 1).getName();

        Table table = new Table(
            paramName,
            displayName,
            displayName,
            description,
            tableKey,
            tableValue,
            tableColumns,
            arrayParam.getEnableCondition(),
            arrayParam.isRequired()
        );

        builder.addFromTemplate(TABLE_TEMPLATE_PATH, table);
    }

    /**
     * Writes a 2D array parameter as a nested table UI.
     * The outer table has a "Row Label" attribute and an inner table for the inner array elements.
     * For example, int[][] produces an outer table where each row has an inner table for entering integers.
     */
    private static void writeNestedArrayAsTable(ArrayFunctionParam arrayParam, JsonTemplateBuilder builder,
                                                 boolean isCombo) throws IOException {

        String paramName = Utils.sanitizeParamName(arrayParam.getValue());
        String displayName = arrayParam.getValue();
        if (displayName.contains(".")) {
            displayName = displayName.substring(displayName.lastIndexOf('.') + 1);
        }
        displayName = Utils.sanitizeParamName(displayName);

        // Build inner column based on inner element type
        TypeDescKind innerKind = arrayParam.getInnerElementTypeKind();
        if (innerKind == null) {
            TypeSymbol innerElementType = arrayParam.getInnerElementTypeSymbol();
            innerKind = innerElementType != null ? Utils.getActualTypeKind(innerElementType) : null;
        }

        String inputType = INPUT_TYPE_STRING_OR_EXPRESSION;
        String validateType = "";
        String matchPattern = "";
        String helpTip = "Value";

        if (innerKind != null) {
            switch (innerKind) {
                case INT:
                    validateType = VALIDATE_TYPE_REGEX;
                    matchPattern = INTEGER_REGEX;
                    helpTip = "Integer value";
                    break;
                case FLOAT, DECIMAL:
                    validateType = VALIDATE_TYPE_REGEX;
                    matchPattern = DECIMAL_REGEX;
                    helpTip = "Decimal value";
                    break;
                case BOOLEAN:
                    inputType = INPUT_TYPE_BOOLEAN;
                    helpTip = "Boolean value (true/false)";
                    break;
                default:
                    break;
            }
        }

        Attribute innerColumn = new Attribute("value", "Value", inputType, "", true,
                helpTip, validateType, matchPattern, false);
        List<Element> innerColumns = new ArrayList<>();
        innerColumns.add(innerColumn);

        Table innerTable = new Table(
            "innerArray",
            "Inner Array",
            "Inner Array",
            "Inner array elements",
            "value",
            "value",
            innerColumns,
            null,
            false
        );

        // Build the outer table with rowLabel attribute + inner table
        List<Element> outerElements = new ArrayList<>();
        Attribute rowLabel = new Attribute("rowLabel", "Row Label", INPUT_TYPE_STRING_OR_EXPRESSION,
                "", false, "Label for this row (optional)", "", "", false);
        outerElements.add(rowLabel);
        outerElements.add(innerTable);

        String description = arrayParam.getDescription() != null ?
            arrayParam.getDescription() :
            "Configure " + displayName + " entries";

        Table outerTable = new Table(
            paramName,
            displayName,
            displayName,
            description,
            "Row",
            "rowLabel",
            outerElements,
            arrayParam.getEnableCondition(),
            arrayParam.isRequired()
        );

        builder.addFromTemplate(TABLE_TEMPLATE_PATH, outerTable);
    }

    /**
     * Writes a union type array parameter as a table UI with a type dropdown and value field per row.
     * For example, (string|int)[] produces a table with a "type" combo column and a "value" text column.
     */
    private static void writeUnionArrayAsTable(ArrayFunctionParam arrayParam, JsonTemplateBuilder builder,
                                                boolean isCombo) throws IOException {

        String paramName = Utils.sanitizeParamName(arrayParam.getValue());
        String displayName = arrayParam.getValue();
        if (displayName.contains(".")) {
            displayName = displayName.substring(displayName.lastIndexOf('.') + 1);
        }
        displayName = Utils.sanitizeParamName(displayName);

        List<Element> tableColumns = new ArrayList<>();

        // Type selector column (combo dropdown with union member types)
        List<String> memberTypes = arrayParam.getUnionMemberTypeNames();
        if (memberTypes != null && !memberTypes.isEmpty()) {
            StringJoiner comboJoiner = new StringJoiner(",", "[", "]");
            for (String memberType : memberTypes) {
                comboJoiner.add("\"" + memberType + "\"");
            }
            String comboValues = comboJoiner.toString();
            String defaultType = memberTypes.get(0);

            Combo typeColumn = new Combo(
                "type",
                "Type",
                INPUT_TYPE_COMBO,
                comboValues,
                defaultType,
                true,
                null,
                "Select the data type for this element"
            );
            tableColumns.add(typeColumn);
        }

        // Value column (string input - no validation since type varies)
        Attribute valueColumn = new Attribute(
            "value",
            "Value",
            INPUT_TYPE_STRING_OR_EXPRESSION,
            "",
            true,
            "Element value",
            "",
            "",
            false
        );
        tableColumns.add(valueColumn);

        String description = arrayParam.getDescription() != null ?
            arrayParam.getDescription() :
            "Configure " + displayName + " entries";

        String tableKey = tableColumns.isEmpty() ? "" : tableColumns.get(0).getName();
        String tableValue = tableColumns.size() < 2 ? tableKey : tableColumns.get(tableColumns.size() - 1).getName();

        Table table = new Table(
            paramName,
            displayName,
            displayName,
            description,
            tableKey,
            tableValue,
            tableColumns,
            arrayParam.getEnableCondition(),
            arrayParam.isRequired()
        );

        builder.addFromTemplate(TABLE_TEMPLATE_PATH, table);
    }

    /**
     * Creates a simple value column for map parameters with primitive value types.
     */
    private static Attribute createSimpleValueColumn(MapFunctionParam mapParam) {
        TypeDescKind valueTypeKind = mapParam.getValueTypeKind();
        if (valueTypeKind == null) {
            TypeSymbol valueTypeSymbol = mapParam.getValueTypeSymbol();
            valueTypeKind = valueTypeSymbol != null ? Utils.getActualTypeKind(valueTypeSymbol) : TypeDescKind.STRING;
        }

        String inputType = INPUT_TYPE_STRING_OR_EXPRESSION;
        String validateType = "";
        String matchPattern = "";
        String helpTip = "Value for the map entry";

        // Set validation based on value type
        switch (valueTypeKind) {
            case INT:
                validateType = VALIDATE_TYPE_REGEX;
                matchPattern = INTEGER_REGEX;
                helpTip = "Integer value";
                break;
            case FLOAT, DECIMAL:
                validateType = VALIDATE_TYPE_REGEX;
                matchPattern = DECIMAL_REGEX;
                helpTip = "Decimal value";
                break;
            case BOOLEAN:
                inputType = INPUT_TYPE_BOOLEAN;
                helpTip = "Boolean value (true/false)";
                break;
            default:
                // String or other types - no validation
                break;
        }

        return new Attribute(
            "value",
            "Value",
            inputType,
            "",
            true,  // Values in maps are always required
            helpTip,
            validateType,
            matchPattern,
            false
        );
    }

    /**
     * Creates a simple element column for array parameters with primitive element types.
     */
    private static Attribute createSimpleElementColumn(ArrayFunctionParam arrayParam) {
        TypeDescKind elementTypeKind = arrayParam.getElementTypeKind();
        if (elementTypeKind == null) {
            TypeSymbol elementTypeSymbol = arrayParam.getElementTypeSymbol();
            elementTypeKind = elementTypeSymbol != null ? Utils.getActualTypeKind(elementTypeSymbol) : TypeDescKind.STRING;
        }

        String inputType = INPUT_TYPE_STRING_OR_EXPRESSION;
        String validateType = "";
        String matchPattern = "";
        String helpTip = "Array element";
        String columnName = "value";

        // Set validation based on element type
        switch (elementTypeKind) {
            case INT:
                validateType = VALIDATE_TYPE_REGEX;
                matchPattern = INTEGER_REGEX;
                helpTip = "Integer value";
                columnName = "value";
                break;
            case FLOAT, DECIMAL:
                validateType = VALIDATE_TYPE_REGEX;
                matchPattern = DECIMAL_REGEX;
                helpTip = "Decimal value";
                columnName = "value";
                break;
            case BOOLEAN:
                inputType = INPUT_TYPE_BOOLEAN;
                helpTip = "Boolean value (true/false)";
                columnName = "value";
                break;
            default:
                // String or other types - no validation
                columnName = "value";
                break;
        }

        return new Attribute(
            columnName,
            StringUtils.capitalize(columnName),
            inputType,
            "",
            true,  // Array elements are always required
            helpTip,
            validateType,
            matchPattern,
            false
        );
    }

    /**
     * Creates an attribute column for a record field in a map value.
     */
    private static Attribute createAttributeForMapValueField(FunctionParam fieldParam) {
        String fieldName = fieldParam.getValue();
        String displayName = StringUtils.capitalize(fieldName);
        String inputType = INPUT_TYPE_STRING_OR_EXPRESSION;
        String validateType = "";
        String matchPattern = "";
        String helpTip = fieldParam.getDescription() != null ? fieldParam.getDescription() : displayName;

        TypeDescKind fieldTypeKind = fieldParam.getResolvedTypeKind();
        if (fieldTypeKind == null) {
            TypeSymbol ts = fieldParam.getTypeSymbol();
            fieldTypeKind = ts != null ? Utils.getActualTypeKind(ts) : TypeDescKind.STRING;
        }

        // Set validation based on field type
        switch (fieldTypeKind) {
            case INT:
                validateType = fieldParam.isRequired() ? VALIDATE_TYPE_REGEX : VALIDATE_TYPE_REGEX;
                matchPattern = fieldParam.isRequired() ? INTEGER_REGEX : INTEGER_REGEX_OPTIONAL;
                break;
            case FLOAT, DECIMAL:
                validateType = fieldParam.isRequired() ? VALIDATE_TYPE_REGEX : VALIDATE_TYPE_REGEX;
                matchPattern = fieldParam.isRequired() ? DECIMAL_REGEX : DECIMAL_REGEX_OPTIONAL;
                break;
            case BOOLEAN:
                inputType = INPUT_TYPE_BOOLEAN;
                break;
            default:
                // String or other types - no validation
                break;
        }

        return new Attribute(
            fieldName,
            displayName,
            inputType,
            "",
            fieldParam.isRequired(),
            helpTip,
            validateType,
            matchPattern,
            false
        );
    }

    /**
     * Creates an attribute column for a record field in an array element.
     */
    private static Attribute createAttributeForElementField(FunctionParam fieldParam) {
        String fieldName = fieldParam.getValue();
        String displayName = StringUtils.capitalize(fieldName);
        String inputType = INPUT_TYPE_STRING_OR_EXPRESSION;
        String validateType = "";
        String matchPattern = "";
        String helpTip = fieldParam.getDescription() != null ? fieldParam.getDescription() : displayName;

        TypeDescKind fieldTypeKind = fieldParam.getResolvedTypeKind();
        if (fieldTypeKind == null) {
            TypeSymbol ts = fieldParam.getTypeSymbol();
            fieldTypeKind = ts != null ? Utils.getActualTypeKind(ts) : TypeDescKind.STRING;
        }

        // Set validation based on field type
        switch (fieldTypeKind) {
            case INT:
                validateType = fieldParam.isRequired() ? VALIDATE_TYPE_REGEX : VALIDATE_TYPE_REGEX;
                matchPattern = fieldParam.isRequired() ? INTEGER_REGEX : INTEGER_REGEX_OPTIONAL;
                break;
            case FLOAT, DECIMAL:
                validateType = fieldParam.isRequired() ? VALIDATE_TYPE_REGEX : VALIDATE_TYPE_REGEX;
                matchPattern = fieldParam.isRequired() ? DECIMAL_REGEX : DECIMAL_REGEX_OPTIONAL;
                break;
            case BOOLEAN:
                inputType = INPUT_TYPE_BOOLEAN;
                break;
            default:
                // String or other types - no validation
                break;
        }

        return new Attribute(
            fieldName,
            displayName,
            inputType,
            "",
            fieldParam.isRequired(),
            helpTip,
            validateType,
            matchPattern,
            false
        );
    }

    /**
     * Converts camelCase or PascalCase string to Title Case with spaces.
     * For example:
     * - "authConfig" -> "Auth Config"
     * - "credentialsConfig" -> "Credentials Config"
     * - "http1Settings" -> "Http1 Settings"
     *
     * @param camelCase The camelCase string to convert
     * @return The Title Case string with spaces
     */
    private static String camelCaseToTitleCase(String camelCase) {
        if (camelCase == null || camelCase.isEmpty()) {
            return camelCase;
        }
        // Insert space before uppercase letters (except at the start) and capitalize first letter
        String result = camelCase.replaceAll("([a-z])([A-Z])", "$1 $2")
                                  .replaceAll("([A-Z])([A-Z][a-z])", "$1 $2");
        return StringUtils.capitalize(result);
    }

    /**
     * Removes the group name prefix from a field name.
     * For nested fields, finds the group name segment in the path and removes everything up to and including it.
     * For example:
     * - "authConfig.token" with group "authConfig" -> "token"
     * - "credentialsConfig.username" with group "credentialsConfig" -> "username"
     * - "http1Settings.proxy.host" with group "proxy" -> "host"
     * - "http1Settings.keepAlive" with group "http1Settings" -> "keepAlive"
     *
     * @param fieldName The full field name
     * @param groupName The group name to remove (can be anywhere in the path)
     * @return The field name without the group prefix
     */
    private static String removeGroupPrefix(String fieldName, String groupName) {
        if (fieldName == null || groupName == null) {
            return fieldName;
        }
        String prefix = groupName + ".";
        
        // Check if field starts with group prefix (direct child)
        if (fieldName.startsWith(prefix)) {
            return fieldName.substring(prefix.length());
        }
        
        // Check if group name appears as a segment in the path (nested group)
        // e.g., "http1Settings.proxy.host" contains ".proxy." when group is "proxy"
        String groupSegment = "." + prefix;
        int groupIndex = fieldName.indexOf(groupSegment);
        if (groupIndex >= 0) {
            // Remove everything up to and including the group segment
            return fieldName.substring(groupIndex + groupSegment.length());
        }
        
        // Group name not found in the path, return as-is
        return fieldName;
    }

    /**
     * Extracts the immediate parent segment from a qualified field name.
     * For example:
     * - "authConfig.token" -> "authConfig"
     * - "credentialsConfig.username" -> "credentialsConfig"
     * - "secureConfig.clientKeyPath" -> "secureConfig"
     * - "idleTimeout" -> null (top-level field, no dots)
     * - "http1Settings.proxy.host" -> "proxy"
     *
     * @param qualifiedName The qualified field name (e.g., "authConfig.token")
     * @return The immediate parent segment, or null if it's a top-level field
     */
    private static String getImmediateParentSegment(String qualifiedName) {
        if (qualifiedName == null || qualifiedName.isEmpty()) {
            return null;
        }

        int lastDotIndex = qualifiedName.lastIndexOf('.');
        if (lastDotIndex == -1) {
            // No dots - this is a top-level field
            return null;
        }

        // Get the part before the last dot
        String beforeLastDot = qualifiedName.substring(0, lastDotIndex);

        // Find the second-to-last dot to get the immediate parent
        int secondLastDotIndex = beforeLastDot.lastIndexOf('.');
        if (secondLastDotIndex == -1) {
            // Only one dot - the parent is the first segment (this is a direct nested field)
            return beforeLastDot;
        }

        // Extract the segment between the second-to-last and last dot
        return beforeLastDot.substring(secondLastDotIndex + 1);
    }

    // Existing methods can now call the new generic method
    public static void generateXml(String templateName, String outputName, ModelElement element) {
        generateFile(templateName, outputName, element, "xml");
    }

    public static void generateJson(String templateName, String outputName, ModelElement element) {
        generateFile(templateName, outputName, element, "json");
    }

    public static void generateXmlForConnector(String templatePath, String templateName, String outputName, ModelElement element) {
        generateFileForConnector(templatePath, templateName, outputName, element, "xml");
    }

    public static void generateJsonForConnector(String templatePath, String templateName, String outputName, ModelElement element) {
        generateFileForConnector(templatePath, templateName, outputName, element, "json");
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
            // Copy dependency JARs (mi-native, module-core, etc.) from resources/lib to MI connector lib/
            copyResources(classLoader, fs, destination, Connector.LIB_PATH, ".jar");
            copyIcons(classLoader, fs, destination);
        }
    }

    /**
     * This is mediator class copy private utility method
     * Copies mediator classes from mi-native dependency
     */
    private static void copyMediatorClasses(ClassLoader classLoader, FileSystem fs, Path destination, String org,
                                            String module, String moduleVersion)
            throws IOException {
        // Read mediator classes from mi-native dependency (io/ballerina/stdlib/mi)
        List<Path> paths = Files.walk(fs.getPath("io/ballerina/stdlib/mi"))
                .filter(f -> f.toString().endsWith(".class"))
                .toList();

        for (Path path : paths) {
            Path relativePath = fs.getPath("io/ballerina/stdlib/mi").relativize(path);
            Path outputPath = destination.resolve(relativePath.toString());
            Files.createDirectories(outputPath.getParent()); // Create parent directories if they don't exist
            InputStream inputStream = getFileFromResourceAsStream(classLoader, path.toString());
            if (path.getFileName().toString().contains("ModuleInfo.class")) {
//                updateConstants(inputStream, outputPath.toString(), org, module, moduleVersion);
            } else {
                Files.copy(inputStream, outputPath, StandardCopyOption.REPLACE_EXISTING);
            }
            inputStream.close();
        }
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
        try (InputStream inputStream = Files.newInputStream(iconPath)) {
            Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
        }
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
        Files.copy(inputStream, outputPath, StandardCopyOption.REPLACE_EXISTING);
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

    private static void writeAttributeGroup(String groupName, List<FunctionParam> params, boolean isLastGroup, JsonTemplateBuilder builder) {
        writeAttributeGroup(groupName, params, isLastGroup, builder, false);
    }

    private static void writeAttributeGroup(String groupName, List<FunctionParam> params, boolean isLastGroup, JsonTemplateBuilder builder, boolean collapsed) {
        try {
            AttributeGroup attributeGroup = new AttributeGroup(groupName, collapsed);
            builder.addFromTemplate(ATTRIBUTE_GROUP_TEMPLATE_PATH, attributeGroup);

            for (int i = 0; i < params.size(); i++) {
                if (i == 0) {
                    builder.addSeparator("                  "); // Indentation alignment
                }
                // Write param - expand records
                writeJsonAttributeForFunctionParam(params.get(i), i, params.size(), builder, false, true, groupName);
            }

            // Close the attributeGroup
            builder.addSeparator("\n                    ]");
            builder.addSeparator("\n                  }");

            if (isLastGroup) {
                builder.addSeparator("\n                }");
            } else {
                builder.addSeparator("\n                },");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    /**
     * Merges two enable conditions (JSON arrays of objects) into a single condition.
     * Assumes simple [{"key":"value"}] format generated by ParamFactory.
     * Merges by combining the objects: [{"p":"v"}] + [{"c":"v"}] -> [{"p":"v","c":"v"}]
     */
    private static String mergeEnableConditions(String parentCondition, String childCondition) {
        if (parentCondition == null || parentCondition.isEmpty()) {
            return childCondition;
        }
        if (childCondition == null || childCondition.isEmpty()) {
            return parentCondition;
        }

        // Strip outer [{ and }]
        // Valid condition format from ParamFactory is strict: [{"key":"val"}]
        // So we can safely substring if checking length
        if (parentCondition.length() > 4 && childCondition.length() > 4) {
            String parentContent = parentCondition.substring(2, parentCondition.length() - 2);
            String childContent = childCondition.substring(2, childCondition.length() - 2);
            return "[{" + parentContent + "," + childContent + "}]";
        }
        
        // Fallback for unexpected format - return parent condition to be safe (hides if parent hidden)
        return parentCondition;
    }

    /**
     * Updates the enable conditions of union members when the union parameter itself is renamed.
     * This ensures that the member conditions (which reference the union param name) remain valid.
     */
    private static void updateEnableConditionsForUnionMembers(UnionFunctionParam unionParam, String oldName, String newName) {
        String oldConditionKey = Utils.sanitizeParamName(oldName) + "DataType";
        String newConditionKey = Utils.sanitizeParamName(newName) + "DataType";

        for (FunctionParam member : unionParam.getUnionMemberParams()) {
             String condition = member.getEnableCondition();
             if (condition != null && !condition.isEmpty()) {
                 // Condition format: [{"key":"value"}]
                 // We safely replace the quoted key
                 member.setEnableCondition(condition.replace("\"" + oldConditionKey + "\"", "\"" + newConditionKey + "\""));
             }
        }
    }
}
