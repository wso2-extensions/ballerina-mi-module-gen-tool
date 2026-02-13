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

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests for FunctionType enum.
 */
public class FunctionTypeTest {

    @Test
    public void testFunctionTypeValues() {
        FunctionType[] values = FunctionType.values();
        Assert.assertEquals(values.length, 4);
    }

    @Test
    public void testFunctionTypeFunction() {
        Assert.assertEquals(FunctionType.FUNCTION.toString(), "FUNCTION");
        Assert.assertEquals(FunctionType.valueOf("FUNCTION"), FunctionType.FUNCTION);
    }

    @Test
    public void testFunctionTypeRemote() {
        Assert.assertEquals(FunctionType.REMOTE.toString(), "REMOTE");
        Assert.assertEquals(FunctionType.valueOf("REMOTE"), FunctionType.REMOTE);
    }

    @Test
    public void testFunctionTypeInit() {
        Assert.assertEquals(FunctionType.INIT.toString(), "INIT");
        Assert.assertEquals(FunctionType.valueOf("INIT"), FunctionType.INIT);
    }

    @Test
    public void testFunctionTypeResource() {
        Assert.assertEquals(FunctionType.RESOURCE.toString(), "RESOURCE");
        Assert.assertEquals(FunctionType.valueOf("RESOURCE"), FunctionType.RESOURCE);
    }

    @Test
    public void testFunctionTypeOrdinals() {
        Assert.assertEquals(FunctionType.FUNCTION.ordinal(), 0);
        Assert.assertEquals(FunctionType.REMOTE.ordinal(), 1);
        Assert.assertEquals(FunctionType.INIT.ordinal(), 2);
        Assert.assertEquals(FunctionType.RESOURCE.ordinal(), 3);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testInvalidFunctionType() {
        FunctionType.valueOf("INVALID");
    }

    @Test
    public void testFunctionTypeEquality() {
        FunctionType type1 = FunctionType.FUNCTION;
        FunctionType type2 = FunctionType.FUNCTION;
        FunctionType type3 = FunctionType.REMOTE;

        Assert.assertEquals(type1, type2);
        Assert.assertNotEquals(type1, type3);
    }
}
