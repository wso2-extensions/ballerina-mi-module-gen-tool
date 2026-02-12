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

import io.ballerina.runtime.api.utils.JsonUtils;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.stdlib.mi.Constants;
import io.ballerina.stdlib.mi.utils.SynapseUtils;
import org.apache.synapse.MessageContext;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DataTransformerTest {

    @Test
    public void testTransformNestedTableTo2DArray() {
        try (MockedStatic<StringUtils> stringUtilsMock = Mockito.mockStatic(StringUtils.class)) {
            BString innerArrayKey = mock(BString.class);
            BString valueKey = mock(BString.class);
            BString someStringValue = mock(BString.class);

            stringUtilsMock.when(() -> StringUtils.fromString("innerArray")).thenReturn(innerArrayKey);
            stringUtilsMock.when(() -> StringUtils.fromString("value")).thenReturn(valueKey);
            
            // Mock Outer Array
            BArray outerArray = mock(BArray.class);
            when(outerArray.size()).thenReturn(1);

            // Mock Outer Row (BMap)
            BMap outerRow = mock(BMap.class);
            when(outerArray.get(0)).thenReturn(outerRow);
            
            // Mock Inner Array
            BArray innerArray = mock(BArray.class);
            when(outerRow.get(innerArrayKey)).thenReturn(innerArray);
            when(innerArray.size()).thenReturn(2);

            // Mock Inner Element 1 (BMap with value)
            BMap innerRow1 = mock(BMap.class);
            when(innerArray.get(0)).thenReturn(innerRow1);
            when(innerRow1.containsKey(valueKey)).thenReturn(true);
            when(innerRow1.size()).thenReturn(1);
            when(innerRow1.get(valueKey)).thenReturn(10L); // long value

            // Mock Inner Element 2 (BMap with value)
            BMap innerRow2 = mock(BMap.class);
            when(innerArray.get(1)).thenReturn(innerRow2);
            when(innerRow2.containsKey(valueKey)).thenReturn(true);
            when(innerRow2.size()).thenReturn(1);
            when(innerRow2.get(valueKey)).thenReturn("hello"); // string value

            String result = DataTransformer.transformNestedTableTo2DArray(outerArray);
            
            // Result is a JSON string of a 2D array.
            // The method builds it manually.
            // [[10,"hello"]]
            // "hello" is quoted.
            // 10 is long.
            
            // The implementation:
            // appendJsonValue(jsonBuilder, val);
            // appendJsonValue uses value.toString() and checks number format.
            // 10L.toString() -> "10". Long.parseLong("10") works -> append("10")
            // "hello".toString() -> "hello". Long/Double parse fail -> append("\"hello\"") if not boolean
            
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

            // Element 1: String
            BMap row1 = mock(BMap.class);
            when(tableArray.get(0)).thenReturn(row1);
            BString strVal = mock(BString.class);
            when(strVal.getValue()).thenReturn("foo");
            when(row1.get(valueKey)).thenReturn(strVal);

            // Element 2: Number
            BMap row2 = mock(BMap.class);
            when(tableArray.get(1)).thenReturn(row2);
            when(row2.get(valueKey)).thenReturn(42);

            // Element 3: Boolean
            BMap row3 = mock(BMap.class);
            when(tableArray.get(2)).thenReturn(row3);
            when(row3.get(valueKey)).thenReturn(true);

            String result = DataTransformer.transformTableArrayToSimpleArray(tableArray);

            // Expected: ["foo",42,true]
            Assert.assertEquals(result, "[\"foo\",42,true]");
        }
    }

    @Test
    public void testReconstructRecordFromFields() {
        try (MockedStatic<SynapseUtils> synapseUtilsMock = Mockito.mockStatic(SynapseUtils.class);
             MockedStatic<JsonUtils> jsonUtilsMock = Mockito.mockStatic(JsonUtils.class)) {
             
            MessageContext context = mock(MessageContext.class);
            String prefix = "testPrefix";

            // Index 0: name (String)
            when(context.getProperty(prefix + "_param0")).thenReturn("name");
            when(context.getProperty(prefix + "_paramType0")).thenReturn(Constants.STRING);
            synapseUtilsMock.when(() -> SynapseUtils.lookupTemplateParameter(context, "name")).thenReturn("Alice");

            // Index 1: age (Int)
            when(context.getProperty(prefix + "_param1")).thenReturn("age");
            when(context.getProperty(prefix + "_paramType1")).thenReturn(Constants.INT);
            synapseUtilsMock.when(() -> SynapseUtils.lookupTemplateParameter(context, "age")).thenReturn("30");

            // Index 2: null (Stop loop 2)
            when(context.getProperty(prefix + "_param2")).thenReturn(null);

            // Mock loop 1 (union check) - iterate same properties
            // Since loop 1 also uses tempIndex starting 0 and checks property existence, it will run until Index 2 returns null (or properties missing)
            
            // Mock JsonUtils.parse
            Object expectedBallerinaRecord = new Object();
            // The JSON constructed will be {"name":"Alice","age":30}
            // Note: JsonObject.toString() output order depends on implementation but usually insertion order for Gson
            jsonUtilsMock.when(() -> JsonUtils.parse(anyString())).thenReturn(expectedBallerinaRecord);
            
            Object result = DataTransformer.reconstructRecordFromFields(prefix, context);
            
            Assert.assertSame(result, expectedBallerinaRecord);
        }
    }
}
