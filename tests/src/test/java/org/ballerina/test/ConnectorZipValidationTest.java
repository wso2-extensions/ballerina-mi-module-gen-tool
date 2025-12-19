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

package org.ballerina.test;

import io.ballerina.mi.MiCmd;
import io.ballerina.mi.connectorModel.Connector;
import io.ballerina.mi.test.util.ArtifactGenerationUtil;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

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
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Test class for validating the generated connector artifacts for Ballerina projects.
 * <p>
 * This test suite is data-driven and supports validation for multiple projects using the
 * {@link org.testng.annotations.DataProvider} mechanism. Each project to be tested should have
 * its own directory under {@code src/test/resources/ballerina/}.
 * <p>
 * The test suite supports two types of projects:
 * <ul>
 *   <li><b>Build Projects</b> (project1, project2, project3): Regular Ballerina projects with
 *       {@code @mi:Operation} annotated functions. Use {@code miProjectDataProvider()}.</li>
 *   <li><b>Bala Projects</b> (project4): Ballerina connector projects that need to be packed
 *       into .bala files first. Use {@code balaProjectDataProvider()}.</li>
 * </ul>
 * <p>
 * To add a new project to the test suite:
 * <ol>
 *   <li>Add the project's source files and expected output files to the appropriate directories under
 *       {@code src/test/resources/ballerina/} and {@code src/test/resources/expected/}.</li>
 *   <li>For build projects: Update the {@code miProjectDataProvider()} method to include the new project name.</li>
 *   <li>For bala projects: Update the {@code balaProjectDataProvider()} method to include the new project name.</li>
 * </ol>
 */
public class ConnectorZipValidationTest {

    private Path ballerinaHome;
    private static final Path RESOURCES_DIR = Paths.get("src", "test", "resources");
    private static final Path EXPECTED_DIR = RESOURCES_DIR.resolve("expected");
    private static final Path BALLERINA_PROJECTS_DIR = RESOURCES_DIR.resolve("ballerina");

    @BeforeClass
    public void setup() {
        String balCommand = System.getProperty("bal.command");
        if (balCommand != null) {
            ballerinaHome = Paths.get(balCommand).getParent().getParent();
            System.setProperty("ballerina.home", ballerinaHome.toString());
        }
    }

    @DataProvider(name = "miProjectDataProvider")
    public Object[][] miProjectDataProvider() {
        return new Object[][]{
                {"project1"},
                {"project2"},
                {"project3"},
        };
    }

    @DataProvider(name = "balaProjectDataProvider")
    public Object[][] balaProjectDataProvider() {
        return new Object[][]{
                {"project4", null},  // project4 is a local bala project
                {"unionProject", null},
                // Format: {"projectName", "org/package:version"} or {"projectName", null} for local
                // Example: {"ballerinax-milvus", "ballerina/http:2.15.3"} - uncomment when connector is available
                {"ballerinax-milvus", "ballerinax/milvus:1.1.0"},  // project5 is from Central (example)
        };
    }

