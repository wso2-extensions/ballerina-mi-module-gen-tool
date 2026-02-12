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
 * Strategy interface for generating connector artifacts.
 * <p>
 * Implementations of this interface handle the generation of specific artifact types
 * (e.g., XML templates, JSON UI schemas) for connector components and connections.
 * This follows the Strategy Pattern to allow different generation strategies to be
 * selected and composed at runtime.
 * </p>
 */
public interface ArtifactGenerator {

    /**
     * Generates artifacts for a single component (e.g., function XML template, UI schema).
     *
     * @param component       The component to generate artifacts for
     * @param connectorFolder The root output folder
     * @throws IOException If an I/O error occurs during generation
     */
    void generateComponentArtifact(Component component, File connectorFolder) throws IOException;

    /**
     * Generates artifacts for a connection's init/config (e.g., config XML, config UI schema).
     *
     * @param connection      The connection to generate artifacts for
     * @param connectorFolder The root output folder
     * @throws IOException If an I/O error occurs during generation
     */
    void generateConnectionArtifact(Connection connection, File connectorFolder) throws IOException;

    /**
     * Generates aggregate/top-level connector artifacts (e.g., connector.xml, functions.xml).
     *
     * @param connector       The connector to generate artifacts for
     * @param connectorFolder The root output folder
     * @throws IOException If an I/O error occurs during generation
     */
    void generateConnectorArtifact(Connector connector, File connectorFolder) throws IOException;
}
