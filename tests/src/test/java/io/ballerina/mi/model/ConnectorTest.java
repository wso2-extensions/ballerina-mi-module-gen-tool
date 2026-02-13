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

import io.ballerina.projects.PackageDescriptor;
import io.ballerina.projects.PackageName;
import io.ballerina.projects.PackageOrg;
import io.ballerina.projects.PackageVersion;
import io.ballerina.projects.SemanticVersion;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for Connector model class.
 */
public class ConnectorTest {

    @BeforeMethod
    @AfterMethod
    public void resetConnector() {
        // Reset the singleton before and after each test
        Connector.reset();
    }

    private Connector createTestConnector(String moduleName, String orgName, String version) {
        PackageDescriptor descriptor = mock(PackageDescriptor.class);
        PackageOrg packageOrg = mock(PackageOrg.class);
        PackageName packageName = mock(PackageName.class);
        PackageVersion packageVersion = mock(PackageVersion.class);

        when(descriptor.org()).thenReturn(packageOrg);
        when(descriptor.name()).thenReturn(packageName);
        when(descriptor.version()).thenReturn(packageVersion);
        when(packageOrg.value()).thenReturn(orgName);
        when(packageName.value()).thenReturn(moduleName);

        String[] versionParts = version.split("\\.");
        int major = versionParts.length > 0 ? Integer.parseInt(versionParts[0]) : 1;
        int minor = versionParts.length > 1 ? Integer.parseInt(versionParts[1]) : 0;
        int patch = versionParts.length > 2 ? Integer.parseInt(versionParts[2]) : 0;
        SemanticVersion semVer = SemanticVersion.from(major + "." + minor + "." + patch);
        when(packageVersion.value()).thenReturn(semVer);

        return Connector.getConnector(descriptor);
    }

    @Test
    public void testConnectorCreation() {
        Connector connector = createTestConnector("TestModule", "wso2", "1.0.0");

        Assert.assertEquals(connector.getModuleName(), "TestModule");
        Assert.assertEquals(connector.getVersion(), "1.0.0");
        Assert.assertEquals(connector.getOrgName(), "wso2");
        Assert.assertEquals(connector.getMajorVersion(), "1");
    }

    @Test
    public void testDefaultDescription() {
        Connector connector = createTestConnector("TestModule", "org", "1.0.0");

        Assert.assertEquals(connector.getDescription(), "helps to connect with external systems");
    }

    @Test
    public void testSetDescription() {
        Connector connector = createTestConnector("TestModule", "org", "1.0.0");

        connector.setDescription("Custom connector description");
        Assert.assertEquals(connector.getDescription(), "Custom connector description");
    }

    @Test
    public void testIconPath() {
        Connector connector = createTestConnector("TestModule", "org", "1.0.0");

        Assert.assertNull(connector.getIconPath());

        connector.setIconPath("/path/to/icon.png");
        Assert.assertEquals(connector.getIconPath(), "/path/to/icon.png");
    }

    @Test
    public void testVersion() {
        Connector connector = createTestConnector("TestModule", "org", "1.0.0");

        Assert.assertEquals(connector.getVersion(), "1.0.0");

        connector.setVersion("2.0.0");
        Assert.assertEquals(connector.getVersion(), "2.0.0");
    }

    @Test
    public void testBalModuleFlag() {
        Connector connector = createTestConnector("TestModule", "org", "1.0.0");

        Assert.assertFalse(connector.isBalModule());

        connector.setBalModule(true);
        Assert.assertTrue(connector.isBalModule());

        connector.setBalModule(false);
        Assert.assertFalse(connector.isBalModule());
    }

    @Test
    public void testGenerationAborted() {
        Connector connector = createTestConnector("TestModule", "org", "1.0.0");

        Assert.assertFalse(connector.isGenerationAborted());
        Assert.assertNull(connector.getAbortionReason());

        connector.setGenerationAborted(true, "Missing required configuration");
        Assert.assertTrue(connector.isGenerationAborted());
        Assert.assertEquals(connector.getAbortionReason(), "Missing required configuration");
    }

    @Test
    public void testZipFileName_BalModule() {
        Connector connector = createTestConnector("MyModule", "org", "1.2.3");
        connector.setBalModule(true);

        String zipFileName = connector.getZipFileName();
        Assert.assertEquals(zipFileName, "MyModule-connector-1.2.3.zip");
    }

