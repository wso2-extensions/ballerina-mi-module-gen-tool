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

package io.ballerina.stdlib.mi;

import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.utils.JsonUtils;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BDecimal;
import io.ballerina.runtime.api.values.BString;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for TypeConverter utility class.
 */
public class TypeConverterTest {

    @Test
    public void testIsArrayType() {
        Assert.assertTrue(TypeConverter.isArrayType("array"));
        Assert.assertFalse(TypeConverter.isArrayType("string"));
        Assert.assertFalse(TypeConverter.isArrayType("int"));
        Assert.assertFalse(TypeConverter.isArrayType(null));
        Assert.assertFalse(TypeConverter.isArrayType("Array")); // case sensitive
    }

    @Test
    public void testIsPrimitiveType() {
        Assert.assertTrue(TypeConverter.isPrimitiveType("boolean"));
        Assert.assertTrue(TypeConverter.isPrimitiveType("int"));
        Assert.assertTrue(TypeConverter.isPrimitiveType("string"));
        Assert.assertTrue(TypeConverter.isPrimitiveType("float"));
        Assert.assertTrue(TypeConverter.isPrimitiveType("decimal"));
        Assert.assertFalse(TypeConverter.isPrimitiveType("array"));
        Assert.assertFalse(TypeConverter.isPrimitiveType("record"));
        Assert.assertFalse(TypeConverter.isPrimitiveType("json"));
        Assert.assertFalse(TypeConverter.isPrimitiveType("xml"));
        Assert.assertFalse(TypeConverter.isPrimitiveType(null));
    }

    @Test
    public void testArrayToJsonString_Null() {
        String result = TypeConverter.arrayToJsonString(null);
        Assert.assertEquals(result, "[]");
    }

    @Test
    public void testArrayToJsonString_NonNull() {
        BArray mockArray = mock(BArray.class);
        when(mockArray.toString()).thenReturn("[1, 2, 3]");

        String result = TypeConverter.arrayToJsonString(mockArray);
        Assert.assertEquals(result, "[1, 2, 3]");
    }

    @Test
    public void testConvertToDecimalArray() {
        BArray mockArray = mock(BArray.class);
        BArray result = TypeConverter.convertToDecimalArray(mockArray);
        Assert.assertSame(result, mockArray); // Returns input as-is
    }

    @Test
    public void testConvertToStringArray() {
        try (MockedStatic<StringUtils> stringUtilsMock = Mockito.mockStatic(StringUtils.class);
             MockedStatic<ValueCreator> valueCreatorMock = Mockito.mockStatic(ValueCreator.class)) {

            BArray jsonArray = mock(BArray.class);
            when(jsonArray.size()).thenReturn(3);
            when(jsonArray.get(0)).thenReturn("hello");
            when(jsonArray.get(1)).thenReturn("world");
            when(jsonArray.get(2)).thenReturn("test");

            BString mockStr1 = mock(BString.class);
            BString mockStr2 = mock(BString.class);
            BString mockStr3 = mock(BString.class);
            stringUtilsMock.when(() -> StringUtils.fromString("hello")).thenReturn(mockStr1);
            stringUtilsMock.when(() -> StringUtils.fromString("world")).thenReturn(mockStr2);
            stringUtilsMock.when(() -> StringUtils.fromString("test")).thenReturn(mockStr3);

            BArray resultArray = mock(BArray.class);
            valueCreatorMock.when(() -> ValueCreator.createArrayValue(any(BString[].class))).thenReturn(resultArray);

            BArray result = TypeConverter.convertToStringArray(jsonArray);
            Assert.assertSame(result, resultArray);
        }
    }

    @Test
    public void testConvertToIntArray_LongValues() {
        try (MockedStatic<ValueCreator> valueCreatorMock = Mockito.mockStatic(ValueCreator.class)) {
            BArray jsonArray = mock(BArray.class);
            when(jsonArray.size()).thenReturn(3);
            when(jsonArray.get(0)).thenReturn(1L);
            when(jsonArray.get(1)).thenReturn(2L);
            when(jsonArray.get(2)).thenReturn(3L);

            BArray resultArray = mock(BArray.class);
            valueCreatorMock.when(() -> ValueCreator.createArrayValue(any(long[].class))).thenReturn(resultArray);

            BArray result = TypeConverter.convertToIntArray(jsonArray);
            Assert.assertSame(result, resultArray);
        }
    }