    @Test(description = "Validate the generated connector artifacts for a project", dataProvider = "miProjectDataProvider")
    public void testGeneratedConnectorArtifactsForProject(String projectName) throws IOException, NoSuchFieldException, IllegalAccessException {
        Connector.reset();
        Path projectPath = BALLERINA_PROJECTS_DIR.resolve(projectName);
        Path expectedPath = EXPECTED_DIR.resolve(projectName);

        // Programmatically execute MiCmd
        MiCmd miCmd = new MiCmd();
        Field sourcePathField = MiCmd.class.getDeclaredField("sourcePath");
        sourcePathField.setAccessible(true);
        sourcePathField.set(miCmd, projectPath.toString());
        miCmd.execute();

        // Validate the generated artifacts
        Path targetPath = projectPath.resolve("target");
        Path connectorPath = targetPath.resolve("generated");

        Assert.assertTrue(Files.exists(connectorPath), "Connector path does not exist for project: " + projectName);
        Assert.assertTrue(Files.isDirectory(connectorPath), "Connector path is not a directory for project: " + projectName);

        // Validate connector.xml
        Path connectorXml = connectorPath.resolve("connector.xml");
        Assert.assertTrue(Files.exists(connectorXml), "connector.xml does not exist for project: " + projectName);
        compareFileContent(connectorXml, expectedPath.resolve("connector.xml"));

        // Validate component directory
        Path componentDir = connectorPath.resolve("functions");
        Assert.assertTrue(Files.exists(componentDir), "component 'functions' directory does not exist for project: " + projectName);
        Assert.assertTrue(Files.isDirectory(componentDir), "component 'functions' path is not a directory for project: " + projectName);

        // Validate component xml
        Path testComponentXml = componentDir.resolve("component.xml");
        Assert.assertTrue(Files.exists(testComponentXml), "component.xml does not exist in 'functions' for project: " + projectName);
        compareFileContent(testComponentXml, expectedPath.resolve("functions").resolve("component.xml"));

        // Validate lib directory and jar
        Path libDir = connectorPath.resolve("lib");
        Assert.assertTrue(Files.exists(libDir), "lib directory does not exist for project: " + projectName);
        Assert.assertTrue(Files.isDirectory(libDir), "lib path is not a directory for project: " + projectName);

        Path projectJar = libDir.resolve(projectName + ".jar");
        Assert.assertTrue(Files.exists(projectJar), "project JAR does not exist in 'lib' for project: " + projectName);

        try (var libFiles = Files.list(libDir)) {
            boolean miNativeJarExists = libFiles
                    .anyMatch(path -> path.getFileName().toString().matches("mi-native-.*\\.jar"));
            Assert.assertTrue(miNativeJarExists, "mi-native JAR does not exist in 'lib' for project: " + projectName);
        }

        Path moduleCoreJar = libDir.resolve("module-core-1.0.2.jar");
        Assert.assertTrue(Files.exists(moduleCoreJar), "module-core JAR does not exist in 'lib' for project: " + projectName);

        // Validate icon directory
        Path iconDir = connectorPath.resolve("icon");
        Assert.assertTrue(Files.exists(iconDir), "icon directory does not exist for project: " + projectName);
        Assert.assertTrue(Files.isDirectory(iconDir), "icon path is not a directory for project: " + projectName);
        
        Path iconLarge = iconDir.resolve("icon-large.png");
        Assert.assertTrue(Files.exists(iconLarge), "icon-large.png does not exist in 'icon' for project: " + projectName);
        
        Path iconSmall = iconDir.resolve("icon-small.png");
        Assert.assertTrue(Files.exists(iconSmall), "icon-small.png does not exist in 'icon' for project: " + projectName);
        
        // Validate uischema directory
        Path uiSchemaDir = connectorPath.resolve("uischema");
        Assert.assertTrue(Files.exists(uiSchemaDir), "uischema directory does not exist for project: " + projectName);
        Assert.assertTrue(Files.isDirectory(uiSchemaDir), "uischema path is not a directory for project: " + projectName);

        Path testUiSchema = uiSchemaDir.resolve("test.json");
        Assert.assertTrue(Files.exists(testUiSchema), "test.json does not exist in 'uischema' for project: " + projectName);
        compareFileContent(testUiSchema, expectedPath.resolve("uischema").resolve("test.json"));
    }

