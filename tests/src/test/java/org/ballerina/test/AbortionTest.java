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
import org.testng.annotations.Test;

public class AbortionTest {

    @Test(description = "Test warning when skip rate is high but generation continues")
    public void testAbortionOnHighFailureRate() throws Exception {
         ArtifactGenerationUtil.setupBallerinaHome();
         String projectPathStr = "src/test/resources/ballerina/unsupportedProject";
         java.nio.file.Path projectDir = java.nio.file.Paths.get(projectPathStr);
         
         // Pack the project to create a .bala file
         java.nio.file.Path balaPath = ArtifactGenerationUtil.packBallerinaProject(projectDir);
         
         // Unzip the bala to a temporary directory
         java.nio.file.Path extractedBala = projectDir.resolve("target").resolve("extracted-bala");
         if (java.nio.file.Files.exists(extractedBala)) {
             io.ballerina.mi.util.Utils.deleteDirectory(extractedBala);
         }
         java.nio.file.Files.createDirectories(extractedBala);
         unzip(balaPath, extractedBala);

         String projectName = "unsupportedProject";
         String targetPath = projectDir.resolve("target").resolve("mi-artifacts").toString();
         
         // The generation should now succeed with a warning about skipped operations
         // Previously, it would abort. Now it continues with partial artifact generation.
         // The method returns void and throws AssertionError if no zip is found.
         // If this call completes without exception, artifacts were generated successfully.
         ArtifactGenerationUtil.generateExpectedArtifacts(
                 extractedBala.toAbsolutePath().toString(), targetPath, projectName);
         
         // If we reach here, the generation completed successfully despite high skip rate
         System.out.println("Artifact generation completed successfully with partial operations");
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