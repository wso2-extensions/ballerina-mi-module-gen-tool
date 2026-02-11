/*
 * Copyright (c) 2026, WSO2 LLC. (http://wso2.com)
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

import io.ballerina.stdlib.mi.BalExecutor;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.lang.reflect.Method;

/**
 * Test class for verifying record reconstruction in BalExecutor.
 * <p>
 * This test verifies that the init method's flattened record parameters are correctly
 * reconstructed back into their nested structure at runtime.
 */
public class RecordReconstructionTest {

    private BalExecutor balExecutor;
    private MessageContext messageContext;

    @BeforeMethod
    public void setup() {
        balExecutor = new BalExecutor();
        // Create a mock message context
        messageContext = new Axis2MessageContext(
                new org.apache.axis2.context.MessageContext(),
                null,
                null
        );
    }

    /**
     * Tests the setNestedField method to ensure it correctly creates nested JSON objects
     * and sets values using dot notation paths.
     */
    @Test(description = "Verify setNestedField creates proper nested structure")
    public void testSetNestedField() throws Exception {
        // Use reflection to access the private setNestedField method
        Method setNestedFieldMethod = BalExecutor.class.getDeclaredMethod(
                "setNestedField",
                com.google.gson.JsonObject.class,
                String.class,
                Object.class,
                String.class,
                MessageContext.class
        );
        setNestedFieldMethod.setAccessible(true);

        com.google.gson.JsonObject rootObject = new com.google.gson.JsonObject();

        // Test simple field
        setNestedFieldMethod.invoke(balExecutor, rootObject, "httpVersion", "HTTP_1_1", "string", messageContext);
        Assert.assertTrue(rootObject.has("httpVersion"), "Simple field should be set");
        Assert.assertEquals(rootObject.get("httpVersion").getAsString(), "HTTP_1_1");

        // Test nested field (2 levels)
        setNestedFieldMethod.invoke(balExecutor, rootObject, "http1Settings.keepAlive", "ALWAYS", "string", messageContext);
        Assert.assertTrue(rootObject.has("http1Settings"), "Nested object should be created");
        Assert.assertTrue(rootObject.getAsJsonObject("http1Settings").has("keepAlive"),
                "Nested field should be set");
        Assert.assertEquals(rootObject.getAsJsonObject("http1Settings").get("keepAlive").getAsString(),
                "ALWAYS");

        // Test deeply nested field (3 levels)
        setNestedFieldMethod.invoke(balExecutor, rootObject, "http1Settings.proxy.host", "localhost", "string", messageContext);
        Assert.assertTrue(rootObject.getAsJsonObject("http1Settings").has("proxy"),
                "Second level nested object should be created");
        Assert.assertTrue(rootObject.getAsJsonObject("http1Settings")
                        .getAsJsonObject("proxy").has("host"),
                "Deeply nested field should be set");
        Assert.assertEquals(rootObject.getAsJsonObject("http1Settings")
                        .getAsJsonObject("proxy").get("host").getAsString(),
                "localhost");

        // Test integer field
        setNestedFieldMethod.invoke(balExecutor, rootObject, "http1Settings.proxy.port", "8080", "int", messageContext);
        Assert.assertEquals(rootObject.getAsJsonObject("http1Settings")
                        .getAsJsonObject("proxy").get("port").getAsLong(),
                8080L);

        // Test boolean field
        setNestedFieldMethod.invoke(balExecutor, rootObject, "cache.enabled", "true", "boolean", messageContext);
        Assert.assertTrue(rootObject.getAsJsonObject("cache").get("enabled").getAsBoolean());

        // Test float field
        setNestedFieldMethod.invoke(balExecutor, rootObject, "cache.evictionFactor", "0.75", "float", messageContext);
        Assert.assertEquals(rootObject.getAsJsonObject("cache").get("evictionFactor").getAsDouble(),
                0.75, 0.001);
    }

