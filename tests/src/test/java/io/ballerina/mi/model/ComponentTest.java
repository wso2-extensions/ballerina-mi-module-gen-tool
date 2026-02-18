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

import java.util.ArrayList;
import java.util.List;

/**
 * Tests for Component model class.
 */
public class ComponentTest {

    @Test
    public void testComponentCreation() {
        Component component = new Component("testFunc", "Test documentation",
            FunctionType.FUNCTION, "0", null, null, "string");

        Assert.assertEquals(component.getName(), "testFunc");
        Assert.assertEquals(component.getDocumentation(), "Test documentation");
        Assert.assertEquals(component.getFunctionType(), FunctionType.FUNCTION);
        Assert.assertEquals(component.getIndex(), "0");
        Assert.assertEquals(component.getReturnType(), "string");
        Assert.assertEquals(component.getType(), "component");
    }

    @Test
    public void testSettersAndGetters() {
        Component component = new Component("func", null, FunctionType.REMOTE, "1", null, null, "int");

        component.setName("newName");
        Assert.assertEquals(component.getName(), "newName");

        component.setObjectTypeName("MyObject");
        Assert.assertEquals(component.getObjectTypeName(), "MyObject");
    }

    @Test
    public void testResourceFunctionDetection() {
        Component resourceComponent = new Component("getUsers", null, FunctionType.RESOURCE, "0", null, null, "json");
        Assert.assertTrue(resourceComponent.isResourceFunction());

        Component remoteComponent = new Component("fetchData", null, FunctionType.REMOTE, "0", null, null, "json");
        Assert.assertFalse(remoteComponent.isResourceFunction());

        Component regularFunction = new Component("helper", null, FunctionType.FUNCTION, "0", null, null, "void");
        Assert.assertFalse(regularFunction.isResourceFunction());
    }

    @Test
    public void testResourceAccessor() {
        Component component = new Component("getItems", null, FunctionType.RESOURCE, "0", null, null, "json");

        Assert.assertNull(component.getResourceAccessor());

        component.setResourceAccessor("get");
        Assert.assertEquals(component.getResourceAccessor(), "get");

        component.setResourceAccessor("post");
        Assert.assertEquals(component.getResourceAccessor(), "post");
    }

    @Test
    public void testPathParamSize() {
        // Null path params
        Component component1 = new Component("func", null, FunctionType.RESOURCE, "0", null, null, "void");
        Assert.assertEquals(component1.getPathParamSize(), 0);
    }

    @Test
    public void testResourcePathSegments() {
        Component component = new Component("getUsers", null, FunctionType.RESOURCE, "0", null, null, "json");

        Assert.assertNull(component.getResourcePathSegments());
        Assert.assertEquals(component.getResourcePathSegmentSize(), 0);

        List<ResourcePathSegment> segments = new ArrayList<>();
        segments.add(new ResourcePathSegment("users")); // static segment
        segments.add(new ResourcePathSegment("userId", "string")); // path param segment

        component.setResourcePathSegments(segments);

        Assert.assertEquals(component.getResourcePathSegments().size(), 2);
        Assert.assertEquals(component.getResourcePathSegmentSize(), 2);
    }

    @Test
    public void testOperationIdFlag() {
        Component component = new Component("getUsers", null, FunctionType.RESOURCE, "0", null, null, "json");

        Assert.assertFalse(component.hasOperationId());

        component.setHasOperationId(true);
        Assert.assertTrue(component.hasOperationId());

        component.setHasOperationId(false);
        Assert.assertFalse(component.hasOperationId());
    }

    @Test
    public void testJvmMethodName_NonResource() {
        Component component = new Component("fetchData", null, FunctionType.REMOTE, "0", null, null, "json");
        Assert.assertNull(component.getJvmMethodName());
    }

