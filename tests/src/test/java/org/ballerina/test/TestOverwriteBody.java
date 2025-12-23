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
import io.ballerina.stdlib.mi.ModuleInfo;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Test class for testing all 9 combinations between JSON, XML, and Text formats.
 * - JSON -> JSON, JSON -> XML, JSON -> Text
 * - XML -> JSON, XML -> XML, XML -> Text
 * - Text -> JSON, Text -> XML, Text -> Text
 */
public class TestOverwriteBody {

    private static final String CONNECTION_NAME = "OVERWRITEBODYPROJECT_OVERWRITEBODYCLIENT";

    @BeforeClass
    public void setupRuntime() throws Exception {
        // Initialize BalConnectorConfig with the correct module info
        ModuleInfo moduleInfo = new ModuleInfo("testOrg", "overwriteBodyProject", "1");
        BalConnectorConfig config = new BalConnectorConfig(moduleInfo);

        // Create a context for connection initialization
        TestMessageContext initContext = ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .isConnection(true)
                .addParameter("serviceUrl", "string", "http://test.api.com")
                .addParameter("connectionType", "string", CONNECTION_NAME)
                .build();

        initContext.setProperty(CONNECTION_NAME + "_objectTypeName", "OverwriteBodyClient");
        initContext.setProperty(CONNECTION_NAME + "_paramSize", 1);
        initContext.setProperty(CONNECTION_NAME + "_paramFunctionName", "init");
        initContext.setProperty(CONNECTION_NAME + "_param0", "serviceUrl");
        initContext.setProperty(CONNECTION_NAME + "_paramType0", "string");

        // Use BalConnectorConfig to create the connection
        config.connect(initContext);
    }

    // ==================== JSON Input Tests ====================

    @Test(description = "Test JSON request -> JSON response")
    public void testJsonToJson() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        TestMessageContext context = ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("jsonToJson")
                .returnType("json")
                .addParameter("jsonInput", "json", "{\"name\":\"John\",\"age\":30}")
                .addParameter("overwriteBody", "boolean", "true")
                .build();

        connector.connect(context);

        String result = context.getEnvelope().getBody().toString();
        Assert.assertTrue(result.contains("name") && result.contains("John"),
                "JSON output should contain name field");
        Assert.assertTrue(result.contains("age") && result.contains("30"),
                "JSON output should contain age field");
    }

    @Test(description = "Test JSON request -> XML response")
    public void testJsonToXml() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        TestMessageContext context = ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("jsonToXml")
                .returnType("xml")
                .addParameter("jsonInput", "json", "{\"name\":\"John\",\"age\":30}")
                .addParameter("overwriteBody", "boolean", "true")
                .build();

        connector.connect(context);

        String result = context.getEnvelope().getBody().toString();
        Assert.assertTrue(result.contains("<name>John</name>"), "XML output should contain name element");
        Assert.assertTrue(result.contains("<age>30</age>"), "XML output should contain age element");
    }

    @Test(description = "Test JSON request -> Text response")
    public void testJsonToText() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        TestMessageContext context = ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("jsonToText")
                .returnType("string")
                .addParameter("jsonInput", "json", "{\"key\":\"value\",\"number\":42}")
                .addParameter("overwriteBody", "boolean", "true")
                .build();

        connector.connect(context);

        String result = context.getEnvelope().getBody().toString();
        Assert.assertTrue(result.contains("key") && result.contains("value"),
                "Text output should contain JSON content");
    }

    // ==================== XML Input Tests ====================

    @Test(description = "Test XML request -> JSON response")
    public void testXmlToJson() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        String xmlInput = "<root><name>Alice</name><city>NYC</city></root>";

        TestMessageContext context = ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("xmlToJson")
                .returnType("json")
                .addParameter("xmlInput", "xml", xmlInput)
                .addParameter("overwriteBody", "boolean", "true")
                .build();

        connector.connect(context);

        String result = context.getEnvelope().getBody().toString();
        Assert.assertTrue(result.contains("name") && result.contains("Alice"),
                "JSON output should contain name field with value Alice");
        Assert.assertTrue(result.contains("city") && result.contains("NYC"),
                "JSON output should contain city field with value NYC");
    }

    @Test(description = "Test XML request -> XML response")
    public void testXmlToXml() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        String xmlInput = "<root><name>Alice</name><city>NYC</city></root>";

        TestMessageContext context = ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("xmlToXml")
                .returnType("xml")
                .addParameter("xmlInput", "xml", xmlInput)
                .addParameter("overwriteBody", "boolean", "true")
                .build();

        connector.connect(context);

        String result = context.getEnvelope().getBody().toString();
        Assert.assertTrue(result.contains("<name>Alice</name>"),
                "XML output should contain name element");
        Assert.assertTrue(result.contains("<city>NYC</city>"),
                "XML output should contain city element");
    }

    @Test(description = "Test XML request -> Text response")
    public void testXmlToText() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        String xmlInput = "<greeting>Hello World</greeting>";

        TestMessageContext context = ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("xmlToText")
                .returnType("string")
                .addParameter("xmlInput", "xml", xmlInput)
                .addParameter("overwriteBody", "boolean", "true")
                .build();

        connector.connect(context);

        String result = context.getEnvelope().getBody().toString();
        Assert.assertTrue(result.contains("greeting") && result.contains("Hello World"),
                "Text output should contain XML content");
    }

    // ==================== Text Input Tests ====================

    @Test(description = "Test Text request -> JSON response")
    public void testTextToJson() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        String textInput = "{\"message\":\"Hello\",\"count\":5}";

        TestMessageContext context = ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("textToJson")
                .returnType("json")
                .addParameter("textInput", "string", textInput)
                .addParameter("overwriteBody", "boolean", "true")
                .build();

        connector.connect(context);

        String result = context.getEnvelope().getBody().toString();
        Assert.assertTrue(result.contains("message") && result.contains("Hello"),
                "JSON output should contain parsed message field");
    }

    @Test(description = "Test Text request -> XML response")
    public void testTextToXml() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        String textInput = "<book><title>Ballerina Guide</title><author>WSO2</author></book>";

        TestMessageContext context = ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("textToXml")
                .returnType("xml")
                .addParameter("textInput", "string", textInput)
                .addParameter("overwriteBody", "boolean", "true")
                .build();

        connector.connect(context);

        String result = context.getEnvelope().getBody().toString();
        Assert.assertTrue(result.contains("<title>Ballerina Guide</title>"),
                "XML output should contain parsed title element");
        Assert.assertTrue(result.contains("<author>WSO2</author>"),
                "XML output should contain parsed author element");
    }

    @Test(description = "Test Text request -> Text response")
    public void testTextToText() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        String textInput = "Hello, this is plain text!";

        TestMessageContext context = ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("textToText")
                .returnType("string")
                .addParameter("textInput", "string", textInput)
                .addParameter("overwriteBody", "boolean", "true")
                .build();

        connector.connect(context);

        String result = context.getEnvelope().getBody().toString();
        Assert.assertTrue(result.contains("Hello, this is plain text!"),
                "Text output should contain the input text");
    }
}


