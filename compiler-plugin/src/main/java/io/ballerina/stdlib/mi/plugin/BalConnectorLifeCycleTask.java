package io.ballerina.stdlib.mi.plugin;

import io.ballerina.projects.PackageDescriptor;
import io.ballerina.projects.plugins.CompilerLifecycleEventContext;
import io.ballerina.projects.plugins.CompilerLifecycleTask;
import io.ballerina.stdlib.mi.plugin.connectorModel.Component;
import io.ballerina.stdlib.mi.plugin.connectorModel.Connector;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static io.ballerina.stdlib.mi.plugin.MICompilerPlugin.CONNECTOR_TARGET_PATH;

public class BalConnectorLifeCycleTask implements CompilerLifecycleTask<CompilerLifecycleEventContext>  {
    private static final String CONFIG_TEMPLATE_PATH = "balConnector" + File.separator + "config";
    private static final String FUNCTION_TEMPLATE_PATH = "balConnector" + File.separator + "functions";

    @Override
    public void perform(CompilerLifecycleEventContext context) {
        Path generatedArtifactPath = Paths.get(System.getProperty(CONNECTOR_TARGET_PATH));

        PackageDescriptor descriptor = context.currentPackage().manifest().descriptor();

        Connector connector = Connector.getConnector();
        // TODO: Remove one of module name and name
        connector.setName(descriptor.name().value());
        connector.setVersion(descriptor.version().value().toString());

        try {
            Path destinationPath = Files.createTempDirectory(Connector.TEMP_PATH);
            generateXmlFiles(destinationPath, connector);
            //TODO: Do output schema generation
            generateJsonFiles(destinationPath, connector);
            URI jarPath = getClass().getProtectionDomain().getCodeSource().getLocation().toURI();
            Utils.copyResources(getClass().getClassLoader(), destinationPath, jarPath, connector.getOrgName(),
                    connector.getModuleName(), connector.getModuleVersion());
            Files.copy(generatedArtifactPath, destinationPath.resolve(Connector.LIB_PATH).resolve(generatedArtifactPath.getFileName()));
            String zipFilePath = generatedArtifactPath.getParent().getParent().toString() + File.separator + "Downloaded" + File.separator + connector.getZipFileName();
            Utils.zipFolder(destinationPath, zipFilePath);
            Utils.deleteDirectory(destinationPath);
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private static void generateXmlFiles(Path connectorFolderPath, Connector connector) {
        File connectorFolder = new File(connectorFolderPath.toUri());
        if (!connectorFolder.exists()) {
            connectorFolder.mkdir();
        }

        connector.generateInstanceXml(connectorFolder);
        connector.generateFunctionsXml(connectorFolder, FUNCTION_TEMPLATE_PATH, "functions");

        for (Component component : connector.getComponents()) {
            if (component.getIsConfig()) {
                component.generateInstanceXml(connectorFolder, CONFIG_TEMPLATE_PATH, "config");
                component.generateTemplateXml(connectorFolder, CONFIG_TEMPLATE_PATH, "config");
            } else {
                component.generateTemplateXml(connectorFolder, FUNCTION_TEMPLATE_PATH, "functions");
            }
        }
    }

    private static void generateJsonFiles(Path connectorFolderPath, Connector connector) {
        File connectorFolder = new File(connectorFolderPath.toUri());
        for (Component component : connector.getComponents()) {
            if (component.getIsConfig()) {
                component.generateUIJson(connectorFolder, CONFIG_TEMPLATE_PATH, connector.getName());
            }
            component.generateUIJson(connectorFolder, FUNCTION_TEMPLATE_PATH, component.getName());
            //TODO: Generate output schemas
//            component.generateOutputSchemaJson(connectorFolder);
        }
    }
}
