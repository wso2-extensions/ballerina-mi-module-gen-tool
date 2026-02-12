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

import io.ballerina.mi.model.Component;
import io.ballerina.mi.model.Connection;
import io.ballerina.mi.model.Connector;
import io.ballerina.mi.util.Constants;

import java.io.File;
import java.io.IOException;

/**
 * Strategy implementation that generates XML artifacts for the connector.
 * <p>
 * Responsible for generating:
 * - Per-component function template XML files
 * - Aggregate connector.xml, functions.xml
 * - Config instance and template XML for non-module connectors
 * </p>
 */
public class XmlArtifactGenerator implements ArtifactGenerator {

    private static final String CONFIG_TEMPLATE_PATH = "balConnector" + File.separator + "config";
    private static final String FUNCTION_TEMPLATE_PATH = "balConnector" + File.separator + "functions";

    private final TemplateEngine templateEngine;

    public XmlArtifactGenerator(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    @Override
    public void generateComponentArtifact(Component component, File connectorFolder) throws IOException {
        File functionsDir = new File(connectorFolder, "functions");
        if (!functionsDir.exists()) {
            functionsDir.mkdir();
        }
        String templateFilePath = String.format("%s/%s.xml", FUNCTION_TEMPLATE_PATH, "functions_template");
        String sanitizedName = ConnectorSerializer.sanitizeFileName(
                functionsDir + File.separator + component.getName(), false);
        templateEngine.renderToFile(templateFilePath, sanitizedName, component, "xml");
    }

    @Override
    public void generateConnectionArtifact(Connection connection, File connectorFolder) throws IOException {
        // XML config artifacts are generated at the connector level (aggregate), not per-connection
        // See generateConnectorArtifact for config XML generation
    }

    @Override
    public void generateConnectorArtifact(Connector connector, File connectorFolder) throws IOException {
        // connector.xml
        String connectorTemplatePath = String.format("%s/%s.xml", "balConnector", "connector");
        templateEngine.renderToFile(connectorTemplatePath,
                connectorFolder + File.separator + Connector.TYPE_NAME, connector, "xml");

        // functions/component.xml (aggregate)
        File functionsDir = new File(connectorFolder, "functions");
        if (!functionsDir.exists()) {
            functionsDir.mkdir();
        }
        String functionsTemplatePath = String.format("%s/%s.xml", Constants.FUNCTION_TEMPLATE_PATH, "component");
        templateEngine.renderToFile(functionsTemplatePath,
                functionsDir + File.separator + "component", connector, "xml");

        // Config XMLs (only for non-module connectors)
        if (!connector.isBalModule()) {
            File configDir = new File(connectorFolder, "config");
            if (!configDir.exists()) {
                configDir.mkdir();
            }
            // config/component.xml
            String configComponentPath = String.format("%s/%s.xml", CONFIG_TEMPLATE_PATH, "component");
            templateEngine.renderToFile(configComponentPath,
                    configDir + File.separator + "component", connector, "xml");
            // config/init_template.xml
            String configTemplatePath = String.format("%s/%s.xml", CONFIG_TEMPLATE_PATH, Constants.INIT_FUNCTION_NAME + "_template");
            templateEngine.renderToFile(configTemplatePath,
                    configDir + File.separator + Constants.INIT_FUNCTION_NAME, connector, "xml");
        }
    }
}
