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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tests for ResourcePackager class.
 * Note: Full integration tests with packageConnector() require complex
 * file system setup and are tested via integration tests instead.
 */
public class ResourcePackagerTest {

    private Path tempSourceDir;
    private Path tempTargetDir;

    @BeforeMethod
    public void setUp() throws IOException {
        tempSourceDir = Files.createTempDirectory("resource-packager-source");
        tempTargetDir = Files.createTempDirectory("resource-packager-target");
    }

    @AfterMethod
    public void tearDown() throws IOException {
        // Clean up temp directories
        if (tempSourceDir != null) {
            Files.walk(tempSourceDir)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            // ignore
                        }
                    });
        }
        if (tempTargetDir != null) {
            Files.walk(tempTargetDir)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            // ignore
                        }
                    });
        }
    }

    @Test
    public void testResourcePackagerConstruction() {
        ResourcePackager packager = new ResourcePackager(tempSourceDir, tempTargetDir);
        Assert.assertNotNull(packager);
    }

    @Test
    public void testResourcePackagerWithNullPaths() {
        // Should allow null paths (validation happens at runtime)
        ResourcePackager packager = new ResourcePackager(null, null);
        Assert.assertNotNull(packager);
    }

    @Test
    public void testResourcePackagerWithValidPaths() {
        Path sourcePath = tempSourceDir.resolve("source");
        Path targetPath = tempTargetDir.resolve("target");

        ResourcePackager packager = new ResourcePackager(sourcePath, targetPath);
        Assert.assertNotNull(packager);
    }

    @Test
    public void testTempDirectoriesCreated() {
        Assert.assertTrue(Files.exists(tempSourceDir));
        Assert.assertTrue(Files.exists(tempTargetDir));
        Assert.assertTrue(Files.isDirectory(tempSourceDir));
        Assert.assertTrue(Files.isDirectory(tempTargetDir));
    }
}
