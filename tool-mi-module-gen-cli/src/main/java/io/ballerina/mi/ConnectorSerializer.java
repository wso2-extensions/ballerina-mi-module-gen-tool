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
import java.util.Optional;
import java.util.StringJoiner;

import static io.ballerina.mi.util.Constants.*;
import static io.ballerina.mi.util.Constants.RECORD;

public class ConnectorSerializer {

    private static final String CONFIG_TEMPLATE_PATH = "balConnector" + File.separator + "config";
    private static final String FUNCTION_TEMPLATE_PATH = "balConnector" + File.separator + "functions";

    private final PrintStream printStream;
    private final Path sourcePath;
    private final Path targetPath;

    public ConnectorSerializer(Path sourcePath, Path targetPath) {
        this.sourcePath = sourcePath;
        this.targetPath = targetPath;
        this.printStream = System.out;
    }

    public void serialize(Connector connector) {

        try {
            Path destinationPath = targetPath.resolve("generated");
            if (Files.exists(destinationPath)) {
                FileUtils.cleanDirectory(destinationPath.toFile());
            } else {
                Files.createDirectories(destinationPath);
            }
            generateXmlFiles(destinationPath, connector);
            //TODO: Do output schema generation
            generateJsonFiles(destinationPath, connector);
            URI jarPath = getClass().getProtectionDomain().getCodeSource().getLocation().toURI();
            copyResources(getClass().getClassLoader(), destinationPath, jarPath, connector.getOrgName(),
                    connector.getModuleName(), connector.getMajorVersion());

            String zipFilePath;
            if (connector.isBalModule()) {
                Files.copy(targetPath.resolve("bin").resolve(connector.getModuleName() + ".jar"), destinationPath.resolve(Connector.LIB_PATH).resolve(connector.getModuleName() + ".jar"));
                zipFilePath = targetPath.toAbsolutePath().getParent().resolve(connector.getZipFileName()).toString();
            } else {
                Path generatedArtifactPath = Paths.get(System.getProperty(Constants.CONNECTOR_TARGET_PATH));
                Files.copy(generatedArtifactPath, destinationPath.resolve(Connector.LIB_PATH).resolve(generatedArtifactPath.getFileName()));
                zipFilePath = generatedArtifactPath.getParent().getParent().resolve(connector.getZipFileName()).toString();
            }
            Utils.zipFolder(destinationPath, zipFilePath);
//            Utils.deleteDirectory(destinationPath);
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private static void generateXmlFiles(Path connectorFolderPath, Connector connector) {
        File connectorFolder = new File(connectorFolderPath.toUri());
        if (!connectorFolder.exists()) {
            connectorFolder.mkdir();
        }
        // Generate the connector.xml
        connector.generateInstanceXml(connectorFolder);
        // Generate the component.xml for functions
        connector.generateFunctionsXml(connectorFolder, Constants.FUNCTION_TEMPLATE_PATH, "functions");
        if (!connector.isBalModule()) {
            // Generate the config/component.xml
            connector.generateConfigInstanceXml(connectorFolder, CONFIG_TEMPLATE_PATH, "config");
            // Generate the init.xml from config_template.xml
            connector.generateConfigTemplateXml(connectorFolder, CONFIG_TEMPLATE_PATH, "config");
        }
        for (Connection connection : connector.getConnections()) {
            for (Component component : connection.getComponents()) {
                component.generateTemplateXml(connectorFolder, FUNCTION_TEMPLATE_PATH, "functions");
            }
        }
    }

    private static void generateJsonFiles(Path connectorFolderPath, Connector connector) {
        File connectorFolder = new File(connectorFolderPath.toUri());
        for (Connection connection : connector.getConnections()) {
            // TODO: revisit this
            if (connection.getInitComponent() != null) {
                // Generate config JSON - filename sanitization is handled in generateFileForConnector
                // Config files use exact connectionType (no PascalCase) to match connectionName
                connection.getInitComponent().generateUIJson(connectorFolder, CONFIG_TEMPLATE_PATH,
                        connection.getConnectionType());
            }
            for (Component component : connection.getComponents()) {
                component.generateUIJson(connectorFolder, FUNCTION_TEMPLATE_PATH, component.getName());
                //TODO: Generate output schemas
//            component.generateOutputSchemaJson(connectorFolder);
            }
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
            Handlebars handlebar = new Handlebars();
            String templateFileName = String.format("%s.%s", templateName, extension);
            String content = Utils.readFile(templateFileName);
            Template template = handlebar.compileInline(content);
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
            Handlebars handlebar = new Handlebars();
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
                return true; // default value if context is not boolean
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
                // Write path parameters first (pathParam0, pathParam1, etc.)
                List<PathParamType> pathParams = component.getPathParams();
                for (int i = 0; i < pathParams.size(); i++) {
                    writeComponentXmlPathProperty(pathParams.get(i), i, result, i == 0);
                }
                // Then write query parameters (queryParam0, queryParam1, etc.)
                List<Type> queryParams = component.getQueryParams();
                for (int i = 0; i < queryParams.size(); i++) {
                    writeComponentXmlQueryProperty(queryParams.get(i), i, result);
                }
                if (templatePath.equals(FUNCTION_TEMPLATE_PATH)) {
                    // Only add indentation if there are already properties written
                    // (Handlebars adds indentation to the first line automatically)
                    boolean hasPreviousProperties = !pathParams.isEmpty() || !queryParams.isEmpty();
                    String indent = hasPreviousProperties ? "        " : "";
                    result.append(String.format("%s<property name=\"returnType\" value=\"%s\"/>\n",
                            indent, component.getReturnType()));
                }
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
                for (FunctionParam functionParam : functionParams) {
                    writeXmlParameterElements(functionParam, result, isFirst);
                }
                // Remove trailing newline and indentation
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
                    int[] indexHolder = {0};  // Use array to allow modification in lambda
                    boolean[] isFirst = {true};
                    for (FunctionParam functionParam : functionParams) {
                        writeXmlParamProperties(functionParam, connection.getConnectionType().toUpperCase(), result, indexHolder, isFirst);
                    }
                }
                // Ensure output ends with a newline for proper formatting
                String output = result.toString();
                if (!output.isEmpty() && !output.endsWith("\n")) {
                    output = output + "\n";
                }
                return new Handlebars.SafeString(output);
            });
            handlebar.registerHelper("writeConfigJsonProperties", (context, options) -> {
                Component component = (Component) context;
                JsonTemplateBuilder builder = new JsonTemplateBuilder();
                List<FunctionParam> functionParams = component.getFunctionParams();

                // Split parameters into Basic and Advanced
                List<FunctionParam> basicParams = new ArrayList<>();
                List<FunctionParam> advancedParams = new ArrayList<>();

                // Flatten parameters: if a param is a record, extract its fields
                List<FunctionParam> flattenedParams = new ArrayList<>();
                for (FunctionParam param : functionParams) {
                    if (param instanceof RecordFunctionParam recordParam && !recordParam.getRecordFieldParams().isEmpty()) {
                        flattenedParams.addAll(recordParam.getRecordFieldParams());
                    } else {
                        flattenedParams.add(param);
                    }
                }

                for (FunctionParam param : flattenedParams) {
                    // Advanced params are optional records or params with default values or optional fields
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

                // Write Basic Group
                if (!basicParams.isEmpty()) {
                    writeAttributeGroup("Basic", basicParams, advancedParams.isEmpty(), builder);
                }

                // Write Advanced Group
                if (!advancedParams.isEmpty()) {
                    writeAttributeGroup("Advanced", advancedParams, true, builder, true);
                }

                return new Handlebars.SafeString(builder.build());
            });
            handlebar.registerHelper("writeComponentJsonProperties", (context, options) -> {
                Component component = (Component) context;
                JsonTemplateBuilder builder = new JsonTemplateBuilder();

                // First, add path parameters as input elements
                List<PathParamType> pathParams = component.getPathParams();
                int totalPathParams = pathParams.size();
                for (int i = 0; i < totalPathParams; i++) {
                    PathParamType pathParam = pathParams.get(i);
                    writeJsonAttributeForPathParam(pathParam, i, totalPathParams, builder);
                    // Add separator if not the last path param or if there are function params
                    if (i < totalPathParams - 1 || !component.getFunctionParams().isEmpty()) {
                        builder.addSeparator(ATTRIBUTE_SEPARATOR);
                    }
                }

                // Then, add regular function parameters
                List<FunctionParam> functionParams = component.getFunctionParams();
                for (FunctionParam functionParam : functionParams) {
                    // Do NOT expand records for regular function parameters
                    writeJsonAttributeForFunctionParam(functionParam, functionParams.indexOf(functionParam),
                            functionParams.size(), builder, false, false, null);
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
                // Replace dots with underscores
                return new Handlebars.SafeString(moduleName.replace(".", "_"));
            }));
            String templateFileName = String.format("%s/%s.%s", templatePath, templateName, extension);
            String content = Utils.readFile(templateFileName);
            Template template = handlebar.compileInline(content);
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
        String paramValue = functionParam.getValue();

        
        // For display and ID generation, remove group prefix if we're in a group context
        // For nested fields like "http1Settings.proxy.host" in "Proxy" group, 
        // remove everything up to and including the group name segment
        if (groupName != null && !groupName.isEmpty() && paramValue != null) {
            paramValue = removeGroupPrefix(paramValue, groupName);
        }
        
        String sanitizedParamName = Utils.sanitizeParamName(paramValue);
        String displayName = paramValue;
        
        // Ensure display name is friendly (not fully qualified) by taking the last segment
        if (displayName.contains(".")) {
             displayName = displayName.substring(displayName.lastIndexOf('.') + 1);
        }
        
        // Apply sanitizeParamName to displayName as well to remove leading quotes
        // (e.g., 'limit -> limit) to avoid breaking Synapse XML parsing
        displayName = Utils.sanitizeParamName(displayName);

        
        String defaultValue = functionParam.getDefaultValue() != null ? functionParam.getDefaultValue() : "";
        switch (paramType) {
            case STRING, XML, JSON, MAP, ARRAY:
                Attribute stringAttr = new Attribute(sanitizedParamName, displayName, INPUT_TYPE_STRING_OR_EXPRESSION,
                        defaultValue, functionParam.isRequired(), functionParam.getDescription(), "",
                        "", isCombo);
                stringAttr.setEnableCondition(functionParam.getEnableCondition());
                builder.addFromTemplate(ATTRIBUTE_TEMPLATE_PATH, stringAttr);
                break;
            case RECORD:
                if (expandRecords) {
                    writeRecordFields(functionParam, builder, expandRecords, groupName);
                } else {
                    // Treat as single stringOrExpression field for regular function params
                    Attribute recordAttr = new Attribute(functionParam.getValue(), displayName,
                            INPUT_TYPE_STRING_OR_EXPRESSION, defaultValue, functionParam.isRequired(),
                            functionParam.getDescription(), "", "", isCombo);
                    recordAttr.setEnableCondition(functionParam.getEnableCondition());
                    builder.addFromTemplate(ATTRIBUTE_TEMPLATE_PATH, recordAttr);
                }
                break;
            case INT:
                Attribute intAttr = new Attribute(functionParam.getValue(), displayName, INPUT_TYPE_STRING_OR_EXPRESSION,
                        defaultValue, functionParam.isRequired(), functionParam.getDescription(), VALIDATE_TYPE_REGEX,
                        INTEGER_REGEX, isCombo);
                intAttr.setEnableCondition(functionParam.getEnableCondition());
                builder.addFromTemplate(ATTRIBUTE_TEMPLATE_PATH, intAttr);
                break;
            case DECIMAL, FLOAT:
                Attribute decAttr = new Attribute(functionParam.getValue(), displayName,
                        INPUT_TYPE_STRING_OR_EXPRESSION, defaultValue, functionParam.isRequired(),
                        functionParam.getDescription(), VALIDATE_TYPE_REGEX, DECIMAL_REGEX, isCombo);
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
                    // If groupName is not provided but we're in a group context, detect it from the field path
                    String effectiveGroupName = groupName;
                    if (effectiveGroupName == null && paramValue != null && paramValue.contains(".")) {
                        String immediateParent = getImmediateParentSegment(paramValue);
                        if (immediateParent != null) {
                            effectiveGroupName = immediateParent;
                        }
                    }
                    
                    Combo comboField = getComboField(unionFunctionParam, functionParam.getValue(),
                            functionParam.getDescription(), effectiveGroupName);
                    builder.addFromTemplate(COMBO_TEMPLATE_PATH, comboField);

                    // Add attribute fields for each type with enable conditions
                    // Filter out union members that are themselves empty unions (would produce no output)
                    List<FunctionParam> unionMembers = unionFunctionParam.getUnionMemberParams();
                    List<FunctionParam> validMembers = new ArrayList<>();
                    for (FunctionParam member : unionMembers) {
                        // Skip nested unions that have no members (would produce no output)
                        if (member instanceof UnionFunctionParam nestedUnion && nestedUnion.getUnionMemberParams().isEmpty()) {
                            continue;
                        }
                        validMembers.add(member);
                    }

                    // Add separator after combo field if there are valid members
                    if (!validMembers.isEmpty()) {
                        builder.addSeparator(ATTRIBUTE_SEPARATOR);
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
                                for (FunctionParam field : recordParam.getRecordFieldParams()) {
                                    // Propagate member condition to field - essential because virtualRecordParam
                                    // will only hold the union's general condition, not the specific member selection condition
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

    private static Combo getComboField(UnionFunctionParam unionFunctionParam, String paramName, String helpTip, String groupName) {
        List<FunctionParam> unionMembers = unionFunctionParam.getUnionMemberParams();
        StringJoiner unionJoiner = new StringJoiner(",", "[", "]");
        for (int i = 0; i < unionMembers.size(); i++) {
            FunctionParam member = unionMembers.get(i);
            String comboItem;
            if (member.getParamType().equals(RECORD)) {
                comboItem = member.getTypeSymbol().getName().orElse("Record" + i);
            } else if (member.getParamType().equals(UNION)) {
                // For union types, use type name if available, otherwise use indexed name
                comboItem = member.getTypeSymbol().getName().orElse("Union" + i);
            } else {
                comboItem = member.getParamType();
            }
            unionJoiner.add("\"" + comboItem + "\"");
        }
        String unionComboValues = unionJoiner.toString();
        FunctionParam firstMember = unionMembers.getFirst();
        String defaultValue;
        if (firstMember.getParamType().equals(RECORD)) {
            defaultValue = firstMember.getTypeSymbol().getName().orElseThrow();
        } else if (firstMember.getParamType().equals(UNION)) {
            // For union types, use type name if available, otherwise use "union"
            defaultValue = firstMember.getTypeSymbol().getName().orElse(UNION);
        } else {
            defaultValue = firstMember.getParamType();
        }
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
            comboDisplayName = String.format("%s%s", Utils.sanitizeParamName(displayParamName), "DataType");
        }
        
        return new Combo(comboName, comboDisplayName, INPUT_TYPE_COMBO, unionComboValues, defaultValue,
                unionFunctionParam.isRequired(), unionFunctionParam.getEnableCondition(), helpTip);
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
        // If this is a nested record, recursively expand its fields
        if (fieldParam instanceof RecordFunctionParam nestedRecordParam && !nestedRecordParam.getRecordFieldParams().isEmpty()) {
            // Recursively expand nested record fields
            for (FunctionParam nestedFieldParam : nestedRecordParam.getRecordFieldParams()) {
                writeRecordFieldParamProperties(nestedFieldParam, connectionType, recordParamName, result, fieldIndexHolder);
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
            fieldIndexHolder[0]++;
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
            fieldIndexHolder[0]++;
        }
    }

    /**
     * Writes XML parameter elements for function params, expanding record fields as separate parameters.
     */
    private static void writeXmlParameterElements(FunctionParam functionParam, StringBuilder result, boolean[] isFirst) {
        if (functionParam instanceof RecordFunctionParam recordParam && !recordParam.getRecordFieldParams().isEmpty()) {
            // Expand record fields as separate parameters
            for (FunctionParam fieldParam : recordParam.getRecordFieldParams()) {
                writeXmlParameterElements(fieldParam, result, isFirst);
            }
        } else {
            // Generate single parameter element
            String description = functionParam.getDescription() != null ? functionParam.getDescription() : "";
            if (!isFirst[0]) {
                result.append("\n    ");
            }
            String sanitizedParamName = Utils.sanitizeParamName(functionParam.getValue());
            result.append(String.format("<parameter name=\"%s\" description=\"%s\"/>",
                    sanitizedParamName, escapeXml(description)));
            isFirst[0] = false;
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
                    builder.addFromTemplate(ATTRIBUTE_GROUP_TEMPLATE_PATH, attributeGroup);

                    // Write fields within this group
                    // The attributeGroup template ends with "elements": [ and a newline
                    // Add proper indentation (18 spaces) to align with the attribute template's content indentation
                    for (int i = 0; i < groupFields.size(); i++) {
                        FunctionParam fieldParam = groupFields.get(i);

                        // Propagate parent's enable condition to the field
                        String parentCondition = functionParam.getEnableCondition();
                        if (parentCondition != null && !parentCondition.isEmpty()) {
                            String currentCondition = fieldParam.getEnableCondition();
                            String mergedCondition = mergeEnableConditions(parentCondition, currentCondition);
                            fieldParam.setEnableCondition(mergedCondition);
                        }

                        // Remove the group name prefix from the field name
                        String originalFieldName = fieldParam.getValue();
                        String shortFieldName = removeGroupPrefix(originalFieldName, groupName);

                        // For UnionFunctionParam, we need to update the enable conditions of its members
                        // because the combo field name (derived from param name) is changing.
                        boolean isUnion = fieldParam instanceof UnionFunctionParam;
                        String savedFieldName = fieldParam.getValue();
                        
                        if (isUnion) {
                            updateEnableConditionsForUnionMembers((UnionFunctionParam) fieldParam, savedFieldName, shortFieldName);
                        }
                        fieldParam.setValue(shortFieldName);

                        // Add indentation before the first attribute in the group
                        if (i == 0) {
                            builder.addSeparator("                  ");  // 18 spaces to align with template content
                        }
                        // Pass groupName to remove prefix from displayName
                        writeJsonAttributeForFunctionParam(fieldParam, i, groupFields.size(), builder, false, expandRecords, groupName);

                        // Restore original field name and conditions
                        fieldParam.setValue(savedFieldName);
                        if (isUnion) {
                            updateEnableConditionsForUnionMembers((UnionFunctionParam) fieldParam, shortFieldName, savedFieldName);
                        }
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
        InputStream inputStream = Files.newInputStream(iconPath);
        Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
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
