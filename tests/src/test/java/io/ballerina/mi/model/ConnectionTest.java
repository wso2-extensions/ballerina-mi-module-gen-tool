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

import io.ballerina.projects.*;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for Connection model class.
 */
public class ConnectionTest {

    @BeforeMethod
    @AfterMethod
    public void resetConnector() {
        Connector.reset();
    }

    private Connector createTestConnector() {
        PackageDescriptor descriptor = mock(PackageDescriptor.class);
        PackageOrg packageOrg = mock(PackageOrg.class);
        PackageName packageName = mock(PackageName.class);
        PackageVersion packageVersion = mock(PackageVersion.class);

        when(descriptor.org()).thenReturn(packageOrg);
        when(descriptor.name()).thenReturn(packageName);
        when(descriptor.version()).thenReturn(packageVersion);
        when(packageOrg.value()).thenReturn("testOrg");
        when(packageName.value()).thenReturn("TestModule");
        when(packageVersion.value()).thenReturn(SemanticVersion.from("1.0.0"));

        return Connector.getConnector(descriptor);
    }

    @Test
    public void testConnectionCreation() {
        Connector connector = createTestConnector();
        Connection connection = new Connection(connector, "HttpClient", "HttpClientType", "0");

        Assert.assertSame(connection.getParent(), connector);
        Assert.assertEquals(connection.getConnectionType(), "HttpClient");
        Assert.assertEquals(connection.getObjectTypeName(), "HttpClientType");
        Assert.assertEquals(connection.getIndex(), "0");
    }

    @Test
    public void testConnectionWithNullParent() {
        Connection connection = new Connection(null, "Client", "ClientType", "1");

        Assert.assertNull(connection.getParent());
        Assert.assertEquals(connection.getConnectionType(), "Client");
    }

    @Test
    public void testDescriptionGetterSetter() {
        Connector connector = createTestConnector();
        Connection connection = new Connection(connector, "Client", "ClientType", "0");

        Assert.assertNull(connection.getDescription());

        connection.setDescription("A test connection");
        Assert.assertEquals(connection.getDescription(), "A test connection");

        connection.setDescription("Updated description");
        Assert.assertEquals(connection.getDescription(), "Updated description");
    }

    @Test
    public void testComponentsCollection() {
        Connector connector = createTestConnector();
        Connection connection = new Connection(connector, "Client", "ClientType", "0");

        ArrayList<Component> components = connection.getComponents();
        Assert.assertNotNull(components);
        Assert.assertTrue(components.isEmpty());
    }

    @Test
    public void testSetComponent() {
        Connector connector = createTestConnector();
        Connection connection = new Connection(connector, "Client", "ClientType", "0");

        Component component = new Component("fetchData", "Fetches data", FunctionType.REMOTE, "0", null, null, "json");
        connection.setComponent(component);

        Assert.assertEquals(connection.getComponents().size(), 1);
        Assert.assertSame(connection.getComponents().get(0), component);
        Assert.assertSame(component.getParent(), connection);
    }

    @Test
    public void testSetMultipleComponents() {
        Connector connector = createTestConnector();
        Connection connection = new Connection(connector, "Client", "ClientType", "0");

        Component comp1 = new Component("func1", null, FunctionType.REMOTE, "0", null, null, "string");
        Component comp2 = new Component("func2", null, FunctionType.REMOTE, "1", null, null, "int");
        Component comp3 = new Component("func3", null, FunctionType.RESOURCE, "2", null, null, "json");

        connection.setComponent(comp1);
        connection.setComponent(comp2);
        connection.setComponent(comp3);

        Assert.assertEquals(connection.getComponents().size(), 3);
        Assert.assertSame(comp1.getParent(), connection);
        Assert.assertSame(comp2.getParent(), connection);
        Assert.assertSame(comp3.getParent(), connection);
    }

    @Test
    public void testInitComponent() {
        Connector connector = createTestConnector();
        Connection connection = new Connection(connector, "Client", "ClientType", "0");

        Assert.assertNull(connection.getInitComponent());

        Component initComp = new Component("init", "Initialize connection", FunctionType.INIT, "0", null, null, "void");
        connection.setInitComponent(initComp);

        Assert.assertSame(connection.getInitComponent(), initComp);
        Assert.assertSame(initComp.getParent(), connection);
    }

    @Test
    public void testInitComponentOverwrite() {
        Connector connector = createTestConnector();
        Connection connection = new Connection(connector, "Client", "ClientType", "0");

        Component initComp1 = new Component("init1", null, FunctionType.INIT, "0", null, null, "void");
        Component initComp2 = new Component("init2", null, FunctionType.INIT, "0", null, null, "void");

        connection.setInitComponent(initComp1);
        Assert.assertSame(connection.getInitComponent(), initComp1);

        connection.setInitComponent(initComp2);
        Assert.assertSame(connection.getInitComponent(), initComp2);
        Assert.assertSame(initComp2.getParent(), connection);
    }

    @Test
    public void testConnectionWithEmptyStrings() {
        Connection connection = new Connection(null, "", "", "");

        Assert.assertEquals(connection.getConnectionType(), "");
        Assert.assertEquals(connection.getObjectTypeName(), "");
        Assert.assertEquals(connection.getIndex(), "");
    }

    @Test
    public void testConnectionWithNullValues() {
        Connection connection = new Connection(null, null, null, null);

        Assert.assertNull(connection.getConnectionType());
        Assert.assertNull(connection.getObjectTypeName());
        Assert.assertNull(connection.getIndex());
    }
}