    @Test
    public void testJvmMethodName_NoAccessor() {
        Component component = new Component("getItems", null, FunctionType.RESOURCE, "0", null, null, "json");
        Assert.assertNull(component.getJvmMethodName());
    }

    @Test
    public void testJvmMethodName_SimpleResource() {
        Component component = new Component("getItems", null, FunctionType.RESOURCE, "0", null, null, "json");
        component.setResourceAccessor("get");

        List<ResourcePathSegment> segments = new ArrayList<>();
        segments.add(new ResourcePathSegment("items"));
        component.setResourcePathSegments(segments);

        Assert.assertEquals(component.getJvmMethodName(), "$get$items");
    }

    @Test
    public void testJvmMethodName_WithPathParam() {
        Component component = new Component("getItem", null, FunctionType.RESOURCE, "0", null, null, "json");
        component.setResourceAccessor("get");

        List<ResourcePathSegment> segments = new ArrayList<>();
        segments.add(new ResourcePathSegment("items"));
        segments.add(new ResourcePathSegment("itemId", "string")); // path param
        component.setResourcePathSegments(segments);

        String methodName = component.getJvmMethodName();
        Assert.assertNotNull(methodName);
        Assert.assertTrue(methodName.startsWith("$get$items"));
    }

    @Test
    public void testDisplayName_NonResource() {
        Component component = new Component("getAllCountriesAndProvincesStatus", null, FunctionType.REMOTE, "0", null, null, "json");

        String displayName = component.getDisplayName();
        Assert.assertEquals(displayName, "get all countries and provinces status");
    }

    @Test
    public void testDisplayName_ResourceWithOperationId() {
        Component component = new Component("getUserProfile", "Fetches the user profile", FunctionType.RESOURCE, "0", null, null, "json");
        component.setHasOperationId(true);

        String displayName = component.getDisplayName();
        Assert.assertEquals(displayName, "get user profile");
    }

    @Test
    public void testDisplayName_ResourceWithDocumentation() {
        Component component = new Component("get_api_v3_serverinfo", "Get server information", FunctionType.RESOURCE, "0", null, null, "json");
        // No operationId set

        String displayName = component.getDisplayName();
        Assert.assertEquals(displayName, "Get server information");
    }

    @Test
    public void testDisplayName_ResourceWithMultilineDocumentation() {
        Component component = new Component("getUsers", "Retrieves all users from the system.\nThis is a detailed description.", FunctionType.RESOURCE, "0", null, null, "json");

        String displayName = component.getDisplayName();
        Assert.assertEquals(displayName, "Retrieves all users from the system.");
    }

    @Test
    public void testDisplayName_ResourceWithPeriodInDocumentation() {
        Component component = new Component("getUsers", "Retrieves all users. More details here.", FunctionType.RESOURCE, "0", null, null, "json");

        String displayName = component.getDisplayName();
        Assert.assertEquals(displayName, "Retrieves all users.");
    }

    @Test
    public void testDisplayName_ResourceNoDocNoOperationId() {
        Component component = new Component("get_api_users", null, FunctionType.RESOURCE, "0", null, null, "json");

        String displayName = component.getDisplayName();
        Assert.assertEquals(displayName, "get api users");
    }

    @Test
    public void testDisplayName_LongDocumentation() {
        String longDoc = "This is a very long documentation string that exceeds one hundred characters and should be truncated to maintain readability in the user interface";
        Component component = new Component("longDocFunc", longDoc, FunctionType.RESOURCE, "0", null, null, "json");

        String displayName = component.getDisplayName();
        Assert.assertTrue(displayName.length() <= 103); // 100 + "..."
        Assert.assertTrue(displayName.endsWith("..."));
    }

    @Test
    public void testFunctionParams() {
        Component component = new Component("func", null, FunctionType.FUNCTION, "0", null, null, "void");

        Assert.assertNotNull(component.getFunctionParams());
        Assert.assertTrue(component.getFunctionParams().isEmpty());
    }

}
