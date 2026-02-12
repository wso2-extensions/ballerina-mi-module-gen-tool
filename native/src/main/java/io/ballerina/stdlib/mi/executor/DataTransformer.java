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

package io.ballerina.stdlib.mi.executor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.ArrayType;
import io.ballerina.runtime.api.types.Field;
import io.ballerina.runtime.api.types.StructureType;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.types.TypeTags;
import io.ballerina.runtime.api.utils.JsonUtils;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.runtime.internal.values.MapValueImpl;
import io.ballerina.stdlib.mi.BalConnectorConfig;
import io.ballerina.stdlib.mi.Constants;
import io.ballerina.stdlib.mi.utils.SynapseUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.ballerinalang.langlib.value.FromJsonStringWithType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.ballerina.stdlib.mi.Constants.ARRAY;
import static io.ballerina.stdlib.mi.Constants.BOOLEAN;
import static io.ballerina.stdlib.mi.Constants.DECIMAL;
import static io.ballerina.stdlib.mi.Constants.FLOAT;
import static io.ballerina.stdlib.mi.Constants.INT;
import static io.ballerina.stdlib.mi.Constants.JSON;
import static io.ballerina.stdlib.mi.Constants.MAP;
import static io.ballerina.stdlib.mi.Constants.RECORD;
import static io.ballerina.stdlib.mi.Constants.UNION;

public class DataTransformer {

    private static final Log log = LogFactory.getLog(DataTransformer.class);
    private static final Pattern SYNAPSE_EXPRESSION_PATTERN = Pattern.compile("\\$\\{(.+?)\\}");

    public static Object convertValueToType(Object sourceValue, Type targetType) {
        if (sourceValue == null) {
            return null;
        }
        if (targetType.getTag() == TypeTags.RECORD_TYPE_TAG && sourceValue instanceof BMap) {
            return createTypedRecordFromGeneric((BMap<BString, Object>) sourceValue, (StructureType) targetType);
        }
        if (targetType.getTag() == TypeTags.ARRAY_TAG && sourceValue instanceof BArray) {
            return createTypedArrayFromGeneric((BArray) sourceValue, (ArrayType) targetType);
        }
        return sourceValue;
    }

    public static BMap<BString, Object> createTypedRecordFromGeneric(BMap<BString, Object> genericMap, StructureType targetType) {
        BMap<BString, Object> typedRecord = ValueCreator.createRecordValue(targetType.getPackage(), targetType.getName());
        for (Field field : targetType.getFields().values()) {
            String fieldName = field.getFieldName();
            BString bFieldName = StringUtils.fromString(fieldName);
            if (genericMap.containsKey(bFieldName)) {
                Object genericValue = genericMap.get(bFieldName);
                Object convertedValue = convertValueToType(genericValue, field.getFieldType());
                typedRecord.put(bFieldName, convertedValue);
            }
        }
        return typedRecord;
    }

    public static BArray createTypedArrayFromGeneric(BArray genericArray, ArrayType targetType) {
        long size = genericArray.size();
        for (long i = 0; i < size; i++) {
            Object value = genericArray.get(i);
            Object converted = convertValueToType(value, targetType.getElementType());
            if (value != converted) {
                try {
                    genericArray.add(i, converted);
                } catch (Exception e) {
                    log.warn("Failed to update array element at index " + i + ": " + e.getMessage());
                }
            }
        }
        return genericArray;
    }

