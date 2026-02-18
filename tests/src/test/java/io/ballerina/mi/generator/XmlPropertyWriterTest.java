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

import io.ballerina.mi.model.param.FunctionParam;
import io.ballerina.mi.model.param.RecordFunctionParam;
import io.ballerina.mi.model.param.UnionFunctionParam;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.*;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for XmlPropertyWriter class.
 * Note: Tests for methods that require PathParamType and Type from diagramutil
 * are not included as those classes are not available in the tests module.
 */
public class XmlPropertyWriterTest {

    @Test
    public void testWriteXmlParamProperties_Simple() {
        FunctionParam param = mock(FunctionParam.class);
        when(param.getValue()).thenReturn("timeout");
        when(param.getParamType()).thenReturn("decimal");

        StringBuilder result = new StringBuilder();
        int[] indexHolder = {0};
        boolean[] isFirst = {true};

        XmlPropertyWriter.writeXmlParamProperties(param, "CONN", result, indexHolder, isFirst);

        String output = result.toString();
        Assert.assertTrue(output.contains("<property name=\"CONN_param0\" value=\"timeout\"/>"));
        Assert.assertTrue(output.contains("<property name=\"CONN_paramType0\" value=\"decimal\"/>"));
        Assert.assertEquals(indexHolder[0], 1);
    }

    @Test
    public void testWriteXmlParamProperties_RecordParam() {
        RecordFunctionParam recordParam = mock(RecordFunctionParam.class);
        when(recordParam.getValue()).thenReturn("config");
        when(recordParam.getParamType()).thenReturn("record");
        when(recordParam.getRecordName()).thenReturn("ConfigRecord");

        FunctionParam field1 = mock(FunctionParam.class);
        when(field1.getValue()).thenReturn("host");
        when(field1.getParamType()).thenReturn("string");

        FunctionParam field2 = mock(FunctionParam.class);
        when(field2.getValue()).thenReturn("port");
        when(field2.getParamType()).thenReturn("int");

        List<FunctionParam> fields = Arrays.asList(field1, field2);
        when(recordParam.getRecordFieldParams()).thenReturn(fields);

        StringBuilder result = new StringBuilder();
        int[] indexHolder = {0};
        boolean[] isFirst = {true};

        XmlPropertyWriter.writeXmlParamProperties(recordParam, "CONN", result, indexHolder, isFirst);

        String output = result.toString();
        Assert.assertTrue(output.contains("<property name=\"CONN_param0\" value=\"config\"/>"));
        Assert.assertTrue(output.contains("<property name=\"CONN_paramType0\" value=\"record\"/>"));
        Assert.assertTrue(output.contains("<property name=\"CONN_param0_recordName\" value=\"ConfigRecord\"/>"));
        Assert.assertTrue(output.contains("<property name=\"CONN_config_param0\" value=\"host\"/>"));
        Assert.assertTrue(output.contains("<property name=\"CONN_config_paramType0\" value=\"string\"/>"));
        Assert.assertTrue(output.contains("<property name=\"CONN_config_param1\" value=\"port\"/>"));
        Assert.assertTrue(output.contains("<property name=\"CONN_config_paramType1\" value=\"int\"/>"));
    }

    @Test
    public void testWriteXmlParameterElements_Simple() {
        FunctionParam param = mock(FunctionParam.class);
        when(param.getValue()).thenReturn("token");
        when(param.getDescription()).thenReturn("Access Token");

        StringBuilder result = new StringBuilder();
        boolean[] isFirst = {true};
        Set<String> processed = new HashSet<>();

        XmlPropertyWriter.writeXmlParameterElements(param, result, isFirst, processed);

        String output = result.toString();
        Assert.assertTrue(output.contains("<parameter name=\"token\" description=\"Access Token\"/>"));
        Assert.assertTrue(processed.contains("token"));
    }

