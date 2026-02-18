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

package io.ballerina.mi.generator;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import io.ballerina.mi.model.Component;
import io.ballerina.mi.model.Connection;
import io.ballerina.mi.model.Connector;
import io.ballerina.mi.model.ModelElement;
import io.ballerina.mi.util.Constants;
import io.ballerina.mi.util.JsonTemplateBuilder;
import io.ballerina.mi.util.ResourceLifecycleManager;
import io.ballerina.mi.util.Utils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Orchestrates the connector serialization pipeline.
 * <p>
 * This class is the main entry point for generating MI connector artifacts from a
 * {@link Connector} model. It coordinates the following phases:
 * <ol>
 *   <li><strong>Component generation</strong> — XML templates + JSON UI schemas per component</li>
 *   <li><strong>Connection config generation</strong> — JSON UI schemas per connection</li>
 *   <li><strong>Aggregate XML generation</strong> — instance, functions, and config XMLs</li>
 *   <li><strong>Resource packaging</strong> — copy JARs, icons, and create ZIP</li>
 * </ol>
 * <p>
 * Template rendering and helper registration are delegated to:
 * <ul>
 *   <li>{@link HandlebarsHelperRegistry} — Handlebars helper registration</li>
 *   <li>{@link XmlPropertyWriter} — XML property/parameter element generation</li>
 *   <li>{@link ResourceCopier} — Resource copying (JARs, icons)</li>
 * </ul>
 *
 * @since 0.6.0
 */
public class ConnectorSerializer {

    private static final String CONFIG_TEMPLATE_PATH = "balConnector" + File.separator + "config";
    private static final String FUNCTION_TEMPLATE_PATH = "balConnector" + File.separator + "functions";

    // ─── Template Caching ─────────────────────────────────────────────────────

    private static Handlebars cachedConnectorHandlebars;
    private static final Map<String, Template> connectorTemplateCache = new ConcurrentHashMap<>();
    private static Handlebars cachedSimpleHandlebars;
    private static final Map<String, Template> simpleTemplateCache = new ConcurrentHashMap<>();

    // ─── Instance Fields ──────────────────────────────────────────────────────

    private final PrintStream printStream;
    private final Path sourcePath;
    private final Path targetPath;
    private final List<ArtifactGenerator> artifactGenerators;
    private final ResourcePackager resourcePackager;

    private static final int BATCH_SIZE = 10;

    // ─── Constructors ─────────────────────────────────────────────────────────

    /**
     * Creates a ConnectorSerializer with default artifact generators.
     * This is the backward-compatible constructor used by existing callers.
     */
    public ConnectorSerializer(Path sourcePath, Path targetPath) {
        this.sourcePath = sourcePath;
        this.targetPath = targetPath;
        this.printStream = System.out;
        this.resourcePackager = new ResourcePackager(sourcePath, targetPath);
        this.artifactGenerators = List.of();
    }

    /**
     * Creates a ConnectorSerializer with custom artifact generators (Dependency Injection).
     * Enables the Strategy pattern: callers can inject custom generation strategies.
     */
    public ConnectorSerializer(Path sourcePath, Path targetPath,
                               List<ArtifactGenerator> artifactGenerators,
                               ResourcePackager resourcePackager) {
        this.sourcePath = sourcePath;
        this.targetPath = targetPath;
        this.printStream = System.out;
        this.artifactGenerators = artifactGenerators;
        this.resourcePackager = resourcePackager;
    }

    // ─── Template Engine ──────────────────────────────────────────────────────

    private static synchronized Handlebars getConnectorHandlebars() {
        if (cachedConnectorHandlebars != null) return cachedConnectorHandlebars;
        cachedConnectorHandlebars = new Handlebars(new ClassPathTemplateLoader("/", ""));
        HandlebarsHelperRegistry.registerAll(cachedConnectorHandlebars);
        return cachedConnectorHandlebars;
    }

    static Template getConnectorTemplate(String templateFilePath) throws IOException {
        Template cached = connectorTemplateCache.get(templateFilePath);
        if (cached != null) return cached;
        Template template = getConnectorHandlebars().compile(templateFilePath);
        connectorTemplateCache.put(templateFilePath, template);
        return template;
    }

    private static synchronized Handlebars getSimpleHandlebars() {
        if (cachedSimpleHandlebars != null) return cachedSimpleHandlebars;
        cachedSimpleHandlebars = new Handlebars(new ClassPathTemplateLoader("/", ""));
        return cachedSimpleHandlebars;
    }

    private static Template getSimpleTemplate(String templateFileName) throws IOException {
        Template cached = simpleTemplateCache.get(templateFileName);
        if (cached != null) return cached;
        Template template = getSimpleHandlebars().compile(templateFileName);
        simpleTemplateCache.put(templateFileName, template);
        return template;
    }

