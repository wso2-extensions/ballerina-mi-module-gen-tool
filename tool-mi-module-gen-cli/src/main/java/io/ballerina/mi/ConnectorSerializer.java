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
import io.ballerina.mi.connectorModel.attributeModel.Combo;
import io.ballerina.mi.util.Constants;
import io.ballerina.mi.util.JsonTemplateBuilder;
import io.ballerina.mi.util.Utils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
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
            handlebar.registerHelper("writeConfigJsonProperties", (context, options) -> {
                Component component = (Component) context;
                JsonTemplateBuilder builder = new JsonTemplateBuilder();
                List<FunctionParam> functionParams = component.getFunctionParams();
                for (FunctionParam functionParam : functionParams) {
                    writeJsonAttributeForFunctionParam(functionParam, functionParams.indexOf(functionParam),
                            functionParams.size(), builder, false);
                }
//                List<Type> queryParams = component.getQueryParams();
//                String helpTip = String.format("Input parameters required for %s connection",
//                        component.getParent().getConnectionType());
//                for (Type configParam : queryParams) {
//                    String paramName = String.format("%s_%s", component.getParent().getConnectionType(),
//                            configParam.name);
//                    writeJsonAttributeForQueryParam(configParam.typeName, configParam, queryParams.indexOf(configParam),
//                            queryParams.size(), builder, paramName, helpTip, false);
//                }
                return new Handlebars.SafeString(builder.build());
            });
            handlebar.registerHelper("writeComponentJsonProperties", (context, options) -> {
                Component component = (Component) context;
                JsonTemplateBuilder builder = new JsonTemplateBuilder();
                List<FunctionParam> functionParams = component.getFunctionParams();
                for (FunctionParam functionParam : functionParams) {
                    writeJsonAttributeForFunctionParam(functionParam, functionParams.indexOf(functionParam),
                            functionParams.size(), builder, false);
                }
//                List<PathParamType> pathParams = component.getPathParams();
//                for (PathParamType pathParam : pathParams) {
//                    writeJsonAttributeForPathParam(pathParam.typeName, pathParam, pathParams.indexOf(pathParam),
//                            pathParams.size(), builder, pathParam.name, "", false);
//                }
//                List<Type> queryParams = component.getQueryParams();
//                for (Type queryParam: queryParams) {
//                    writeJsonAttributeForQueryParam(queryParam.typeName, queryParam, queryParams.indexOf(queryParam),
//                            queryParams.size(), builder, queryParam.name, queryParam.documentation, false);
//                }
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
                                                           boolean isCombo) throws IOException {
        String paramType = functionParam.getParamType();
        String displayName = functionParam.getValue();
        switch (paramType) {
            case STRING, XML, JSON, MAP, RECORD, ARRAY:
                Attribute stringAttr = new Attribute(functionParam.getValue(), displayName, INPUT_TYPE_STRING_OR_EXPRESSION,
                        "", functionParam.isRequired(), functionParam.getDescription(), "",
                        "", isCombo);
                stringAttr.setEnableCondition(functionParam.getEnableCondition());
                builder.addFromTemplate(ATTRIBUTE_TEMPLATE_PATH, stringAttr);
                break;
            case INT:
                Attribute intAttr = new Attribute(functionParam.getValue(), displayName, INPUT_TYPE_STRING_OR_EXPRESSION,
                        "", functionParam.isRequired(), functionParam.getDescription(), VALIDATE_TYPE_REGEX,
                        INTEGER_REGEX, isCombo);
                intAttr.setEnableCondition(functionParam.getEnableCondition());
                builder.addFromTemplate(ATTRIBUTE_TEMPLATE_PATH, intAttr);
                break;
            case DECIMAL, FLOAT:
                Attribute decAttr = new Attribute(functionParam.getValue(), displayName,
                        INPUT_TYPE_STRING_OR_EXPRESSION, "", functionParam.isRequired(),
                        functionParam.getDescription(), VALIDATE_TYPE_REGEX, DECIMAL_REGEX, isCombo);
                decAttr.setEnableCondition(functionParam.getEnableCondition());
                builder.addFromTemplate(ATTRIBUTE_TEMPLATE_PATH, decAttr);
                break;
            case BOOLEAN:
                Attribute boolAttr = new Attribute(functionParam.getValue(), displayName, INPUT_TYPE_BOOLEAN,
                        "", functionParam.isRequired(), functionParam.getDescription(), "",
                        "", isCombo);
                boolAttr.setEnableCondition(functionParam.getEnableCondition());
                builder.addFromTemplate(ATTRIBUTE_TEMPLATE_PATH, boolAttr);
                break;
            case UNION:
                // Gather the data types in the union
                if (!((UnionFunctionParam) functionParam).getUnionMemberParams().isEmpty()) {
                    Combo comboField = getComboField((UnionFunctionParam) functionParam, functionParam.getValue(),
                            functionParam.getDescription());
                    builder.addFromTemplate(COMBO_TEMPLATE_PATH, comboField).addSeparator(ATTRIBUTE_SEPARATOR);
                }

                // Add attribute fields for each type with enable conditions
                List<FunctionParam> unionMembers = ((UnionFunctionParam) functionParam).getUnionMemberParams();
                for (FunctionParam member : unionMembers) {
                    writeJsonAttributeForFunctionParam(member, index, paramLength, builder, false);
                    builder.addConditionalSeparator((unionMembers.indexOf(member) < unionMembers.size() - 1),
                            ATTRIBUTE_SEPARATOR);
                }
                break;
            default:
                throw new IllegalArgumentException("Unsupported parameter type '" + paramType + "' for parameter: " + functionParam.getValue());
        }
        builder.addConditionalSeparator((index < paramLength - 1), ATTRIBUTE_SEPARATOR);
    }

//    private static void writeJsonAttributeForPathParam(String paramType, PathParamType parameter, int index, int paramLength,
//                                                       JsonTemplateBuilder builder, String paramName, String helpTip,
//                                                       boolean isCombo) throws IOException {
//        switch (paramType) {
//            case STRING, XML, JSON, MAP, RECORD:
//                Attribute stringAttr = new Attribute(paramName, parameter.name, INPUT_TYPE_STRING_OR_EXPRESSION,
//                        "", true, helpTip, "",
//                        "", isCombo);
//                builder.addFromTemplate(ATTRIBUTE_TEMPLATE_PATH, stringAttr);
//                break;
//            case INT:
//                Attribute intAttr = new Attribute(paramName, parameter.name, INPUT_TYPE_STRING_OR_EXPRESSION,
//                        "", true, helpTip, VALIDATE_TYPE_REGEX,
//                        INTEGER_REGEX, isCombo);
//                builder.addFromTemplate(ATTRIBUTE_TEMPLATE_PATH, intAttr);
//                break;
//            case DECIMAL, FLOAT:
//                Attribute decAttr = new Attribute(paramName, parameter.name,
//                        INPUT_TYPE_STRING_OR_EXPRESSION, "", true,
//                        helpTip, VALIDATE_TYPE_REGEX, DECIMAL_REGEX, isCombo);
//                builder.addFromTemplate(ATTRIBUTE_TEMPLATE_PATH, decAttr);
//                break;
//            case BOOLEAN:
//                Attribute boolAttr = new Attribute(paramName, parameter.name, INPUT_TYPE_BOOLEAN,
//                        "", true, helpTip, "",
//                        "", isCombo);
//                builder.addFromTemplate(ATTRIBUTE_TEMPLATE_PATH, boolAttr);
//                break;
//            default:
//                throw new IllegalArgumentException("Unsupported parameter type '" + paramType + "' for parameter: " + paramName);
//        }
//        builder.addConditionalSeparator((index < paramLength - 1), ATTRIBUTE_SEPARATOR);
//    }

//    private static void writeJsonAttributeForQueryParam(String paramType, Type parameter, int index, int paramLength,
//                                                        JsonTemplateBuilder builder, String paramName, String helpTip,
//                                                        boolean isCombo)
//            throws IOException {
//        switch (paramType) {
//            case STRING, XML, JSON, MAP:
//                Attribute stringAttr = new Attribute(paramName, parameter.name, INPUT_TYPE_STRING_OR_EXPRESSION,
//                        parameter.getDefaultValue(), !parameter.isOptional(), helpTip, "",
//                        "", isCombo);
//                builder.addFromTemplate(ATTRIBUTE_TEMPLATE_PATH, stringAttr);
//                break;
//            case INT:
//                Attribute intAttr = new Attribute(paramName, parameter.name, INPUT_TYPE_STRING_OR_EXPRESSION,
//                        parameter.getDefaultValue(), !parameter.isOptional(), helpTip, VALIDATE_TYPE_REGEX,
//                        INTEGER_REGEX, isCombo);
//                builder.addFromTemplate(ATTRIBUTE_TEMPLATE_PATH, intAttr);
//                break;
//            case DECIMAL, FLOAT:
//                Attribute decAttr = new Attribute(paramName, parameter.name,
//                        INPUT_TYPE_STRING_OR_EXPRESSION, parameter.getDefaultValue(), !parameter.isOptional(),
//                        helpTip, VALIDATE_TYPE_REGEX, DECIMAL_REGEX, isCombo);
//                builder.addFromTemplate(ATTRIBUTE_TEMPLATE_PATH, decAttr);
//                break;
//            case ARRAY:
//                Table arrayTable = new Table(paramName, parameter.name, parameter.name, helpTip);
//                builder.addFromTemplate(TABLE_TEMPLATE_PATH, arrayTable);
//                //TODO: Generate attribute according to the data type of the array fields

    /// /                writeJsonAttributeForQueryParam();
//                break;
//            case UNION:
//                // Gather the data types in the union
//                Combo comboField = getComboField(parameter, paramName, helpTip);
//                builder.addFromTemplate(COMBO_TEMPLATE_PATH, comboField).addSeparator(ATTRIBUTE_SEPARATOR);
//
//                // Add attribute fields for each type with enable conditions
//                List<Type> unionMembers = ((UnionType) parameter).members;
//                for (Type member : unionMembers) {
//                    writeJsonAttributeForQueryParam(member.typeName, parameter, index, paramLength, builder, paramName,
//                            helpTip, true);
//                }
//                break;
//            case ENUM:
//                List<Type> enumMembers = ((EnumType) parameter).members;
//                StringJoiner joiner = new StringJoiner(",", "[", "]");
//                for (Type member : enumMembers) {
//                    joiner.add("\"" + member.typeName + "\"");
//                }
//                String enumComboValues = joiner.toString();
//                Combo enumComboField = new Combo(paramName, parameter.name, INPUT_TYPE_COMBO, enumComboValues,
//                        !parameter.isOptional(), helpTip);
//                builder.addFromTemplate(COMBO_TEMPLATE_PATH, enumComboField);
//                break;
//            case BOOLEAN:
//                Attribute boolAttr = new Attribute(paramName, parameter.name, INPUT_TYPE_BOOLEAN,
//                        parameter.getDefaultValue(), !parameter.isOptional(), helpTip, "",
//                        "", isCombo);
//                builder.addFromTemplate(ATTRIBUTE_TEMPLATE_PATH, boolAttr);
//                break;
//            case RECORD:
//                AttributeGroup recGroup = new AttributeGroup(paramName);
//                builder.addFromTemplate(ATTRIBUTE_GROUP_TEMPLATE_PATH, recGroup);
//                List<Type> recordFields = ((RecordType) parameter).fields;
//                for (Type field : recordFields) {
//                    String fieldName = String.format("%s_%s", paramName, field.name);
//                    writeJsonAttributeForQueryParam(field.typeName, field, recordFields.indexOf(field), paramLength,
//                            builder, fieldName, field.documentation, false);
//                    builder.addConditionalSeparator((recordFields.indexOf(field) < recordFields.size() - 1),
//                            ATTRIBUTE_SEPARATOR);
//                }
//                builder.addSeparator(ATTRIBUTE_GROUP_END);
//                break;
//            default:
//                throw new IllegalArgumentException("Unsupported parameter type '" + paramType + "' for parameter: " + paramName);
//        }
//        builder.addConditionalSeparator((index < paramLength - 1), ATTRIBUTE_SEPARATOR);
//    }
    private static Combo getComboField(UnionFunctionParam unionFunctionParam, String paramName, String helpTip) {
        List<FunctionParam> unionMembers = unionFunctionParam.getUnionMemberParams();
        StringJoiner unionJoiner = new StringJoiner(",", "[", "]");
        for (FunctionParam member : unionMembers) {
            String comboItem = member.getParamType().equals(RECORD) ?
                    member.getTypeSymbol().getName().orElseThrow() : member.getParamType();
            unionJoiner.add("\"" + comboItem + "\"");
        }
        String unionComboValues = unionJoiner.toString();
        String defaultValue = unionMembers.getFirst().getParamType().equals(RECORD) ?
                unionMembers.getFirst().getTypeSymbol().getName().orElseThrow() : unionMembers.getFirst().getParamType();
        // Combo field for selecting the data type
        String comboName = String.format("%s%s", paramName, "DataType");
        return new Combo(comboName, comboName, INPUT_TYPE_COMBO, unionComboValues, defaultValue,
                unionFunctionParam.isRequired(), unionFunctionParam.getEnableCondition(), helpTip);
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
