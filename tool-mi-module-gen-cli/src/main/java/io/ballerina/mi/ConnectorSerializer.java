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
                List<Type> queryParams = component.getQueryParams();
                for (int i = 0; i < queryParams.size(); i++) {
                    writeComponentXmlQueryProperty(queryParams.get(i), i, result);
                }
                List<PathParamType> pathParams = component.getPathParams();
                for (int i = 0; i < pathParams.size(); i++) {
                    writeComponentXmlPathProperty(pathParams.get(i), i, result);
                }
                if (templatePath.equals(FUNCTION_TEMPLATE_PATH)) {
                    result.append(String.format("<property name=\"returnType\" value=\"%s\"/>\n",
                            component.getReturnType()));
                }
                return new Handlebars.SafeString(result.toString());
            });
            handlebar.registerHelper("writeConfigXmlParameters", (context, options) -> {
                @SuppressWarnings("unchecked")
                List<FunctionParam> functionParams = (List<FunctionParam>) context;
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
                // Remove trailing newline if present
                String output = result.toString();
                if (output.endsWith("\n")) {
                    output = output.substring(0, output.length() - 1);
                }
                return new Handlebars.SafeString(output);
            });
            handlebar.registerHelper("writeConfigJsonProperties", (context, options) -> {
                Component component = (Component) context;
                JsonTemplateBuilder builder = new JsonTemplateBuilder();
                List<FunctionParam> functionParams = component.getFunctionParams();
                for (FunctionParam functionParam : functionParams) {
                    // Expand records for config (init) parameters
                    writeJsonAttributeForFunctionParam(functionParam, functionParams.indexOf(functionParam),
                            functionParams.size(), builder, false, true);
                }
                return new Handlebars.SafeString(builder.build());
            });
            handlebar.registerHelper("writeComponentJsonProperties", (context, options) -> {
                Component component = (Component) context;
                JsonTemplateBuilder builder = new JsonTemplateBuilder();
                List<FunctionParam> functionParams = component.getFunctionParams();
                for (FunctionParam functionParam : functionParams) {
                    // Do NOT expand records for regular function parameters
                    writeJsonAttributeForFunctionParam(functionParam, functionParams.indexOf(functionParam),
                            functionParams.size(), builder, false, false);
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
            String templateFileName = String.format("%s/%s.%s", templatePath, templateName, extension);
            String content = Utils.readFile(templateFileName);
            Template template = handlebar.compileInline(content);
            String output = template.apply(element);
            String outputFileName = String.format("%s.%s", outputName, extension);
            Utils.writeFile(outputFileName, output);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void writeComponentXmlPathProperty(PathParamType parameter, int index, StringBuilder result) {
        result.append(String.format("<property name=\"pathParam%d\" value=\"%s\"/>\n", index, parameter.name));
        result.append(String.format("<property name=\"pathParamType%d\" value=\"%s\"/>\n", index, parameter.typeName));
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
        String paramType = functionParam.getParamType();
        String displayName = functionParam.getValue();
        String defaultValue = functionParam.getDefaultValue() != null ? functionParam.getDefaultValue() : "";
        switch (paramType) {
            case STRING, XML, JSON, MAP, ARRAY:
                Attribute stringAttr = new Attribute(functionParam.getValue(), displayName, INPUT_TYPE_STRING_OR_EXPRESSION,
                        defaultValue, functionParam.isRequired(), functionParam.getDescription(), "",
                        "", isCombo);
                stringAttr.setEnableCondition(functionParam.getEnableCondition());
                builder.addFromTemplate(ATTRIBUTE_TEMPLATE_PATH, stringAttr);
                break;
            case RECORD:
                if (expandRecords) {
                    writeRecordFields(functionParam, builder, expandRecords);
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
                if (!(functionParam instanceof UnionFunctionParam unionParam)) {
                    throw new IllegalStateException("Union parameter must be modelled as UnionFunctionParam");
                }

                List<FunctionParam> unionMembers = unionParam.getUnionMemberParams();
                if (unionMembers.isEmpty()) {
                    // Empty union (all members are nil or unsupported) - skip entirely
                    return;
                }

                // Add combo field for selecting the data type
                Combo comboField = getComboField(unionParam, functionParam.getValue(),
                        functionParam.getDescription());
                builder.addFromTemplate(COMBO_TEMPLATE_PATH, comboField).addSeparator(ATTRIBUTE_SEPARATOR);

                // Add attribute fields for each type with enable conditions
                for (int i = 0; i < unionMembers.size(); i++) {
                    FunctionParam member = unionMembers.get(i);
                    // Pass member's position within union to correctly handle separators
                    writeJsonAttributeForFunctionParam(member, i, unionMembers.size(), builder, false, expandRecords);
                }
                break;
            default:
                throw new IllegalArgumentException("Unsupported parameter type '" + paramType + "' for parameter: " + functionParam.getValue());
        }
        builder.addConditionalSeparator((index < paramLength - 1), ATTRIBUTE_SEPARATOR);
    }

    private static Combo getComboField(UnionFunctionParam unionFunctionParam, String paramName, String helpTip) {
        List<FunctionParam> unionMembers = unionFunctionParam.getUnionMemberParams();
        StringJoiner unionJoiner = new StringJoiner(",", "[", "]");
        for (int i = 0; i < unionMembers.size(); i++) {
            FunctionParam member = unionMembers.get(i);
            String comboItem = member.getParamType().equals(RECORD) ?
                    member.getTypeSymbol().getName().orElse("Record" + i) : member.getParamType();
            unionJoiner.add("\"" + comboItem + "\"");
        }
        String unionComboValues = unionJoiner.toString();
        FunctionParam firstMember = unionMembers.getFirst();
        String defaultValue = firstMember.getParamType().equals(RECORD) ?
                firstMember.getTypeSymbol().getName().orElse("Record0") : firstMember.getParamType();
        // Combo field for selecting the data type
        String comboName = String.format("%s%s", paramName, "DataType");
        return new Combo(comboName, comboName, INPUT_TYPE_COMBO, unionComboValues, defaultValue,
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
     */
    private static void writeXmlParamProperties(FunctionParam functionParam, String connectionType,
                                                 StringBuilder result, int[] indexHolder, boolean[] isFirst) {
        if (functionParam instanceof RecordFunctionParam recordParam && !recordParam.getRecordFieldParams().isEmpty()) {
            // Expand record fields as separate param properties
            for (FunctionParam fieldParam : recordParam.getRecordFieldParams()) {
                writeXmlParamProperties(fieldParam, connectionType, result, indexHolder, isFirst);
            }
        } else {
            // Generate param and paramType properties
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
            result.append(String.format("<parameter name=\"%s\" description=\"%s\"/>",
                    functionParam.getValue(), escapeXml(description)));
            isFirst[0] = false;
        }
    }

    private static String escapeXml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    /**
     * Writes record fields as separate UI elements instead of a single opaque text field.
     * Only used for config (init) parameters where expandRecords is true.
     * If the param is a RecordFunctionParam with extracted fields, each field is rendered
     * as its own input element. Nested record types are grouped into attributeGroups.
     * Otherwise, falls back to a single stringOrExpression field.
     */
    private static void writeRecordFields(FunctionParam functionParam, JsonTemplateBuilder builder,
                                          boolean expandRecords) throws IOException {
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
                writeJsonAttributeForFunctionParam(fieldParam, i, topLevelFields.size(), builder, false, expandRecords);
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
                
                // Create attributeGroup for this nested type
                AttributeGroup attributeGroup = new AttributeGroup(groupName);
                builder.addFromTemplate(ATTRIBUTE_GROUP_TEMPLATE_PATH, attributeGroup);
                
                // Write fields within this group
                // The attributeGroup template ends with "elements": [ and a newline
                // Add proper indentation (18 spaces) to align with the attribute template's content indentation
                for (int i = 0; i < groupFields.size(); i++) {
                    FunctionParam fieldParam = groupFields.get(i);
                    // Add indentation before the first attribute in the group
                    if (i == 0) {
                        builder.addSeparator("                  ");  // 18 spaces to align with template content
                    }
                    writeJsonAttributeForFunctionParam(fieldParam, i, groupFields.size(), builder, false, expandRecords);
                }
                
                // Close the attributeGroup - close elements array, value object, and attributeGroup object
                // Match the indentation format from the expected output
                builder.addSeparator("\n                    ]");
                builder.addSeparator("\n                  }");
                
                // Close attributeGroup with comma if there are more groups, otherwise just close it
                // The comma should be on the same line as the closing brace: },{
                // The next attributeGroup template starts with {, so we get },{ on the same line
                if (groupIndex < totalGroups - 1) {
                    // Add closing brace and comma on same line, then newline
                    builder.addSeparator("\n                },");
                } else {
                    builder.addSeparator("\n                }");
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
}
