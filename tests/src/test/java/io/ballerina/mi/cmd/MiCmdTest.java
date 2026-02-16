/*
 * Copyright (c) 2026, WSO2 LLC. (https://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.mi.cmd;

import io.ballerina.mi.model.Connector;
import io.ballerina.projects.PackageDescriptor;
import io.ballerina.projects.PackageName;
import io.ballerina.projects.PackageOrg;
import io.ballerina.projects.PackageVersion;
import io.ballerina.projects.SemanticVersion;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import picocli.CommandLine;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MiCmdTest {

    @AfterMethod
    public void resetConnector() {
        Connector.reset();
    }

    @Test
    public void testCliMetadataMethods() {
        MiCmd cmd = new MiCmd();
        Assert.assertEquals(cmd.getName(), "mi-module-gen");

        StringBuilder help = new StringBuilder();
        cmd.printLongDesc(help);
        Assert.assertTrue(help.length() > 0);

        cmd.printUsage(new StringBuilder());
        cmd.setParentCmdParser(new CommandLine(cmd));
    }

    @Test
    public void testExecuteInternalWithMissingArgsShowsHelp() throws Exception {
        MiCmd cmd = new MiCmd();
        Method executeInternal = MiCmd.class.getDeclaredMethod("executeInternal");
        executeInternal.setAccessible(true);
        executeInternal.invoke(cmd);
    }

    @Test
    public void testGenerateMIArtifactsWhenNoComponents() throws Exception {
        MiCmd cmd = new MiCmd();
        createConnector("wso2", "empty", "1.0.0");

        Method generate = MiCmd.class.getDeclaredMethod("generateMIArtifacts", Path.class, Path.class, boolean.class);
        generate.setAccessible(true);

        Path tempDir = Files.createTempDirectory("micmd-no-components");
        boolean result = (boolean) generate.invoke(cmd, tempDir, tempDir, true);
        Assert.assertFalse(result);
    }

    @Test
    public void testGenerateMIArtifactsWhenGenerationAborted() throws Exception {
        MiCmd cmd = new MiCmd();
        Connector connector = createConnector("wso2", "aborted", "1.0.0");
        connector.setGenerationAborted(true, "analysis failed");

        Method generate = MiCmd.class.getDeclaredMethod("generateMIArtifacts", Path.class, Path.class, boolean.class);
        generate.setAccessible(true);

        Path tempDir = Files.createTempDirectory("micmd-aborted");
        boolean result = (boolean) generate.invoke(cmd, tempDir, tempDir, true);
        Assert.assertFalse(result);
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testExecuteWrapsInternalExceptions() throws Exception {
        MiCmd cmd = new MiCmd();
        setField(cmd, "sourcePath", "/definitely/missing/project/path");
        setField(cmd, "targetPath", Files.createTempDirectory("micmd-target").toString());
        cmd.execute();
    }

    private static Connector createConnector(String org, String module, String version) {
        PackageDescriptor descriptor = mock(PackageDescriptor.class);
        PackageOrg packageOrg = mock(PackageOrg.class);
        PackageName packageName = mock(PackageName.class);
        PackageVersion packageVersion = mock(PackageVersion.class);

        when(descriptor.org()).thenReturn(packageOrg);
        when(descriptor.name()).thenReturn(packageName);
        when(descriptor.version()).thenReturn(packageVersion);
        when(packageOrg.value()).thenReturn(org);
        when(packageName.value()).thenReturn(module);
        when(packageVersion.value()).thenReturn(SemanticVersion.from(version));

        return Connector.getConnector(descriptor);
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
