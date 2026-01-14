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

package io.ballerina.mi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.apache.synapse.libraries.model.Library;
import org.apache.synapse.libraries.util.LibDeployerUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Validator for MI connector artifacts and UI schemas.
 */
public class ConnectorValidator {

    private static final String UI_SCHEMA_PATH = "schema/ui-schema.json";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static JsonSchema uiSchema;

    /**
     * Validates a connector zip file.
     *
     * @param connectorPath Path to the connector zip file
     * @return true if the connector is valid, false otherwise
     */
    public static boolean validateConnector(Path connectorPath) {
        try (Stream<Path> targetFolder = Files.list(connectorPath)) {
            Path connectorZipPath = targetFolder
                    .filter(path -> path.toString().endsWith(".zip"))
                    .findFirst()
                    .orElseThrow(() -> new IOException("No connector zip file found in: " + connectorPath));
            Library library = LibDeployerUtils.createSynapseLibrary(connectorZipPath.toString());
            if (library == null) {
                return false;
            }
            Path uiSchemaPath = connectorPath.resolve("generated").resolve("uischema");
            if (Files.exists(uiSchemaPath) && Files.isDirectory(uiSchemaPath)) {
                try (DirectoryStream<Path> uiSchemas = Files.newDirectoryStream(uiSchemaPath)) {
                    for (Path path : uiSchemas) {
                        if (path.toString().endsWith(".json")) {
                            ValidationResult validationResult = validateUISchema(path);
                            if (!validationResult.valid()) {
                                System.out.println("UI Schema validation errors in " + path + ":");
                                System.out.println(validationResult);
                                return false;
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    /**
     * Loads the UI schema from resources. Uses lazy initialization.
     *
     * @return The loaded JsonSchema
     * @throws IOException if the schema cannot be loaded
     */
    private static JsonSchema getUISchema() throws IOException {
        if (uiSchema == null) {
            synchronized (ConnectorValidator.class) {
                if (uiSchema == null) {
                    JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
                    try (InputStream schemaStream = ConnectorValidator.class.getClassLoader()
                            .getResourceAsStream(UI_SCHEMA_PATH)) {
                        if (schemaStream == null) {
                            throw new IOException("UI schema resource not found: " + UI_SCHEMA_PATH);
                        }
                        uiSchema = factory.getSchema(schemaStream);
                    }
                }
            }
        }
        return uiSchema;
    }

    /**
     * Validates a UI schema JSON file that will be loaded by FormGenerator.
     *
     * @param uiSchemaPath Path to the UI schema JSON file
     * @return ValidationResult containing validation status and any error messages
     */
    public static ValidationResult validateUISchema(Path uiSchemaPath) {
        // Check if file exists
        if (!Files.exists(uiSchemaPath)) {
            return new ValidationResult(false, List.of("UI schema file does not exist: " + uiSchemaPath));
        }

        // Read JSON content
        String jsonContent;
        try {
            jsonContent = Files.readString(uiSchemaPath);
        } catch (IOException e) {
            return new ValidationResult(false, List.of("Failed to read UI schema file: " + e.getMessage()));
        }

        return validateUISchemaContent(jsonContent);
    }

    /**
     * Validates a UI schema from a JSON string using JSON Schema validation.
     *
     * @param jsonContent The JSON content as a string
     * @return ValidationResult containing validation status and any error messages
     */
    public static ValidationResult validateUISchemaContent(String jsonContent) {
        if (jsonContent == null || jsonContent.isBlank()) {
            return new ValidationResult(false, List.of("UI schema content is empty or null"));
        }

        try {
            // Parse the JSON content
            JsonNode jsonNode = objectMapper.readTree(jsonContent);

            // Get the schema and validate
            JsonSchema schema = getUISchema();
            Set<ValidationMessage> validationMessages = schema.validate(jsonNode);

            if (validationMessages.isEmpty()) {
                return new ValidationResult(true, List.of());
            }

            // Convert validation messages to error strings
            List<String> errors = new ArrayList<>();
            for (ValidationMessage message : validationMessages) {
                errors.add(formatValidationMessage(message));
            }

            return new ValidationResult(false, errors);

        } catch (IOException e) {
            return new ValidationResult(false, List.of("Failed to validate UI schema: " + e.getMessage()));
        }
    }

    /**
     * Formats a validation message for better readability.
     *
     * @param message The validation message
     * @return Formatted error string
     */
    private static String formatValidationMessage(ValidationMessage message) {
        String path = message.getInstanceLocation().toString();
        String msg = message.getMessage();

        // Clean up the path for readability
        if (path.isEmpty() || path.equals("$")) {
            return msg;
        }
        return path + ": " + msg;
    }

    /**
     * Result class for validation operations.
     */
    public record ValidationResult(boolean valid, List<String> errors) {
        public ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors != null ? errors : new ArrayList<>();
        }

        @NotNull
        @Override
        public String toString() {
            if (valid) {
                return "ValidationResult: VALID";
            }
            return "ValidationResult: INVALID\nErrors:\n- " + String.join("\n- ", errors);
        }
    }
}
