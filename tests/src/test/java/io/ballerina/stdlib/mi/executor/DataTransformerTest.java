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

package io.ballerina.stdlib.mi.executor;

import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.*;
import io.ballerina.runtime.api.utils.JsonUtils;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.stdlib.mi.Constants;
import io.ballerina.stdlib.mi.utils.SynapseUtils;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for DataTransformer utility class.
 */
public class DataTransformerTest {

    @Test
    public void testTransformNestedTableTo2DArray() {
        try (MockedStatic<StringUtils> stringUtilsMock = Mockito.mockStatic(StringUtils.class)) {
            BString innerArrayKey = mock(BString.class);
            BString valueKey = mock(BString.class);

            stringUtilsMock.when(() -> StringUtils.fromString("innerArray")).thenReturn(innerArrayKey);
            stringUtilsMock.when(() -> StringUtils.fromString("value")).thenReturn(valueKey);

            BArray outerArray = mock(BArray.class);
            when(outerArray.size()).thenReturn(1);

            BMap outerRow = mock(BMap.class);
            when(outerArray.get(0)).thenReturn(outerRow);

            BArray innerArray = mock(BArray.class);
            when(outerRow.get(innerArrayKey)).thenReturn(innerArray);
            when(innerArray.size()).thenReturn(2);

            BMap innerRow1 = mock(BMap.class);
            when(innerArray.get(0)).thenReturn(innerRow1);
            when(innerRow1.containsKey(valueKey)).thenReturn(true);
            when(innerRow1.size()).thenReturn(1);
            when(innerRow1.get(valueKey)).thenReturn(10L);

            BMap innerRow2 = mock(BMap.class);
            when(innerArray.get(1)).thenReturn(innerRow2);
            when(innerRow2.containsKey(valueKey)).thenReturn(true);
            when(innerRow2.size()).thenReturn(1);
            when(innerRow2.get(valueKey)).thenReturn("hello");

            String result = DataTransformer.transformNestedTableTo2DArray(outerArray);

            Assert.assertEquals(result, "[[10,\"hello\"]]");
        }
    }

    @Test
    public void testTransformTableArrayToSimpleArray() {
        try (MockedStatic<StringUtils> stringUtilsMock = Mockito.mockStatic(StringUtils.class)) {
            BString valueKey = mock(BString.class);
            stringUtilsMock.when(() -> StringUtils.fromString("value")).thenReturn(valueKey);

            BArray tableArray = mock(BArray.class);
            when(tableArray.size()).thenReturn(3);

            BMap row1 = mock(BMap.class);
            when(tableArray.get(0)).thenReturn(row1);
            BString strVal = mock(BString.class);
            when(strVal.getValue()).thenReturn("foo");
            when(row1.get(valueKey)).thenReturn(strVal);

            BMap row2 = mock(BMap.class);
            when(tableArray.get(1)).thenReturn(row2);
            when(row2.get(valueKey)).thenReturn(42);

            BMap row3 = mock(BMap.class);
            when(tableArray.get(2)).thenReturn(row3);
            when(row3.get(valueKey)).thenReturn(true);

            String result = DataTransformer.transformTableArrayToSimpleArray(tableArray);

            Assert.assertEquals(result, "[\"foo\",42,true]");
        }
    }

    @Test
    public void testReconstructRecordFromFields() {
        try (MockedStatic<SynapseUtils> synapseUtilsMock = Mockito.mockStatic(SynapseUtils.class);
             MockedStatic<JsonUtils> jsonUtilsMock = Mockito.mockStatic(JsonUtils.class)) {

            MessageContext context = mock(MessageContext.class);
            String prefix = "testPrefix";

            when(context.getProperty(prefix + "_param0")).thenReturn("name");
            when(context.getProperty(prefix + "_paramType0")).thenReturn(Constants.STRING);
            synapseUtilsMock.when(() -> SynapseUtils.lookupTemplateParameter(context, "name")).thenReturn("Alice");

            when(context.getProperty(prefix + "_param1")).thenReturn("age");
            when(context.getProperty(prefix + "_paramType1")).thenReturn(Constants.INT);
            synapseUtilsMock.when(() -> SynapseUtils.lookupTemplateParameter(context, "age")).thenReturn("30");

            when(context.getProperty(prefix + "_param2")).thenReturn(null);

            Object expectedBallerinaRecord = new Object();
            jsonUtilsMock.when(() -> JsonUtils.parse(anyString())).thenReturn(expectedBallerinaRecord);

            Object result = DataTransformer.reconstructRecordFromFields(prefix, context);

            Assert.assertSame(result, expectedBallerinaRecord);
        }
    }

    @Test
    public void testParseInnerTableValues() {
        Assert.assertEquals(DataTransformer.parseInnerTableValues(null, null), "[]");
        Assert.assertEquals(DataTransformer.parseInnerTableValues("", null), "[]");
        Assert.assertEquals(DataTransformer.parseInnerTableValues("[]", null), "[]");
    }

    @Test
    public void testAppendJsonValue_Direct() {
        StringBuilder sb = new StringBuilder();

        DataTransformer.appendJsonValue(sb, null);
        Assert.assertEquals(sb.toString(), "null");

        sb.setLength(0);
        DataTransformer.appendJsonValue(sb, true);
        Assert.assertEquals(sb.toString(), "true");

        sb.setLength(0);
        DataTransformer.appendJsonValue(sb, false);
        Assert.assertEquals(sb.toString(), "false");

        sb.setLength(0);
        DataTransformer.appendJsonValue(sb, 123);
        Assert.assertEquals(sb.toString(), "123");

        sb.setLength(0);
        DataTransformer.appendJsonValue(sb, 12.34);
        Assert.assertEquals(sb.toString(), "12.34");

        sb.setLength(0);
        DataTransformer.appendJsonValue(sb, "hello");
        Assert.assertEquals(sb.toString(), "\"hello\"");

        sb.setLength(0);
        DataTransformer.appendJsonValue(sb, "say \"hello\"");
        Assert.assertEquals(sb.toString(), "\"say \\\"hello\\\"\"");
    }

    @Test
    public void testEscapeJsonString() {
        Assert.assertEquals(DataTransformer.escapeJsonString(null), "");
        Assert.assertEquals(DataTransformer.escapeJsonString("hello"), "hello");
        Assert.assertEquals(DataTransformer.escapeJsonString("hello\nworld"), "hello\\nworld");
        Assert.assertEquals(DataTransformer.escapeJsonString("hello\tworld"), "hello\\tworld");
        Assert.assertEquals(DataTransformer.escapeJsonString("hello\\world"), "hello\\\\world");
        Assert.assertEquals(DataTransformer.escapeJsonString("say \"hi\""), "say \\\"hi\\\"");
        Assert.assertEquals(DataTransformer.escapeJsonString("line1\rline2"), "line1\\rline2");
    }

    @Test(expectedExceptions = SynapseException.class)
    public void testTransformTableArrayToSimpleArray_MissingValue() {
        try (MockedStatic<StringUtils> stringUtilsMock = Mockito.mockStatic(StringUtils.class)) {
            BString valueKey = mock(BString.class);
            stringUtilsMock.when(() -> StringUtils.fromString("value")).thenReturn(valueKey);

            BArray tableArray = mock(BArray.class);
            when(tableArray.size()).thenReturn(1);

            BMap row = mock(BMap.class);
            when(tableArray.get(0)).thenReturn(row);
            when(row.get(valueKey)).thenReturn(null);

            DataTransformer.transformTableArrayToSimpleArray(tableArray);
        }
    }

    @Test
    public void testCreateRecordValue_SimpleJson() {
        try (MockedStatic<JsonUtils> jsonUtilsMock = Mockito.mockStatic(JsonUtils.class)) {
            String jsonString = "{\"foo\":\"bar\"}";
            Object parsedParams = new Object();
            jsonUtilsMock.when(() -> JsonUtils.parse(jsonString)).thenReturn(parsedParams);

            MessageContext context = mock(MessageContext.class);
            when(context.getProperty("param0_recordName")).thenReturn(null);

            Object result = DataTransformer.createRecordValue(jsonString, "param0", context, 0);

            Assert.assertSame(result, parsedParams);
        }
    }

    @Test
    public void testGetJsonParameter_String() {
        try (MockedStatic<JsonUtils> jsonUtilsMock = Mockito.mockStatic(JsonUtils.class)) {
            String jsonString = "{\"key\":\"value\"}";
            Object expectedResult = new Object();
            jsonUtilsMock.when(() -> JsonUtils.parse(jsonString)).thenReturn(expectedResult);

            Object result = DataTransformer.getJsonParameter(jsonString);
            Assert.assertSame(result, expectedResult);
        }
    }

    @Test
    public void testGetJsonParameter_WithQuotes() {
        try (MockedStatic<JsonUtils> jsonUtilsMock = Mockito.mockStatic(JsonUtils.class)) {
            String quotedJson = "'{\"key\":\"value\"}'";
            String unquotedJson = "{\"key\":\"value\"}";
            Object expectedResult = new Object();
            jsonUtilsMock.when(() -> JsonUtils.parse(unquotedJson)).thenReturn(expectedResult);

            Object result = DataTransformer.getJsonParameter(quotedJson);
            Assert.assertSame(result, expectedResult);
        }
    }