    @Test
    public void testWriteXmlParameterElements_Record() {
        RecordFunctionParam recordParam = mock(RecordFunctionParam.class);
        when(recordParam.getValue()).thenReturn("config");

        FunctionParam field1 = mock(FunctionParam.class);
        when(field1.getValue()).thenReturn("host");
        when(field1.getDescription()).thenReturn("Server host");

        FunctionParam field2 = mock(FunctionParam.class);
        when(field2.getValue()).thenReturn("port");
        when(field2.getDescription()).thenReturn("Server port");

        List<FunctionParam> fields = Arrays.asList(field1, field2);
        when(recordParam.getRecordFieldParams()).thenReturn(fields);

        StringBuilder result = new StringBuilder();
        boolean[] isFirst = {true};
        Set<String> processed = new HashSet<>();

        XmlPropertyWriter.writeXmlParameterElements(recordParam, result, isFirst, processed);

        String output = result.toString();
        Assert.assertTrue(output.contains("<parameter name=\"host\" description=\"Server host\"/>"));
        Assert.assertTrue(output.contains("<parameter name=\"port\" description=\"Server port\"/>"));
    }

    @Test
    public void testWriteXmlParameterElements_Union() {
        UnionFunctionParam unionParam = mock(UnionFunctionParam.class);
        when(unionParam.getValue()).thenReturn("authConfig");
        when(unionParam.getDescription()).thenReturn("Authentication config");

        FunctionParam member1 = mock(FunctionParam.class);
        when(member1.getValue()).thenReturn("basicAuth");
        when(member1.getDescription()).thenReturn("Basic authentication");

        List<FunctionParam> members = Arrays.asList(member1);
        when(unionParam.getUnionMemberParams()).thenReturn(members);

        StringBuilder result = new StringBuilder();
        boolean[] isFirst = {true};
        Set<String> processed = new HashSet<>();

        XmlPropertyWriter.writeXmlParameterElements(unionParam, result, isFirst, processed);

        String output = result.toString();
        // Should contain the DataType parameter
        Assert.assertTrue(output.contains("<parameter name=\"authConfig_DataType\""));
        // Should contain the member parameter
        Assert.assertTrue(output.contains("<parameter name=\"basicAuth\""));
    }

    @Test
    public void testWriteXmlParameterElements_Deduplication() {
        FunctionParam param = mock(FunctionParam.class);
        when(param.getValue()).thenReturn("token");
        when(param.getDescription()).thenReturn("Access Token");

        StringBuilder result = new StringBuilder();
        boolean[] isFirst = {true};
        Set<String> processed = new HashSet<>();

        // First call - should write
        XmlPropertyWriter.writeXmlParameterElements(param, result, isFirst, processed);
        String output1 = result.toString();
        Assert.assertTrue(output1.contains("<parameter name=\"token\""));

        // Second call - should not write (deduplicated)
        int lengthBefore = result.length();
        XmlPropertyWriter.writeXmlParameterElements(param, result, isFirst, processed);
        Assert.assertEquals(result.length(), lengthBefore);
    }

    @Test
    public void testEscapeXml() {
        Assert.assertEquals(XmlPropertyWriter.escapeXml("a < b"), "a &lt; b");
        Assert.assertEquals(XmlPropertyWriter.escapeXml("a > b"), "a &gt; b");
        Assert.assertEquals(XmlPropertyWriter.escapeXml("a & b"), "a &amp; b");
        Assert.assertEquals(XmlPropertyWriter.escapeXml("check \"value\""), "check &quot;value&quot;");
        Assert.assertEquals(XmlPropertyWriter.escapeXml(null), "");
    }

    @Test
    public void testEscapeXml_MultipleSpecialChars() {
        String input = "<tag attr=\"value\">content & more</tag>";
        String expected = "&lt;tag attr=&quot;value&quot;&gt;content &amp; more&lt;/tag&gt;";
        Assert.assertEquals(XmlPropertyWriter.escapeXml(input), expected);
    }

    @Test
    public void testEscapeXml_NoSpecialChars() {
        String input = "plain text with no special characters";
        Assert.assertEquals(XmlPropertyWriter.escapeXml(input), input);
    }

