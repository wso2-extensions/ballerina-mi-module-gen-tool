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
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

/**
 * Test class for validating the generated connector artifacts for Ballerina projects.
 * <p>
 * This test suite is data-driven and supports validation for multiple projects using the
 * {@link org.testng.annotations.DataProvider} mechanism. Each project to be tested should have
 * its own directory under {@code src/test/resources/ballerina/}.
 * <p>
 * To add a new project to the test suite:
 * <ol>
 *   <li>Add the project's source files and expected output files to the appropriate directories under
 *       {@code src/test/resources/ballerina/} and {@code src/test/resources/expected/}.</li>
 *   <li>Update the {@code miProjectDataProvider()} method to include the new project name.</li>
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
        
        Path miNativeJar = libDir.resolve("mi-native-0.4.1-SNAPSHOT.jar");
        Assert.assertTrue(Files.exists(miNativeJar), "mi-native JAR does not exist in 'lib' for project: " + projectName);
        
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

    private void compareFileContent(Path actualFilePath, Path expectedFilePath) throws IOException {
        String actualContent = new String(Files.readAllBytes(actualFilePath)).replaceAll("\\r\\n", "\n");
        System.out.println("Actual Content:\n" + actualContent);
        String expectedContent = new String(Files.readAllBytes(expectedFilePath)).replaceAll("\\r\\n", "\n");
        System.out.println("Expected Content:\n" + expectedContent);
        Assert.assertEquals(actualContent, expectedContent, "Content mismatch for file: " + actualFilePath.getFileName());
    }
}
