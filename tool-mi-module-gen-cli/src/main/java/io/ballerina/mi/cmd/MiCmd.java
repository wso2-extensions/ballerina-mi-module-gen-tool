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
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.AnnotationSymbol;
import io.ballerina.compiler.api.symbols.FunctionSymbol;
import io.ballerina.compiler.api.symbols.ParameterSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.mi.util.Utils;
import io.ballerina.mi.model.Component;
import io.ballerina.mi.model.Connector;
import io.ballerina.mi.model.FunctionParam;
import io.ballerina.mi.model.Param;
import io.ballerina.projects.BuildOptions;
import io.ballerina.projects.DocumentConfig;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.EmitResult;
import io.ballerina.projects.JBallerinaBackend;
import io.ballerina.projects.JvmTarget;
import io.ballerina.projects.Module;
import io.ballerina.projects.ModuleId;
import io.ballerina.projects.Package;
import io.ballerina.projects.PackageCompilation;
import io.ballerina.projects.Project;
import io.ballerina.projects.bala.BalaProject;
import io.ballerina.projects.directory.BuildProject;
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
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
    @CommandLine.Option(names = {"--target", "-t"}, description = "Target path for the generated MI connector")
    private String targetPath;
    private Path executablePath;
    private Path miArtifactsPath;

    public MiCmd() {
        this.printStream = System.out;
        System.err.println("MiCmd constructor called");
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
        BuildOptions buildOptions = BuildOptions.builder().setSticky(false).setOffline(false).build();
        Project project = ProjectLoader.loadProject(path, buildOptions);
        compilePkg = project.currentPackage();
        if (!(project instanceof BuildProject || project instanceof BalaProject)){
            printStream.println("ERROR: Invalid project path provided");
            return;
        }

        if (project instanceof BalaProject) {
            // Project is a Bala project
            if (targetPath == null || targetPath.isEmpty()) {
                printStream.println("ERROR: Target path for the MI connector not provided");
                return;
            }
            miArtifactsPath = Paths.get(targetPath);
            Path miConnectorCache = miArtifactsPath.resolve("BalConnectors");
            executablePath = miConnectorCache.resolve(compilePkg.descriptor().org().value() +
                    CONNECTOR_NAME_SEPARATOR + compilePkg.descriptor().name().value() +
                    CONNECTOR_NAME_SEPARATOR + compilePkg.descriptor().version().toString() + ".jar" );
            // Add a new Document to the project with the wso2/mi import
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
            compilePkg = project.currentPackage();

            try {
                Files.createDirectories(miConnectorCache);
            } catch (IOException e) {
                throw  new RuntimeException(e);
            }
            System.setProperty(CONNECTOR_TARGET_PATH, executablePath.toString());
        }

        PackageCompilation packageCompilation = compilePkg.getCompilation();
        for (Diagnostic diagnostic : packageCompilation.diagnosticResult().diagnostics()) {
            printStream.println(diagnostic.toString());
        }
        if (packageCompilation.diagnosticResult().hasErrors()) {
            printStream.println("ERROR: Ballerina project compilation contains errors");
            return;
        }
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
    }

    private void generateMIArtifacts(Path sourcePath, Package compilePkg, Path targetPath, boolean isBuildProject) {
        printStream.println("Generating MI " + (isBuildProject ? "module" : "connector") + " artifacts...");

        Connector connector = Connector.getConnector();
        connector.setName(compilePkg.descriptor().name().value());
        connector.setVersion(compilePkg.descriptor().version().value().toString());
        connector.setOrgName(compilePkg.descriptor().org().value());
        connector.setModuleName(compilePkg.descriptor().name().value());
        connector.setModuleVersion(compilePkg.descriptor().version().value().toString());

        // Analyze the package to populate connector components
        analyzePackageAndPopulateConnector(compilePkg, connector);

        printStream.println("Found " + connector.getComponents().size() + " component(s)");

        if (connector.getComponents().isEmpty()) {
            printStream.println("WARN: No components found. MI " + (isBuildProject ? "module" : "connector") + " artifacts will not be generated.");
            printStream.println("HINT: Ensure functions are annotated with @mi:Operation");
            return;
        }

        try {
            // Create a subdirectory for MI connector files within the target directory
            Path destinationPath = targetPath.resolve(connector.getName() + "-mi-connector");
            Files.createDirectories(destinationPath);

            generateXmlFiles(destinationPath, connector);
            generateJsonFiles(destinationPath, connector);

            // Get the JAR path from tool resources
            java.net.URI jarPath = getClass().getProtectionDomain().getCodeSource().getLocation().toURI();
            Utils.copyResources(getClass().getClassLoader(), destinationPath, jarPath,
                    connector.getOrgName(), connector.getModuleName(), connector.getModuleVersion());

            // Create lib directory and copy the generated executable JAR
            Path libPath = destinationPath.resolve(Connector.LIB_PATH);
            Files.createDirectories(libPath);
            Files.copy(sourcePath, libPath.resolve(sourcePath.getFileName()), StandardCopyOption.REPLACE_EXISTING);

            // Create the zip file in the target directory
            Path zipPath = targetPath.resolve(connector.getZipFileName());
            Utils.zipFolder(destinationPath, zipPath.toString());

            printStream.println("Generated MI " + (isBuildProject ? "module" : "connector") + ": " + zipPath);
        } catch (IOException | java.net.URISyntaxException e) {
            printStream.println("ERROR: Failed to generate MI " + (isBuildProject ? "module" : "connector") + " artifacts: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private void generateXmlFiles(Path connectorFolderPath, Connector connector) {
        java.io.File connectorFolder = new java.io.File(connectorFolderPath.toUri());
        if (!connectorFolder.exists()) {
            boolean created = connectorFolder.mkdir();
            if (!created && !connectorFolder.exists()) {
                printStream.println("ERROR: Failed to create directory: " + connectorFolder.getAbsolutePath());
                throw new RuntimeException("Failed to create directory: " + connectorFolder.getAbsolutePath());
            }
        }

        connector.generateInstanceXml(connectorFolder);

        for (Component component : connector.getComponents()) {
            component.generateInstanceXml(connectorFolder);
            component.generateTemplateXml(connectorFolder);
        }
    }

    private void generateJsonFiles(Path connectorFolderPath, Connector connector) {
        java.io.File connectorFolder = new java.io.File(connectorFolderPath.toUri());
        for (Component component : connector.getComponents()) {
            component.generateUIJson(connectorFolder);
            component.generateOutputSchemaJson(connectorFolder);
        }
    }

    private void analyzePackageAndPopulateConnector(Package pkg, Connector connector) {
        Module defaultModule = pkg.getDefaultModule();
        PackageCompilation compilation = pkg.getCompilation();
        SemanticModel semanticModel = compilation.getSemanticModel(defaultModule.moduleId());

        // Get all symbols from the module and filter for functions
        Collection<Symbol> allSymbols = semanticModel.moduleSymbols();
        for (Symbol symbol : allSymbols) {
            if (symbol.kind() == SymbolKind.FUNCTION && symbol instanceof FunctionSymbol functionSymbol) {
                analyzeFunctionForMIOperation(functionSymbol, connector);
            }
        }
    }

    private void analyzeFunctionForMIOperation(FunctionSymbol functionSymbol, Connector connector) {
        // Check if function has @mi:Operation annotation
        List<AnnotationSymbol> annotations = functionSymbol.annotations();

        boolean hasOperationAnnotation = false;
        for (AnnotationSymbol annotationSymbol : annotations) {
            Optional<String> annotationName = annotationSymbol.getName();
            if (annotationName.isPresent() && annotationName.get().equals("Operation")) {
                hasOperationAnnotation = true;
                break;
            }
        }

        if (!hasOperationAnnotation) {
            return;
        }

        Optional<String> functionName = functionSymbol.getName();
        if (functionName.isEmpty()) {
            return;
        }

        printStream.println("Found MI operation: " + functionName.get());

        // Create component
        Component component = new Component(functionName.get());

        // Extract parameters
        int noOfParams = 0;
        Optional<List<ParameterSymbol>> params = functionSymbol.typeDescriptor().params();
        if (params.isPresent()) {
            List<ParameterSymbol> parameterSymbols = params.get();
            noOfParams = parameterSymbols.size();

            for (int i = 0; i < noOfParams; i++) {
                ParameterSymbol parameterSymbol = parameterSymbols.get(i);
                String paramType = getParamTypeName(parameterSymbol.typeDescriptor().typeKind());
                if (paramType != null) {
                    Optional<String> optParamName = parameterSymbol.getName();
                    if (optParamName.isPresent()) {
                        FunctionParam param = new FunctionParam(Integer.toString(i), optParamName.get(), paramType);
                        component.addBalFuncParams(param);
                    }
                }
            }
        }

        Param sizeParam = new Param("Size", Integer.toString(noOfParams));
        Param functionNameParam = new Param("FunctionName", component.getName());
        component.setParam(sizeParam);
        component.setParam(functionNameParam);

        // Extract return type
        Optional<TypeSymbol> optReturnTypeSymbol = functionSymbol.typeDescriptor().returnTypeDescriptor();
        if (optReturnTypeSymbol.isEmpty()) {
            component.setBalFuncReturnType(TypeDescKind.NIL.getName());
        } else {
            String returnType = getReturnTypeName(optReturnTypeSymbol.get().typeKind());
            if (returnType != null) {
                component.setBalFuncReturnType(returnType);
            }
        }

        connector.setComponent(component);
    }

    private String getParamTypeName(TypeDescKind typeKind) {
        return switch (typeKind) {
            case BOOLEAN, INT, STRING, FLOAT, DECIMAL, XML, JSON, ARRAY -> typeKind.getName();
            default -> null;
        };
    }

    private String getReturnTypeName(TypeDescKind typeKind) {
        return switch (typeKind) {
            case NIL, BOOLEAN, INT, STRING, FLOAT, DECIMAL, XML, JSON, ANY, ARRAY -> typeKind.getName();
            default -> null;
        };
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
