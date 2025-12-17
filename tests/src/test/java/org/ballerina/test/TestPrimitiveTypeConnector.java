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
 * Tests for primitive type support in BalConnectorFunction (connectors).
 * Tests Ballerina client class remote functions that accept and return primitive types.
 */
public class TestPrimitiveTypeConnector {

    private static final String CONNECTION_NAME = "primitiveTypeConnection";
    private static final String CONNECTION_TYPE = "PRIMITIVETYPEPROJECT_PRIMITIVEDATATYPECLIENT";

    @BeforeClass
    public void setupRuntime() throws Exception {
        ModuleInfo moduleInfo = new ModuleInfo("testOrg", "primitiveTypeProject", "1");
        BalConnectorConfig config = new BalConnectorConfig(moduleInfo);

        TestMessageContext initContext = ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .objectTypeName("PrimitiveDataTypeClient")
                .addParameter("connectionType", "string", CONNECTION_TYPE)
                .addParameter("apiUrl", "string", "http://test.api.com")
                .build();

        // Connection-type specific properties for the init invocation
        initContext.setProperty(CONNECTION_TYPE + "_objectTypeName", "PrimitiveDataTypeClient");
        initContext.setProperty(CONNECTION_TYPE + "_paramSize", 1);
        initContext.setProperty(CONNECTION_TYPE + "_paramFunctionName", "init");
        initContext.setProperty(CONNECTION_TYPE + "_param0", "apiUrl");
        initContext.setProperty(CONNECTION_TYPE + "_paramType0", "string");

        config.connect(initContext);
    }

    @Test(description = "Test string transformation")
    public void testStringTransform() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        TestMessageContext context = ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("transformStringToUpperCase")
                .returnType("string")
                .addParameter("input", "string", "hello")
                .build();

        connector.connect(context);