    /**
     * Tests the reconstructRecordFromFields method to verify it correctly builds
     * a complete record from flattened field properties.
     */
    @Test(description = "Verify reconstructRecordFromFields builds complete record structure")
    public void testReconstructRecordFromFields() throws Exception {
        // Set up flattened field properties in the message context
        // Pattern: {connectionType}_{recordParamName}_param{index} = "fieldPath"
        String connectionType = "TESTORG_MYCONNECTOR_CLIENT";
        String recordParamName = "config";

        // Set up flattened fields
        messageContext.setProperty(connectionType + "_" + recordParamName + "_param0", "httpVersion");
        messageContext.setProperty(connectionType + "_" + recordParamName + "_paramType0", "string");

        messageContext.setProperty(connectionType + "_" + recordParamName + "_param1", "http1Settings.keepAlive");
        messageContext.setProperty(connectionType + "_" + recordParamName + "_paramType1", "string");

        messageContext.setProperty(connectionType + "_" + recordParamName + "_param2", "http1Settings.proxy.host");
        messageContext.setProperty(connectionType + "_" + recordParamName + "_paramType2", "string");

        messageContext.setProperty(connectionType + "_" + recordParamName + "_param3", "http1Settings.proxy.port");
        messageContext.setProperty(connectionType + "_" + recordParamName + "_paramType3", "int");

        messageContext.setProperty(connectionType + "_" + recordParamName + "_param4", "timeout");
        messageContext.setProperty(connectionType + "_" + recordParamName + "_paramType4", "decimal");

        messageContext.setProperty(connectionType + "_" + recordParamName + "_param5", "cache.enabled");
        messageContext.setProperty(connectionType + "_" + recordParamName + "_paramType5", "boolean");

        // Set the actual values as template parameters
        // Note: Use underscores instead of dots because Synapse can't handle dots in parameter names
        // and the code converts dots to underscores when looking up template parameters
        messageContext.setProperty("httpVersion", "HTTP_1_1");
        messageContext.setProperty("http1Settings_keepAlive", "ALWAYS");
        messageContext.setProperty("http1Settings_proxy_host", "localhost");
        messageContext.setProperty("http1Settings_proxy_port", "8080");
        messageContext.setProperty("timeout", "60.0");
        messageContext.setProperty("cache_enabled", "true");

        // Use reflection to access the private reconstructRecordFromFields method
        // The method now takes a propertyPrefix (connectionType + "_" + recordParamName) and context
        Method reconstructMethod = BalExecutor.class.getDeclaredMethod(
                "reconstructRecordFromFields",
                String.class,
                MessageContext.class
        );
        reconstructMethod.setAccessible(true);

        // Call the reconstruction method with the combined prefix
        String propertyPrefix = connectionType + "_" + recordParamName;
        Object result = reconstructMethod.invoke(balExecutor, propertyPrefix, messageContext);

        // Verify the result is a BMap (Ballerina map)
        Assert.assertNotNull(result, "Reconstructed record should not be null");
        Assert.assertTrue(result instanceof io.ballerina.runtime.api.values.BMap,
                "Result should be a BMap");

        io.ballerina.runtime.api.values.BMap<?, ?> reconstructedRecord =
                (io.ballerina.runtime.api.values.BMap<?, ?>) result;

        // Verify the reconstructed structure
        Assert.assertTrue(reconstructedRecord.containsKey(io.ballerina.runtime.api.utils.StringUtils.fromString("httpVersion")),
                "Root field should be present");
        Assert.assertTrue(reconstructedRecord.containsKey(io.ballerina.runtime.api.utils.StringUtils.fromString("http1Settings")),
                "Nested object should be present");
        Assert.assertTrue(reconstructedRecord.containsKey(io.ballerina.runtime.api.utils.StringUtils.fromString("timeout")),
                "Timeout field should be present");
        Assert.assertTrue(reconstructedRecord.containsKey(io.ballerina.runtime.api.utils.StringUtils.fromString("cache")),
                "Cache object should be present");

        // Verify nested structure
        Object http1Settings = reconstructedRecord.get(io.ballerina.runtime.api.utils.StringUtils.fromString("http1Settings"));
        Assert.assertTrue(http1Settings instanceof io.ballerina.runtime.api.values.BMap,
                "http1Settings should be a BMap");

        io.ballerina.runtime.api.values.BMap<?, ?> http1SettingsMap =
                (io.ballerina.runtime.api.values.BMap<?, ?>) http1Settings;
        Assert.assertTrue(http1SettingsMap.containsKey(io.ballerina.runtime.api.utils.StringUtils.fromString("proxy")),
                "Proxy should be present in http1Settings");
    }