    @Test
    public void testGetJsonParameter_NonString() {
        try (MockedStatic<JsonUtils> jsonUtilsMock = Mockito.mockStatic(JsonUtils.class)) {
            Object input = new Object() {
                @Override
                public String toString() {
                    return "{\"key\":123}";
                }
            };
            Object expectedResult = new Object();
            jsonUtilsMock.when(() -> JsonUtils.parse("{\"key\":123}")).thenReturn(expectedResult);

            Object result = DataTransformer.getJsonParameter(input);
            Assert.assertSame(result, expectedResult);
        }
    }

    @Test
    public void testReconstructRecordFromFields_AllTypes() {
        try (MockedStatic<SynapseUtils> synapseUtilsMock = Mockito.mockStatic(SynapseUtils.class);
             MockedStatic<JsonUtils> jsonUtilsMock = Mockito.mockStatic(JsonUtils.class)) {

            MessageContext context = mock(MessageContext.class);
            String prefix = "testPrefix";

            when(context.getProperty(prefix + "_param0")).thenReturn("isReady");
            when(context.getProperty(prefix + "_paramType0")).thenReturn(Constants.BOOLEAN);
            synapseUtilsMock.when(() -> SynapseUtils.lookupTemplateParameter(context, "isReady")).thenReturn("true");

            when(context.getProperty(prefix + "_param1")).thenReturn("count");
            when(context.getProperty(prefix + "_paramType1")).thenReturn(Constants.INT);
            synapseUtilsMock.when(() -> SynapseUtils.lookupTemplateParameter(context, "count")).thenReturn("42");

            when(context.getProperty(prefix + "_param2")).thenReturn("score");
            when(context.getProperty(prefix + "_paramType2")).thenReturn(Constants.FLOAT);
            synapseUtilsMock.when(() -> SynapseUtils.lookupTemplateParameter(context, "score")).thenReturn("3.14");

            when(context.getProperty(prefix + "_param3")).thenReturn("price");
            when(context.getProperty(prefix + "_paramType3")).thenReturn(Constants.DECIMAL);
            synapseUtilsMock.when(() -> SynapseUtils.lookupTemplateParameter(context, "price")).thenReturn("10.50");

            when(context.getProperty(prefix + "_param4")).thenReturn(null);

            Object expectedBallerinaRecord = new Object();
            jsonUtilsMock.when(() -> JsonUtils.parse(anyString())).thenAnswer(invocation -> {
                String json = invocation.getArgument(0);
                Assert.assertTrue(json.contains("\"isReady\":true"));
                Assert.assertTrue(json.contains("\"count\":42"));
                Assert.assertTrue(json.contains("\"score\":3.14"));
                Assert.assertTrue(json.contains("\"price\":10.50"));
                return expectedBallerinaRecord;
            });

            synapseUtilsMock.when(() -> SynapseUtils.cleanupJsonString(anyString())).thenAnswer(i -> i.getArgument(0));

            Object result = DataTransformer.reconstructRecordFromFields(prefix, context);
            Assert.assertSame(result, expectedBallerinaRecord);
        }
    }

    @Test
    public void testConvertValueToType_Null() {
        Object result = DataTransformer.convertValueToType(null, mock(Type.class));
        Assert.assertNull(result);
    }

    @Test
    public void testConvertValueToType_SimpleType() {
        Type simpleType = mock(Type.class);
        when(simpleType.getTag()).thenReturn(TypeTags.STRING_TAG);

        String sourceValue = "hello";
        Object result = DataTransformer.convertValueToType(sourceValue, simpleType);
        Assert.assertEquals(result, "hello");
    }

    @Test
    public void testTransformMIStudioNestedTableTo2DArray() {
        BArray outerArray = mock(BArray.class);
        when(outerArray.size()).thenReturn(1);

        BArray outerRow = mock(BArray.class);
        when(outerArray.get(0)).thenReturn(outerRow);
        when(outerRow.size()).thenReturn(2);

        when(outerRow.get(1)).thenReturn("[{value=100}, {value=test}]");

        String result = DataTransformer.transformMIStudioNestedTableTo2DArray(outerArray, null);
        Assert.assertEquals(result, "[[100,\"test\"]]");
    }

    @Test
    public void testTransformNestedTableTo2DArray_EmptyInnerArray() {
        try (MockedStatic<StringUtils> stringUtilsMock = Mockito.mockStatic(StringUtils.class)) {
            BString innerArrayKey = mock(BString.class);
            BString valueKey = mock(BString.class);

            stringUtilsMock.when(() -> StringUtils.fromString("innerArray")).thenReturn(innerArrayKey);
            stringUtilsMock.when(() -> StringUtils.fromString("value")).thenReturn(valueKey);

            BArray outerArray = mock(BArray.class);
            when(outerArray.size()).thenReturn(1);

            BMap outerRow = mock(BMap.class);
            when(outerArray.get(0)).thenReturn(outerRow);
            when(outerRow.get(innerArrayKey)).thenReturn(null);

            String result = DataTransformer.transformNestedTableTo2DArray(outerArray);
            Assert.assertEquals(result, "[[]]");
        }
    }

    @Test
    public void testTransformNestedTableTo2DArray_NonBMapOuterElement() {
        try (MockedStatic<StringUtils> stringUtilsMock = Mockito.mockStatic(StringUtils.class)) {
            BString innerArrayKey = mock(BString.class);
            BString valueKey = mock(BString.class);

            stringUtilsMock.when(() -> StringUtils.fromString("innerArray")).thenReturn(innerArrayKey);
            stringUtilsMock.when(() -> StringUtils.fromString("value")).thenReturn(valueKey);

            BArray outerArray = mock(BArray.class);
            when(outerArray.size()).thenReturn(1);
            when(outerArray.get(0)).thenReturn("not a BMap");

            String result = DataTransformer.transformNestedTableTo2DArray(outerArray);
            Assert.assertEquals(result, "[[]]");
        }
    }

    @Test
    public void testTransformMIStudioNestedTableTo2DArray_NonBArrayOuterElement() {
        BArray outerArray = mock(BArray.class);
        when(outerArray.size()).thenReturn(1);
        when(outerArray.get(0)).thenReturn("not a BArray");

        String result = DataTransformer.transformMIStudioNestedTableTo2DArray(outerArray, null);
        Assert.assertEquals(result, "[[]]");
    }

    @Test
    public void testTransformMIStudioNestedTableTo2DArray_ShortRow() {
        BArray outerArray = mock(BArray.class);
        when(outerArray.size()).thenReturn(1);

        BArray outerRow = mock(BArray.class);
        when(outerArray.get(0)).thenReturn(outerRow);
        when(outerRow.size()).thenReturn(1);

        String result = DataTransformer.transformMIStudioNestedTableTo2DArray(outerArray, null);
        Assert.assertEquals(result, "[[]]");
    }

    @Test
    public void testGetMapParameter_BMapDirect() {
        try (MockedStatic<JsonUtils> jsonUtilsMock = Mockito.mockStatic(JsonUtils.class);
             MockedStatic<SynapseUtils> synapseUtilsMock = Mockito.mockStatic(SynapseUtils.class)) {

            MessageContext context = mock(MessageContext.class);
            String jsonStr = "{\"a\":1}";
            BMap parsedMap = mock(BMap.class);

            synapseUtilsMock.when(() -> SynapseUtils.cleanupJsonString(jsonStr)).thenReturn(jsonStr);
            jsonUtilsMock.when(() -> JsonUtils.parse(jsonStr)).thenReturn(parsedMap);

            BMap result = DataTransformer.getMapParameter(jsonStr, context, "val");
            Assert.assertSame(result, parsedMap);
        }
    }

    @Test
    public void testGetMapParameter_EmptyArray() {
        try (MockedStatic<JsonUtils> jsonUtilsMock = Mockito.mockStatic(JsonUtils.class);
             MockedStatic<ValueCreator> valueCreatorMock = Mockito.mockStatic(ValueCreator.class);
             MockedStatic<SynapseUtils> synapseUtilsMock = Mockito.mockStatic(SynapseUtils.class)) {

            MessageContext context = mock(MessageContext.class);
            BArray emptyArray = mock(BArray.class);
            when(emptyArray.size()).thenReturn(0);

            synapseUtilsMock.when(() -> SynapseUtils.cleanupJsonString("[]")).thenReturn("[]");
            jsonUtilsMock.when(() -> JsonUtils.parse("[]")).thenReturn(emptyArray);

            BMap emptyMap = mock(BMap.class);
            valueCreatorMock.when(ValueCreator::createMapValue).thenReturn(emptyMap);

            BMap result = DataTransformer.getMapParameter("[]", context, "val");
            Assert.assertSame(result, emptyMap);
        }
    }

    @Test(expectedExceptions = SynapseException.class)
    public void testGetMapParameter_InvalidInput() {
        try (MockedStatic<JsonUtils> jsonUtilsMock = Mockito.mockStatic(JsonUtils.class);
             MockedStatic<SynapseUtils> synapseUtilsMock = Mockito.mockStatic(SynapseUtils.class)) {

            MessageContext context = mock(MessageContext.class);
            BArray array = mock(BArray.class);
            when(array.size()).thenReturn(1);
            when(array.get(0)).thenReturn("not a map or array");

            synapseUtilsMock.when(() -> SynapseUtils.cleanupJsonString("invalid")).thenReturn("invalid");
            jsonUtilsMock.when(() -> JsonUtils.parse("invalid")).thenReturn(array);

            DataTransformer.getMapParameter("invalid", context, "val");
        }
    }

