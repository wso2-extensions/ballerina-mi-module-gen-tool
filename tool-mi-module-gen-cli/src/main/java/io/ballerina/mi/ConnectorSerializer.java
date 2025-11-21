package io.ballerina.mi;

import io.ballerina.mi.connectorModel.Component;
import io.ballerina.mi.connectorModel.Connection;
import io.ballerina.mi.connectorModel.Connector;
//import io.ballerina.mi.model.Component;
import io.ballerina.mi.util.Constants;
import io.ballerina.mi.util.Utils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

//TODO: Move all the connector generation logic here from Utils.
public class ConnectorSerializer {

    private static final String CONFIG_TEMPLATE_PATH = "balConnector" + File.separator + "config";
    private static final String FUNCTION_TEMPLATE_PATH = "balConnector" + File.separator + "functions";

    private final PrintStream printStream;
    private final Path sourcePath;
    private final Path targetPath;

    public ConnectorSerializer(Path sourcePath, Path targetPath) {
        this.sourcePath = sourcePath;
        this.targetPath = targetPath;
        this.printStream = System.out;
    }

    public void serialize(Connector connector) {

        try {
            Path destinationPath = targetPath.resolve("generated");
            if (Files.exists(destinationPath)) {
                FileUtils.cleanDirectory(destinationPath.toFile());
            } else {
                Files.createDirectories(destinationPath);
            }
            generateXmlFiles(destinationPath, connector);
            //TODO: Do output schema generation
            generateJsonFiles(destinationPath, connector);
            URI jarPath = getClass().getProtectionDomain().getCodeSource().getLocation().toURI();
            Utils.copyResources(getClass().getClassLoader(), destinationPath, jarPath, connector.getOrgName(),
                    connector.getModuleName(), connector.getMajorVersion());

            String zipFilePath;
            if (connector.isBalModule()) {
                Files.copy(targetPath.resolve("bin").resolve(connector.getModuleName() + ".jar"), destinationPath.resolve(Connector.LIB_PATH).resolve(connector.getModuleName() + ".jar"));
                zipFilePath = targetPath.resolve(connector.getZipFileName()).toString();
            } else {
                Path generatedArtifactPath = Paths.get(System.getProperty(Constants.CONNECTOR_TARGET_PATH));
                Files.copy(generatedArtifactPath, destinationPath.resolve(Connector.LIB_PATH).resolve(generatedArtifactPath.getFileName()));
                zipFilePath = generatedArtifactPath.getParent().getParent().resolve(connector.getZipFileName()).toString();
            }
            Utils.zipFolder(destinationPath, zipFilePath);
//            Utils.deleteDirectory(destinationPath);
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private static void generateXmlFiles(Path connectorFolderPath, Connector connector) {
        File connectorFolder = new File(connectorFolderPath.toUri());
        if (!connectorFolder.exists()) {
            connectorFolder.mkdir();
        }
        // Generate the connector.xml
        connector.generateInstanceXml(connectorFolder);
        // Generate the component.xml for functions
        connector.generateFunctionsXml(connectorFolder, Constants.FUNCTION_TEMPLATE_PATH, "functions");
        if (!connector.isBalModule()) {
            // Generate the config/component.xml
            connector.generateConfigInstanceXml(connectorFolder, CONFIG_TEMPLATE_PATH, "config");
            // Generate the init.xml from config_template.xml
            connector.generateConfigTemplateXml(connectorFolder, CONFIG_TEMPLATE_PATH, "config");
        }
        for (Connection connection : connector.getConnections()) {
            for (Component component : connection.getComponents()) {
                component.generateTemplateXml(connectorFolder, FUNCTION_TEMPLATE_PATH, "functions");
            }
        }
    }

    private static void generateJsonFiles(Path connectorFolderPath, Connector connector) {
        File connectorFolder = new File(connectorFolderPath.toUri());
        for (Connection connection : connector.getConnections()) {
            // TODO: revisit this
            if (connection.getInitComponent() != null) {
                connection.getInitComponent().generateUIJson(connectorFolder, CONFIG_TEMPLATE_PATH,
                        connection.getConnectionType());
            }
            for (Component component : connection.getComponents()) {
                component.generateUIJson(connectorFolder, FUNCTION_TEMPLATE_PATH, component.getName());
                //TODO: Generate output schemas
//            component.generateOutputSchemaJson(connectorFolder);
            }
        }
    }
}
