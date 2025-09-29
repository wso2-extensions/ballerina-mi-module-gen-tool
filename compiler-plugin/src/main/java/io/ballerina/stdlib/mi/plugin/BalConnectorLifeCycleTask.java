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

package io.ballerina.stdlib.mi.plugin;

import io.ballerina.projects.PackageDescriptor;
import io.ballerina.projects.plugins.CompilerLifecycleEventContext;
import io.ballerina.projects.plugins.CompilerLifecycleTask;
import io.ballerina.stdlib.mi.plugin.connectorModel.Component;
import io.ballerina.stdlib.mi.plugin.connectorModel.Connection;
import io.ballerina.stdlib.mi.plugin.connectorModel.Connector;
import io.ballerina.stdlib.mi.plugin.connectorModel.FunctionType;

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

        Connector connector = Connector.getConnector(descriptor);
        connector.setVersion(descriptor.version().value().toString());

        try {
            Path destinationPath = Files.createTempDirectory(Connector.TEMP_PATH);
            generateXmlFiles(destinationPath, connector);
            //TODO: Do output schema generation
            generateJsonFiles(destinationPath, connector);
            URI jarPath = getClass().getProtectionDomain().getCodeSource().getLocation().toURI();
            Utils.copyResources(getClass().getClassLoader(), destinationPath, jarPath, connector.getOrgName(),
                    connector.getModuleName(), connector.getMajorVersion());
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
        // Generate the connector.xml
        connector.generateInstanceXml(connectorFolder);
        // Generate the component.xml for functions
        connector.generateFunctionsXml(connectorFolder, FUNCTION_TEMPLATE_PATH, "functions");
        // Generate the config/component.xml
        connector.generateConfigInstanceXml(connectorFolder, CONFIG_TEMPLATE_PATH, "config");
        // Generate the init.xml from config_template.xml
        connector.generateConfigTemplateXml(connectorFolder, CONFIG_TEMPLATE_PATH, "config");
        for (Connection connection: connector.getConnections()) {
            for (Component component : connection.getComponents()) {
                component.generateTemplateXml(connectorFolder, FUNCTION_TEMPLATE_PATH, "functions");
            }
        }
    }

    private static void generateJsonFiles(Path connectorFolderPath, Connector connector) {
        File connectorFolder = new File(connectorFolderPath.toUri());
        for (Connection connection: connector.getConnections()) {
            connection.getInitComponent().generateUIJson(connectorFolder, CONFIG_TEMPLATE_PATH,
                    connection.getConnectionType());
            for (Component component : connection.getComponents()) {
                component.generateUIJson(connectorFolder, FUNCTION_TEMPLATE_PATH, component.getName());
                //TODO: Generate output schemas
//            component.generateOutputSchemaJson(connectorFolder);
            }
        }
    }
}