    @Test
    public void testGetMapParameter_WithQuotes() {
        try (MockedStatic<JsonUtils> jsonUtilsMock = Mockito.mockStatic(JsonUtils.class);
             MockedStatic<SynapseUtils> synapseUtilsMock = Mockito.mockStatic(SynapseUtils.class)) {

            MessageContext context = mock(MessageContext.class);
            String quotedJson = "'{\"a\":1}'";
            String unquotedJson = "{\"a\":1}";
            BMap parsedMap = mock(BMap.class);

            synapseUtilsMock.when(() -> SynapseUtils.cleanupJsonString(unquotedJson)).thenReturn(unquotedJson);
            jsonUtilsMock.when(() -> JsonUtils.parse(unquotedJson)).thenReturn(parsedMap);

            BMap result = DataTransformer.getMapParameter(quotedJson, context, "val");
            Assert.assertSame(result, parsedMap);
        }
    }

    @Test
    public void testReconstructRecordFromFields_JsonType() {
        try (MockedStatic<SynapseUtils> synapseUtilsMock = Mockito.mockStatic(SynapseUtils.class);
             MockedStatic<JsonUtils> jsonUtilsMock = Mockito.mockStatic(JsonUtils.class)) {

            MessageContext context = mock(MessageContext.class);
            String prefix = "testPrefix";

            when(context.getProperty(prefix + "_param0")).thenReturn("data");
            when(context.getProperty(prefix + "_paramType0")).thenReturn(Constants.JSON);
            synapseUtilsMock.when(() -> SynapseUtils.lookupTemplateParameter(context, "data"))
                    .thenReturn("{\"nested\":true}");
            synapseUtilsMock.when(() -> SynapseUtils.cleanupJsonString("{\"nested\":true}"))
                    .thenReturn("{\"nested\":true}");

            when(context.getProperty(prefix + "_param1")).thenReturn(null);

            Object expectedBallerinaRecord = new Object();
            jsonUtilsMock.when(() -> JsonUtils.parse(anyString())).thenReturn(expectedBallerinaRecord);

            Object result = DataTransformer.reconstructRecordFromFields(prefix, context);
            Assert.assertSame(result, expectedBallerinaRecord);
        }
    }

    @Test
    public void testReconstructRecordFromFields_RecordType() {
        try (MockedStatic<SynapseUtils> synapseUtilsMock = Mockito.mockStatic(SynapseUtils.class);
             MockedStatic<JsonUtils> jsonUtilsMock = Mockito.mockStatic(JsonUtils.class)) {

            MessageContext context = mock(MessageContext.class);
            String prefix = "testPrefix";

            when(context.getProperty(prefix + "_param0")).thenReturn("person");
            when(context.getProperty(prefix + "_paramType0")).thenReturn(Constants.RECORD);
            synapseUtilsMock.when(() -> SynapseUtils.lookupTemplateParameter(context, "person"))
                    .thenReturn("{\"name\":\"John\"}");
            synapseUtilsMock.when(() -> SynapseUtils.cleanupJsonString("{\"name\":\"John\"}"))
                    .thenReturn("{\"name\":\"John\"}");

            when(context.getProperty(prefix + "_param1")).thenReturn(null);

            Object expectedBallerinaRecord = new Object();
            jsonUtilsMock.when(() -> JsonUtils.parse(anyString())).thenReturn(expectedBallerinaRecord);

            Object result = DataTransformer.reconstructRecordFromFields(prefix, context);
            Assert.assertSame(result, expectedBallerinaRecord);
        }
    }

    @Test
    public void testReconstructRecordFromFields_ArrayType() {
        try (MockedStatic<SynapseUtils> synapseUtilsMock = Mockito.mockStatic(SynapseUtils.class);
             MockedStatic<JsonUtils> jsonUtilsMock = Mockito.mockStatic(JsonUtils.class)) {

            MessageContext context = mock(MessageContext.class);
            String prefix = "testPrefix";

            when(context.getProperty(prefix + "_param0")).thenReturn("items");
            when(context.getProperty(prefix + "_paramType0")).thenReturn(Constants.ARRAY);
            synapseUtilsMock.when(() -> SynapseUtils.lookupTemplateParameter(context, "items"))
                    .thenReturn("[1,2,3]");
            synapseUtilsMock.when(() -> SynapseUtils.cleanupJsonString("[1,2,3]"))
                    .thenReturn("[1,2,3]");

            when(context.getProperty(prefix + "_param1")).thenReturn(null);

            Object expectedBallerinaRecord = new Object();
            jsonUtilsMock.when(() -> JsonUtils.parse(anyString())).thenReturn(expectedBallerinaRecord);

            Object result = DataTransformer.reconstructRecordFromFields(prefix, context);
            Assert.assertSame(result, expectedBallerinaRecord);
        }
    }

    @Test
    public void testReconstructRecordFromFields_MapType() {
        try (MockedStatic<SynapseUtils> synapseUtilsMock = Mockito.mockStatic(SynapseUtils.class);
             MockedStatic<JsonUtils> jsonUtilsMock = Mockito.mockStatic(JsonUtils.class)) {

            MessageContext context = mock(MessageContext.class);
            String prefix = "testPrefix";

            when(context.getProperty(prefix + "_param0")).thenReturn("mappings");
            when(context.getProperty(prefix + "_paramType0")).thenReturn(Constants.MAP);
            synapseUtilsMock.when(() -> SynapseUtils.lookupTemplateParameter(context, "mappings"))
                    .thenReturn("{\"key1\":\"val1\"}");
            synapseUtilsMock.when(() -> SynapseUtils.cleanupJsonString("{\"key1\":\"val1\"}"))
                    .thenReturn("{\"key1\":\"val1\"}");

            when(context.getProperty(prefix + "_param1")).thenReturn(null);

            Object expectedBallerinaRecord = new Object();
            jsonUtilsMock.when(() -> JsonUtils.parse(anyString())).thenReturn(expectedBallerinaRecord);

            Object result = DataTransformer.reconstructRecordFromFields(prefix, context);
            Assert.assertSame(result, expectedBallerinaRecord);
        }
    }

    @Test
    public void testReconstructRecordFromFields_EmptyArraySkipped() {
        try (MockedStatic<SynapseUtils> synapseUtilsMock = Mockito.mockStatic(SynapseUtils.class);
             MockedStatic<JsonUtils> jsonUtilsMock = Mockito.mockStatic(JsonUtils.class)) {

            MessageContext context = mock(MessageContext.class);
            String prefix = "testPrefix";

            when(context.getProperty(prefix + "_param0")).thenReturn("items");
            when(context.getProperty(prefix + "_paramType0")).thenReturn(Constants.ARRAY);
            synapseUtilsMock.when(() -> SynapseUtils.lookupTemplateParameter(context, "items"))
                    .thenReturn("[]");

            when(context.getProperty(prefix + "_param1")).thenReturn(null);

            Object expectedBallerinaRecord = new Object();
            jsonUtilsMock.when(() -> JsonUtils.parse(anyString())).thenReturn(expectedBallerinaRecord);

            Object result = DataTransformer.reconstructRecordFromFields(prefix, context);
            Assert.assertSame(result, expectedBallerinaRecord);
        }
    }

    @Test
    public void testReconstructRecordFromFields_NestedField() {
        try (MockedStatic<SynapseUtils> synapseUtilsMock = Mockito.mockStatic(SynapseUtils.class);
             MockedStatic<JsonUtils> jsonUtilsMock = Mockito.mockStatic(JsonUtils.class)) {

            MessageContext context = mock(MessageContext.class);
            String prefix = "testPrefix";

            when(context.getProperty(prefix + "_param0")).thenReturn("address.city");
            when(context.getProperty(prefix + "_paramType0")).thenReturn(Constants.STRING);
            synapseUtilsMock.when(() -> SynapseUtils.lookupTemplateParameter(context, "address_city"))
                    .thenReturn("New York");

            when(context.getProperty(prefix + "_param1")).thenReturn(null);

            Object expectedBallerinaRecord = new Object();
            jsonUtilsMock.when(() -> JsonUtils.parse(anyString())).thenReturn(expectedBallerinaRecord);

            Object result = DataTransformer.reconstructRecordFromFields(prefix, context);
            Assert.assertSame(result, expectedBallerinaRecord);
        }
    }

    @Test
    public void testReconstructRecordFromFields_UnionType() {
        try (MockedStatic<SynapseUtils> synapseUtilsMock = Mockito.mockStatic(SynapseUtils.class);
             MockedStatic<JsonUtils> jsonUtilsMock = Mockito.mockStatic(JsonUtils.class)) {

            MessageContext context = mock(MessageContext.class);
            String prefix = "testPrefix";

            when(context.getProperty(prefix + "_param0")).thenReturn("unionField");
            when(context.getProperty(prefix + "_paramType0")).thenReturn(Constants.UNION);
            synapseUtilsMock.when(() -> SynapseUtils.lookupTemplateParameter(context, "unionField"))
                    .thenReturn("{\"nested\":true}");
            synapseUtilsMock.when(() -> SynapseUtils.cleanupJsonString("{\"nested\":true}"))
                    .thenReturn("{\"nested\":true}");

            when(context.getProperty(prefix + "_param1")).thenReturn(null);

            Object expectedBallerinaRecord = new Object();
            jsonUtilsMock.when(() -> JsonUtils.parse(anyString())).thenReturn(expectedBallerinaRecord);

            Object result = DataTransformer.reconstructRecordFromFields(prefix, context);
            Assert.assertSame(result, expectedBallerinaRecord);
        }
    }