    @Test(description = "Validate the generated connector artifacts for bala-based connectors", dataProvider = "balaProjectDataProvider")
    public void testGeneratedConnectorArtifactsForBalaProject(String projectName, String centralPackage) throws Exception {
        Connector.reset();
        
        Path tempBalaDir;
        Path centralPackagePath = null; // Track Central package path for cleanup
        String connectorFolderName = projectName; // Default to projectName for local projects
        
        if (centralPackage != null && !centralPackage.isBlank()) {
            // Pull package from Ballerina Central
            tempBalaDir = ArtifactGenerationUtil.pullPackageFromCentral(centralPackage);
            Assert.assertTrue(Files.exists(tempBalaDir), "Bala directory not found in Central: " + tempBalaDir);
            
            // Derive connector folder name from Central package (org-package format)
            String[] parts = centralPackage.split(":");
            String orgPackage = parts[0];
            String[] orgPackageParts = orgPackage.split("/");
            if (orgPackageParts.length == 2) {
                // Use org-package as folder name (e.g., ballerinax/milvus -> ballerinax-milvus)
                connectorFolderName = orgPackageParts[0] + "-" + orgPackageParts[1];
                
                // Store the Central package path for cleanup
                String homeDir = System.getProperty("user.home");
                Path centralRepoBase = Paths.get(homeDir, ".ballerina", "repositories", "central.ballerina.io", "bala", 
                        orgPackageParts[0], orgPackageParts[1]);
                if (Files.exists(centralRepoBase)) {
                    centralPackagePath = centralRepoBase;
                }
            }
        } else {
            // Local project - pack and extract
            Path projectPath = BALLERINA_PROJECTS_DIR.resolve(projectName);
            
            // First, pack the project to create a bala file
            Path balaPath = ArtifactGenerationUtil.packBallerinaProject(projectPath);
            Assert.assertTrue(Files.exists(balaPath), "Bala file was not created: " + balaPath);

            // ProjectLoader.load() doesn't support .bala files directly
            // Extract bala to a temporary directory that mimics repository structure
            tempBalaDir = Files.createTempDirectory("bala-test-" + projectName);
            
            // Extract the bala file
            try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(balaPath))) {
                ZipEntry entry = zis.getNextEntry();
                while (entry != null) {
                    Path filePath = tempBalaDir.resolve(entry.getName());
                    if (entry.isDirectory()) {
                        Files.createDirectories(filePath);
                    } else {
                        Files.createDirectories(filePath.getParent());
                        Files.copy(zis, filePath, StandardCopyOption.REPLACE_EXISTING);
                    }
                    zis.closeEntry();
                    entry = zis.getNextEntry();
                }
            }
        }
        
        // Set expected path using connector folder name (pre-existing expected files)
        Path expectedPath = EXPECTED_DIR.resolve(connectorFolderName);
        
        // Create a temporary directory for generated artifacts
        Path tempTargetDir = Files.createTempDirectory("bala-test-output-" + projectName);
        Path tempGeneratedPath = null;
        
        try {
            // Programmatically execute MiCmd with extracted bala directory and temporary target path
            MiCmd miCmd = new MiCmd();
            Field sourcePathField = MiCmd.class.getDeclaredField("sourcePath");
            sourcePathField.setAccessible(true);
            // Use the extracted directory path
            sourcePathField.set(miCmd, tempBalaDir.toAbsolutePath().toString());

            Field targetPathField = MiCmd.class.getDeclaredField("targetPath");
            targetPathField.setAccessible(true);
            targetPathField.set(miCmd, tempTargetDir.toAbsolutePath().toString());

            miCmd.execute();

            // For bala projects, artifacts are generated in the target path's "generated" subdirectory
            tempGeneratedPath = tempTargetDir.resolve("generated");
            Assert.assertTrue(Files.exists(tempGeneratedPath), 
                    "Generated artifacts directory does not exist for project: " + projectName);

        // Validate the generated artifacts against expected files
        Path connectorPath = tempGeneratedPath;

        Assert.assertTrue(Files.exists(connectorPath), "Connector path does not exist for project: " + projectName);
        Assert.assertTrue(Files.isDirectory(connectorPath), "Connector path is not a directory for project: " + projectName);

        // Validate connector.xml
        Path connectorXml = connectorPath.resolve("connector.xml");
        Assert.assertTrue(Files.exists(connectorXml), "connector.xml does not exist for project: " + projectName);
        Path expectedConnectorXml = expectedPath.resolve("connector.xml");
        Assert.assertTrue(Files.exists(expectedConnectorXml), 
                "Expected connector.xml does not exist for project: " + projectName);
        compareFileContent(connectorXml, expectedConnectorXml);

        // Validate component directory
        Path componentDir = connectorPath.resolve("functions");
        Assert.assertTrue(Files.exists(componentDir), "component 'functions' directory does not exist for project: " + projectName);
        Assert.assertTrue(Files.isDirectory(componentDir), "component 'functions' path is not a directory for project: " + projectName);

        // Validate component xml
        Path testComponentXml = componentDir.resolve("component.xml");
        Assert.assertTrue(Files.exists(testComponentXml), "component.xml does not exist in 'functions' for project: " + projectName);
        Path expectedComponentXml = expectedPath.resolve("functions").resolve("component.xml");
        Assert.assertTrue(Files.exists(expectedComponentXml), 
                "Expected component.xml does not exist in 'functions' for project: " + projectName);
        compareFileContent(testComponentXml, expectedComponentXml);

        // Validate lib directory and jar
        Path libDir = connectorPath.resolve("lib");
        Assert.assertTrue(Files.exists(libDir), "lib directory does not exist for project: " + projectName);
        Assert.assertTrue(Files.isDirectory(libDir), "lib path is not a directory for project: " + projectName);

        // For bala projects, the jar name might be different (based on org-name-module-version.jar)
        // Check for any jar file in the lib directory
        long jarCount = Files.list(libDir).filter(p -> p.toString().endsWith(".jar")).count();
        Assert.assertTrue(jarCount > 0, "No JAR files found in 'lib' for project: " + projectName);

        // Validate presence of mi-native JAR without tying to a specific version
        try (var libFiles = Files.list(libDir)) {
            boolean miNativeJarExists = libFiles
                    .anyMatch(path -> path.getFileName().toString().matches("mi-native-.*\\.jar"));
            Assert.assertTrue(miNativeJarExists,
                    "mi-native JAR does not exist in 'lib' for project: " + projectName);
        }

        Path moduleCoreJar = libDir.resolve("module-core-1.0.2.jar");
        Assert.assertTrue(Files.exists(moduleCoreJar), "module-core JAR does not exist in 'lib' for project: " + projectName);

        // Validate icon directory
        Path iconDir = connectorPath.resolve("icon");
        Assert.assertTrue(Files.exists(iconDir), "icon directory does not exist for project: " + projectName);
        Assert.assertTrue(Files.isDirectory(iconDir), "icon path is not a directory for project: " + projectName);
        
        Path iconLarge = iconDir.resolve("icon-large.png");
        Assert.assertTrue(Files.exists(iconLarge), "icon-large.png does not exist in 'icon' for project: " + projectName);
        
        Path iconSmall = iconDir.resolve("icon-small.png");
        Assert.assertTrue(Files.exists(iconSmall), "icon-small.png does not exist in 'icon' for project: " + projectName);

        // Validate uischema directory
        Path uiSchemaDir = connectorPath.resolve("uischema");
        Assert.assertTrue(Files.exists(uiSchemaDir), "uischema directory does not exist for project: " + projectName);
        Assert.assertTrue(Files.isDirectory(uiSchemaDir), "uischema path is not a directory for project: " + projectName);

        // Check if there are any JSON files in uischema (function names may vary)
        long jsonCount = Files.list(uiSchemaDir).filter(p -> p.toString().endsWith(".json")).count();
        Assert.assertTrue(jsonCount > 0, "No JSON schema files found in 'uischema' for project: " + projectName);
        
        // Validate uischema files against expected files
        Path expectedUiSchemaDir = expectedPath.resolve("uischema");
        if (Files.exists(expectedUiSchemaDir) && Files.isDirectory(expectedUiSchemaDir)) {
            // Compare each JSON file in generated uischema against expected
            try (var generatedFiles = Files.list(uiSchemaDir)) {
                generatedFiles.filter(p -> p.toString().endsWith(".json"))
                    .forEach(generatedFile -> {
                        Path expectedFile = expectedUiSchemaDir.resolve(generatedFile.getFileName());
                        if (Files.exists(expectedFile)) {
                            try {
                                compareFileContent(generatedFile, expectedFile);
                            } catch (IOException e) {
                                throw new RuntimeException("Failed to compare uischema file: " + generatedFile.getFileName(), e);
                            }
                        }
                    });
            } catch (IOException e) {
                throw new RuntimeException("Failed to list uischema files", e);
            }
        }
        } finally {
            // Clean up temporary target directory
            if (tempTargetDir != null && Files.exists(tempTargetDir)) {
                try (var walk = Files.walk(tempTargetDir)) {
                    walk.sorted((a, b) -> b.compareTo(a))
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                // Ignore cleanup errors
                            }
                        });
                } catch (IOException e) {
                    // Ignore cleanup errors
                }
            }
            // Clean up temporary directory (for local projects)
            if (tempBalaDir != null && Files.exists(tempBalaDir) && 
                (centralPackage == null || centralPackage.isBlank())) {
                // Only delete tempBalaDir if it's a temporary directory (local projects)
                // For Central packages, tempBalaDir points to the actual Central repo, don't delete it
                try (var walk = Files.walk(tempBalaDir)) {
                    walk.sorted((a, b) -> b.compareTo(a))
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                // Ignore cleanup errors
                            }
                        });
                } catch (IOException e) {
                    // Ignore cleanup errors
                }
            }
            
            // Clean up pulled package from Central repository
            if (centralPackagePath != null && Files.exists(centralPackagePath)) {
                try (var walk = Files.walk(centralPackagePath)) {
                    walk.sorted((a, b) -> b.compareTo(a))
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                // Ignore cleanup errors - package might be in use or protected
                            }
                        });
                } catch (IOException e) {
                    // Ignore cleanup errors
                }
            }
        }
    }

    private void compareFileContent(Path actualFilePath, Path expectedFilePath) throws IOException {
        String actualContent = new String(Files.readAllBytes(actualFilePath)).replaceAll("\\r\\n", "\n");
        String expectedContent = new String(Files.readAllBytes(expectedFilePath)).replaceAll("\\r\\n", "\n");
        Assert.assertEquals(actualContent, expectedContent, "Content mismatch for file: " + actualFilePath.getFileName());
    }
}
