/*
 * Copyright (c) 2025, WSO2 LLC. (http://wso2.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.ballerina.mi.test.util;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * This test class is intended to be run manually by developers when expected artifacts need to be updated.
 * <p>
 * <b>Instructions:</b>
 * <ul>
 *   <li>By default, all test methods in this class are disabled (enabled = false).</li>
 *   <li>To run a test, set {@code enabled = true} in the corresponding {@code @Test} annotation.</li>
 *   <li>Execute the test class using your preferred TestNG runner (e.g., via your IDE or command line).</li>
 *   <li>After running, verify that the expected artifacts have been updated as intended.</li>
 * </ul>
 */
public class TestArtifactGenerationUtil {

    @BeforeClass
    public void setup() {
        ArtifactGenerationUtil.setupBallerinaHome();
    }

    @Test(description = "Generates expected artifacts for project1", enabled = false) // Set enabled to true to run manually
    public void generateProject1ExpectedArtifacts() throws Exception {
        String projectPath = "src/test/resources/ballerina/project1";
        String projectName = "project1";
        ArtifactGenerationUtil.generateExpectedArtifacts(projectPath, projectName);
        System.out.println("Expected artifacts for project1 generated successfully.");
    }

    @Test(description = "Generates expected artifacts for project2", enabled = false) // Set enabled to true to run manually
    public void generateProject2ExpectedArtifacts() throws Exception {
        String projectPath = "src/test/resources/ballerina/project2";
        String projectName = "project2";
        ArtifactGenerationUtil.generateExpectedArtifacts(projectPath, projectName);
        System.out.println("Expected artifacts for project2 generated successfully.");
    }

    @Test(description = "Generates expected artifacts for project3", enabled = false) // Set enabled to true to run manually
    public void generateProject3ExpectedArtifacts() throws Exception {
        String projectPath = "src/test/resources/ballerina/project3";
        String projectName = "project3";
        ArtifactGenerationUtil.generateExpectedArtifacts(projectPath, projectName);
        System.out.println("Expected artifacts for project3 generated successfully.");
    }