    @Test
    public void testCreateRecordValue_QuotedJson() {
        try (MockedStatic<JsonUtils> jsonUtilsMock = Mockito.mockStatic(JsonUtils.class)) {
            String quotedJson = "'{\"foo\":\"bar\"}'";
            String unquotedJson = "{\"foo\":\"bar\"}";
            Object parsedResult = new Object();
            jsonUtilsMock.when(() -> JsonUtils.parse(unquotedJson)).thenReturn(parsedResult);

            MessageContext context = mock(MessageContext.class);
            when(context.getProperty("param0_recordName")).thenReturn(null);

            Object result = DataTransformer.createRecordValue(quotedJson, "param0", context, 0);
            Assert.assertSame(result, parsedResult);
        }
    }

    @Test
    public void testGetMapParameter_NonStringParam() {
        try (MockedStatic<JsonUtils> jsonUtilsMock = Mockito.mockStatic(JsonUtils.class);
             MockedStatic<SynapseUtils> synapseUtilsMock = Mockito.mockStatic(SynapseUtils.class)) {

            MessageContext context = mock(MessageContext.class);
            Object param = new Object() {
                @Override
                public String toString() {
                    return "{\"a\":1}";
                }
            };
            BMap parsedMap = mock(BMap.class);

            synapseUtilsMock.when(() -> SynapseUtils.cleanupJsonString("{\"a\":1}")).thenReturn("{\"a\":1}");
            jsonUtilsMock.when(() -> JsonUtils.parse("{\"a\":1}")).thenReturn(parsedMap);

            BMap result = DataTransformer.getMapParameter(param, context, "val");
            Assert.assertSame(result, parsedMap);
        }
    }

    @Test
    public void testTransformNestedTableTo2DArray_MultipleRows() {
        try (MockedStatic<StringUtils> stringUtilsMock = Mockito.mockStatic(StringUtils.class)) {
            BString innerArrayKey = mock(BString.class);
            BString valueKey = mock(BString.class);

            stringUtilsMock.when(() -> StringUtils.fromString("innerArray")).thenReturn(innerArrayKey);
            stringUtilsMock.when(() -> StringUtils.fromString("value")).thenReturn(valueKey);

            BArray outerArray = mock(BArray.class);
            when(outerArray.size()).thenReturn(2);

            BMap outerRow1 = mock(BMap.class);
            when(outerArray.get(0)).thenReturn(outerRow1);
            BArray innerArray1 = mock(BArray.class);
            when(outerRow1.get(innerArrayKey)).thenReturn(innerArray1);
            when(innerArray1.size()).thenReturn(1);
            BMap innerRow1 = mock(BMap.class);
            when(innerArray1.get(0)).thenReturn(innerRow1);
            when(innerRow1.containsKey(valueKey)).thenReturn(true);
            when(innerRow1.size()).thenReturn(1);
            when(innerRow1.get(valueKey)).thenReturn(1);

            BMap outerRow2 = mock(BMap.class);
            when(outerArray.get(1)).thenReturn(outerRow2);
            BArray innerArray2 = mock(BArray.class);
            when(outerRow2.get(innerArrayKey)).thenReturn(innerArray2);
            when(innerArray2.size()).thenReturn(1);
            BMap innerRow2 = mock(BMap.class);
            when(innerArray2.get(0)).thenReturn(innerRow2);
            when(innerRow2.containsKey(valueKey)).thenReturn(true);
            when(innerRow2.size()).thenReturn(1);
            when(innerRow2.get(valueKey)).thenReturn(2);

            String result = DataTransformer.transformNestedTableTo2DArray(outerArray);
            Assert.assertEquals(result, "[[1],[2]]");
        }
    }

    @Test
    public void testTransformNestedTableTo2DArray_InnerElementNotBMap() {
        try (MockedStatic<StringUtils> stringUtilsMock = Mockito.mockStatic(StringUtils.class)) {
            BString innerArrayKey = mock(BString.class);
            BString valueKey = mock(BString.class);

            stringUtilsMock.when(() -> StringUtils.fromString("innerArray")).thenReturn(innerArrayKey);
            stringUtilsMock.when(() -> StringUtils.fromString("value")).thenReturn(valueKey);

            BArray outerArray = mock(BArray.class);
            when(outerArray.size()).thenReturn(1);

            BMap outerRow = mock(BMap.class);
            when(outerArray.get(0)).thenReturn(outerRow);
            BArray innerArray = mock(BArray.class);
            when(outerRow.get(innerArrayKey)).thenReturn(innerArray);
            when(innerArray.size()).thenReturn(1);
            when(innerArray.get(0)).thenReturn("plainValue");

            String result = DataTransformer.transformNestedTableTo2DArray(outerArray);
            Assert.assertEquals(result, "[[\"plainValue\"]]");
        }
    }

    @Test
    public void testTransformNestedTableTo2DArray_BMapWithoutSingleValue() {
        try (MockedStatic<StringUtils> stringUtilsMock = Mockito.mockStatic(StringUtils.class)) {
            BString innerArrayKey = mock(BString.class);
            BString valueKey = mock(BString.class);

            stringUtilsMock.when(() -> StringUtils.fromString("innerArray")).thenReturn(innerArrayKey);
            stringUtilsMock.when(() -> StringUtils.fromString("value")).thenReturn(valueKey);

            BArray outerArray = mock(BArray.class);
            when(outerArray.size()).thenReturn(1);

            BMap outerRow = mock(BMap.class);
            when(outerArray.get(0)).thenReturn(outerRow);
            BArray innerArray = mock(BArray.class);
            when(outerRow.get(innerArrayKey)).thenReturn(innerArray);
            when(innerArray.size()).thenReturn(1);

            BMap innerRow = mock(BMap.class);
            when(innerArray.get(0)).thenReturn(innerRow);
            when(innerRow.containsKey(valueKey)).thenReturn(true);
            when(innerRow.size()).thenReturn(2);
            when(innerRow.toString()).thenReturn("{\"a\":1,\"b\":2}");

            String result = DataTransformer.transformNestedTableTo2DArray(outerArray);
            Assert.assertEquals(result, "[[{\"a\":1,\"b\":2}]]");
        }
    }

    @Test
    public void testTransformMIStudioNestedTableTo2DArray_MultipleRows() {
        BArray outerArray = mock(BArray.class);
        when(outerArray.size()).thenReturn(2);

        BArray outerRow1 = mock(BArray.class);
        when(outerArray.get(0)).thenReturn(outerRow1);
        when(outerRow1.size()).thenReturn(2);
        when(outerRow1.get(1)).thenReturn("[{value=1}]");

        BArray outerRow2 = mock(BArray.class);
        when(outerArray.get(1)).thenReturn(outerRow2);
        when(outerRow2.size()).thenReturn(2);
        when(outerRow2.get(1)).thenReturn("[{value=2}]");

        String result = DataTransformer.transformMIStudioNestedTableTo2DArray(outerArray, null);
        Assert.assertEquals(result, "[[1],[2]]");
    }

    @Test
    public void testParseInnerTableValues_WithValues() {
        try (MockedStatic<SynapseUtils> synapseUtilsMock = Mockito.mockStatic(SynapseUtils.class)) {
            String innerTableStr = "[{value=100}, {value=hello}]";
            synapseUtilsMock.when(() -> SynapseUtils.resolveSynapseExpressions(innerTableStr, null))
                    .thenReturn(innerTableStr);

            String result = DataTransformer.parseInnerTableValues(innerTableStr, null);
            Assert.assertEquals(result, "[100,\"hello\"]");
        }
    }

    @Test
    public void testTransformTableArrayToSimpleArray_NonBStringValue() {
        try (MockedStatic<StringUtils> stringUtilsMock = Mockito.mockStatic(StringUtils.class)) {
            BString valueKey = mock(BString.class);
            stringUtilsMock.when(() -> StringUtils.fromString("value")).thenReturn(valueKey);

            BArray tableArray = mock(BArray.class);
            when(tableArray.size()).thenReturn(1);

            BMap row = mock(BMap.class);
            when(tableArray.get(0)).thenReturn(row);
            Object complexValue = new Object() {
                @Override
                public String toString() {
                    return "complex";
                }
            };
            when(row.get(valueKey)).thenReturn(complexValue);

            String result = DataTransformer.transformTableArrayToSimpleArray(tableArray);
            Assert.assertEquals(result, "[\"complex\"]");
        }
    }

    @Test
    public void testAppendJsonValue_LongNumber() {
        StringBuilder sb = new StringBuilder();
        DataTransformer.appendJsonValue(sb, 1234567890123L);
        Assert.assertEquals(sb.toString(), "1234567890123");
    }

    @Test
    public void testAppendJsonValue_NegativeDouble() {
        StringBuilder sb = new StringBuilder();
        DataTransformer.appendJsonValue(sb, -3.14);
        Assert.assertEquals(sb.toString(), "-3.14");
    }

    @Test
    public void testEscapeJsonString_AllEscapes() {
        String input = "a\\b\"c\nd\re\tf";
        String expected = "a\\\\b\\\"c\\nd\\re\\tf";
        Assert.assertEquals(DataTransformer.escapeJsonString(input), expected);
    }