    @Test
    public void testUpdateEnableConditionsForUnionMembers() {
        UnionFunctionParam unionParam = mock(UnionFunctionParam.class);
        FunctionParam member = mock(FunctionParam.class);

        // Setup existing condition
        when(member.getEnableCondition()).thenReturn("condition == \"oldNameDataType\"");

        when(unionParam.getUnionMemberParams()).thenReturn(Arrays.asList(member));

        XmlPropertyWriter.updateEnableConditionsForUnionMembers(unionParam, "oldName", "newName");

        Mockito.verify(member).setEnableCondition("condition == \"newNameDataType\"");
    }

    @Test
    public void testUpdateEnableConditionsForUnionMembers_NullCondition() {
        UnionFunctionParam unionParam = mock(UnionFunctionParam.class);
        FunctionParam member = mock(FunctionParam.class);

        // Setup null condition
        when(member.getEnableCondition()).thenReturn(null);

        when(unionParam.getUnionMemberParams()).thenReturn(Arrays.asList(member));

        // Should not throw - gracefully handles null condition
        XmlPropertyWriter.updateEnableConditionsForUnionMembers(unionParam, "oldName", "newName");

        // setEnableCondition should not be called when condition is null
        Mockito.verify(member, Mockito.never()).setEnableCondition(Mockito.anyString());
    }

    @Test
    public void testUpdateEnableConditionsForUnionMembers_EmptyCondition() {
        UnionFunctionParam unionParam = mock(UnionFunctionParam.class);
        FunctionParam member = mock(FunctionParam.class);

        // Setup empty condition
        when(member.getEnableCondition()).thenReturn("");

        when(unionParam.getUnionMemberParams()).thenReturn(Arrays.asList(member));

        // Should not throw - gracefully handles empty condition
        XmlPropertyWriter.updateEnableConditionsForUnionMembers(unionParam, "oldName", "newName");

        // setEnableCondition should not be called when condition is empty
        Mockito.verify(member, Mockito.never()).setEnableCondition(Mockito.anyString());
    }

    @Test
    public void testWriteFunctionRecordFieldProperties() {
        FunctionParam fieldParam = mock(FunctionParam.class);
        when(fieldParam.getValue()).thenReturn("fieldName");
        when(fieldParam.getParamType()).thenReturn("string");

        StringBuilder result = new StringBuilder();
        int[] fieldIndexHolder = {0};

        XmlPropertyWriter.writeFunctionRecordFieldProperties(fieldParam, "recordParam", result, fieldIndexHolder);

        String output = result.toString();
        Assert.assertTrue(output.contains("<property name=\"recordParam_param0\" value=\"fieldName\"/>"));
        Assert.assertTrue(output.contains("<property name=\"recordParam_paramType0\" value=\"string\"/>"));
        Assert.assertEquals(fieldIndexHolder[0], 1);
    }

    @Test
    public void testWriteFunctionRecordFieldProperties_NestedRecord() {
        RecordFunctionParam nestedRecordParam = mock(RecordFunctionParam.class);
        when(nestedRecordParam.getValue()).thenReturn("nested");
        when(nestedRecordParam.getParamType()).thenReturn("record");

        FunctionParam nestedField = mock(FunctionParam.class);
        when(nestedField.getValue()).thenReturn("nestedField");
        when(nestedField.getParamType()).thenReturn("int");

        List<FunctionParam> nestedFields = Arrays.asList(nestedField);
        when(nestedRecordParam.getRecordFieldParams()).thenReturn(nestedFields);

        StringBuilder result = new StringBuilder();
        int[] fieldIndexHolder = {0};

        XmlPropertyWriter.writeFunctionRecordFieldProperties(nestedRecordParam, "parent", result, fieldIndexHolder);

        String output = result.toString();
        Assert.assertTrue(output.contains("<property name=\"parent_param0\" value=\"nestedField\"/>"));
        Assert.assertTrue(output.contains("<property name=\"parent_paramType0\" value=\"int\"/>"));
    }

