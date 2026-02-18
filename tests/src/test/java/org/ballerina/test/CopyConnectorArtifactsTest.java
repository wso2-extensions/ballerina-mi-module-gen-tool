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

import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Test class to copy generated Ballerina connector artifacts to MI project.
 * Uses TestNG data provider pattern to easily add more projects in the future.
 */
public class CopyConnectorArtifactsTest {

    private static final String PROJECT_ROOT = System.getProperty("project.root", 
        Paths.get(System.getProperty("user.dir"), "..").toAbsolutePath().normalize().toString());
    private static final Path PROJECT_ROOT_PATH = Paths.get(PROJECT_ROOT);
    
    private static final String BALLERINA_PROJECTS_DIR = "tests/src/test/resources/ballerina";
    private static final String MI_CONNECTORS_DIR = "tests/src/test/resources/mi/project1/src/main/wso2mi/resources/connectors";

    /**
     * Data provider for projects that need their artifacts copied to MI project.
     * To add more projects, simply add a new Object[] array with:
     * [0] - project name (e.g., "project3")
     * [1] - artifact zip file name pattern (e.g., "ballerina-connector-project3-1.0.0.zip")
     * 
     * @return Array of project data
     */
    @DataProvider(name = "connectorProjects")
    public Object[][] connectorProjectsDataProvider() {
        return new Object[][] {
            // Format: {projectName, artifactFileName}
            {"project3", "ballerina-connector-project3-1.0.0.zip"},
            // Add more projects here in the future:
            // {"project1", "ballerina-connector-project1-0.0.1.zip"},
            // {"project2", "ballerina-connector-project2-1.0.0.zip"},
        };
    }

    /**
     * Test method that copies connector artifacts from Ballerina projects to MI project.
     * This test runs for each project defined in the data provider.
     * 
     * @param projectName The name of the Ballerina project
     * @param artifactFileName The name of the artifact zip file to copy
     * @throws IOException If file operations fail
     */
    @Test(dataProvider = "connectorProjects")
    public void testCopyConnectorArtifact(String projectName, String artifactFileName) throws IOException {
        // Source: Ballerina project target directory
        Path sourceArtifact = PROJECT_ROOT_PATH
            .resolve(BALLERINA_PROJECTS_DIR)
            .resolve(projectName)
            .resolve("target")
            .resolve(artifactFileName);
        
        // Also check in the project root (some projects might have artifacts there)
        if (!Files.exists(sourceArtifact)) {
            Path alternativeSource = PROJECT_ROOT_PATH
                .resolve(BALLERINA_PROJECTS_DIR)
                .resolve(projectName)
                .resolve(artifactFileName);
            
            if (Files.exists(alternativeSource)) {
                sourceArtifact = alternativeSource;
            }
        }
        
        // Verify source artifact exists - skip test if artifact not found (may not be built yet)
        if (!Files.exists(sourceArtifact)) {
            throw new SkipException("Source artifact not found: " + sourceArtifact + 
                ". The Ballerina project may not have been built yet. This is expected in some CI environments.");
        }
        
        // Destination: MI project connectors directory
        Path destDir = PROJECT_ROOT_PATH.resolve(MI_CONNECTORS_DIR);
        Path destArtifact = destDir.resolve(artifactFileName);
        
        // Create destination directory if it doesn't exist
        Files.createDirectories(destDir);
        
        // Copy the artifact
        Files.copy(sourceArtifact, destArtifact, StandardCopyOption.REPLACE_EXISTING);
        
        // Verify the copy was successful
        Assert.assertTrue(Files.exists(destArtifact), 
            "Failed to copy artifact to: " + destArtifact);
        Assert.assertEquals(Files.size(sourceArtifact), Files.size(destArtifact),
            "File sizes don't match after copy");
        
        System.out.println("Successfully copied artifact:");
        System.out.println("  From: " + sourceArtifact);
        System.out.println("  To:   " + destArtifact);
        System.out.println("  Size: " + Files.size(destArtifact) + " bytes");
    }

    @AfterClass
    public void cleanUpCopiedArtifacts() throws IOException {
        Path destDir = PROJECT_ROOT_PATH.resolve(MI_CONNECTORS_DIR);
        if (!Files.exists(destDir)) {
            return;
        }

        for (Object[] data : connectorProjectsDataProvider()) {
            String artifactFileName = (String) data[1];
            Path destArtifact = destDir.resolve(artifactFileName);
            Files.deleteIfExists(destArtifact);
        }
    }

    /**
     * Helper method to get all projects that should have artifacts copied.
     * This can be used by other classes if needed.
     * 
     * @return List of project names
     */
    public static List<String> getConnectorProjects() {
        CopyConnectorArtifactsTest test = new CopyConnectorArtifactsTest();
        Object[][] data = test.connectorProjectsDataProvider();
        List<String> projects = new ArrayList<>();
        for (Object[] row : data) {
            projects.add((String) row[0]);
        }
        return projects;
    }
}