    @Test
    public void testConvertValueToType_RecordType() {
        try (MockedStatic<ValueCreator> valueCreatorMock = Mockito.mockStatic(ValueCreator.class);
             MockedStatic<StringUtils> stringUtilsMock = Mockito.mockStatic(StringUtils.class)) {

            BMap<BString, Object> sourceMap = mock(BMap.class);
            StructureType targetType = mock(StructureType.class);
            Field field = mock(Field.class);

            io.ballerina.runtime.api.Module module = mock(io.ballerina.runtime.api.Module.class);
            when(targetType.getPackage()).thenReturn(module);
            when(targetType.getName()).thenReturn("TestRecord");
            when(targetType.getTag()).thenReturn(TypeTags.RECORD_TYPE_TAG);

            Map<String, Field> fields = new HashMap<>();
            fields.put("name", field);
            when(targetType.getFields()).thenReturn(fields);
            when(field.getFieldName()).thenReturn("name");

            Type fieldType = mock(Type.class);
            when(fieldType.getTag()).thenReturn(TypeTags.STRING_TAG);
            when(field.getFieldType()).thenReturn(fieldType);

            BString bFieldName = mock(BString.class);
            stringUtilsMock.when(() -> StringUtils.fromString("name")).thenReturn(bFieldName);
            when(sourceMap.containsKey(bFieldName)).thenReturn(true);
            when(sourceMap.get(bFieldName)).thenReturn("testValue");

            BMap<BString, Object> typedRecord = mock(BMap.class);
            valueCreatorMock.when(() -> ValueCreator.createRecordValue(module, "TestRecord"))
                    .thenReturn(typedRecord);

            Object result = DataTransformer.convertValueToType(sourceMap, targetType);
            Assert.assertSame(result, typedRecord);
        }
    }

    @Test
    public void testConvertValueToType_ArrayType() {
        try (MockedStatic<ValueCreator> valueCreatorMock = Mockito.mockStatic(ValueCreator.class)) {
            BArray sourceArray = mock(BArray.class);
            ArrayType targetType = mock(ArrayType.class);

            Type elementType = mock(Type.class);
            when(elementType.getTag()).thenReturn(TypeTags.STRING_TAG);
            when(targetType.getTag()).thenReturn(TypeTags.ARRAY_TAG);
            when(targetType.getElementType()).thenReturn(elementType);
            when(sourceArray.size()).thenReturn(0);

            Object result = DataTransformer.convertValueToType(sourceArray, targetType);
            Assert.assertSame(result, sourceArray);
        }
    }

    @Test
    public void testGetMapParameter_TableArray() {
        try (MockedStatic<JsonUtils> jsonUtilsMock = Mockito.mockStatic(JsonUtils.class);
             MockedStatic<ValueCreator> valueCreatorMock = Mockito.mockStatic(ValueCreator.class);
             MockedStatic<StringUtils> stringUtilsMock = Mockito.mockStatic(StringUtils.class);
             MockedStatic<SynapseUtils> synapseUtilsMock = Mockito.mockStatic(SynapseUtils.class)) {

            MessageContext context = mock(MessageContext.class);
            BArray tableArray = mock(BArray.class);
            BMap row = mock(BMap.class);

            BString keyFieldName = mock(BString.class);
            BString valueFieldName = mock(BString.class);
            stringUtilsMock.when(() -> StringUtils.fromString("key")).thenReturn(keyFieldName);
            stringUtilsMock.when(() -> StringUtils.fromString("value")).thenReturn(valueFieldName);

            when(tableArray.size()).thenReturn(1);
            when(tableArray.get(0)).thenReturn(row);
            when(row.containsKey(keyFieldName)).thenReturn(true);
            when(row.containsKey(valueFieldName)).thenReturn(true);
            when(row.size()).thenReturn(2);
            when(row.get(keyFieldName)).thenReturn("myKey");
            when(row.get(valueFieldName)).thenReturn("myValue");

            Object[] keys = { keyFieldName, valueFieldName };
            when(row.getKeys()).thenReturn(keys);

            synapseUtilsMock.when(() -> SynapseUtils.cleanupJsonString("[{\"key\":\"myKey\",\"value\":\"myValue\"}]"))
                    .thenReturn("[{\"key\":\"myKey\",\"value\":\"myValue\"}]");
            jsonUtilsMock.when(() -> JsonUtils.parse("[{\"key\":\"myKey\",\"value\":\"myValue\"}]"))
                    .thenReturn(tableArray);

            BMap resultMap = mock(BMap.class);
            valueCreatorMock.when(ValueCreator::createMapValue).thenReturn(resultMap);

            BMap result = DataTransformer.getMapParameter("[{\"key\":\"myKey\",\"value\":\"myValue\"}]", context, "param0");
            Assert.assertSame(result, resultMap);
        }
    }

    @Test
    public void testParseInnerTableValues_ResolvedExpression() {
        try (MockedStatic<SynapseUtils> synapseUtilsMock = Mockito.mockStatic(SynapseUtils.class)) {
            String innerTableStr = "[{value=item1}]";
            synapseUtilsMock.when(() -> SynapseUtils.resolveSynapseExpressions(innerTableStr, null))
                    .thenReturn("[{value=item1}]");

            String result = DataTransformer.parseInnerTableValues(innerTableStr, null);
            Assert.assertEquals(result, "[\"item1\"]");
        }
    }

    @Test
    public void testParseInnerTableValues_NumericValues() {
        try (MockedStatic<SynapseUtils> synapseUtilsMock = Mockito.mockStatic(SynapseUtils.class)) {
            String innerTableStr = "[{value=123}, {value=45.67}]";
            synapseUtilsMock.when(() -> SynapseUtils.resolveSynapseExpressions(innerTableStr, null))
                    .thenReturn("[{value=123}, {value=45.67}]");

            String result = DataTransformer.parseInnerTableValues(innerTableStr, null);
            Assert.assertEquals(result, "[123,45.67]");
        }
    }

    @Test
    public void testParseInnerTableValues_BooleanValues() {
        try (MockedStatic<SynapseUtils> synapseUtilsMock = Mockito.mockStatic(SynapseUtils.class)) {
            String innerTableStr = "[{value=true}, {value=false}]";
            synapseUtilsMock.when(() -> SynapseUtils.resolveSynapseExpressions(innerTableStr, null))
                    .thenReturn("[{value=true}, {value=false}]");

            String result = DataTransformer.parseInnerTableValues(innerTableStr, null);
            Assert.assertEquals(result, "[true,false]");
        }
    }

    @Test
    public void testAppendJsonValue_BooleanTrue() {
        StringBuilder sb = new StringBuilder();
        DataTransformer.appendJsonValue(sb, true);
        Assert.assertEquals(sb.toString(), "true");
    }

    @Test
    public void testAppendJsonValue_BooleanFalse() {
        StringBuilder sb = new StringBuilder();
        DataTransformer.appendJsonValue(sb, false);
        Assert.assertEquals(sb.toString(), "false");
    }

    @Test
    public void testAppendJsonValue_IntegerZero() {
        StringBuilder sb = new StringBuilder();
        DataTransformer.appendJsonValue(sb, 0);
        Assert.assertEquals(sb.toString(), "0");
    }

    @Test
    public void testAppendJsonValue_ZeroPointZero() {
        StringBuilder sb = new StringBuilder();
        DataTransformer.appendJsonValue(sb, 0.0);
        Assert.assertEquals(sb.toString(), "0.0");
    }

    @Test
    public void testTransformTableArrayToSimpleArray_NumberValue() {
        try (MockedStatic<StringUtils> stringUtilsMock = Mockito.mockStatic(StringUtils.class)) {
            BString valueKey = mock(BString.class);
            stringUtilsMock.when(() -> StringUtils.fromString("value")).thenReturn(valueKey);

            BArray tableArray = mock(BArray.class);
            when(tableArray.size()).thenReturn(1);

            BMap row = mock(BMap.class);
            when(tableArray.get(0)).thenReturn(row);
            when(row.get(valueKey)).thenReturn(100);

            String result = DataTransformer.transformTableArrayToSimpleArray(tableArray);
            Assert.assertEquals(result, "[100]");
        }
    }

    @Test
    public void testTransformTableArrayToSimpleArray_BooleanValue() {
        try (MockedStatic<StringUtils> stringUtilsMock = Mockito.mockStatic(StringUtils.class)) {
            BString valueKey = mock(BString.class);
            stringUtilsMock.when(() -> StringUtils.fromString("value")).thenReturn(valueKey);

            BArray tableArray = mock(BArray.class);
            when(tableArray.size()).thenReturn(1);

            BMap row = mock(BMap.class);
            when(tableArray.get(0)).thenReturn(row);
            when(row.get(valueKey)).thenReturn(Boolean.TRUE);

            String result = DataTransformer.transformTableArrayToSimpleArray(tableArray);
            Assert.assertEquals(result, "[true]");
        }
    }

    @Test
    public void testParseInnerTableValues_EmptyBrackets() {
        String result = DataTransformer.parseInnerTableValues("[]", null);
        Assert.assertEquals(result, "[]");
    }

    @Test
    public void testParseInnerTableValues_WhitespaceOnly() {
        String result = DataTransformer.parseInnerTableValues("   ", null);
        Assert.assertEquals(result, "[]");
    }

