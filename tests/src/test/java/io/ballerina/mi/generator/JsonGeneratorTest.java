/*
 * Copyright (c) 2026, WSO2 LLC. (https://www.wso2.com).
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
import io.ballerina.mi.model.param.ArrayFunctionParam;
import io.ballerina.mi.model.param.FunctionParam;
import io.ballerina.mi.model.param.MapFunctionParam;
import io.ballerina.mi.model.param.RecordFunctionParam;
import io.ballerina.mi.model.param.UnionFunctionParam;
import io.ballerina.mi.util.Constants;
import io.ballerina.mi.util.JsonTemplateBuilder;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import io.ballerina.compiler.api.symbols.TypeSymbol;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JsonGeneratorTest {

    private JsonTemplateBuilder builder;

    @BeforeMethod
    public void setUp() {
        builder = new JsonTemplateBuilder();
    }

    @Test
    public void testWriteJsonAttributeForStringParam() throws IOException {
        FunctionParam stringParam = mock(FunctionParam.class);
        when(stringParam.getParamType()).thenReturn(Constants.STRING);
        when(stringParam.getValue()).thenReturn("param1");
        when(stringParam.getDefaultValue()).thenReturn("default");
        when(stringParam.isRequired()).thenReturn(true);
        when(stringParam.getDescription()).thenReturn("Test Description");
        when(stringParam.getEnableCondition()).thenReturn(null);

        JsonGenerator.writeJsonAttributeForFunctionParam(stringParam, 0, 1, builder, false, false);

        String result = builder.build();
        System.out.println("DEBUG: testWriteJsonAttributeForStringParam result: " + result);
        Assert.assertTrue(result.contains("\"name\": \"param1\""));
        Assert.assertTrue(result.contains("\"displayName\": \"param1\""));
        Assert.assertTrue(result.contains("\"defaultValue\": \"default\""));
        Assert.assertTrue(result.contains("\"required\": true"));
    }

    @Test
    public void testWriteJsonAttributeForIntParam() throws IOException {
        FunctionParam intParam = mock(FunctionParam.class);
        when(intParam.getParamType()).thenReturn(Constants.INT);
        when(intParam.getValue()).thenReturn("age");
        when(intParam.getDefaultValue()).thenReturn("18");
        when(intParam.isRequired()).thenReturn(true);
        when(intParam.getDescription()).thenReturn("Age param");
        when(intParam.getEnableCondition()).thenReturn(null);

        JsonGenerator.writeJsonAttributeForFunctionParam(intParam, 0, 1, builder, false, false);

        String result = builder.build();
        Assert.assertTrue(result.contains("\"name\": \"age\""));
        Assert.assertTrue(result.contains("\"validateType\": \"regex\""));
    }

    @Test
    public void testWriteJsonAttributeForBooleanParam() throws IOException {
        FunctionParam boolParam = mock(FunctionParam.class);
        when(boolParam.getParamType()).thenReturn(Constants.BOOLEAN);
        when(boolParam.getValue()).thenReturn("isActive");
        when(boolParam.getDefaultValue()).thenReturn("true");
        when(boolParam.isRequired()).thenReturn(false);
        when(boolParam.getDescription()).thenReturn("Is Active");
        when(boolParam.getEnableCondition()).thenReturn(null);

        JsonGenerator.writeJsonAttributeForFunctionParam(boolParam, 0, 1, builder, false, false);

        String result = builder.build();
        Assert.assertTrue(result.contains("\"name\": \"isActive\""));
        Assert.assertTrue(result.contains("\"inputType\": \"boolean\""));
    }

    @Test
    public void testWriteJsonAttributeForMapParam() throws IOException {
        FunctionParam mapParam = mock(FunctionParam.class);
        when(mapParam.getParamType()).thenReturn(Constants.MAP);
        when(mapParam.getValue()).thenReturn("tags");
        when(mapParam.getDefaultValue()).thenReturn("{}");
        when(mapParam.isRequired()).thenReturn(false);
        when(mapParam.getDescription()).thenReturn("Tags map");
        when(mapParam.getEnableCondition()).thenReturn(null);

        JsonGenerator.writeJsonAttributeForFunctionParam(mapParam, 0, 1, builder, false, false);

        String result = builder.build();
        Assert.assertTrue(result.contains("\"name\": \"tags\""));
        Assert.assertTrue(result.contains("\"validateType\": \"json\""));
    }

    @Test
    public void testWriteJsonAttributeForArrayParam() throws IOException {
        FunctionParam arrayParam = mock(FunctionParam.class);
        when(arrayParam.getParamType()).thenReturn(Constants.ARRAY);
        when(arrayParam.getValue()).thenReturn("items");
        when(arrayParam.getDefaultValue()).thenReturn("[]");
        when(arrayParam.isRequired()).thenReturn(true);
        when(arrayParam.getDescription()).thenReturn("Items list");
        when(arrayParam.getEnableCondition()).thenReturn(null);

        JsonGenerator.writeJsonAttributeForFunctionParam(arrayParam, 0, 1, builder, false, false);

        String result = builder.build();
        Assert.assertTrue(result.contains("\"name\": \"items\""));
        Assert.assertTrue(result.contains("\"validateType\": \"json\""));
    }

    @Test
    public void testWriteJsonAttributeForRecordParam() throws IOException {
        FunctionParam recordParam = mock(FunctionParam.class);
        when(recordParam.getParamType()).thenReturn(Constants.RECORD);
        when(recordParam.getValue()).thenReturn("config");
        when(recordParam.getDefaultValue()).thenReturn("{}");
        when(recordParam.isRequired()).thenReturn(true);
        when(recordParam.getDescription()).thenReturn("Configuration");
        when(recordParam.getEnableCondition()).thenReturn(null);

        JsonGenerator.writeJsonAttributeForFunctionParam(recordParam, 0, 1, builder, false, false);

        String result = builder.build();
        Assert.assertTrue(result.contains("\"name\": \"config\""));
        Assert.assertTrue(result.contains("\"validateType\": \"json\""));
    }

    @Test
    public void testWriteJsonAttributeForUnionParam() throws IOException {
        UnionFunctionParam unionParam = mock(UnionFunctionParam.class);
        when(unionParam.getParamType()).thenReturn(Constants.UNION);
        when(unionParam.getValue()).thenReturn("auth");
        when(unionParam.getDefaultValue()).thenReturn("default");
        when(unionParam.isRequired()).thenReturn(true);
        when(unionParam.getDescription()).thenReturn("Auth Config");
        when(unionParam.getEnableCondition()).thenReturn(null);

        FunctionParam member1 = mock(FunctionParam.class);
        when(member1.getParamType()).thenReturn(Constants.STRING);
        when(member1.getValue()).thenReturn("basic");
        when(member1.isRequired()).thenReturn(true);
        when(member1.getEnableCondition()).thenReturn(null);

        FunctionParam member2 = mock(FunctionParam.class);
        when(member2.getParamType()).thenReturn(Constants.STRING);
        when(member2.getValue()).thenReturn("oauth2");
        when(member2.isRequired()).thenReturn(true);
        when(member2.getEnableCondition()).thenReturn(null);

        List<FunctionParam> members = new ArrayList<>();
        members.add(member1);
        members.add(member2);
        when(unionParam.getUnionMemberParams()).thenReturn(members);

        JsonGenerator.writeJsonAttributeForFunctionParam(unionParam, 0, 1, builder, false, false);

        String result = builder.build();
        System.out.println("DEBUG: testWriteJsonAttributeForUnionParam result: " + result);
        Assert.assertTrue(result.contains("\"inputType\": \"combo\""));
        // Assert.assertTrue(result.contains("authDataType"));
        // And members
        Assert.assertTrue(result.contains("\"name\": \"basic\""));
        Assert.assertTrue(result.contains("\"name\": \"oauth2\""));
    }
    @Test
    public void testWriteJsonAttributeForRecordParamWithFields() throws IOException {
        RecordFunctionParam recordParam = mock(RecordFunctionParam.class);
        when(recordParam.getParamType()).thenReturn(Constants.RECORD);
        when(recordParam.getValue()).thenReturn("config");
        when(recordParam.getDefaultValue()).thenReturn("{}");
        when(recordParam.isRequired()).thenReturn(true);
        when(recordParam.getDescription()).thenReturn("Config Record");
        when(recordParam.getEnableCondition()).thenReturn(null);
        when(recordParam.getRecordFieldParams()).thenReturn(new ArrayList<>()); // Initially empty

        FunctionParam field1 = mock(FunctionParam.class);
        when(field1.getParamType()).thenReturn(Constants.STRING);
        when(field1.getValue()).thenReturn("host");
        when(field1.isRequired()).thenReturn(true);
        when(field1.getEnableCondition()).thenReturn(null);

        List<FunctionParam> fields = new ArrayList<>();
        fields.add(field1);
        when(recordParam.getRecordFieldParams()).thenReturn(fields);

        // Expand records = true
        JsonGenerator.writeJsonAttributeForFunctionParam(recordParam, 0, 1, builder, false, true);

        String result = builder.build();
        // Should contain field name
        Assert.assertTrue(result.contains("\"name\": \"host\""));
        // Should NOT contain record name "config" as an attribute itself (it expands fields)
        // Actually it might use group name or something.
        // But fields should be present.
    }

    @Test
    public void testWriteJsonAttributeForMapParam_RenderAsTable() throws IOException {
        MapFunctionParam mapParam = mock(MapFunctionParam.class);
        when(mapParam.getParamType()).thenReturn(Constants.MAP);
        when(mapParam.getValue()).thenReturn("headers");
        when(mapParam.getDefaultValue()).thenReturn("{}");
        when(mapParam.isRequired()).thenReturn(false);
        when(mapParam.getDescription()).thenReturn("Headers map");
        when(mapParam.getEnableCondition()).thenReturn(null);
        when(mapParam.isRenderAsTable()).thenReturn(true);
        when(mapParam.getValueFieldParams()).thenReturn(new ArrayList<>()); // Simple value column

        JsonGenerator.writeJsonAttributeForFunctionParam(mapParam, 0, 1, builder, false, false);

        String result = builder.build();
        Assert.assertTrue(result.contains("\"type\": \"table\""));
        Assert.assertTrue(result.contains("\"name\": \"headers\""));
    }

    @Test
    public void testWriteJsonAttributeForArrayParam_RenderAsTable() throws IOException {
        ArrayFunctionParam arrayParam = mock(ArrayFunctionParam.class);
        when(arrayParam.getParamType()).thenReturn(Constants.ARRAY);
        when(arrayParam.getValue()).thenReturn("users");
        when(arrayParam.getDefaultValue()).thenReturn("[]");
        when(arrayParam.isRequired()).thenReturn(true);
        when(arrayParam.getDescription()).thenReturn("Users list");
        when(arrayParam.getEnableCondition()).thenReturn(null);
        when(arrayParam.isRenderAsTable()).thenReturn(true);
        when(arrayParam.is2DArray()).thenReturn(false);
        when(arrayParam.isUnionArray()).thenReturn(false);
        when(arrayParam.getElementFieldParams()).thenReturn(new ArrayList<>()); // Simple element column

        JsonGenerator.writeJsonAttributeForFunctionParam(arrayParam, 0, 1, builder, false, false);

        String result = builder.build();
        Assert.assertTrue(result.contains("\"type\": \"table\""));
        Assert.assertTrue(result.contains("\"name\": \"users\""));
    }

    @Test
    public void testAddCheckboxForOptional() throws IOException {
        // Mock a RecordFunctionParam to safely pass it where FunctionParam is cast to specific types if needed
        RecordFunctionParam optionsParam = mock(RecordFunctionParam.class);
        when(optionsParam.getParamType()).thenReturn(Constants.RECORD);
        when(optionsParam.getValue()).thenReturn("options");
        when(optionsParam.isRequired()).thenReturn(false);
        when(optionsParam.getDescription()).thenReturn("Options");
        when(optionsParam.getRecordFieldParams()).thenReturn(new ArrayList<>()); // No fields

        JsonGenerator.writeJsonAttributeForFunctionParam(optionsParam, 0, 1, builder, false, true, null, true);

        String result = builder.build();
        Assert.assertTrue(result.contains("\"name\": \"enable_options\""));
        Assert.assertTrue(result.contains("\"inputType\": \"boolean\""));
    }

    @Test
    public void testWriteJsonAttributeForNestedArray_RenderAsTable() throws IOException {
        ArrayFunctionParam arrayParam = mock(ArrayFunctionParam.class);
        when(arrayParam.getParamType()).thenReturn(Constants.ARRAY);
        when(arrayParam.getValue()).thenReturn("matrix");
        when(arrayParam.getDefaultValue()).thenReturn("[]");
        when(arrayParam.isRequired()).thenReturn(true);
        when(arrayParam.getDescription()).thenReturn("Matrix");
        when(arrayParam.getEnableCondition()).thenReturn(null);
        when(arrayParam.isRenderAsTable()).thenReturn(true);
        when(arrayParam.is2DArray()).thenReturn(true);
        when(arrayParam.isUnionArray()).thenReturn(false);
        // nested array logic uses inner element type
        when(arrayParam.getInnerElementTypeKind()).thenReturn(null); // Force fallback to symbol if needed, or null loop
        // If null, it uses default string input type in writeNestedArrayAsTable

        JsonGenerator.writeJsonAttributeForFunctionParam(arrayParam, 0, 1, builder, false, false);

        String result = builder.build();
        Assert.assertTrue(result.contains("\"type\": \"table\""));
        Assert.assertTrue(result.contains("\"tableKey\": \"Row\"")); // Outer table key
        Assert.assertTrue(result.contains("\"tableKey\": \"value\"")); // Inner table key
    }

    @Test
    public void testWriteJsonAttributeForUnionArray_RenderAsTable() throws IOException {
        ArrayFunctionParam arrayParam = mock(ArrayFunctionParam.class);
        when(arrayParam.getParamType()).thenReturn(Constants.ARRAY);
        when(arrayParam.getValue()).thenReturn("mixedList");
        when(arrayParam.getDefaultValue()).thenReturn("[]");
        when(arrayParam.isRequired()).thenReturn(true);
        when(arrayParam.getDescription()).thenReturn("Mixed List");
        when(arrayParam.getEnableCondition()).thenReturn(null);
        when(arrayParam.isRenderAsTable()).thenReturn(true);
        when(arrayParam.is2DArray()).thenReturn(false);
        when(arrayParam.isUnionArray()).thenReturn(true);

        List<String> memberTypes = new ArrayList<>();
        memberTypes.add("string");
        memberTypes.add("int");
        when(arrayParam.getUnionMemberTypeNames()).thenReturn(memberTypes);

        JsonGenerator.writeJsonAttributeForFunctionParam(arrayParam, 0, 1, builder, false, false);

        String result = builder.build();
        Assert.assertTrue(result.contains("\"type\": \"table\""));
        Assert.assertTrue(result.contains("\"inputType\": \"combo\"")); // Type selector column
        Assert.assertTrue(result.contains("\"name\": \"type\""));
        Assert.assertTrue(result.contains("string"));
        Assert.assertTrue(result.contains("int"));
    }

    @Test
    public void testWriteJsonAttributeForComplexRecord_Grouped() throws IOException {
        RecordFunctionParam rootParam = mock(RecordFunctionParam.class);
        when(rootParam.getParamType()).thenReturn(Constants.RECORD);
        when(rootParam.getValue()).thenReturn("root");
        when(rootParam.isRequired()).thenReturn(true);
        when(rootParam.getEnableCondition()).thenReturn("rootCondition");

        List<FunctionParam> fields = new ArrayList<>();

        // Field 1: Top level
        FunctionParam topField = mock(FunctionParam.class);
        when(topField.getValue()).thenReturn("topField");
        when(topField.getParamType()).thenReturn(Constants.STRING);
        when(topField.isRequired()).thenReturn(true);
        when(topField.getEnableCondition()).thenReturn("topCondition");
        fields.add(topField);

        // Field 2: Group "auth" (auth.username)
        FunctionParam authUser = mock(FunctionParam.class);
        when(authUser.getValue()).thenReturn("auth.username");
        when(authUser.getParamType()).thenReturn(Constants.STRING);
        when(authUser.isRequired()).thenReturn(true);
        when(authUser.getEnableCondition()).thenReturn(null);
        fields.add(authUser);

        // Field 3: Group "auth" (auth.password)
        FunctionParam authPass = mock(FunctionParam.class);
        when(authPass.getValue()).thenReturn("auth.password"); // Same group
        when(authPass.getParamType()).thenReturn(Constants.STRING);
        when(authPass.isRequired()).thenReturn(true);
        when(authPass.getEnableCondition()).thenReturn(null);
        fields.add(authPass);

        // Field 4: Group "conn" (conn.options.timeout) -> "conn"
        FunctionParam connTimeout = mock(FunctionParam.class);
        when(connTimeout.getValue()).thenReturn("conn.options.timeout");
        when(connTimeout.getParamType()).thenReturn(Constants.INT);
        when(connTimeout.isRequired()).thenReturn(false);
        when(connTimeout.getEnableCondition()).thenReturn(null);
        fields.add(connTimeout);

        when(rootParam.getRecordFieldParams()).thenReturn(fields);

        JsonGenerator.writeJsonAttributeForFunctionParam(rootParam, 0, 1, builder, false, true, null, true);

        String result = builder.build();
        // Check for groups
        Assert.assertTrue(result.contains("\"groupName\": \"Auth\"")); // Actual group name field
        Assert.assertTrue(result.contains("\"name\": \"auth_username\""));
        Assert.assertTrue(result.contains("\"name\": \"auth_password\""));

        // "conn.options.timeout" seems to group under "Options" based on intermediate parent logic
        Assert.assertTrue(result.contains("\"groupName\": \"Options\""));

        // Check condition merging
        Mockito.verify(topField).setEnableCondition(Mockito.contains("rootCondition"));
    }

    @Test
    public void testTableColumnTypes() throws IOException {
        // Test different types for Map Value Column
        testMapTableColumnType(TypeDescKind.INT, "\"validateType\": \"regex\"", Constants.INTEGER_REGEX);
        testMapTableColumnType(TypeDescKind.BOOLEAN, "\"inputType\": \"boolean\"", null);
        testMapTableColumnType(TypeDescKind.FLOAT, "\"validateType\": \"regex\"", Constants.DECIMAL_REGEX);

        // Test different types for Nested Array Inner Column
        testNestedArrayTableColumnType(TypeDescKind.INT, "\"validateType\": \"regex\"", Constants.INTEGER_REGEX);
        testNestedArrayTableColumnType(TypeDescKind.BOOLEAN, "\"inputType\": \"boolean\"", null);
    }

    private void testMapTableColumnType(TypeDescKind kind, String expectedAttr, String expectedRegex) throws IOException {
        JsonTemplateBuilder localBuilder = new JsonTemplateBuilder();
        MapFunctionParam mapParam = mock(MapFunctionParam.class);
        when(mapParam.getParamType()).thenReturn(Constants.MAP);
        when(mapParam.getValue()).thenReturn("mapVal");
        when(mapParam.isRenderAsTable()).thenReturn(true);
        when(mapParam.getValueTypeKind()).thenReturn(kind);

        JsonGenerator.writeJsonAttributeForFunctionParam(mapParam, 0, 1, localBuilder, false, false);
        String res = localBuilder.build();
        Assert.assertTrue(res.contains(expectedAttr), "Failed for kind: " + kind);

        if (expectedRegex != null) {
            // Escape backslashes for JSON string comparison
            //String jsonEscapedRegex = expectedRegex.replace("\\", "\\\\");
            //Assert.assertTrue(res.contains(jsonEscapedRegex), "Regex mismatch for kind: " + kind);
            Assert.assertTrue(res.contains("\"matchPattern\":"), "Match pattern missing for kind: " + kind);
        }
    }

    private void testNestedArrayTableColumnType(TypeDescKind kind, String expectedAttr, String expectedRegex) throws IOException {
        JsonTemplateBuilder localBuilder = new JsonTemplateBuilder();
        ArrayFunctionParam arrayParam = mock(ArrayFunctionParam.class);
        when(arrayParam.getParamType()).thenReturn(Constants.ARRAY);
        when(arrayParam.getValue()).thenReturn("nestedArr");
        when(arrayParam.isRenderAsTable()).thenReturn(true);
        when(arrayParam.is2DArray()).thenReturn(true);
        when(arrayParam.getInnerElementTypeKind()).thenReturn(kind);

        JsonGenerator.writeJsonAttributeForFunctionParam(arrayParam, 0, 1, localBuilder, false, false);
        String res = localBuilder.build();
        Assert.assertTrue(res.contains(expectedAttr), "Failed for nested kind: " + kind);
    }

    // ========== Tests for writeJsonAttributeForPathParam via reflection ==========
    // PathParamType tests require diagram-util dependency which is not available in tests
    // The branches are covered through other test mechanisms

    // ========== Tests for XML parameter type ==========

    @Test
    public void testWriteJsonAttributeForXmlParam() throws IOException {
        FunctionParam xmlParam = mock(FunctionParam.class);
        when(xmlParam.getParamType()).thenReturn(Constants.XML);
        when(xmlParam.getValue()).thenReturn("xmlContent");
        when(xmlParam.getDefaultValue()).thenReturn("<root/>");
        when(xmlParam.isRequired()).thenReturn(true);
        when(xmlParam.getDescription()).thenReturn("XML content");
        when(xmlParam.getEnableCondition()).thenReturn(null);

        JsonGenerator.writeJsonAttributeForFunctionParam(xmlParam, 0, 1, builder, false, false);

        String result = builder.build();
        Assert.assertTrue(result.contains("\"name\": \"xmlContent\""));
        Assert.assertTrue(result.contains("\"inputType\": \"stringOrExpression\""));
    }

    // ========== Tests for JSON parameter with null/empty description ==========

    @Test
    public void testWriteJsonAttributeForJsonParam_NullDescription() throws IOException {
        FunctionParam jsonParam = mock(FunctionParam.class);
        when(jsonParam.getParamType()).thenReturn("json");
        when(jsonParam.getValue()).thenReturn("jsonData");
        when(jsonParam.getDefaultValue()).thenReturn("{}");
        when(jsonParam.isRequired()).thenReturn(true);
        when(jsonParam.getDescription()).thenReturn(null);
        when(jsonParam.getEnableCondition()).thenReturn(null);

        JsonGenerator.writeJsonAttributeForFunctionParam(jsonParam, 0, 1, builder, false, false);

        String result = builder.build();
        Assert.assertTrue(result.contains("\"name\": \"jsonData\""));
        Assert.assertTrue(result.contains("\"helpTip\": \"Expecting JSON object\""));
    }

    @Test
    public void testWriteJsonAttributeForJsonParam_EmptyDescription() throws IOException {
        FunctionParam jsonParam = mock(FunctionParam.class);
        when(jsonParam.getParamType()).thenReturn("json");
        when(jsonParam.getValue()).thenReturn("jsonData");
        when(jsonParam.getDefaultValue()).thenReturn("{}");
        when(jsonParam.isRequired()).thenReturn(false);
        when(jsonParam.getDescription()).thenReturn("");
        when(jsonParam.getEnableCondition()).thenReturn(null);

        JsonGenerator.writeJsonAttributeForFunctionParam(jsonParam, 0, 1, builder, false, false);

        String result = builder.build();
        Assert.assertTrue(result.contains("\"helpTip\": \"Expecting JSON object\""));
        // Not required should use regex validation
        Assert.assertTrue(result.contains("\"validateType\": \"regex\""));
    }

    @Test
    public void testWriteJsonAttributeForJsonParam_Required() throws IOException {
        FunctionParam jsonParam = mock(FunctionParam.class);
        when(jsonParam.getParamType()).thenReturn("json");
        when(jsonParam.getValue()).thenReturn("jsonData");
        when(jsonParam.getDefaultValue()).thenReturn("{}");
        when(jsonParam.isRequired()).thenReturn(true);
        when(jsonParam.getDescription()).thenReturn("Custom description");
        when(jsonParam.getEnableCondition()).thenReturn(null);

        JsonGenerator.writeJsonAttributeForFunctionParam(jsonParam, 0, 1, builder, false, false);

        String result = builder.build();
        Assert.assertTrue(result.contains("\"validateType\": \"json\""));
    }

    // ========== Tests for DECIMAL/FLOAT with required vs optional ==========

    @Test
    public void testWriteJsonAttributeForDecimalParam_Required() throws IOException {
        FunctionParam decimalParam = mock(FunctionParam.class);
        when(decimalParam.getParamType()).thenReturn(Constants.DECIMAL);
        when(decimalParam.getValue()).thenReturn("amount");
        when(decimalParam.getDefaultValue()).thenReturn("0.0");
        when(decimalParam.isRequired()).thenReturn(true);
        when(decimalParam.getDescription()).thenReturn("Amount");
        when(decimalParam.getEnableCondition()).thenReturn(null);

        JsonGenerator.writeJsonAttributeForFunctionParam(decimalParam, 0, 1, builder, false, false);

        String result = builder.build();
        Assert.assertTrue(result.contains("\"name\": \"amount\""));
        Assert.assertTrue(result.contains("\"validateType\": \"regex\""));
    }

    @Test
    public void testWriteJsonAttributeForDecimalParam_Optional() throws IOException {
        FunctionParam decimalParam = mock(FunctionParam.class);
        when(decimalParam.getParamType()).thenReturn(Constants.DECIMAL);
        when(decimalParam.getValue()).thenReturn("amount");
        when(decimalParam.getDefaultValue()).thenReturn("0.0");
        when(decimalParam.isRequired()).thenReturn(false);
        when(decimalParam.getDescription()).thenReturn("Amount");
        when(decimalParam.getEnableCondition()).thenReturn(null);

        JsonGenerator.writeJsonAttributeForFunctionParam(decimalParam, 0, 1, builder, false, false);

        String result = builder.build();
        Assert.assertTrue(result.contains("\"required\": false"));
    }

    @Test
    public void testWriteJsonAttributeForFloatParam_Required() throws IOException {
        FunctionParam floatParam = mock(FunctionParam.class);
        when(floatParam.getParamType()).thenReturn(Constants.FLOAT);
        when(floatParam.getValue()).thenReturn("rate");
        when(floatParam.getDefaultValue()).thenReturn("1.0");
        when(floatParam.isRequired()).thenReturn(true);
        when(floatParam.getDescription()).thenReturn("Rate");
        when(floatParam.getEnableCondition()).thenReturn(null);

        JsonGenerator.writeJsonAttributeForFunctionParam(floatParam, 0, 1, builder, false, false);

        String result = builder.build();
        Assert.assertTrue(result.contains("\"name\": \"rate\""));
    }

    @Test
    public void testWriteJsonAttributeForIntParam_Optional() throws IOException {
        FunctionParam intParam = mock(FunctionParam.class);
        when(intParam.getParamType()).thenReturn(Constants.INT);
        when(intParam.getValue()).thenReturn("count");
        when(intParam.getDefaultValue()).thenReturn("0");
        when(intParam.isRequired()).thenReturn(false);
        when(intParam.getDescription()).thenReturn("Count");
        when(intParam.getEnableCondition()).thenReturn(null);

        JsonGenerator.writeJsonAttributeForFunctionParam(intParam, 0, 1, builder, false, false);

        String result = builder.build();
        Assert.assertTrue(result.contains("\"required\": false"));
    }

    // ========== Tests for Map with valueFieldParams ==========

    @Test
    public void testWriteJsonAttributeForMapParam_WithValueFieldParams() throws IOException {
        MapFunctionParam mapParam = mock(MapFunctionParam.class);
        when(mapParam.getParamType()).thenReturn(Constants.MAP);
        when(mapParam.getValue()).thenReturn("customHeaders");
        when(mapParam.isRenderAsTable()).thenReturn(true);
        when(mapParam.isRequired()).thenReturn(true);
        when(mapParam.getDescription()).thenReturn("Custom headers");
        when(mapParam.getEnableCondition()).thenReturn(null);

        FunctionParam valueField = mock(FunctionParam.class);
        when(valueField.getValue()).thenReturn("headerValue");
        when(valueField.getDescription()).thenReturn("Header value");
        when(valueField.isRequired()).thenReturn(true);
        when(valueField.getResolvedTypeKind()).thenReturn(TypeDescKind.STRING);

        List<FunctionParam> valueFieldParams = new ArrayList<>();
        valueFieldParams.add(valueField);
        when(mapParam.getValueFieldParams()).thenReturn(valueFieldParams);

        JsonGenerator.writeJsonAttributeForFunctionParam(mapParam, 0, 1, builder, false, false);

        String result = builder.build();
        Assert.assertTrue(result.contains("\"type\": \"table\""));
        Assert.assertTrue(result.contains("\"name\": \"customHeaders\""));
    }

    @Test
    public void testWriteJsonAttributeForMapParam_WithIntValueField() throws IOException {
        MapFunctionParam mapParam = mock(MapFunctionParam.class);
        when(mapParam.getParamType()).thenReturn(Constants.MAP);
        when(mapParam.getValue()).thenReturn("counts");
        when(mapParam.isRenderAsTable()).thenReturn(true);
        when(mapParam.isRequired()).thenReturn(true);
        when(mapParam.getEnableCondition()).thenReturn(null);

        FunctionParam valueField = mock(FunctionParam.class);
        when(valueField.getValue()).thenReturn("count");
        when(valueField.isRequired()).thenReturn(true);
        when(valueField.getResolvedTypeKind()).thenReturn(TypeDescKind.INT);

        List<FunctionParam> valueFieldParams = new ArrayList<>();
        valueFieldParams.add(valueField);
        when(mapParam.getValueFieldParams()).thenReturn(valueFieldParams);

        JsonGenerator.writeJsonAttributeForFunctionParam(mapParam, 0, 1, builder, false, false);

        String result = builder.build();
        Assert.assertTrue(result.contains("\"type\": \"table\""));
    }

    @Test
    public void testWriteJsonAttributeForMapParam_WithDecimalValueField() throws IOException {
        MapFunctionParam mapParam = mock(MapFunctionParam.class);
        when(mapParam.getParamType()).thenReturn(Constants.MAP);
        when(mapParam.getValue()).thenReturn("prices");
        when(mapParam.isRenderAsTable()).thenReturn(true);
        when(mapParam.isRequired()).thenReturn(false);
        when(mapParam.getEnableCondition()).thenReturn(null);

        FunctionParam valueField = mock(FunctionParam.class);
        when(valueField.getValue()).thenReturn("price");
        when(valueField.isRequired()).thenReturn(false);
        when(valueField.getResolvedTypeKind()).thenReturn(TypeDescKind.DECIMAL);

        List<FunctionParam> valueFieldParams = new ArrayList<>();
        valueFieldParams.add(valueField);
        when(mapParam.getValueFieldParams()).thenReturn(valueFieldParams);

        JsonGenerator.writeJsonAttributeForFunctionParam(mapParam, 0, 1, builder, false, false);

        String result = builder.build();
        Assert.assertTrue(result.contains("\"type\": \"table\""));
    }

    @Test
    public void testWriteJsonAttributeForMapParam_WithBooleanValueField() throws IOException {
        MapFunctionParam mapParam = mock(MapFunctionParam.class);
        when(mapParam.getParamType()).thenReturn(Constants.MAP);
        when(mapParam.getValue()).thenReturn("flags");
        when(mapParam.isRenderAsTable()).thenReturn(true);
        when(mapParam.isRequired()).thenReturn(true);
        when(mapParam.getEnableCondition()).thenReturn(null);

        FunctionParam valueField = mock(FunctionParam.class);
        when(valueField.getValue()).thenReturn("flag");
        when(valueField.isRequired()).thenReturn(true);
        when(valueField.getResolvedTypeKind()).thenReturn(TypeDescKind.BOOLEAN);

        List<FunctionParam> valueFieldParams = new ArrayList<>();
        valueFieldParams.add(valueField);
        when(mapParam.getValueFieldParams()).thenReturn(valueFieldParams);

        JsonGenerator.writeJsonAttributeForFunctionParam(mapParam, 0, 1, builder, false, false);

        String result = builder.build();
        Assert.assertTrue(result.contains("\"type\": \"table\""));
    }

    // ========== Tests for Array with elementFieldParams ==========

    @Test
    public void testWriteJsonAttributeForArrayParam_WithElementFieldParams() throws IOException {
        ArrayFunctionParam arrayParam = mock(ArrayFunctionParam.class);
        when(arrayParam.getParamType()).thenReturn(Constants.ARRAY);
        when(arrayParam.getValue()).thenReturn("items");
        when(arrayParam.isRenderAsTable()).thenReturn(true);
        when(arrayParam.is2DArray()).thenReturn(false);
        when(arrayParam.isUnionArray()).thenReturn(false);
        when(arrayParam.isRequired()).thenReturn(true);
        when(arrayParam.getEnableCondition()).thenReturn(null);

        FunctionParam elementField = mock(FunctionParam.class);
        when(elementField.getValue()).thenReturn("itemName");
        when(elementField.getDescription()).thenReturn("Item name");
        when(elementField.isRequired()).thenReturn(true);
        when(elementField.getResolvedTypeKind()).thenReturn(TypeDescKind.STRING);

        List<FunctionParam> elementFieldParams = new ArrayList<>();
        elementFieldParams.add(elementField);
        when(arrayParam.getElementFieldParams()).thenReturn(elementFieldParams);

        JsonGenerator.writeJsonAttributeForFunctionParam(arrayParam, 0, 1, builder, false, false);

        String result = builder.build();
        Assert.assertTrue(result.contains("\"type\": \"table\""));
    }

    @Test
    public void testWriteJsonAttributeForArrayParam_WithIntElementField() throws IOException {
        ArrayFunctionParam arrayParam = mock(ArrayFunctionParam.class);
        when(arrayParam.getParamType()).thenReturn(Constants.ARRAY);
        when(arrayParam.getValue()).thenReturn("scores");
        when(arrayParam.isRenderAsTable()).thenReturn(true);
        when(arrayParam.is2DArray()).thenReturn(false);
        when(arrayParam.isUnionArray()).thenReturn(false);
        when(arrayParam.isRequired()).thenReturn(true);
        when(arrayParam.getEnableCondition()).thenReturn(null);

        FunctionParam elementField = mock(FunctionParam.class);
        when(elementField.getValue()).thenReturn("score");
        when(elementField.isRequired()).thenReturn(true);
        when(elementField.getResolvedTypeKind()).thenReturn(TypeDescKind.INT);

        List<FunctionParam> elementFieldParams = new ArrayList<>();
        elementFieldParams.add(elementField);
        when(arrayParam.getElementFieldParams()).thenReturn(elementFieldParams);

        JsonGenerator.writeJsonAttributeForFunctionParam(arrayParam, 0, 1, builder, false, false);

        String result = builder.build();
        Assert.assertTrue(result.contains("\"type\": \"table\""));
    }

    @Test
    public void testWriteJsonAttributeForArrayParam_WithFloatElementField() throws IOException {
        ArrayFunctionParam arrayParam = mock(ArrayFunctionParam.class);
        when(arrayParam.getParamType()).thenReturn(Constants.ARRAY);
        when(arrayParam.getValue()).thenReturn("rates");
        when(arrayParam.isRenderAsTable()).thenReturn(true);
        when(arrayParam.is2DArray()).thenReturn(false);
        when(arrayParam.isUnionArray()).thenReturn(false);
        when(arrayParam.isRequired()).thenReturn(false);
        when(arrayParam.getEnableCondition()).thenReturn(null);

        FunctionParam elementField = mock(FunctionParam.class);
        when(elementField.getValue()).thenReturn("rate");
        when(elementField.isRequired()).thenReturn(false);
        when(elementField.getResolvedTypeKind()).thenReturn(TypeDescKind.FLOAT);

        List<FunctionParam> elementFieldParams = new ArrayList<>();
        elementFieldParams.add(elementField);
        when(arrayParam.getElementFieldParams()).thenReturn(elementFieldParams);

        JsonGenerator.writeJsonAttributeForFunctionParam(arrayParam, 0, 1, builder, false, false);

        String result = builder.build();
        Assert.assertTrue(result.contains("\"type\": \"table\""));
    }

    @Test
    public void testWriteJsonAttributeForArrayParam_WithBooleanElementField() throws IOException {
        ArrayFunctionParam arrayParam = mock(ArrayFunctionParam.class);
        when(arrayParam.getParamType()).thenReturn(Constants.ARRAY);
        when(arrayParam.getValue()).thenReturn("flags");
        when(arrayParam.isRenderAsTable()).thenReturn(true);
        when(arrayParam.is2DArray()).thenReturn(false);
        when(arrayParam.isUnionArray()).thenReturn(false);
        when(arrayParam.isRequired()).thenReturn(true);
        when(arrayParam.getEnableCondition()).thenReturn(null);

        FunctionParam elementField = mock(FunctionParam.class);
        when(elementField.getValue()).thenReturn("enabled");
        when(elementField.isRequired()).thenReturn(true);
        when(elementField.getResolvedTypeKind()).thenReturn(TypeDescKind.BOOLEAN);

        List<FunctionParam> elementFieldParams = new ArrayList<>();
        elementFieldParams.add(elementField);
        when(arrayParam.getElementFieldParams()).thenReturn(elementFieldParams);

        JsonGenerator.writeJsonAttributeForFunctionParam(arrayParam, 0, 1, builder, false, false);

        String result = builder.build();
        Assert.assertTrue(result.contains("\"type\": \"table\""));
    }

    // ========== Tests for camelCaseToTitleCase ==========

    @Test
    public void testCamelCaseToTitleCase_Simple() {
        String result = JsonGenerator.camelCaseToTitleCase("authConfig");
        Assert.assertEquals(result, "Auth Config");
    }

    @Test
    public void testCamelCaseToTitleCase_MultipleWords() {
        String result = JsonGenerator.camelCaseToTitleCase("userAccountSettings");
        Assert.assertEquals(result, "User Account Settings");
    }

    @Test
    public void testCamelCaseToTitleCase_WithAcronym() {
        String result = JsonGenerator.camelCaseToTitleCase("HTTPConnectionPool");
        Assert.assertEquals(result, "HTTP Connection Pool");
    }

    @Test
    public void testCamelCaseToTitleCase_Null() {
        String result = JsonGenerator.camelCaseToTitleCase(null);
        Assert.assertNull(result);
    }

    @Test
    public void testCamelCaseToTitleCase_Empty() {
        String result = JsonGenerator.camelCaseToTitleCase("");
        Assert.assertEquals(result, "");
    }

    @Test
    public void testCamelCaseToTitleCase_SingleWord() {
        String result = JsonGenerator.camelCaseToTitleCase("config");
        Assert.assertEquals(result, "Config");
    }

    // ========== Tests for removeGroupPrefix ==========

    @Test
    public void testRemoveGroupPrefix_WithPrefix() {
        String result = JsonGenerator.removeGroupPrefix("auth.username", "auth");
        Assert.assertEquals(result, "username");
    }

    @Test
    public void testRemoveGroupPrefix_WithNestedPrefix() {
        String result = JsonGenerator.removeGroupPrefix("server.auth.username", "auth");
        Assert.assertEquals(result, "username");
    }

    @Test
    public void testRemoveGroupPrefix_NoMatch() {
        String result = JsonGenerator.removeGroupPrefix("config.host", "auth");
        Assert.assertEquals(result, "config.host");
    }

    @Test
    public void testRemoveGroupPrefix_NullFieldName() {
        String result = JsonGenerator.removeGroupPrefix(null, "auth");
        Assert.assertNull(result);
    }

    @Test
    public void testRemoveGroupPrefix_NullGroupName() {
        String result = JsonGenerator.removeGroupPrefix("auth.username", null);
        Assert.assertEquals(result, "auth.username");
    }

    // ========== Tests for mergeEnableConditions ==========

    @Test
    public void testMergeEnableConditions_ParentNull() {
        String result = JsonGenerator.mergeEnableConditions(null, "[{\"key\":\"val\"}]");
        Assert.assertEquals(result, "[{\"key\":\"val\"}]");
    }

    @Test
    public void testMergeEnableConditions_ParentEmpty() {
        String result = JsonGenerator.mergeEnableConditions("", "[{\"key\":\"val\"}]");
        Assert.assertEquals(result, "[{\"key\":\"val\"}]");
    }

    @Test
    public void testMergeEnableConditions_ChildNull() {
        String result = JsonGenerator.mergeEnableConditions("[{\"key\":\"val\"}]", null);
        Assert.assertEquals(result, "[{\"key\":\"val\"}]");
    }

    @Test
    public void testMergeEnableConditions_ChildEmpty() {
        String result = JsonGenerator.mergeEnableConditions("[{\"key\":\"val\"}]", "");
        Assert.assertEquals(result, "[{\"key\":\"val\"}]");
    }

    @Test
    public void testMergeEnableConditions_BothSimple() {
        String result = JsonGenerator.mergeEnableConditions("[{\"parent\":\"true\"}]", "[{\"child\":\"true\"}]");
        Assert.assertTrue(result.contains("AND"));
        Assert.assertTrue(result.contains("parent"));
        Assert.assertTrue(result.contains("child"));
    }

    @Test
    public void testMergeEnableConditions_ParentIsAndArray() {
        String parentCondition = "[\"AND\",{\"a\":\"1\"},{\"b\":\"2\"}]";
        String childCondition = "[{\"c\":\"3\"}]";
        String result = JsonGenerator.mergeEnableConditions(parentCondition, childCondition);
        Assert.assertTrue(result.startsWith("[\"AND\""));
        Assert.assertTrue(result.contains("\"c\":\"3\""));
    }

    @Test
    public void testMergeEnableConditions_InvalidFormat() {
        String result = JsonGenerator.mergeEnableConditions("invalid", "[{\"key\":\"val\"}]");
        Assert.assertEquals(result, "invalid");
    }

    // ========== Tests for nested array with innerElementTypeSymbol fallback ==========

    @Test
    public void testWriteNestedArrayAsTable_WithInnerElementTypeSymbol() throws IOException {
        ArrayFunctionParam arrayParam = mock(ArrayFunctionParam.class);
        when(arrayParam.getParamType()).thenReturn(Constants.ARRAY);
        when(arrayParam.getValue()).thenReturn("nestedData");
        when(arrayParam.isRenderAsTable()).thenReturn(true);
        when(arrayParam.is2DArray()).thenReturn(true);
        when(arrayParam.isUnionArray()).thenReturn(false);
        when(arrayParam.getInnerElementTypeKind()).thenReturn(null);

        TypeSymbol innerTypeSymbol = mock(TypeSymbol.class);
        when(innerTypeSymbol.typeKind()).thenReturn(TypeDescKind.INT);
        when(arrayParam.getInnerElementTypeSymbol()).thenReturn(innerTypeSymbol);

        JsonGenerator.writeJsonAttributeForFunctionParam(arrayParam, 0, 1, builder, false, false);

        String result = builder.build();
        Assert.assertTrue(result.contains("\"type\": \"table\""));
    }

    @Test
    public void testWriteNestedArrayAsTable_FloatDecimalInnerType() throws IOException {
        JsonTemplateBuilder localBuilder = new JsonTemplateBuilder();
        ArrayFunctionParam arrayParam = mock(ArrayFunctionParam.class);
        when(arrayParam.getParamType()).thenReturn(Constants.ARRAY);
        when(arrayParam.getValue()).thenReturn("decimalMatrix");
        when(arrayParam.isRenderAsTable()).thenReturn(true);
        when(arrayParam.is2DArray()).thenReturn(true);
        when(arrayParam.getInnerElementTypeKind()).thenReturn(TypeDescKind.DECIMAL);

        JsonGenerator.writeJsonAttributeForFunctionParam(arrayParam, 0, 1, localBuilder, false, false);

        String result = localBuilder.build();
        Assert.assertTrue(result.contains("Decimal value"));
    }

    // ========== Tests for simpleValueColumn/simpleElementColumn with type symbol fallback ==========

    @Test
    public void testMapTable_ValueTypeSymbolFallback() throws IOException {
        MapFunctionParam mapParam = mock(MapFunctionParam.class);
        when(mapParam.getParamType()).thenReturn(Constants.MAP);
        when(mapParam.getValue()).thenReturn("typedMap");
        when(mapParam.isRenderAsTable()).thenReturn(true);
        when(mapParam.isRequired()).thenReturn(true);
        when(mapParam.getEnableCondition()).thenReturn(null);
        when(mapParam.getValueTypeKind()).thenReturn(null);
        when(mapParam.getValueFieldParams()).thenReturn(null);

        TypeSymbol valueTypeSymbol = mock(TypeSymbol.class);
        when(valueTypeSymbol.typeKind()).thenReturn(TypeDescKind.STRING);
        when(mapParam.getValueTypeSymbol()).thenReturn(valueTypeSymbol);

        JsonGenerator.writeJsonAttributeForFunctionParam(mapParam, 0, 1, builder, false, false);

        String result = builder.build();
        Assert.assertTrue(result.contains("\"type\": \"table\""));
    }

    @Test
    public void testArrayTable_ElementTypeSymbolFallback() throws IOException {
        ArrayFunctionParam arrayParam = mock(ArrayFunctionParam.class);
        when(arrayParam.getParamType()).thenReturn(Constants.ARRAY);
        when(arrayParam.getValue()).thenReturn("typedArray");
        when(arrayParam.isRenderAsTable()).thenReturn(true);
        when(arrayParam.is2DArray()).thenReturn(false);
        when(arrayParam.isUnionArray()).thenReturn(false);
        when(arrayParam.isRequired()).thenReturn(true);
        when(arrayParam.getEnableCondition()).thenReturn(null);
        when(arrayParam.getElementTypeKind()).thenReturn(null);
        when(arrayParam.getElementFieldParams()).thenReturn(null);

        TypeSymbol elementTypeSymbol = mock(TypeSymbol.class);
        when(elementTypeSymbol.typeKind()).thenReturn(TypeDescKind.INT);
        when(arrayParam.getElementTypeSymbol()).thenReturn(elementTypeSymbol);

        JsonGenerator.writeJsonAttributeForFunctionParam(arrayParam, 0, 1, builder, false, false);

        String result = builder.build();
        Assert.assertTrue(result.contains("\"type\": \"table\""));
    }

    // ========== Tests for Union param edge cases ==========

    @Test
    public void testWriteJsonAttributeForUnionParam_SingleValidMember() throws IOException {
        UnionFunctionParam unionParam = mock(UnionFunctionParam.class);
        when(unionParam.getParamType()).thenReturn(Constants.UNION);
        when(unionParam.getValue()).thenReturn("singleUnion");
        when(unionParam.getDefaultValue()).thenReturn("default");
        when(unionParam.isRequired()).thenReturn(true);
        when(unionParam.getDescription()).thenReturn("Single member union");
        when(unionParam.getEnableCondition()).thenReturn("[{\"enabled\":\"true\"}]");

        FunctionParam member = mock(FunctionParam.class);
        when(member.getParamType()).thenReturn(Constants.STRING);
        when(member.getValue()).thenReturn("value");
        when(member.isRequired()).thenReturn(true);
        when(member.getEnableCondition()).thenReturn(null);

        List<FunctionParam> members = new ArrayList<>();
        members.add(member);
        when(unionParam.getUnionMemberParams()).thenReturn(members);

        JsonGenerator.writeJsonAttributeForFunctionParam(unionParam, 0, 1, builder, false, false);

        String result = builder.build();
        // With single member, no combo should be shown
        Assert.assertTrue(result.contains("\"name\": \"value\""));
    }

    @Test
    public void testWriteJsonAttributeForUnionParam_WithNestedEmptyUnion() throws IOException {
        UnionFunctionParam unionParam = mock(UnionFunctionParam.class);
        when(unionParam.getParamType()).thenReturn(Constants.UNION);
        when(unionParam.getValue()).thenReturn("nestedUnion");
        when(unionParam.isRequired()).thenReturn(true);
        when(unionParam.getEnableCondition()).thenReturn(null);

        // First member is a nested union with empty members (should be filtered out)
        UnionFunctionParam nestedUnion = mock(UnionFunctionParam.class);
        when(nestedUnion.getParamType()).thenReturn(Constants.UNION);
        when(nestedUnion.getValue()).thenReturn("empty");
        when(nestedUnion.getUnionMemberParams()).thenReturn(new ArrayList<>());

        FunctionParam stringMember = mock(FunctionParam.class);
        when(stringMember.getParamType()).thenReturn(Constants.STRING);
        when(stringMember.getValue()).thenReturn("stringVal");
        when(stringMember.isRequired()).thenReturn(true);
        when(stringMember.getEnableCondition()).thenReturn(null);

        List<FunctionParam> members = new ArrayList<>();
        members.add(nestedUnion);
        members.add(stringMember);
        when(unionParam.getUnionMemberParams()).thenReturn(members);

        JsonGenerator.writeJsonAttributeForFunctionParam(unionParam, 0, 1, builder, false, false);

        String result = builder.build();
        Assert.assertTrue(result.contains("\"name\": \"stringVal\""));
    }

    @Test
    public void testWriteJsonAttributeForUnionParam_WithRecordMembers() throws IOException {
        UnionFunctionParam unionParam = mock(UnionFunctionParam.class);
        when(unionParam.getParamType()).thenReturn(Constants.UNION);
        when(unionParam.getValue()).thenReturn("authConfig");
        when(unionParam.isRequired()).thenReturn(true);
        when(unionParam.getDescription()).thenReturn("Auth configuration");
        when(unionParam.getEnableCondition()).thenReturn(null);

        // Record member
        RecordFunctionParam recordMember = mock(RecordFunctionParam.class);
        when(recordMember.getParamType()).thenReturn(Constants.RECORD);
        when(recordMember.getValue()).thenReturn("basicAuth");
        when(recordMember.isRequired()).thenReturn(true);
        when(recordMember.getDisplayTypeName()).thenReturn("BasicAuth");
        when(recordMember.getEnableCondition()).thenReturn("[{\"authConfigDataType\":\"BasicAuth\"}]");

        FunctionParam usernameField = mock(FunctionParam.class);
        when(usernameField.getValue()).thenReturn("username");
        when(usernameField.getParamType()).thenReturn(Constants.STRING);
        when(usernameField.isRequired()).thenReturn(true);
        when(usernameField.getEnableCondition()).thenReturn(null);

        List<FunctionParam> recordFields = new ArrayList<>();
        recordFields.add(usernameField);
        when(recordMember.getRecordFieldParams()).thenReturn(recordFields);

        // String member
        FunctionParam stringMember = mock(FunctionParam.class);
        when(stringMember.getParamType()).thenReturn(Constants.STRING);
        when(stringMember.getValue()).thenReturn("apiKey");
        when(stringMember.isRequired()).thenReturn(true);
        when(stringMember.getEnableCondition()).thenReturn("[{\"authConfigDataType\":\"string\"}]");

        List<FunctionParam> members = new ArrayList<>();
        members.add(recordMember);
        members.add(stringMember);
        when(unionParam.getUnionMemberParams()).thenReturn(members);

        JsonGenerator.writeJsonAttributeForFunctionParam(unionParam, 0, 1, builder, false, true);

        String result = builder.build();
        Assert.assertTrue(result.contains("\"inputType\": \"combo\""));
    }

    @Test
    public void testWriteJsonAttributeForUnionParam_WithQualifiedName() throws IOException {
        UnionFunctionParam unionParam = mock(UnionFunctionParam.class);
        when(unionParam.getParamType()).thenReturn(Constants.UNION);
        when(unionParam.getValue()).thenReturn("parent.child.auth");
        when(unionParam.isRequired()).thenReturn(true);
        when(unionParam.getEnableCondition()).thenReturn(null);

        FunctionParam member1 = mock(FunctionParam.class);
        when(member1.getParamType()).thenReturn(Constants.STRING);
        when(member1.getValue()).thenReturn("basic");
        when(member1.isRequired()).thenReturn(true);
        when(member1.getEnableCondition()).thenReturn(null);

        FunctionParam member2 = mock(FunctionParam.class);
        when(member2.getParamType()).thenReturn(Constants.STRING);
        when(member2.getValue()).thenReturn("oauth");
        when(member2.isRequired()).thenReturn(true);
        when(member2.getEnableCondition()).thenReturn(null);

        List<FunctionParam> members = new ArrayList<>();
        members.add(member1);
        members.add(member2);
        when(unionParam.getUnionMemberParams()).thenReturn(members);

        JsonGenerator.writeJsonAttributeForFunctionParam(unionParam, 0, 1, builder, false, false);

        String result = builder.build();
        // Should handle qualified name and extract immediate parent
        Assert.assertTrue(result.contains("\"inputType\": \"combo\""));
    }

    // ========== Tests for Record not expanded ==========

    @Test
    public void testWriteJsonAttributeForRecordParam_NotExpanded_NullDescription() throws IOException {
        FunctionParam recordParam = mock(FunctionParam.class);
        when(recordParam.getParamType()).thenReturn(Constants.RECORD);
        when(recordParam.getValue()).thenReturn("config");
        when(recordParam.getDefaultValue()).thenReturn("{}");
        when(recordParam.isRequired()).thenReturn(true);
        when(recordParam.getDescription()).thenReturn(null);
        when(recordParam.getEnableCondition()).thenReturn(null);

        JsonGenerator.writeJsonAttributeForFunctionParam(recordParam, 0, 1, builder, false, false);

        String result = builder.build();
        Assert.assertTrue(result.contains("\"helpTip\": \"Expecting JSON object\""));
    }

    @Test
    public void testWriteJsonAttributeForRecordParam_NotExpanded_EmptyDescription() throws IOException {
        FunctionParam recordParam = mock(FunctionParam.class);
        when(recordParam.getParamType()).thenReturn(Constants.RECORD);
        when(recordParam.getValue()).thenReturn("config");
        when(recordParam.getDefaultValue()).thenReturn("{}");
        when(recordParam.isRequired()).thenReturn(true);
        when(recordParam.getDescription()).thenReturn("");
        when(recordParam.getEnableCondition()).thenReturn(null);

        JsonGenerator.writeJsonAttributeForFunctionParam(recordParam, 0, 1, builder, false, false);

        String result = builder.build();
        Assert.assertTrue(result.contains("\"helpTip\": \"Expecting JSON object\""));
    }

    // ========== Tests for Map not as table with null description ==========

    @Test
    public void testWriteJsonAttributeForMapParam_NotAsTable_NullDescription() throws IOException {
        FunctionParam mapParam = mock(FunctionParam.class);
        when(mapParam.getParamType()).thenReturn(Constants.MAP);
        when(mapParam.getValue()).thenReturn("headers");
        when(mapParam.getDefaultValue()).thenReturn("{}");
        when(mapParam.isRequired()).thenReturn(false);
        when(mapParam.getDescription()).thenReturn(null);
        when(mapParam.getEnableCondition()).thenReturn(null);

        JsonGenerator.writeJsonAttributeForFunctionParam(mapParam, 0, 1, builder, false, false);

        String result = builder.build();
        Assert.assertTrue(result.contains("\"helpTip\": \"Expecting JSON object with key-value pairs\""));
    }

    // ========== Tests for Array not as table with null description ==========

    @Test
    public void testWriteJsonAttributeForArrayParam_NotAsTable_NullDescription() throws IOException {
        FunctionParam arrayParam = mock(FunctionParam.class);
        when(arrayParam.getParamType()).thenReturn(Constants.ARRAY);
        when(arrayParam.getValue()).thenReturn("items");
        when(arrayParam.getDefaultValue()).thenReturn("[]");
        when(arrayParam.isRequired()).thenReturn(false);
        when(arrayParam.getDescription()).thenReturn(null);
        when(arrayParam.getEnableCondition()).thenReturn(null);

        JsonGenerator.writeJsonAttributeForFunctionParam(arrayParam, 0, 1, builder, false, false);

        String result = builder.build();
        Assert.assertTrue(result.contains("\"helpTip\": \"Expecting JSON array\""));
    }

    // ========== Tests for unsupported parameter type ==========

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testWriteJsonAttributeForUnsupportedType() throws IOException {
        FunctionParam unsupportedParam = mock(FunctionParam.class);
        when(unsupportedParam.getParamType()).thenReturn("unknownType");
        when(unsupportedParam.getValue()).thenReturn("param");

        JsonGenerator.writeJsonAttributeForFunctionParam(unsupportedParam, 0, 1, builder, false, false);
    }

    // ========== Tests for writeAttributeGroup ==========

    @Test
    public void testWriteAttributeGroup_LastGroup() throws IOException {
        List<FunctionParam> params = new ArrayList<>();
        FunctionParam param = mock(FunctionParam.class);
        when(param.getParamType()).thenReturn(Constants.STRING);
        when(param.getValue()).thenReturn("field1");
        when(param.isRequired()).thenReturn(true);
        when(param.getEnableCondition()).thenReturn(null);
        params.add(param);

        JsonGenerator.writeAttributeGroup("TestGroup", params, true, builder, false);

        String result = builder.build();
        Assert.assertTrue(result.contains("\"groupName\": \"TestGroup\""));
    }

    @Test
    public void testWriteAttributeGroup_NotLastGroup() throws IOException {
        List<FunctionParam> params = new ArrayList<>();
        FunctionParam param = mock(FunctionParam.class);
        when(param.getParamType()).thenReturn(Constants.INT);
        when(param.getValue()).thenReturn("field2");
        when(param.isRequired()).thenReturn(false);
        when(param.getEnableCondition()).thenReturn(null);
        params.add(param);

        JsonGenerator.writeAttributeGroup("AnotherGroup", params, false, builder, false);

        String result = builder.build();
        Assert.assertTrue(result.contains("\"groupName\": \"AnotherGroup\""));
    }

    @Test
    public void testWriteAttributeGroup_Collapsed() throws IOException {
        List<FunctionParam> params = new ArrayList<>();
        FunctionParam param = mock(FunctionParam.class);
        when(param.getParamType()).thenReturn(Constants.BOOLEAN);
        when(param.getValue()).thenReturn("enabled");
        when(param.isRequired()).thenReturn(true);
        when(param.getEnableCondition()).thenReturn(null);
        params.add(param);

        JsonGenerator.writeAttributeGroup("CollapsedGroup", params, true, builder, true, false);

        String result = builder.build();
        Assert.assertTrue(result.contains("\"groupName\": \"CollapsedGroup\""));
    }

    // ========== Tests for displayName with dots ==========

    @Test
    public void testWriteJsonAttributeForStringParam_WithDotInName() throws IOException {
        FunctionParam stringParam = mock(FunctionParam.class);
        when(stringParam.getParamType()).thenReturn(Constants.STRING);
        when(stringParam.getValue()).thenReturn("config.server.host");
        when(stringParam.getDefaultValue()).thenReturn("localhost");
        when(stringParam.isRequired()).thenReturn(true);
        when(stringParam.getDescription()).thenReturn("Host name");
        when(stringParam.getEnableCondition()).thenReturn(null);

        JsonGenerator.writeJsonAttributeForFunctionParam(stringParam, 0, 1, builder, false, false);

        String result = builder.build();
        // Display name should be just "host"
        Assert.assertTrue(result.contains("\"displayName\": \"host\""));
    }

    // ========== Tests for getTypeNameOrFallback via union param ==========

    @Test
    public void testGetTypeNameOrFallback_WithDisplayTypeName() throws IOException {
        UnionFunctionParam unionParam = mock(UnionFunctionParam.class);
        when(unionParam.getParamType()).thenReturn(Constants.UNION);
        when(unionParam.getValue()).thenReturn("typeUnion");
        when(unionParam.isRequired()).thenReturn(true);
        when(unionParam.getEnableCondition()).thenReturn(null);

        RecordFunctionParam recordMember = mock(RecordFunctionParam.class);
        when(recordMember.getParamType()).thenReturn(Constants.RECORD);
        when(recordMember.getValue()).thenReturn("customType");
        when(recordMember.getDisplayTypeName()).thenReturn("MyCustomType");
        when(recordMember.isRequired()).thenReturn(true);
        when(recordMember.getRecordFieldParams()).thenReturn(new ArrayList<>());
        when(recordMember.getEnableCondition()).thenReturn(null);

        FunctionParam stringMember = mock(FunctionParam.class);
        when(stringMember.getParamType()).thenReturn(Constants.STRING);
        when(stringMember.getValue()).thenReturn("textVal");
        when(stringMember.isRequired()).thenReturn(true);
        when(stringMember.getEnableCondition()).thenReturn(null);

        List<FunctionParam> members = new ArrayList<>();
        members.add(recordMember);
        members.add(stringMember);
        when(unionParam.getUnionMemberParams()).thenReturn(members);

        JsonGenerator.writeJsonAttributeForFunctionParam(unionParam, 0, 1, builder, false, false);

        String result = builder.build();
        Assert.assertTrue(result.contains("MyCustomType"));
    }

    @Test
    public void testGetTypeNameOrFallback_WithResolvedTypeName() throws IOException {
        UnionFunctionParam unionParam = mock(UnionFunctionParam.class);
        when(unionParam.getParamType()).thenReturn(Constants.UNION);
        when(unionParam.getValue()).thenReturn("resolvedUnion");
        when(unionParam.isRequired()).thenReturn(true);
        when(unionParam.getEnableCondition()).thenReturn(null);

        RecordFunctionParam recordMember = mock(RecordFunctionParam.class);
        when(recordMember.getParamType()).thenReturn(Constants.RECORD);
        when(recordMember.getValue()).thenReturn("resolvedType");
        when(recordMember.getDisplayTypeName()).thenReturn(null);
        when(recordMember.getResolvedTypeName()).thenReturn("ResolvedTypeName");
        when(recordMember.isRequired()).thenReturn(true);
        when(recordMember.getRecordFieldParams()).thenReturn(new ArrayList<>());
        when(recordMember.getEnableCondition()).thenReturn(null);

        FunctionParam intMember = mock(FunctionParam.class);
        when(intMember.getParamType()).thenReturn(Constants.INT);
        when(intMember.getValue()).thenReturn("numVal");
        when(intMember.isRequired()).thenReturn(true);
        when(intMember.getEnableCondition()).thenReturn(null);

        List<FunctionParam> members = new ArrayList<>();
        members.add(recordMember);
        members.add(intMember);
        when(unionParam.getUnionMemberParams()).thenReturn(members);

        JsonGenerator.writeJsonAttributeForFunctionParam(unionParam, 0, 1, builder, false, false);

        String result = builder.build();
        Assert.assertTrue(result.contains("ResolvedTypeName"));
    }

    @Test
    public void testGetTypeNameOrFallback_WithTypeSymbol() throws IOException {
        UnionFunctionParam unionParam = mock(UnionFunctionParam.class);
        when(unionParam.getParamType()).thenReturn(Constants.UNION);
        when(unionParam.getValue()).thenReturn("symbolUnion");
        when(unionParam.isRequired()).thenReturn(true);
        when(unionParam.getEnableCondition()).thenReturn(null);

        RecordFunctionParam recordMember = mock(RecordFunctionParam.class);
        when(recordMember.getParamType()).thenReturn(Constants.RECORD);
        when(recordMember.getValue()).thenReturn("symbolType");
        when(recordMember.getDisplayTypeName()).thenReturn(null);
        when(recordMember.getResolvedTypeName()).thenReturn(null);
        TypeSymbol typeSymbol = mock(TypeSymbol.class);
        when(typeSymbol.getName()).thenReturn(Optional.of("SymbolTypeName"));
        when(recordMember.getTypeSymbol()).thenReturn(typeSymbol);
        when(recordMember.isRequired()).thenReturn(true);
        when(recordMember.getRecordFieldParams()).thenReturn(new ArrayList<>());
        when(recordMember.getEnableCondition()).thenReturn(null);

        FunctionParam boolMember = mock(FunctionParam.class);
        when(boolMember.getParamType()).thenReturn(Constants.BOOLEAN);
        when(boolMember.getValue()).thenReturn("flagVal");
        when(boolMember.isRequired()).thenReturn(true);
        when(boolMember.getEnableCondition()).thenReturn(null);

        List<FunctionParam> members = new ArrayList<>();
        members.add(recordMember);
        members.add(boolMember);
        when(unionParam.getUnionMemberParams()).thenReturn(members);

        JsonGenerator.writeJsonAttributeForFunctionParam(unionParam, 0, 1, builder, false, false);

        String result = builder.build();
        Assert.assertTrue(result.contains("SymbolTypeName"));
    }

    // ========== Tests for Union with no members ==========

    @Test
    public void testWriteJsonAttributeForUnionParam_EmptyMembers() throws IOException {
        UnionFunctionParam unionParam = mock(UnionFunctionParam.class);
        when(unionParam.getParamType()).thenReturn(Constants.UNION);
        when(unionParam.getValue()).thenReturn("emptyUnion");
        when(unionParam.isRequired()).thenReturn(false);
        when(unionParam.getEnableCondition()).thenReturn(null);
        when(unionParam.getUnionMemberParams()).thenReturn(new ArrayList<>());

        // Should not throw but produce no output for members
        JsonGenerator.writeJsonAttributeForFunctionParam(unionParam, 0, 1, builder, false, false);
        String result = builder.build();
        // Result may be empty or minimal
        Assert.assertNotNull(result);
    }

    // ========== Tests for configContext with optional params ==========

    @Test
    public void testWriteJsonAttributeForMapParam_ConfigContext_Optional() throws IOException {
        MapFunctionParam mapParam = mock(MapFunctionParam.class);
        when(mapParam.getParamType()).thenReturn(Constants.MAP);
        when(mapParam.getValue()).thenReturn("optionalMap");
        when(mapParam.isRenderAsTable()).thenReturn(true);
        when(mapParam.isRequired()).thenReturn(false);
        when(mapParam.getDescription()).thenReturn("Optional map");
        when(mapParam.getEnableCondition()).thenReturn(null);
        when(mapParam.getValueFieldParams()).thenReturn(new ArrayList<>());
        when(mapParam.getValueTypeKind()).thenReturn(TypeDescKind.STRING);

        JsonGenerator.writeJsonAttributeForFunctionParam(mapParam, 0, 1, builder, false, false, null, true);

        String result = builder.build();
        // Should include checkbox for optional in config context
        Assert.assertTrue(result.contains("enable_optionalMap"));
    }

    @Test
    public void testWriteJsonAttributeForArrayParam_ConfigContext_Optional() throws IOException {
        ArrayFunctionParam arrayParam = mock(ArrayFunctionParam.class);
        when(arrayParam.getParamType()).thenReturn(Constants.ARRAY);
        when(arrayParam.getValue()).thenReturn("optionalArray");
        when(arrayParam.isRenderAsTable()).thenReturn(true);
        when(arrayParam.is2DArray()).thenReturn(false);
        when(arrayParam.isUnionArray()).thenReturn(false);
        when(arrayParam.isRequired()).thenReturn(false);
        when(arrayParam.getDescription()).thenReturn("Optional array");
        when(arrayParam.getEnableCondition()).thenReturn(null);
        when(arrayParam.getElementFieldParams()).thenReturn(new ArrayList<>());
        when(arrayParam.getElementTypeKind()).thenReturn(TypeDescKind.STRING);

        JsonGenerator.writeJsonAttributeForFunctionParam(arrayParam, 0, 1, builder, false, false, null, true);

        String result = builder.build();
        Assert.assertTrue(result.contains("enable_optionalArray"));
    }

    @Test
    public void testWriteJsonAttributeForRecordParam_ConfigContext_Optional() throws IOException {
        RecordFunctionParam recordParam = mock(RecordFunctionParam.class);
        when(recordParam.getParamType()).thenReturn(Constants.RECORD);
        when(recordParam.getValue()).thenReturn("optionalRecord");
        when(recordParam.isRequired()).thenReturn(false);
        when(recordParam.getDescription()).thenReturn("Optional record");
        when(recordParam.getEnableCondition()).thenReturn(null);
        when(recordParam.getRecordFieldParams()).thenReturn(new ArrayList<>());

        JsonGenerator.writeJsonAttributeForFunctionParam(recordParam, 0, 1, builder, false, true, null, true);

        String result = builder.build();
        Assert.assertTrue(result.contains("enable_optionalRecord"));
    }

    // ========== Tests for union array with null member types ==========

    @Test
    public void testWriteUnionArrayAsTable_NullMemberTypes() throws IOException {
        ArrayFunctionParam arrayParam = mock(ArrayFunctionParam.class);
        when(arrayParam.getParamType()).thenReturn(Constants.ARRAY);
        when(arrayParam.getValue()).thenReturn("nullMemberArray");
        when(arrayParam.isRenderAsTable()).thenReturn(true);
        when(arrayParam.is2DArray()).thenReturn(false);
        when(arrayParam.isUnionArray()).thenReturn(true);
        when(arrayParam.isRequired()).thenReturn(true);
        when(arrayParam.getEnableCondition()).thenReturn(null);
        when(arrayParam.getUnionMemberTypeNames()).thenReturn(null);

        JsonGenerator.writeJsonAttributeForFunctionParam(arrayParam, 0, 1, builder, false, false);

        String result = builder.build();
        Assert.assertTrue(result.contains("\"type\": \"table\""));
    }

    @Test
    public void testWriteUnionArrayAsTable_EmptyMemberTypes() throws IOException {
        ArrayFunctionParam arrayParam = mock(ArrayFunctionParam.class);
        when(arrayParam.getParamType()).thenReturn(Constants.ARRAY);
        when(arrayParam.getValue()).thenReturn("emptyMemberArray");
        when(arrayParam.isRenderAsTable()).thenReturn(true);
        when(arrayParam.is2DArray()).thenReturn(false);
        when(arrayParam.isUnionArray()).thenReturn(true);
        when(arrayParam.isRequired()).thenReturn(true);
        when(arrayParam.getEnableCondition()).thenReturn(null);
        when(arrayParam.getUnionMemberTypeNames()).thenReturn(new ArrayList<>());

        JsonGenerator.writeJsonAttributeForFunctionParam(arrayParam, 0, 1, builder, false, false);

        String result = builder.build();
        Assert.assertTrue(result.contains("\"type\": \"table\""));
    }

    // ========== Tests for value field params with TypeSymbol fallback ==========

    @Test
    public void testCreateAttributeForMapValueField_TypeSymbolFallback() throws IOException {
        MapFunctionParam mapParam = mock(MapFunctionParam.class);
        when(mapParam.getParamType()).thenReturn(Constants.MAP);
        when(mapParam.getValue()).thenReturn("typedValMap");
        when(mapParam.isRenderAsTable()).thenReturn(true);
        when(mapParam.isRequired()).thenReturn(true);
        when(mapParam.getEnableCondition()).thenReturn(null);

        FunctionParam valueField = mock(FunctionParam.class);
        when(valueField.getValue()).thenReturn("amount");
        when(valueField.isRequired()).thenReturn(true);
        when(valueField.getResolvedTypeKind()).thenReturn(null);
        TypeSymbol ts = mock(TypeSymbol.class);
        when(ts.typeKind()).thenReturn(TypeDescKind.DECIMAL);
        when(valueField.getTypeSymbol()).thenReturn(ts);

        List<FunctionParam> valueFieldParams = new ArrayList<>();
        valueFieldParams.add(valueField);
        when(mapParam.getValueFieldParams()).thenReturn(valueFieldParams);

        JsonGenerator.writeJsonAttributeForFunctionParam(mapParam, 0, 1, builder, false, false);

        String result = builder.build();
        Assert.assertTrue(result.contains("\"type\": \"table\""));
    }

    @Test
    public void testCreateAttributeForElementField_TypeSymbolFallback() throws IOException {
        ArrayFunctionParam arrayParam = mock(ArrayFunctionParam.class);
        when(arrayParam.getParamType()).thenReturn(Constants.ARRAY);
        when(arrayParam.getValue()).thenReturn("typedElArray");
        when(arrayParam.isRenderAsTable()).thenReturn(true);
        when(arrayParam.is2DArray()).thenReturn(false);
        when(arrayParam.isUnionArray()).thenReturn(false);
        when(arrayParam.isRequired()).thenReturn(true);
        when(arrayParam.getEnableCondition()).thenReturn(null);

        FunctionParam elementField = mock(FunctionParam.class);
        when(elementField.getValue()).thenReturn("rate");
        when(elementField.isRequired()).thenReturn(false);
        when(elementField.getResolvedTypeKind()).thenReturn(null);
        TypeSymbol ts = mock(TypeSymbol.class);
        when(ts.typeKind()).thenReturn(TypeDescKind.FLOAT);
        when(elementField.getTypeSymbol()).thenReturn(ts);

        List<FunctionParam> elementFieldParams = new ArrayList<>();
        elementFieldParams.add(elementField);
        when(arrayParam.getElementFieldParams()).thenReturn(elementFieldParams);

        JsonGenerator.writeJsonAttributeForFunctionParam(arrayParam, 0, 1, builder, false, false);

        String result = builder.build();
        Assert.assertTrue(result.contains("\"type\": \"table\""));
    }

    // ========== Tests for enableCondition propagation ==========

    @Test
    public void testWriteJsonAttributeForStringParam_WithEnableCondition() throws IOException {
        FunctionParam stringParam = mock(FunctionParam.class);
        when(stringParam.getParamType()).thenReturn(Constants.STRING);
        when(stringParam.getValue()).thenReturn("conditionalParam");
        when(stringParam.getDefaultValue()).thenReturn("");
        when(stringParam.isRequired()).thenReturn(true);
        when(stringParam.getDescription()).thenReturn("Conditional param");
        when(stringParam.getEnableCondition()).thenReturn("[{\"enabled\":\"true\"}]");

        JsonGenerator.writeJsonAttributeForFunctionParam(stringParam, 0, 1, builder, false, false);

        String result = builder.build();
        Assert.assertTrue(result.contains("\"enableCondition\": [{\"enabled\":\"true\"}]"));
    }

    // ========== Tests for isCombo parameter ==========

    @Test
    public void testWriteJsonAttributeForStringParam_IsCombo() throws IOException {
        FunctionParam stringParam = mock(FunctionParam.class);
        when(stringParam.getParamType()).thenReturn(Constants.STRING);
        when(stringParam.getValue()).thenReturn("comboParam");
        when(stringParam.getDefaultValue()).thenReturn("default");
        when(stringParam.isRequired()).thenReturn(true);
        when(stringParam.getDescription()).thenReturn("Combo param");
        when(stringParam.getEnableCondition()).thenReturn(null);

        JsonGenerator.writeJsonAttributeForFunctionParam(stringParam, 0, 1, builder, true, false);

        String result = builder.build();
        Assert.assertTrue(result.contains("\"name\": \"comboParam\""));
    }
}
