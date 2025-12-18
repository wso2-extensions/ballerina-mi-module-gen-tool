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

public class TestRecordType {
    private static final String CONNECTION_NAME = "testRecordConnection";

    @BeforeClass
    public void setupRuntime() throws Exception {
        // First, initialize BalConnectorConfig with the correct module info
        ModuleInfo moduleInfo = new ModuleInfo("testOrg", "recordProject", "1");
        BalConnectorConfig config = new BalConnectorConfig(moduleInfo);

        // Create a context for connection initialization
        TestMessageContext initContext = ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .isConnection(true)
                .objectTypeName("RecordClient")
                .addParameter("serviceUrl", "string", "http://test.api.com")
                .addParameter("connectionType", "string", "RECORDPROJECT_RECORDCLIENT")
                .build();

        initContext.setProperty("RECORDPROJECT_RECORDCLIENT_objectTypeName", "RecordClient");
        initContext.setProperty("RECORDPROJECT_RECORDCLIENT_paramSize", 1);
        initContext.setProperty("RECORDPROJECT_RECORDCLIENT_paramFunctionName", "init");
        initContext.setProperty("RECORDPROJECT_RECORDCLIENT_param0", "serviceUrl");
        initContext.setProperty("RECORDPROJECT_RECORDCLIENT_paramType0", "string");

        // Use BalConnectorConfig to create the connection
        config.connect(initContext);
    }

    @Test(description = "Test connector with string array parameter")
    public void testSimpleRecord() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        TestMessageContext context = ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("simpleRecordFunction")
                .returnType("string")
                .addParameter("person", "record", "{\"first_name\":\"Alice\",\"last_name\":\"Bob\", \"age\":30}")
                .build();

        context.setProperty("param0", "person");
        context.setProperty("paramType0", "record");
        context.setProperty("param0_recordName", "Person");
        context.setProperty("paramFunctionName", "simpleRecordFunction");
        context.setProperty("paramSize", 1);
        context.setProperty("returnType", "string");

        connector.connect(context);