    @Test
    public void testTransformMIStudioNestedTableTo2DArray_EmptyArray() {
        BArray outerArray = mock(BArray.class);
        when(outerArray.size()).thenReturn(0);

        String result = DataTransformer.transformMIStudioNestedTableTo2DArray(outerArray, null);
        Assert.assertEquals(result, "[]");
    }

    @Test
    public void testGetMapParameter_2DArrayWithFieldNames() {
        try (MockedStatic<JsonUtils> jsonUtilsMock = Mockito.mockStatic(JsonUtils.class);
             MockedStatic<ValueCreator> valueCreatorMock = Mockito.mockStatic(ValueCreator.class);
             MockedStatic<StringUtils> stringUtilsMock = Mockito.mockStatic(StringUtils.class);
             MockedStatic<SynapseUtils> synapseUtilsMock = Mockito.mockStatic(SynapseUtils.class)) {

            MessageContext context = mock(MessageContext.class);
            BArray array2D = mock(BArray.class);
            BArray innerRow = mock(BArray.class);
            BString keyBString = mock(BString.class);

            stringUtilsMock.when(() -> StringUtils.fromString("key")).thenReturn(keyBString);
            stringUtilsMock.when(() -> StringUtils.fromString("myKey")).thenReturn(mock(BString.class));
            stringUtilsMock.when(() -> StringUtils.fromString("field0")).thenReturn(mock(BString.class));

            when(array2D.size()).thenReturn(1);
            when(array2D.get(0)).thenReturn(innerRow);
            when(innerRow.size()).thenReturn(2);
            when(innerRow.get(0)).thenReturn("myKey");
            when(innerRow.get(1)).thenReturn("myValue");

            synapseUtilsMock.when(() -> SynapseUtils.cleanupJsonString("[[\"myKey\",\"myValue\"]]"))
                    .thenReturn("[[\"myKey\",\"myValue\"]]");
            jsonUtilsMock.when(() -> JsonUtils.parse("[[\"myKey\",\"myValue\"]]"))
                    .thenReturn(array2D);

            BMap resultMap = mock(BMap.class);
            valueCreatorMock.when(ValueCreator::createMapValue).thenReturn(resultMap);

            when(context.getProperty("mapRecordFields0")).thenReturn(null);

            BMap result = DataTransformer.getMapParameter("[[\"myKey\",\"myValue\"]]", context, "param0");
            Assert.assertSame(result, resultMap);
        }
    }

    @Test
    public void testTransformNestedTableTo2DArray_BMapWithNoValueField() {
        try (MockedStatic<StringUtils> stringUtilsMock = Mockito.mockStatic(StringUtils.class)) {
            BString innerArrayKey = mock(BString.class);
            BString valueKey = mock(BString.class);

            stringUtilsMock.when(() -> StringUtils.fromString("innerArray")).thenReturn(innerArrayKey);
            stringUtilsMock.when(() -> StringUtils.fromString("value")).thenReturn(valueKey);

            BArray outerArray = mock(BArray.class);
            when(outerArray.size()).thenReturn(1);

            BMap outerRow = mock(BMap.class);
            when(outerArray.get(0)).thenReturn(outerRow);
            BArray innerArray = mock(BArray.class);
            when(outerRow.get(innerArrayKey)).thenReturn(innerArray);
            when(innerArray.size()).thenReturn(1);

            BMap innerRow = mock(BMap.class);
            when(innerArray.get(0)).thenReturn(innerRow);
            when(innerRow.containsKey(valueKey)).thenReturn(false);

            String result = DataTransformer.transformNestedTableTo2DArray(outerArray);
            Assert.assertTrue(result.startsWith("[["));
        }
    }

    @Test
    public void testGetMapParameter_2DArrayWithMoreThan2Columns() {
        try (MockedStatic<JsonUtils> jsonUtilsMock = Mockito.mockStatic(JsonUtils.class);
             MockedStatic<ValueCreator> valueCreatorMock = Mockito.mockStatic(ValueCreator.class);
             MockedStatic<StringUtils> stringUtilsMock = Mockito.mockStatic(StringUtils.class);
             MockedStatic<SynapseUtils> synapseUtilsMock = Mockito.mockStatic(SynapseUtils.class)) {

            MessageContext context = mock(MessageContext.class);
            BArray array2D = mock(BArray.class);
            BArray innerRow = mock(BArray.class);

            stringUtilsMock.when(() -> StringUtils.fromString("key")).thenReturn(mock(BString.class));
            stringUtilsMock.when(() -> StringUtils.fromString("myKey")).thenReturn(mock(BString.class));
            stringUtilsMock.when(() -> StringUtils.fromString("field0")).thenReturn(mock(BString.class));
            stringUtilsMock.when(() -> StringUtils.fromString("field1")).thenReturn(mock(BString.class));

            when(array2D.size()).thenReturn(1);
            when(array2D.get(0)).thenReturn(innerRow);
            when(innerRow.size()).thenReturn(3);  // More than 2 columns
            when(innerRow.get(0)).thenReturn("myKey");
            when(innerRow.get(1)).thenReturn("value1");
            when(innerRow.get(2)).thenReturn("value2");

            synapseUtilsMock.when(() -> SynapseUtils.cleanupJsonString("[[\"myKey\",\"value1\",\"value2\"]]"))
                    .thenReturn("[[\"myKey\",\"value1\",\"value2\"]]");
            jsonUtilsMock.when(() -> JsonUtils.parse("[[\"myKey\",\"value1\",\"value2\"]]"))
                    .thenReturn(array2D);

            BMap resultMap = mock(BMap.class);
            BMap recordValue = mock(BMap.class);
            valueCreatorMock.when(ValueCreator::createMapValue).thenReturn(resultMap, recordValue);

            when(context.getProperty("mapRecordFields0")).thenReturn("fieldA,fieldB");

            BMap result = DataTransformer.getMapParameter("[[\"myKey\",\"value1\",\"value2\"]]", context, "param0");
            Assert.assertSame(result, resultMap);
        }
    }

    @Test
    public void testGetMapParameter_TableArrayWithMultipleFields() {
        try (MockedStatic<JsonUtils> jsonUtilsMock = Mockito.mockStatic(JsonUtils.class);
             MockedStatic<ValueCreator> valueCreatorMock = Mockito.mockStatic(ValueCreator.class);
             MockedStatic<StringUtils> stringUtilsMock = Mockito.mockStatic(StringUtils.class);
             MockedStatic<SynapseUtils> synapseUtilsMock = Mockito.mockStatic(SynapseUtils.class)) {

            MessageContext context = mock(MessageContext.class);
            BArray tableArray = mock(BArray.class);
            BMap row = mock(BMap.class);

            BString keyFieldName = mock(BString.class);
            BString valueFieldName = mock(BString.class);
            BString otherFieldName = mock(BString.class);
            stringUtilsMock.when(() -> StringUtils.fromString("key")).thenReturn(keyFieldName);
            stringUtilsMock.when(() -> StringUtils.fromString("value")).thenReturn(valueFieldName);
            stringUtilsMock.when(() -> StringUtils.fromString("otherField")).thenReturn(otherFieldName);

            BString keyValue = mock(BString.class);
            when(keyValue.getValue()).thenReturn("testKey");

            when(tableArray.size()).thenReturn(1);
            when(tableArray.get(0)).thenReturn(row);
            when(row.containsKey(keyFieldName)).thenReturn(true);
            when(row.containsKey(valueFieldName)).thenReturn(false);  // Not simple key-value
            when(row.size()).thenReturn(3);  // key + 2 other fields
            when(row.get(keyFieldName)).thenReturn(keyValue);

            Object[] keys = { keyFieldName, valueFieldName, otherFieldName };
            when(row.getKeys()).thenReturn(keys);
            when(row.get(valueFieldName)).thenReturn("val1");
            when(row.get(otherFieldName)).thenReturn("val2");

            when(keyFieldName.getValue()).thenReturn("key");
            when(valueFieldName.getValue()).thenReturn("value");
            when(otherFieldName.getValue()).thenReturn("otherField");

            synapseUtilsMock.when(() -> SynapseUtils.cleanupJsonString(anyString()))
                    .thenAnswer(i -> i.getArgument(0));
            jsonUtilsMock.when(() -> JsonUtils.parse(anyString()))
                    .thenReturn(tableArray);

            BMap resultMap = mock(BMap.class);
            BMap recordValue = mock(BMap.class);
            valueCreatorMock.when(ValueCreator::createMapValue).thenReturn(resultMap, recordValue);

            BMap result = DataTransformer.getMapParameter("{}", context, "param0");
            Assert.assertSame(result, resultMap);
        }
    }

