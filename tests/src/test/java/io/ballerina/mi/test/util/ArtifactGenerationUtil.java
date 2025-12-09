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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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

        // 2. Programmatically execute MiCmd
        System.out.println("Executing MiCmd for project: " + projectPathStr);
        MiCmd miCmd = new MiCmd();
        Field sourcePathField = MiCmd.class.getDeclaredField("sourcePath");
        sourcePathField.setAccessible(true);
        sourcePathField.set(miCmd, projectPathStr);
        if (targetPathStr != null && !targetPathStr.isBlank()) {
            Field targetPathField = MiCmd.class.getDeclaredField("targetPath");
            targetPathField.setAccessible(true);
            targetPathField.set(miCmd, targetPathStr);
        }
        miCmd.execute();
        System.out.println("MiCmd execution completed.");

        // 3. Find the generated zip file and unzip it to the expected directory
        Optional<Path> generatedZipFile;
        try (Stream<Path> files = Files.walk(targetDir, 2)) {
            generatedZipFile = files.filter(p -> p.toString().endsWith(".zip")).findFirst();
        }

        Assert.assertTrue(generatedZipFile.isPresent(), "Generated zip file not found in " + targetDir);

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
}