    @Test(description = "Generates expected artifacts for project4", enabled = false) // Set enabled to true to run manually (disabled by default)
    public void generateProject4ExpectedArtifacts() throws Exception {
        String sourceProjectPath = "src/test/resources/ballerina/project4";
        String projectName = "project4";
        Path projectPath = Paths.get(sourceProjectPath);
        Path balaPath = ArtifactGenerationUtil.packBallerinaProject(projectPath);
        
        // Extract bala to temporary directory since ProjectLoader doesn't support .bala files directly
        Path tempBalaDir = java.nio.file.Files.createTempDirectory("bala-test-" + projectName);
        try {
            // Extract the bala file
            try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(
                    java.nio.file.Files.newInputStream(balaPath))) {
                java.util.zip.ZipEntry entry = zis.getNextEntry();
                while (entry != null) {
                    Path filePath = tempBalaDir.resolve(entry.getName());
                    if (entry.isDirectory()) {
                        java.nio.file.Files.createDirectories(filePath);
                    } else {
                        java.nio.file.Files.createDirectories(filePath.getParent());
                        java.nio.file.Files.copy(zis, filePath, 
                                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    }
                    zis.closeEntry();
                    entry = zis.getNextEntry();
                }
            }
            
            // Use expected path's parent as target so generated folder will be at expectedPath level
            Path expectedPath = Paths.get("src/test/resources/expected", projectName);
            Path tempTargetPath = expectedPath.getParent();
            
            ArtifactGenerationUtil.generateExpectedArtifacts(
                    tempBalaDir.toAbsolutePath().toString(), 
                    tempTargetPath.toAbsolutePath().toString(), 
                    projectName);
            
            // Copy generated artifacts from tempTargetPath/generated to expectedPath
            Path generatedPath = tempTargetPath.resolve("generated");
            if (java.nio.file.Files.exists(generatedPath)) {
                // Clean up existing expected path if it exists
                if (java.nio.file.Files.exists(expectedPath)) {
                    try (var walk = java.nio.file.Files.walk(expectedPath)) {
                        walk.sorted((a, b) -> b.compareTo(a))
                            .forEach(path -> {
                                try {
                                    java.nio.file.Files.delete(path);
                                } catch (java.io.IOException e) {
                                    // Ignore cleanup errors
                                }
                            });
                    }
                }
                java.nio.file.Files.createDirectories(expectedPath);
                
                // Copy all contents from generated to expectedPath
                try (var walk = java.nio.file.Files.walk(generatedPath)) {
                    walk.forEach(source -> {
                        try {
                            Path destination = expectedPath.resolve(generatedPath.relativize(source));
                            if (java.nio.file.Files.isDirectory(source)) {
                                java.nio.file.Files.createDirectories(destination);
                            } else {
                                java.nio.file.Files.createDirectories(destination.getParent());
                                java.nio.file.Files.copy(source, destination, 
                                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                            }
                        } catch (java.io.IOException e) {
                            throw new RuntimeException("Failed to copy artifact: " + source, e);
                        }
                    });
                }
                
                // Clean up the generated folder
                try (var walk = java.nio.file.Files.walk(generatedPath)) {
                    walk.sorted((a, b) -> b.compareTo(a))
                        .forEach(path -> {
                            try {
                                java.nio.file.Files.delete(path);
                            } catch (java.io.IOException e) {
                                // Ignore cleanup errors
                            }
                        });
                }
            }
            
            System.out.println("Expected artifacts for project4 generated successfully.");
        } finally {
            // Clean up temporary directory
            if (java.nio.file.Files.exists(tempBalaDir)) {
                try (var walk = java.nio.file.Files.walk(tempBalaDir)) {
                    walk.sorted((a, b) -> b.compareTo(a))
                        .forEach(path -> {
                            try {
                                java.nio.file.Files.delete(path);
                            } catch (java.io.IOException e) {
                                // Ignore cleanup errors
                            }
                        });
                } catch (java.io.IOException e) {
                    // Ignore cleanup errors
                }
            }
        }
    }

    @Test(description = "Generates expected artifacts for project5 from Central", enabled = false) // Set enabled to true to run manually
    public void generateProject5ExpectedArtifacts() throws Exception {
        String projectName = "project5";
        // Pull package from Ballerina Central
        String centralPackage = "ballerina/http:2.15.3"; 
        Path balaDir = ArtifactGenerationUtil.pullPackageFromCentral(centralPackage);
        
        // Use expected path's parent as target so generated folder will be at expectedPath level
        Path expectedPath = Paths.get("src/test/resources/expected", projectName);
        Path tempTargetPath = expectedPath.getParent();
        
        ArtifactGenerationUtil.generateExpectedArtifacts(
                balaDir.toAbsolutePath().toString(), 
                tempTargetPath.toAbsolutePath().toString(), 
                projectName);
        
        // Copy generated artifacts from tempTargetPath/generated to expectedPath
        Path generatedPath = tempTargetPath.resolve("generated");
        if (java.nio.file.Files.exists(generatedPath)) {
            // Clean up existing expected path if it exists
            if (java.nio.file.Files.exists(expectedPath)) {
                try (var walk = java.nio.file.Files.walk(expectedPath)) {
                    walk.sorted((a, b) -> b.compareTo(a))
                        .forEach(path -> {
                            try {
                                java.nio.file.Files.delete(path);
                            } catch (java.io.IOException e) {
                                // Ignore cleanup errors
                            }
                        });
                }
            }
            java.nio.file.Files.createDirectories(expectedPath);
            
            // Copy all contents from generated to expectedPath
            try (var walk = java.nio.file.Files.walk(generatedPath)) {
                walk.forEach(source -> {
                    try {
                        Path destination = expectedPath.resolve(generatedPath.relativize(source));
                        if (java.nio.file.Files.isDirectory(source)) {
                            java.nio.file.Files.createDirectories(destination);
                        } else {
                            java.nio.file.Files.createDirectories(destination.getParent());
                            java.nio.file.Files.copy(source, destination, 
                                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        }
                    } catch (java.io.IOException e) {
                        throw new RuntimeException("Failed to copy artifact: " + source, e);
                    }
                });
            }
            
            // Clean up the generated folder
            try (var walk = java.nio.file.Files.walk(generatedPath)) {
                walk.sorted((a, b) -> b.compareTo(a))
                    .forEach(path -> {
                        try {
                            java.nio.file.Files.delete(path);
                        } catch (java.io.IOException e) {
                            // Ignore cleanup errors
                        }
                    });
            }
        }
        
        System.out.println("Expected artifacts for project5 generated successfully from Central package: " + centralPackage);
    }
}