    @Test
    public void testGetMapParameter_2DArrayWithEmptyRow() {
        try (MockedStatic<JsonUtils> jsonUtilsMock = Mockito.mockStatic(JsonUtils.class);
             MockedStatic<ValueCreator> valueCreatorMock = Mockito.mockStatic(ValueCreator.class);
             MockedStatic<StringUtils> stringUtilsMock = Mockito.mockStatic(StringUtils.class);
             MockedStatic<SynapseUtils> synapseUtilsMock = Mockito.mockStatic(SynapseUtils.class)) {

            MessageContext context = mock(MessageContext.class);
            BArray array2D = mock(BArray.class);
            BArray emptyRow = mock(BArray.class);

            stringUtilsMock.when(() -> StringUtils.fromString("key")).thenReturn(mock(BString.class));

            when(array2D.size()).thenReturn(1);
            when(array2D.get(0)).thenReturn(emptyRow);
            when(emptyRow.size()).thenReturn(0);  // Empty row

            synapseUtilsMock.when(() -> SynapseUtils.cleanupJsonString("[[]]"))
                    .thenReturn("[[]]");
            jsonUtilsMock.when(() -> JsonUtils.parse("[[]]"))
                    .thenReturn(array2D);

            BMap resultMap = mock(BMap.class);
            valueCreatorMock.when(ValueCreator::createMapValue).thenReturn(resultMap);

            when(context.getProperty("mapRecordFields0")).thenReturn(null);

            BMap result = DataTransformer.getMapParameter("[[]]", context, "param0");
            Assert.assertSame(result, resultMap);
        }
    }

    @Test
    public void testReconstructRecordFromFields_UnionMemberTypeSkipped() {
        try (MockedStatic<SynapseUtils> synapseUtilsMock = Mockito.mockStatic(SynapseUtils.class);
             MockedStatic<JsonUtils> jsonUtilsMock = Mockito.mockStatic(JsonUtils.class)) {

            MessageContext context = mock(MessageContext.class);
            String prefix = "testPrefix";

            // First pass for union type detection
            when(context.getProperty(prefix + "_param0")).thenReturn("data");
            when(context.getProperty(prefix + "_paramType0")).thenReturn(Constants.UNION);
            when(context.getProperty(prefix + "_dataType0")).thenReturn("dataDataType");
            synapseUtilsMock.when(() -> SynapseUtils.lookupTemplateParameter(context, "dataDataType"))
                    .thenReturn("stringType");

            // Second pass
            when(context.getProperty(prefix + "_unionMember0")).thenReturn("intType");  // Different from selected type

            when(context.getProperty(prefix + "_param1")).thenReturn(null);

            synapseUtilsMock.when(() -> SynapseUtils.findParentUnionPath("data", java.util.Set.of("data")))
                    .thenReturn("data");
            synapseUtilsMock.when(() -> SynapseUtils.lookupTemplateParameter(context, "data"))
                    .thenReturn(null);

            Object expectedBallerinaRecord = new Object();
            jsonUtilsMock.when(() -> JsonUtils.parse(anyString())).thenReturn(expectedBallerinaRecord);

            Object result = DataTransformer.reconstructRecordFromFields(prefix, context);
            Assert.assertSame(result, expectedBallerinaRecord);
        }
    }

    @Test
    public void testReconstructRecordFromFields_SynapseExpressionResolution() {
        try (MockedStatic<SynapseUtils> synapseUtilsMock = Mockito.mockStatic(SynapseUtils.class);
             MockedStatic<JsonUtils> jsonUtilsMock = Mockito.mockStatic(JsonUtils.class)) {

            MessageContext context = mock(MessageContext.class);
            String prefix = "testPrefix";

            when(context.getProperty(prefix + "_param0")).thenReturn("dynamicField");
            when(context.getProperty(prefix + "_paramType0")).thenReturn(Constants.STRING);
            synapseUtilsMock.when(() -> SynapseUtils.lookupTemplateParameter(context, "dynamicField"))
                    .thenReturn("${payload.value}");
            synapseUtilsMock.when(() -> SynapseUtils.resolveSynapseExpressions("${payload.value}", context))
                    .thenReturn("resolvedValue");

            when(context.getProperty(prefix + "_param1")).thenReturn(null);

            Object expectedBallerinaRecord = new Object();
            jsonUtilsMock.when(() -> JsonUtils.parse(anyString())).thenReturn(expectedBallerinaRecord);

            Object result = DataTransformer.reconstructRecordFromFields(prefix, context);
            Assert.assertSame(result, expectedBallerinaRecord);
        }
    }

    @Test
    public void testReconstructRecordFromFields_NullFieldValue() {
        try (MockedStatic<SynapseUtils> synapseUtilsMock = Mockito.mockStatic(SynapseUtils.class);
             MockedStatic<JsonUtils> jsonUtilsMock = Mockito.mockStatic(JsonUtils.class)) {

            MessageContext context = mock(MessageContext.class);
            String prefix = "testPrefix";

            when(context.getProperty(prefix + "_param0")).thenReturn("nullField");
            when(context.getProperty(prefix + "_paramType0")).thenReturn(Constants.STRING);
            synapseUtilsMock.when(() -> SynapseUtils.lookupTemplateParameter(context, "nullField"))
                    .thenReturn(null);  // Null field value

            when(context.getProperty(prefix + "_param1")).thenReturn(null);

            Object expectedBallerinaRecord = new Object();
            jsonUtilsMock.when(() -> JsonUtils.parse(anyString())).thenReturn(expectedBallerinaRecord);

            Object result = DataTransformer.reconstructRecordFromFields(prefix, context);
            Assert.assertSame(result, expectedBallerinaRecord);
        }
    }

    @Test
    public void testReconstructRecordFromFields_UnionFieldWithSelectedType() {
        try (MockedStatic<SynapseUtils> synapseUtilsMock = Mockito.mockStatic(SynapseUtils.class);
             MockedStatic<JsonUtils> jsonUtilsMock = Mockito.mockStatic(JsonUtils.class)) {

            MessageContext context = mock(MessageContext.class);
            String prefix = "testPrefix";

            // Union field with matching selected type
            when(context.getProperty(prefix + "_param0")).thenReturn("unionField");
            when(context.getProperty(prefix + "_paramType0")).thenReturn(Constants.UNION);
            when(context.getProperty(prefix + "_dataType0")).thenReturn("unionFieldDataType");
            synapseUtilsMock.when(() -> SynapseUtils.lookupTemplateParameter(context, "unionFieldDataType"))
                    .thenReturn("stringType");

            when(context.getProperty(prefix + "_unionMember0")).thenReturn(null);
            synapseUtilsMock.when(() -> SynapseUtils.lookupTemplateParameter(context, "unionField"))
                    .thenReturn("{\"nested\":true}");
            synapseUtilsMock.when(() -> SynapseUtils.cleanupJsonString("{\"nested\":true}"))
                    .thenReturn("{\"nested\":true}");

            when(context.getProperty(prefix + "_param1")).thenReturn(null);

            Object expectedBallerinaRecord = new Object();
            jsonUtilsMock.when(() -> JsonUtils.parse(anyString())).thenReturn(expectedBallerinaRecord);

            Object result = DataTransformer.reconstructRecordFromFields(prefix, context);
            Assert.assertSame(result, expectedBallerinaRecord);
        }
    }

    @Test
    public void testReconstructRecordFromFields_NonUnionWithUnionMember() {
        try (MockedStatic<SynapseUtils> synapseUtilsMock = Mockito.mockStatic(SynapseUtils.class);
             MockedStatic<JsonUtils> jsonUtilsMock = Mockito.mockStatic(JsonUtils.class)) {

            MessageContext context = mock(MessageContext.class);
            String prefix = "testPrefix";

            // Non-union field but has a union member marker
            when(context.getProperty(prefix + "_param0")).thenReturn("childField");
            when(context.getProperty(prefix + "_paramType0")).thenReturn(Constants.STRING);
            when(context.getProperty(prefix + "_unionMember0")).thenReturn("stringType");

            // Parent union path exists and matches
            synapseUtilsMock.when(() -> SynapseUtils.findParentUnionPath(anyString(), any()))
                    .thenReturn(null);  // No parent union path
            synapseUtilsMock.when(() -> SynapseUtils.lookupTemplateParameter(context, "childField"))
                    .thenReturn("testValue");

            when(context.getProperty(prefix + "_param1")).thenReturn(null);

            Object expectedBallerinaRecord = new Object();
            jsonUtilsMock.when(() -> JsonUtils.parse(anyString())).thenReturn(expectedBallerinaRecord);

            Object result = DataTransformer.reconstructRecordFromFields(prefix, context);
            Assert.assertSame(result, expectedBallerinaRecord);
        }
    }

    @Test
    public void testConvertValueToType_RecordTypeWithNestedArray() {
        try (MockedStatic<ValueCreator> valueCreatorMock = Mockito.mockStatic(ValueCreator.class);
             MockedStatic<StringUtils> stringUtilsMock = Mockito.mockStatic(StringUtils.class)) {

            BMap<BString, Object> sourceMap = mock(BMap.class);
            StructureType targetType = mock(StructureType.class);
            Field field = mock(Field.class);

            io.ballerina.runtime.api.Module module = mock(io.ballerina.runtime.api.Module.class);
            when(targetType.getPackage()).thenReturn(module);
            when(targetType.getName()).thenReturn("TestRecord");
            when(targetType.getTag()).thenReturn(TypeTags.RECORD_TYPE_TAG);

            Map<String, Field> fields = new HashMap<>();
            fields.put("items", field);
            when(targetType.getFields()).thenReturn(fields);
            when(field.getFieldName()).thenReturn("items");

            ArrayType arrayType = mock(ArrayType.class);
            Type elementType = mock(Type.class);
            when(arrayType.getTag()).thenReturn(TypeTags.ARRAY_TAG);
            when(arrayType.getElementType()).thenReturn(elementType);
            when(elementType.getTag()).thenReturn(TypeTags.STRING_TAG);
            when(field.getFieldType()).thenReturn(arrayType);

            BString bFieldName = mock(BString.class);
            BArray sourceArray = mock(BArray.class);
            when(sourceArray.size()).thenReturn(0);
            stringUtilsMock.when(() -> StringUtils.fromString("items")).thenReturn(bFieldName);
            when(sourceMap.containsKey(bFieldName)).thenReturn(true);
            when(sourceMap.get(bFieldName)).thenReturn(sourceArray);

            BMap<BString, Object> typedRecord = mock(BMap.class);
            valueCreatorMock.when(() -> ValueCreator.createRecordValue(module, "TestRecord"))
                    .thenReturn(typedRecord);

            Object result = DataTransformer.convertValueToType(sourceMap, targetType);
            Assert.assertSame(result, typedRecord);
        }
    }