    @Test
    public void testWriteXmlParamProperties_EmptyRecord() {
        RecordFunctionParam recordParam = mock(RecordFunctionParam.class);
        when(recordParam.getValue()).thenReturn("emptyConfig");
        when(recordParam.getParamType()).thenReturn("record");
        when(recordParam.getRecordFieldParams()).thenReturn(new ArrayList<>());

        StringBuilder result = new StringBuilder();
        int[] indexHolder = {0};
        boolean[] isFirst = {true};

        XmlPropertyWriter.writeXmlParamProperties(recordParam, "CONN", result, indexHolder, isFirst);

        String output = result.toString();
        Assert.assertTrue(output.contains("<property name=\"CONN_param0\" value=\"emptyConfig\"/>"));
        Assert.assertTrue(output.contains("<property name=\"CONN_paramType0\" value=\"record\"/>"));
    }

    @Test
    public void testWriteXmlParamProperties_NotFirst() {
        FunctionParam param = mock(FunctionParam.class);
        when(param.getValue()).thenReturn("secondParam");
        when(param.getParamType()).thenReturn("string");

        StringBuilder result = new StringBuilder();
        int[] indexHolder = {1};
        boolean[] isFirst = {false};

        XmlPropertyWriter.writeXmlParamProperties(param, "CONN", result, indexHolder, isFirst);

        String output = result.toString();
        Assert.assertTrue(output.startsWith("\n        "));
        Assert.assertTrue(output.contains("<property name=\"CONN_param1\" value=\"secondParam\"/>"));
    }

    @Test
    public void testWriteXmlParameterElements_NullDescription() {
        FunctionParam param = mock(FunctionParam.class);
        when(param.getValue()).thenReturn("nullDescParam");
        when(param.getDescription()).thenReturn(null);

        StringBuilder result = new StringBuilder();
        boolean[] isFirst = {true};
        Set<String> processed = new HashSet<>();

        XmlPropertyWriter.writeXmlParameterElements(param, result, isFirst, processed);

        String output = result.toString();
        Assert.assertTrue(output.contains("description=\"\""));
    }

    @Test
    public void testWriteXmlParameterElements_EmptyRecord() {
        RecordFunctionParam recordParam = mock(RecordFunctionParam.class);
        when(recordParam.getValue()).thenReturn("emptyRecord");
        when(recordParam.getRecordFieldParams()).thenReturn(new ArrayList<>());
        when(recordParam.getDescription()).thenReturn("Empty record");

        StringBuilder result = new StringBuilder();
        boolean[] isFirst = {true};
        Set<String> processed = new HashSet<>();

        XmlPropertyWriter.writeXmlParameterElements(recordParam, result, isFirst, processed);

        String output = result.toString();
        Assert.assertTrue(output.contains("<parameter name=\"emptyRecord\" description=\"Empty record\"/>"));
    }

    @Test
    public void testWriteXmlParameterElements_NestedUnion() {
        // Create a nested union inside a union
        UnionFunctionParam outerUnion = mock(UnionFunctionParam.class);
        when(outerUnion.getValue()).thenReturn("outerUnion");
        when(outerUnion.getDescription()).thenReturn("Outer union");

        UnionFunctionParam innerUnion = mock(UnionFunctionParam.class);
        when(innerUnion.getValue()).thenReturn("innerUnion");
        when(innerUnion.getDescription()).thenReturn("Inner union");
        when(innerUnion.getUnionMemberParams()).thenReturn(new ArrayList<>());

        List<FunctionParam> members = Arrays.asList(innerUnion);
        when(outerUnion.getUnionMemberParams()).thenReturn(members);

        StringBuilder result = new StringBuilder();
        boolean[] isFirst = {true};
        Set<String> processed = new HashSet<>();

        XmlPropertyWriter.writeXmlParameterElements(outerUnion, result, isFirst, processed);

        String output = result.toString();
        Assert.assertTrue(output.contains("outerUnion_DataType"));
        Assert.assertTrue(output.contains("innerUnion_DataType"));
    }

    @Test
    public void testWriteXmlParameterElements_NotFirstElement() {
        FunctionParam param = mock(FunctionParam.class);
        when(param.getValue()).thenReturn("secondElement");
        when(param.getDescription()).thenReturn("Second");

        StringBuilder result = new StringBuilder();
        result.append("<parameter name=\"first\"/>");
        boolean[] isFirst = {false};
        Set<String> processed = new HashSet<>();

        XmlPropertyWriter.writeXmlParameterElements(param, result, isFirst, processed);

        String output = result.toString();
        Assert.assertTrue(output.contains("\n    <parameter name=\"secondElement\""));
    }