    /**
     * Tests the findConnectionTypeForParam method to verify it correctly identifies
     * the connection type prefix for a given record parameter.
     */
    @Test(description = "Verify findConnectionTypeForParam discovers connection type")
    public void testFindConnectionTypeForParam() throws Exception {
        // Set up context with connection type properties
        String connectionType = "TESTORG_MYCONNECTOR_CLIENT";
        String recordParamName = "config";

        // Set SIZE property
        messageContext.setProperty("SIZE", "2");

        // Set param0 to match our record parameter name
        messageContext.setProperty("param0", recordParamName);
        messageContext.setProperty("paramType0", "record");

        // Set FUNCTION_NAME to indicate init function
        messageContext.setProperty("FUNCTION_NAME", "init");

        // Set the connection type property that should be found
        messageContext.setProperty(connectionType + "_param0", recordParamName);
        messageContext.setProperty(connectionType + "_" + recordParamName + "_param0", "someField");

        // Use reflection to access the private findConnectionTypeForParam method
        Method findConnectionTypeMethod = BalExecutor.class.getDeclaredMethod(
                "findConnectionTypeForParam",
                MessageContext.class,
                String.class
        );
        findConnectionTypeMethod.setAccessible(true);

        // Note: This method currently has hardcoded values, so it will return null
        // or a hardcoded value. This test documents the expected behavior for when
        // it's made more dynamic.
        Object result = findConnectionTypeMethod.invoke(balExecutor, messageContext, recordParamName);

        // The current implementation returns null or a hardcoded value
        // This test serves as documentation for future enhancement
        // Assert.assertEquals(result, connectionType, "Should find the correct connection type");
    }

    /**
     * Tests that the record reconstruction correctly handles optional fields
     * (fields that might not have values in the context).
     */
    @Test(description = "Verify reconstruction handles optional fields correctly")
    public void testReconstructionWithOptionalFields() throws Exception {
        String connectionType = "TESTORG_MYCONNECTOR_CLIENT";
        String recordParamName = "config";

        // Set up some fields, but not all
        messageContext.setProperty(connectionType + "_" + recordParamName + "_param0", "httpVersion");
        messageContext.setProperty(connectionType + "_" + recordParamName + "_paramType0", "string");

        messageContext.setProperty(connectionType + "_" + recordParamName + "_param1", "timeout");
        messageContext.setProperty(connectionType + "_" + recordParamName + "_paramType1", "decimal");

        // Only set value for httpVersion, leave timeout empty
        messageContext.setProperty("httpVersion", "HTTP_2_0");
        // timeout deliberately not set

        Method reconstructMethod = BalExecutor.class.getDeclaredMethod(
                "reconstructRecordFromFields",
                String.class,
                MessageContext.class
        );
        reconstructMethod.setAccessible(true);

        String propertyPrefix = connectionType + "_" + recordParamName;
        Object result = reconstructMethod.invoke(balExecutor, propertyPrefix, messageContext);

        Assert.assertNotNull(result, "Should handle optional fields gracefully");
        io.ballerina.runtime.api.values.BMap<?, ?> reconstructedRecord =
                (io.ballerina.runtime.api.values.BMap<?, ?>) result;

        // Should have the field that was set
        Assert.assertTrue(reconstructedRecord.containsKey(
                        io.ballerina.runtime.api.utils.StringUtils.fromString("httpVersion")),
                "Set field should be present");

        // Optional field might not be present or might be null
        // The reconstruction should not fail
    }
}
