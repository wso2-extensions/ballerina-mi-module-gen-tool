/*
 * Copyright (c) 2025, WSO2 LLC. (http://wso2.com)
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
import io.ballerina.stdlib.mi.BallerinaExecutionException;
import io.ballerina.stdlib.mi.ModuleInfo;
import org.apache.synapse.data.connector.DefaultConnectorResponse;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class TestUnionDataType {
    private static final String CONNECTION_NAME = "testUnionConnection";

    @BeforeClass
    public void setupRuntime() throws Exception {
        // First, initialize BalConnectorConfig with the correct module info
        ModuleInfo moduleInfo = new ModuleInfo("testOrg", "unionProject", "1");
        BalConnectorConfig config = new BalConnectorConfig(moduleInfo);

        // Create a context for connection initialization
        TestMessageContext initContext = TestArrayConnector.ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .objectTypeName("UnionClient")
                .addParameter("serviceUrl", "string", "http://test.api.com")
                .addParameter("connectionType", "string", "UNIONPROJECT_UNIONCLIENT")
                .build();

        initContext.setProperty("UNIONPROJECT_UNIONCLIENT_objectTypeName", "UnionClient");
        initContext.setProperty("UNIONPROJECT_UNIONCLIENT_paramSize", 1);
        initContext.setProperty("UNIONPROJECT_UNIONCLIENT_paramFunctionName", "init");
        initContext.setProperty("UNIONPROJECT_UNIONCLIENT_param0", "serviceUrl");
        initContext.setProperty("UNIONPROJECT_UNIONCLIENT_paramType0", "string");

        // Use BalConnectorConfig to create the connection
        config.connect(initContext);
    }

    @Test(description = "Test processing string|int union with string value")
    public void testProcessStringOrIntWithString() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        TestMessageContext context = TestArrayConnector.ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("processStringOrInt")
                .returnType("string")
                .addParameter("valueDataType", "string", "string")
                .addParameter("valueString", "string", "hello")
                .build();

        context.setProperty("param0", "value");
        context.setProperty("paramType0", "union");
        context.setProperty("param0UnionString", "valueString");
        context.setProperty("param0UnionInt", "valueInt");
        context.setProperty("paramFunctionName", "processStringOrInt");
        context.setProperty("paramSize", 1);
        context.setProperty("returnType", "string");

        connector.connect(context);

        String result = ((DefaultConnectorResponse) context.getVariable("result")).getPayload().toString();
        Assert.assertEquals(result, "string:hello", "Should process string value correctly");
    }

    @Test(description = "Test processing string|int union with int value")
    public void testProcessStringOrIntWithInt() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        TestMessageContext context = TestArrayConnector.ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("processStringOrInt")
                .returnType("string")
                .addParameter("valueDataType", "string", "int")
                .addParameter("valueInt", "int", "42")
                .build();

        context.setProperty("param0", "value");
        context.setProperty("paramType0", "union");
        context.setProperty("param0UnionString", "valueString");
        context.setProperty("param0UnionInt", "valueInt");
        context.setProperty("paramFunctionName", "processStringOrInt");
        context.setProperty("paramSize", 1);
        context.setProperty("returnType", "string");

        connector.connect(context);

        String result = ((DefaultConnectorResponse) context.getVariable("result")).getPayload().toString();
        Assert.assertEquals(result, "int:42", "Should process int value correctly");
    }

    @Test(description = "Test processing string|int|float union with float value")
    public void testProcessStringOrIntOrFloatWithFloat() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        TestMessageContext context = TestArrayConnector.ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("processStringOrIntOrFloat")
                .returnType("string")
                .addParameter("valueDataType", "string", "float")
                .addParameter("valueFloat", "float", "3.14")
                .build();

        context.setProperty("param0", "value");
        context.setProperty("paramType0", "union");
        context.setProperty("param0UnionString", "valueString");
        context.setProperty("param0UnionInt", "valueInt");
        context.setProperty("param0UnionFloat", "valueFloat");
        context.setProperty("paramFunctionName", "processStringOrIntOrFloat");
        context.setProperty("paramSize", 1);
        context.setProperty("returnType", "string");

        connector.connect(context);

        String result = ((DefaultConnectorResponse) context.getVariable("result")).getPayload().toString();
        Assert.assertEquals(result, "float:3.14", "Should process float value correctly");
    }

    @Test(description = "Test processing nullable string with value")
    public void testProcessNullableStringWithValue() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        TestMessageContext context = TestArrayConnector.ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("processNullableString")
                .returnType("string")
                .addParameter("valueDataType", "string", "string")
                .addParameter("valueString", "string", "test")
                .build();

        context.setProperty("param0", "value");
        context.setProperty("paramType0", "union");
        context.setProperty("param0UnionString", "valueString");
        context.setProperty("param0UnionNil", "valueNil");
        context.setProperty("paramFunctionName", "processNullableString");
        context.setProperty("paramSize", 1);
        context.setProperty("returnType", "string");

        connector.connect(context);

        String result = ((DefaultConnectorResponse) context.getVariable("result")).getPayload().toString();
        Assert.assertEquals(result, "value:test", "Should process string value correctly");
    }

    @Test(description = "Test processing nullable string with null")
    public void testProcessNullableStringWithNull() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        TestMessageContext context = TestArrayConnector.ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("processNullableString")
                .returnType("string")
                .addParameter("valueDataType", "string", "nil")
                .addParameter("valueNil", "nil", "")
                .build();

        context.setProperty("param0", "value");
        context.setProperty("paramType0", "union");
        context.setProperty("param0UnionString", "valueString");
        context.setProperty("param0UnionNil", "valueNil");
        context.setProperty("paramFunctionName", "processNullableString");
        context.setProperty("paramSize", 1);
        context.setProperty("returnType", "string");

        connector.connect(context);

        String result = ((DefaultConnectorResponse) context.getVariable("result")).getPayload().toString();
        Assert.assertEquals(result, "null", "Should process null value correctly");
    }

    @Test(description = "Test returning string from string|int union")
    public void testReturnStringOrIntReturnsString() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        TestMessageContext context = TestArrayConnector.ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("returnStringOrInt")
                .returnType("union")
                .addParameter("returnString", "boolean", "true")
                .build();

        context.setProperty("param0", "returnString");
        context.setProperty("paramType0", "boolean");
        context.setProperty("paramFunctionName", "returnStringOrInt");
        context.setProperty("paramSize", 1);
        context.setProperty("returnType", "union");

        connector.connect(context);

        String result = ((DefaultConnectorResponse) context.getVariable("result")).getPayload().toString();
        Assert.assertEquals(result, "hello", "Should return string value");
    }

    @Test(description = "Test returning int from string|int union")
    public void testReturnStringOrIntReturnsInt() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        TestMessageContext context = TestArrayConnector.ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("returnStringOrInt")
                .returnType("union")
                .addParameter("returnString", "boolean", "false")
                .build();

        context.setProperty("param0", "returnString");
        context.setProperty("paramType0", "boolean");
        context.setProperty("paramFunctionName", "returnStringOrInt");
        context.setProperty("paramSize", 1);
        context.setProperty("returnType", "union");

        connector.connect(context);

        String result = ((DefaultConnectorResponse) context.getVariable("result")).getPayload().toString();
        Assert.assertEquals(result, "42", "Should return int value");
    }

    @Test(description = "Test processing int|boolean union with int")
    public void testProcessIntOrBooleanWithInt() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        TestMessageContext context = TestArrayConnector.ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("processIntOrBoolean")
                .returnType("string")
                .addParameter("valueDataType", "string", "int")
                .addParameter("valueInt", "int", "100")
                .build();

        context.setProperty("param0", "value");
        context.setProperty("paramType0", "union");
        context.setProperty("param0UnionInt", "valueInt");
        context.setProperty("param0UnionBoolean", "valueBoolean");
        context.setProperty("paramFunctionName", "processIntOrBoolean");
        context.setProperty("paramSize", 1);
        context.setProperty("returnType", "string");

        connector.connect(context);

        String result = ((DefaultConnectorResponse) context.getVariable("result")).getPayload().toString();
        Assert.assertEquals(result, "int:100", "Should process int value correctly");
    }

    @Test(description = "Test processing int|boolean union with boolean")
    public void testProcessIntOrBooleanWithBoolean() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        TestMessageContext context = TestArrayConnector.ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("processIntOrBoolean")
                .returnType("string")
                .addParameter("valueDataType", "string", "boolean")
                .addParameter("valueBoolean", "boolean", "true")
                .build();

        context.setProperty("param0", "value");
        context.setProperty("paramType0", "union");
        context.setProperty("param0UnionInt", "valueInt");
        context.setProperty("param0UnionBoolean", "valueBoolean");
        context.setProperty("paramFunctionName", "processIntOrBoolean");
        context.setProperty("paramSize", 1);
        context.setProperty("returnType", "string");

        connector.connect(context);

        String result = ((DefaultConnectorResponse) context.getVariable("result")).getPayload().toString();
        Assert.assertEquals(result, "boolean:true", "Should process boolean value correctly");
    }

    @Test(description = "Test processing number union with decimal")
    public void testProcessNumberTypeWithDecimal() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        TestMessageContext context = TestArrayConnector.ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("processNumberType")
                .returnType("string")
                .addParameter("valueDataType", "string", "decimal")
                .addParameter("valueDecimal", "decimal", "99.99")
                .build();

        context.setProperty("param0", "value");
        context.setProperty("paramType0", "union");
        context.setProperty("param0UnionInt", "valueInt");
        context.setProperty("param0UnionFloat", "valueFloat");
        context.setProperty("param0UnionDecimal", "valueDecimal");
        context.setProperty("paramFunctionName", "processNumberType");
        context.setProperty("paramSize", 1);
        context.setProperty("returnType", "string");

        connector.connect(context);

        String result = ((DefaultConnectorResponse) context.getVariable("result")).getPayload().toString();
        Assert.assertEquals(result, "decimal:99.99", "Should process decimal value correctly");
    }

    @Test(description = "Test returning success response from record union")
    public void testGetResponseSuccess() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        TestMessageContext context = TestArrayConnector.ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("getResponse")
                .returnType("record")
                .addParameter("success", "boolean", "true")
                .build();

        context.setProperty("param0", "success");
        context.setProperty("paramType0", "boolean");
        context.setProperty("paramFunctionName", "getResponse");
        context.setProperty("paramSize", 1);
        context.setProperty("returnType", "record");

        connector.connect(context);

        String result = ((DefaultConnectorResponse) context.getVariable("result")).getPayload().toString();
        Assert.assertTrue(result.contains("\"status\":\"OK\""), "Should contain success status");
        Assert.assertTrue(result.contains("\"message\":\"Operation successful\""), "Should contain success message");
    }

    @Test(description = "Test returning error response from record union")
    public void testGetResponseError() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        TestMessageContext context = TestArrayConnector.ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("getResponse")
                .returnType("record")
                .addParameter("success", "boolean", "false")
                .build();

        context.setProperty("param0", "success");
        context.setProperty("paramType0", "boolean");
        context.setProperty("paramFunctionName", "getResponse");
        context.setProperty("paramSize", 1);
        context.setProperty("returnType", "record");

        connector.connect(context);

        String result = ((DefaultConnectorResponse) context.getVariable("result")).getPayload().toString();
        Assert.assertTrue(result.contains("\"errorCode\":\"ERR001\""), "Should contain error code");
        Assert.assertTrue(result.contains("\"errorMessage\":\"Operation failed\""), "Should contain error message");
    }

    @Test(description = "Test processing Person from Person|Company union")
    public void testProcessEntityWithPerson() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        TestMessageContext context = TestArrayConnector.ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("processEntity")
                .returnType("string")
                .addParameter("entity", "record", "{\"name\":\"John\",\"age\":25}")
                .build();

        context.setProperty("param0", "entity");
        context.setProperty("paramType0", "record");
        context.setProperty("param0_recordName", "Person");
        context.setProperty("paramFunctionName", "processEntity");
        context.setProperty("paramSize", 1);
        context.setProperty("returnType", "string");

        connector.connect(context);

        String result = ((DefaultConnectorResponse) context.getVariable("result")).getPayload().toString();
        Assert.assertEquals(result, "Person:John:25", "Should process Person entity correctly");
    }

    @Test(description = "Test processing Company from Person|Company union")
    public void testProcessEntityWithCompany() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        TestMessageContext context = TestArrayConnector.ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("processEntity")
                .returnType("string")
                .addParameter("entity", "record", "{\"companyName\":\"WSO2\",\"employeeCount\":1000}")
                .build();

        context.setProperty("param0", "entity");
        context.setProperty("paramType0", "record");
        context.setProperty("param0_recordName", "Company");
        context.setProperty("paramFunctionName", "processEntity");
        context.setProperty("paramSize", 1);
        context.setProperty("returnType", "string");

        connector.connect(context);

        String result = ((DefaultConnectorResponse) context.getVariable("result")).getPayload().toString();
        Assert.assertEquals(result, "Company:WSO2:1000", "Should process Company entity correctly");
    }

    @Test(description = "Test finding person returns Person record")
    public void testFindPersonFound() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        TestMessageContext context = TestArrayConnector.ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("findPerson")
                .returnType("record")
                .addParameter("name", "string", "Alice")
                .build();

        context.setProperty("param0", "name");
        context.setProperty("paramType0", "string");
        context.setProperty("paramFunctionName", "findPerson");
        context.setProperty("paramSize", 1);
        context.setProperty("returnType", "record");

        connector.connect(context);

        String result = ((DefaultConnectorResponse) context.getVariable("result")).getPayload().toString();
        Assert.assertTrue(result.contains("\"name\":\"Alice\""), "Should contain name");
        Assert.assertTrue(result.contains("\"age\":30"), "Should contain age");
    }

    @Test(description = "Test finding person returns nil")
    public void testFindPersonNotFound() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        TestMessageContext context = TestArrayConnector.ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("findPerson")
                .returnType("record")
                .addParameter("name", "string", "Bob")
                .build();

        context.setProperty("param0", "name");
        context.setProperty("paramType0", "string");
        context.setProperty("paramFunctionName", "findPerson");
        context.setProperty("paramSize", 1);
        context.setProperty("returnType", "record");

        connector.connect(context);

        Object result = ((DefaultConnectorResponse) context.getVariable("result")).getPayload();
        Assert.assertTrue(result == null || result.toString().equals("null"), "Should return null for not found");
    }

    @Test(description = "Test processing string|int|() union with nil")
    public void testProcessWithNilValue() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        TestMessageContext context = TestArrayConnector.ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("processWithNil")
                .returnType("string")
                .build();

        context.setProperty("param0", "value");
        context.setProperty("paramType0", "union");
        context.setProperty("param0UnionString", "valueString");
        context.setProperty("param0UnionInt", "valueInt");
        context.setProperty("param0UnionNil", "valueNil");
        context.setProperty("paramFunctionName", "processWithNil");
        context.setProperty("paramSize", 1);
        context.setProperty("returnType", "string");

        connector.connect(context);

        String result = ((DefaultConnectorResponse) context.getVariable("result")).getPayload().toString();
        Assert.assertEquals(result, "nil", "Should process nil value correctly");
    }

    @Test(description = "Test processing string|error union with error thrown")
    public void testProcessWithErrorThrown() {
        BalConnectorFunction connector = new BalConnectorFunction();

        TestMessageContext context = TestArrayConnector.ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("processWithError")
                .returnType("union")
                .addParameter("shouldThrowError", "boolean", "true")
                .build();

        context.setProperty("param0", "shouldThrowError");
        context.setProperty("paramType0", "boolean");
        context.setProperty("paramFunctionName", "processWithError");
        context.setProperty("paramSize", 1);
        context.setProperty("returnType", "union");

        try {
            connector.connect(context);
            Assert.fail("Expected an exception to be thrown");
        } catch (Throwable t) {
            String msg = t.getMessage() == null ? "" : t.getMessage();
            Assert.assertEquals(msg, "Error while executing ballerina");
            Assert.assertEquals(t.getCause().getClass(), BallerinaExecutionException.class,
                    "Cause should be BallerinaExecutionException");
            Assert.assertEquals(t.getCause().getMessage(), "Operation failed: Invalid input provided",
                    "BallerinaExecutionException message should match");
        }
    }

    @Test(description = "Test processing string|error union with success")
    public void testProcessWithErrorSuccess() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        TestMessageContext context = TestArrayConnector.ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("processWithError")
                .returnType("union")
                .addParameter("shouldThrowError", "boolean", "false")
                .build();

        context.setProperty("param0", "shouldThrowError");
        context.setProperty("paramType0", "boolean");
        context.setProperty("paramFunctionName", "processWithError");
        context.setProperty("paramSize", 1);
        context.setProperty("returnType", "union");

        connector.connect(context);

        String result = ((DefaultConnectorResponse) context.getVariable("result")).getPayload().toString();
        Assert.assertEquals(result, "Success", "Should return success string when no error");
    }
}
