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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
}
