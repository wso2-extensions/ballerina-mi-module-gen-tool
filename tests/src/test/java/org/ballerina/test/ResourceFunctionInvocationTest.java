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

import io.ballerina.runtime.api.values.BDecimal;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.stdlib.mi.BalExecutor;
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
 * Test class for verifying resource function invocation in BalExecutor.
 * <p>
 * This test validates that the BalExecutor correctly handles resource functions by:
 * <ul>
 *   <li>Detecting resource functions via the functionType property</li>
 *   <li>Using the resourceAccessor as the method name for invocation</li>
 *   <li>Prepending path parameters to the function arguments</li>
 *   <li>Converting path parameter types correctly</li>
 * </ul>
 */
public class ResourceFunctionInvocationTest {

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
     * Sets up a template context stack for template parameter lookup.
     */
    @SuppressWarnings("unchecked")
    private void setupTemplateContext(Map<String, Object> parameters) {
        Stack<TemplateContext> templateContextStack = new Stack<>();
        TemplateContext templateContext = new TemplateContext("testTemplate", new ArrayList<>());
        // Convert Map<String, Object> to HashMap<Object, Object> for setMappedValues
        HashMap<Object, Object> mappedValues = new HashMap<>();
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            mappedValues.put(entry.getKey(), entry.getValue());
        }
        templateContext.setMappedValues(mappedValues);
        templateContextStack.push(templateContext);
        messageContext.setProperty(Constants.SYNAPSE_FUNCTION_STACK, templateContextStack);
    }

    @DataProvider(name = "pathParamTypeConversionProvider")
    public Object[][] pathParamTypeConversionProvider() {
        return new Object[][]{
                // {inputValue, inputType, expectedClass}
                {"hello", "string", BString.class},
                {"123", "int", Long.class},
                {"45.67", "float", Double.class},
                {"true", "boolean", Boolean.class},
                {"false", "boolean", Boolean.class},
                {"123.456", "decimal", BDecimal.class},
                {"world", null, BString.class},  // null type defaults to string
        };
    }

    /**
     * Tests the convertPathParam method to ensure it correctly converts path parameter
     * values to the appropriate Ballerina types.
     */
    @Test(description = "Verify convertPathParam converts values to correct types",
            dataProvider = "pathParamTypeConversionProvider")
    public void testConvertPathParam(String inputValue, String inputType, Class<?> expectedClass) throws Exception {
        Method convertPathParamMethod = BalExecutor.class.getDeclaredMethod(
                "convertPathParam",
                String.class,
                String.class
        );
        convertPathParamMethod.setAccessible(true);

        Object result = convertPathParamMethod.invoke(balExecutor, inputValue, inputType);

        Assert.assertNotNull(result, "Converted path param should not be null");
        Assert.assertTrue(expectedClass.isInstance(result),
                "Expected " + expectedClass.getSimpleName() + " but got " + result.getClass().getSimpleName());

        // Verify specific values
        if ("string".equals(inputType) || inputType == null) {
            Assert.assertEquals(result.toString(), inputValue);
        } else if ("int".equals(inputType)) {
            Assert.assertEquals(result, Long.parseLong(inputValue));
        } else if ("float".equals(inputType)) {
            Assert.assertEquals((Double) result, Double.parseDouble(inputValue), 0.001);
        } else if ("boolean".equals(inputType)) {
            Assert.assertEquals(result, Boolean.parseBoolean(inputValue));
        }
    }

    /**
     * Tests the getPropertyAsString helper method.
     */
    @Test(description = "Verify getPropertyAsString returns correct string values")
    public void testGetPropertyAsString() throws Exception {
        Method getPropertyAsStringMethod = BalExecutor.class.getDeclaredMethod(
                "getPropertyAsString",
                MessageContext.class,
                String.class
        );
        getPropertyAsStringMethod.setAccessible(true);

        // Test with existing property
        messageContext.setProperty("testProperty", "testValue");
        Object result = getPropertyAsStringMethod.invoke(balExecutor, messageContext, "testProperty");
        Assert.assertEquals(result, "testValue");

        // Test with non-existing property
        Object nullResult = getPropertyAsStringMethod.invoke(balExecutor, messageContext, "nonExistentProperty");
        Assert.assertNull(nullResult);

        // Test with non-string property (should convert to string)
        messageContext.setProperty("intProperty", 123);
        Object intResult = getPropertyAsStringMethod.invoke(balExecutor, messageContext, "intProperty");
        Assert.assertEquals(intResult, "123");
    }

    /**
     * Tests the prependPathParams method with no path parameters.
     * When pathParamSize is 0, the original args array should be returned unchanged.
     */
    @Test(description = "Verify prependPathParams returns original args when no path params")
    public void testPrependPathParamsWithNoPathParams() throws Exception {
        Method prependPathParamsMethod = BalExecutor.class.getDeclaredMethod(
                "prependPathParams",
                Object[].class,
                MessageContext.class
        );
        prependPathParamsMethod.setAccessible(true);

        // Setup: no path params
        messageContext.setProperty(Constants.PATH_PARAM_SIZE, "0");

        Object[] originalArgs = new Object[]{"arg1", "arg2"};
        Object[] result = (Object[]) prependPathParamsMethod.invoke(balExecutor, originalArgs, messageContext);

        Assert.assertSame(result, originalArgs, "Should return same array when no path params");
        Assert.assertEquals(result.length, 2);
    }

    /**
     * Tests the prependPathParams method with one path parameter.
     */
    @Test(description = "Verify prependPathParams correctly prepends single path param")
    public void testPrependPathParamsWithSinglePathParam() throws Exception {
        Method prependPathParamsMethod = BalExecutor.class.getDeclaredMethod(
                "prependPathParams",
                Object[].class,
                MessageContext.class
        );
        prependPathParamsMethod.setAccessible(true);

        // Setup: one path param
        messageContext.setProperty(Constants.PATH_PARAM_SIZE, "1");
        messageContext.setProperty("pathParam0", "itemId");
        messageContext.setProperty("pathParamType0", "string");

        // Setup template context with path param value
        Map<String, Object> templateParams = new HashMap<>();
        templateParams.put("itemId", "item-123");
        setupTemplateContext(templateParams);

        Object[] originalArgs = new Object[]{"existingArg"};
        Object[] result = (Object[]) prependPathParamsMethod.invoke(balExecutor, originalArgs, messageContext);

        Assert.assertEquals(result.length, 2, "Result should have path param + original args");
        Assert.assertTrue(result[0] instanceof BString, "First element should be path param (BString)");
        Assert.assertEquals(result[0].toString(), "item-123");
        Assert.assertEquals(result[1], "existingArg");
    }

    /**
     * Tests the prependPathParams method with multiple path parameters.
     */
    @Test(description = "Verify prependPathParams correctly prepends multiple path params")
    public void testPrependPathParamsWithMultiplePathParams() throws Exception {
        Method prependPathParamsMethod = BalExecutor.class.getDeclaredMethod(
                "prependPathParams",
                Object[].class,
                MessageContext.class
        );
        prependPathParamsMethod.setAccessible(true);

        // Setup: two path params
        messageContext.setProperty(Constants.PATH_PARAM_SIZE, "2");
        messageContext.setProperty("pathParam0", "userId");
        messageContext.setProperty("pathParamType0", "int");
        messageContext.setProperty("pathParam1", "orderId");
        messageContext.setProperty("pathParamType1", "string");

        // Setup template context with path param values
        Map<String, Object> templateParams = new HashMap<>();
        templateParams.put("userId", "42");
        templateParams.put("orderId", "order-abc");
        setupTemplateContext(templateParams);

        Object[] originalArgs = new Object[]{"bodyParam"};
        Object[] result = (Object[]) prependPathParamsMethod.invoke(balExecutor, originalArgs, messageContext);

        Assert.assertEquals(result.length, 3, "Result should have 2 path params + 1 original arg");
        Assert.assertTrue(result[0] instanceof Long, "First path param should be Long (int type)");
        Assert.assertEquals(result[0], 42L);
        Assert.assertTrue(result[1] instanceof BString, "Second path param should be BString");
        Assert.assertEquals(result[1].toString(), "order-abc");
        Assert.assertEquals(result[2], "bodyParam");
    }

    /**
     * Tests the prependPathParams method with path parameter type conversions.
     */
    @Test(description = "Verify prependPathParams correctly converts path param types")
    public void testPrependPathParamsWithTypeConversions() throws Exception {
        Method prependPathParamsMethod = BalExecutor.class.getDeclaredMethod(
                "prependPathParams",
                Object[].class,
                MessageContext.class
        );
        prependPathParamsMethod.setAccessible(true);

        // Setup: path params with different types
        messageContext.setProperty(Constants.PATH_PARAM_SIZE, "4");
        messageContext.setProperty("pathParam0", "stringParam");
        messageContext.setProperty("pathParamType0", "string");
        messageContext.setProperty("pathParam1", "intParam");
        messageContext.setProperty("pathParamType1", "int");
        messageContext.setProperty("pathParam2", "floatParam");
        messageContext.setProperty("pathParamType2", "float");
        messageContext.setProperty("pathParam3", "boolParam");
        messageContext.setProperty("pathParamType3", "boolean");

        // Setup template context with path param values
        Map<String, Object> templateParams = new HashMap<>();
        templateParams.put("stringParam", "hello");
        templateParams.put("intParam", "123");
        templateParams.put("floatParam", "45.67");
        templateParams.put("boolParam", "true");
        setupTemplateContext(templateParams);

        Object[] originalArgs = new Object[]{};
        Object[] result = (Object[]) prependPathParamsMethod.invoke(balExecutor, originalArgs, messageContext);

        Assert.assertEquals(result.length, 4);
        Assert.assertTrue(result[0] instanceof BString);
        Assert.assertEquals(result[0].toString(), "hello");
        Assert.assertTrue(result[1] instanceof Long);
        Assert.assertEquals(result[1], 123L);
        Assert.assertTrue(result[2] instanceof Double);
        Assert.assertEquals((Double) result[2], 45.67, 0.001);
        Assert.assertTrue(result[3] instanceof Boolean);
        Assert.assertEquals(result[3], true);
    }

    /**
     * Tests that resource function detection works correctly based on functionType property.
     */
    @Test(description = "Verify resource function is detected via functionType property")
    public void testResourceFunctionDetection() throws Exception {
        Method getPropertyAsStringMethod = BalExecutor.class.getDeclaredMethod(
                "getPropertyAsString",
                MessageContext.class,
                String.class
        );
        getPropertyAsStringMethod.setAccessible(true);

        // Test RESOURCE type
        messageContext.setProperty(Constants.FUNCTION_TYPE, Constants.FUNCTION_TYPE_RESOURCE);
        String functionType = (String) getPropertyAsStringMethod.invoke(balExecutor, messageContext, Constants.FUNCTION_TYPE);
        Assert.assertEquals(functionType, "RESOURCE");
        Assert.assertTrue(Constants.FUNCTION_TYPE_RESOURCE.equals(functionType));

        // Test REMOTE type
        messageContext.setProperty(Constants.FUNCTION_TYPE, Constants.FUNCTION_TYPE_REMOTE);
        functionType = (String) getPropertyAsStringMethod.invoke(balExecutor, messageContext, Constants.FUNCTION_TYPE);
        Assert.assertEquals(functionType, "REMOTE");

        // Test FUNCTION type
        messageContext.setProperty(Constants.FUNCTION_TYPE, Constants.FUNCTION_TYPE_FUNCTION);
        functionType = (String) getPropertyAsStringMethod.invoke(balExecutor, messageContext, Constants.FUNCTION_TYPE);
        Assert.assertEquals(functionType, "FUNCTION");
    }

    /**
     * Tests that resource accessor is correctly retrieved for resource functions.
     */
    @Test(description = "Verify resourceAccessor property retrieval for resource functions")
    public void testResourceAccessorRetrieval() throws Exception {
        Method getPropertyAsStringMethod = BalExecutor.class.getDeclaredMethod(
                "getPropertyAsString",
                MessageContext.class,
                String.class
        );
        getPropertyAsStringMethod.setAccessible(true);

        // Test different HTTP accessors
        String[] accessors = {"get", "post", "put", "delete", "patch", "head", "options"};

        for (String accessor : accessors) {
            messageContext.setProperty(Constants.RESOURCE_ACCESSOR, accessor);
            String result = (String) getPropertyAsStringMethod.invoke(balExecutor, messageContext, Constants.RESOURCE_ACCESSOR);
            Assert.assertEquals(result, accessor, "Resource accessor should be: " + accessor);
        }
    }

    /**
     * Tests the complete resource function invocation setup with all properties.
     */
    @Test(description = "Verify complete resource function invocation setup")
    public void testCompleteResourceFunctionSetup() throws Exception {
        // Setup all properties needed for resource function invocation
        messageContext.setProperty(Constants.FUNCTION_TYPE, Constants.FUNCTION_TYPE_RESOURCE);
        messageContext.setProperty(Constants.RESOURCE_ACCESSOR, "get");
        messageContext.setProperty(Constants.PATH_PARAM_SIZE, "1");
        messageContext.setProperty(Constants.SIZE, "2");  // paramSize for other params
        messageContext.setProperty(Constants.FUNCTION_NAME, "getItemsByItemId");  // synapse name
        messageContext.setProperty("pathParam0", "itemId");
        messageContext.setProperty("pathParamType0", "string");

        // Setup template context
        Map<String, Object> templateParams = new HashMap<>();
        templateParams.put("itemId", "item-456");
        templateParams.put("param0Value", "someValue");
        setupTemplateContext(templateParams);

        // Verify all properties are set correctly
        Method getPropertyAsStringMethod = BalExecutor.class.getDeclaredMethod(
                "getPropertyAsString",
                MessageContext.class,
                String.class
        );
        getPropertyAsStringMethod.setAccessible(true);

        Assert.assertEquals(getPropertyAsStringMethod.invoke(balExecutor, messageContext, Constants.FUNCTION_TYPE),
                "RESOURCE");
        Assert.assertEquals(getPropertyAsStringMethod.invoke(balExecutor, messageContext, Constants.RESOURCE_ACCESSOR),
                "get");
        Assert.assertEquals(getPropertyAsStringMethod.invoke(balExecutor, messageContext, Constants.PATH_PARAM_SIZE),
                "1");
        Assert.assertEquals(getPropertyAsStringMethod.invoke(balExecutor, messageContext, "pathParam0"),
                "itemId");
        Assert.assertEquals(getPropertyAsStringMethod.invoke(balExecutor, messageContext, "pathParamType0"),
                "string");

        // Verify prependPathParams works with this setup
        Method prependPathParamsMethod = BalExecutor.class.getDeclaredMethod(
                "prependPathParams",
                Object[].class,
                MessageContext.class
        );
        prependPathParamsMethod.setAccessible(true);

        Object[] originalArgs = new Object[]{"otherParam"};
        Object[] result = (Object[]) prependPathParamsMethod.invoke(balExecutor, originalArgs, messageContext);

        Assert.assertEquals(result.length, 2);
        Assert.assertEquals(result[0].toString(), "item-456");  // path param prepended
        Assert.assertEquals(result[1], "otherParam");  // original arg preserved
    }

    /**
     * Tests that when pathParamSize is null or missing, prependPathParams handles it gracefully.
     */
    @Test(description = "Verify prependPathParams handles missing pathParamSize gracefully")
    public void testPrependPathParamsWithMissingPathParamSize() throws Exception {
        Method prependPathParamsMethod = BalExecutor.class.getDeclaredMethod(
                "prependPathParams",
                Object[].class,
                MessageContext.class
        );
        prependPathParamsMethod.setAccessible(true);

        // Don't set PATH_PARAM_SIZE - it should default to 0
        Object[] originalArgs = new Object[]{"arg1", "arg2"};
        Object[] result = (Object[]) prependPathParamsMethod.invoke(balExecutor, originalArgs, messageContext);

        Assert.assertSame(result, originalArgs, "Should return same array when pathParamSize is missing");
    }

    /**
     * Tests decimal type path parameter conversion.
     */
    @Test(description = "Verify decimal path parameter conversion")
    public void testDecimalPathParamConversion() throws Exception {
        Method convertPathParamMethod = BalExecutor.class.getDeclaredMethod(
                "convertPathParam",
                String.class,
                String.class
        );
        convertPathParamMethod.setAccessible(true);

        Object result = convertPathParamMethod.invoke(balExecutor, "123.456789", "decimal");

        Assert.assertNotNull(result);
        Assert.assertTrue(result instanceof BDecimal, "Should be BDecimal type");
        Assert.assertTrue(result.toString().contains("123.456789"));
    }
}