        String result = ((DefaultConnectorResponse) context.getVariable("result")).getPayload().toString();
        Assert.assertEquals(result, "HELLO", "String should be transformed to uppercase");
    }

    @Test(description = "Test integer transformation")
    public void testIntegerTransform() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        TestMessageContext context = ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("addIntegers")
                .returnType("int")
                .addParameter("first", "int", "10")
                .addParameter("second", "int", "20")
                .build();

        connector.connect(context);

        String result = ((DefaultConnectorResponse) context.getVariable("result")).getPayload().toString();
        Assert.assertEquals(result, "30", "10 + 20 should be 30");
    }

    @Test(description = "Test boolean transformation")
    public void testBooleanTransform() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        TestMessageContext context = ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("performLogicalAnd")
                .returnType("boolean")
                .addParameter("first", "boolean", "true")
                .addParameter("second", "boolean", "false")
                .build();

        connector.connect(context);

        String result = ((DefaultConnectorResponse) context.getVariable("result")).getPayload().toString();
        Assert.assertEquals(result, "false", "true AND false should be false");
    }

    @Test(description = "Test float transformation")
    public void testFloatTransform() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        TestMessageContext context = ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("addFloats")
                .returnType("float")
                .addParameter("first", "float", "10.5")
                .addParameter("second", "float", "20.5")
                .build();

        connector.connect(context);

        String result = ((DefaultConnectorResponse) context.getVariable("result")).getPayload().toString();
        Assert.assertEquals(result, "31.0", "10.5 + 20.5 should be 31.0");
    }

    @Test(description = "Test decimal transformation")
    public void testDecimalTransform() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        TestMessageContext context = ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("addDecimals")
                .returnType("decimal")
                .addParameter("first", "decimal", "10.5")
                .addParameter("second", "decimal", "20.5")
                .build();

        connector.connect(context);

        String result = ((DefaultConnectorResponse) context.getVariable("result")).getPayload().toString();
        Assert.assertEquals(result, "31.0", "10.5 + 20.5 should be 31.0");
    }

    @Test(description = "Test Signed8 parameter")
    public void testSigned8Parameter() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        TestMessageContext context = ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("processSigned8")
                .returnType("int")
                .addParameter("value", "int", "-128")
                .build();

        connector.connect(context);

        String result = ((DefaultConnectorResponse) context.getVariable("result")).getPayload().toString();
        Assert.assertEquals(result, "-128", "Signed8 value should be -128");
    }

    @Test(description = "Test Signed16 parameter")
    public void testSigned16Parameter() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        TestMessageContext context = ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("processSigned16")
                .returnType("int")
                .addParameter("value", "int", "-32768")
                .build();

        connector.connect(context);

        String result = ((DefaultConnectorResponse) context.getVariable("result")).getPayload().toString();
        Assert.assertEquals(result, "-32768", "Signed16 value should be -32768");
    }

    @Test(description = "Test Signed32 parameter")
    public void testSigned32Parameter() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        TestMessageContext context = ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("processSigned32")
                .returnType("int")
                .addParameter("value", "int", "-2147483648")
                .build();

        connector.connect(context);

        String result = ((DefaultConnectorResponse) context.getVariable("result")).getPayload().toString();
        Assert.assertEquals(result, "-2147483648", "Signed32 value should be -2147483648");
    }

    @Test(description = "Test Unsigned8 parameter")
    public void testUnsigned8Parameter() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        TestMessageContext context = ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("processUnsigned8")
                .returnType("int")
                .addParameter("value", "int", "255")
                .build();

        connector.connect(context);

        String result = ((DefaultConnectorResponse) context.getVariable("result")).getPayload().toString();
        Assert.assertEquals(result, "255", "Unsigned8 value should be 255");
    }

    @Test(description = "Test Unsigned16 parameter")
    public void testUnsigned16Parameter() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        TestMessageContext context = ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("processUnsigned16")
                .returnType("int")
                .addParameter("value", "int", "65535")
                .build();

        connector.connect(context);

        String result = ((DefaultConnectorResponse) context.getVariable("result")).getPayload().toString();
        Assert.assertEquals(result, "65535", "Unsigned16 value should be 65535");
    }

    @Test(description = "Test Unsigned32 parameter")
    public void testUnsigned32Parameter() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        TestMessageContext context = ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("processUnsigned32")
                .returnType("int")
                .addParameter("value", "int", "4294967295")
                .build();

        connector.connect(context);

        String result = ((DefaultConnectorResponse) context.getVariable("result")).getPayload().toString();
        Assert.assertEquals(result, "4294967295", "Unsigned32 value should be 4294967295");
    }

    @Test(description = "Test Char parameter")
    public void testCharParameter() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        TestMessageContext context = ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("processChar")
                .returnType("string")
                .addParameter("value", "string", "A")
                .build();

        connector.connect(context);

        String result = ((DefaultConnectorResponse) context.getVariable("result")).getPayload().toString();
        Assert.assertEquals(result, "A", "Char value should be A");
    }

    /**
     * Helper class to build test MessageContext for connector tests.
     */
    static class ConnectorContextBuilder {
        private int paramCount = 0;
        private String connectionName;
        private String methodName;
        private String returnType;
        private HashMap<String, Object> properties = new HashMap<>();
        private HashMap<Object, Object> templateParams = new HashMap<>();

        static ConnectorContextBuilder connectorContext() {
            return new ConnectorContextBuilder();
        }

        public ConnectorContextBuilder connectionName(String name) {
            this.connectionName = name;
            return this;
        }

        public ConnectorContextBuilder methodName(String name) {
            this.methodName = name;
            return this;
        }

        public ConnectorContextBuilder objectTypeName(String name) {
            properties.put("objectTypeName", name);
            return this;
        }

        public ConnectorContextBuilder returnType(String type) {
            this.returnType = type;
            return this;
        }

        public ConnectorContextBuilder addParameter(String name, String type, String value) {
            properties.put("param" + paramCount, name);
            properties.put("paramType" + paramCount, type);
            templateParams.put(name, value);
            if ("connectionType".equals(name)) {
                properties.put("connectionType", value);
            }
            paramCount++;
            return this;
        }

        public ConnectorContextBuilder addProperty(String key, Object value) {
            properties.put(key, value);
            return this;
        }

        public TestMessageContext build() {
            TestMessageContext context = new TestMessageContext();

            // Set connector-specific properties
            if (connectionName != null) {
                properties.put("connectionName", connectionName);
            }
            if (properties.containsKey("connectionType")) {
                templateParams.put("connectionType", properties.get("connectionType"));
            }
            if (methodName != null) {
                properties.put("paramFunctionName", methodName);
            }
            if (returnType != null) {
                properties.put("returnType", returnType);
            }
            properties.put("paramSize", paramCount);

            for (var entry : properties.entrySet()) {
                context.setProperty(entry.getKey(), entry.getValue());
            }

            Stack<TemplateContext> stack = new Stack<>();
            TemplateContext templateContext = new TemplateContext("testConnectorFunc", new ArrayList<>());
            templateParams.put("responseVariable", "result");
            templateParams.put("name", connectionName != null ? connectionName : CONNECTION_NAME);
            templateParams.put("connectionName", connectionName != null ? connectionName : CONNECTION_NAME);
            if (properties.containsKey("connectionType")) {
                templateParams.put("connectionType", properties.get("connectionType"));
            }
            templateContext.setMappedValues(templateParams);
            stack.push(templateContext);
            context.setProperty("_SYNAPSE_FUNCTION_STACK", stack);

            return context;
        }
    }
}
