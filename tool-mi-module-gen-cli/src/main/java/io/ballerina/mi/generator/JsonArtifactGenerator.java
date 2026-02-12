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

import java.io.File;
import java.io.IOException;

/**
 * Strategy implementation that generates JSON UI schema artifacts for the connector.
 * <p>
 * Responsible for generating:
 * - Per-component UI schema JSON files (used by MI FormGenerator)
 * - Per-connection config UI schema JSON files
 * </p>
 */
public class JsonArtifactGenerator implements ArtifactGenerator {

    private static final String CONFIG_TEMPLATE_PATH = "balConnector" + File.separator + "config";
    private static final String FUNCTION_TEMPLATE_PATH = "balConnector" + File.separator + "functions";

    private final TemplateEngine templateEngine;

    public JsonArtifactGenerator(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    @Override
    public void generateComponentArtifact(Component component, File connectorFolder) throws IOException {
        File uiSchemaDir = new File(connectorFolder, "uischema");
        if (!uiSchemaDir.exists()) {
            uiSchemaDir.mkdir();
        }
        String templateFilePath = String.format("%s/%s.json", FUNCTION_TEMPLATE_PATH, "component");
        String sanitizedName = ConnectorSerializer.sanitizeFileName(
                uiSchemaDir + File.separator + component.getName(), false);
        templateEngine.renderToFile(templateFilePath, sanitizedName, component, "json");
    }

    @Override
    public void generateConnectionArtifact(Connection connection, File connectorFolder) throws IOException {
        if (connection.getInitComponent() == null) {
            return;
        }
        File uiSchemaDir = new File(connectorFolder, "uischema");
        if (!uiSchemaDir.exists()) {
            uiSchemaDir.mkdir();
        }
        String templateFilePath = String.format("%s/%s.json", CONFIG_TEMPLATE_PATH, "component");
        // Config files use connectionType as-is (no PascalCase sanitization)
        String outputPath = uiSchemaDir + File.separator + connection.getConnectionType();
        templateEngine.renderToFile(templateFilePath, outputPath, connection.getInitComponent(), "json");
    }

    @Override
    public void generateConnectorArtifact(Connector connector, File connectorFolder) throws IOException {
        // JSON UI schema doesn't have aggregate-level connector artifacts
        // All JSON generation is per-component and per-connection
    }
}