    @Test
    public void testZipFileName_NotBalModule() {
        Connector connector = createTestConnector("MyModule", "org", "1.2.3");
        connector.setBalModule(false);

        String zipFileName = connector.getZipFileName();
        Assert.assertEquals(zipFileName, "ballerina-connector-MyModule-1.2.3.zip");
    }

    @Test
    public void testConnectionsCollection() {
        Connector connector = createTestConnector("TestModule", "org", "1.0.0");

        Assert.assertNotNull(connector.getConnections());
        Assert.assertTrue(connector.getConnections().isEmpty());
    }

    @Test
    public void testSetConnection() {
        Connector connector = createTestConnector("TestModule", "org", "1.0.0");
        Connection connection = new Connection(connector, "HttpClient", "HttpClientType", "0");

        Component comp = new Component("fetchData", null, FunctionType.REMOTE, "0", null, null, "json");
        connection.setComponent(comp);

        connector.setConnection(connection);

        Assert.assertEquals(connector.getConnections().size(), 1);
        Assert.assertSame(connector.getConnections().get(0), connection);
        Assert.assertEquals(connector.getComponents().size(), 1);
    }

    @Test
    public void testComponentsCollection() {
        Connector connector = createTestConnector("TestModule", "org", "1.0.0");

        Assert.assertNotNull(connector.getComponents());
        Assert.assertTrue(connector.getComponents().isEmpty());
    }

    @Test
    public void testMultipleConnections() {
        Connector connector = createTestConnector("TestModule", "org", "1.0.0");

        Connection conn1 = new Connection(connector, "Client1", "Type1", "0");
        Connection conn2 = new Connection(connector, "Client2", "Type2", "1");

        Component comp1 = new Component("func1", null, FunctionType.REMOTE, "0", null, null, "void");
        Component comp2 = new Component("func2", null, FunctionType.REMOTE, "1", null, null, "void");

        conn1.setComponent(comp1);
        conn2.setComponent(comp2);

        connector.setConnection(conn1);
        connector.setConnection(conn2);

        Assert.assertEquals(connector.getConnections().size(), 2);
        Assert.assertEquals(connector.getComponents().size(), 2);
    }

    @Test
    public void testClearComponentData() {
        Connector connector = createTestConnector("TestModule", "org", "1.0.0");

        Connection connection = new Connection(connector, "Client", "Type", "0");
        Component comp = new Component("func", null, FunctionType.REMOTE, "0", null, null, "void");
        connection.setComponent(comp);
        connector.setConnection(connection);

        Assert.assertEquals(connector.getConnections().size(), 1);
        Assert.assertEquals(connector.getComponents().size(), 1);

        connector.clearComponentData();

        Assert.assertTrue(connector.getConnections().isEmpty());
        Assert.assertTrue(connector.getComponents().isEmpty());
    }

    @Test
    public void testStaticConstants() {
        Assert.assertEquals(Connector.TYPE_NAME, "connector");
        Assert.assertEquals(Connector.TEMP_PATH, "connector");
        Assert.assertEquals(Connector.ICON_FOLDER, "icon");
        Assert.assertEquals(Connector.SMALL_ICON_NAME, "icon-small.png");
        Assert.assertEquals(Connector.LARGE_ICON_NAME, "icon-large.png");
        Assert.assertEquals(Connector.LIB_PATH, "lib");
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testGetConnectorWithoutInit() {
        Connector.reset();
        Connector.getConnector();
    }

    @Test
    public void testClearTypeSymbols() {
        Connector connector = createTestConnector("TestModule", "org", "1.0.0");

        Connection connection = new Connection(connector, "Client", "Type", "0");
        Component comp = new Component("func", null, FunctionType.REMOTE, "0", null, null, "void");
        connection.setComponent(comp);

        Component initComp = new Component("init", null, FunctionType.INIT, "0", null, null, "void");
        connection.setInitComponent(initComp);

        connector.setConnection(connection);

        // Should not throw - clears TypeSymbol references
        connector.clearTypeSymbols();
    }

    @Test
    public void testSingletonBehavior() {
        Connector connector1 = createTestConnector("TestModule", "org", "1.0.0");
        Connector connector2 = Connector.getConnector();

        Assert.assertSame(connector1, connector2);
    }

    @Test
    public void testResetAndRecreate() {
        Connector connector1 = createTestConnector("Module1", "org1", "1.0.0");
        Assert.assertEquals(connector1.getModuleName(), "Module1");

        Connector.reset();

        Connector connector2 = createTestConnector("Module2", "org2", "2.0.0");
        Assert.assertEquals(connector2.getModuleName(), "Module2");
        Assert.assertNotSame(connector1, connector2);
    }
}
