/*
 * Copyright (c) 2026, WSO2 LLC. (https://www.wso2.com).
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

package io.ballerina.stdlib.mi.utils;

import io.ballerina.stdlib.mi.Constants;
import org.apache.synapse.MessageContext;
import org.apache.synapse.mediators.template.TemplateContext;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SynapseUtilsTest {

    @Test
    public void testGetPropertyAsString() {
        MessageContext context = mock(MessageContext.class);
        when(context.getProperty("key")).thenReturn("value");
        Assert.assertEquals(SynapseUtils.getPropertyAsString(context, "key"), "value");

        when(context.getProperty("missing")).thenReturn(null);
        Assert.assertNull(SynapseUtils.getPropertyAsString(context, "missing"));
    }

    @Test
    public void testResolveSynapseExpressions() {
        MessageContext context = mock(MessageContext.class);
        String plain = "plain text";
        Assert.assertEquals(SynapseUtils.resolveSynapseExpressions(plain, context), "plain text");

        // This test only covers the no-match path. The match path requires mocking SynapseExpression constructor or external system.
        // Assuming no exception thrown for valid input.
    }

    @Test
    public void testCleanupJsonString() {
        Assert.assertEquals(SynapseUtils.cleanupJsonString("{\"a\":1, \"b\":2}"), "{\"a\":1, \"b\":2}");
        Assert.assertEquals(SynapseUtils.cleanupJsonString("{\"a\":1, }"), "{\"a\":1}");
        Assert.assertEquals(SynapseUtils.cleanupJsonString("[1, 2, ]"), "[1, 2]");
        Assert.assertEquals(SynapseUtils.cleanupJsonString(null), null);
    }

    @Test
    public void testFindConnectionTypeForParam() {
        MessageContext context = mock(MessageContext.class);
        Stack<TemplateContext> stack = new Stack<>();

        TemplateContext tc = mock(TemplateContext.class);
        when(tc.getParameterValue("connectionType")).thenReturn("GMAIL");

        stack.push(tc);

        when(context.getProperty(Constants.SYNAPSE_FUNCTION_STACK)).thenReturn(stack);

        String connectionType = SynapseUtils.findConnectionTypeForParam(context, "someParam");

        Assert.assertEquals(connectionType, "GMAIL");
    }

    @Test
    public void testFindConnectionTypeForParam_NullValue() {
        MessageContext context = mock(MessageContext.class);
        Stack<TemplateContext> stack = new Stack<>();

        TemplateContext tc = mock(TemplateContext.class);
        when(tc.getParameterValue("connectionType")).thenReturn(null);

        stack.push(tc);

        when(context.getProperty(Constants.SYNAPSE_FUNCTION_STACK)).thenReturn(stack);

        String connectionType = SynapseUtils.findConnectionTypeForParam(context, "someParam");

        Assert.assertNull(connectionType);
    }

    @Test
    public void testLookupTemplateParameter_NullStack() {
        MessageContext context = mock(MessageContext.class);
        when(context.getProperty(Constants.SYNAPSE_FUNCTION_STACK)).thenReturn(null);
        when(context.getProperty("paramName")).thenReturn("directValue");

        Object result = SynapseUtils.lookupTemplateParameter(context, "paramName");

        Assert.assertEquals(result, "directValue");
    }

    @Test
    public void testLookupTemplateParameter_EmptyStack() {
        MessageContext context = mock(MessageContext.class);
        Stack<TemplateContext> stack = new Stack<>();

        when(context.getProperty(Constants.SYNAPSE_FUNCTION_STACK)).thenReturn(stack);
        when(context.getProperty("paramName")).thenReturn("fromContext");

        Object result = SynapseUtils.lookupTemplateParameter(context, "paramName");

        Assert.assertEquals(result, "fromContext");
    }

    @Test
    public void testLookupTemplateParameter_FromTemplateContext() {
        MessageContext context = mock(MessageContext.class);
        Stack<TemplateContext> stack = new Stack<>();

        TemplateContext tc = mock(TemplateContext.class);
        when(tc.getParameterValue("myParam")).thenReturn("templateValue");

        stack.push(tc);

        when(context.getProperty(Constants.SYNAPSE_FUNCTION_STACK)).thenReturn(stack);

        Object result = SynapseUtils.lookupTemplateParameter(context, "myParam");

        Assert.assertEquals(result, "templateValue");
    }

    @Test
    public void testLookupTemplateParameter_DottedParamName() {
        MessageContext context = mock(MessageContext.class);
        Stack<TemplateContext> stack = new Stack<>();

        TemplateContext tc = mock(TemplateContext.class);
        Map<String, Object> mappedValues = new HashMap<>();
        mappedValues.put("parent.child", "nestedValue");

        when(tc.getParameterValue("parent.child")).thenReturn(null);
        when(tc.getMappedValues()).thenReturn(mappedValues);

        stack.push(tc);

        when(context.getProperty(Constants.SYNAPSE_FUNCTION_STACK)).thenReturn(stack);

        Object result = SynapseUtils.lookupTemplateParameter(context, "parent.child");

        // Returns null because the getParameterValue returns null
        Assert.assertNull(result);
    }

    @Test
    public void testFindParentUnionPath() {
        java.util.Set<String> unionPaths = new java.util.HashSet<>();
        unionPaths.add("config.auth");
        unionPaths.add("settings.type");

        String result1 = SynapseUtils.findParentUnionPath("config.auth.username", unionPaths);
        Assert.assertEquals(result1, "config.auth");

        String result2 = SynapseUtils.findParentUnionPath("settings.type.value", unionPaths);
        Assert.assertEquals(result2, "settings.type");

        String result3 = SynapseUtils.findParentUnionPath("other.field", unionPaths);
        Assert.assertNull(result3);
    }

    @Test
    public void testFindParentUnionPath_EmptySet() {
        java.util.Set<String> unionPaths = new java.util.HashSet<>();

        String result = SynapseUtils.findParentUnionPath("any.path", unionPaths);
        Assert.assertNull(result);
    }

    @Test
    public void testFindParentUnionPath_ExactMatch() {
        java.util.Set<String> unionPaths = new java.util.HashSet<>();
        unionPaths.add("config");

        // Exact match shouldn't return anything (need trailing dot)
        String result = SynapseUtils.findParentUnionPath("config", unionPaths);
        Assert.assertNull(result);
    }

    @Test
    public void testCleanupJsonString_MultipleTrailingCommas() {
        String json = "{\"a\":1, \"b\":2, }";
        Assert.assertEquals(SynapseUtils.cleanupJsonString(json), "{\"a\":1, \"b\":2}");

        String arrayJson = "[1, 2, 3, ]";
        Assert.assertEquals(SynapseUtils.cleanupJsonString(arrayJson), "[1, 2, 3]");
    }

    @Test
    public void testCleanupJsonString_NestedTrailingCommas() {
        String json = "{\"arr\":[1, 2, ], }";
        Assert.assertEquals(SynapseUtils.cleanupJsonString(json), "{\"arr\":[1, 2]}");
    }

    @Test
    public void testCleanupJsonString_NoTrailingCommas() {
        String json = "{\"a\":1, \"b\":2}";
        Assert.assertEquals(SynapseUtils.cleanupJsonString(json), "{\"a\":1, \"b\":2}");
    }

    @Test
    public void testGetPropertyAsString_NonStringValue() {
        MessageContext context = mock(MessageContext.class);
        when(context.getProperty("intKey")).thenReturn(123);

        String result = SynapseUtils.getPropertyAsString(context, "intKey");
        Assert.assertEquals(result, "123");
    }

    @Test
    public void testResolveSynapseExpressions_NullContext() {
        String text = "plain text without expressions";
        String result = SynapseUtils.resolveSynapseExpressions(text, null);
        Assert.assertEquals(result, "plain text without expressions");
    }

    @Test
    public void testResolveSynapseExpressions_EmptyString() {
        MessageContext context = mock(MessageContext.class);
        String result = SynapseUtils.resolveSynapseExpressions("", context);
        Assert.assertEquals(result, "");
    }
}
