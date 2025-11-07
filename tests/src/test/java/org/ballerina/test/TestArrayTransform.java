/*
 * Copyright (c) 2025, WSO2 LLC. (http://wso2.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ballerina.test;

import io.ballerina.stdlib.mi.Mediator;
import io.ballerina.stdlib.mi.ModuleInfo;
import org.apache.synapse.data.connector.DefaultConnectorResponse;
import org.apache.synapse.mediators.template.TemplateContext;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

/**
 * Tests for array type support in Mediator (transformations).
 * Tests Ballerina functions that accept and return arrays.
 */
public class TestArrayTransform {

    @Test(description = "Test string array transformation")
    public void testStringArrayTransform() {
        String project = "arrayProject";
        ModuleInfo moduleInfo = new ModuleInfo("testOrg", project, "1");
        Mediator mediator = new Mediator(moduleInfo);

        TestMessageContext context = TestDataBuilder.messageContext()
                .functionName("testStringArray")
                .returnType("string")
                .addParameter("names", "array", "[\"Alice\", \"Bob\", \"Charlie\"]")
                .addProperty("arrayElementType0", "string")
                .build();

        mediator.mediate(context);

        String result = ((DefaultConnectorResponse) context.getVariable("result")).getPayload().toString();
        Assert.assertEquals(result, "Alice,Bob,Charlie,", "String array should be processed correctly");
    }

    @Test(description = "Test int array transformation - sum")
    public void testIntArrayTransform() {
        String project = "arrayProject";
        ModuleInfo moduleInfo = new ModuleInfo("testOrg", project, "1");
        Mediator mediator = new Mediator(moduleInfo);

        TestMessageContext context = TestDataBuilder.messageContext()
                .functionName("testIntArray")
                .returnType("int")
                .addParameter("numbers", "array", "[1, 2, 3, 4, 5]")
                .addProperty("arrayElementType0", "int")
                .build();

        mediator.mediate(context);

        String result = ((DefaultConnectorResponse) context.getVariable("result")).getPayload().toString();
        Assert.assertEquals(result, "15", "Int array sum should be 15");
    }

    @Test(description = "Test boolean array transformation - all true check")
    public void testBooleanArrayTransform() {
        String project = "arrayProject";
        ModuleInfo moduleInfo = new ModuleInfo("testOrg", project, "1");
        Mediator mediator = new Mediator(moduleInfo);

        TestMessageContext context = TestDataBuilder.messageContext()
                .functionName("testBooleanArray")
                .returnType("boolean")
                .addParameter("flags", "array", "[true, true, true]")
                .addProperty("arrayElementType0", "boolean")
                .build();

        mediator.mediate(context);

        String result = ((DefaultConnectorResponse) context.getVariable("result")).getPayload().toString();
        Assert.assertEquals(result, "true", "All true flags should return true");
    }

    @Test(description = "Test boolean array with false value")
    public void testBooleanArrayWithFalse() {
        String project = "arrayProject";
        ModuleInfo moduleInfo = new ModuleInfo("testOrg", project, "1");
        Mediator mediator = new Mediator(moduleInfo);

        TestMessageContext context = TestDataBuilder.messageContext()
                .functionName("testBooleanArray")
                .returnType("boolean")
                .addParameter("flags", "array", "[true, false, true]")
                .addProperty("arrayElementType0", "boolean")
                .build();

        mediator.mediate(context);

        String result = ((DefaultConnectorResponse) context.getVariable("result")).getPayload().toString();
        Assert.assertEquals(result, "false", "Array with false should return false");
    }

    @Test(description = "Test float array transformation - average")
    public void testFloatArrayTransform() {
        String project = "arrayProject";
        ModuleInfo moduleInfo = new ModuleInfo("testOrg", project, "1");
        Mediator mediator = new Mediator(moduleInfo);

        TestMessageContext context = TestDataBuilder.messageContext()
                .functionName("testFloatArray")
                .returnType("float")
                .addParameter("values", "array", "[10.0, 20.0, 30.0]")
                .addProperty("arrayElementType0", "float")
                .build();

        mediator.mediate(context);

        String result = ((DefaultConnectorResponse) context.getVariable("result")).getPayload().toString();
        Assert.assertEquals(result, "20.0", "Average of 10, 20, 30 should be 20");
    }

    @Test(description = "Test function that returns array")
    public void testReturnArray() {
        String project = "arrayProject";
        ModuleInfo moduleInfo = new ModuleInfo("testOrg", project, "1");
        Mediator mediator = new Mediator(moduleInfo);

        TestMessageContext context = TestDataBuilder.messageContext()
                .functionName("testReturnArray")
                .returnType("array")
                .addParameter("count", "int", "3")
                .build();

        mediator.mediate(context);

        String result = ((DefaultConnectorResponse) context.getVariable("result")).getPayload().toString();
        // Result should be array representation
        Assert.assertTrue(result.contains("item0"), "Should contain item0");
        Assert.assertTrue(result.contains("item1"), "Should contain item1");
        Assert.assertTrue(result.contains("item2"), "Should contain item2");
    }

    @Test(description = "Test empty array handling")
    public void testEmptyArray() {
        String project = "arrayProject";
        ModuleInfo moduleInfo = new ModuleInfo("testOrg", project, "1");
        Mediator mediator = new Mediator(moduleInfo);

        TestMessageContext context = TestDataBuilder.messageContext()
                .functionName("testEmptyArray")
                .returnType("int")
                .addParameter("items", "array", "[]")
                .addProperty("arrayElementType0", "string")
                .build();

        mediator.mediate(context);

        String result = ((DefaultConnectorResponse) context.getVariable("result")).getPayload().toString();
        Assert.assertEquals(result, "0", "Empty array should have length 0");
    }

