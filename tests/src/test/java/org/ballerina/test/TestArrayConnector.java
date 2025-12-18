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

import io.ballerina.stdlib.mi.BalConnectorConfig;
import io.ballerina.stdlib.mi.BalConnectorFunction;
import io.ballerina.stdlib.mi.ModuleInfo;
import org.apache.synapse.data.connector.DefaultConnectorResponse;
import org.apache.synapse.mediators.template.TemplateContext;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

/**
 * Tests for array type support in BalConnectorFunction (connectors).
 * Tests Ballerina client class remote functions that accept and return arrays.
 */
public class TestArrayConnector {

    private static final String CONNECTION_NAME = "testArrayConnection";

    @BeforeClass
    public void setupRuntime() throws Exception {
        // First, initialize BalConnectorConfig with the correct module info
        ModuleInfo moduleInfo = new ModuleInfo("testOrg", "arrayProject", "1");
        BalConnectorConfig config = new BalConnectorConfig(moduleInfo);

        // Create a context for connection initialization
        TestMessageContext initContext = ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .isConnection(true)
                .addParameter("apiUrl", "string", "http://test.api.com")
                .addParameter("connectionType", "string", "ARRAYPROJECT_ARRAYCLIENT")
                .build();

        initContext.setProperty("ARRAYPROJECT_ARRAYCLIENT_objectTypeName", "ArrayClient");
        initContext.setProperty("ARRAYPROJECT_ARRAYCLIENT_paramSize", 1);
        initContext.setProperty("ARRAYPROJECT_ARRAYCLIENT_paramFunctionName", "init");
        initContext.setProperty("ARRAYPROJECT_ARRAYCLIENT_param0", "apiUrl");
        initContext.setProperty("ARRAYPROJECT_ARRAYCLIENT_paramType0", "string");

        // Use BalConnectorConfig to create the connection
        config.connect(initContext);
    }

    @Test(description = "Test connector with string array parameter")
    public void testConnectorStringArray() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        TestMessageContext context = ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("processStringArray")
                .returnType("string")
                .addParameter("names", "array", "[\"Alice\", \"Bob\", \"Charlie\"]")
                .addProperty("arrayElementType0", "string")
                .build();

        connector.connect(context);

