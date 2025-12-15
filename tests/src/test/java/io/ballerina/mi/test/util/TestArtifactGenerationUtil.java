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

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utility to regenerate expected connector artifacts used by the tests.
 * <p>
 * Run via Gradle: {@code ./gradlew :mi-tests:generateExpectedArtifacts -PartifactTarget=<project>}.
 */
public class TestArtifactGenerationUtil {

    private static final String[] DEFAULT_CENTRAL_PACKAGES = {"ballerinax/milvus:1.1.0","ballerinax/azure.ai.search:1.0.0"};

    public static void main(String[] args) throws Exception {
        if (args.length == 0 || args[0].isBlank()) {
            printUsageAndExit();
        }
        setup();

        String target = args[0];
        switch (target) {
            case "project1" -> generateProject1ExpectedArtifacts();
            case "project2" -> generateProject2ExpectedArtifacts();
            case "project3" -> generateProject3ExpectedArtifacts();
            case "project4" -> generateProject4ExpectedArtifacts();
            case "central" -> generateCentralExpectedArtifacts(resolveCentralPackages());
            default -> printUsageAndExit();
        }
    }

    private static void setup() {
        ArtifactGenerationUtil.setupBallerinaHome();
    }

    public static void generateProject1ExpectedArtifacts() throws Exception {
        String projectPath = "src/test/resources/ballerina/project1";
        String projectName = "project1";
        ArtifactGenerationUtil.generateExpectedArtifacts(projectPath, projectName);
        System.out.println("Expected artifacts for project1 generated successfully.");
    }

    public static void generateProject2ExpectedArtifacts() throws Exception {
        String projectPath = "src/test/resources/ballerina/project2";
        String projectName = "project2";
        ArtifactGenerationUtil.generateExpectedArtifacts(projectPath, projectName);
        System.out.println("Expected artifacts for project2 generated successfully.");
    }

    public static void generateProject3ExpectedArtifacts() throws Exception {
        String projectPath = "src/test/resources/ballerina/project3";
        String projectName = "project3";
        ArtifactGenerationUtil.generateExpectedArtifacts(projectPath, projectName);
        System.out.println("Expected artifacts for project3 generated successfully.");
    }

    public static void generateProject4ExpectedArtifacts() throws Exception {
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

    public static void generateCentralExpectedArtifacts(String[] centralPackages) throws Exception {
        for (String centralPackage : centralPackages) {
            // Pull package from Ballerina Central
            Path balaDir = ArtifactGenerationUtil.pullPackageFromCentral(centralPackage);

            String connectorFolderName = deriveConnectorFolderName(centralPackage);

            // Use expected path's parent as target so generated folder will be at expectedPath level
            Path expectedPath = Paths.get("src/test/resources/expected", connectorFolderName);
            Path tempTargetPath = expectedPath.getParent();

            ArtifactGenerationUtil.generateExpectedArtifacts(
                    balaDir.toAbsolutePath().toString(),
                    tempTargetPath.toAbsolutePath().toString(),
                    connectorFolderName);

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

            System.out.println("Expected artifacts for " + connectorFolderName +
                    " generated successfully from Central package: " + centralPackage);
        }
    }

    private static String[] resolveCentralPackages() {
        String centralPackageProp = System.getProperty("centralPackage");
        if (centralPackageProp == null || centralPackageProp.isBlank()) {
            return DEFAULT_CENTRAL_PACKAGES;
        }
        return centralPackageProp.split("\\s*,\\s*");
    }

    private static String deriveConnectorFolderName(String centralPackage) {
        String[] parts = centralPackage.split(":");
        String orgPackage = parts.length > 0 ? parts[0] : centralPackage;
        String[] orgPackageParts = orgPackage.split("/");
        if (orgPackageParts.length == 2) {
            return orgPackageParts[0] + "-" + orgPackageParts[1]; // e.g., ballerinax-milvus
        }
        // Fallback: sanitize to a folder-friendly name
        return orgPackage.replace("/", "-").replace(":", "-");
    }

    private static void printUsageAndExit() {
        System.err.println("Usage: gradle :mi-tests:generateExpectedArtifacts "
                + "-PartifactTarget=<project1|project2|project3|project4|central> "
                + "[-PcentralPackage=<org/name:version,org2/name2:version2,...>]");
        System.exit(1);
    }
}