        String result = ((DefaultConnectorResponse) context.getVariable("result")).getPayload().toString();
        Assert.assertEquals(result, "Alice Bob, 30", "Record should be processed correctly by connector");
    }

    @Test(description = "Test nested record parameter")
    public void testNestedRecord() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        TestMessageContext context = ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("getUserSummary")
                .returnType("string")
                .addParameter("user", "record", "{\"name\":\"Alice\",\"address\":{\"street\":\"1 Main St\",\"city\":\"Colombo\"},\"age\":30}")
                .build();

        context.setProperty("param0", "user");
        context.setProperty("paramType0", "record");
        context.setProperty("param0_recordName", "User");
        context.setProperty("paramFunctionName", "getUserSummary");
        context.setProperty("paramSize", 1);
        context.setProperty("returnType", "string");

        connector.connect(context);

        String result = ((DefaultConnectorResponse) context.getVariable("result")).getPayload().toString();
        Assert.assertEquals(result, "Alice, Colombo, 30", "Nested record should be processed correctly");
    }

    @Test(description = "Test record with optional fields and arrays")
    public void testOptionalFieldsAndArrays() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        TestMessageContext context = ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("summarizeOrder")
                .returnType("string")
                .addParameter("order", "record", "{\"orderId\":\"ORD123\",\"items\":[{\"id\":\"i1\",\"description\":\"first\"},{\"id\":\"i2\"}]}")
                .build();

        context.setProperty("param0", "order");
        context.setProperty("paramType0", "record");
        context.setProperty("param0_recordName", "Order");
        context.setProperty("paramFunctionName", "summarizeOrder");
        context.setProperty("paramSize", 1);
        context.setProperty("returnType", "string");

        connector.connect(context);

        String result = ((DefaultConnectorResponse) context.getVariable("result")).getPayload().toString();
        Assert.assertEquals(result, "ORD123:2:no-note", "Order summary should reflect item count and missing note");
    }

    @Test(description = "Test nested arrays of records and sum prices")
    public void testNestedArraysSum() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        TestMessageContext context = ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("computeCatalogTotal")
                .returnType("float")
                .addParameter("catalog", "record", "{\"name\":\"Store\",\"categories\":[{\"name\":\"C1\",\"products\":[{\"id\":\"p1\",\"price\":10.5},{\"id\":\"p2\",\"price\":20.0}]},{\"name\":\"C2\",\"products\":[{\"id\":\"p3\",\"price\":5.5}]}]}")
                .build();

        context.setProperty("param0", "catalog");
        context.setProperty("paramType0", "record");
        context.setProperty("param0_recordName", "Catalog");
        context.setProperty("paramFunctionName", "computeCatalogTotal");
        context.setProperty("paramSize", 1);
        context.setProperty("returnType", "float");

        connector.connect(context);

        String result = ((DefaultConnectorResponse) context.getVariable("result")).getPayload().toString();
        // The connector returns float payload.toString(), compare as string for consistency with other tests
        Assert.assertEquals(result, "36.0", "Catalog total should be sum of all product prices");
    }

    @Test(description = "Test optional nested record present and absent")
    public void testOptionalNestedPresentAbsent() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        // Present
        TestMessageContext ctxPresent = ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("formatProfile")
                .returnType("string")
                .addParameter("profile", "record", "{\"id\":\"u1\",\"settings\":{\"emailOptIn\":true,\"tags\":[\"alpha\",\"beta\"]}}")
                .build();

        ctxPresent.setProperty("param0", "profile");
        ctxPresent.setProperty("paramType0", "record");
        ctxPresent.setProperty("param0_recordName", "Profile");
        ctxPresent.setProperty("paramFunctionName", "formatProfile");
        ctxPresent.setProperty("paramSize", 1);
        ctxPresent.setProperty("returnType", "string");

        connector.connect(ctxPresent);

        String resPresent = ((DefaultConnectorResponse) ctxPresent.getVariable("result")).getPayload().toString();
        Assert.assertEquals(resPresent, "u1:true:2", "Profile present should reflect emailOptIn and tag count");

        // Absent
        TestMessageContext ctxAbsent = ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("formatProfile")
                .returnType("string")
                .addParameter("profile", "record", "{\"id\":\"u2\"}")
                .build();

        ctxAbsent.setProperty("param0", "profile");
        ctxAbsent.setProperty("paramType0", "record");
        ctxAbsent.setProperty("param0_recordName", "Profile");
        ctxAbsent.setProperty("paramFunctionName", "formatProfile");
        ctxAbsent.setProperty("paramSize", 1);
        ctxAbsent.setProperty("returnType", "string");

        connector.connect(ctxAbsent);

        String resAbsent = ((DefaultConnectorResponse) ctxAbsent.getVariable("result")).getPayload().toString();
        Assert.assertEquals(resAbsent, "u2:false:0", "Absent settings should produce default false and 0 tags");
    }

    @Test(description = "Test getting person with uppercased name")
    public void testGetUppercasedPerson() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        TestMessageContext context = ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("getUppercasedPerson")
                .returnType("record")
                .addParameter("person", "record", "{\"first_name\":\"alice\",\"last_name\":\"smith\",\"age\":25}")
                .build();

        context.setProperty("param0", "person");
        context.setProperty("paramType0", "record");
        context.setProperty("param0_recordName", "Person");
        context.setProperty("paramFunctionName", "getUppercasedPerson");
        context.setProperty("paramSize", 1);
        context.setProperty("returnType", "record");

        connector.connect(context);

        Object object = ((DefaultConnectorResponse) context.getVariable("result")).getPayload();
        Assert.assertTrue(object instanceof JsonElement, "Expected result to be a JSON");

        String result = object.toString();
        Assert.assertTrue(result.contains("\"first_name\":\"ALICE\""), "First name should be uppercased");
        Assert.assertTrue(result.contains("\"last_name\":\"SMITH\""), "Last name should be uppercased");
        Assert.assertTrue(result.contains("\"age\":25"), "Age should remain unchanged");
    }
}
