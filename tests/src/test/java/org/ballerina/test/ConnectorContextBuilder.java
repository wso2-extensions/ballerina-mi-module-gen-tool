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

package org.ballerina.test;

import org.apache.synapse.mediators.template.TemplateContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

/**
 * Helper class to build test MessageContext for connector tests.
 */
public class ConnectorContextBuilder {
    private int paramCount = 0;
    private String connectionName;
    private String methodName;
    private String returnType;
    private HashMap<String, Object> properties = new HashMap<>();
    private HashMap<Object, Object> templateParams = new HashMap<>();
    private boolean isConnection;

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
        paramCount++;
        return this;
    }

    public ConnectorContextBuilder addProperty(String key, Object value) {
        properties.put(key, value);
        return this;
    }

    public ConnectorContextBuilder isConnection(boolean isConnection) {
        this.isConnection = isConnection;
        return this;
    }

    public TestMessageContext build() {
        TestMessageContext context = new TestMessageContext();

        // Set connector-specific properties
        if (connectionName != null) {
            properties.put("connectionName", connectionName);
        }
        if (methodName != null) {
            properties.put("paramFunctionName", methodName);  // Use correct constant
        }
        if (returnType != null) {
            properties.put("returnType", returnType);
        }
        properties.put("paramSize", paramCount);

        for (var entry : properties.entrySet()) {
            context.setProperty(entry.getKey(), entry.getValue());
        }

        // Set up template context for parameter lookup
        Stack<TemplateContext> stack = new Stack<>();
        TemplateContext templateContext = new TemplateContext("testConnectorFunc", new ArrayList<>());
        templateParams.put("responseVariable", "result");
        templateParams.put(isConnection ? "name" : "connectionName", connectionName);
        templateContext.setMappedValues(templateParams);
        stack.push(templateContext);
        context.setProperty("_SYNAPSE_FUNCTION_STACK", stack);

        return context;
    }
}