    @Test
    public void testConvertToIntArray_StringValues() {
        try (MockedStatic<ValueCreator> valueCreatorMock = Mockito.mockStatic(ValueCreator.class)) {
            BArray jsonArray = mock(BArray.class);
            when(jsonArray.size()).thenReturn(2);
            when(jsonArray.get(0)).thenReturn("100");
            when(jsonArray.get(1)).thenReturn("200");

            BArray resultArray = mock(BArray.class);
            valueCreatorMock.when(() -> ValueCreator.createArrayValue(any(long[].class))).thenReturn(resultArray);

            BArray result = TypeConverter.convertToIntArray(jsonArray);
            Assert.assertSame(result, resultArray);
        }
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testConvertToIntArray_InvalidValue() {
        try (MockedStatic<ValueCreator> valueCreatorMock = Mockito.mockStatic(ValueCreator.class)) {
            BArray jsonArray = mock(BArray.class);
            when(jsonArray.size()).thenReturn(1);
            when(jsonArray.get(0)).thenReturn("not_a_number");

            TypeConverter.convertToIntArray(jsonArray);
        }
    }

    @Test
    public void testConvertToBooleanArray() {
        try (MockedStatic<ValueCreator> valueCreatorMock = Mockito.mockStatic(ValueCreator.class)) {
            BArray jsonArray = mock(BArray.class);
            when(jsonArray.size()).thenReturn(3);
            when(jsonArray.get(0)).thenReturn(true);
            when(jsonArray.get(1)).thenReturn(false);
            when(jsonArray.get(2)).thenReturn(true);

            BArray resultArray = mock(BArray.class);
            valueCreatorMock.when(() -> ValueCreator.createArrayValue(any(boolean[].class))).thenReturn(resultArray);

            BArray result = TypeConverter.convertToBooleanArray(jsonArray);
            Assert.assertSame(result, resultArray);
        }
    }

    @Test
    public void testConvertToFloatArray_DoubleValues() {
        try (MockedStatic<ValueCreator> valueCreatorMock = Mockito.mockStatic(ValueCreator.class)) {
            BArray jsonArray = mock(BArray.class);
            when(jsonArray.size()).thenReturn(3);
            when(jsonArray.get(0)).thenReturn(1.1);
            when(jsonArray.get(1)).thenReturn(2.2);
            when(jsonArray.get(2)).thenReturn(3.3);

            BArray resultArray = mock(BArray.class);
            valueCreatorMock.when(() -> ValueCreator.createArrayValue(any(double[].class))).thenReturn(resultArray);

            BArray result = TypeConverter.convertToFloatArray(jsonArray);
            Assert.assertSame(result, resultArray);
        }
    }

    @Test
    public void testConvertToFloatArray_StringValues() {
        try (MockedStatic<ValueCreator> valueCreatorMock = Mockito.mockStatic(ValueCreator.class)) {
            BArray jsonArray = mock(BArray.class);
            when(jsonArray.size()).thenReturn(2);
            when(jsonArray.get(0)).thenReturn("1.5");
            when(jsonArray.get(1)).thenReturn("2.5");

            BArray resultArray = mock(BArray.class);
            valueCreatorMock.when(() -> ValueCreator.createArrayValue(any(double[].class))).thenReturn(resultArray);

            BArray result = TypeConverter.convertToFloatArray(jsonArray);
            Assert.assertSame(result, resultArray);
        }
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testConvertToFloatArray_InvalidValue() {
        try (MockedStatic<ValueCreator> valueCreatorMock = Mockito.mockStatic(ValueCreator.class)) {
            BArray jsonArray = mock(BArray.class);
            when(jsonArray.size()).thenReturn(1);
            when(jsonArray.get(0)).thenReturn("not_a_float");

            TypeConverter.convertToFloatArray(jsonArray);
        }
    }

    @Test
    public void testConvertToArray_String() {
        try (MockedStatic<JsonUtils> jsonUtilsMock = Mockito.mockStatic(JsonUtils.class);
             MockedStatic<StringUtils> stringUtilsMock = Mockito.mockStatic(StringUtils.class);
             MockedStatic<ValueCreator> valueCreatorMock = Mockito.mockStatic(ValueCreator.class)) {

            BArray jsonArray = mock(BArray.class);
            when(jsonArray.size()).thenReturn(1);
            when(jsonArray.get(0)).thenReturn("test");

            jsonUtilsMock.when(() -> JsonUtils.parse("[\"test\"]")).thenReturn(jsonArray);

            BString mockStr = mock(BString.class);
            stringUtilsMock.when(() -> StringUtils.fromString("test")).thenReturn(mockStr);

            BArray resultArray = mock(BArray.class);
            valueCreatorMock.when(() -> ValueCreator.createArrayValue(any(BString[].class))).thenReturn(resultArray);

            BArray result = TypeConverter.convertToArray("[\"test\"]", "string");
            Assert.assertSame(result, resultArray);
        }
    }

    @Test
    public void testConvertToArray_Int() {
        try (MockedStatic<JsonUtils> jsonUtilsMock = Mockito.mockStatic(JsonUtils.class);
             MockedStatic<ValueCreator> valueCreatorMock = Mockito.mockStatic(ValueCreator.class)) {

            BArray jsonArray = mock(BArray.class);
            when(jsonArray.size()).thenReturn(1);
            when(jsonArray.get(0)).thenReturn(42L);

            jsonUtilsMock.when(() -> JsonUtils.parse("[42]")).thenReturn(jsonArray);

            BArray resultArray = mock(BArray.class);
            valueCreatorMock.when(() -> ValueCreator.createArrayValue(any(long[].class))).thenReturn(resultArray);

            BArray result = TypeConverter.convertToArray("[42]", "int");
            Assert.assertSame(result, resultArray);
        }
    }

    @Test
    public void testConvertToArray_Boolean() {
        try (MockedStatic<JsonUtils> jsonUtilsMock = Mockito.mockStatic(JsonUtils.class);
             MockedStatic<ValueCreator> valueCreatorMock = Mockito.mockStatic(ValueCreator.class)) {

            BArray jsonArray = mock(BArray.class);
            when(jsonArray.size()).thenReturn(1);
            when(jsonArray.get(0)).thenReturn(true);

            jsonUtilsMock.when(() -> JsonUtils.parse("[true]")).thenReturn(jsonArray);

            BArray resultArray = mock(BArray.class);
            valueCreatorMock.when(() -> ValueCreator.createArrayValue(any(boolean[].class))).thenReturn(resultArray);

            BArray result = TypeConverter.convertToArray("[true]", "boolean");
            Assert.assertSame(result, resultArray);
        }
    }

    @Test
    public void testConvertToArray_Float() {
        try (MockedStatic<JsonUtils> jsonUtilsMock = Mockito.mockStatic(JsonUtils.class);
             MockedStatic<ValueCreator> valueCreatorMock = Mockito.mockStatic(ValueCreator.class)) {

            BArray jsonArray = mock(BArray.class);
            when(jsonArray.size()).thenReturn(1);
            when(jsonArray.get(0)).thenReturn(3.14);

            jsonUtilsMock.when(() -> JsonUtils.parse("[3.14]")).thenReturn(jsonArray);

            BArray resultArray = mock(BArray.class);
            valueCreatorMock.when(() -> ValueCreator.createArrayValue(any(double[].class))).thenReturn(resultArray);

            BArray result = TypeConverter.convertToArray("[3.14]", "float");
            Assert.assertSame(result, resultArray);
        }
    }

    @Test
    public void testConvertToArray_Decimal() {
        try (MockedStatic<JsonUtils> jsonUtilsMock = Mockito.mockStatic(JsonUtils.class)) {
            BArray jsonArray = mock(BArray.class);
            jsonUtilsMock.when(() -> JsonUtils.parse("[10.5]")).thenReturn(jsonArray);

            BArray result = TypeConverter.convertToArray("[10.5]", "decimal");
            Assert.assertSame(result, jsonArray); // Returns as-is for decimal
        }
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testConvertToArray_UnsupportedType() {
        try (MockedStatic<JsonUtils> jsonUtilsMock = Mockito.mockStatic(JsonUtils.class)) {
            BArray jsonArray = mock(BArray.class);
            jsonUtilsMock.when(() -> JsonUtils.parse("[1]")).thenReturn(jsonArray);

            TypeConverter.convertToArray("[1]", "unknown_type");
        }
    }

    @Test
    public void testConvertValue_Boolean() {
        Object result = TypeConverter.convertValue("true", "boolean");
        Assert.assertEquals(result, true);

        result = TypeConverter.convertValue("false", "boolean");
        Assert.assertEquals(result, false);
    }

    @Test
    public void testConvertValue_Int() {
        Object result = TypeConverter.convertValue("42", "int");
        Assert.assertEquals(result, 42L);

        result = TypeConverter.convertValue("-100", "int");
        Assert.assertEquals(result, -100L);
    }

    @Test
    public void testConvertValue_String() {
        try (MockedStatic<StringUtils> stringUtilsMock = Mockito.mockStatic(StringUtils.class)) {
            BString mockStr = mock(BString.class);
            stringUtilsMock.when(() -> StringUtils.fromString("hello")).thenReturn(mockStr);

            Object result = TypeConverter.convertValue("hello", "string");
            Assert.assertSame(result, mockStr);
        }
    }

    @Test
    public void testConvertValue_Float() {
        Object result = TypeConverter.convertValue("3.14", "float");
        Assert.assertEquals(result, 3.14);

        result = TypeConverter.convertValue("-2.5", "float");
        Assert.assertEquals(result, -2.5);
    }

    @Test
    public void testConvertValue_Decimal() {
        try (MockedStatic<ValueCreator> valueCreatorMock = Mockito.mockStatic(ValueCreator.class)) {
            BDecimal mockDecimal = mock(BDecimal.class);
            valueCreatorMock.when(() -> ValueCreator.createDecimalValue("10.99")).thenReturn(mockDecimal);

            Object result = TypeConverter.convertValue("10.99", "decimal");
            Assert.assertSame(result, mockDecimal);
        }
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testConvertValue_UnsupportedType() {
        TypeConverter.convertValue("test", "xml");
    }

    @Test(expectedExceptions = NumberFormatException.class)
    public void testConvertValue_InvalidInt() {
        TypeConverter.convertValue("not_a_number", "int");
    }

    @Test(expectedExceptions = NumberFormatException.class)
    public void testConvertValue_InvalidFloat() {
        TypeConverter.convertValue("not_a_float", "float");
    }
}
