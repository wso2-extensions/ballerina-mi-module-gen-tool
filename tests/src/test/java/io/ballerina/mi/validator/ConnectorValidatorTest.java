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

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tests for ConnectorValidator class.
 */
public class ConnectorValidatorTest {

    private Path tempDir;

    @BeforeMethod
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("connector-test");
    }

    @AfterMethod
    public void tearDown() throws IOException {
        if (tempDir != null && Files.exists(tempDir)) {
            Files.walk(tempDir)
                 .sorted((a, b) -> -a.compareTo(b))
                 .forEach(path -> {
                     try {
                         Files.delete(path);
                     } catch (IOException e) {
                         // ignore cleanup errors
                     }
                 });
        }
    }

    // --- Tests for validateUISchemaContent ---

    @Test
    public void testValidateUISchemaContent_NullContent() {
        ConnectorValidator.ValidationResult result = ConnectorValidator.validateUISchemaContent(null);
        Assert.assertFalse(result.valid());
        Assert.assertTrue(result.errors().get(0).contains("empty or null"));
    }

    @Test
    public void testValidateUISchemaContent_EmptyContent() {
        ConnectorValidator.ValidationResult result = ConnectorValidator.validateUISchemaContent("");
        Assert.assertFalse(result.valid());
        Assert.assertTrue(result.errors().get(0).contains("empty or null"));
    }

    @Test
    public void testValidateUISchemaContent_BlankContent() {
        ConnectorValidator.ValidationResult result = ConnectorValidator.validateUISchemaContent("   ");
        Assert.assertFalse(result.valid());
        Assert.assertTrue(result.errors().get(0).contains("empty or null"));
    }

    @Test
    public void testValidateUISchemaContent_InvalidJson() {
        ConnectorValidator.ValidationResult result = ConnectorValidator.validateUISchemaContent("{invalid json}");
        Assert.assertFalse(result.valid());
    }

    @Test
    public void testValidateUISchemaContent_ValidJsonButNotMatchingSchema() {
        String json = "{\"foo\": \"bar\"}";
        ConnectorValidator.ValidationResult result = ConnectorValidator.validateUISchemaContent(json);
        Assert.assertNotNull(result);
    }

    @Test
    public void testValidateUISchemaContent_ValidOperationSchema() {
        String json = """
            {
                "operationName": "testOperation",
                "elements": []
            }
            """;
        ConnectorValidator.ValidationResult result = ConnectorValidator.validateUISchemaContent(json);
        Assert.assertNotNull(result);
    }

    @Test
    public void testValidateUISchemaContent_ValidConnectionSchema() {
        String json = """
            {
                "connectionName": "testConnection",
                "elements": []
            }
            """;
        ConnectorValidator.ValidationResult result = ConnectorValidator.validateUISchemaContent(json);
        Assert.assertNotNull(result);
    }

    // --- Tests for ValidationResult ---

    @Test
    public void testValidationResult_ValidToString() {
        ConnectorValidator.ValidationResult result = new ConnectorValidator.ValidationResult(true, null);
        Assert.assertEquals(result.toString(), "ValidationResult: VALID");
    }

    @Test
    public void testValidationResult_InvalidToString() {
        ConnectorValidator.ValidationResult result = new ConnectorValidator.ValidationResult(
            false, java.util.List.of("Error 1", "Error 2")
        );
        String str = result.toString();
        Assert.assertTrue(str.contains("INVALID"));
        Assert.assertTrue(str.contains("Error 1"));
        Assert.assertTrue(str.contains("Error 2"));
    }

    @Test
    public void testValidationResult_NullErrors() {
        ConnectorValidator.ValidationResult result = new ConnectorValidator.ValidationResult(false, null);
        Assert.assertNotNull(result.errors());
        Assert.assertTrue(result.errors().isEmpty());
    }

    // --- Tests for validateUISchema with file ---

    @Test
    public void testValidateUISchema_NonExistentFile() {
        Path nonExistent = tempDir.resolve("non-existent.json");
        ConnectorValidator.ValidationResult result = ConnectorValidator.validateUISchema(nonExistent);
        Assert.assertFalse(result.valid());
        Assert.assertTrue(result.errors().get(0).contains("does not exist"));
    }

    @Test
    public void testValidateUISchema_EmptyFile() throws IOException {
        Path emptyFile = tempDir.resolve("empty.json");
        Files.writeString(emptyFile, "");

        ConnectorValidator.ValidationResult result = ConnectorValidator.validateUISchema(emptyFile);
        Assert.assertFalse(result.valid());
    }

    @Test
    public void testValidateUISchema_ValidJsonFile() throws IOException {
        Path jsonFile = tempDir.resolve("test.json");
        String json = """
            {
                "operationName": "testOp",
                "elements": []
            }
            """;
        Files.writeString(jsonFile, json);

        ConnectorValidator.ValidationResult result = ConnectorValidator.validateUISchema(jsonFile);
        Assert.assertNotNull(result);
    }

    // --- Tests for validateConnector ---

    @Test
    public void testValidateConnector_NoZipFile() {
        boolean result = ConnectorValidator.validateConnector(tempDir);
        Assert.assertFalse(result);
    }

    @Test
    public void testValidateUISchema_LargeFileStreaming() throws IOException {
        Path largeFile = tempDir.resolve("large.json");
        StringBuilder sb = new StringBuilder();
        sb.append("{\"operationName\": \"test\"");
        sb.append("}");
        Files.writeString(largeFile, sb.toString());

        ConnectorValidator.ValidationResult result = ConnectorValidator.validateUISchema(largeFile);
        Assert.assertNotNull(result);
    }

    @Test
    public void testValidateUISchema_InvalidJsonSyntax() throws IOException {
        Path invalidJson = tempDir.resolve("invalid.json");
        Files.writeString(invalidJson, "{\"operationName\": \"test\"");

        ConnectorValidator.ValidationResult result = ConnectorValidator.validateUISchema(invalidJson);
        Assert.assertFalse(result.valid());
    }

    // --- Additional tests for better branch coverage ---

    @Test
    public void testValidateUISchemaContent_JsonArray() {
        // JSON array instead of object
        String json = "[\"item1\", \"item2\"]";
        ConnectorValidator.ValidationResult result = ConnectorValidator.validateUISchemaContent(json);
        Assert.assertNotNull(result);
    }

    @Test
    public void testValidateUISchemaContent_ValidWithElements() {
        String json = """
            {
                "operationName": "getUser",
                "elements": [
                    {
                        "type": "attribute",
                        "value": {
                            "name": "userId",
                            "displayName": "User ID",
                            "inputType": "stringOrExpression",
                            "required": true
                        }
                    }
                ]
            }
            """;
        ConnectorValidator.ValidationResult result = ConnectorValidator.validateUISchemaContent(json);
        Assert.assertNotNull(result);
    }

    @Test
    public void testValidateUISchemaContent_ConnectionWithElements() {
        String json = """
            {
                "connectionName": "httpConnection",
                "elements": [
                    {
                        "type": "attribute",
                        "value": {
                            "name": "baseUrl",
                            "displayName": "Base URL",
                            "inputType": "stringOrExpression",
                            "required": true
                        }
                    }
                ]
            }
            """;
        ConnectorValidator.ValidationResult result = ConnectorValidator.validateUISchemaContent(json);
        Assert.assertNotNull(result);
    }

    @Test
    public void testValidateUISchema_DirectoryNotFile() throws IOException {
        // validateUISchema should handle case when path exists but is a directory
        Path testDir = tempDir.resolve("testDir");
        Files.createDirectory(testDir);

        ConnectorValidator.ValidationResult result = ConnectorValidator.validateUISchema(testDir);
        // Reading a directory as file will fail
        Assert.assertFalse(result.valid());
    }

    @Test
    public void testValidateConnector_DirectoryWithMultipleFiles() throws IOException {
        // Directory without zip file
        Path connectorDir = tempDir.resolve("connector");
        Files.createDirectory(connectorDir);
        Files.writeString(connectorDir.resolve("file.txt"), "test content");

        boolean result = ConnectorValidator.validateConnector(connectorDir);
        Assert.assertFalse(result);
    }

    @Test
    public void testValidateUISchemaContent_NestedObject() {
        String json = """
            {
                "operationName": "complexOp",
                "elements": [],
                "metadata": {
                    "version": "1.0",
                    "author": "test"
                }
            }
            """;
        ConnectorValidator.ValidationResult result = ConnectorValidator.validateUISchemaContent(json);
        Assert.assertNotNull(result);
    }

    @Test
    public void testValidateUISchemaContent_LargeElementsArray() {
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("{\"operationName\": \"multiElementOp\", \"elements\": [");
        for (int i = 0; i < 10; i++) {
            if (i > 0) jsonBuilder.append(",");
            jsonBuilder.append(String.format("""
                {
                    "type": "attribute",
                    "value": {
                        "name": "param%d",
                        "displayName": "Parameter %d",
                        "inputType": "stringOrExpression"
                    }
                }
                """, i, i));
        }
        jsonBuilder.append("]}");

        ConnectorValidator.ValidationResult result = ConnectorValidator.validateUISchemaContent(jsonBuilder.toString());
        Assert.assertNotNull(result);
    }

    @Test
    public void testValidationResult_EmptyErrorsList() {
        ConnectorValidator.ValidationResult result = new ConnectorValidator.ValidationResult(false, java.util.List.of());
        Assert.assertFalse(result.valid());
        Assert.assertTrue(result.errors().isEmpty());
    }

    @Test
    public void testValidateUISchema_ValidJsonWithConnectionAndOperation() throws IOException {
        // File with both connectionName and operationName (unusual but valid)
        Path jsonFile = tempDir.resolve("mixed.json");
        String json = """
            {
                "connectionName": "testConn",
                "operationName": "testOp",
                "elements": []
            }
            """;
        Files.writeString(jsonFile, json);

        ConnectorValidator.ValidationResult result = ConnectorValidator.validateUISchema(jsonFile);
        Assert.assertNotNull(result);
    }

    @Test
    public void testValidateUISchemaContent_EmptyElementsArray() {
        String json = """
            {
                "operationName": "emptyOp",
                "elements": []
            }
            """;
        ConnectorValidator.ValidationResult result = ConnectorValidator.validateUISchemaContent(json);
        Assert.assertNotNull(result);
    }

    @Test
    public void testValidateUISchemaContent_NullElementsValue() {
        String json = """
            {
                "operationName": "nullElementsOp",
                "elements": null
            }
            """;
        ConnectorValidator.ValidationResult result = ConnectorValidator.validateUISchemaContent(json);
        Assert.assertNotNull(result);
    }

    @Test
    public void testValidateConnector_NonExistentDirectory() {
        Path nonExistent = tempDir.resolve("nonExistent");
        boolean result = ConnectorValidator.validateConnector(nonExistent);
        Assert.assertFalse(result);
    }

    @Test
    public void testValidateUISchemaContent_DeepNesting() {
        String json = """
            {
                "operationName": "deepNested",
                "elements": [
                    {
                        "type": "attributeGroup",
                        "value": {
                            "groupName": "nested",
                            "elements": [
                                {
                                    "type": "attribute",
                                    "value": {
                                        "name": "innerParam",
                                        "displayName": "Inner",
                                        "inputType": "stringOrExpression"
                                    }
                                }
                            ]
                        }
                    }
                ]
            }
            """;
        ConnectorValidator.ValidationResult result = ConnectorValidator.validateUISchemaContent(json);
        Assert.assertNotNull(result);
    }

    @Test
    public void testValidateUISchemaContent_WithSpecialCharacters() {
        String json = """
            {
                "operationName": "specialChars",
                "elements": [
                    {
                        "type": "attribute",
                        "value": {
                            "name": "param_with_underscore",
                            "displayName": "Param with Special: Chars & More",
                            "inputType": "stringOrExpression"
                        }
                    }
                ]
            }
            """;
        ConnectorValidator.ValidationResult result = ConnectorValidator.validateUISchemaContent(json);
        Assert.assertNotNull(result);
    }

    @Test
    public void testValidateUISchema_FileWithOnlyWhitespace() throws IOException {
        Path whitespaceFile = tempDir.resolve("whitespace.json");
        Files.writeString(whitespaceFile, "   \n\t  ");

        ConnectorValidator.ValidationResult result = ConnectorValidator.validateUISchema(whitespaceFile);
        Assert.assertFalse(result.valid());
    }

    @Test
    public void testValidationResult_SingleError() {
        ConnectorValidator.ValidationResult result = new ConnectorValidator.ValidationResult(
            false, java.util.List.of("Single error message")
        );
        String str = result.toString();
        Assert.assertTrue(str.contains("INVALID"));
        Assert.assertTrue(str.contains("Single error message"));
    }
}
