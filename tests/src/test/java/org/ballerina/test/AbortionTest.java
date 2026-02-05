/*
 * Copyright (c) 2026, WSO2 LLC. (http://wso2.com).
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

import io.ballerina.mi.test.util.ArtifactGenerationUtil;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;

public class AbortionTest {

    @Test(description = "Test abortion when skip rate is high")
    public void testAbortionOnHighFailureRate() throws Exception {
         ArtifactGenerationUtil.setupBallerinaHome();
         String projectPathStr = "src/test/resources/ballerina/unsupportedProject";
         java.nio.file.Path projectDir = java.nio.file.Paths.get(projectPathStr);

         // Pack the project to create a .bala file
         java.nio.file.Path balaPath = ArtifactGenerationUtil.packBallerinaProject(projectDir);

         // Use temporary directories for extracted bala contents and MI artifacts output
         java.nio.file.Path extractedBala = null;
         java.nio.file.Path targetDir = null;
         try {
             extractedBala = java.nio.file.Files.createTempDirectory("unsupported-extracted-bala-");
             targetDir = java.nio.file.Files.createTempDirectory("unsupported-mi-artifacts-");

             // Unzip the bala to the temporary directory
             unzip(balaPath, extractedBala);

             String projectName = "unsupportedProject";
             String targetPath = targetDir.toString();

             // The generation should verify that NO artifacts are generated.
             // ArtifactGenerationUtil throws AssertionError if no zip is found.
             try {
                 ArtifactGenerationUtil.generateExpectedArtifacts(
                         extractedBala.toAbsolutePath().toString(), targetPath, projectName);
                 Assert.fail("Artifact generation should have produced no artifacts, but it seems to have " +
                         "succeeded/found artifacts.");
             } catch (AssertionError e) {
                 Assert.assertTrue(e.getMessage().contains("Generated zip file not found"),
                         "Expected 'Generated zip file not found' error, but got: " + e.getMessage());
             }
         } finally {
             // Clean up temporary directories to avoid leaving stray files on disk
             if (extractedBala != null && java.nio.file.Files.exists(extractedBala)) {
                 io.ballerina.mi.util.Utils.deleteDirectory(extractedBala);
             }
             if (targetDir != null && java.nio.file.Files.exists(targetDir)) {
                 io.ballerina.mi.util.Utils.deleteDirectory(targetDir);
             }
         }
    }

    private void unzip(java.nio.file.Path sourceZip, java.nio.file.Path targetDir) throws java.io.IOException {
        try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(java.nio.file.Files.newInputStream(sourceZip))) {
            java.util.zip.ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                java.nio.file.Path newPath = targetDir.resolve(zipEntry.getName());
                if (zipEntry.isDirectory()) {
                    java.nio.file.Files.createDirectories(newPath);
                } else {
                    if (newPath.getParent() != null) {
                        java.nio.file.Files.createDirectories(newPath.getParent());
                    }
                    java.nio.file.Files.copy(zis, newPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
                zipEntry = zis.getNextEntry();
            }
        }
    }
}
