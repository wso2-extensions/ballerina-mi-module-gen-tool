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

import com.google.gson.JsonElement;
import io.ballerina.stdlib.mi.BalConnectorConfig;
import io.ballerina.stdlib.mi.BalConnectorFunction;
import io.ballerina.stdlib.mi.ModuleInfo;
import org.apache.synapse.data.connector.DefaultConnectorResponse;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class TestJsonType {
    private static final String CONNECTION_NAME = "testJsonConnection";

    @BeforeClass
    public void setupRuntime() throws Exception {
        // First, initialize BalConnectorConfig with the correct module info
        ModuleInfo moduleInfo = new ModuleInfo("testOrg", "jsonProject", "1");
        BalConnectorConfig config = new BalConnectorConfig(moduleInfo);

        // Create a context for connection initialization
        TestMessageContext initContext = ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .isConnection(true)
                .objectTypeName("JsonClient")
                .addParameter("serviceUrl", "string", "http://test.api.com")
                .addParameter("connectionType", "string", "JSONPROJECT_JSONCLIENT")
                .build();

        initContext.setProperty("JSONPROJECT_JSONCLIENT_objectTypeName", "JsonClient");
        initContext.setProperty("JSONPROJECT_JSONCLIENT_paramSize", 1);
        initContext.setProperty("JSONPROJECT_JSONCLIENT_paramFunctionName", "init");
        initContext.setProperty("JSONPROJECT_JSONCLIENT_param0", "serviceUrl");
        initContext.setProperty("JSONPROJECT_JSONCLIENT_paramType0", "string");

        // Use BalConnectorConfig to create the connection
        config.connect(initContext);
    }

    @Test(description = "Test getting JSON as string")
    public void testGetJsonAsString() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        TestMessageContext context = ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("getJsonAsString")
                .returnType("string")
                .addParameter("data", "json", "{\"name\":\"Alice\",\"age\":30}")
                .build();

        context.setProperty("param0", "data");
        context.setProperty("paramType0", "json");
        context.setProperty("paramFunctionName", "getJsonAsString");
        context.setProperty("paramSize", 1);
        context.setProperty("returnType", "string");

        connector.connect(context);

        String result = ((DefaultConnectorResponse) context.getVariable("result")).getPayload().toString();
        Assert.assertTrue(result.contains("name") && result.contains("Alice"), "JSON should be converted to string");
    }

    @Test(description = "Test creating JSON object")
    public void testCreateJsonObject() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        TestMessageContext context = ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("createJsonObject")
                .returnType("json")
                .addParameter("name", "string", "Bob")
                .addParameter("age", "int", "25")
                .build();

        context.setProperty("param0", "name");
        context.setProperty("paramType0", "string");
        context.setProperty("param1", "age");
        context.setProperty("paramType1", "int");
        context.setProperty("paramFunctionName", "createJsonObject");
        context.setProperty("paramSize", 2);
        context.setProperty("returnType", "json");

        connector.connect(context);

        String result = ((DefaultConnectorResponse) context.getVariable("result")).getPayload().toString();
        Assert.assertTrue(result.contains("\"name\":\"Bob\""), "JSON should contain name field");
        Assert.assertTrue(result.contains("\"age\":25"), "JSON should contain age field");
        Assert.assertTrue(result.contains("\"active\":true"), "JSON should contain active field");
    }

    @Test(description = "Test merging two JSON objects")
    public void testMergeJson() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        TestMessageContext context = ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("mergeJson")
                .returnType("json")
                .addParameter("obj1", "json", "{\"name\":\"Alice\"}")
                .addParameter("obj2", "json", "{\"age\":30}")
                .build();

        context.setProperty("param0", "obj1");
        context.setProperty("paramType0", "json");
        context.setProperty("param1", "obj2");
        context.setProperty("paramType1", "json");
        context.setProperty("paramFunctionName", "mergeJson");
        context.setProperty("paramSize", 2);
        context.setProperty("returnType", "json");

        connector.connect(context);

        String result = ((DefaultConnectorResponse) context.getVariable("result")).getPayload().toString();
        Assert.assertTrue(result.contains("name") && result.contains("Alice"), "Merged JSON should contain name");
        Assert.assertTrue(result.contains("age") && result.contains("30"), "Merged JSON should contain age");
    }

    @Test(description = "Test getting JSON array length")
    public void testGetArrayLength() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        TestMessageContext context = ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("getArrayLength")
                .returnType("int")
                .addParameter("arr", "json", "[1, 2, 3, 4, 5]")
                .build();

        context.setProperty("param0", "arr");
        context.setProperty("paramType0", "json");
        context.setProperty("paramFunctionName", "getArrayLength");
        context.setProperty("paramSize", 1);
        context.setProperty("returnType", "int");

        connector.connect(context);

        String result = ((DefaultConnectorResponse) context.getVariable("result")).getPayload().toString();
        Assert.assertEquals(result, "5", "Array length should be 5");
    }

    @Test(description = "Test transforming JSON - uppercase string values")
    public void testTransformJson() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        TestMessageContext context = ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("transformJson")
                .returnType("json")
                .addParameter("data", "json", "{\"name\":\"alice\",\"city\":\"colombo\",\"age\":25}")
                .build();

        context.setProperty("param0", "data");
        context.setProperty("paramType0", "json");
        context.setProperty("paramFunctionName", "transformJson");
        context.setProperty("paramSize", 1);
        context.setProperty("returnType", "json");

        connector.connect(context);

        Object resultObj = ((DefaultConnectorResponse) context.getVariable("result")).getPayload();
        Assert.assertTrue(resultObj instanceof JsonElement, "Result should be a JSON element");

        String result = resultObj.toString();
        Assert.assertTrue(result.contains("\"name\":\"ALICE\""), "Name should be uppercased");
        Assert.assertTrue(result.contains("\"city\":\"COLOMBO\""), "City should be uppercased");
        Assert.assertTrue(result.contains("\"age\":25"), "Age should remain unchanged");
    }

    @Test(description = "Test getting nested JSON value")
    public void testGetNestedValue() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        TestMessageContext context = ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("getNestedValue")
                .returnType("string")
                .addParameter("data", "json", "{\"person\":{\"name\":\"John\",\"age\":30}}")
                .build();

        context.setProperty("param0", "data");
        context.setProperty("paramType0", "json");
        context.setProperty("paramFunctionName", "getNestedValue");
        context.setProperty("paramSize", 1);
        context.setProperty("returnType", "string");

        connector.connect(context);

        String result = ((DefaultConnectorResponse) context.getVariable("result")).getPayload().toString();
        Assert.assertEquals(result, "John", "Should extract nested name value");
    }

    @Test(description = "Test summing values from JSON array")
    public void testSumJsonArray() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        TestMessageContext context = ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("sumJsonArray")
                .returnType("int")
                .addParameter("arr", "json", "[10, 20, 30, 40]")
                .build();

        context.setProperty("param0", "arr");
        context.setProperty("paramType0", "json");
        context.setProperty("paramFunctionName", "sumJsonArray");
        context.setProperty("paramSize", 1);
        context.setProperty("returnType", "int");

        connector.connect(context);

        String result = ((DefaultConnectorResponse) context.getVariable("result")).getPayload().toString();
        Assert.assertEquals(result, "100", "Sum of array should be 100");
    }

    @Test(description = "Test checking if JSON has a key")
    public void testHasKey() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        // Test with existing key
        TestMessageContext contextExists = ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("hasKey")
                .returnType("boolean")
                .addParameter("data", "json", "{\"name\":\"Alice\",\"age\":30}")
                .addParameter("key", "string", "name")
                .build();

        contextExists.setProperty("param0", "data");
        contextExists.setProperty("paramType0", "json");
        contextExists.setProperty("param1", "key");
        contextExists.setProperty("paramType1", "string");
        contextExists.setProperty("paramFunctionName", "hasKey");
        contextExists.setProperty("paramSize", 2);
        contextExists.setProperty("returnType", "boolean");

        connector.connect(contextExists);

        String resultExists = ((DefaultConnectorResponse) contextExists.getVariable("result")).getPayload().toString();
        Assert.assertEquals(resultExists, "true", "Should return true for existing key");

        // Test with non-existing key
        TestMessageContext contextNotExists = ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("hasKey")
                .returnType("boolean")
                .addParameter("data", "json", "{\"name\":\"Alice\",\"age\":30}")
                .addParameter("key", "string", "email")
                .build();

        contextNotExists.setProperty("param0", "data");
        contextNotExists.setProperty("paramType0", "json");
        contextNotExists.setProperty("param1", "key");
        contextNotExists.setProperty("paramType1", "string");
        contextNotExists.setProperty("paramFunctionName", "hasKey");
        contextNotExists.setProperty("paramSize", 2);
        contextNotExists.setProperty("returnType", "boolean");

        connector.connect(contextNotExists);

        String resultNotExists = ((DefaultConnectorResponse) contextNotExists.getVariable("result")).getPayload().toString();
        Assert.assertEquals(resultNotExists, "false", "Should return false for non-existing key");
    }

    @Test(description = "Test getting JSON array element at index")
    public void testGetArrayElement() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        TestMessageContext context = ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("getArrayElement")
                .returnType("json")
                .addParameter("arr", "json", "[\"apple\", \"banana\", \"cherry\"]")
                .addParameter("index", "int", "1")
                .build();

        context.setProperty("param0", "arr");
        context.setProperty("paramType0", "json");
        context.setProperty("param1", "index");
        context.setProperty("paramType1", "int");
        context.setProperty("paramFunctionName", "getArrayElement");
        context.setProperty("paramSize", 2);
        context.setProperty("returnType", "json");

        connector.connect(context);

        String result = ((DefaultConnectorResponse) context.getVariable("result")).getPayload().toString();
        Assert.assertEquals(result, "banana", "Should return element at index 1");
    }

    @Test(description = "Test creating nested JSON object")
    public void testCreateNestedJson() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        TestMessageContext context = ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("createNestedJson")
                .returnType("json")
                .addParameter("name", "string", "Alice")
                .addParameter("city", "string", "Colombo")
                .addParameter("country", "string", "Sri Lanka")
                .build();

        context.setProperty("param0", "name");
        context.setProperty("paramType0", "string");
        context.setProperty("param1", "city");
        context.setProperty("paramType1", "string");
        context.setProperty("param2", "country");
        context.setProperty("paramType2", "string");
        context.setProperty("paramFunctionName", "createNestedJson");
        context.setProperty("paramSize", 3);
        context.setProperty("returnType", "json");

        connector.connect(context);

        String result = ((DefaultConnectorResponse) context.getVariable("result")).getPayload().toString();
        Assert.assertTrue(result.contains("\"name\":\"Alice\""), "Should contain name");
        Assert.assertTrue(result.contains("\"city\":\"Colombo\""), "Should contain city");
        Assert.assertTrue(result.contains("\"country\":\"Sri Lanka\""), "Should contain country");
    }

    @Test(description = "Test filtering JSON array by status")
    public void testFilterJsonArray() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        String jsonArray = "[{\"id\":1,\"status\":\"active\"},{\"id\":2,\"status\":\"inactive\"},{\"id\":3,\"status\":\"active\"}]";
        TestMessageContext context = ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("filterJsonArray")
                .returnType("json")
                .addParameter("arr", "json", jsonArray)
                .addParameter("status", "string", "active")
                .build();

        context.setProperty("param0", "arr");
        context.setProperty("paramType0", "json");
        context.setProperty("param1", "status");
        context.setProperty("paramType1", "string");
        context.setProperty("paramFunctionName", "filterJsonArray");
        context.setProperty("paramSize", 2);
        context.setProperty("returnType", "json");

        connector.connect(context);

        String result = ((DefaultConnectorResponse) context.getVariable("result")).getPayload().toString();
        Assert.assertTrue(result.contains("\"id\":1"), "Should contain first active item");
        Assert.assertTrue(result.contains("\"id\":3"), "Should contain third active item");
        Assert.assertFalse(result.contains("\"id\":2"), "Should not contain inactive item");
    }

    @Test(description = "Test counting keys in JSON object")
    public void testCountKeys() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        TestMessageContext context = ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("countKeys")
                .returnType("int")
                .addParameter("data", "json", "{\"name\":\"Alice\",\"age\":30,\"city\":\"Colombo\",\"active\":true}")
                .build();

        context.setProperty("param0", "data");
        context.setProperty("paramType0", "json");
        context.setProperty("paramFunctionName", "countKeys");
        context.setProperty("paramSize", 1);
        context.setProperty("returnType", "int");

        connector.connect(context);

        String result = ((DefaultConnectorResponse) context.getVariable("result")).getPayload().toString();
        Assert.assertEquals(result, "4", "Should count 4 keys");
    }
}
