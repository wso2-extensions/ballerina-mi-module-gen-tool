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

package io.ballerina.mi;

import io.ballerina.cli.BLauncherCmd;
import io.ballerina.mi.analyzer.Analyzer;
import io.ballerina.mi.analyzer.BalConnectorAnalyzer;
import io.ballerina.mi.analyzer.BalModuleAnalyzer;
import io.ballerina.mi.connectorModel.Connector;
import io.ballerina.mi.util.Constants;
import io.ballerina.projects.BuildOptions;
import io.ballerina.projects.EmitResult;
import io.ballerina.projects.JBallerinaBackend;
import io.ballerina.projects.JvmTarget;
import io.ballerina.projects.Package;
import io.ballerina.projects.PackageCompilation;
import io.ballerina.projects.Project;
import io.ballerina.projects.ProjectLoadResult;
import io.ballerina.projects.directory.BalaProject;
import io.ballerina.projects.directory.BuildProject;
import io.ballerina.projects.directory.ProjectLoader;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;
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
import java.util.Objects;

@CommandLine.Command(name = "mi-module-gen", description = "Generate WSO2 MI module")
public class MiCmd implements BLauncherCmd {
    private static final String CMD_NAME = "mi-module-gen";
    private static final String CONNECTOR_NAME_SEPARATOR = "-";
    private final PrintStream printStream;
    @CommandLine.Option(names = {"--help", "-h"}, usageHelp = true)
    private boolean helpFlag;
    @CommandLine.Option(names = {"--input", "-i"}, description = "Ballerina project path")
    private String sourcePath;
    @CommandLine.Option(names = {"--target", "-t"}, description = "Target path for the generated MI connector")
    private String targetPath;
    private Path executablePath;
    private Path miArtifactsPath;
    private Analyzer balAnalyzer;

    public MiCmd() {
        this.printStream = System.out;
    }

    @Override
    public void execute() {
        try {
            executeInternal();
        } catch (Exception e) {
            e.printStackTrace(System.err);
            throw new RuntimeException(e);
        }
    }

    private void executeInternal() {
        String miImportDocumentName;
        Package compilePkg;
        if (sourcePath == null || helpFlag) {
            StringBuilder stringBuilder = new StringBuilder();
            setHelpMessage(stringBuilder);
            printStream.println(stringBuilder);
            return;
        }

        Path path = Path.of(sourcePath).normalize();
        BuildOptions buildOptions = BuildOptions.builder().setOffline(false).build();
        ProjectLoadResult projectLoadResult = ProjectLoader.load(path.toAbsolutePath(), buildOptions);
        Project project = projectLoadResult.project();
        compilePkg = project.currentPackage();
        if (!(project instanceof BuildProject || project instanceof BalaProject)) {
            printStream.println("ERROR: Invalid project path provided");
            return;
        }

        if (project instanceof BalaProject) {
            // Project is a Bala project
            if (targetPath == null || targetPath.isEmpty()) {
                printStream.println("ERROR: Target path for the MI connector not provided");
                return;
            }
            balAnalyzer = new BalConnectorAnalyzer();
            miArtifactsPath = Paths.get(targetPath);
            Path miConnectorCache = miArtifactsPath.resolve("BalConnectors");
            executablePath = miConnectorCache.resolve(compilePkg.descriptor().org().value() +
                    CONNECTOR_NAME_SEPARATOR + compilePkg.descriptor().name().value() +
                    CONNECTOR_NAME_SEPARATOR + compilePkg.descriptor().version().toString() + ".jar" );

            try {
                Files.createDirectories(miConnectorCache);
            } catch (IOException e) {
                throw  new RuntimeException(e);
            }
            System.setProperty(Constants.CONNECTOR_TARGET_PATH, executablePath.toString());
        } else {
            balAnalyzer = new BalModuleAnalyzer();
        }

        PackageCompilation packageCompilation = compilePkg.getCompilation();
        for (Diagnostic diagnostic : packageCompilation.diagnosticResult().diagnostics()) {
            if (!(project instanceof BalaProject) || diagnostic.diagnosticInfo().severity() == DiagnosticSeverity.ERROR) {
                printStream.println(diagnostic.toString());
            }
        }
        if (packageCompilation.diagnosticResult().hasErrors()) {
            printStream.println("ERROR: Ballerina project compilation contains errors");
            return;
        }

        balAnalyzer.analyze(compilePkg);

        JBallerinaBackend jBallerinaBackend = JBallerinaBackend.from(packageCompilation, JvmTarget.JAVA_21);

        if (project instanceof BuildProject) {
            // Project is a build project
            if (null != targetPath) {
                printStream.println("WARNING: Arguments provided for -t will be ignored.\n");
            }
            miArtifactsPath = path.resolve("target");
            Path bin = miArtifactsPath.resolve("bin");
            try {
                createBinFolder(bin);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            executablePath = bin.resolve(compilePkg.descriptor().name().value() + ".jar");
        }

        EmitResult emitResult = jBallerinaBackend.emit(JBallerinaBackend.OutputType.EXEC, executablePath);
        if (!jBallerinaBackend.conflictedJars().isEmpty()) {
            printStream.println("WARNING: Detected conflicting jar files:");
            for (JBallerinaBackend.JarConflict conflict : jBallerinaBackend.conflictedJars()) {
                printStream.println(conflict.getWarning(project.buildOptions().listConflictedClasses()));
            }
        }

        // Print diagnostics found during executable emit
        if (!emitResult.diagnostics().diagnostics().isEmpty()) {
            emitResult.diagnostics().diagnostics().forEach(d -> printStream.println("\n" + d.toString()));
        }

        // Generate MI connector artifacts (XML/JSON files and zip package)
        // Both BuildProject and BalaProject need MI artifacts
        generateMIArtifacts(executablePath, compilePkg, miArtifactsPath, project instanceof BuildProject);
        printStream.println("MI " + (project instanceof BuildProject ? "module" : "connector") +
                " generation completed successfully.");
    }

    private void generateMIArtifacts(Path sourcePath, Package compilePkg, Path targetPath, boolean isBuildProject) {
        printStream.println("Generating MI " + (isBuildProject ? "module" : "connector") + " artifacts...");

        Connector connector = Connector.getConnector();

        printStream.println("Found " + connector.getComponents().size() + " component(s)");

        if (connector.getComponents().isEmpty()) {
            printStream.println("WARN: No components found. MI " + (isBuildProject ? "module" : "connector") + " artifacts will not be generated.");
            printStream.println("HINT: Ensure functions are annotated with @mi:Operation");
            return;
        }

        ConnectorSerializer connectorSerializer = new ConnectorSerializer(sourcePath, targetPath);
        connectorSerializer.serialize(connector);
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
