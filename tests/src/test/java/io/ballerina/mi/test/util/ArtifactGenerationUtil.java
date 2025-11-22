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

import io.ballerina.mi.cmd.MiCmd;
import org.testng.Assert;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.StandardCopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Stream;

public class ArtifactGenerationUtil {

    private static final Path RESOURCES_DIR = Paths.get("src", "test", "resources");
    private static final Path EXPECTED_DIR = RESOURCES_DIR.resolve("expected");

    public static void generateExpectedArtifacts(String projectPathStr, String projectName) throws Exception {
        System.out.println("Starting generateExpectedArtifacts for project: " + projectName);
        System.out.println("ProjectPathStr: " + projectPathStr);
        Path projectPath = Paths.get(projectPathStr);
        Path expectedOutputPath = EXPECTED_DIR.resolve(projectName);
        Path generatedConnectorPath = projectPath.resolve("target").resolve(projectName + "-mi-connector");

        System.out.println("ExpectedOutputPath: " + expectedOutputPath);
        System.out.println("GeneratedConnectorPath: " + generatedConnectorPath);

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

        // 2. Programmatically execute MiCmd
        System.out.println("Executing MiCmd for project: " + projectPathStr);
        MiCmd miCmd = new MiCmd();
        Field sourcePathField = MiCmd.class.getDeclaredField("sourcePath");
        sourcePathField.setAccessible(true);
        sourcePathField.set(miCmd, projectPathStr);
        miCmd.execute();
        System.out.println("MiCmd execution completed.");

        // 3. Copy generated artifacts to expected directory
        Assert.assertTrue(Files.exists(generatedConnectorPath), "Generated connector path does not exist: " + generatedConnectorPath);
        System.out.println("Generated connector path exists. Copying artifacts from " + generatedConnectorPath + " to " + expectedOutputPath);
        
        try (Stream<Path> walk = Files.walk(generatedConnectorPath)) {
            walk.forEach(source -> {
                Path destination = expectedOutputPath.resolve(generatedConnectorPath.relativize(source));
                try {
                    Files.createDirectories(destination.getParent()); // Ensure parent directories exist
                    System.out.println("Copying: " + source + " to " + destination);
                    Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to copy " + source + " to " + destination, e);
                }
            });
        }
        System.out.println("Artifacts copied successfully to: " + expectedOutputPath);
    }

    public static void setupBallerinaHome() {
        String balCommand = System.getProperty("bal.command");
        if (balCommand != null) {
            Path ballerinaHome = Paths.get(balCommand).getParent().getParent();
            System.setProperty("ballerina.home", ballerinaHome.toString());
        }
    }
}