    public static Object createRecordValue(String jsonString, String paramName, MessageContext context, int paramIndex) {
        if (jsonString == null) {
            String recordParamName = paramName;
            String connectionType = SynapseUtils.findConnectionTypeForParam(context, recordParamName);
            String propertyPrefix;
            String recordNamePropertyKey;
            if (connectionType != null) {
                propertyPrefix = connectionType + "_" + recordParamName;
                recordNamePropertyKey = connectionType + "_param" + paramIndex + "_recordName";
            } else {
                propertyPrefix = recordParamName;
                recordNamePropertyKey = "param" + paramIndex + "_recordName";
            }

            Object reconstructedBMap = reconstructRecordFromFields(propertyPrefix, context);
            Object recordNameObj = context.getProperty(recordNamePropertyKey);
            if (recordNameObj == null) {
                throw new SynapseException("Record name not found for parameter at index " + paramIndex +
                        ". Ensure '" + recordNamePropertyKey + "' property is set in the synapse template.");
            }
            String recordName = recordNameObj.toString();
            BMap<BString, Object> recValue = ValueCreator.createRecordValue(BalConnectorConfig.getModule(), recordName);
            Type recType = recValue.getType();

            if (reconstructedBMap instanceof BMap) {
                String jsonStr = ((MapValueImpl<?, ?>) reconstructedBMap).getJSONString();
                BString jsonBString = StringUtils.fromString(jsonStr);
                try{
                     return FromJsonStringWithType.fromJsonStringWithType(jsonBString, ValueCreator.createTypedescValue(recType));
                } catch(Exception e) {
                     return convertValueToType(reconstructedBMap, recType);
                }
            }
            throw new SynapseException("Failed to reconstruct record from flattened fields for parameter '" + paramName + "'");
        }

        if (jsonString.startsWith("'") && jsonString.endsWith("'")) {
            jsonString = jsonString.substring(1, jsonString.length() - 1);
        }

        Object recordNameObj = context.getProperty("param" + paramIndex + "_recordName");
        if (recordNameObj != null) {
            String recordName = recordNameObj.toString();
            try {
                BString jsonBString = StringUtils.fromString(jsonString);
                BMap<BString, Object> recValue = ValueCreator.createRecordValue(BalConnectorConfig.getModule(), recordName);
                Type recType = recValue.getType();
                return FromJsonStringWithType.fromJsonStringWithType(jsonBString, ValueCreator.createTypedescValue(recType));
            } catch (Exception e) {
                try {
                    Object parseResult = JsonUtils.parse(jsonString);
                    BMap<BString, Object> emptyRecord = ValueCreator.createRecordValue(BalConnectorConfig.getModule(), recordName);
                    Type targetType = emptyRecord.getType();
                    return convertValueToType(parseResult, targetType);
                } catch (Exception deepEx) {
                    log.error("Manual deep conversion failed: " + deepEx.getMessage(), deepEx);
                }
            }
        }

        try {
            Object parseResult = JsonUtils.parse(jsonString);
            if (parseResult instanceof BError) {
                throw new SynapseException("Failed to parse JSON for record: " + ((BError) parseResult).getMessage());
            }
            return parseResult;
        } catch (Exception e) {
            throw new SynapseException("Failed to create record value: " + e.getMessage(), e);
        }
    }

    public static Object reconstructRecordFromFields(String propertyPrefix, MessageContext context) {
        com.google.gson.JsonObject recordJson = new com.google.gson.JsonObject();
        Map<String, String> unionFieldSelectedTypes = new HashMap<>();
        
        int tempIndex = 0;
        while (true) {
            String fieldNameKey = propertyPrefix + "_param" + tempIndex;
            String fieldTypeKey = propertyPrefix + "_paramType" + tempIndex;
            Object fieldNameObj = context.getProperty(fieldNameKey);
            Object fieldTypeObj = context.getProperty(fieldTypeKey);

            if (fieldNameObj == null || fieldTypeObj == null) break;

            if (UNION.equals(fieldTypeObj.toString())) {
                String dataTypeKey = propertyPrefix + "_dataType" + tempIndex;
                Object dataTypeParamNameObj = context.getProperty(dataTypeKey);
                if (dataTypeParamNameObj != null) {
                    Object selectedTypeObj = SynapseUtils.lookupTemplateParameter(context, dataTypeParamNameObj.toString());
                    if (selectedTypeObj != null) unionFieldSelectedTypes.put(fieldNameObj.toString(), selectedTypeObj.toString());
                }
            }
            tempIndex++;
        }

        int fieldIndex = 0;
        while (true) {
            String fieldNameKey = propertyPrefix + "_param" + fieldIndex;
            String fieldTypeKey = propertyPrefix + "_paramType" + fieldIndex;
            String unionMemberKey = propertyPrefix + "_unionMember" + fieldIndex;

            Object fieldNameObj = context.getProperty(fieldNameKey);
            Object fieldTypeObj = context.getProperty(fieldTypeKey);
            Object unionMemberObj = context.getProperty(unionMemberKey);

            if (fieldNameObj == null || fieldTypeObj == null) break;

            String fieldPath = fieldNameObj.toString();
            String fieldType = fieldTypeObj.toString();
            String unionMemberType = unionMemberObj != null ? unionMemberObj.toString() : null;

            if (UNION.equals(fieldType)) {
               if (unionMemberType != null) {
                    String parentUnionPath = SynapseUtils.findParentUnionPath(fieldPath, unionFieldSelectedTypes.keySet());
                    if (parentUnionPath != null) {
                        String selectedType = unionFieldSelectedTypes.get(parentUnionPath);
                        if (selectedType != null && !selectedType.equals(unionMemberType)) {
                            fieldIndex++;
                            continue;
                        }
                    }
                }
                String sanitizedFieldPath = fieldPath.replace(".", "_");
                Object unionValue = SynapseUtils.lookupTemplateParameter(context, sanitizedFieldPath);
                if (unionValue != null) {
                     setNestedField(recordJson, fieldPath, unionValue, fieldType, context);
                     fieldIndex++;
                     continue;
                }
                fieldIndex++;
                continue;
            }

            if (unionMemberType != null) {
                String parentUnionPath = SynapseUtils.findParentUnionPath(fieldPath, unionFieldSelectedTypes.keySet());
                if (parentUnionPath != null) {
                    String selectedType = unionFieldSelectedTypes.get(parentUnionPath);
                    if (selectedType != null && !selectedType.equals(unionMemberType)) {
                        fieldIndex++;
                        continue;
                    }
                }
            }

            String sanitizedFieldPath = fieldPath.replace(".", "_");
            Object fieldValue = SynapseUtils.lookupTemplateParameter(context, sanitizedFieldPath);

            if (fieldValue != null) {
                String valueStr = fieldValue.toString();
                if ((MAP.equals(fieldType) || ARRAY.equals(fieldType)) && "[]".equals(valueStr)) {
                    fieldIndex++;
                    continue;
                }
                setNestedField(recordJson, fieldPath, fieldValue, fieldType, context);
            }
            fieldIndex++;
        }

        return JsonUtils.parse(recordJson.toString());
    }

