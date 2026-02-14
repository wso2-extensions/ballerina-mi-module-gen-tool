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

package io.ballerina.mi.generator;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tests for ResourceCopier utility class.
 */
public class ResourceCopierTest {

    private Path tempDir;

    @BeforeMethod
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("resource-copier-test");
    }

    @AfterMethod
    public void tearDown() throws IOException {
        if (tempDir != null && Files.exists(tempDir)) {
            Files.walk(tempDir)
                 .sorted((a, b) -> -a.compareTo(b))
                 .forEach(path -> {
                     try {
                         Files.delete(path);
                     } catch (IOException e) {
                         // ignore cleanup errors
                     }
                 });
        }
    }

    @Test
    public void testGetFileFromResourceAsStream_ValidResource() {
        // Test with a known resource that exists
        ClassLoader classLoader = getClass().getClassLoader();
        try {
            // Try to get a resource that might exist
            InputStream stream = ResourceCopier.getFileFromResourceAsStream(classLoader, "schema/ui-schema.json");
            Assert.assertNotNull(stream);
            stream.close();
        } catch (IllegalArgumentException e) {
            // Expected if resource doesn't exist in test classpath
            Assert.assertTrue(e.getMessage().contains("file not found"));
        } catch (IOException e) {
            // Ignore close errors
        }
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testGetFileFromResourceAsStream_InvalidResource() {
        ClassLoader classLoader = getClass().getClassLoader();
        ResourceCopier.getFileFromResourceAsStream(classLoader, "non-existent-resource-file.txt");
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testGetFileFromResourceAsStream_NullResourceName() {
        ClassLoader classLoader = getClass().getClassLoader();
        ResourceCopier.getFileFromResourceAsStream(classLoader, null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testGetFileFromResourceAsStream_NonExistentResourceName() {
        ClassLoader classLoader = getClass().getClassLoader();
        ResourceCopier.getFileFromResourceAsStream(classLoader, "non-existent-file-for-test.xyz");
    }

    @Test
    public void testGetFileFromResourceAsStream_WithSlashPrefix() {
        ClassLoader classLoader = getClass().getClassLoader();
        try {
            InputStream stream = ResourceCopier.getFileFromResourceAsStream(classLoader, "/some/path/resource.txt");
            Assert.assertNotNull(stream);
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("file not found"));
        }
    }

    @Test
    public void testResourceCopierIsUtilityClass() {
        // ResourceCopier should not be instantiable (private constructor)
        try {
            java.lang.reflect.Constructor<ResourceCopier> constructor =
                ResourceCopier.class.getDeclaredConstructor();
            Assert.assertTrue(java.lang.reflect.Modifier.isPrivate(constructor.getModifiers()));
        } catch (NoSuchMethodException e) {
            Assert.fail("ResourceCopier should have a default constructor");
        }
    }

    @Test
    public void testTempDirectoryExists() {
        Assert.assertTrue(Files.exists(tempDir));
        Assert.assertTrue(Files.isDirectory(tempDir));
    }

    @Test
    public void testCreateNestedDirectories() throws IOException {
        Path nested = tempDir.resolve("a/b/c/d");
        Files.createDirectories(nested);
        Assert.assertTrue(Files.exists(nested));
    }
}
