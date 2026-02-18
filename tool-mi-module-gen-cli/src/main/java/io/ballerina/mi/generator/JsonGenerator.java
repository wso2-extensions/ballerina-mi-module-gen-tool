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

import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.mi.model.attribute.*;
import io.ballerina.mi.model.param.*;
import io.ballerina.mi.util.JsonTemplateBuilder;
import io.ballerina.mi.util.Utils;
import org.apache.commons.lang3.StringUtils;
import org.ballerinalang.diagramutil.connector.models.connector.types.PathParamType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import static io.ballerina.mi.util.Constants.*;

/**
 * Generates JSON UI schema for the connector.
 */
public class JsonGenerator {

    public static void writeJsonAttributeForFunctionParam(FunctionParam functionParam, int index, int paramLength,
                                                           JsonTemplateBuilder builder,
                                                           boolean isCombo, boolean expandRecords) throws IOException {
        writeJsonAttributeForFunctionParam(functionParam, index, paramLength, builder, isCombo, expandRecords, null, false);
    }

    public static void writeJsonAttributeForFunctionParam(FunctionParam functionParam, int index, int paramLength,
                                                           JsonTemplateBuilder builder,
                                                           boolean isCombo, boolean expandRecords, String groupName,
                                                           boolean isConfigContext) throws IOException {
        String paramType = functionParam.getParamType();
        String originalParamValue = functionParam.getValue();
        String displayParamValue = originalParamValue;

        // For display purposes, remove group prefix if we're in a group context
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
        displayName = Utils.sanitizeParamName(displayName);

        String defaultValue = functionParam.getDefaultValue() != null ? functionParam.getDefaultValue() : "";
        switch (paramType) {
            case io.ballerina.mi.util.Constants.STRING:
            case io.ballerina.mi.util.Constants.XML:
                Attribute stringAttr = new Attribute(sanitizedParamName, displayName, INPUT_TYPE_STRING_OR_EXPRESSION,
                        defaultValue, functionParam.isRequired(), functionParam.getDescription(), "",
                        "", isCombo);
                stringAttr.setEnableCondition(functionParam.getEnableCondition());
                builder.addFromTemplate(ATTRIBUTE_TEMPLATE_PATH, stringAttr);
                break;
            case io.ballerina.mi.util.Constants.MAP:
                if (functionParam instanceof MapFunctionParam mapParam && mapParam.isRenderAsTable()) {
                    if (isConfigContext && !functionParam.isRequired()) {
                        addCheckboxForOptional(functionParam, mapParam, sanitizedParamName, displayName, builder);
                    }
                    writeMapAsTable(mapParam, builder, isCombo);
                } else {
                    Attribute mapAttr = new Attribute(sanitizedParamName, displayName,
                        INPUT_TYPE_STRING_OR_EXPRESSION, defaultValue, functionParam.isRequired(),
                        functionParam.getDescription() != null ? functionParam.getDescription() : "Expecting JSON object with key-value pairs",
                        JSON, "", isCombo);
                    mapAttr.setEnableCondition(functionParam.getEnableCondition());
                    builder.addFromTemplate(ATTRIBUTE_TEMPLATE_PATH, mapAttr);
                }
                break;
            case io.ballerina.mi.util.Constants.ARRAY:
                if (functionParam instanceof ArrayFunctionParam arrayParam && arrayParam.isRenderAsTable()) {
                    if (isConfigContext && !functionParam.isRequired()) {
                         addCheckboxForOptional(functionParam, arrayParam, sanitizedParamName, displayName, builder);
                    }
                    if (arrayParam.is2DArray()) {
                        writeNestedArrayAsTable(arrayParam, builder, isCombo);
                    } else if (arrayParam.isUnionArray()) {
                        writeUnionArrayAsTable(arrayParam, builder, isCombo);
                    } else {
                        writeArrayAsTable(arrayParam, builder, isCombo);
                    }
                } else {
                    Attribute arrayAttr = new Attribute(sanitizedParamName, displayName,
                        INPUT_TYPE_STRING_OR_EXPRESSION, defaultValue, functionParam.isRequired(),
                        functionParam.getDescription() != null ? functionParam.getDescription() : "Expecting JSON array",
                        JSON, "", isCombo);
                    arrayAttr.setEnableCondition(functionParam.getEnableCondition());
                    builder.addFromTemplate(ATTRIBUTE_TEMPLATE_PATH, arrayAttr);
                }
                break;
            case JSON:
                String jsonHelpTip = functionParam.getDescription();
                if (jsonHelpTip == null || jsonHelpTip.isEmpty()) {
                    jsonHelpTip = "Expecting JSON object";
                }
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
                    if (isConfigContext && !functionParam.isRequired()) {
                        addCheckboxForOptional(functionParam, functionParam, sanitizedParamName, displayName, builder);
                    }
                    writeRecordFields(functionParam, builder, expandRecords, groupName, isConfigContext);
                } else {
                    String recordHelpTip = functionParam.getDescription();
                    if (recordHelpTip == null || recordHelpTip.isEmpty()) {
                        recordHelpTip = "Expecting JSON object";
                    }
                    Attribute recordAttr = new Attribute(functionParam.getValue(), displayName,
                            INPUT_TYPE_STRING_OR_EXPRESSION, defaultValue, functionParam.isRequired(),
                            recordHelpTip, JSON, "", isCombo);
                    recordAttr.setEnableCondition(functionParam.getEnableCondition());
                    builder.addFromTemplate(ATTRIBUTE_TEMPLATE_PATH, recordAttr);
                }
                break;
            case io.ballerina.mi.util.Constants.INT:
                String intMatchPattern = functionParam.isRequired() ? INTEGER_REGEX : INTEGER_REGEX_OPTIONAL;
                Attribute intAttr = new Attribute(functionParam.getValue(), displayName, INPUT_TYPE_STRING_OR_EXPRESSION,
                        defaultValue, functionParam.isRequired(), functionParam.getDescription(), VALIDATE_TYPE_REGEX,
                        intMatchPattern, isCombo);
                intAttr.setEnableCondition(functionParam.getEnableCondition());
                builder.addFromTemplate(ATTRIBUTE_TEMPLATE_PATH, intAttr);
                break;
            case io.ballerina.mi.util.Constants.DECIMAL:
            case io.ballerina.mi.util.Constants.FLOAT:
                String decMatchPattern = functionParam.isRequired() ? DECIMAL_REGEX : DECIMAL_REGEX_OPTIONAL;
                Attribute decAttr = new Attribute(functionParam.getValue(), displayName,
                        INPUT_TYPE_STRING_OR_EXPRESSION, defaultValue, functionParam.isRequired(),
                        functionParam.getDescription(), VALIDATE_TYPE_REGEX, decMatchPattern, isCombo);
                decAttr.setEnableCondition(functionParam.getEnableCondition());
                builder.addFromTemplate(ATTRIBUTE_TEMPLATE_PATH, decAttr);
                break;
            case io.ballerina.mi.util.Constants.BOOLEAN:
                Attribute boolAttr = new Attribute(functionParam.getValue(), displayName, INPUT_TYPE_BOOLEAN,
                        defaultValue, functionParam.isRequired(), functionParam.getDescription(), "",
                        "", isCombo);
                boolAttr.setEnableCondition(functionParam.getEnableCondition());
                builder.addFromTemplate(ATTRIBUTE_TEMPLATE_PATH, boolAttr);
                break;
            case UNION:
                if (!(functionParam instanceof UnionFunctionParam unionFunctionParam)) {
                    throw new IllegalArgumentException("FunctionParam with paramType 'union' must be an instance of UnionFunctionParam for parameter: " + functionParam.getValue());
                }
                if (!unionFunctionParam.getUnionMemberParams().isEmpty()) {
                    List<FunctionParam> unionMembers = unionFunctionParam.getUnionMemberParams();
                    List<FunctionParam> validMembers = new ArrayList<>();
                    for (FunctionParam member : unionMembers) {
                        if (member instanceof UnionFunctionParam nestedUnion && nestedUnion.getUnionMemberParams().isEmpty()) {
                            continue;
                        }
                        validMembers.add(member);
                    }

                    boolean showCombo = validMembers.size() > 1;
                    String effectiveGroupName = groupName;
                    if (effectiveGroupName == null && originalParamValue != null && originalParamValue.contains(".")) {
                        String immediateParent = getImmediateParentSegment(originalParamValue);
                        if (immediateParent != null) {
                            effectiveGroupName = immediateParent;
                        }
                    }

                    if (showCombo) {
                        Combo comboField = getComboField(unionFunctionParam, functionParam.getValue(),
                                functionParam.getDescription(), effectiveGroupName);
                        builder.addFromTemplate(COMBO_TEMPLATE_PATH, comboField);

                        if (!validMembers.isEmpty()) {
                            builder.addSeparator(ATTRIBUTE_SEPARATOR);
                        }
                    } else if (validMembers.size() == 1) {
                        FunctionParam singleMember = validMembers.get(0);
                        singleMember.setEnableCondition(functionParam.getEnableCondition());
                    }

                    List<FunctionParam> recordMembers = new ArrayList<>();
                    List<FunctionParam> simpleMembers = new ArrayList<>();
                    
                    for (FunctionParam member : validMembers) {
                        if (member.getParamType().equals(RECORD) && expandRecords) {
                            recordMembers.add(member);
                        } else {
                            simpleMembers.add(member);
                        }
                    }

                    for (int i = 0; i < simpleMembers.size(); i++) {
                        writeJsonAttributeForFunctionParam(simpleMembers.get(i), index, paramLength, builder, true, expandRecords, effectiveGroupName, isConfigContext);
                        if (i < simpleMembers.size() - 1 || !recordMembers.isEmpty()) {
                            builder.addSeparator(ATTRIBUTE_SEPARATOR);
                        }
                    }
                    
                    if (!recordMembers.isEmpty()) {
                        RecordFunctionParam virtualRecordParam = new RecordFunctionParam(
                            Integer.toString(index), 
                            functionParam.getValue(), 
                            RECORD
                        );
                        virtualRecordParam.setEnableCondition(functionParam.getEnableCondition());
                        
                        for (FunctionParam member : recordMembers) {
                            if (member instanceof RecordFunctionParam recordParam) {
                                String memberCondition = member.getEnableCondition();
                                for (FunctionParam field : recordParam.getRecordFieldParams()) {
                                    String fieldCondition = field.getEnableCondition();
                                    String mergedCondition = mergeEnableConditions(memberCondition, fieldCondition);
                                    field.setEnableCondition(mergedCondition);
                                    virtualRecordParam.addRecordFieldParam(field);
                                }
                            }
                        }
                        writeRecordFields(virtualRecordParam, builder, true, effectiveGroupName, isConfigContext);
                    }
                }
                break;
            default:
                throw new IllegalArgumentException("Unsupported parameter type '" + paramType + "' for parameter: " + functionParam.getValue());
        }
        if (!isCombo) {
            builder.addConditionalSeparator((index < paramLength - 1), ATTRIBUTE_SEPARATOR);
        }
    }

    private static void addCheckboxForOptional(FunctionParam functionParam, FunctionParam targetParam, 
                                               String sanitizedParamName, String displayName, JsonTemplateBuilder builder) throws IOException {
        String checkboxName = "enable_" + sanitizedParamName;
        String checkboxDisplayName = "Configure " + displayName;
        Attribute checkbox = new Attribute(checkboxName, checkboxDisplayName,
                INPUT_TYPE_BOOLEAN, "false", false,
                "Enable to configure " + displayName + " settings",
                "", "", false);
        checkbox.setEnableCondition(functionParam.getEnableCondition());
        builder.addFromTemplate(ATTRIBUTE_TEMPLATE_PATH, checkbox);
        builder.addSeparator(ATTRIBUTE_SEPARATOR);

        String checkboxCondition = "[{\"" + checkboxName + "\":\"true\"}]";
        String existingCondition = functionParam.getEnableCondition();
        targetParam.setEnableCondition(mergeEnableConditions(existingCondition, checkboxCondition));
    }

    public static void writeJsonAttributeForPathParam(PathParamType pathParam, int index, int paramLength,
                                                      JsonTemplateBuilder builder) throws IOException {
        String paramType = pathParam.typeName;
        String sanitizedParamName = Utils.sanitizeParamName(pathParam.name);
        String displayName = pathParam.name;
        displayName = Utils.sanitizeParamName(displayName);
        String description = ""; 
        
        switch (paramType) {
            case io.ballerina.mi.util.Constants.STRING:
            case io.ballerina.mi.util.Constants.XML:
            case JSON:
            case io.ballerina.mi.util.Constants.MAP:
            case RECORD:
            case io.ballerina.mi.util.Constants.ARRAY:
                Attribute stringAttr = new Attribute(sanitizedParamName, displayName, INPUT_TYPE_STRING_OR_EXPRESSION,
                        "", true, description, "", "", false);
                builder.addFromTemplate(ATTRIBUTE_TEMPLATE_PATH, stringAttr);
                break;
            case io.ballerina.mi.util.Constants.INT:
                Attribute intAttr = new Attribute(sanitizedParamName, displayName, INPUT_TYPE_STRING_OR_EXPRESSION,
                        "", true, description, VALIDATE_TYPE_REGEX, INTEGER_REGEX, false);
                builder.addFromTemplate(ATTRIBUTE_TEMPLATE_PATH, intAttr);
                break;
            case io.ballerina.mi.util.Constants.DECIMAL:
            case io.ballerina.mi.util.Constants.FLOAT:
                Attribute decAttr = new Attribute(sanitizedParamName, displayName,
                        INPUT_TYPE_STRING_OR_EXPRESSION, "", true, description, 
                        VALIDATE_TYPE_REGEX, DECIMAL_REGEX, false);
                builder.addFromTemplate(ATTRIBUTE_TEMPLATE_PATH, decAttr);
                break;
            case io.ballerina.mi.util.Constants.BOOLEAN:
                Attribute boolAttr = new Attribute(sanitizedParamName, displayName, INPUT_TYPE_BOOLEAN,
                        "", true, description, "", "", false);
                builder.addFromTemplate(ATTRIBUTE_TEMPLATE_PATH, boolAttr);
                break;
            default:
                Attribute defaultAttr = new Attribute(sanitizedParamName, displayName, INPUT_TYPE_STRING_OR_EXPRESSION,
                        "", true, description, "", "", false);
                builder.addFromTemplate(ATTRIBUTE_TEMPLATE_PATH, defaultAttr);
                break;
        }
    }

    private static String getTypeNameOrFallback(FunctionParam member, String fallback) {
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

        FunctionParam firstMember = unionMembers.get(0);
        String defaultValue;
        if (firstMember.getParamType().equals(RECORD)) {
            defaultValue = getTypeNameOrFallback(firstMember, RECORD);
        } else if (firstMember.getParamType().equals(UNION)) {
            defaultValue = getTypeNameOrFallback(firstMember, UNION);
        } else {
            defaultValue = firstMember.getParamType();
        }
        String enableCondition = unionFunctionParam.getEnableCondition();
        String sanitizedParamName = Utils.sanitizeParamName(paramName);
        String comboName = String.format("%s%s", sanitizedParamName, "DataType");
        
        String comboDisplayName = comboName;
        if (groupName != null && !groupName.isEmpty() && paramName != null) {
            String displayParamName = removeGroupPrefix(paramName, groupName);
            if (displayParamName.contains(".")) {
                displayParamName = displayParamName.substring(displayParamName.lastIndexOf('.') + 1);
            }
            comboDisplayName = String.format("%s%s", Utils.sanitizeParamName(displayParamName), "DataType");
        }
        
        return new Combo(comboName, comboDisplayName, INPUT_TYPE_COMBO, unionComboValues, defaultValue,
                unionFunctionParam.isRequired(), enableCondition, helpTip);
    }

    private static void writeRecordFields(FunctionParam functionParam, JsonTemplateBuilder builder,
                                          boolean expandRecords, String parentGroupName, boolean isConfigContext) throws IOException {
        if (functionParam instanceof RecordFunctionParam recordParam && !recordParam.getRecordFieldParams().isEmpty()) {
            List<FunctionParam> recordFields = recordParam.getRecordFieldParams();
            
            java.util.Map<String, java.util.List<FunctionParam>> groupedFields = new java.util.LinkedHashMap<>();
            java.util.List<FunctionParam> topLevelFields = new java.util.ArrayList<>();
            
            for (FunctionParam fieldParam : recordFields) {
                String fieldName = fieldParam.getValue();
                String immediateParent = getImmediateParentSegment(fieldName);
                
                if (immediateParent != null && !immediateParent.isEmpty()) {
                    groupedFields.computeIfAbsent(immediateParent, k -> new java.util.ArrayList<>()).add(fieldParam);
                } else {
                    topLevelFields.add(fieldParam);
                }
            }
            
            for (int i = 0; i < topLevelFields.size(); i++) {
                FunctionParam fieldParam = topLevelFields.get(i);
                
                String parentCondition = functionParam.getEnableCondition();
                if (parentCondition != null && !parentCondition.isEmpty()) {
                    String currentCondition = fieldParam.getEnableCondition();
                    String mergedCondition = mergeEnableConditions(parentCondition, currentCondition);
                    fieldParam.setEnableCondition(mergedCondition);
                }
                
                writeJsonAttributeForFunctionParam(fieldParam, i, topLevelFields.size(), builder, false, expandRecords, parentGroupName, isConfigContext);
            }
            
            int groupIndex = 0;
            int totalGroups = groupedFields.size();
            for (java.util.Map.Entry<String, java.util.List<FunctionParam>> groupEntry : groupedFields.entrySet()) {
                String groupName = groupEntry.getKey();
                java.util.List<FunctionParam> groupFields = groupEntry.getValue();
                
                if (!topLevelFields.isEmpty() || groupIndex > 0) {
                    builder.addSeparator(ATTRIBUTE_SEPARATOR);
                }
                
                String displayGroupName = camelCaseToTitleCase(groupName);

                if (parentGroupName != null && displayGroupName.equals(parentGroupName)) {
                    for (int i = 0; i < groupFields.size(); i++) {
                        FunctionParam fieldParam = groupFields.get(i);
                        
                        String parentCondition = functionParam.getEnableCondition();
                        if (parentCondition != null && !parentCondition.isEmpty()) {
                            String currentCondition = fieldParam.getEnableCondition();
                            String mergedCondition = mergeEnableConditions(parentCondition, currentCondition);
                            fieldParam.setEnableCondition(mergedCondition);
                        }

                        writeJsonAttributeForFunctionParam(fieldParam, i, groupFields.size(), builder, false, expandRecords, groupName, isConfigContext);

                         if (i < groupFields.size() - 1) {
                            builder.addSeparator(ATTRIBUTE_SEPARATOR);
                        }
                    }
                } else {
                    AttributeGroup attributeGroup = new AttributeGroup(displayGroupName);
                    String parentCondition = functionParam.getEnableCondition();
                    if (parentCondition != null && !parentCondition.isEmpty()) {
                        attributeGroup.setEnableCondition(parentCondition);
                    }

                    builder.addFromTemplate(ATTRIBUTE_GROUP_TEMPLATE_PATH, attributeGroup);

                    for (int i = 0; i < groupFields.size(); i++) {
                        FunctionParam fieldParam = groupFields.get(i);
                        if (parentCondition != null && !parentCondition.isEmpty()) {
                            String currentCondition = fieldParam.getEnableCondition();
                            String mergedCondition = mergeEnableConditions(parentCondition, currentCondition);
                            fieldParam.setEnableCondition(mergedCondition);
                        }
                        if (i == 0) {
                            builder.addSeparator("                  "); 
                        }
                        writeJsonAttributeForFunctionParam(fieldParam, i, groupFields.size(), builder, false, expandRecords, groupName, isConfigContext);
                    }

                    builder.addSeparator("\n                    ]");
                    builder.addSeparator("\n                  }");
                }
                
                if (!(parentGroupName != null && displayGroupName.equals(parentGroupName))) { 
                    if (groupIndex < totalGroups - 1) {
                       builder.addSeparator("\n                },");
                    } else {
                       builder.addSeparator("\n                }");
                    }
                } else {
                     if (groupIndex < totalGroups - 1) {
                        builder.addSeparator(ATTRIBUTE_SEPARATOR);
                     }
                }
                
                groupIndex++;
            }
        } else {
            Attribute recordAttr = new Attribute(functionParam.getValue(), functionParam.getValue(),
                    INPUT_TYPE_STRING_OR_EXPRESSION, "", functionParam.isRequired(),
                    functionParam.getDescription(), "", "", false);
            recordAttr.setEnableCondition(functionParam.getEnableCondition());
            builder.addFromTemplate(ATTRIBUTE_TEMPLATE_PATH, recordAttr);
        }
    }

    private static void writeMapAsTable(MapFunctionParam mapParam, JsonTemplateBuilder builder, boolean isCombo)
            throws IOException {

        String paramName = Utils.sanitizeParamName(mapParam.getValue());
        String displayName = mapParam.getValue();
        if (displayName.contains(".")) {
            displayName = displayName.substring(displayName.lastIndexOf('.') + 1);
        }
        displayName = Utils.sanitizeParamName(displayName);

        List<Element> tableColumns = new ArrayList<>();

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

        if (mapParam.getValueFieldParams() != null && !mapParam.getValueFieldParams().isEmpty()) {
            for (FunctionParam valueField : mapParam.getValueFieldParams()) {
                Attribute column = createAttributeForMapValueField(valueField);
                tableColumns.add(column);
            }
        } else {
            Attribute valueColumn = createSimpleValueColumn(mapParam);
            tableColumns.add(valueColumn);
        }

        String description = mapParam.getDescription() != null ?
            mapParam.getDescription() :
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
            mapParam.getEnableCondition(),
            mapParam.isRequired()
        );

        builder.addFromTemplate(TABLE_TEMPLATE_PATH, table);
    }

    private static void writeArrayAsTable(ArrayFunctionParam arrayParam, JsonTemplateBuilder builder, boolean isCombo)
            throws IOException {

        String paramName = Utils.sanitizeParamName(arrayParam.getValue());
        String displayName = arrayParam.getValue();
        if (displayName.contains(".")) {
            displayName = displayName.substring(displayName.lastIndexOf('.') + 1);
        }
        displayName = Utils.sanitizeParamName(displayName);

        List<Element> tableColumns = new ArrayList<>();

        if (arrayParam.getElementFieldParams() != null && !arrayParam.getElementFieldParams().isEmpty()) {
            for (FunctionParam elementField : arrayParam.getElementFieldParams()) {
                Attribute column = createAttributeForElementField(elementField);
                tableColumns.add(column);
            }
        } else {
            Attribute column = createSimpleElementColumn(arrayParam);
            tableColumns.add(column);
        }

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

    private static void writeNestedArrayAsTable(ArrayFunctionParam arrayParam, JsonTemplateBuilder builder,
                                                 boolean isCombo) throws IOException {

        String paramName = Utils.sanitizeParamName(arrayParam.getValue());
        String displayName = arrayParam.getValue();
        if (displayName.contains(".")) {
            displayName = displayName.substring(displayName.lastIndexOf('.') + 1);
        }
        displayName = Utils.sanitizeParamName(displayName);

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

    private static void writeUnionArrayAsTable(ArrayFunctionParam arrayParam, JsonTemplateBuilder builder,
                                                boolean isCombo) throws IOException {

        String paramName = Utils.sanitizeParamName(arrayParam.getValue());
        String displayName = arrayParam.getValue();
        if (displayName.contains(".")) {
            displayName = displayName.substring(displayName.lastIndexOf('.') + 1);
        }
        displayName = Utils.sanitizeParamName(displayName);

        List<Element> tableColumns = new ArrayList<>();

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
                break;
        }

        return new Attribute(
            "value",
            "Value",
            inputType,
            "",
            true, 
            helpTip,
            validateType,
            matchPattern,
            false
        );
    }

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
                columnName = "value";
                break;
        }

        return new Attribute(
            columnName,
            StringUtils.capitalize(columnName),
            inputType,
            "",
            true,  
            helpTip,
            validateType,
            matchPattern,
            false
        );
    }

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

    public static String camelCaseToTitleCase(String camelCase) {
        if (camelCase == null || camelCase.isEmpty()) {
            return camelCase;
        }
        String result = camelCase.replaceAll("([a-z])([A-Z])", "$1 $2")
                                  .replaceAll("([A-Z])([A-Z][a-z])", "$1 $2");
        return StringUtils.capitalize(result);
    }

    public static String removeGroupPrefix(String fieldName, String groupName) {
        if (fieldName == null || groupName == null) {
            return fieldName;
        }
        String prefix = groupName + ".";
        
        if (fieldName.startsWith(prefix)) {
            return fieldName.substring(prefix.length());
        }
        
        String groupSegment = "." + prefix;
        int groupIndex = fieldName.indexOf(groupSegment);
        if (groupIndex >= 0) {
            return fieldName.substring(groupIndex + groupSegment.length());
        }
        
        return fieldName;
    }

    private static String getImmediateParentSegment(String qualifiedName) {
        if (qualifiedName == null || qualifiedName.isEmpty()) {
            return null;
        }

        int lastDotIndex = qualifiedName.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return null;
        }

        String beforeLastDot = qualifiedName.substring(0, lastDotIndex);

        int secondLastDotIndex = beforeLastDot.lastIndexOf('.');
        if (secondLastDotIndex == -1) {
            return beforeLastDot;
        }

        return beforeLastDot.substring(secondLastDotIndex + 1);
    }
    
    public static String mergeEnableConditions(String parentCondition, String childCondition) {
        if (parentCondition == null || parentCondition.isEmpty()) {
            return childCondition;
        }
        if (childCondition == null || childCondition.isEmpty()) {
            return parentCondition;
        }

        String parentObj = extractConditionObject(parentCondition);
        String childObj = extractConditionObject(childCondition);

        if (parentObj == null || childObj == null) {
            return parentCondition;
        }

        // If parent is already an AND array, append child to it
        if (parentCondition.startsWith("[\"AND\"")) {
            // parentCondition = ["AND", {...}, {...}]
            // Insert child before the closing ]
            return parentCondition.substring(0, parentCondition.length() - 1) + "," + childObj + "]";
        }

        // Both are simple conditions — combine into AND array
        return "[\"AND\"," + parentObj + "," + childObj + "]";
    }

    private static String extractConditionObject(String condition) {
        if (condition == null || condition.isEmpty()) {
            return null;
        }
        // Simple format: [{"key":"val"}] -> {"key":"val"}
        if (condition.startsWith("[{") && condition.endsWith("}]")) {
            return condition.substring(1, condition.length() - 1);
        }
        // AND format: ["AND",...] -> return as nested array
        if (condition.startsWith("[\"AND\"")) {
            return condition;
        }
        return null;
    }

    public static void writeAttributeGroup(String groupName, List<FunctionParam> params, boolean isLastGroup, JsonTemplateBuilder builder, boolean isConfigContext) {
        writeAttributeGroup(groupName, params, isLastGroup, builder, false, isConfigContext);
    }

    public static void writeAttributeGroup(String groupName, List<FunctionParam> params, boolean isLastGroup, JsonTemplateBuilder builder, boolean collapsed, boolean isConfigContext) {
        try {
            AttributeGroup attributeGroup = new AttributeGroup(groupName, collapsed);
            builder.addFromTemplate(ATTRIBUTE_GROUP_TEMPLATE_PATH, attributeGroup);

            for (int i = 0; i < params.size(); i++) {
                if (i == 0) {
                    builder.addSeparator("                  "); // Indentation alignment
                }
                // Write param - expand records
                writeJsonAttributeForFunctionParam(params.get(i), i, params.size(), builder, false, true, groupName, isConfigContext);
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
}
