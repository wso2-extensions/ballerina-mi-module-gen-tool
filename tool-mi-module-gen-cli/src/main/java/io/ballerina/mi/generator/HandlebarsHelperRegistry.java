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

package io.ballerina.mi.generator;

import com.github.jknack.handlebars.Handlebars;
import io.ballerina.compiler.api.symbols.ArrayTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.mi.model.Component;
import io.ballerina.mi.model.Connection;
import io.ballerina.mi.model.Connector;
import io.ballerina.mi.model.FunctionType;
import io.ballerina.mi.model.param.ArrayFunctionParam;
import io.ballerina.mi.model.param.FunctionParam;
import io.ballerina.mi.model.param.MapFunctionParam;
import io.ballerina.mi.model.param.RecordFunctionParam;
import io.ballerina.mi.util.JsonTemplateBuilder;
import io.ballerina.mi.util.Utils;
import org.apache.commons.lang3.StringUtils;
import org.ballerinalang.diagramutil.connector.models.connector.Type;
import org.ballerinalang.diagramutil.connector.models.connector.types.PathParamType;

import java.util.*;
import java.util.stream.Collectors;

import static io.ballerina.mi.util.Constants.ATTRIBUTE_SEPARATOR;

/**
 * Registers all Handlebars template helpers used during connector artifact generation.
 * <p>
 * This class was extracted from ConnectorSerializer to provide a single, focused registry
 * of template helpers. Each helper is responsible for a specific rendering concern:
 * <ul>
 *   <li><strong>Comparison/Logic:</strong> eq, not, checkFuncType</li>
 *   <li><strong>String formatting:</strong> escapeChars, sanitizeParamName, uppercase, capitalize, sanitizeModuleName</li>
 *   <li><strong>XML property writing:</strong> writeConfigXmlProperties, writeComponentXmlProperties, etc.</li>
 *   <li><strong>JSON property writing:</strong> writeConfigJsonProperties, writeComponentJsonProperties</li>
 *   <li><strong>Type introspection:</strong> arrayElementType, mapValueType, isMapOfRecord, mapRecordFieldNames</li>
 *   <li><strong>Misc:</strong> unwrapOptional, writeConfigDependency</li>
 * </ul>
 *
 * @since 0.6.0
 */
public final class HandlebarsHelperRegistry {

    private HandlebarsHelperRegistry() {
        // Utility class — no instantiation
    }

    /**
     * Registers all connector template helpers on the given Handlebars instance.
     *
     * @param handlebar The Handlebars instance to register helpers on
     */
    public static void registerAll(Handlebars handlebar) {
        registerComparisonHelpers(handlebar);
        registerStringHelpers(handlebar);
        registerXmlHelpers(handlebar);
        registerJsonHelpers(handlebar);
        registerTypeIntrospectionHelpers(handlebar);
        registerMiscHelpers(handlebar);
    }

    // ─── Comparison & Logic ───────────────────────────────────────────────────

    private static void registerComparisonHelpers(Handlebars handlebar) {
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
        handlebar.registerHelper("checkFuncType", (context, options) -> {
            FunctionType functionType = (FunctionType) context;
            return functionType.toString().equals(options.param(0));
        });
    }

    // ─── String Formatting ────────────────────────────────────────────────────

