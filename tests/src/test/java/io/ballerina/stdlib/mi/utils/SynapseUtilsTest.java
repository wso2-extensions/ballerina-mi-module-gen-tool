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


}
