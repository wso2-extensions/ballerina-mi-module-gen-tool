/*
 * Copyright (c) 2025, WSO2 LLC. (https://www.wso2.com).
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

import io.ballerina.cli.BLauncherCmd;
import io.ballerina.projects.*;
import io.ballerina.projects.Module;
import io.ballerina.projects.Package;
import io.ballerina.projects.PackageCompilation;
import io.ballerina.projects.Project;
import io.ballerina.projects.directory.ProjectLoader;
import io.ballerina.tools.diagnostics.Diagnostic;
import picocli.CommandLine;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@CommandLine.Command(name = "mi-module-gen", description = "Generate WSO2 MI module")
public class MiCmd implements BLauncherCmd {
    private static final String CMD_NAME = "mi-module-gen";
    private static final String CONNECTOR_TARGET_PATH = "CONNECTOR_TARGET_PATH";
    private static final String CONNECTOR_NAME_SEPARATOR = "-";
    private static final String MI_IMPORT_FILE = "mi_import_file";
    private static final String MI_IMPORT = "import wso2/mi as _;" + System.lineSeparator();
    private final PrintStream printStream;
    @CommandLine.Option(names = {"--help", "-h"}, usageHelp = true)
    private boolean helpFlag;
    @CommandLine.Option(names = {"--input", "-i"}, description = "Ballerina project path")
    private String sourcePath;
    @CommandLine.Option(names = {"--connector", "-c"}, description = "Generate MI module for Ballerina connector")
    private String targetPath;
    Path executablePath;

    public MiCmd() {
        this.printStream = System.out;
    }

    @Override
    public void execute() {
        String miImportDocumentName;
        if (sourcePath == null || helpFlag) {
            StringBuilder stringBuilder = new StringBuilder();
            setHelpMessage(stringBuilder);
            printStream.println(stringBuilder);
            return;
        }

        Path path = Path.of(sourcePath).normalize();
        BuildOptions buildOptions = BuildOptions.builder().setSticky(false).setOffline(false).build();
        Project project = ProjectLoader.loadProject(path, buildOptions);
        Package pkgBeforeImportAdded = project.currentPackage();

        if (null != targetPath) {
            Path miConnectorCache = Paths.get(targetPath, "BalConnectors");
            executablePath = miConnectorCache.resolve(pkgBeforeImportAdded.descriptor().org().value() +
                    CONNECTOR_NAME_SEPARATOR + pkgBeforeImportAdded.descriptor().name().value() +
                    CONNECTOR_NAME_SEPARATOR + pkgBeforeImportAdded.descriptor().version().toString() + ".jar" );
            if (Files.exists(executablePath)) {
                // Executable for the specific connector version will not be created if it already exists
                //TODO: Check if the ZIP exists as well before quiting
                printStream.println("executable already exists");
                return;
            }
            try {
                Files.createDirectories(miConnectorCache);
            } catch (IOException e) {
                throw  new RuntimeException(e);
            }
            System.setProperty(CONNECTOR_TARGET_PATH, executablePath.toString());
        } else {
            Path bin = path.resolve("target").resolve("bin");
            try {
                createBinFolder(bin);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            executablePath = bin.resolve(pkgBeforeImportAdded.descriptor().name().value() + ".jar");
        }
//        TODO: REMOVE-if DOCUMENT adding is not needed
        // Add new Document with import
        Module defaultModule = project.currentPackage().getDefaultModule();
        List<String> defaultModuleFiles = defaultModule.documentIds()
                .stream()
                .map(documentId -> defaultModule.document(documentId).name())
                .toList();
        do {
            miImportDocumentName = String.format("%s-%s.bal", MI_IMPORT_FILE, UUID.randomUUID());
        } while (defaultModuleFiles.contains(miImportDocumentName));

        ModuleId defaultModuleId = defaultModule.moduleId();
        DocumentId documentId = DocumentId.create(miImportDocumentName, defaultModuleId);
        DocumentConfig documentConfig = DocumentConfig.from(documentId, MI_IMPORT,
                miImportDocumentName);
        defaultModule.modify().addDocument(documentConfig).apply();

        Package pkgAfterImportAdded = project.currentPackage();
        PackageCompilation packageCompilation = pkgAfterImportAdded.getCompilation();
        // TODO: FIX-below only prints a single diagnostic before returning
        for (Diagnostic diagnostic : packageCompilation.diagnosticResult().diagnostics()) {
            printStream.println(diagnostic.toString());
            return;
        }
//        TODO: CHECK-How to determine the JvmTarget, does it have any connection with the connector platform?
        JBallerinaBackend jBallerinaBackend = JBallerinaBackend.from(packageCompilation, JvmTarget.JAVA_21);

        jBallerinaBackend.emit(JBallerinaBackend.OutputType.EXEC, executablePath);
        // TODO: FIX-check emit result diagnostics and conflicting jars and remove if the below is not needed
        if (packageCompilation.diagnosticResult().diagnosticCount() > 0) {
            printStream.println("Errors in compiling Ballerina project");
        }
    }

    private void createBinFolder(Path bin) throws IOException {
        File[] files = bin.toFile().listFiles();
        if (files != null) {
            for (File file : Objects.requireNonNull(files)) {
                file.delete();
            }
        }
        Files.deleteIfExists(bin);
        Files.createDirectory(bin);
    }

    @Override
    public String getName() {
        return CMD_NAME;
    }

    @Override
    public void printLongDesc(StringBuilder stringBuilder) {
        setHelpMessage(stringBuilder);
    }

    @Override
    public void printUsage(StringBuilder stringBuilder) {

    }

    @Override
    public void setParentCmdParser(picocli.CommandLine commandLine) {

    }

    private void setHelpMessage(StringBuilder stringBuilder) {
        Class<?> clazz = MiCmd.class;
        ClassLoader classLoader = clazz.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("cli-docs/mi.help");
        if (inputStream != null) {
            try (InputStreamReader inputStreamREader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                 BufferedReader br = new BufferedReader(inputStreamREader)) {
                String content = br.readLine();
                stringBuilder.append(content);
                while ((content = br.readLine()) != null) {
                    stringBuilder.append('\n').append(content);
                }
            } catch (IOException e) {
                stringBuilder.append(e.getMessage());
            }
        }
    }
}