    static void clearCaches() {
        cachedConnectorHandlebars = null;
        connectorTemplateCache.clear();
        cachedSimpleHandlebars = null;
        simpleTemplateCache.clear();
    }

    // ─── Serialization Pipeline ───────────────────────────────────────────────

    /**
     * Main serialization entry point. Orchestrates the complete connector generation pipeline.
     * <p>
     * Uses {@link ResourceLifecycleManager} with DSA-based ArrayDeque LIFO stack
     * for deterministic memory cleanup at phase boundaries instead of relying on
     * non-deterministic System.gc() hints.
     */
    public void serialize(Connector connector) {

        // Pre-compute TypeSymbol-derived values and release heavy compiler references
        connector.clearTypeSymbols();

        ResourceLifecycleManager lifecycle = new ResourceLifecycleManager();

        try {
            Path destinationPath = targetPath.resolve("generated");
            if (Files.exists(destinationPath)) {
                FileUtils.cleanDirectory(destinationPath.toFile());
            } else {
                Files.createDirectories(destinationPath);
            }

            File connectorFolder = new File(destinationPath.toUri());
            if (!connectorFolder.exists() && !connectorFolder.mkdirs()) {
                throw new IOException("Failed to create connector folder: " + connectorFolder.getAbsolutePath());
            }

            // Phase 1: Generate per-component files (XML template + JSON UI schema)
            generateComponentArtifacts(connector, connectorFolder, lifecycle);

            // Phase 2: Generate per-connection config JSON
            generateConnectionConfigs(connector, connectorFolder);

            // Phase 3: Generate aggregate XML files
            generateAggregateXmls(connector, connectorFolder);

            // Cleanup init component FunctionParams
            for (Connection connection : connector.getConnections()) {
                if (connection.getInitComponent() != null) {
                    lifecycle.register(connection.getInitComponent().getFunctionParams()::clear);
                }
            }
            lifecycle.cleanup();

            // Phase 4: Copy resources and package
            copyResourcesAndPackage(connector, destinationPath, lifecycle);

        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Phase 1: Generate per-component XML template and JSON UI schema files.
     * Processes in batches with deterministic cleanup to prevent OOM on large connectors.
     */
    private void generateComponentArtifacts(Connector connector, File connectorFolder,
                                            ResourceLifecycleManager lifecycle) {
        int totalComponents = connector.getComponents().size();
        int processed = 0;
        for (Connection connection : connector.getConnections()) {
            for (Component component : connection.getComponents()) {
                component.generateTemplateXml(connectorFolder, FUNCTION_TEMPLATE_PATH, "functions");
                component.generateUIJson(connectorFolder, FUNCTION_TEMPLATE_PATH, component.getName());
                lifecycle.register(component.getFunctionParams()::clear);
                processed++;
                if (processed % BATCH_SIZE == 0) {
                    System.out.println("Processed " + processed + "/" + totalComponents + " components...");
                    lifecycle.cleanup();
                }
            }
        }
        lifecycle.cleanup();
        if (processed % BATCH_SIZE != 0) {
            System.out.println("Processed " + processed + "/" + totalComponents + " components.");
        }
    }

    /**
     * Phase 2: Generate per-connection config JSON UI schema files.
     */
    private void generateConnectionConfigs(Connector connector, File connectorFolder) {
        for (Connection connection : connector.getConnections()) {
            if (connection.getInitComponent() != null) {
                connection.getInitComponent().generateUIJson(connectorFolder, CONFIG_TEMPLATE_PATH,
                        connection.getConnectionType());
            }
        }
    }

    /**
     * Phase 3: Generate aggregate XML files (instance, functions, config).
     */
    private void generateAggregateXmls(Connector connector, File connectorFolder) {
        System.out.println("Generating aggregate XML files...");
        connector.generateInstanceXml(connectorFolder);
        connector.generateFunctionsXml(connectorFolder, Constants.FUNCTION_TEMPLATE_PATH, "functions");
        if (!connector.isBalModule()) {
            connector.generateConfigInstanceXml(connectorFolder, CONFIG_TEMPLATE_PATH, "config");
            connector.generateConfigTemplateXml(connectorFolder, CONFIG_TEMPLATE_PATH, "config");
        }
    }

    /**
     * Phase 4: Copy resources (JARs, icons) and create the final ZIP package.
     */
    private void copyResourcesAndPackage(Connector connector, Path destinationPath,
                                         ResourceLifecycleManager lifecycle)
            throws IOException, URISyntaxException {
        URI jarPath = getClass().getProtectionDomain().getCodeSource().getLocation().toURI();
        ResourceCopier.copyResources(getClass().getClassLoader(), destinationPath, jarPath,
                connector.getOrgName(), connector.getModuleName(), connector.getMajorVersion());

        if (connector.isBalModule()) {
            Files.copy(targetPath.resolve("bin").resolve(connector.getModuleName() + ".jar"),
                    destinationPath.resolve(Connector.LIB_PATH).resolve(connector.getModuleName() + ".jar"));
        } else {
            Path generatedArtifactPath = Paths.get(System.getProperty(Constants.CONNECTOR_TARGET_PATH));
            Files.copy(generatedArtifactPath,
                    destinationPath.resolve(Connector.LIB_PATH).resolve(generatedArtifactPath.getFileName()));
        }

        String zipFilePath = targetPath.resolve(connector.getZipFileName()).toString();

        // Deterministic cleanup before ZIP packaging
        lifecycle.register(connector::clearComponentData);
        lifecycle.register(Connector::reset);
        lifecycle.register(ConnectorSerializer::clearCaches);
        lifecycle.register(JsonTemplateBuilder::clearCache);
        lifecycle.cleanup();

        Runtime rt = Runtime.getRuntime();
        long usedMB = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
        long maxMB = rt.maxMemory() / (1024 * 1024);
        System.out.println("Packaging connector ZIP... (heap: " + usedMB + "MB used / " + maxMB + "MB max)");
        Utils.zipFolder(destinationPath, zipFilePath);
        System.out.println("Connector ZIP created successfully.");
    }

    // ─── File Generation (Template Rendering) ─────────────────────────────────

    /**
     * Generate file (XML/JSON) using the simple template engine (no connector helpers).
     */
    private static void generateFile(String templateName, String outputName,
                                     ModelElement element, String extension) {
        try {
            String templateFileName = String.format("%s.%s", templateName, extension);
            Template template = getSimpleTemplate(templateFileName);
            String output = template.apply(element);

            String outputFileName = String.format("%s.%s", outputName, extension);
            Utils.writeFile(outputFileName, output);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Generate file (XML/JSON) using the connector template engine (with registered helpers).
     */
    private static void generateFileForConnector(String templatePath, String templateName,
                                                 String outputName, ModelElement element,
                                                 String extension) {
        try {
            String templateFileName = String.format("%s/%s.%s", templatePath, templateName, extension);
            Template template = getConnectorTemplate(templateFileName);
            String output = template.apply(element);

            boolean isConfigFile = templatePath.equals(CONFIG_TEMPLATE_PATH);
            String outputFileName;
            if (isConfigFile) {
                int lastSeparator = Math.max(outputName.lastIndexOf('/'), outputName.lastIndexOf('\\'));
                String filename = (lastSeparator >= 0) ? outputName.substring(lastSeparator + 1) : outputName;
                outputFileName = String.format("%s.%s", filename, extension);
                if (lastSeparator >= 0) {
                    String directory = outputName.substring(0, lastSeparator + 1);
                    outputFileName = directory + outputFileName;
                }
            } else {
                String sanitizedOutputName = sanitizeFileName(outputName, false);
                outputFileName = String.format("%s.%s", sanitizedOutputName, extension);
            }
            Utils.writeFile(outputFileName, output);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Sanitize filename by replacing dots with underscores.
     * Optionally converts to PascalCase for function files (not config files).
     * Preserves the directory path structure.
     */
    static String sanitizeFileName(String filePath, boolean isConfigFile) {
        if (filePath == null || filePath.isEmpty()) {
            return filePath;
        }

        int lastSeparatorIndex = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
        String filename;
        String directory = "";

        if (lastSeparatorIndex < 0) {
            filename = filePath;
        } else {
            directory = filePath.substring(0, lastSeparatorIndex + 1);
            filename = filePath.substring(lastSeparatorIndex + 1);
        }

        String sanitizedFilename = filename.replace(".", "_");

        if (!isConfigFile) {
            if (sanitizedFilename.contains("_")) {
                String[] parts = sanitizedFilename.split("_");
                if (parts.length > 0) {
                    StringBuilder pascalCase = new StringBuilder();
                    pascalCase.append(parts[0].toLowerCase());
                    for (int i = 1; i < parts.length; i++) {
                        if (!parts[i].isEmpty()) {
                            pascalCase.append("_");
                            pascalCase.append(StringUtils.capitalize(parts[i].toLowerCase()));
                        }
                    }
                    sanitizedFilename = pascalCase.toString();
                }
            }
        }

        return directory + sanitizedFilename;
    }

    // ─── Public Convenience Methods (backward-compatible API) ─────────────────

    public static void generateXml(String templateName, String outputName, ModelElement element) {
        generateFile(templateName, outputName, element, "xml");
    }

    public static void generateJson(String templateName, String outputName, ModelElement element) {
        generateFile(templateName, outputName, element, "json");
    }

    public static void generateXmlForConnector(String templatePath, String templateName,
                                               String outputName, ModelElement element) {
        generateFileForConnector(templatePath, templateName, outputName, element, "xml");
    }

    public static void generateJsonForConnector(String templatePath, String templateName,
                                                String outputName, ModelElement element) {
        generateFileForConnector(templatePath, templateName, outputName, element, "json");
    }
}