    private static void registerStringHelpers(Handlebars handlebar) {
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
        handlebar.registerHelper("uppercase", (context, options) -> {
            if (context == null) {
                return "";
            }
            return context.toString().toUpperCase();
        });
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

    // ─── XML Property/Parameter Helpers ───────────────────────────────────────

    private static void registerXmlHelpers(Handlebars handlebar) {
        handlebar.registerHelper("writeConfigXmlProperties", (context, options) -> {
            Connection connection = (Connection) context;
            StringBuilder result = new StringBuilder();
            List<Type> initParams = connection.getInitComponent() != null
                    ? connection.getInitComponent().getQueryParams() : List.of();
            for (int i = 0; i < initParams.size(); i++) {
                XmlPropertyWriter.writeConfigXmlProperty(initParams.get(i), i, connection.getConnectionType(), result);
            }
            return new Handlebars.SafeString(result.toString());
        });
        handlebar.registerHelper("writeComponentXmlProperties", (context, options) -> {
            Component component = (Component) context;
            StringBuilder result = new StringBuilder();
            List<PathParamType> pathParams = component.getPathParams();
            for (int i = 0; i < pathParams.size(); i++) {
                XmlPropertyWriter.writeComponentXmlPathProperty(pathParams.get(i), i, result, i == 0);
            }
            List<Type> queryParams = component.getQueryParams();
            for (int i = 0; i < queryParams.size(); i++) {
                XmlPropertyWriter.writeComponentXmlQueryProperty(queryParams.get(i), i, result);
            }
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
                XmlPropertyWriter.writeXmlParameterElements(functionParam, result, isFirst, processedParams);
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
                    XmlPropertyWriter.writeXmlParamProperties(functionParam, connection.getConnectionType().toUpperCase(), result, indexHolder, isFirst);
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
                    XmlPropertyWriter.writeXmlParameterElements(fieldParam, result, isFirst, processedParams);
                }
            } else {
                String sanitizedParamName = Utils.sanitizeParamName(functionParam.getValue());
                String description = functionParam.getDescription() != null ? functionParam.getDescription() : "";
                result.append(String.format("<parameter name=\"%s\" description=\"%s\"/>",
                        sanitizedParamName, XmlPropertyWriter.escapeXml(description)));
            }
            return new Handlebars.SafeString(result.toString());
        });
        handlebar.registerHelper("writeFunctionRecordXmlProperties", (context, options) -> {
            FunctionParam functionParam = (FunctionParam) context;

            StringBuilder result = new StringBuilder();
            if (functionParam instanceof RecordFunctionParam recordParam && !recordParam.getRecordFieldParams().isEmpty()) {
                int[] fieldIndexHolder = {0};
                String recordParamName = recordParam.getValue();
                for (FunctionParam fieldParam : recordParam.getRecordFieldParams()) {
                    XmlPropertyWriter.writeFunctionRecordFieldProperties(fieldParam, recordParamName, result, fieldIndexHolder);
                }
            }
            return new Handlebars.SafeString(result.toString());
        });
    }

    // ─── JSON Property Helpers ────────────────────────────────────────────────

    private static void registerJsonHelpers(Handlebars handlebar) {
        handlebar.registerHelper("writeConfigJsonProperties", (context, options) -> {
            Component component = (Component) context;
            JsonTemplateBuilder builder = new JsonTemplateBuilder();
            List<FunctionParam> functionParams = component.getFunctionParams();

            List<FunctionParam> basicParams = new ArrayList<>();
            List<FunctionParam> advancedParams = new ArrayList<>();

            for (FunctionParam param : functionParams) {
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
                JsonGenerator.writeAttributeGroup("Basic", basicParams, advancedParams.isEmpty(), builder, false, true);
            }
            if (!advancedParams.isEmpty()) {
                JsonGenerator.writeAttributeGroup("Advanced", advancedParams, true, builder, true, true);
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
                JsonGenerator.writeJsonAttributeForPathParam(pathParam, i, totalPathParams, builder);
                if (i < totalPathParams - 1) {
                    builder.addSeparator(ATTRIBUTE_SEPARATOR);
                }
            }

            List<FunctionParam> functionParams = component.getFunctionParams();

            List<FunctionParam> requiredParams = new ArrayList<>();
            List<FunctionParam> advancedParams = new ArrayList<>();
            for (FunctionParam param : functionParams) {
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
                JsonGenerator.writeJsonAttributeForFunctionParam(requiredParams.get(i), i, totalRequired, builder, false, true, null, false);
            }

            if (hasAdvancedParams) {
                if (hasRequiredParams) {
                    builder.addSeparator(ATTRIBUTE_SEPARATOR);
                }
                JsonGenerator.writeAttributeGroup("Advanced", advancedParams, true, builder, true, false);
            }

            return new Handlebars.SafeString(builder.build());
        });
    }

    // ─── Type Introspection Helpers ───────────────────────────────────────────

    private static void registerTypeIntrospectionHelpers(Handlebars handlebar) {
        handlebar.registerHelper("arrayElementType", (context, options) -> {
            if (!(context instanceof FunctionParam functionParam)) {
                return "";
            }
            if (functionParam instanceof ArrayFunctionParam arrayParam) {
                String cached = arrayParam.getArrayElementTypeName();
                if (cached != null) return cached;
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
        handlebar.registerHelper("mapValueType", (context, options) -> {
            if (!(context instanceof MapFunctionParam mapParam)) {
                return "";
            }
            String cached = mapParam.getValueTypeName();
            if (cached != null) return cached;
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
            TypeDescKind valueKind = mapParam.getValueTypeKind();
            if (valueKind != null) return valueKind == TypeDescKind.RECORD;
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
    }

    // ─── Miscellaneous Helpers ────────────────────────────────────────────────

    private static void registerMiscHelpers(Handlebars handlebar) {
        handlebar.registerHelper("writeConfigDependency", (context, options) -> {
            Connector connector = (Connector) context;
            if (!connector.isBalModule()) {
                return new Handlebars.SafeString("<dependency component=\"config\"/>");
            }
            return new Handlebars.SafeString("");
        });
        handlebar.registerHelper("unwrapOptional", ((context, options) -> {
            if (context instanceof Optional<?> optional) {
                if (optional.isPresent()) {
                    return optional.get();
                }
            }
            return "";
        }));
    }
}
