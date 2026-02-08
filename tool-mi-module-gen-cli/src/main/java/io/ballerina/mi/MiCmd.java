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
import io.ballerina.mi.util.Utils;
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
import java.util.Objects;
import java.util.logging.LogManager;

@CommandLine.Command(name = "mi-module-gen", description = "Generate WSO2 MI module")
public class MiCmd implements BLauncherCmd {
    private static final String CMD_NAME = "mi-module-gen";
    private static final String CONNECTOR_NAME_SEPARATOR = "-";
    private final PrintStream printStream;

    static {
        // Suppress Axiom StAX dialect detection warning - must be done before any Axiom classes are loaded
        try {
            InputStream configStream = MiCmd.class.getResourceAsStream("/logging.properties");
            if (configStream != null) {
                LogManager.getLogManager().readConfiguration(configStream);
                configStream.close();
            }
        } catch (IOException ignored) {
        }
    }

    @CommandLine.Option(names = {"--help", "-h"}, usageHelp = true)
    private boolean helpFlag;
    @CommandLine.Option(names = {"--input", "-i"}, description = "Ballerina project path")
    private String sourcePath;
    @CommandLine.Option(names = {"--target", "-t"}, description = "Target path for the generated MI connector")
    private String targetPath;
    private Path executablePath;

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
        if (sourcePath == null || targetPath == null || helpFlag) {
            StringBuilder stringBuilder = new StringBuilder();
            setHelpMessage(stringBuilder);
            printStream.println(stringBuilder);
            return;
        }

        Path miArtifactsPath = Path.of(targetPath);

        // Compile, analyze, and emit in a separate method so that ALL compiler objects
        // (Project, Package, PackageCompilation, JBallerinaBackend, SemanticModel, BIR, etc.)
        // go fully out of scope when the method returns. This allows the GC to reclaim the
        // multi-GB compiler state before serialization begins.
        Boolean isBuildProject = compileAnalyzeAndEmit(miArtifactsPath);
        if (isBuildProject == null) {
            return; // compilation failed or was aborted
        }

        // Force GC to reclaim compiler memory before serialization
        System.gc();

        // Generate MI connector artifacts (XML/JSON files and zip package)
        // Both BuildProject and BalaProject need MI artifacts
        boolean artifactsGenerated = generateMIArtifacts(executablePath, miArtifactsPath, isBuildProject);
        if (!artifactsGenerated) {
            return;
        }

        // Free serialization memory before validation
        System.gc();
        printStream.println("Validating connector artifacts...");

        boolean isValid = ConnectorValidator.validateConnector(miArtifactsPath);
        if (!isValid) {
            printStream.println("ERROR: MI " + (isBuildProject ? "module" : "connector") +
                    " generation failed due to validation errors.");
            try {
                Utils.deleteDirectory(miArtifactsPath);
            } catch (IOException e) {
                printStream.println("ERROR: Failed to delete invalid MI " + (isBuildProject ?
                        "module" : "connector") + " artifacts at: " + miArtifactsPath);
            }
            return;
        }
        printStream.println("MI " + (isBuildProject ? "module" : "connector") +
                " generation completed successfully.");
    }

    /**
     * Compiles the Ballerina project, analyzes it to build the Connector model, and emits the
     * executable JAR. This method is intentionally separated from executeInternal() so that all
     * compiler objects (Project, Package, PackageCompilation, JBallerinaBackend, SemanticModel,
     * BIR, bytecode) go fully out of scope when it returns. For large connectors like Jira
     * (584 operations), the compiler state can consume multiple GB.
     *
     * @param miArtifactsPath Target path for generated artifacts
     * @return true if the project is a BuildProject, false if BalaProject, null if an error occurred
     */
    private Boolean compileAnalyzeAndEmit(Path miArtifactsPath) {
        Path projectPath = Path.of(sourcePath).normalize();
        BuildOptions buildOptions = BuildOptions.builder().setOffline(false).build();
        ProjectLoadResult projectLoadResult = ProjectLoader.load(projectPath.toAbsolutePath(), buildOptions);
        Project project = projectLoadResult.project();
        Package compilePkg = project.currentPackage();
        boolean isBuildProject = project instanceof BuildProject;

        if (!(project instanceof BuildProject || project instanceof BalaProject)) {
            printStream.println("ERROR: Invalid project path provided");
            return null;
        }

        Analyzer balAnalyzer;
        if (project instanceof BalaProject) {
            // Project is a Bala project
            balAnalyzer = new BalConnectorAnalyzer();
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
            return null;
        }

        balAnalyzer.analyze(compilePkg);

        JBallerinaBackend jBallerinaBackend = JBallerinaBackend.from(packageCompilation, JvmTarget.JAVA_21);

        if (isBuildProject) {
            // Project is a build project
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

        // Pre-compute TypeSymbol-derived values and release TypeSymbol references
        // so the semantic model graph is not reachable from the Connector model
        Connector.getConnector().clearTypeSymbols();

        return isBuildProject;
    }

    private boolean generateMIArtifacts(Path sourcePath, Path targetPath, boolean isBuildProject) {
        printStream.println("Generating MI " + (isBuildProject ? "module" : "connector") + " artifacts...");

        Connector connector = Connector.getConnector();

        printStream.println("Found " + connector.getComponents().size() + " component(s)");

        if (connector.getComponents().isEmpty()) {
            if (connector.isGenerationAborted()) {
                printStream.println("WARN: Skipping MI " + (isBuildProject ? "module" : "connector")
                        + " artifacts generation. Reason: " + connector.getAbortionReason());
            } else {
                printStream.println("WARN: No components found. MI " + (isBuildProject ? "module" : "connector") + " artifacts will not be generated.");
            }
            return false;
        }

        ConnectorSerializer connectorSerializer = new ConnectorSerializer(sourcePath, targetPath);
        connectorSerializer.serialize(connector);
        return true;
    }

    private void createBinFolder(Path bin) throws IOException {
        File[] files = bin.toFile().listFiles();
        if (files != null) {
            for (File file : Objects.requireNonNull(files)) {
                file.delete();
            }
        }
        Files.deleteIfExists(bin);
        Files.createDirectories(bin);
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
