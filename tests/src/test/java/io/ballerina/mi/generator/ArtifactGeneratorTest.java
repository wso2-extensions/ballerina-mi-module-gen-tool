/*
 * Copyright (c) 2026, WSO2 LLC. (https://www.wso2.com).
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
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.*;

public class ArtifactGeneratorTest {

    @Mock
    private TemplateEngine templateEngine;

    private JsonArtifactGenerator jsonGenerator;
    private XmlArtifactGenerator xmlGenerator;
    private Path tempDir;

    @BeforeMethod
    public void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);
        jsonGenerator = new JsonArtifactGenerator(templateEngine);
        xmlGenerator = new XmlArtifactGenerator(templateEngine);
        tempDir = Files.createTempDirectory("artifact-test");
    }

    @AfterMethod
    public void tearDown() throws IOException {
        Files.walk(tempDir)
                .sorted((a, b) -> b.compareTo(a)) // Reverse order/delete files first
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        // ignore
                    }
                });
    }

    @Test
    public void testJson_GenerateComponentArtifact() throws IOException {
        Component component = mock(Component.class);
        when(component.getName()).thenReturn("TestComponent");

        jsonGenerator.generateComponentArtifact(component, tempDir.toFile());

        verify(templateEngine).renderToFile(
                eq("balConnector/functions/component.json"),
                matches(".*uischema.*TestComponent"), // Path match
                eq(component),
                eq("json")
        );
    }

    @Test
    public void testJson_GenerateConnectionArtifact() throws IOException {
        Connection connection = mock(Connection.class);
        Component initComponent = mock(Component.class);
        when(connection.getInitComponent()).thenReturn(initComponent);
        when(connection.getConnectionType()).thenReturn("MyConnection");

        jsonGenerator.generateConnectionArtifact(connection, tempDir.toFile());

        verify(templateEngine).renderToFile(
                eq("balConnector/config/component.json"),
                matches(".*uischema.*MyConnection"),
                eq(initComponent),
                eq("json")
        );
    }

    @Test
    public void testXml_GenerateComponentArtifact() throws IOException {
        Component component = mock(Component.class);
        when(component.getName()).thenReturn("TestComponent");

        xmlGenerator.generateComponentArtifact(component, tempDir.toFile());

        verify(templateEngine).renderToFile(
                eq("balConnector/functions/functions_template.xml"),
                matches(".*functions.*TestComponent"),
                eq(component),
                eq("xml")
        );
    }

    @Test
    public void testXml_GenerateConnectorArtifact_NonModule() throws IOException {
        Connector connector = mock(Connector.class);
        when(connector.isBalModule()).thenReturn(false);

        xmlGenerator.generateConnectorArtifact(connector, tempDir.toFile());

        // Verify connector.xml
        verify(templateEngine).renderToFile(
                eq("balConnector/connector.xml"),
                matches(".*connector"),
                eq(connector),
                eq("xml")
        );

        // Verify functions/component.xml
        verify(templateEngine).renderToFile(
                eq("balConnector/functions/component.xml"),
                matches(".*functions.*component"),
                eq(connector),
                eq("xml")
        );

        // Verify config/component.xml (since it is not module)
        verify(templateEngine).renderToFile(
                eq("balConnector/config/component.xml"),
                matches(".*config.*component"),
                eq(connector),
                eq("xml")
        );
    }
}
