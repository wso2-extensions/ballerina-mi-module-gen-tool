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

package io.ballerina.stdlib.mi;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests for Mediator class.
 * Note: Full integration tests require Ballerina runtime initialization.
 */
public class MediatorTest {

    @Test
    public void testDefaultConstructor() {
        Mediator mediator = new Mediator();
        Assert.assertNotNull(mediator);
    }

    @Test
    public void testSetAndGetOrgName() {
        Mediator mediator = new Mediator();
        Assert.assertNull(mediator.getOrgName());

        mediator.setOrgName("wso2");
        Assert.assertEquals(mediator.getOrgName(), "wso2");
    }

    @Test
    public void testSetAndGetModuleName() {
        Mediator mediator = new Mediator();
        Assert.assertNull(mediator.getModuleName());

        mediator.setModuleName("mymodule");
        Assert.assertEquals(mediator.getModuleName(), "mymodule");
    }

    @Test
    public void testSetAndGetVersion() {
        Mediator mediator = new Mediator();
        Assert.assertNull(mediator.getVersion());

        mediator.setVersion("1.0.0");
        Assert.assertEquals(mediator.getVersion(), "1.0.0");
    }

    @Test
    public void testSetAllProperties() {
        Mediator mediator = new Mediator();

        mediator.setOrgName("testOrg");
        mediator.setModuleName("testModule");
        mediator.setVersion("2.0.0");

        Assert.assertEquals(mediator.getOrgName(), "testOrg");
        Assert.assertEquals(mediator.getModuleName(), "testModule");
        Assert.assertEquals(mediator.getVersion(), "2.0.0");
    }

    @Test
    public void testMultipleSetCalls() {
        Mediator mediator = new Mediator();

        // First set
        mediator.setOrgName("org1");
        Assert.assertEquals(mediator.getOrgName(), "org1");

        // Override
        mediator.setOrgName("org2");
        Assert.assertEquals(mediator.getOrgName(), "org2");
    }

    @Test
    public void testSetNullValues() {
        Mediator mediator = new Mediator();

        mediator.setOrgName("org");
        mediator.setOrgName(null);
        Assert.assertNull(mediator.getOrgName());

        mediator.setModuleName("module");
        mediator.setModuleName(null);
        Assert.assertNull(mediator.getModuleName());

        mediator.setVersion("1.0.0");
        mediator.setVersion(null);
        Assert.assertNull(mediator.getVersion());
    }

    @Test
    public void testSetEmptyValues() {
        Mediator mediator = new Mediator();

        mediator.setOrgName("");
        Assert.assertEquals(mediator.getOrgName(), "");

        mediator.setModuleName("");
        Assert.assertEquals(mediator.getModuleName(), "");

        mediator.setVersion("");
        Assert.assertEquals(mediator.getVersion(), "");
    }
}
