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

    @Test(description = "Generates expected artifacts for project4", enabled = true) // Set enabled to true to run manually
    public void generateProject4ExpectedArtifacts() throws Exception {
        String projectPath = "/home/virul/.ballerina/repositories/local/bala/testOrg/project3/1.0.0";
        String projectName = "project4";
        ArtifactGenerationUtil.generateExpectedArtifacts(projectPath, projectName);
        System.out.println("Expected artifacts for project4 generated successfully.");
    }
}
