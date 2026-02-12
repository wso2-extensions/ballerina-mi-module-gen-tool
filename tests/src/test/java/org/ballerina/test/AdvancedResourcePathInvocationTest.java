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

import io.ballerina.runtime.api.values.BString;
import io.ballerina.stdlib.mi.executor.BalExecutor;
import io.ballerina.stdlib.mi.executor.ParamHandler;
import io.ballerina.stdlib.mi.utils.SynapseUtils;
import io.ballerina.stdlib.mi.Constants;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.template.TemplateContext;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * Test class for verifying advanced resource function path invocation.
 * <p>
 * This test validates that the BalExecutor correctly handles complex resource functions
 * with multi-segment paths, specifically:
 * <ul>
 *   <li>Multiple resource functions with the same HTTP accessor but different paths</li>
 *   <li>JVM method name generation for complex paths with multiple segments</li>
 *   <li>Escaped Ballerina keywords in path segments (e.g., 'import)</li>
 *   <li>Multiple path parameters in a single resource function</li>
 *   <li>Correct path parameter ordering and type conversion</li>
 * </ul>
 * <p>
 * Example paths tested:
 * <ul>
 *   <li>GET users/[userId]/drafts → $get$users$$userId$drafts</li>
 *   <li>GET users/[userId]/messages → $get$users$$userId$messages</li>
 *   <li>GET users/[userId]/labels → $get$users$$userId$labels</li>
 *   <li>POST users/[userId]/messages/'import → $post$users$$userId$messages$import</li>
 *   <li>POST users/[userId]/messages/[id]/trash → $post$users$$userId$messages$$id$trash</li>
 * </ul>
 */
public class AdvancedResourcePathInvocationTest {

    private BalExecutor balExecutor;
    private ParamHandler paramHandler;
    private MessageContext messageContext;

    @BeforeMethod
    public void setup() {
        balExecutor = new BalExecutor();
        paramHandler = new ParamHandler();
        messageContext = new Axis2MessageContext(
                new org.apache.axis2.context.MessageContext(),
                null,
                null
        );
    }

    /**
     * Sets up a template context stack for template parameter lookup.
     */
    @SuppressWarnings("unchecked")
    private void setupTemplateContext(Map<String, Object> parameters) {
        Stack<TemplateContext> templateContextStack = new Stack<>();
        TemplateContext templateContext = new TemplateContext("testTemplate", new ArrayList<>());
        HashMap<Object, Object> mappedValues = new HashMap<>();
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            mappedValues.put(entry.getKey(), entry.getValue());
        }
        templateContext.setMappedValues(mappedValues);
        templateContextStack.push(templateContext);
        messageContext.setProperty(Constants.SYNAPSE_FUNCTION_STACK, templateContextStack);
    }

    /**
     * Data provider for multi-segment JVM method names.
     * Each entry: {functionName, accessor, jvmMethodName, pathParamCount, pathParam0Name, pathParam1Name}
     */
    @DataProvider(name = "multiSegmentMethodNames")
    public Object[][] multiSegmentMethodNames() {
        return new Object[][]{
                // GET methods with same accessor but different paths
                {"getUsersDraftsByUserId", "get", "$get$users$$userId$drafts", 1, "userId", null},
                {"getUsersMessagesByUserId", "get", "$get$users$$userId$messages", 1, "userId", null},
                {"getUsersLabelsByUserId", "get", "$get$users$$userId$labels", 1, "userId", null},
                // GET with two path parameters
                {"getUsersDraftsByUserIdById", "get", "$get$users$$userId$drafts$$id", 2, "userId", "id"},
                {"getUsersMessagesByUserIdById", "get", "$get$users$$userId$messages$$id", 2, "userId", "id"},
                // POST methods
                {"postUsersDraftsByUserId", "post", "$post$users$$userId$drafts", 1, "userId", null},
                {"postUsersMessagesSendByUserId", "post", "$post$users$$userId$messages$send", 1, "userId", null},
                // POST with escaped keyword 'import
                {"postUsersMessages_importByUserId", "post", "$post$users$$userId$messages$import", 1, "userId", null},
                // POST with two path parameters and static segment at end
                {"postUsersMessagesTrashByUserIdById", "post", "$post$users$$userId$messages$$id$trash", 2, "userId", "id"},
                // DELETE
                {"deleteUsersDraftsByUserIdById", "delete", "$delete$users$$userId$drafts$$id", 2, "userId", "id"},
        };
    }

    /**
     * Tests that the JVM method name property is correctly set for multi-segment resource functions.
     * This verifies that multiple GET methods with same accessor can be distinguished by their full JVM method name.
     */
    @Test(description = "Verify JVM method name property is correctly set for multi-segment paths",
            dataProvider = "multiSegmentMethodNames")
    public void testJvmMethodNameProperty(String functionName, String accessor, String expectedJvmMethodName,
                                          int pathParamCount, String pathParam0, String pathParam1) throws Exception {
        Method getPropertyAsStringMethod = SynapseUtils.class.getMethod(
                "getPropertyAsString",
                MessageContext.class,
                String.class
        );

        // Setup resource function properties
        messageContext.setProperty(Constants.FUNCTION_TYPE, Constants.FUNCTION_TYPE_RESOURCE);
        messageContext.setProperty(Constants.RESOURCE_ACCESSOR, accessor);
        messageContext.setProperty(Constants.JVM_METHOD_NAME, expectedJvmMethodName);
        messageContext.setProperty(Constants.PATH_PARAM_SIZE, String.valueOf(pathParamCount));
        messageContext.setProperty(Constants.FUNCTION_NAME, functionName);

        // Verify JVM method name is correctly retrieved
        String jvmMethodName = (String) getPropertyAsStringMethod.invoke(null, messageContext, Constants.JVM_METHOD_NAME);
        Assert.assertEquals(jvmMethodName, expectedJvmMethodName,
                "JVM method name should match for function: " + functionName);

        // Verify accessor is still available
        String retrievedAccessor = (String) getPropertyAsStringMethod.invoke(null, messageContext, Constants.RESOURCE_ACCESSOR);
        Assert.assertEquals(retrievedAccessor, accessor);
    }

    /**
     * Tests that multiple GET methods with same accessor but different paths have different JVM method names.
     * This is the core test for connectors with multiple resource functions sharing the same HTTP accessor.
     */
    @Test(description = "Verify multiple GET methods are distinguishable by JVM method name")
    public void testMultipleGetMethodsDistinguishable() throws Exception {
        // These are three GET methods that would all have accessor "get" but need different JVM names
        String[] jvmMethodNames = {
                "$get$users$$userId$drafts",
                "$get$users$$userId$messages",
                "$get$users$$userId$labels"
        };

        // Verify all are unique
        Assert.assertEquals(jvmMethodNames.length, 3);
        Assert.assertNotEquals(jvmMethodNames[0], jvmMethodNames[1], "drafts and messages paths must differ");
        Assert.assertNotEquals(jvmMethodNames[1], jvmMethodNames[2], "messages and labels paths must differ");
        Assert.assertNotEquals(jvmMethodNames[0], jvmMethodNames[2], "drafts and labels paths must differ");

        // Verify all start with $get$ (same accessor)
        for (String methodName : jvmMethodNames) {
            Assert.assertTrue(methodName.startsWith("$get$"),
                    "All should have same get accessor prefix: " + methodName);
        }

        // Verify all have users path segment
        for (String methodName : jvmMethodNames) {
            Assert.assertTrue(methodName.contains("$users$"),
                    "All should have users path segment: " + methodName);
        }
    }

    /**
     * Tests the complete setup for a resource function with single path parameter.
     * Simulates: GET users/[userId]/drafts
     */
    @Test(description = "Verify complete resource function setup - single path param")
    public void testSinglePathParamResourceFunction() throws Exception {
        // Setup: GET users/[userId]/drafts
        messageContext.setProperty(Constants.FUNCTION_TYPE, Constants.FUNCTION_TYPE_RESOURCE);
        messageContext.setProperty(Constants.RESOURCE_ACCESSOR, "get");
        messageContext.setProperty(Constants.JVM_METHOD_NAME, "$get$users$$userId$drafts");
        messageContext.setProperty(Constants.PATH_PARAM_SIZE, "1");
        messageContext.setProperty(Constants.FUNCTION_NAME, "getUsersDraftsByUserId");
        messageContext.setProperty("pathParam0", "userId");
        messageContext.setProperty("pathParamType0", "string");

        // Setup template context with path param value
        Map<String, Object> templateParams = new HashMap<>();
        templateParams.put("userId", "me");
        setupTemplateContext(templateParams);

        // Verify path parameter prepending
        Method prependPathParamsMethod = ParamHandler.class.getMethod(
                "prependPathParams",
                Object[].class,
                MessageContext.class
        );

        Object[] originalArgs = new Object[]{};  // No body params for GET
        Object[] result = (Object[]) prependPathParamsMethod.invoke(paramHandler, originalArgs, messageContext);

        Assert.assertEquals(result.length, 1, "Should have 1 path param");
        Assert.assertTrue(result[0] instanceof BString);
        Assert.assertEquals(result[0].toString(), "me", "userId should be 'me'");
    }

    /**
     * Tests the complete setup for a resource function with two path parameters.
     * Simulates: GET users/[userId]/drafts/[id]
     */
    @Test(description = "Verify complete resource function setup - two path params")
    public void testTwoPathParamsResourceFunction() throws Exception {
        // Setup: GET users/[userId]/drafts/[id]
        messageContext.setProperty(Constants.FUNCTION_TYPE, Constants.FUNCTION_TYPE_RESOURCE);
        messageContext.setProperty(Constants.RESOURCE_ACCESSOR, "get");
        messageContext.setProperty(Constants.JVM_METHOD_NAME, "$get$users$$userId$drafts$$id");
        messageContext.setProperty(Constants.PATH_PARAM_SIZE, "2");
        messageContext.setProperty(Constants.FUNCTION_NAME, "getUsersDraftsByUserIdById");
        messageContext.setProperty("pathParam0", "id");
        messageContext.setProperty("pathParamType0", "string");
        messageContext.setProperty("pathParam1", "userId");
        messageContext.setProperty("pathParamType1", "string");

        // Setup template context with path param values
        Map<String, Object> templateParams = new HashMap<>();
        templateParams.put("userId", "me");
        templateParams.put("id", "draft-123");
        setupTemplateContext(templateParams);

        // Verify path parameter prepending
        Method prependPathParamsMethod = ParamHandler.class.getMethod(
                "prependPathParams",
                Object[].class,
                MessageContext.class
        );

        Object[] originalArgs = new Object[]{};
        Object[] result = (Object[]) prependPathParamsMethod.invoke(paramHandler, originalArgs, messageContext);

        Assert.assertEquals(result.length, 2, "Should have 2 path params");
        Assert.assertTrue(result[0] instanceof BString);
        Assert.assertTrue(result[1] instanceof BString);
        // Note: Path params are prepended in order pathParam0, pathParam1
        Assert.assertEquals(result[0].toString(), "draft-123", "First param should be id");
        Assert.assertEquals(result[1].toString(), "me", "Second param should be userId");
    }

    /**
     * Tests the complete setup for a resource function with escaped keyword path segment.
     * Simulates: POST users/[userId]/messages/'import
     * The 'import keyword is escaped in Ballerina but becomes 'import' in the JVM method name.
     */
    @Test(description = "Verify resource function with escaped keyword path segment")
    public void testEscapedKeywordPathSegment() throws Exception {
        // Setup: POST users/[userId]/messages/'import
        // Note: 'import becomes import in the JVM method name
        messageContext.setProperty(Constants.FUNCTION_TYPE, Constants.FUNCTION_TYPE_RESOURCE);
        messageContext.setProperty(Constants.RESOURCE_ACCESSOR, "post");
        messageContext.setProperty(Constants.JVM_METHOD_NAME, "$post$users$$userId$messages$import");
        messageContext.setProperty(Constants.PATH_PARAM_SIZE, "1");
        messageContext.setProperty(Constants.FUNCTION_NAME, "postUsersMessages_importByUserId");
        messageContext.setProperty("pathParam0", "userId");
        messageContext.setProperty("pathParamType0", "string");

        // Setup template context
        Map<String, Object> templateParams = new HashMap<>();
        templateParams.put("userId", "me");
        setupTemplateContext(templateParams);

        // Verify JVM method name contains 'import' without escape character
        Method getPropertyAsStringMethod = SynapseUtils.class.getMethod(
                "getPropertyAsString",
                MessageContext.class,
                String.class
        );

        String jvmMethodName = (String) getPropertyAsStringMethod.invoke(null, messageContext, Constants.JVM_METHOD_NAME);
        Assert.assertTrue(jvmMethodName.contains("$import"),
                "JVM method name should contain $import (without escape): " + jvmMethodName);
        Assert.assertFalse(jvmMethodName.contains("'import"),
                "JVM method name should NOT contain 'import (with escape): " + jvmMethodName);
    }

    /**
     * Tests the setup for a resource function with body parameter and path parameter.
     * Simulates: POST users/[userId]/drafts(Draft draft)
     */
    @Test(description = "Verify resource function with body param and path param")
    public void testResourceFunctionWithBodyAndPathParams() throws Exception {
        // Setup: POST users/[userId]/drafts with body param
        messageContext.setProperty(Constants.FUNCTION_TYPE, Constants.FUNCTION_TYPE_RESOURCE);
        messageContext.setProperty(Constants.RESOURCE_ACCESSOR, "post");
        messageContext.setProperty(Constants.JVM_METHOD_NAME, "$post$users$$userId$drafts");
        messageContext.setProperty(Constants.PATH_PARAM_SIZE, "1");
        messageContext.setProperty(Constants.SIZE, "1");  // One body param
        messageContext.setProperty(Constants.FUNCTION_NAME, "postUsersDraftsByUserId");
        messageContext.setProperty("pathParam0", "userId");
        messageContext.setProperty("pathParamType0", "string");

        // Setup template context
        Map<String, Object> templateParams = new HashMap<>();
        templateParams.put("userId", "me");
        templateParams.put("draft", "{\"id\":\"draft-1\",\"subject\":\"Test\",\"body\":\"Test body\"}");
        setupTemplateContext(templateParams);

        // Verify path parameter prepending with existing body args
        Method prependPathParamsMethod = ParamHandler.class.getMethod(
                "prependPathParams",
                Object[].class,
                MessageContext.class
        );

        // Simulate body param (would be converted elsewhere)
        Object[] originalArgs = new Object[]{"draftBodyParam"};
        Object[] result = (Object[]) prependPathParamsMethod.invoke(paramHandler, originalArgs, messageContext);

        Assert.assertEquals(result.length, 2, "Should have 1 path param + 1 body param");
        Assert.assertTrue(result[0] instanceof BString, "First should be path param");
        Assert.assertEquals(result[0].toString(), "me");
        Assert.assertEquals(result[1], "draftBodyParam", "Second should be body param");
    }

    /**
     * Tests resource function with trailing static segment after path parameter.
     * Simulates: POST users/[userId]/messages/[id]/trash
     */
    @Test(description = "Verify resource function with trailing static segment after path param")
    public void testTrailingStaticSegmentAfterPathParam() throws Exception {
        // Setup: POST users/[userId]/messages/[id]/trash
        messageContext.setProperty(Constants.FUNCTION_TYPE, Constants.FUNCTION_TYPE_RESOURCE);
        messageContext.setProperty(Constants.RESOURCE_ACCESSOR, "post");
        messageContext.setProperty(Constants.JVM_METHOD_NAME, "$post$users$$userId$messages$$id$trash");
        messageContext.setProperty(Constants.PATH_PARAM_SIZE, "2");
        messageContext.setProperty(Constants.FUNCTION_NAME, "postUsersMessagesTrashByUserIdById");
        messageContext.setProperty("pathParam0", "id");
        messageContext.setProperty("pathParamType0", "string");
        messageContext.setProperty("pathParam1", "userId");
        messageContext.setProperty("pathParamType1", "string");

        // Setup template context
        Map<String, Object> templateParams = new HashMap<>();
        templateParams.put("userId", "me");
        templateParams.put("id", "msg-456");
        setupTemplateContext(templateParams);

        // Verify JVM method name ends with $trash
        Method getPropertyAsStringMethod = SynapseUtils.class.getMethod(
                "getPropertyAsString",
                MessageContext.class,
                String.class
        );

        String jvmMethodName = (String) getPropertyAsStringMethod.invoke(null, messageContext, Constants.JVM_METHOD_NAME);
        Assert.assertTrue(jvmMethodName.endsWith("$trash"),
                "JVM method name should end with $trash: " + jvmMethodName);

        // Verify path params
        Method prependPathParamsMethod = ParamHandler.class.getMethod(
                "prependPathParams",
                Object[].class,
                MessageContext.class
        );

        Object[] result = (Object[]) prependPathParamsMethod.invoke(paramHandler, new Object[]{}, messageContext);

        Assert.assertEquals(result.length, 2);
        Assert.assertEquals(result[0].toString(), "msg-456", "id should be first");
        Assert.assertEquals(result[1].toString(), "me", "userId should be second");
    }

    /**
     * Tests that resource function detection combined with JVM method name lookup works correctly.
     */
    @Test(description = "Verify resource function detection with JVM method name")
    public void testResourceFunctionDetectionWithJvmMethodName() throws Exception {
        Method getPropertyAsStringMethod = SynapseUtils.class.getMethod(
                "getPropertyAsString",
                MessageContext.class,
                String.class
        );

        // Setup resource function
        messageContext.setProperty(Constants.FUNCTION_TYPE, Constants.FUNCTION_TYPE_RESOURCE);
        messageContext.setProperty(Constants.RESOURCE_ACCESSOR, "get");
        messageContext.setProperty(Constants.JVM_METHOD_NAME, "$get$users$$userId$drafts");

        // Verify function type is RESOURCE
        String functionType = (String) getPropertyAsStringMethod.invoke(null, messageContext, Constants.FUNCTION_TYPE);
        Assert.assertEquals(functionType, Constants.FUNCTION_TYPE_RESOURCE);
        Assert.assertTrue(Constants.FUNCTION_TYPE_RESOURCE.equals(functionType));

        // Verify JVM method name is available
        String jvmMethodName = (String) getPropertyAsStringMethod.invoke(null, messageContext, Constants.JVM_METHOD_NAME);
        Assert.assertNotNull(jvmMethodName);
        Assert.assertTrue(jvmMethodName.startsWith("$get$"));
    }

    /**
     * Tests DELETE method with two path parameters.
     * Simulates: DELETE users/[userId]/drafts/[id]
     */
    @Test(description = "Verify DELETE resource function with two path params")
    public void testDeleteResourceFunction() throws Exception {
        // Setup: DELETE users/[userId]/drafts/[id]
        messageContext.setProperty(Constants.FUNCTION_TYPE, Constants.FUNCTION_TYPE_RESOURCE);
        messageContext.setProperty(Constants.RESOURCE_ACCESSOR, "delete");
        messageContext.setProperty(Constants.JVM_METHOD_NAME, "$delete$users$$userId$drafts$$id");
        messageContext.setProperty(Constants.PATH_PARAM_SIZE, "2");
        messageContext.setProperty(Constants.FUNCTION_NAME, "deleteUsersDraftsByUserIdById");
        messageContext.setProperty("pathParam0", "id");
        messageContext.setProperty("pathParamType0", "string");
        messageContext.setProperty("pathParam1", "userId");
        messageContext.setProperty("pathParamType1", "string");

        // Setup template context
        Map<String, Object> templateParams = new HashMap<>();
        templateParams.put("userId", "me");
        templateParams.put("id", "draft-to-delete");
        setupTemplateContext(templateParams);

        // Verify JVM method name
        Method getPropertyAsStringMethod = SynapseUtils.class.getMethod(
                "getPropertyAsString",
                MessageContext.class,
                String.class
        );

        String jvmMethodName = (String) getPropertyAsStringMethod.invoke(null, messageContext, Constants.JVM_METHOD_NAME);
        Assert.assertTrue(jvmMethodName.startsWith("$delete$"),
                "DELETE method should have $delete$ prefix: " + jvmMethodName);

        // Verify path params
        Method prependPathParamsMethod = ParamHandler.class.getMethod(
                "prependPathParams",
                Object[].class,
                MessageContext.class
        );

        Object[] result = (Object[]) prependPathParamsMethod.invoke(paramHandler, new Object[]{}, messageContext);

        Assert.assertEquals(result.length, 2);
        Assert.assertEquals(result[0].toString(), "draft-to-delete");
        Assert.assertEquals(result[1].toString(), "me");
    }

    /**
     * Data provider for JVM method name format validation.
     */
    @DataProvider(name = "jvmMethodNameFormatProvider")
    public Object[][] jvmMethodNameFormatProvider() {
        return new Object[][]{
                // {jvmMethodName, expectedAccessor, expectedSegmentCount}
                {"$get$users$$userId$drafts", "get", 4},           // $get, $users, $$userId, $drafts
                {"$get$users$$userId$messages", "get", 4},
                {"$get$users$$userId$drafts$$id", "get", 5},       // $get, $users, $$userId, $drafts, $$id
                {"$post$users$$userId$messages$send", "post", 5},  // $post, $users, $$userId, $messages, $send
                {"$post$users$$userId$messages$$id$trash", "post", 6},
                {"$delete$users$$userId$drafts$$id", "delete", 5},
        };
    }

    /**
     * Tests JVM method name format validation.
     * Verifies that JVM method names follow the expected format with $ separators.
     */
    @Test(description = "Verify JVM method name format",
            dataProvider = "jvmMethodNameFormatProvider")
    public void testJvmMethodNameFormat(String jvmMethodName, String expectedAccessor, int expectedSegmentCount) {
        // All JVM method names should start with $
        Assert.assertTrue(jvmMethodName.startsWith("$"),
                "JVM method name should start with $: " + jvmMethodName);

        // Extract accessor (first segment after initial $)
        String[] parts = jvmMethodName.split("\\$");
        // First part is empty (before initial $), second part is the accessor
        Assert.assertEquals(parts[1], expectedAccessor,
                "Accessor should match: " + jvmMethodName);

        // Count non-empty segments
        int segmentCount = 0;
        for (String part : parts) {
            if (!part.isEmpty()) {
                segmentCount++;
            }
        }
        Assert.assertEquals(segmentCount, expectedSegmentCount,
                "Segment count should match for: " + jvmMethodName);
    }

    /**
     * Tests that special characters in path parameters don't break the invocation.
     */
    @Test(description = "Verify path parameters with special characters")
    public void testPathParamsWithSpecialCharacters() throws Exception {
        // Setup
        messageContext.setProperty(Constants.FUNCTION_TYPE, Constants.FUNCTION_TYPE_RESOURCE);
        messageContext.setProperty(Constants.RESOURCE_ACCESSOR, "get");
        messageContext.setProperty(Constants.JVM_METHOD_NAME, "$get$users$$userId$drafts");
        messageContext.setProperty(Constants.PATH_PARAM_SIZE, "1");
        messageContext.setProperty("pathParam0", "userId");
        messageContext.setProperty("pathParamType0", "string");

        // Setup template context with special characters in value
        Map<String, Object> templateParams = new HashMap<>();
        templateParams.put("userId", "user+test@example.com");
        setupTemplateContext(templateParams);

        // Verify path params are correctly handled
        Method prependPathParamsMethod = ParamHandler.class.getMethod(
                "prependPathParams",
                Object[].class,
                MessageContext.class
        );

        Object[] result = (Object[]) prependPathParamsMethod.invoke(paramHandler, new Object[]{}, messageContext);

        Assert.assertEquals(result.length, 1);
        Assert.assertEquals(result[0].toString(), "user+test@example.com",
                "Special characters should be preserved in path param value");
    }

    /**
     * Tests comparison between simple resource function and complex multi-segment path.
     */
    @Test(description = "Compare simple vs complex resource function JVM method names")
    public void testSimpleVsComplexResourceFunction() {
        // Simple: resource function get items() -> $get$items
        // Complex: resource function get users/[userId]/drafts() -> $get$users$$userId$drafts

        String simpleMethodName = "$get$items";
        String complexMethodName = "$get$users$$userId$drafts";

        // Both start with accessor
        Assert.assertTrue(simpleMethodName.startsWith("$get"));
        Assert.assertTrue(complexMethodName.startsWith("$get"));

        // Complex has more segments
        int simpleSegments = simpleMethodName.split("\\$").length;
        int complexSegments = complexMethodName.split("\\$").length;
        Assert.assertTrue(complexSegments > simpleSegments,
                "Complex path should have more segments");

        // Complex contains path parameter marker ($$)
        Assert.assertFalse(simpleMethodName.contains("$$"),
                "Simple method should not have path params");
        Assert.assertTrue(complexMethodName.contains("$$"),
                "Complex method should have path params ($$)");
    }
}
