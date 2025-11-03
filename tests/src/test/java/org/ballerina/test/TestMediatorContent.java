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
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

public class TestMediatorContent {

    public static final Path RES_DIR = Paths.get("src/test/resources/ballerina").toAbsolutePath();
    private static final boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");


    // The test is failing because files are not generated as expected due to compiler plugin issues.
    // TODO: Reenable once we move them to the module repo.
    @Test(dataProvider = "data-provider", enabled = false)
    public void test(String project) throws IOException, InterruptedException {
        Path balExecutable =
                Paths.get(System.getProperty("bal.command"));
        Path projectDir = RES_DIR.resolve(project).toAbsolutePath();
        ProcessBuilder processBuilder = new ProcessBuilder()
                .command(balExecutable.toString(), "mi-module-gen", "-i", projectDir.toString());

        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        OutputStream outputStream = process.getOutputStream();
        InputStream inputStream = process.getInputStream();
        InputStream errorStream = process.getErrorStream();

        printStream(inputStream);
        printStream(errorStream);

        boolean isFinished = process.waitFor(10, TimeUnit.SECONDS);
        outputStream.flush();
        outputStream.close();

        if (!isFinished) {
            process.destroyForcibly();
        }

        Path zipPath = Path.of(project + "-connector-0.0.1.zip");
        Assert.assertTrue(Files.exists(zipPath));
        Files.deleteIfExists(zipPath);
    }

    private static void printStream(InputStream inputStream) throws IOException {
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                System.out.println(line);
            }

        }
    }

    @DataProvider(name = "data-provider")
    public Object[][] dataProvider() {
        return new Object[][]{
                {"project1"}
        };
    }
}
