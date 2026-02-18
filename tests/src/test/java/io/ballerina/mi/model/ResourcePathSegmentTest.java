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

package io.ballerina.mi.model;

import io.ballerina.mi.model.param.ResourcePathSegment;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests for ResourcePathSegment class.
 */
public class ResourcePathSegmentTest {

    @Test
    public void testStaticSegmentCreation() {
        ResourcePathSegment segment = new ResourcePathSegment("users");

        Assert.assertEquals(segment.getValue(), "users");
        Assert.assertFalse(segment.isParameter());
        Assert.assertNull(segment.getParameterType());
    }

    @Test
    public void testParameterSegmentCreation() {
        ResourcePathSegment segment = new ResourcePathSegment("userId", "string");

        Assert.assertEquals(segment.getValue(), "userId");
        Assert.assertTrue(segment.isParameter());
        Assert.assertEquals(segment.getParameterType(), "string");
    }

    @Test
    public void testJvmMethodNameComponent_StaticSegment() {
        ResourcePathSegment segment = new ResourcePathSegment("users");
        Assert.assertEquals(segment.toJvmMethodNameComponent(), "$users");

        ResourcePathSegment segment2 = new ResourcePathSegment("drafts");
        Assert.assertEquals(segment2.toJvmMethodNameComponent(), "$drafts");
    }

    @Test
    public void testJvmMethodNameComponent_ParameterSegment() {
        ResourcePathSegment segment = new ResourcePathSegment("userId", "string");
        Assert.assertEquals(segment.toJvmMethodNameComponent(), "$$");

        ResourcePathSegment segment2 = new ResourcePathSegment("id", "int");
        Assert.assertEquals(segment2.toJvmMethodNameComponent(), "$$");
    }

    @Test
    public void testToString_StaticSegment() {
        ResourcePathSegment segment = new ResourcePathSegment("users");
        Assert.assertEquals(segment.toString(), "users");
    }

    @Test
    public void testToString_ParameterSegment() {
        ResourcePathSegment segment = new ResourcePathSegment("userId", "string");
        Assert.assertEquals(segment.toString(), "[string userId]");

        ResourcePathSegment segment2 = new ResourcePathSegment("id", "int");
        Assert.assertEquals(segment2.toString(), "[int id]");
    }

    @Test
    public void testConstructors() {
        // Test single-argument constructor for static segment
        ResourcePathSegment staticSegment = new ResourcePathSegment("items");
        Assert.assertEquals(staticSegment.getValue(), "items");
        Assert.assertFalse(staticSegment.isParameter());
        Assert.assertNull(staticSegment.getParameterType());

        // Test two-argument constructor for parameter segment
        ResourcePathSegment paramSegment = new ResourcePathSegment("itemId", "string");
        Assert.assertEquals(paramSegment.getValue(), "itemId");
        Assert.assertTrue(paramSegment.isParameter());
        Assert.assertEquals(paramSegment.getParameterType(), "string");
    }

    @Test
    public void testMultipleSegmentsJvmMethodName() {
        // Simulate constructing a JVM method name for: get users/[string userId]/drafts
        StringBuilder methodName = new StringBuilder("$get");

        ResourcePathSegment seg1 = new ResourcePathSegment("users");
        ResourcePathSegment seg2 = new ResourcePathSegment("userId", "string");
        ResourcePathSegment seg3 = new ResourcePathSegment("drafts");

        methodName.append(seg1.toJvmMethodNameComponent());
        methodName.append(seg2.toJvmMethodNameComponent());
        methodName.append(seg3.toJvmMethodNameComponent());

        Assert.assertEquals(methodName.toString(), "$get$users$$$drafts");
    }

    @Test
    public void testDifferentParameterTypes() {
        ResourcePathSegment stringParam = new ResourcePathSegment("name", "string");
        Assert.assertEquals(stringParam.getParameterType(), "string");
        Assert.assertEquals(stringParam.toString(), "[string name]");

        ResourcePathSegment intParam = new ResourcePathSegment("count", "int");
        Assert.assertEquals(intParam.getParameterType(), "int");
        Assert.assertEquals(intParam.toString(), "[int count]");

        ResourcePathSegment floatParam = new ResourcePathSegment("score", "float");
        Assert.assertEquals(floatParam.getParameterType(), "float");
        Assert.assertEquals(floatParam.toString(), "[float score]");
    }

    @Test
    public void testEmptyValue() {
        ResourcePathSegment segment = new ResourcePathSegment("");
        Assert.assertEquals(segment.getValue(), "");
        Assert.assertEquals(segment.toJvmMethodNameComponent(), "$");
    }

    @Test
    public void testSpecialCharactersInValue() {
        ResourcePathSegment segment = new ResourcePathSegment("api-v2");
        Assert.assertEquals(segment.getValue(), "api-v2");
        Assert.assertEquals(segment.toJvmMethodNameComponent(), "$api-v2");
    }
}