    @Test
    public void testWriteRecordFieldParamProperties_UnionFieldParam() {
        UnionFunctionParam unionFieldParam = mock(UnionFunctionParam.class);
        when(unionFieldParam.getValue()).thenReturn("unionField");
        when(unionFieldParam.getParamType()).thenReturn("union");
        when(unionFieldParam.getUnionMemberParams()).thenReturn(new ArrayList<>());

        StringBuilder result = new StringBuilder();
        int[] fieldIndexHolder = {0};

        XmlPropertyWriter.writeRecordFieldParamProperties(unionFieldParam, "CONN", "recordName", result, fieldIndexHolder);

        String output = result.toString();
        Assert.assertTrue(output.contains("<property name=\"CONN_recordName_param0\" value=\"unionField\"/>"));
        Assert.assertTrue(output.contains("<property name=\"CONN_recordName_paramType0\" value=\"union\"/>"));
        Assert.assertTrue(output.contains("_dataType0\""));
    }

    @Test
    public void testWriteRecordFieldParamPropertiesWithUnionMember() {
        FunctionParam fieldParam = mock(FunctionParam.class);
        when(fieldParam.getValue()).thenReturn("field1");
        when(fieldParam.getParamType()).thenReturn("string");

        StringBuilder result = new StringBuilder();
        int[] fieldIndexHolder = {0};

        XmlPropertyWriter.writeRecordFieldParamPropertiesWithUnionMember(fieldParam, "CONN", "record",
                result, fieldIndexHolder, "MemberType");

        String output = result.toString();
        Assert.assertTrue(output.contains("<property name=\"CONN_record_param0\" value=\"field1\"/>"));
        Assert.assertTrue(output.contains("<property name=\"CONN_record_unionMember0\" value=\"MemberType\"/>"));
    }

    @Test
    public void testWriteFunctionRecordFieldProperties_UnionWithRecordMember() {
        UnionFunctionParam unionParam = mock(UnionFunctionParam.class);
        when(unionParam.getValue()).thenReturn("unionField");
        when(unionParam.getParamType()).thenReturn("union");

        RecordFunctionParam recordMember = mock(RecordFunctionParam.class);
        when(recordMember.getDisplayTypeName()).thenReturn("TypeA");
        when(recordMember.getRecordName()).thenReturn("TypeA");

        FunctionParam recordField = mock(FunctionParam.class);
        when(recordField.getValue()).thenReturn("innerField");
        when(recordField.getParamType()).thenReturn("int");

        when(recordMember.getRecordFieldParams()).thenReturn(Arrays.asList(recordField));
        when(unionParam.getUnionMemberParams()).thenReturn(Arrays.asList(recordMember));

        StringBuilder result = new StringBuilder();
        int[] fieldIndexHolder = {0};

        XmlPropertyWriter.writeFunctionRecordFieldProperties(unionParam, "record", result, fieldIndexHolder);

        String output = result.toString();
        Assert.assertTrue(output.contains("<property name=\"record_param0\" value=\"unionField\"/>"));
        Assert.assertTrue(output.contains("<property name=\"record_paramType0\" value=\"union\"/>"));
    }

    @Test
    public void testWriteFunctionRecordFieldPropertiesWithUnionMember_NullMemberTypeName() {
        UnionFunctionParam unionParam = mock(UnionFunctionParam.class);
        when(unionParam.getValue()).thenReturn("unionField");
        when(unionParam.getParamType()).thenReturn("union");

        RecordFunctionParam recordMember = mock(RecordFunctionParam.class);
        when(recordMember.getDisplayTypeName()).thenReturn(null);
        when(recordMember.getRecordName()).thenReturn("FallbackName");

        FunctionParam recordField = mock(FunctionParam.class);
        when(recordField.getValue()).thenReturn("innerField");
        when(recordField.getParamType()).thenReturn("string");

        when(recordMember.getRecordFieldParams()).thenReturn(Arrays.asList(recordField));
        when(unionParam.getUnionMemberParams()).thenReturn(Arrays.asList(recordMember));

        StringBuilder result = new StringBuilder();
        int[] fieldIndexHolder = {0};

        XmlPropertyWriter.writeFunctionRecordFieldPropertiesWithUnionMember(unionParam, "record",
                result, fieldIndexHolder, "ParentType");

        String output = result.toString();
        Assert.assertTrue(output.contains("<property name=\"record_param0\" value=\"unionField\"/>"));
    }

