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

import io.ballerina.mi.MiCmd;
import org.testng.Assert;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Utility class for generating and managing expected artifacts for integration tests.
 * <p>
 * <b>Purpose:</b> This class provides methods to programmatically generate expected output artifacts
 * for Ballerina MI connector projects. It is primarily used in test scenarios to ensure that
 * the expected outputs are up-to-date and consistent with the current project state.
 * </p>
 * <p>
 * <b>Directory Structure Assumptions:</b>
 * <ul>
 *   <li>Test resources are located under <code>src/test/resources</code>.</li>
 *   <li>Expected artifacts are stored in <code>src/test/resources/expected/&lt;projectName&gt;</code>.</li>
 *   <li>Generated connector artifacts are produced in <code>&lt;projectPath&gt;/target/&lt;projectName&gt;-mi-connector</code>.</li>
 * </ul>
 * </p>
 * <p>
 * <b>Artifact Generation Process:</b>
 * <ol>
 *   <li>Cleans up any existing expected artifacts for the given project.</li>
 *   <li>Executes the {@link io.ballerina.mi.MiCmd} command to generate new artifacts.</li>
 *   <li>Copies the generated artifacts from the build output directory to the expected directory.</li>
 * </ol>
 * </p>
 * <p>
 * <b>Usage:</b> Call {@link #generateExpectedArtifacts(String, String)} with the project path and name
 * to refresh the expected artifacts before running assertions in tests. For bala projects that require a custom
 * output directory, use {@link #generateExpectedArtifacts(String, String, String)} and provide the target path.
 * </p>
 */
public class ArtifactGenerationUtil {
    private static final Path RESOURCES_DIR = Paths.get("src", "test", "resources");
    private static final Path EXPECTED_DIR = RESOURCES_DIR.resolve("expected");

    public static void generateExpectedArtifacts(String projectPathStr, String projectName) throws Exception {
        generateExpectedArtifacts(projectPathStr, null, projectName);
    }

    public static void generateExpectedArtifacts(String projectPathStr, String targetPathStr, String projectName)
            throws Exception {
        System.out.println("Starting generateExpectedArtifacts for project: " + projectName);
        System.out.println("ProjectPathStr: " + projectPathStr);
        Path projectPath = Paths.get(projectPathStr);
        Path expectedOutputPath = EXPECTED_DIR.resolve(projectName);
        Path targetDir = targetPathStr == null || targetPathStr.isBlank()
                ? projectPath.resolve("target")
                : Paths.get(targetPathStr);

        // 1. Clean up existing expected artifacts
        if (Files.exists(expectedOutputPath)) {
            System.out.println("Cleaning up existing expected artifacts at: " + expectedOutputPath);
            try (Stream<Path> walk = Files.walk(expectedOutputPath)) {
                walk.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            System.out.println("Deleting: " + path);
                            Files.delete(path);
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to delete " + path, e);
                        }
                    });
            }
        }
        Files.createDirectories(expectedOutputPath);
        System.out.println("Ensured expected output directory exists: " + expectedOutputPath);
        Files.createDirectories(targetDir);
        System.out.println("Ensured target directory exists: " + targetDir);

        // 1.5. Clean up old zip files from target directory to avoid picking up wrong artifacts
        System.out.println("Cleaning up old zip files from target directory: " + targetDir);
        try (Stream<Path> oldZips = Files.walk(targetDir, 2)) {
            oldZips.filter(p -> p.toString().endsWith(".zip"))
                    .forEach(zipPath -> {
                        try {
                            System.out.println("Deleting old zip: " + zipPath);
                            Files.delete(zipPath);
                        } catch (IOException e) {
                            System.out.println("WARN: Failed to delete old zip " + zipPath + ": " + e.getMessage());
                        }
                    });
        }

        // 2. Programmatically execute MiCmd
        System.out.println("Executing MiCmd for project: " + projectPathStr);
        // Reset Connector state to avoid pollution from previous tests
        io.ballerina.mi.connectorModel.Connector.reset();
        
        MiCmd miCmd = new MiCmd();
        Field sourcePathField = MiCmd.class.getDeclaredField("sourcePath");
        sourcePathField.setAccessible(true);
        sourcePathField.set(miCmd, projectPathStr);
        if (targetPathStr == null || targetPathStr.isBlank()) {
            targetPathStr = projectPathStr + File.separator + "target";
        }
        Field targetPathField = MiCmd.class.getDeclaredField("targetPath");
        targetPathField.setAccessible(true);
        targetPathField.set(miCmd, targetPathStr);

        miCmd.execute();
        System.out.println("MiCmd execution completed.");

        // 3. Find the generated zip file and unzip it to the expected directory
        //    The zip file name pattern for connectors is: "ballerina-connector-<moduleName>-<version>.zip"
        //    For Central packages, projectName is typically "<org>-<module>", e.g., "ballerinax-googleapis.gmail"
        //    We need to match the module name part (after the first '-') in the zip filename.
        Optional<Path> generatedZipFile;
        // Derive a matcher key from projectName. Extract the module name part after the first '-'
        final String matchKey;
        int dashIndex = projectName.indexOf('-');
        if (dashIndex >= 0 && dashIndex + 1 < projectName.length()) {
            matchKey = projectName.substring(dashIndex + 1);
        } else {
            matchKey = projectName;
        }
        
        System.out.println("Looking for zip file matching module: " + matchKey);
        // Search in targetDir and its parent (zip might be created in parent for bala projects)
        Path[] searchPaths = {targetDir, targetDir.getParent() != null ? targetDir.getParent() : targetDir};
        generatedZipFile = Optional.empty();
        for (Path searchPath : searchPaths) {
            if (!Files.exists(searchPath)) {
                continue;
            }
            try (Stream<Path> files = Files.walk(searchPath, 2)) {
                Optional<Path> found = files
                        .filter(p -> {
                            try {
                                return p.toString().endsWith(".zip");
                            } catch (Exception e) {
                                // Skip files that can't be accessed
                                return false;
                            }
                        })
                        .filter(p -> {
                            String zipName = p.getFileName().toString();
                            // Match zip files that contain the module name
                            // Pattern: ballerina-connector-<moduleName>-<version>.zip
                            boolean matches = zipName.contains(matchKey);
                            if (matches) {
                                System.out.println("Found matching zip: " + zipName + " in " + searchPath);
                            }
                            return matches;
                        })
                        .findFirst();
                if (found.isPresent()) {
                    generatedZipFile = found;
                    break;
                }
            } catch (IOException | UncheckedIOException e) {
                // Skip directories that can't be accessed (e.g., systemd private directories)
                // UncheckedIOException wraps IOException that occurs during stream iteration
                System.out.println("WARN: Unable to walk directory " + searchPath + ": " + e.getMessage());
            }
        }

        if (generatedZipFile.isEmpty()) {
            // List all available zip files for debugging
            System.out.println("No matching zip file found. Available zip files:");
            for (Path searchPath : searchPaths) {
                if (Files.exists(searchPath)) {
                    System.out.println("  In " + searchPath + ":");
                    try (Stream<Path> files = Files.walk(searchPath, 2)) {
                        files.filter(p -> {
                                    try {
                                        return p.toString().endsWith(".zip");
                                    } catch (Exception e) {
                                        return false;
                                    }
                                })
                                .forEach(p -> System.out.println("    - " + p.getFileName()));
                    } catch (IOException | UncheckedIOException e) {
                        // UncheckedIOException wraps IOException that occurs during stream iteration
                        System.out.println("    WARN: Unable to access directory: " + e.getMessage());
                    }
                }
            }
            throw new AssertionError("Generated zip file not found for project '" + projectName + 
                    "' (expected to contain '" + matchKey + "') in " + targetDir + 
                    ". This usually means no components were generated (all methods were skipped).");
        }

        Path generatedConnectorPath = generatedZipFile.get();
        Assert.assertTrue(Files.exists(generatedConnectorPath), "Generated connector zip does not exist: " + generatedConnectorPath);
        System.out.println("Generated connector zip exists. Unzipping artifacts from " + generatedConnectorPath + " to " + expectedOutputPath);

        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(generatedConnectorPath))) {
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                Path newPath = expectedOutputPath.resolve(zipEntry.getName());
                if (zipEntry.isDirectory()) {
                    Files.createDirectories(newPath);
                } else {
                    if (newPath.getParent() != null) {
                        if (Files.notExists(newPath.getParent())) {
                            Files.createDirectories(newPath.getParent());
                        }
                    }
                    Files.copy(zis, newPath, StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
                zipEntry = zis.getNextEntry();
            }
        }

        System.out.println("Artifacts copied successfully to: " + expectedOutputPath);
        System.out.println("--- Completed generateExpectedArtifacts for project: " + projectName + " ---");
    }

    public static void setupBallerinaHome() {
        String balCommand = System.getProperty("bal.command");
        if (balCommand != null) {
            Path ballerinaHome = Paths.get(balCommand).getParent().getParent();
            System.setProperty("ballerina.home", ballerinaHome.toString());
        }
    }

    public static Path packBallerinaProject(Path projectPath) throws Exception {
        String balCommand = System.getProperty("bal.command");
        if (balCommand == null || balCommand.isBlank()) {
            throw new IllegalStateException("System property 'bal.command' is not set. Cannot run 'bal pack'.");
        }

        ProcessBuilder processBuilder = new ProcessBuilder(balCommand, "pack");
        processBuilder.directory(projectPath.toFile());
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        StringBuilder output = new StringBuilder();
        try (InputStream inputStream = process.getInputStream();
             InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             BufferedReader bufferedReader = new BufferedReader(reader)) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("'bal pack' failed for " + projectPath + " with exit code " + exitCode +
                    System.lineSeparator() + output);
        }

        try (Stream<Path> files = Files.walk(projectPath.resolve("target"))) {
            return files.filter(path -> path.toString().endsWith(".bala"))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No .bala artifact found under " + projectPath));
        }
    }

    /**
     * Pulls a package from Ballerina Central and returns the path to the bala directory.
     * The bala directory structure in Central is: ~/.ballerina/repositories/central.ballerina.io/bala/org/package/version/platform/
     *
     * @param packageName Package name in format "org/package" or "org/package:version"
     * @return Path to the extracted bala directory in Central repository
     * @throws Exception if pull fails or package not found
     */
    public static Path pullPackageFromCentral(String packageName) throws Exception {
        String balCommand = System.getProperty("bal.command");
        if (balCommand == null || balCommand.isBlank()) {
            throw new IllegalStateException("System property 'bal.command' is not set. Cannot run 'bal pull'.");
        }

        System.out.println("Pulling package from Central: " + packageName);
        ProcessBuilder processBuilder = new ProcessBuilder(balCommand, "pull", packageName);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        StringBuilder output = new StringBuilder();
        try (InputStream inputStream = process.getInputStream();
             InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             BufferedReader bufferedReader = new BufferedReader(reader)) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            String errorOutput = output.toString();
            // "Package already exists" is not an error
            if (!errorOutput.contains("Package already exists") && !errorOutput.contains("already exists")) {
                throw new RuntimeException("'bal pull' failed for " + packageName + " with exit code " + exitCode +
                        System.lineSeparator() + errorOutput);
            }
        }

        // Parse package name to extract org and package name
        String[] parts = packageName.split(":");
        String orgPackage = parts[0];
        String[] orgPackageParts = orgPackage.split("/");
        if (orgPackageParts.length != 2) {
            throw new IllegalArgumentException("Invalid package name format. Expected 'org/package' or 'org/package:version', got: " + packageName);
        }
        String org = orgPackageParts[0];
        String packageNameOnly = orgPackageParts[1];

        // Find the package in Central repository
        String homeDir = System.getProperty("user.home");
        Path centralRepoBase = Paths.get(homeDir, ".ballerina", "repositories", "central.ballerina.io", "bala", org, packageNameOnly);

        if (!Files.exists(centralRepoBase)) {
            throw new IllegalStateException("Package not found in Central repository: " + centralRepoBase);
        }

        // Find the latest version directory or specified version
        Path versionDir;
        if (parts.length > 1) {
            // Specific version requested
            versionDir = centralRepoBase.resolve(parts[1]);
        } else {
            // Find latest version (highest version number)
            try (Stream<Path> versionDirs = Files.list(centralRepoBase)) {
                versionDir = versionDirs
                        .filter(Files::isDirectory)
                        .max(Comparator.comparing(Path::getFileName, (v1, v2) -> {
                            // Simple version comparison - find highest
                            return v1.toString().compareTo(v2.toString());
                        }))
                        .orElseThrow(() -> new IllegalStateException("No version directory found for " + packageName));
            }
        }

        // Find platform directory (java21, any, etc.)
        Path platformDir;
        try (Stream<Path> platformDirs = Files.list(versionDir)) {
            platformDir = platformDirs
                    .filter(Files::isDirectory)
                    .filter(p -> {
                        String name = p.getFileName().toString();
                        return name.equals("java21") || name.equals("any") || name.equals("java17") || name.equals("java11");
                    })
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No platform directory found in " + versionDir));
        }

        System.out.println("Found bala directory in Central: " + platformDir);
        return platformDir;
    }
}