        String result = ((DefaultConnectorResponse) context.getVariable("result")).getPayload().toString();
        Assert.assertEquals(result, "Alice,Bob,Charlie,", "String array should be processed correctly by connector");
    }

    @Test(description = "Test connector with int array parameter")
    public void testConnectorIntArray() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        TestMessageContext context = ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("processIntArray")
                .returnType("int")
                .addParameter("numbers", "array", "[10, 20, 30, 40]")
                .addProperty("arrayElementType0", "int")
                .build();

        connector.connect(context);

        String result = ((DefaultConnectorResponse) context.getVariable("result")).getPayload().toString();
        Assert.assertEquals(result, "100", "Int array sum should be 100");
    }

    @Test(description = "Test connector with boolean array - all true")
    public void testConnectorBooleanArrayAllTrue() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        TestMessageContext context = ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("processBooleanArray")
                .returnType("boolean")
                .addParameter("flags", "array", "[true, true, true]")
                .addProperty("arrayElementType0", "boolean")
                .build();

        connector.connect(context);

        String result = ((DefaultConnectorResponse) context.getVariable("result")).getPayload().toString();
        Assert.assertEquals(result, "true", "All true flags should return true");
    }

    @Test(description = "Test connector with boolean array - contains false")
    public void testConnectorBooleanArrayWithFalse() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        TestMessageContext context = ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("processBooleanArray")
                .returnType("boolean")
                .addParameter("flags", "array", "[true, false, true]")
                .addProperty("arrayElementType0", "boolean")
                .build();

        connector.connect(context);

        String result = ((DefaultConnectorResponse) context.getVariable("result")).getPayload().toString();
        Assert.assertEquals(result, "false", "Array with false should return false");
    }

    @Test(description = "Test connector with float array - calculate average")
    public void testConnectorFloatArray() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        TestMessageContext context = ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("processFloatArray")
                .returnType("float")
                .addParameter("values", "array", "[5.0, 10.0, 15.0, 20.0]")
                .addProperty("arrayElementType0", "float")
                .build();

        connector.connect(context);

        String result = ((DefaultConnectorResponse) context.getVariable("result")).getPayload().toString();
        Assert.assertEquals(result, "12.5", "Average of 5, 10, 15, 20 should be 12.5");
    }

    @Test(description = "Test connector with empty array")
    public void testConnectorEmptyArray() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        TestMessageContext context = ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("getArrayLength")
                .returnType("int")
                .addParameter("items", "array", "[]")
                .addProperty("arrayElementType0", "string")
                .build();

        connector.connect(context);

        String result = ((DefaultConnectorResponse) context.getVariable("result")).getPayload().toString();
        Assert.assertEquals(result, "0", "Empty array should have length 0");
    }

    @Test(description = "Test connector with large array (50 elements)")
    public void testConnectorLargeArray() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        // Generate array with numbers 1-50
        StringBuilder arrayJson = new StringBuilder("[");
        for (int i = 1; i <= 50; i++) {
            if (i > 1) arrayJson.append(",");
            arrayJson.append(i);
        }
        arrayJson.append("]");

        TestMessageContext context = ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("processIntArray")
                .returnType("int")
                .addParameter("numbers", "array", arrayJson.toString())
                .addProperty("arrayElementType0", "int")
                .build();

        connector.connect(context);

        String result = ((DefaultConnectorResponse) context.getVariable("result")).getPayload().toString();
        // Sum of 1-50 = 1275
        Assert.assertEquals(result, "1275", "Sum of 1-50 should be 1275");
    }

    @Test(description = "Test connector with single element array")
    public void testConnectorSingleElementArray() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        TestMessageContext context = ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("processStringArray")
                .returnType("string")
                .addParameter("names", "array", "[\"OnlyOne\"]")
                .addProperty("arrayElementType0", "string")
                .build();

        connector.connect(context);

        String result = ((DefaultConnectorResponse) context.getVariable("result")).getPayload().toString();
        Assert.assertEquals(result, "OnlyOne,", "Single element array should be processed correctly");
    }

    @Test(description = "Test connector returning string array")
    public void testConnectorReturnStringArray() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        TestMessageContext context = ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("generateStringArray")
                .returnType("array")
                .addParameter("count", "int", "3")
                .build();

        connector.connect(context);

        String result = ((DefaultConnectorResponse) context.getVariable("result")).getPayload().toString();
        Assert.assertEquals(result, "[\"item0\",\"item1\",\"item2\"]", "Should return JSON string array");
    }

    @Test(description = "Test connector returning int array")
    public void testConnectorReturnIntArray() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        TestMessageContext context = ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("generateIntArray")
                .returnType("array")
                .addParameter("max", "int", "5")
                .build();

        connector.connect(context);

        String result = ((DefaultConnectorResponse) context.getVariable("result")).getPayload().toString();
        Assert.assertEquals(result, "[1,2,3,4,5]", "Should return JSON int array");
    }

    @Test(description = "Test connector returning boolean array")
    public void testConnectorReturnBooleanArray() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        TestMessageContext context = ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("generateBooleanArray")
                .returnType("array")
                .addParameter("size", "int", "4")
                .build();

        connector.connect(context);

        String result = ((DefaultConnectorResponse) context.getVariable("result")).getPayload().toString();
        Assert.assertEquals(result, "[true,false,true,false]", "Should return JSON boolean array");
    }

    @Test(description = "Test connector returning float array")
    public void testConnectorReturnFloatArray() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        TestMessageContext context = ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("generateFloatArray")
                .returnType("array")
                .addParameter("size", "int", "3")
                .build();

        connector.connect(context);

        String result = ((DefaultConnectorResponse) context.getVariable("result")).getPayload().toString();
        Assert.assertEquals(result, "[0.0,1.5,3.0]", "Should return JSON float array");
    }

    @Test(description = "Test connector returning empty array")
    public void testConnectorReturnEmptyArray() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        TestMessageContext context = ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("generateStringArray")
                .returnType("array")
                .addParameter("count", "int", "0")
                .build();

        connector.connect(context);

        String result = ((DefaultConnectorResponse) context.getVariable("result")).getPayload().toString();
        Assert.assertEquals(result, "[]", "Should return empty JSON array");
    }
}
