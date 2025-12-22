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

public class TestMapType {
    private static final String CONNECTION_NAME = "testMapConnection";

    @BeforeClass
    public void setupRuntime() throws Exception {
        // First, initialize BalConnectorConfig with the correct module info
        ModuleInfo moduleInfo = new ModuleInfo("testOrg", "mapProject", "1");
        BalConnectorConfig config = new BalConnectorConfig(moduleInfo);

        // Create a context for connection initialization
        TestMessageContext initContext = ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .isConnection(true)
                .objectTypeName("MapClient")
                .addParameter("serviceUrl", "string", "http://test.api.com")
                .addParameter("connectionType", "string", "MAPPROJECT_MAPCLIENT")
                .build();

        initContext.setProperty("MAPPROJECT_MAPCLIENT_objectTypeName", "MapClient");
        initContext.setProperty("MAPPROJECT_MAPCLIENT_paramSize", 1);
        initContext.setProperty("MAPPROJECT_MAPCLIENT_paramFunctionName", "init");
        initContext.setProperty("MAPPROJECT_MAPCLIENT_param0", "serviceUrl");
        initContext.setProperty("MAPPROJECT_MAPCLIENT_paramType0", "string");

        // Use BalConnectorConfig to create the connection
        config.connect(initContext);
    }

    @Test(description = "Test connector with simple map parameter")
    public void testSimpleMap() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        TestMessageContext context = ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("simpleRecordFunction")
                .returnType("string")
                .addParameter("personMap", "map", "{\"first_name\":\"Alice\",\"last_name\":\"Bob\", \"age\":30}")
                .build();

        context.setProperty("param0", "personMap");
        context.setProperty("paramType0", "map");
        context.setProperty("paramFunctionName", "simpleRecordFunction");
        context.setProperty("paramSize", 1);
        context.setProperty("returnType", "string");

        connector.connect(context);

        String result = ((DefaultConnectorResponse) context.getVariable("result")).getPayload().toString();
        Assert.assertEquals(result, "Alice Bob, 30", "Map should be processed correctly by connector");
    }

    @Test(description = "Test nested map parameter")
    public void testNestedMap() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        TestMessageContext context = ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("getUserSummary")
                .returnType("string")
                .addParameter("userMap", "map", "{\"name\":\"Alice\",\"address\":{\"street\":\"1 Main St\",\"city\":\"Colombo\"},\"age\":30}")
                .build();

        context.setProperty("param0", "userMap");
        context.setProperty("paramType0", "map");
        context.setProperty("paramFunctionName", "getUserSummary");
        context.setProperty("paramSize", 1);
        context.setProperty("returnType", "string");

        connector.connect(context);

        String result = ((DefaultConnectorResponse) context.getVariable("result")).getPayload().toString();
        Assert.assertEquals(result, "Alice, Colombo, 30", "Nested map should be processed correctly");
    }

    @Test(description = "Test map with optional fields and arrays")
    public void testOptionalFieldsAndArrays() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        TestMessageContext context = ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("summarizeOrder")
                .returnType("string")
                .addParameter("orderMap", "map", "{\"orderId\":\"ORD123\",\"items\":[{\"itemId\":\"i1\",\"quantity\":1},{\"itemId\":\"i2\",\"quantity\":1}],\"note\":null}")
                .build();

        context.setProperty("param0", "orderMap");
        context.setProperty("paramType0", "map");
        context.setProperty("paramFunctionName", "summarizeOrder");
        context.setProperty("paramSize", 1);
        context.setProperty("returnType", "string");

        connector.connect(context);

        String result = ((DefaultConnectorResponse) context.getVariable("result")).getPayload().toString();
        Assert.assertEquals(result, "ORD123:2:no-note", "Order summary should reflect item count and missing note");
    }

    @Test(description = "Test nested arrays of records in map and sum prices")
    public void testNestedArraysSum() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        TestMessageContext context = ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("computeCatalogTotal")
                .returnType("float")
                .addParameter("catalogMap", "map", "{\"categories\":[{\"categoryName\":\"C1\",\"products\":[{\"productId\":\"p1\",\"price\":10.5},{\"productId\":\"p2\",\"price\":20.0}]},{\"categoryName\":\"C2\",\"products\":[{\"productId\":\"p3\",\"price\":5.5}]}]}")
                .build();

        context.setProperty("param0", "catalogMap");
        context.setProperty("paramType0", "map");
        context.setProperty("paramFunctionName", "computeCatalogTotal");
        context.setProperty("paramSize", 1);
        context.setProperty("returnType", "float");

        connector.connect(context);

        String result = ((DefaultConnectorResponse) context.getVariable("result")).getPayload().toString();
        // The connector returns float payload.toString(), compare as string for consistency with other tests
        Assert.assertEquals(result, "36.0", "Catalog total should be sum of all product prices");
    }

    @Test(description = "Test optional nested map present and absent")
    public void testOptionalNestedPresentAbsent() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        // Present
        TestMessageContext ctxPresent = ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("formatProfile")
                .returnType("string")
                .addParameter("profileMap", "map", "{\"id\":\"u1\",\"settings\":{\"emailOptIn\":true,\"tags\":[\"alpha\",\"beta\"]}}")
                .build();

        ctxPresent.setProperty("param0", "profileMap");
        ctxPresent.setProperty("paramType0", "map");
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
                .addParameter("profileMap", "map", "{\"id\":\"u2\",\"settings\":null}")
                .build();

        ctxAbsent.setProperty("param0", "profileMap");
        ctxAbsent.setProperty("paramType0", "map");
        ctxAbsent.setProperty("paramFunctionName", "formatProfile");
        ctxAbsent.setProperty("paramSize", 1);
        ctxAbsent.setProperty("returnType", "string");

        connector.connect(ctxAbsent);

        String resAbsent = ((DefaultConnectorResponse) ctxAbsent.getVariable("result")).getPayload().toString();
        Assert.assertEquals(resAbsent, "u2:false:0", "Absent settings should produce default false and 0 tags");
    }

    @Test(description = "Test getting person with uppercased name from map")
    public void testGetUppercasedPerson() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        TestMessageContext context = ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("getUppercasedPerson")
                .returnType("record")
                .addParameter("personMap", "map", "{\"first_name\":\"alice\",\"last_name\":\"smith\",\"age\":25}")
                .build();

        context.setProperty("param0", "personMap");
        context.setProperty("paramType0", "map");
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