    @Test
    public void testConvertValueToType_RecordTypeWithMissingField() {
        try (MockedStatic<ValueCreator> valueCreatorMock = Mockito.mockStatic(ValueCreator.class);
             MockedStatic<StringUtils> stringUtilsMock = Mockito.mockStatic(StringUtils.class)) {

            BMap<BString, Object> sourceMap = mock(BMap.class);
            StructureType targetType = mock(StructureType.class);
            Field field = mock(Field.class);

            io.ballerina.runtime.api.Module module = mock(io.ballerina.runtime.api.Module.class);
            when(targetType.getPackage()).thenReturn(module);
            when(targetType.getName()).thenReturn("TestRecord");
            when(targetType.getTag()).thenReturn(TypeTags.RECORD_TYPE_TAG);

            Map<String, Field> fields = new HashMap<>();
            fields.put("name", field);
            when(targetType.getFields()).thenReturn(fields);
            when(field.getFieldName()).thenReturn("name");

            Type fieldType = mock(Type.class);
            when(fieldType.getTag()).thenReturn(TypeTags.STRING_TAG);
            when(field.getFieldType()).thenReturn(fieldType);

            BString bFieldName = mock(BString.class);
            stringUtilsMock.when(() -> StringUtils.fromString("name")).thenReturn(bFieldName);
            when(sourceMap.containsKey(bFieldName)).thenReturn(false);  // Field not present

            BMap<BString, Object> typedRecord = mock(BMap.class);
            valueCreatorMock.when(() -> ValueCreator.createRecordValue(module, "TestRecord"))
                    .thenReturn(typedRecord);

            Object result = DataTransformer.convertValueToType(sourceMap, targetType);
            Assert.assertSame(result, typedRecord);
        }
    }

    @Test
    public void testCreateTypedArrayFromGeneric_WithConversion() {
        try (MockedStatic<ValueCreator> valueCreatorMock = Mockito.mockStatic(ValueCreator.class);
             MockedStatic<StringUtils> stringUtilsMock = Mockito.mockStatic(StringUtils.class)) {

            BArray sourceArray = mock(BArray.class);
            ArrayType targetType = mock(ArrayType.class);

            StructureType elementType = mock(StructureType.class);
            when(elementType.getTag()).thenReturn(TypeTags.RECORD_TYPE_TAG);
            when(targetType.getTag()).thenReturn(TypeTags.ARRAY_TAG);
            when(targetType.getElementType()).thenReturn(elementType);

            io.ballerina.runtime.api.Module module = mock(io.ballerina.runtime.api.Module.class);
            when(elementType.getPackage()).thenReturn(module);
            when(elementType.getName()).thenReturn("ItemRecord");
            when(elementType.getFields()).thenReturn(new HashMap<>());

            BMap sourceElement = mock(BMap.class);
            when(sourceArray.size()).thenReturn(1);
            when(sourceArray.get(0)).thenReturn(sourceElement);

            BMap<BString, Object> typedElement = mock(BMap.class);
            valueCreatorMock.when(() -> ValueCreator.createRecordValue(module, "ItemRecord"))
                    .thenReturn(typedElement);

            BArray result = DataTransformer.createTypedArrayFromGeneric(sourceArray, targetType);
            Assert.assertSame(result, sourceArray);
        }
    }

    @Test
    public void testGetMapParameter_TableArrayWithBStringKey() {
        try (MockedStatic<JsonUtils> jsonUtilsMock = Mockito.mockStatic(JsonUtils.class);
             MockedStatic<ValueCreator> valueCreatorMock = Mockito.mockStatic(ValueCreator.class);
             MockedStatic<StringUtils> stringUtilsMock = Mockito.mockStatic(StringUtils.class);
             MockedStatic<SynapseUtils> synapseUtilsMock = Mockito.mockStatic(SynapseUtils.class)) {

            MessageContext context = mock(MessageContext.class);
            BArray tableArray = mock(BArray.class);
            BMap row = mock(BMap.class);

            BString keyFieldName = mock(BString.class);
            BString valueFieldName = mock(BString.class);
            stringUtilsMock.when(() -> StringUtils.fromString("key")).thenReturn(keyFieldName);
            stringUtilsMock.when(() -> StringUtils.fromString("value")).thenReturn(valueFieldName);
            stringUtilsMock.when(() -> StringUtils.fromString("actualKey")).thenReturn(mock(BString.class));

            BString keyBString = mock(BString.class);
            when(keyBString.getValue()).thenReturn("actualKey");

            when(tableArray.size()).thenReturn(1);
            when(tableArray.get(0)).thenReturn(row);
            when(row.containsKey(keyFieldName)).thenReturn(true);
            when(row.containsKey(valueFieldName)).thenReturn(true);
            when(row.size()).thenReturn(2);
            when(row.get(keyFieldName)).thenReturn(keyBString);  // BString key
            when(row.get(valueFieldName)).thenReturn("myValue");

            Object[] keys = { keyFieldName, valueFieldName };
            when(row.getKeys()).thenReturn(keys);

            synapseUtilsMock.when(() -> SynapseUtils.cleanupJsonString(anyString()))
                    .thenAnswer(i -> i.getArgument(0));
            jsonUtilsMock.when(() -> JsonUtils.parse(anyString()))
                    .thenReturn(tableArray);

            BMap resultMap = mock(BMap.class);
            valueCreatorMock.when(ValueCreator::createMapValue).thenReturn(resultMap);

            BMap result = DataTransformer.getMapParameter("{}", context, "param0");
            Assert.assertSame(result, resultMap);
        }
    }

    @Test
    public void testGetMapParameter_2DArrayWithBStringKey() {
        try (MockedStatic<JsonUtils> jsonUtilsMock = Mockito.mockStatic(JsonUtils.class);
             MockedStatic<ValueCreator> valueCreatorMock = Mockito.mockStatic(ValueCreator.class);
             MockedStatic<StringUtils> stringUtilsMock = Mockito.mockStatic(StringUtils.class);
             MockedStatic<SynapseUtils> synapseUtilsMock = Mockito.mockStatic(SynapseUtils.class)) {

            MessageContext context = mock(MessageContext.class);
            BArray array2D = mock(BArray.class);
            BArray innerRow = mock(BArray.class);
            BString keyBString = mock(BString.class);

            stringUtilsMock.when(() -> StringUtils.fromString("key")).thenReturn(mock(BString.class));
            stringUtilsMock.when(() -> StringUtils.fromString("bstringKey")).thenReturn(mock(BString.class));

            when(keyBString.getValue()).thenReturn("bstringKey");

            when(array2D.size()).thenReturn(1);
            when(array2D.get(0)).thenReturn(innerRow);
            when(innerRow.size()).thenReturn(2);
            when(innerRow.get(0)).thenReturn(keyBString);  // BString key
            when(innerRow.get(1)).thenReturn("myValue");

            synapseUtilsMock.when(() -> SynapseUtils.cleanupJsonString(anyString()))
                    .thenAnswer(i -> i.getArgument(0));
            jsonUtilsMock.when(() -> JsonUtils.parse(anyString()))
                    .thenReturn(array2D);

            BMap resultMap = mock(BMap.class);
            valueCreatorMock.when(ValueCreator::createMapValue).thenReturn(resultMap);

            when(context.getProperty("mapRecordFields0")).thenReturn(null);

            BMap result = DataTransformer.getMapParameter("[[\"key\",\"value\"]]", context, "param0");
            Assert.assertSame(result, resultMap);
        }
    }

    @Test(expectedExceptions = SynapseException.class)
    public void testCreateRecordValue_NullJson_MissingRecordName() {
        try (MockedStatic<JsonUtils> jsonUtilsMock = Mockito.mockStatic(JsonUtils.class)) {
            MessageContext context = mock(MessageContext.class);
            // No flattened fields; reconstruction still calls JsonUtils.parse("{}")
            jsonUtilsMock.when(() -> JsonUtils.parse(anyString())).thenReturn(new Object());
            when(context.getProperty("param0_recordName")).thenReturn(null);

            DataTransformer.createRecordValue(null, "param0", context, 0);
        }
    }

    @Test(expectedExceptions = SynapseException.class)
    public void testCreateRecordValue_InvalidJson_ThrowsSynapseException() {
        try (MockedStatic<JsonUtils> jsonUtilsMock = Mockito.mockStatic(JsonUtils.class)) {
            MessageContext context = mock(MessageContext.class);
            when(context.getProperty("param0_recordName")).thenReturn(null);
            jsonUtilsMock.when(() -> JsonUtils.parse("{invalid-json"))
                    .thenThrow(new RuntimeException("invalid json"));

            DataTransformer.createRecordValue("{invalid-json", "param0", context, 0);
        }
    }
}
