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

package io.ballerina.mi.analyzer;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Optional;

/**
 * Tests for SynapseNameContext class.
 */
public class SynapseNameContextTest {

    @Test
    public void testEmptyContext() {
        SynapseNameContext context = SynapseNameContext.builder().build();

        Assert.assertFalse(context.getOperationId().isPresent());
        Assert.assertFalse(context.getResourcePathSegments().isPresent());
        Assert.assertFalse(context.getModule().isPresent());
    }

    @Test
    public void testWithOperationId() {
        SynapseNameContext context = SynapseNameContext.builder()
                .operationId("getUserProfile")
                .build();

        Assert.assertTrue(context.getOperationId().isPresent());
        Assert.assertEquals(context.getOperationId().get(), "getUserProfile");
        Assert.assertFalse(context.getResourcePathSegments().isPresent());
        Assert.assertFalse(context.getModule().isPresent());
    }

    @Test
    public void testWithNullOperationId() {
        SynapseNameContext context = SynapseNameContext.builder()
                .operationId(null)
                .build();

        Assert.assertFalse(context.getOperationId().isPresent());
    }

    @Test
    public void testWithResourcePathSegments() {
        SynapseNameContext context = SynapseNameContext.builder()
                .resourcePathSegments(new ArrayList<>())
                .build();

        Assert.assertTrue(context.getResourcePathSegments().isPresent());
        Assert.assertTrue(context.getResourcePathSegments().get().isEmpty());
    }

    @Test
    public void testWithNullResourcePathSegments() {
        SynapseNameContext context = SynapseNameContext.builder()
                .resourcePathSegments(null)
                .build();

        Assert.assertFalse(context.getResourcePathSegments().isPresent());
    }

    @Test
    public void testWithModule() {
        // Module is an interface that would require mocking
        // Test with null to ensure the optional behavior works
        SynapseNameContext context = SynapseNameContext.builder()
                .module(null)
                .build();

        Assert.assertFalse(context.getModule().isPresent());
    }

    @Test
    public void testBuilderChaining() {
        SynapseNameContext context = SynapseNameContext.builder()
                .operationId("getUsers")
                .resourcePathSegments(new ArrayList<>())
                .module(null)
                .build();

        Assert.assertTrue(context.getOperationId().isPresent());
        Assert.assertEquals(context.getOperationId().get(), "getUsers");
        Assert.assertTrue(context.getResourcePathSegments().isPresent());
        Assert.assertFalse(context.getModule().isPresent());
    }

    @Test
    public void testMultipleBuilds() {
        SynapseNameContext.Builder builder = SynapseNameContext.builder()
                .operationId("operation1");

        SynapseNameContext context1 = builder.build();
        Assert.assertEquals(context1.getOperationId().get(), "operation1");

        // Builder can be reused
        builder.operationId("operation2");
        SynapseNameContext context2 = builder.build();
        Assert.assertEquals(context2.getOperationId().get(), "operation2");

        // First context should still have original value
        Assert.assertEquals(context1.getOperationId().get(), "operation1");
    }

    @Test
    public void testBuilderStaticMethod() {
        SynapseNameContext.Builder builder = SynapseNameContext.builder();
        Assert.assertNotNull(builder);
    }
}