    private static void setNestedField(JsonObject jsonObject, String fieldPath, Object value, String fieldType, MessageContext context) {
        String[] parts = fieldPath.split("\\.");
        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            if (!jsonObject.has(part)) jsonObject.add(part, new JsonObject());
            jsonObject = jsonObject.getAsJsonObject(part);
        }

        String finalField = parts[parts.length - 1];
        String valueStr = value.toString();

        if (SYNAPSE_EXPRESSION_PATTERN.matcher(valueStr).find()) {
            valueStr = SynapseUtils.resolveSynapseExpressions(valueStr, context);
        }

        switch (fieldType) {
            case BOOLEAN:
                jsonObject.addProperty(finalField, Boolean.parseBoolean(valueStr));
                break;
            case INT:
                jsonObject.addProperty(finalField, Long.parseLong(valueStr));
                break;
            case FLOAT:
                jsonObject.addProperty(finalField, Double.parseDouble(valueStr));
                break;
            case DECIMAL:
                jsonObject.addProperty(finalField, new java.math.BigDecimal(valueStr));
                break;
            case JSON:
            case RECORD:
            case UNION:
                try {
                    String cleanedJson = SynapseUtils.cleanupJsonString(valueStr);
                    JsonElement jsonElement = JsonParser.parseString(cleanedJson);
                    jsonObject.add(finalField, jsonElement);
                } catch (JsonSyntaxException e) {
                    jsonObject.addProperty(finalField, valueStr);
                }
                break;
            case ARRAY:
            case MAP: 
                 try {
                    String cleanedJson = SynapseUtils.cleanupJsonString(valueStr);
                    JsonElement jsonElement = JsonParser.parseString(cleanedJson);
                    if (jsonElement.isJsonArray() && jsonElement.getAsJsonArray().size() > 0 && jsonElement.getAsJsonArray().get(0).isJsonArray()) {
                         if (MAP.equals(fieldType)) {
                             JsonObject mapObj = new JsonObject();
                             for(JsonElement e : jsonElement.getAsJsonArray()) {
                                 if(e.isJsonArray()) {
                                     com.google.gson.JsonArray pair = e.getAsJsonArray();
                                     if(pair.size() >= 2) mapObj.add(pair.get(0).getAsString(), pair.get(1));
                                 }
                             }
                             jsonObject.add(finalField, mapObj);
                         } else {
                             com.google.gson.JsonArray flatArr = new com.google.gson.JsonArray();
                             for(JsonElement e : jsonElement.getAsJsonArray()) {
                                 if(e.isJsonArray()) {
                                     for(JsonElement inner : e.getAsJsonArray()) if(!inner.isJsonNull()) flatArr.add(inner);
                                 }
                             }
                             jsonObject.add(finalField, flatArr);
                         }
                    } else {
                        jsonObject.add(finalField, jsonElement);
                    }
                } catch (JsonSyntaxException e) {
                    jsonObject.addProperty(finalField, valueStr);
                }
                break;
            default:
                jsonObject.addProperty(finalField, valueStr);
                break;
        }
    }
    
    public static Object getJsonParameter(Object param) {
        if (param instanceof String strParam) {
            if (strParam.startsWith("'") && strParam.endsWith("'")) {
                strParam = strParam.substring(1, strParam.length() - 1);
            }
            return JsonUtils.parse(strParam);
        } else {
            return JsonUtils.parse(param.toString());
        }
    }
    
    public static BMap getMapParameter(Object param, MessageContext context, String valueKey) {
        String jsonString;
        if (param instanceof String strParam) {
            if (strParam.startsWith("'") && strParam.endsWith("'")) {
                strParam = strParam.substring(1, strParam.length() - 1);
            }
            jsonString = strParam;
        } else {
            jsonString = param.toString();
        }
        jsonString = SynapseUtils.cleanupJsonString(jsonString);
        Object parsed = JsonUtils.parse(jsonString);

        if (parsed instanceof BMap) return (BMap) parsed;

        if (parsed instanceof BArray array) {
            if (array.size() == 0) return ValueCreator.createMapValue();
            Object firstElement = array.get(0);
            if (firstElement instanceof BMap firstRow && firstRow.containsKey(StringUtils.fromString("key"))) {
                return transformTableToMap(array);
            }
            if (firstElement instanceof BArray) {
                int paramIndex = -1;
                String indexStr = valueKey.replaceAll("\\D+", "");
                if (!indexStr.isEmpty()) {
                    try { paramIndex = Integer.parseInt(indexStr); } catch (NumberFormatException ignored) {}
                }
                String[] fieldNames = null;
                if (paramIndex >= 0) {
                    Object fieldNamesObj = context.getProperty("mapRecordFields" + paramIndex);
                    if (fieldNamesObj != null) fieldNames = fieldNamesObj.toString().split(",");
                }
                return transform2DArrayToMap(array, fieldNames);
            }
        }
        throw new SynapseException("Map parameter must be a JSON object or table array");
    }

    public static String transformNestedTableTo2DArray(BArray outerArray) {
        BString innerArrayKey = StringUtils.fromString("innerArray");
        BString valueKey = StringUtils.fromString("value");
        StringBuilder jsonBuilder = new StringBuilder("[");

        for (int i = 0; i < outerArray.size(); i++) {
            if (i > 0) jsonBuilder.append(",");
            Object outerElement = outerArray.get(i);
            if (outerElement instanceof BMap outerRow) {
                Object innerValue = outerRow.get(innerArrayKey);
                if (innerValue instanceof BArray innerArray) {
                    jsonBuilder.append("[");
                    for (int j = 0; j < innerArray.size(); j++) {
                        if (j > 0) jsonBuilder.append(",");
                        Object innerElement = innerArray.get(j);
                        if (innerElement instanceof BMap innerRow) {
                            if (innerRow.containsKey(valueKey) && innerRow.size() == 1) {
                                appendJsonValue(jsonBuilder, innerRow.get(valueKey));
                            } else {
                                jsonBuilder.append(innerElement.toString());
                            }
                        } else {
                            appendJsonValue(jsonBuilder, innerElement);
                        }
                    }
                    jsonBuilder.append("]");
                } else {
                    jsonBuilder.append("[]");
                }
            } else {
                jsonBuilder.append("[]");
            }
        }
        jsonBuilder.append("]");
        return jsonBuilder.toString();
    }

    public static String transformMIStudioNestedTableTo2DArray(BArray outerArray, MessageContext context) {
        StringBuilder jsonBuilder = new StringBuilder("[");
        for (int i = 0; i < outerArray.size(); i++) {
            if (i > 0) jsonBuilder.append(",");
            Object outerElement = outerArray.get(i);
            if (outerElement instanceof BArray innerRow && innerRow.size() >= 2) {
                String innerTableStr = innerRow.get(1).toString();
                String innerArrayJson = parseInnerTableValues(innerTableStr, context);
                jsonBuilder.append(innerArrayJson);
            } else {
                jsonBuilder.append("[]");
            }
        }
        jsonBuilder.append("]");
        return jsonBuilder.toString();
    }

    private static String parseInnerTableValues(String innerTableStr, MessageContext context) {
        if (innerTableStr == null || innerTableStr.trim().isEmpty() || "[]".equals(innerTableStr.trim())) return "[]";

        String resolvedStr = SynapseUtils.resolveSynapseExpressions(innerTableStr, context);
        StringBuilder result = new StringBuilder("[");
        List<String> values = new ArrayList<>();

        Pattern resolvedExprPattern = Pattern.compile(",\\s*value=([^{}]+?)\\}\\}");
        Matcher resolvedMatcher = resolvedExprPattern.matcher(resolvedStr);
        while (resolvedMatcher.find()) values.add(resolvedMatcher.group(1).trim());

        if (values.isEmpty()) {
            Pattern plainPattern = Pattern.compile("\\{value=([^{}]+)\\}");
            Matcher plainMatcher = plainPattern.matcher(resolvedStr);
            while (plainMatcher.find()) values.add(plainMatcher.group(1).trim());
        }

        for (int i = 0; i < values.size(); i++) {
            if (i > 0) result.append(",");
            appendJsonValue(result, values.get(i));
        }
        result.append("]");
        return result.toString();
    }

    private static void appendJsonValue(StringBuilder builder, Object value) {
        if (value == null) {
            builder.append("null");
        } else {
            String str = value.toString();
            try { Long.parseLong(str); builder.append(str); return; } catch (NumberFormatException ignored) {}
            try { Double.parseDouble(str); builder.append(str); return; } catch (NumberFormatException ignored) {}
            if ("true".equals(str) || "false".equals(str)) { builder.append(str); return; }
            builder.append("\"").append(escapeJsonString(str)).append("\"");
        }
    }

    private static String escapeJsonString(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    public static String transformTableArrayToSimpleArray(BArray tableArray) {
        BString valueFieldName = StringUtils.fromString("value");
        StringBuilder jsonBuilder = new StringBuilder("[");
        for (int i = 0; i < tableArray.size(); i++) {
            if (i > 0) jsonBuilder.append(",");
            Object element = tableArray.get(i);
            if (element instanceof BMap row) {
                Object value = row.get(valueFieldName);
                if (value == null) throw new SynapseException("Table row missing 'value' field at index " + i);
                if (value instanceof BString) {
                    String strValue = ((BString) value).getValue();
                    jsonBuilder.append("\"").append(strValue.replace("\"", "\\\"")).append("\"");
                } else if (value instanceof Boolean || value instanceof Number) {
                    jsonBuilder.append(value.toString());
                } else {
                    jsonBuilder.append("\"").append(value.toString().replace("\"", "\\\"")).append("\"");
                }
            }
        }
        jsonBuilder.append("]");
        return jsonBuilder.toString();
    }
    
    // Helper methods for map transformation
    private static BMap transformTableToMap(BArray tableArray) {
        BMap resultMap = ValueCreator.createMapValue();
        BString keyFieldName = StringUtils.fromString("key");
        for (int i = 0; i < tableArray.size(); i++) {
            Object element = tableArray.get(i);
            if (element instanceof BMap row) {
                Object keyObj = row.get(keyFieldName);
                if (keyObj == null) throw new SynapseException("Table row missing 'key' field at index " + i);
                String keyStr = keyObj instanceof BString ? ((BString) keyObj).getValue() : keyObj.toString();
                BString key = StringUtils.fromString(keyStr);

                if (row.size() == 2 && row.containsKey(StringUtils.fromString("value"))) {
                    resultMap.put(key, row.get(StringUtils.fromString("value")));
                } else {
                    BMap recordValue = ValueCreator.createMapValue();
                    for (Object rowKey : row.getKeys()) {
                        BString rowKeyStr = (BString) rowKey;
                        if (!rowKeyStr.getValue().equals("key")) recordValue.put(rowKeyStr, row.get(rowKeyStr));
                    }
                    resultMap.put(key, recordValue);
                }
            }
        }
        return resultMap;
    }

    private static BMap transform2DArrayToMap(BArray array2D, String[] fieldNames) {
        BMap resultMap = ValueCreator.createMapValue();
        for (int i = 0; i < array2D.size(); i++) {
            Object element = array2D.get(i);
            if (element instanceof BArray row) {
                if (row.size() == 0) continue;
                Object keyObj = row.get(0);
                String keyStr = keyObj instanceof BString ? ((BString) keyObj).getValue() : keyObj.toString();
                BString key = StringUtils.fromString(keyStr);

                if (row.size() == 2) {
                    resultMap.put(key, row.get(1));
                } else if (row.size() > 2) {
                    BMap recordValue = ValueCreator.createMapValue();
                    for (int j = 1; j < row.size(); j++) {
                        String fieldName;
                        if (fieldNames != null && (j - 1) < fieldNames.length) fieldName = fieldNames[j - 1];
                        else fieldName = "field" + (j - 1);
                        recordValue.put(StringUtils.fromString(fieldName), row.get(j));
                    }
                    resultMap.put(key, recordValue);
                }
            }
        }
        return resultMap;
    }
}
