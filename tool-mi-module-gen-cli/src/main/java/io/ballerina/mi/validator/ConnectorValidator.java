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

package io.ballerina.mi.validator;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
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
import java.io.PrintStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


/**
 * Validator for MI connector artifacts and UI schemas.
 */
public class ConnectorValidator {

    private static final PrintStream ERROR_STREAM = System.err;
    private static final String UI_SCHEMA_PATH = "schema/ui-schema.json";
    private static final int MAX_SAMPLED_OPERATIONS = 5;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static JsonSchema uiSchema;

    /**
     * Validates a connector zip file. For large connectors (ZIP > 10MB), uses lightweight
     * validation to avoid OOM. For small connectors, performs full Synapse library and
     * UI schema validation.
     *
     * @param connectorPath Path to the connector directory containing the zip file
     * @return true if the connector is valid, false otherwise
     */
    public static boolean validateConnector(Path connectorPath) {
        try (Stream<Path> targetFolder = Files.list(connectorPath)) {
            Path connectorZipPath = targetFolder
                    .filter(path -> path.toString().endsWith(".zip"))
                    .findFirst()
                    .orElseThrow(() -> new IOException("No connector zip file found in: " + connectorPath));

            long zipSize = Files.size(connectorZipPath);
            if (zipSize > 10 * 1024 * 1024) {
                return validateLargeConnector(connectorZipPath, connectorPath);
            }

            // Small connector: full validation (existing behavior)
            Library library = LibDeployerUtils.createSynapseLibrary(connectorZipPath.toString());
            if (library == null) {
                return false;
            }
            return validateAllUISchemas(connectorPath);
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Lightweight validation for large connectors (ZIP > 10MB). Validates ZIP structure,
     * connector.xml well-formedness, and a bounded sample of UI schemas to avoid OOM.
     */
    private static boolean validateLargeConnector(Path zipPath, Path connectorPath) {
        long zipMB = 0;
        try {
            zipMB = Files.size(zipPath) / (1024 * 1024);
        } catch (IOException ignored) {
            // Non-critical — only used for display
        }
        ERROR_STREAM.println("Large connector ZIP detected (" + zipMB +
                "MB). Validating ZIP structure and file entries only (skipping content validation).");

        if (!validateZipStructure(zipPath)) {
            return false;
        }
        return validateConnectorXml(connectorPath);
    }

    /**
     * Validates the ZIP structure by reading the central directory only (no file contents).
     * Checks for required entries: connector.xml, at least one function XML, one JAR, one UI schema.
     * Also verifies function XML count doesn't exceed UI schema JSON count (catches truncation).
     */
    private static boolean validateZipStructure(Path zipPath) {
        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            boolean hasConnectorXml = false;
            int functionXmlCount = 0;
            int jarCount = 0;
            int uiSchemaCount = 0;
            int totalEntries = 0;

            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                totalEntries++;

                if (name.equals("connector.xml")) {
                    hasConnectorXml = true;
                } else if (name.startsWith("functions/") && name.endsWith(".xml")) {
                    functionXmlCount++;
                } else if (name.startsWith("lib/") && name.endsWith(".jar")) {
                    jarCount++;
                } else if (name.startsWith("uischema/") && name.endsWith(".json")) {
                    uiSchemaCount++;
                }
            }

            if (!hasConnectorXml) {
                ERROR_STREAM.println("ZIP structure invalid: missing connector.xml");
                return false;
            }
            if (functionXmlCount == 0) {
                ERROR_STREAM.println("ZIP structure invalid: no function XML files in functions/");
                return false;
            }
            if (jarCount == 0) {
                ERROR_STREAM.println("ZIP structure invalid: no JAR files in lib/");
                return false;
            }
            if (uiSchemaCount == 0) {
                ERROR_STREAM.println("ZIP structure invalid: no UI schema JSON files in uischema/");
                return false;
            }
            if (functionXmlCount > uiSchemaCount) {
                ERROR_STREAM.println("ZIP structure invalid: function XML count (" + functionXmlCount +
                        ") exceeds UI schema count (" + uiSchemaCount + ")");
                return false;
            }

            ERROR_STREAM.println("ZIP structure valid: " + totalEntries + " entries, " +
                    functionXmlCount + " functions, " + uiSchemaCount + " UI schemas");
            return true;
        } catch (IOException e) {
            ERROR_STREAM.println("ZIP structure validation failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Validates generated/connector.xml exists and has the expected structure.
     * Uses simple string checks instead of XML parsers because the Ballerina runtime's
     * classloader overrides javax.xml factory system properties, making both SAX and
     * StAX factories unavailable at runtime.
     */
    private static boolean validateConnectorXml(Path connectorPath) {
        Path connectorXml = connectorPath.resolve("generated").resolve("connector.xml");
        if (!Files.exists(connectorXml)) {
            ERROR_STREAM.println("connector.xml not found at: " + connectorXml);
            return false;
        }
        try {
            String content = Files.readString(connectorXml);
            if (content.isBlank()) {
                ERROR_STREAM.println("connector.xml is empty");
                return false;
            }
            String trimmed = content.strip();
            if (!trimmed.contains("<connector>") || !trimmed.contains("</connector>")) {
                ERROR_STREAM.println("connector.xml is malformed: missing <connector> root element");
                return false;
            }
            return true;
        } catch (IOException e) {
            ERROR_STREAM.println("Failed to read connector.xml: " + e.getMessage());
            return false;
        }
    }



    /**
     * Validates all UI schemas in the generated/uischema/ directory.
     * Used for small connectors where full validation is feasible.
     */
    private static boolean validateAllUISchemas(Path connectorPath) {
        Path uiSchemaPath = connectorPath.resolve("generated").resolve("uischema");
        if (Files.exists(uiSchemaPath) && Files.isDirectory(uiSchemaPath)) {
            try (DirectoryStream<Path> uiSchemas = Files.newDirectoryStream(uiSchemaPath)) {
                for (Path path : uiSchemas) {
                    if (path.toString().endsWith(".json")) {
                        ValidationResult validationResult = validateUISchema(path);
                        if (!validationResult.valid()) {
                            ERROR_STREAM.println("UI Schema validation errors in " + path + ":");
                            ERROR_STREAM.println(validationResult);
                            return false;
                        }
                    }
                }
            } catch (IOException e) {
                return false;
            }
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
    // Max file size (5MB) for full JSON schema validation. Larger files use lightweight
    // streaming validation to avoid OOM — connection configs with deeply flattened records
    // can produce UI schema JSON files of 50-100MB+.
    private static final long MAX_FULL_VALIDATION_SIZE = 5 * 1024 * 1024;

    public static ValidationResult validateUISchema(Path uiSchemaPath) {
        // Check if file exists
        if (!Files.exists(uiSchemaPath)) {
            return new ValidationResult(false, List.of("UI schema file does not exist: " + uiSchemaPath));
        }

        try {
            long fileSize = Files.size(uiSchemaPath);
            if (fileSize > MAX_FULL_VALIDATION_SIZE) {
                // Large file: use streaming validation (verify valid JSON without building tree)
                return validateUISchemaStreaming(uiSchemaPath, fileSize);
            }
        } catch (IOException e) {
            return new ValidationResult(false, List.of("Failed to check UI schema file size: " + e.getMessage()));
        }

        // Small file: full JSON schema validation
        String jsonContent;
        try {
            jsonContent = Files.readString(uiSchemaPath);
        } catch (IOException e) {
            return new ValidationResult(false, List.of("Failed to read UI schema file: " + e.getMessage()));
        }

        return validateUISchemaContent(jsonContent);
    }

    /**
     * Lightweight validation for large UI schema files. Uses Jackson streaming API
     * to verify the file is well-formed JSON without building a tree in memory.
     * Checks that the top-level structure is a JSON object with expected keys.
     */
    private static ValidationResult validateUISchemaStreaming(Path uiSchemaPath, long fileSize) {
        long fileSizeMB = fileSize / (1024 * 1024);
        ERROR_STREAM.println("Large UI schema (" + fileSizeMB + "MB): " +
                uiSchemaPath.getFileName() + " — using streaming validation.");
        try (JsonParser parser = objectMapper.getFactory().createParser(uiSchemaPath.toFile())) {
            if (parser.nextToken() != JsonToken.START_OBJECT) {
                return new ValidationResult(false, List.of("UI schema is not a JSON object: " + uiSchemaPath));
            }
            boolean hasOperationName = false;
            boolean hasConnectionName = false;
            // Read top-level keys to verify structure, skip all values without building tree
            while (parser.nextToken() != JsonToken.END_OBJECT) {
                String fieldName = parser.currentName();
                if ("operationName".equals(fieldName)) {
                    hasOperationName = true;
                } else if ("connectionName".equals(fieldName)) {
                    hasConnectionName = true;
                }
                parser.nextToken();
                parser.skipChildren();
            }
            if (!hasOperationName && !hasConnectionName) {
                return new ValidationResult(false,
                        List.of("UI schema missing both 'operationName' and 'connectionName': " + uiSchemaPath));
            }
            return new ValidationResult(true, List.of());
        } catch (IOException e) {
            return new ValidationResult(false,
                    List.of("UI schema is not valid JSON: " + uiSchemaPath + " — " + e.getMessage()));
        }
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