    @Test
    public void testWriteFunctionRecordFieldPropertiesWithUnionMember_EmptyMemberTypeName() {
        UnionFunctionParam unionParam = mock(UnionFunctionParam.class);
        when(unionParam.getValue()).thenReturn("unionField");
        when(unionParam.getParamType()).thenReturn("union");

        RecordFunctionParam recordMember = mock(RecordFunctionParam.class);
        when(recordMember.getDisplayTypeName()).thenReturn("");
        when(recordMember.getRecordName()).thenReturn("FallbackRecord");

        FunctionParam recordField = mock(FunctionParam.class);
        when(recordField.getValue()).thenReturn("innerField");
        when(recordField.getParamType()).thenReturn("boolean");

        when(recordMember.getRecordFieldParams()).thenReturn(Arrays.asList(recordField));
        when(unionParam.getUnionMemberParams()).thenReturn(Arrays.asList(recordMember));

        StringBuilder result = new StringBuilder();
        int[] fieldIndexHolder = {0};

        XmlPropertyWriter.writeFunctionRecordFieldPropertiesWithUnionMember(unionParam, "record",
                result, fieldIndexHolder, null);

        String output = result.toString();
        Assert.assertTrue(output.contains("<property name=\"record_param0\" value=\"unionField\"/>"));
    }

    @Test
    public void testUpdateEnableConditionsForUnionMembers_ConditionNoMatch() {
        UnionFunctionParam unionParam = mock(UnionFunctionParam.class);
        FunctionParam member = mock(FunctionParam.class);

        // Condition doesn't contain the old pattern
        when(member.getEnableCondition()).thenReturn("someOtherCondition");

        when(unionParam.getUnionMemberParams()).thenReturn(Arrays.asList(member));

        XmlPropertyWriter.updateEnableConditionsForUnionMembers(unionParam, "oldName", "newName");

        // Should still call setEnableCondition with the same value
        Mockito.verify(member).setEnableCondition("someOtherCondition");
    }

    @Test
    public void testEscapeXml_EmptyString() {
        Assert.assertEquals(XmlPropertyWriter.escapeXml(""), "");
    }

    @Test
    public void testEscapeXml_SingleCharacter() {
        Assert.assertEquals(XmlPropertyWriter.escapeXml("<"), "&lt;");
        Assert.assertEquals(XmlPropertyWriter.escapeXml(">"), "&gt;");
        Assert.assertEquals(XmlPropertyWriter.escapeXml("&"), "&amp;");
        Assert.assertEquals(XmlPropertyWriter.escapeXml("\""), "&quot;");
    }

    @Test
    public void testWriteXmlParameterElements_RecordWithMultipleFields() {
        RecordFunctionParam recordParam = mock(RecordFunctionParam.class);
        when(recordParam.getValue()).thenReturn("config");

        FunctionParam field1 = mock(FunctionParam.class);
        when(field1.getValue()).thenReturn("host");
        when(field1.getDescription()).thenReturn("Host name");

        FunctionParam field2 = mock(FunctionParam.class);
        when(field2.getValue()).thenReturn("port");
        when(field2.getDescription()).thenReturn("Port number");

        FunctionParam field3 = mock(FunctionParam.class);
        when(field3.getValue()).thenReturn("timeout");
        when(field3.getDescription()).thenReturn("Timeout in ms");

        List<FunctionParam> fields = Arrays.asList(field1, field2, field3);
        when(recordParam.getRecordFieldParams()).thenReturn(fields);

        StringBuilder result = new StringBuilder();
        boolean[] isFirst = {true};
        Set<String> processed = new HashSet<>();

        XmlPropertyWriter.writeXmlParameterElements(recordParam, result, isFirst, processed);

        String output = result.toString();
        Assert.assertTrue(output.contains("<parameter name=\"host\""));
        Assert.assertTrue(output.contains("<parameter name=\"port\""));
        Assert.assertTrue(output.contains("<parameter name=\"timeout\""));
        Assert.assertEquals(processed.size(), 3);
    }

