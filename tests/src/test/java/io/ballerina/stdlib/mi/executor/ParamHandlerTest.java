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
import io.ballerina.runtime.api.utils.JsonUtils;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BDecimal;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.runtime.api.values.BXml;
import io.ballerina.stdlib.mi.Constants;
import io.ballerina.stdlib.mi.utils.SynapseUtils;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ParamHandlerTest {

    @Test
    public void testSetParameters() {
        try (MockedStatic<SynapseUtils> synapseUtilsMock = Mockito.mockStatic(SynapseUtils.class);
             MockedStatic<StringUtils> stringUtilsMock = Mockito.mockStatic(StringUtils.class)) {

            MessageContext context = mock(MessageContext.class);
            Object[] args = new Object[2];

            // Mock param0 (String)
            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "param0")).thenReturn("p0Name");
            synapseUtilsMock.when(() -> SynapseUtils.lookupTemplateParameter(context, "p0Name")).thenReturn("value0");
            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "paramType0")).thenReturn(Constants.STRING);

            BString bStringVal = mock(BString.class);
            stringUtilsMock.when(() -> StringUtils.fromString("value0")).thenReturn(bStringVal);

            // Mock param1 (Int)
            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "param1")).thenReturn("p1Name");
            synapseUtilsMock.when(() -> SynapseUtils.lookupTemplateParameter(context, "p1Name")).thenReturn("123");
            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "paramType1")).thenReturn(Constants.INT);

            ParamHandler handler = new ParamHandler();
            handler.setParameters(args, context);

            Assert.assertEquals(args[0], bStringVal);
            Assert.assertEquals(args[1], 123L);
        }
    }

    @Test
    public void testGetParameterTypes() {
       try (MockedStatic<SynapseUtils> synapseUtilsMock = Mockito.mockStatic(SynapseUtils.class);
            MockedStatic<StringUtils> stringUtilsMock = Mockito.mockStatic(StringUtils.class);
            MockedStatic<DataTransformer> dataTransformerMock = Mockito.mockStatic(DataTransformer.class)) {

            MessageContext context = mock(MessageContext.class);
            ParamHandler handler = new ParamHandler();

            // Test BOOLEAN
            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "paramBool")).thenReturn("boolName");
            synapseUtilsMock.when(() -> SynapseUtils.lookupTemplateParameter(context, "boolName")).thenReturn("true");
            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "typeBool")).thenReturn(Constants.BOOLEAN);

            Object resultBool = handler.getParameter(context, "paramBool", "typeBool", 0);
            Assert.assertEquals(resultBool, true);

            // Test FLOAT
            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "paramFloat")).thenReturn("floatName");
            synapseUtilsMock.when(() -> SynapseUtils.lookupTemplateParameter(context, "floatName")).thenReturn("10.5");
            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "typeFloat")).thenReturn(Constants.FLOAT);

            Object resultFloat = handler.getParameter(context, "paramFloat", "typeFloat", 0);
            Assert.assertEquals(resultFloat, 10.5);

            // Test JSON (Delegates to DataTransformer)
            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "paramJson")).thenReturn("jsonName");
            synapseUtilsMock.when(() -> SynapseUtils.lookupTemplateParameter(context, "jsonName")).thenReturn("{}");
            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "typeJson")).thenReturn(Constants.JSON);

            Object mockJson = new Object();
            dataTransformerMock.when(() -> DataTransformer.getJsonParameter(any())).thenReturn(mockJson);

            Object resultJson = handler.getParameter(context, "paramJson", "typeJson", 0);
            Assert.assertSame(resultJson, mockJson);
       }
    }

    @Test
    public void testPrependPathParams() {
        try (MockedStatic<SynapseUtils> synapseUtilsMock = Mockito.mockStatic(SynapseUtils.class);
             MockedStatic<StringUtils> stringUtilsMock = Mockito.mockStatic(StringUtils.class)) {

            MessageContext context = mock(MessageContext.class);
            ParamHandler handler = new ParamHandler();
            Object[] args = new Object[] { "arg1" };

            // PATH_PARAM_SIZE = 2
            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, Constants.PATH_PARAM_SIZE)).thenReturn("2");

            // Path Param 0: int 100
            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "pathParam0")).thenReturn("p0");
            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "pathParamType0")).thenReturn(Constants.INT);
            synapseUtilsMock.when(() -> SynapseUtils.lookupTemplateParameter(context, "p0")).thenReturn("100");

            // Path Param 1: string "hello"
            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "pathParam1")).thenReturn("p1");
            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "pathParamType1")).thenReturn(Constants.STRING);
            synapseUtilsMock.when(() -> SynapseUtils.lookupTemplateParameter(context, "p1")).thenReturn("hello");
            BString helloBStr = mock(BString.class);
            stringUtilsMock.when(() -> StringUtils.fromString("hello")).thenReturn(helloBStr);

            Object[] combined = handler.prependPathParams(args, context);

            Assert.assertEquals(combined.length, 3);
            Assert.assertEquals(combined[0], 100L);
            Assert.assertEquals(combined[1], helloBStr);
            Assert.assertEquals(combined[2], "arg1");
        }
    }

    @Test
    public void testConvertPathParam_IntType() {
        ParamHandler handler = new ParamHandler();
        Object result = handler.convertPathParam("42", Constants.INT);
        Assert.assertEquals(result, 42L);
    }

    @Test
    public void testConvertPathParam_FloatType() {
        ParamHandler handler = new ParamHandler();
        Object result = handler.convertPathParam("3.14", Constants.FLOAT);
        Assert.assertEquals(result, 3.14);
    }

    @Test
    public void testConvertPathParam_BooleanType() {
        ParamHandler handler = new ParamHandler();
        Object result = handler.convertPathParam("true", Constants.BOOLEAN);
        Assert.assertEquals(result, true);
    }

    @Test
    public void testConvertPathParam_DecimalType() {
        try (MockedStatic<ValueCreator> valueCreatorMock = Mockito.mockStatic(ValueCreator.class)) {
            BDecimal mockDecimal = mock(BDecimal.class);
            valueCreatorMock.when(() -> ValueCreator.createDecimalValue("123.45"))
                    .thenReturn(mockDecimal);

            ParamHandler handler = new ParamHandler();
            Object result = handler.convertPathParam("123.45", Constants.DECIMAL);
            Assert.assertEquals(result, mockDecimal);
        }
    }

    @Test
    public void testConvertPathParam_NullType() {
        try (MockedStatic<StringUtils> stringUtilsMock = Mockito.mockStatic(StringUtils.class)) {
            BString mockBString = mock(BString.class);
            stringUtilsMock.when(() -> StringUtils.fromString("test")).thenReturn(mockBString);

            ParamHandler handler = new ParamHandler();
            Object result = handler.convertPathParam("test", null);
            Assert.assertEquals(result, mockBString);
        }
    }

    @Test
    public void testConvertPathParam_DefaultType() {
        try (MockedStatic<StringUtils> stringUtilsMock = Mockito.mockStatic(StringUtils.class)) {
            BString mockBString = mock(BString.class);
            stringUtilsMock.when(() -> StringUtils.fromString("value")).thenReturn(mockBString);

            ParamHandler handler = new ParamHandler();
            Object result = handler.convertPathParam("value", "unknownType");
            Assert.assertEquals(result, mockBString);
        }
    }

    @Test(expectedExceptions = SynapseException.class)
    public void testConvertPathParam_InvalidIntValue() {
        ParamHandler handler = new ParamHandler();
        handler.convertPathParam("not_a_number", Constants.INT);
    }

    @Test(expectedExceptions = SynapseException.class)
    public void testConvertPathParam_InvalidFloatValue() {
        ParamHandler handler = new ParamHandler();
        handler.convertPathParam("not_a_float", Constants.FLOAT);
    }

    @Test
    public void testPrependPathParams_NullPathParamSize() {
        try (MockedStatic<SynapseUtils> synapseUtilsMock = Mockito.mockStatic(SynapseUtils.class)) {
            MessageContext context = mock(MessageContext.class);
            ParamHandler handler = new ParamHandler();
            Object[] args = new Object[] { "arg1", "arg2" };

            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, Constants.PATH_PARAM_SIZE))
                    .thenReturn(null);

            Object[] combined = handler.prependPathParams(args, context);

            Assert.assertSame(combined, args);
            Assert.assertEquals(combined.length, 2);
        }
    }

    @Test
    public void testPrependPathParams_ZeroPathParamSize() {
        try (MockedStatic<SynapseUtils> synapseUtilsMock = Mockito.mockStatic(SynapseUtils.class)) {
            MessageContext context = mock(MessageContext.class);
            ParamHandler handler = new ParamHandler();
            Object[] args = new Object[] { "arg1" };

            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, Constants.PATH_PARAM_SIZE))
                    .thenReturn("0");

            Object[] combined = handler.prependPathParams(args, context);

            Assert.assertSame(combined, args);
        }
    }

    @Test
    public void testPrependPathParams_InvalidPathParamSize() {
        try (MockedStatic<SynapseUtils> synapseUtilsMock = Mockito.mockStatic(SynapseUtils.class)) {
            MessageContext context = mock(MessageContext.class);
            ParamHandler handler = new ParamHandler();
            Object[] args = new Object[] { "arg1" };

            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, Constants.PATH_PARAM_SIZE))
                    .thenReturn("invalid");

            Object[] combined = handler.prependPathParams(args, context);

            // Should default to 0 path params and return original args
            Assert.assertSame(combined, args);
        }
    }

    @Test
    public void testPrependPathParams_NullPathParamValue() {
        try (MockedStatic<SynapseUtils> synapseUtilsMock = Mockito.mockStatic(SynapseUtils.class)) {
            MessageContext context = mock(MessageContext.class);
            ParamHandler handler = new ParamHandler();
            Object[] args = new Object[] { "arg1" };

            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, Constants.PATH_PARAM_SIZE))
                    .thenReturn("1");
            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "pathParam0"))
                    .thenReturn("p0");
            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "pathParamType0"))
                    .thenReturn(Constants.STRING);
            synapseUtilsMock.when(() -> SynapseUtils.lookupTemplateParameter(context, "p0"))
                    .thenReturn(null);

            Object[] combined = handler.prependPathParams(args, context);

            Assert.assertEquals(combined.length, 2);
            Assert.assertNull(combined[0]);
            Assert.assertEquals(combined[1], "arg1");
        }
    }

    @Test(expectedExceptions = SynapseException.class)
    public void testGetParameter_NullParamName() {
        try (MockedStatic<SynapseUtils> synapseUtilsMock = Mockito.mockStatic(SynapseUtils.class)) {
            MessageContext context = mock(MessageContext.class);
            ParamHandler handler = new ParamHandler();

            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "param0"))
                    .thenReturn(null);

            handler.getParameter(context, "param0", "paramType0", 0);
        }
    }

    @Test
    public void testGetParameter_NullParamType() {
        try (MockedStatic<SynapseUtils> synapseUtilsMock = Mockito.mockStatic(SynapseUtils.class);
             MockedStatic<StringUtils> stringUtilsMock = Mockito.mockStatic(StringUtils.class)) {

            MessageContext context = mock(MessageContext.class);
            ParamHandler handler = new ParamHandler();

            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "param0"))
                    .thenReturn("myParam");
            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "paramType0"))
                    .thenReturn(null); // null type defaults to STRING
            synapseUtilsMock.when(() -> SynapseUtils.lookupTemplateParameter(context, "myParam"))
                    .thenReturn("value");

            BString mockBString = mock(BString.class);
            stringUtilsMock.when(() -> StringUtils.fromString("value")).thenReturn(mockBString);

            Object result = handler.getParameter(context, "param0", "paramType0", 0);
            Assert.assertEquals(result, mockBString);
        }
    }

    @Test
    public void testGetParameter_NullParamValue_Union() {
        try (MockedStatic<SynapseUtils> synapseUtilsMock = Mockito.mockStatic(SynapseUtils.class)) {
            MessageContext context = mock(MessageContext.class);
            ParamHandler handler = new ParamHandler();

            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "param0"))
                    .thenReturn("unionParam");
            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "paramType0"))
                    .thenReturn(Constants.UNION);
            synapseUtilsMock.when(() -> SynapseUtils.lookupTemplateParameter(context, "unionParam"))
                    .thenReturn(null);
            synapseUtilsMock.when(() -> SynapseUtils.lookupTemplateParameter(context, "unionParamDataType"))
                    .thenReturn(null);

            Object result = handler.getParameter(context, "param0", "paramType0", 0);
            Assert.assertNull(result);
        }
    }

    @Test
    public void testGetParameter_NullParamValue_Record() {
        try (MockedStatic<SynapseUtils> synapseUtilsMock = Mockito.mockStatic(SynapseUtils.class);
             MockedStatic<DataTransformer> dataTransformerMock = Mockito.mockStatic(DataTransformer.class)) {

            MessageContext context = mock(MessageContext.class);
            ParamHandler handler = new ParamHandler();

            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "param0"))
                    .thenReturn("recordParam");
            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "paramType0"))
                    .thenReturn(Constants.RECORD);
            synapseUtilsMock.when(() -> SynapseUtils.lookupTemplateParameter(context, "recordParam"))
                    .thenReturn(null);

            Object mockRecord = new Object();
            dataTransformerMock.when(() -> DataTransformer.createRecordValue(null, "recordParam", context, 0))
                    .thenReturn(mockRecord);

            Object result = handler.getParameter(context, "param0", "paramType0", 0);
            Assert.assertEquals(result, mockRecord);
        }
    }

    @Test
    public void testGetParameter_DecimalType() {
        try (MockedStatic<SynapseUtils> synapseUtilsMock = Mockito.mockStatic(SynapseUtils.class);
             MockedStatic<ValueCreator> valueCreatorMock = Mockito.mockStatic(ValueCreator.class)) {

            MessageContext context = mock(MessageContext.class);
            ParamHandler handler = new ParamHandler();

            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "param0"))
                    .thenReturn("decimalParam");
            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "paramType0"))
                    .thenReturn(Constants.DECIMAL);
            synapseUtilsMock.when(() -> SynapseUtils.lookupTemplateParameter(context, "decimalParam"))
                    .thenReturn("123.456");

            BDecimal mockDecimal = mock(BDecimal.class);
            valueCreatorMock.when(() -> ValueCreator.createDecimalValue("123.456"))
                    .thenReturn(mockDecimal);

            Object result = handler.getParameter(context, "param0", "paramType0", 0);
            Assert.assertEquals(result, mockDecimal);
        }
    }

    @Test
    public void testGetParameter_MapType() {
        try (MockedStatic<SynapseUtils> synapseUtilsMock = Mockito.mockStatic(SynapseUtils.class);
             MockedStatic<DataTransformer> dataTransformerMock = Mockito.mockStatic(DataTransformer.class)) {

            MessageContext context = mock(MessageContext.class);
            ParamHandler handler = new ParamHandler();

            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "param0"))
                    .thenReturn("mapParam");
            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "paramType0"))
                    .thenReturn(Constants.MAP);
            synapseUtilsMock.when(() -> SynapseUtils.lookupTemplateParameter(context, "mapParam"))
                    .thenReturn("{\"key\":\"value\"}");

            Object mockMap = mock(BMap.class);
            dataTransformerMock.when(() -> DataTransformer.getMapParameter(any(), eq(context), eq("param0")))
                    .thenReturn(mockMap);

            Object result = handler.getParameter(context, "param0", "paramType0", 0);
            Assert.assertEquals(result, mockMap);
        }
    }

    @Test
    public void testGetParameter_UnionTypeWithDataType() {
        try (MockedStatic<SynapseUtils> synapseUtilsMock = Mockito.mockStatic(SynapseUtils.class);
             MockedStatic<StringUtils> stringUtilsMock = Mockito.mockStatic(StringUtils.class)) {

            MessageContext context = mock(MessageContext.class);
            ParamHandler handler = new ParamHandler();

            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "param0"))
                    .thenReturn("unionParam");
            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "paramType0"))
                    .thenReturn(Constants.UNION);
            synapseUtilsMock.when(() -> SynapseUtils.lookupTemplateParameter(context, "unionParam"))
                    .thenReturn("someValue");

            // For union, should lookup DataType
            synapseUtilsMock.when(() -> SynapseUtils.lookupTemplateParameter(context, "unionParamDataType"))
                    .thenReturn("string");

            // Now it will call getParameter with param0UnionString
            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "param0UnionString"))
                    .thenReturn("unionStringParam");
            synapseUtilsMock.when(() -> SynapseUtils.lookupTemplateParameter(context, "unionStringParam"))
                    .thenReturn("actual value");

            BString mockBString = mock(BString.class);
            stringUtilsMock.when(() -> StringUtils.fromString("actual value")).thenReturn(mockBString);

            Object result = handler.getParameter(context, "param0", "paramType0", 0);
            Assert.assertEquals(result, mockBString);
        }
    }

    @Test
    public void testGetParameter_UnknownType() {
        try (MockedStatic<SynapseUtils> synapseUtilsMock = Mockito.mockStatic(SynapseUtils.class)) {
            MessageContext context = mock(MessageContext.class);
            ParamHandler handler = new ParamHandler();

            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "param0"))
                    .thenReturn("unknownParam");
            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "paramType0"))
                    .thenReturn("unknownType");
            synapseUtilsMock.when(() -> SynapseUtils.lookupTemplateParameter(context, "unknownParam"))
                    .thenReturn("value");

            Object result = handler.getParameter(context, "param0", "paramType0", 0);
            Assert.assertNull(result);
        }
    }

    @Test
    public void testGetParameter_NullParamValueReturnsNull() {
        try (MockedStatic<SynapseUtils> synapseUtilsMock = Mockito.mockStatic(SynapseUtils.class)) {
            MessageContext context = mock(MessageContext.class);
            ParamHandler handler = new ParamHandler();

            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "param0"))
                    .thenReturn("stringParam");
            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "paramType0"))
                    .thenReturn(Constants.STRING);
            synapseUtilsMock.when(() -> SynapseUtils.lookupTemplateParameter(context, "stringParam"))
                    .thenReturn(null);

            Object result = handler.getParameter(context, "param0", "paramType0", 0);
            Assert.assertNull(result);
        }
    }

    @Test
    public void testGetParameter_ArrayType_RecordElementType() {
        try (MockedStatic<SynapseUtils> synapseUtilsMock = Mockito.mockStatic(SynapseUtils.class);
             MockedStatic<JsonUtils> jsonUtilsMock = Mockito.mockStatic(JsonUtils.class)) {

            MessageContext context = mock(MessageContext.class);
            ParamHandler handler = new ParamHandler();

            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "param0"))
                    .thenReturn("arrayParam");
            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "paramType0"))
                    .thenReturn(Constants.ARRAY);
            synapseUtilsMock.when(() -> SynapseUtils.lookupTemplateParameter(context, "arrayParam"))
                    .thenReturn("[{\"name\":\"test\"}]");
            synapseUtilsMock.when(() -> SynapseUtils.cleanupJsonString(any()))
                    .thenReturn("[{\"name\":\"test\"}]");

            when(context.getProperty("arrayElementType0")).thenReturn("record");

            BArray mockArray = mock(BArray.class);
            jsonUtilsMock.when(() -> JsonUtils.parse("[{\"name\":\"test\"}]")).thenReturn(mockArray);

            Object result = handler.getParameter(context, "param0", "paramType0", 0);
            Assert.assertEquals(result, mockArray);
        }
    }

    @Test
    public void testGetParameter_ArrayType_FloatElementType() {
        try (MockedStatic<SynapseUtils> synapseUtilsMock = Mockito.mockStatic(SynapseUtils.class);
             MockedStatic<JsonUtils> jsonUtilsMock = Mockito.mockStatic(JsonUtils.class);
             MockedStatic<ValueCreator> valueCreatorMock = Mockito.mockStatic(ValueCreator.class)) {

            MessageContext context = mock(MessageContext.class);
            ParamHandler handler = new ParamHandler();

            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "param1"))
                    .thenReturn("floatArrayParam");
            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "paramType1"))
                    .thenReturn(Constants.ARRAY);
            synapseUtilsMock.when(() -> SynapseUtils.lookupTemplateParameter(context, "floatArrayParam"))
                    .thenReturn("[1.5, 2.5]");
            synapseUtilsMock.when(() -> SynapseUtils.cleanupJsonString(any()))
                    .thenReturn("[1.5, 2.5]");

            when(context.getProperty("arrayElementType1")).thenReturn("float");

            BArray mockArray = mock(BArray.class);
            when(mockArray.size()).thenReturn(2);
            when(mockArray.get(0)).thenReturn(1.5);
            when(mockArray.get(1)).thenReturn(2.5);
            jsonUtilsMock.when(() -> JsonUtils.parse("[1.5, 2.5]")).thenReturn(mockArray);

            BArray resultArray = mock(BArray.class);
            valueCreatorMock.when(() -> ValueCreator.createArrayValue(any(double[].class)))
                    .thenReturn(resultArray);

            Object result = handler.getParameter(context, "param1", "paramType1", 1);
            Assert.assertNotNull(result);
        }
    }

    @Test
    public void testGetParameter_ArrayType_DefaultSimpleArray() {
        try (MockedStatic<SynapseUtils> synapseUtilsMock = Mockito.mockStatic(SynapseUtils.class);
             MockedStatic<JsonUtils> jsonUtilsMock = Mockito.mockStatic(JsonUtils.class)) {

            MessageContext context = mock(MessageContext.class);
            ParamHandler handler = new ParamHandler();

            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "param2"))
                    .thenReturn("intArrayParam");
            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "paramType2"))
                    .thenReturn(Constants.ARRAY);
            synapseUtilsMock.when(() -> SynapseUtils.lookupTemplateParameter(context, "intArrayParam"))
                    .thenReturn("[1, 2, 3]");
            synapseUtilsMock.when(() -> SynapseUtils.cleanupJsonString(any()))
                    .thenReturn("[1, 2, 3]");

            when(context.getProperty("arrayElementType2")).thenReturn("int");

            BArray mockArray = mock(BArray.class);
            when(mockArray.size()).thenReturn(3);
            when(mockArray.get(0)).thenReturn(1L);
            jsonUtilsMock.when(() -> JsonUtils.parse("[1, 2, 3]")).thenReturn(mockArray);

            Object result = handler.getParameter(context, "param2", "paramType2", 2);
            Assert.assertEquals(result, mockArray);
        }
    }

    @Test
    public void testGetParameter_RecordType() {
        try (MockedStatic<SynapseUtils> synapseUtilsMock = Mockito.mockStatic(SynapseUtils.class);
             MockedStatic<DataTransformer> dataTransformerMock = Mockito.mockStatic(DataTransformer.class)) {

            MessageContext context = mock(MessageContext.class);
            ParamHandler handler = new ParamHandler();

            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "param0"))
                    .thenReturn("recordParam");
            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "paramType0"))
                    .thenReturn(Constants.RECORD);
            synapseUtilsMock.when(() -> SynapseUtils.lookupTemplateParameter(context, "recordParam"))
                    .thenReturn("{\"name\":\"test\"}");

            Object mockRecord = new Object();
            dataTransformerMock.when(() -> DataTransformer.createRecordValue("{\"name\":\"test\"}", "recordParam", context, 0))
                    .thenReturn(mockRecord);

            Object result = handler.getParameter(context, "param0", "paramType0", 0);
            Assert.assertEquals(result, mockRecord);
        }
    }

    @Test
    public void testConvertPathParam_BooleanFalse() {
        ParamHandler handler = new ParamHandler();
        Object result = handler.convertPathParam("false", Constants.BOOLEAN);
        Assert.assertEquals(result, false);
    }

    @Test
    public void testGetParameter_IntType() {
        try (MockedStatic<SynapseUtils> synapseUtilsMock = Mockito.mockStatic(SynapseUtils.class)) {
            MessageContext context = mock(MessageContext.class);
            ParamHandler handler = new ParamHandler();

            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "param0"))
                    .thenReturn("intParam");
            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "paramType0"))
                    .thenReturn(Constants.INT);
            synapseUtilsMock.when(() -> SynapseUtils.lookupTemplateParameter(context, "intParam"))
                    .thenReturn("42");

            Object result = handler.getParameter(context, "param0", "paramType0", 0);
            Assert.assertEquals(result, 42L);
        }
    }

    @Test
    public void testSetParameters_EmptyArgs() {
        try (MockedStatic<SynapseUtils> synapseUtilsMock = Mockito.mockStatic(SynapseUtils.class)) {
            MessageContext context = mock(MessageContext.class);
            ParamHandler handler = new ParamHandler();
            Object[] args = new Object[0];

            handler.setParameters(args, context);

            Assert.assertEquals(args.length, 0);
        }
    }

    @Test
    public void testGetParameter_UnionParamPattern() {
        try (MockedStatic<SynapseUtils> synapseUtilsMock = Mockito.mockStatic(SynapseUtils.class)) {
            MessageContext context = mock(MessageContext.class);
            ParamHandler handler = new ParamHandler();

            // Pattern "param\\d+Union.*" should use type directly
            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "param0UnionInt"))
                    .thenReturn("unionIntParam");
            synapseUtilsMock.when(() -> SynapseUtils.lookupTemplateParameter(context, "unionIntParam"))
                    .thenReturn("123");

            Object result = handler.getParameter(context, "param0UnionInt", Constants.INT, -1);
            Assert.assertEquals(result, 123L);
        }
    }

    @Test
    public void testPrependPathParams_WithFloatType() {
        try (MockedStatic<SynapseUtils> synapseUtilsMock = Mockito.mockStatic(SynapseUtils.class)) {
            MessageContext context = mock(MessageContext.class);
            ParamHandler handler = new ParamHandler();
            Object[] args = new Object[] { "arg1" };

            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, Constants.PATH_PARAM_SIZE))
                    .thenReturn("1");
            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "pathParam0"))
                    .thenReturn("p0");
            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "pathParamType0"))
                    .thenReturn(Constants.FLOAT);
            synapseUtilsMock.when(() -> SynapseUtils.lookupTemplateParameter(context, "p0"))
                    .thenReturn("3.14");

            Object[] combined = handler.prependPathParams(args, context);

            Assert.assertEquals(combined.length, 2);
            Assert.assertEquals(combined[0], 3.14);
            Assert.assertEquals(combined[1], "arg1");
        }
    }

    @Test
    public void testPrependPathParams_WithBooleanType() {
        try (MockedStatic<SynapseUtils> synapseUtilsMock = Mockito.mockStatic(SynapseUtils.class)) {
            MessageContext context = mock(MessageContext.class);
            ParamHandler handler = new ParamHandler();
            Object[] args = new Object[] { "arg1" };

            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, Constants.PATH_PARAM_SIZE))
                    .thenReturn("1");
            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "pathParam0"))
                    .thenReturn("p0");
            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "pathParamType0"))
                    .thenReturn(Constants.BOOLEAN);
            synapseUtilsMock.when(() -> SynapseUtils.lookupTemplateParameter(context, "p0"))
                    .thenReturn("true");

            Object[] combined = handler.prependPathParams(args, context);

            Assert.assertEquals(combined.length, 2);
            Assert.assertEquals(combined[0], true);
            Assert.assertEquals(combined[1], "arg1");
        }
    }

    @Test
    public void testPrependPathParams_WithDecimalType() {
        try (MockedStatic<SynapseUtils> synapseUtilsMock = Mockito.mockStatic(SynapseUtils.class);
             MockedStatic<ValueCreator> valueCreatorMock = Mockito.mockStatic(ValueCreator.class)) {

            MessageContext context = mock(MessageContext.class);
            ParamHandler handler = new ParamHandler();
            Object[] args = new Object[] { "arg1" };

            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, Constants.PATH_PARAM_SIZE))
                    .thenReturn("1");
            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "pathParam0"))
                    .thenReturn("p0");
            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "pathParamType0"))
                    .thenReturn(Constants.DECIMAL);
            synapseUtilsMock.when(() -> SynapseUtils.lookupTemplateParameter(context, "p0"))
                    .thenReturn("99.99");

            BDecimal mockDecimal = mock(BDecimal.class);
            valueCreatorMock.when(() -> ValueCreator.createDecimalValue("99.99"))
                    .thenReturn(mockDecimal);

            Object[] combined = handler.prependPathParams(args, context);

            Assert.assertEquals(combined.length, 2);
            Assert.assertEquals(combined[0], mockDecimal);
            Assert.assertEquals(combined[1], "arg1");
        }
    }

    @Test
    public void testGetParameter_ArrayRecordType() {
        try (MockedStatic<SynapseUtils> synapseUtilsMock = Mockito.mockStatic(SynapseUtils.class)) {
            MessageContext context = mock(MessageContext.class);
            ParamHandler handler = new ParamHandler();
            String input = "[{\"name\":\"alice\"}]";

            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "param0"))
                    .thenReturn("recordsParam");
            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "paramType0"))
                    .thenReturn(Constants.ARRAY);
            synapseUtilsMock.when(() -> SynapseUtils.lookupTemplateParameter(context, "recordsParam"))
                    .thenReturn(input);
            synapseUtilsMock.when(() -> SynapseUtils.cleanupJsonString(input))
                    .thenReturn(input);
            when(context.getProperty("arrayElementType0")).thenReturn("record");

            Object result = handler.getParameter(context, "param0", "paramType0", 0);
            Assert.assertNotNull(result);
        }
    }

    @Test
    public void testGetParameter_ArrayUnionTypeTable() {
        try (MockedStatic<SynapseUtils> synapseUtilsMock = Mockito.mockStatic(SynapseUtils.class)) {
            MessageContext context = mock(MessageContext.class);
            ParamHandler handler = new ParamHandler();
            String input = "[{\"type\":\"int\",\"value\":\"10\"},{\"type\":\"string\",\"value\":\"abc\"}]";

            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "param0"))
                    .thenReturn("unionArrayParam");
            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "paramType0"))
                    .thenReturn(Constants.ARRAY);
            synapseUtilsMock.when(() -> SynapseUtils.lookupTemplateParameter(context, "unionArrayParam"))
                    .thenReturn(input);
            synapseUtilsMock.when(() -> SynapseUtils.cleanupJsonString(input)).thenReturn(input);
            when(context.getProperty("arrayElementType0")).thenReturn("union");

            Object result = handler.getParameter(context, "param0", "paramType0", 0);
            Assert.assertNotNull(result);
            Assert.assertTrue(result instanceof BArray);
        }
    }

    @Test
    public void testGetParameter_ArrayNoIndexInValueKey() {
        try (MockedStatic<SynapseUtils> synapseUtilsMock = Mockito.mockStatic(SynapseUtils.class)) {
            MessageContext context = mock(MessageContext.class);
            ParamHandler handler = new ParamHandler();

            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "paramX"))
                    .thenReturn("arrayParam");
            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "paramTypeX"))
                    .thenReturn(Constants.ARRAY);
            synapseUtilsMock.when(() -> SynapseUtils.lookupTemplateParameter(context, "arrayParam"))
                    .thenReturn("[]");

            Object result = handler.getParameter(context, "paramX", "paramTypeX", 0);
            Assert.assertNull(result);
        }
    }

    @Test(expectedExceptions = SynapseException.class)
    public void testGetParameter_CatchBlock_ForInvalidIntValue() {
        try (MockedStatic<SynapseUtils> synapseUtilsMock = Mockito.mockStatic(SynapseUtils.class)) {
            MessageContext context = mock(MessageContext.class);
            ParamHandler handler = new ParamHandler();

            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "param0"))
                    .thenReturn("intParam");
            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "paramType0"))
                    .thenReturn(Constants.INT);
            synapseUtilsMock.when(() -> SynapseUtils.lookupTemplateParameter(context, "intParam"))
                    .thenReturn("not-a-number");

            handler.getParameter(context, "param0", "paramType0", 0);
        }
    }

    @Test
    public void testGetParameter_ArrayType_WithOverflowIndex_ReturnsNull() {
        try (MockedStatic<SynapseUtils> synapseUtilsMock = Mockito.mockStatic(SynapseUtils.class)) {
            MessageContext context = mock(MessageContext.class);
            ParamHandler handler = new ParamHandler();

            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "param999999999999999999999"))
                    .thenReturn("arrayParam");
            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "paramType999999999999999999999"))
                    .thenReturn(Constants.ARRAY);
            synapseUtilsMock.when(() -> SynapseUtils.lookupTemplateParameter(context, "arrayParam"))
                    .thenReturn("[]");

            Object result = handler.getParameter(
                    context, "param999999999999999999999", "paramType999999999999999999999", 0);
            Assert.assertNull(result);
        }
    }

    @Test(expectedExceptions = SynapseException.class)
    public void testGetParameter_ArrayRecordType_ParseException() {
        try (MockedStatic<SynapseUtils> synapseUtilsMock = Mockito.mockStatic(SynapseUtils.class);
             MockedStatic<JsonUtils> jsonUtilsMock = Mockito.mockStatic(JsonUtils.class)) {
            MessageContext context = mock(MessageContext.class);
            ParamHandler handler = new ParamHandler();
            String input = "[{\"name\":\"alice\"}]";

            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "param0"))
                    .thenReturn("recordsParam");
            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "paramType0"))
                    .thenReturn(Constants.ARRAY);
            synapseUtilsMock.when(() -> SynapseUtils.lookupTemplateParameter(context, "recordsParam"))
                    .thenReturn(input);
            synapseUtilsMock.when(() -> SynapseUtils.cleanupJsonString(input))
                    .thenReturn(input);
            when(context.getProperty("arrayElementType0")).thenReturn("record");
            jsonUtilsMock.when(() -> JsonUtils.parse(input))
                    .thenThrow(new RuntimeException("invalid record array"));

            handler.getParameter(context, "param0", "paramType0", 0);
        }
    }

    @Test(expectedExceptions = SynapseException.class)
    public void testGetParameter_ArrayFloatType_ParseException() {
        try (MockedStatic<SynapseUtils> synapseUtilsMock = Mockito.mockStatic(SynapseUtils.class);
             MockedStatic<JsonUtils> jsonUtilsMock = Mockito.mockStatic(JsonUtils.class)) {
            MessageContext context = mock(MessageContext.class);
            ParamHandler handler = new ParamHandler();
            String input = "[1.2,2.3]";

            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "param1"))
                    .thenReturn("floatArrayParam");
            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "paramType1"))
                    .thenReturn(Constants.ARRAY);
            synapseUtilsMock.when(() -> SynapseUtils.lookupTemplateParameter(context, "floatArrayParam"))
                    .thenReturn(input);
            synapseUtilsMock.when(() -> SynapseUtils.cleanupJsonString(input))
                    .thenReturn(input);
            when(context.getProperty("arrayElementType1")).thenReturn("float");
            jsonUtilsMock.when(() -> JsonUtils.parse(input))
                    .thenThrow(new RuntimeException("invalid float array"));

            handler.getParameter(context, "param1", "paramType1", 1);
        }
    }

    @Test(expectedExceptions = SynapseException.class)
    public void testGetParameter_Array2DType_ParseException() {
        try (MockedStatic<SynapseUtils> synapseUtilsMock = Mockito.mockStatic(SynapseUtils.class);
             MockedStatic<JsonUtils> jsonUtilsMock = Mockito.mockStatic(JsonUtils.class)) {
            MessageContext context = mock(MessageContext.class);
            ParamHandler handler = new ParamHandler();
            String input = "[{\"innerArray\":[1,2]}]";

            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "param2"))
                    .thenReturn("twoDParam");
            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "paramType2"))
                    .thenReturn(Constants.ARRAY);
            synapseUtilsMock.when(() -> SynapseUtils.lookupTemplateParameter(context, "twoDParam"))
                    .thenReturn(input);
            synapseUtilsMock.when(() -> SynapseUtils.cleanupJsonString(input))
                    .thenReturn(input);
            when(context.getProperty("arrayElementType2")).thenReturn("array");
            jsonUtilsMock.when(() -> JsonUtils.parse(input))
                    .thenThrow(new RuntimeException("invalid 2d array"));

            handler.getParameter(context, "param2", "paramType2", 2);
        }
    }

    @Test(expectedExceptions = SynapseException.class)
    public void testGetParameter_ArrayUnionType_ParseException() {
        try (MockedStatic<SynapseUtils> synapseUtilsMock = Mockito.mockStatic(SynapseUtils.class);
             MockedStatic<JsonUtils> jsonUtilsMock = Mockito.mockStatic(JsonUtils.class)) {
            MessageContext context = mock(MessageContext.class);
            ParamHandler handler = new ParamHandler();
            String input = "[{\"type\":\"int\",\"value\":\"1\"}]";

            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "param3"))
                    .thenReturn("unionParam");
            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "paramType3"))
                    .thenReturn(Constants.ARRAY);
            synapseUtilsMock.when(() -> SynapseUtils.lookupTemplateParameter(context, "unionParam"))
                    .thenReturn(input);
            synapseUtilsMock.when(() -> SynapseUtils.cleanupJsonString(input))
                    .thenReturn(input);
            when(context.getProperty("arrayElementType3")).thenReturn("union");
            jsonUtilsMock.when(() -> JsonUtils.parse(input))
                    .thenThrow(new RuntimeException("invalid union array"));

            handler.getParameter(context, "param3", "paramType3", 3);
        }
    }

    @Test(expectedExceptions = SynapseException.class)
    public void testGetParameter_ArrayDefaultType_ParseException() {
        try (MockedStatic<SynapseUtils> synapseUtilsMock = Mockito.mockStatic(SynapseUtils.class);
             MockedStatic<JsonUtils> jsonUtilsMock = Mockito.mockStatic(JsonUtils.class)) {
            MessageContext context = mock(MessageContext.class);
            ParamHandler handler = new ParamHandler();
            String input = "[1,2,3]";

            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "param4"))
                    .thenReturn("defaultArrayParam");
            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "paramType4"))
                    .thenReturn(Constants.ARRAY);
            synapseUtilsMock.when(() -> SynapseUtils.lookupTemplateParameter(context, "defaultArrayParam"))
                    .thenReturn(input);
            synapseUtilsMock.when(() -> SynapseUtils.cleanupJsonString(input))
                    .thenReturn(input);
            when(context.getProperty("arrayElementType4")).thenReturn("int");
            jsonUtilsMock.when(() -> JsonUtils.parse(input))
                    .thenThrow(new RuntimeException("invalid default array"));

            handler.getParameter(context, "param4", "paramType4", 4);
        }
    }

    @Test
    public void testGetParameter_ArrayType_FloatElementType_NonArrayParsedObject() {
        try (MockedStatic<SynapseUtils> synapseUtilsMock = Mockito.mockStatic(SynapseUtils.class);
             MockedStatic<JsonUtils> jsonUtilsMock = Mockito.mockStatic(JsonUtils.class)) {
            MessageContext context = mock(MessageContext.class);
            ParamHandler handler = new ParamHandler();
            String input = "{\"n\":1}";

            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "param5"))
                    .thenReturn("floatArrayAsObject");
            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "paramType5"))
                    .thenReturn(Constants.ARRAY);
            synapseUtilsMock.when(() -> SynapseUtils.lookupTemplateParameter(context, "floatArrayAsObject"))
                    .thenReturn(input);
            synapseUtilsMock.when(() -> SynapseUtils.cleanupJsonString(input))
                    .thenReturn(input);
            when(context.getProperty("arrayElementType5")).thenReturn("float");

            Object parsedObj = new Object();
            jsonUtilsMock.when(() -> JsonUtils.parse(input)).thenReturn(parsedObj);

            Object result = handler.getParameter(context, "param5", "paramType5", 5);
            Assert.assertSame(result, parsedObj);
        }
    }

    @Test
    public void testTransformUnionTableToArray_InvalidBranches() throws Exception {
        try (MockedStatic<StringUtils> stringUtilsMock = Mockito.mockStatic(StringUtils.class)) {
            ParamHandler handler = new ParamHandler();
            Method method = ParamHandler.class.getDeclaredMethod("transformUnionTableToArray", BArray.class);
            method.setAccessible(true);

            BString typeKey = mock(BString.class);
            BString valueKey = mock(BString.class);
            stringUtilsMock.when(() -> StringUtils.fromString("type")).thenReturn(typeKey);
            stringUtilsMock.when(() -> StringUtils.fromString("value")).thenReturn(valueKey);

            BArray rows = mock(BArray.class);
            when(rows.size()).thenReturn(5);

            BMap row0 = mock(BMap.class);
            when(rows.get(0)).thenReturn(row0);
            when(row0.get(typeKey)).thenReturn("int");
            when(row0.get(valueKey)).thenReturn("x");

            BMap row1 = mock(BMap.class);
            when(rows.get(1)).thenReturn(row1);
            when(row1.get(typeKey)).thenReturn("float");
            when(row1.get(valueKey)).thenReturn("y");

            BMap row2 = mock(BMap.class);
            when(rows.get(2)).thenReturn(row2);
            when(row2.get(typeKey)).thenReturn("boolean");
            when(row2.get(valueKey)).thenReturn("not-bool");

            BMap row3 = mock(BMap.class);
            when(rows.get(3)).thenReturn(row3);
            when(row3.get(typeKey)).thenReturn(null);
            when(row3.get(valueKey)).thenReturn(null);

            BMap row4 = mock(BMap.class);
            when(rows.get(4)).thenReturn(row4);
            when(row4.get(typeKey)).thenReturn("decimal");
            when(row4.get(valueKey)).thenReturn("12.5");

            String output = (String) method.invoke(handler, rows);
            Assert.assertEquals(output, "[\"x\",\"y\",\"not-bool\",\"\",12.5]");
        }
    }

    @Test
    public void testConvertDecimalArrayToFloatArray_ParseFailureDefaultsToZero() throws Exception {
        try (MockedStatic<ValueCreator> valueCreatorMock = Mockito.mockStatic(ValueCreator.class)) {
            ParamHandler handler = new ParamHandler();
            Method method = ParamHandler.class.getDeclaredMethod("convertDecimalArrayToFloatArray", BArray.class);
            method.setAccessible(true);

            BArray input = mock(BArray.class);
            when(input.size()).thenReturn(1);
            when(input.get(0)).thenReturn("not-a-number");

            BArray outputArray = mock(BArray.class);
            valueCreatorMock.when(() -> ValueCreator.createArrayValue(any(double[].class))).thenReturn(outputArray);

            BArray result = (BArray) method.invoke(handler, input);
            Assert.assertSame(result, outputArray);
        }
    }

    @Test
    public void testGetOMElement_InvalidXmlString_ReturnsNull() throws Exception {
        try (MockedStatic<SynapseUtils> synapseUtilsMock = Mockito.mockStatic(SynapseUtils.class)) {
            ParamHandler handler = new ParamHandler();
            Method method = ParamHandler.class.getDeclaredMethod("getOMElement", MessageContext.class, String.class);
            method.setAccessible(true);

            MessageContext context = mock(MessageContext.class);
            when(context.getProperty("param0")).thenReturn("templateKey");
            synapseUtilsMock.when(() -> SynapseUtils.lookupTemplateParameter(context, "templateKey"))
                    .thenReturn("<bad");

            Object result = method.invoke(handler, context, "param0");
            Assert.assertNull(result);
        }
    }

    @Test
    public void testGetParameter_XmlType_WithOMElementTemplateValue() throws Exception {
        try (MockedStatic<SynapseUtils> synapseUtilsMock = Mockito.mockStatic(SynapseUtils.class)) {
            ParamHandler handler = new ParamHandler();
            MessageContext context = mock(MessageContext.class);
            OMElement omElement = AXIOMUtil.stringToOM("<root><a>1</a></root>");

            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "param0"))
                    .thenReturn("xmlParam");
            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "paramType0"))
                    .thenReturn(Constants.XML);
            synapseUtilsMock.when(() -> SynapseUtils.lookupTemplateParameter(context, "xmlParam"))
                    .thenReturn(omElement);
            when(context.getProperty("param0")).thenReturn("xmlParam");

            Object result = handler.getParameter(context, "param0", "paramType0", 0);
            Assert.assertNotNull(result);
            Assert.assertTrue(result instanceof BXml);
        }
    }

    @Test
    public void testGetParameter_XmlType_WithXmlStringTemplateValue() {
        try (MockedStatic<SynapseUtils> synapseUtilsMock = Mockito.mockStatic(SynapseUtils.class)) {
            ParamHandler handler = new ParamHandler();
            MessageContext context = mock(MessageContext.class);

            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "param0"))
                    .thenReturn("xmlParam");
            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "paramType0"))
                    .thenReturn(Constants.XML);
            synapseUtilsMock.when(() -> SynapseUtils.lookupTemplateParameter(context, "xmlParam"))
                    .thenReturn("<root><a>2</a></root>");
            when(context.getProperty("param0")).thenReturn("xmlParam");

            Object result = handler.getParameter(context, "param0", "paramType0", 0);
            Assert.assertNotNull(result);
            Assert.assertTrue(result instanceof BXml);
        }
    }

    @Test
    public void testGetParameter_ArrayType_NestedTableBMap_UsesTransformNestedTableTo2DArray() {
        try (MockedStatic<SynapseUtils> synapseUtilsMock = Mockito.mockStatic(SynapseUtils.class);
             MockedStatic<JsonUtils> jsonUtilsMock = Mockito.mockStatic(JsonUtils.class);
             MockedStatic<DataTransformer> dataTransformerMock = Mockito.mockStatic(DataTransformer.class)) {
            ParamHandler handler = new ParamHandler();
            MessageContext context = mock(MessageContext.class);
            String raw = "[{\"innerArray\":[{\"value\":1}]}]";
            String transformed = "[[1]]";

            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "param6")).thenReturn("nested2d");
            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "paramType6")).thenReturn(Constants.ARRAY);
            synapseUtilsMock.when(() -> SynapseUtils.lookupTemplateParameter(context, "nested2d")).thenReturn(raw);
            synapseUtilsMock.when(() -> SynapseUtils.cleanupJsonString(raw)).thenReturn(raw);
            when(context.getProperty("arrayElementType6")).thenReturn("array");

            BArray parsedOuter = mock(BArray.class);
            BMap firstRow = mock(BMap.class);
            when(parsedOuter.size()).thenReturn(1);
            when(parsedOuter.get(0)).thenReturn(firstRow);
            when(firstRow.containsKey(any(BString.class))).thenReturn(true);

            Object transformedParsed = new Object();
            jsonUtilsMock.when(() -> JsonUtils.parse(raw)).thenReturn(parsedOuter);
            dataTransformerMock.when(() -> DataTransformer.transformNestedTableTo2DArray(parsedOuter)).thenReturn(transformed);
            jsonUtilsMock.when(() -> JsonUtils.parse(transformed)).thenReturn(transformedParsed);

            Object result = handler.getParameter(context, "param6", "paramType6", 6);
            Assert.assertSame(result, transformedParsed);
        }
    }

    @Test
    public void testGetParameter_ArrayType_MiStudioNestedTable_UsesTransformMIStudioNestedTableTo2DArray() {
        try (MockedStatic<SynapseUtils> synapseUtilsMock = Mockito.mockStatic(SynapseUtils.class);
             MockedStatic<JsonUtils> jsonUtilsMock = Mockito.mockStatic(JsonUtils.class);
             MockedStatic<DataTransformer> dataTransformerMock = Mockito.mockStatic(DataTransformer.class)) {
            ParamHandler handler = new ParamHandler();
            MessageContext context = mock(MessageContext.class);
            String raw = "[[\"a\",\"b\"]]";
            String transformed = "[[\"a\",\"b\"]]";

            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "param7")).thenReturn("nestedMi");
            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "paramType7")).thenReturn(Constants.ARRAY);
            synapseUtilsMock.when(() -> SynapseUtils.lookupTemplateParameter(context, "nestedMi")).thenReturn(raw);
            synapseUtilsMock.when(() -> SynapseUtils.cleanupJsonString(raw)).thenReturn(raw);
            when(context.getProperty("arrayElementType7")).thenReturn("array");

            BArray parsedOuter = mock(BArray.class);
            BArray firstRow = mock(BArray.class);
            when(parsedOuter.size()).thenReturn(1);
            when(parsedOuter.get(0)).thenReturn(firstRow);
            when(firstRow.size()).thenReturn(2);

            Object transformedParsed = new Object();
            jsonUtilsMock.when(() -> JsonUtils.parse(raw)).thenReturn(parsedOuter);
            dataTransformerMock.when(() -> DataTransformer.transformMIStudioNestedTableTo2DArray(parsedOuter, context))
                    .thenReturn(transformed);
            jsonUtilsMock.when(() -> JsonUtils.parse(transformed)).thenReturn(transformedParsed);

            Object result = handler.getParameter(context, "param7", "paramType7", 7);
            Assert.assertSame(result, transformedParsed);
        }
    }

    @Test
    public void testConvertDecimalArrayToFloatArray_BDecimalAndNumberBranches() throws Exception {
        try (MockedStatic<ValueCreator> valueCreatorMock = Mockito.mockStatic(ValueCreator.class)) {
            ParamHandler handler = new ParamHandler();
            Method method = ParamHandler.class.getDeclaredMethod("convertDecimalArrayToFloatArray", BArray.class);
            method.setAccessible(true);

            BArray input = mock(BArray.class);
            when(input.size()).thenReturn(2);

            BDecimal decimal = mock(BDecimal.class);
            when(decimal.decimalValue()).thenReturn(new BigDecimal("1.5"));

            when(input.get(0)).thenReturn(decimal);
            when(input.get(1)).thenReturn(2);

            BArray outputArray = mock(BArray.class);
            valueCreatorMock.when(() -> ValueCreator.createArrayValue(any(double[].class))).thenReturn(outputArray);

            BArray result = (BArray) method.invoke(handler, input);
            Assert.assertSame(result, outputArray);
        }
    }

    @Test
    public void testTransformUnionTableToArray_IgnoresNonBMapRows() throws Exception {
        ParamHandler handler = new ParamHandler();
        Method method = ParamHandler.class.getDeclaredMethod("transformUnionTableToArray", BArray.class);
        method.setAccessible(true);

        BArray rows = mock(BArray.class);
        when(rows.size()).thenReturn(1);
        when(rows.get(0)).thenReturn("not-a-map");

        String output = (String) method.invoke(handler, rows);
        Assert.assertEquals(output, "[]");
    }
}
