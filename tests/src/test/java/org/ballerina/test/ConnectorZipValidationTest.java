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

import io.ballerina.mi.cmd.MiCmd;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

public class ConnectorZipValidationTest {

    private Path ballerinaHome;
    private static final Path RESOURCES_DIR = Paths.get("src", "test", "resources");
    private static final Path EXPECTED_DIR = RESOURCES_DIR.resolve("expected");

    @BeforeClass
    public void setup() {
        String balCommand = System.getProperty("bal.command");
        if (balCommand != null) {
            ballerinaHome = Paths.get(balCommand).getParent().getParent();
            System.setProperty("ballerina.home", ballerinaHome.toString());
        }
    }

    @Test(description = "Validate the generated connector artifacts")
    public void testGeneratedConnectorArtifacts() throws IOException, NoSuchFieldException, IllegalAccessException {
        String projectPath = "src/test/resources/ballerina/project2";

        // Programmatically execute MiCmd
        MiCmd miCmd = new MiCmd();
        Field sourcePathField = MiCmd.class.getDeclaredField("sourcePath");
        sourcePathField.setAccessible(true);
        sourcePathField.set(miCmd, projectPath);
        miCmd.execute();

        // Validate the generated artifacts
        Path targetPath = Paths.get(projectPath, "target");
        Path connectorPath = targetPath.resolve("project2-mi-connector");

        Assert.assertTrue(Files.exists(connectorPath));
        Assert.assertTrue(Files.isDirectory(connectorPath));

        // Validate connector.xml
        Path connectorXml = connectorPath.resolve("connector.xml");
        Assert.assertTrue(Files.exists(connectorXml));
        compareFileContent(connectorXml, EXPECTED_DIR.resolve("connector.xml"));

        // Validate component directory
        Path componentDir = connectorPath.resolve("test");
        Assert.assertTrue(Files.exists(componentDir));
        Assert.assertTrue(Files.isDirectory(componentDir));

        // Validate component xml
        Path testComponentXml = componentDir.resolve("component.xml");
        Assert.assertTrue(Files.exists(testComponentXml));
        compareFileContent(testComponentXml, EXPECTED_DIR.resolve("test").resolve("component.xml"));

        // Validate component template
        Path testComponentTemplate = componentDir.resolve("test_template.xml");
        Assert.assertTrue(Files.exists(testComponentTemplate));
        compareFileContent(testComponentTemplate, EXPECTED_DIR.resolve("test").resolve("test_template.xml"));

        // Validate lib directory and jar
        Path libDir = connectorPath.resolve("lib");
        Assert.assertTrue(Files.exists(libDir));
        Assert.assertTrue(Files.isDirectory(libDir));

        Path projectJar = libDir.resolve("project2.jar");
        Assert.assertTrue(Files.exists(projectJar));
        
        Path miNativeJar = libDir.resolve("mi-native-0.4.1-SNAPSHOT.jar");
        Assert.assertTrue(Files.exists(miNativeJar));
        
        Path moduleCoreJar = libDir.resolve("module-core-1.0.2.jar");
        Assert.assertTrue(Files.exists(moduleCoreJar));

        // Validate icon directory
        Path iconDir = connectorPath.resolve("icon");
        Assert.assertTrue(Files.exists(iconDir));
        Assert.assertTrue(Files.isDirectory(iconDir));
        
        Path iconLarge = iconDir.resolve("icon-large.png");
        Assert.assertTrue(Files.exists(iconLarge));
        
        Path iconSmall = iconDir.resolve("icon-small.png");
        Assert.assertTrue(Files.exists(iconSmall));
        
        // Validate uischema directory
        Path uiSchemaDir = connectorPath.resolve("uischema");
        Assert.assertTrue(Files.exists(uiSchemaDir));
        Assert.assertTrue(Files.isDirectory(uiSchemaDir));

        Path testUiSchema = uiSchemaDir.resolve("test.json");
        Assert.assertTrue(Files.exists(testUiSchema));
        compareFileContent(testUiSchema, EXPECTED_DIR.resolve("uischema").resolve("test.json"));

        // Validate outputschema directory
        Path outputSchemaDir = connectorPath.resolve("outputschema");
        Assert.assertTrue(Files.exists(outputSchemaDir));
        Assert.assertTrue(Files.isDirectory(outputSchemaDir));

        Path testOutputSchema = outputSchemaDir.resolve("test.json");
        Assert.assertTrue(Files.exists(testOutputSchema));
        compareFileContent(testOutputSchema, EXPECTED_DIR.resolve("outputschema").resolve("test.json"));
    }

    private void compareFileContent(Path actualFilePath, Path expectedFilePath) throws IOException {
        String actualContent = Files.lines(actualFilePath).collect(Collectors.joining(System.lineSeparator()));
        String expectedContent = Files.lines(expectedFilePath).collect(Collectors.joining(System.lineSeparator()));
        Assert.assertEquals(actualContent, expectedContent, "Content mismatch for file: " + actualFilePath.getFileName());
    }
}