    @Test
    public void testWriteXmlParameterElements_UnionWithNonRecordMember() {
        UnionFunctionParam unionParam = mock(UnionFunctionParam.class);
        when(unionParam.getValue()).thenReturn("mixedUnion");
        when(unionParam.getDescription()).thenReturn("Mixed union");

        FunctionParam simpleMember = mock(FunctionParam.class);
        when(simpleMember.getValue()).thenReturn("simpleValue");
        when(simpleMember.getDescription()).thenReturn("Simple value");

        List<FunctionParam> members = Arrays.asList(simpleMember);
        when(unionParam.getUnionMemberParams()).thenReturn(members);

        StringBuilder result = new StringBuilder();
        boolean[] isFirst = {true};
        Set<String> processed = new HashSet<>();

        XmlPropertyWriter.writeXmlParameterElements(unionParam, result, isFirst, processed);

        String output = result.toString();
        Assert.assertTrue(output.contains("mixedUnion_DataType"));
        Assert.assertTrue(output.contains("simpleValue"));
    }

    @Test
    public void testWriteRecordFieldParamPropertiesWithUnionMember_NestedRecord() {
        RecordFunctionParam nestedRecord = mock(RecordFunctionParam.class);
        when(nestedRecord.getValue()).thenReturn("nested");
        when(nestedRecord.getParamType()).thenReturn("record");

        FunctionParam innerField = mock(FunctionParam.class);
        when(innerField.getValue()).thenReturn("innerField");
        when(innerField.getParamType()).thenReturn("string");

        when(nestedRecord.getRecordFieldParams()).thenReturn(Arrays.asList(innerField));

        StringBuilder result = new StringBuilder();
        int[] fieldIndexHolder = {0};

        XmlPropertyWriter.writeRecordFieldParamPropertiesWithUnionMember(nestedRecord, "CONN", "parent",
                result, fieldIndexHolder, "MemberType");

        String output = result.toString();
        Assert.assertTrue(output.contains("<property name=\"CONN_parent_param0\" value=\"innerField\"/>"));
        Assert.assertTrue(output.contains("<property name=\"CONN_parent_unionMember0\" value=\"MemberType\"/>"));
    }

    @Test
    public void testWriteRecordFieldParamPropertiesWithUnionMember_UnionWithRecordMember() {
        UnionFunctionParam unionFieldParam = mock(UnionFunctionParam.class);
        when(unionFieldParam.getValue()).thenReturn("unionField");
        when(unionFieldParam.getParamType()).thenReturn("union");

        RecordFunctionParam recordMember = mock(RecordFunctionParam.class);
        when(recordMember.getDisplayTypeName()).thenReturn("TypeB");
        when(recordMember.getRecordName()).thenReturn("TypeB");

        FunctionParam recordField = mock(FunctionParam.class);
        when(recordField.getValue()).thenReturn("recordField");
        when(recordField.getParamType()).thenReturn("int");

        when(recordMember.getRecordFieldParams()).thenReturn(Arrays.asList(recordField));
        when(unionFieldParam.getUnionMemberParams()).thenReturn(Arrays.asList(recordMember));

        StringBuilder result = new StringBuilder();
        int[] fieldIndexHolder = {0};

        XmlPropertyWriter.writeRecordFieldParamPropertiesWithUnionMember(unionFieldParam, "CONN", "parent",
                result, fieldIndexHolder, "OuterMember");

        String output = result.toString();
        Assert.assertTrue(output.contains("<property name=\"CONN_parent_param0\" value=\"unionField\"/>"));
    }
}
