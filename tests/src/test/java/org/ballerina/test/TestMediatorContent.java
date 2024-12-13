/*
 * Copyright (c) 2024, WSO2 LLC. (http://wso2.com)
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
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TestMediatorContent {

    public static final Path RES_DIR = Paths.get("src/test/resources/ballerina").toAbsolutePath();

    @Test(dataProvider = "data-provider")
    public void test(String project) throws IOException {
        Path projectDir = RES_DIR.resolve(project).toAbsolutePath();
        String[] args = {"-i", projectDir.toString()};
        MiCmd miCmd = new MiCmd();
        new CommandLine(miCmd).parseArgs(args);
        miCmd.execute();
        Path zipPath = Path.of(project + "-connector-0.0.1.zip");
        Assert.assertTrue(Files.exists(zipPath));
        Files.deleteIfExists(zipPath);
    }

    @DataProvider(name = "data-provider")
    public Object[][] dataProvider() {
        return new Object[][]{
                {"project1"}
        };
    }
}