    @Test(description = "Test large array (100 elements)")
    public void testLargeArray() {
        String project = "arrayProject";
        ModuleInfo moduleInfo = new ModuleInfo("testOrg", project, "1");
        Mediator mediator = new Mediator(moduleInfo);

        // Generate array with numbers 1-100
        StringBuilder arrayJson = new StringBuilder("[");
        for (int i = 1; i <= 100; i++) {
            if (i > 1) arrayJson.append(",");
            arrayJson.append(i);
        }
        arrayJson.append("]");

        TestMessageContext context = TestDataBuilder.messageContext()
                .functionName("testIntArray")
                .returnType("int")
                .addParameter("numbers", "array", arrayJson.toString())
                .addProperty("arrayElementType0", "int")
                .build();

        mediator.mediate(context);

        String result = ((DefaultConnectorResponse) context.getVariable("result")).getPayload().toString();
        // Sum of 1-100 = 5050
        Assert.assertEquals(result, "5050", "Sum of 1-100 should be 5050");
    }

    @Test(description = "Test function returning string array")
    public void testReturnStringArray() {
        String project = "arrayProject";
        ModuleInfo moduleInfo = new ModuleInfo("testOrg", project, "1");
        Mediator mediator = new Mediator(moduleInfo);

        TestMessageContext context = TestDataBuilder.messageContext()
                .functionName("testReturnArray")
                .returnType("array")
                .addParameter("count", "int", "3")
                .build();

        mediator.mediate(context);

        String result = ((DefaultConnectorResponse) context.getVariable("result")).getPayload().toString();
        Assert.assertEquals(result, "[\"item0\",\"item1\",\"item2\"]", "Should return JSON string array");
    }

    @Test(description = "Test function returning int array")
    public void testReturnIntArray() {
        String project = "arrayProject";
        ModuleInfo moduleInfo = new ModuleInfo("testOrg", project, "1");
        Mediator mediator = new Mediator(moduleInfo);

        TestMessageContext context = TestDataBuilder.messageContext()
                .functionName("testReturnIntArray")
                .returnType("array")
                .addParameter("max", "int", "5")
                .build();

        mediator.mediate(context);

        String result = ((DefaultConnectorResponse) context.getVariable("result")).getPayload().toString();
        Assert.assertEquals(result, "[1,2,3,4,5]", "Should return JSON int array");
    }

    @Test(description = "Test function returning boolean array")
    public void testReturnBooleanArray() {
        String project = "arrayProject";
        ModuleInfo moduleInfo = new ModuleInfo("testOrg", project, "1");
        Mediator mediator = new Mediator(moduleInfo);

        TestMessageContext context = TestDataBuilder.messageContext()
                .functionName("testReturnBooleanArray")
                .returnType("array")
                .addParameter("size", "int", "4")
                .build();

        mediator.mediate(context);

        String result = ((DefaultConnectorResponse) context.getVariable("result")).getPayload().toString();
        Assert.assertEquals(result, "[true,false,true,false]", "Should return JSON boolean array");
    }

    @Test(description = "Test function returning float array")
    public void testReturnFloatArray() {
        String project = "arrayProject";
        ModuleInfo moduleInfo = new ModuleInfo("testOrg", project, "1");
        Mediator mediator = new Mediator(moduleInfo);

        TestMessageContext context = TestDataBuilder.messageContext()
                .functionName("testReturnFloatArray")
                .returnType("array")
                .addParameter("size", "int", "3")
                .build();

        mediator.mediate(context);

        String result = ((DefaultConnectorResponse) context.getVariable("result")).getPayload().toString();
        Assert.assertEquals(result, "[0.0,1.5,3.0]", "Should return JSON float array");
    }

    /**
     * Helper class to build test MessageContext.
     * Simplified version for these tests.
     */
    static class TestDataBuilder {
        static MessageContextBuilder messageContext() {
            return new MessageContextBuilder();
        }

        static class MessageContextBuilder {
            private int paramCount = 0;
            private String functionName;
            private String returnType;
            private HashMap<String, Object> properties = new HashMap<>();
            private HashMap<Object, Object> templateParams = new HashMap<>();

            public MessageContextBuilder functionName(String name) {
                this.functionName = name;
                return this;
            }

            public MessageContextBuilder returnType(String type) {
                this.returnType = type;
                return this;
            }

            public MessageContextBuilder addParameter(String name, String type, String value) {
                properties.put("param" + paramCount, name);
                properties.put("paramType" + paramCount, type);
                templateParams.put(name, value);
                paramCount++;
                return this;
            }

            public MessageContextBuilder addProperty(String key, Object value) {
                properties.put(key, value);
                return this;
            }

            public TestMessageContext build() {
                TestMessageContext context = new TestMessageContext();

                properties.put("paramFunctionName", functionName);
                properties.put("paramSize", paramCount);
                properties.put("returnType", returnType);

                for (var entry : properties.entrySet()) {
                    context.setProperty(entry.getKey(), entry.getValue());
                }

                Stack<TemplateContext> stack = new Stack<>();
                TemplateContext templateContext = new TemplateContext("testTemplateFunc", new ArrayList<>());
                templateParams.put("responseVariable", "result");
                templateContext.setMappedValues(templateParams);
                stack.push(templateContext);
                context.setProperty("_SYNAPSE_FUNCTION_STACK", stack);

                return context;
            }
        }
    }
}
