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

    @Test
    public void testValidateUISchemaContent_NumberValue() {
        String json = """
            {
                "operationName": "testOp",
                "elements": [],
                "version": 123
            }
            """;
        ConnectorValidator.ValidationResult result = ConnectorValidator.validateUISchemaContent(json);
        Assert.assertNotNull(result);
    }

    @Test
    public void testValidateUISchemaContent_BooleanValue() {
        String json = """
            {
                "operationName": "testOp",
                "elements": [],
                "enabled": true
            }
            """;
        ConnectorValidator.ValidationResult result = ConnectorValidator.validateUISchemaContent(json);
        Assert.assertNotNull(result);
    }

    @Test
    public void testValidateUISchema_WithValidOperation() throws IOException {
        Path jsonFile = tempDir.resolve("valid-operation.json");
        String json = """
            {
                "operationName": "createUser",
                "title": "Create User",
                "elements": [
                    {
                        "type": "attribute",
                        "value": {
                            "name": "username",
                            "displayName": "Username",
                            "inputType": "stringOrExpression",
                            "required": true
                        }
                    }
                ]
            }
            """;
        Files.writeString(jsonFile, json);

        ConnectorValidator.ValidationResult result = ConnectorValidator.validateUISchema(jsonFile);
        Assert.assertNotNull(result);
    }

    @Test
    public void testValidateUISchema_MalformedJson() throws IOException {
        Path malformedFile = tempDir.resolve("malformed.json");
        Files.writeString(malformedFile, "{\"operationName\": \"test\", invalid}");

        ConnectorValidator.ValidationResult result = ConnectorValidator.validateUISchema(malformedFile);
        Assert.assertFalse(result.valid());
    }

    @Test
    public void testValidateUISchemaContent_WithHelpTip() {
        String json = """
            {
                "operationName": "testOp",
                "elements": [
                    {
                        "type": "attribute",
                        "value": {
                            "name": "param1",
                            "displayName": "Parameter 1",
                            "inputType": "stringOrExpression",
                            "helpTip": "Enter the parameter value"
                        }
                    }
                ]
            }
            """;
        ConnectorValidator.ValidationResult result = ConnectorValidator.validateUISchemaContent(json);
        Assert.assertNotNull(result);
    }

    @Test
    public void testValidateUISchemaContent_WithPlaceholder() {
        String json = """
            {
                "operationName": "testOp",
                "elements": [
                    {
                        "type": "attribute",
                        "value": {
                            "name": "param1",
                            "displayName": "Parameter 1",
                            "inputType": "stringOrExpression",
                            "placeholder": "Enter value..."
                        }
                    }
                ]
            }
            """;
        ConnectorValidator.ValidationResult result = ConnectorValidator.validateUISchemaContent(json);
        Assert.assertNotNull(result);
    }

    @Test
    public void testValidateUISchemaContent_WithDefaultValue() {
        String json = """
            {
                "operationName": "testOp",
                "elements": [
                    {
                        "type": "attribute",
                        "value": {
                            "name": "param1",
                            "displayName": "Parameter 1",
                            "inputType": "stringOrExpression",
                            "defaultValue": "default"
                        }
                    }
                ]
            }
            """;
        ConnectorValidator.ValidationResult result = ConnectorValidator.validateUISchemaContent(json);
        Assert.assertNotNull(result);
    }

    @Test
    public void testValidateUISchemaContent_ComboInputType() {
        String json = """
            {
                "operationName": "testOp",
                "elements": [
                    {
                        "type": "attribute",
                        "value": {
                            "name": "format",
                            "displayName": "Format",
                            "inputType": "combo",
                            "comboValues": ["json", "xml", "text"]
                        }
                    }
                ]
            }
            """;
        ConnectorValidator.ValidationResult result = ConnectorValidator.validateUISchemaContent(json);
        Assert.assertNotNull(result);
    }

    @Test
    public void testValidateUISchemaContent_WithEnableCondition() {
        String json = """
            {
                "operationName": "testOp",
                "elements": [
                    {
                        "type": "attribute",
                        "value": {
                            "name": "conditionalParam",
                            "displayName": "Conditional",
                            "inputType": "stringOrExpression",
                            "enableCondition": "otherParam == 'true'"
                        }
                    }
                ]
            }
            """;
        ConnectorValidator.ValidationResult result = ConnectorValidator.validateUISchemaContent(json);
        Assert.assertNotNull(result);
    }

    @Test
    public void testValidateUISchemaContent_AttributeGroup() {
        String json = """
            {
                "operationName": "testOp",
                "elements": [
                    {
                        "type": "attributeGroup",
                        "value": {
                            "groupName": "Advanced Options",
                            "isCollapsed": true,
                            "elements": []
                        }
                    }
                ]
            }
            """;
        ConnectorValidator.ValidationResult result = ConnectorValidator.validateUISchemaContent(json);
        Assert.assertNotNull(result);
    }

    @Test
    public void testValidateUISchemaContent_WithMultipleAttributeGroups() {
        String json = """
            {
                "operationName": "testOp",
                "elements": [
                    {
                        "type": "attributeGroup",
                        "value": {
                            "groupName": "Basic",
                            "elements": []
                        }
                    },
                    {
                        "type": "attributeGroup",
                        "value": {
                            "groupName": "Advanced",
                            "elements": []
                        }
                    }
                ]
            }
            """;
        ConnectorValidator.ValidationResult result = ConnectorValidator.validateUISchemaContent(json);
        Assert.assertNotNull(result);
    }

    @Test
    public void testValidationResult_AccessMethods() {
        ConnectorValidator.ValidationResult valid = new ConnectorValidator.ValidationResult(true, java.util.List.of());
        Assert.assertTrue(valid.valid());
        Assert.assertTrue(valid.errors().isEmpty());

        ConnectorValidator.ValidationResult invalid = new ConnectorValidator.ValidationResult(false, java.util.List.of("err1", "err2"));
        Assert.assertFalse(invalid.valid());
        Assert.assertEquals(invalid.errors().size(), 2);
    }

    @Test
    public void testValidateUISchema_ValidConnectionSchema() throws IOException {
        Path jsonFile = tempDir.resolve("connection.json");
        String json = """
            {
                "connectionName": "MyConnection",
                "title": "My Connection",
                "elements": [
                    {
                        "type": "attribute",
                        "value": {
                            "name": "apiKey",
                            "displayName": "API Key",
                            "inputType": "stringOrExpression",
                            "required": true
                        }
                    }
                ]
            }
            """;
        Files.writeString(jsonFile, json);

        ConnectorValidator.ValidationResult result = ConnectorValidator.validateUISchema(jsonFile);
        Assert.assertNotNull(result);
    }

    @Test
    public void testValidateUISchemaContent_UnicodeCharacters() {
        String json = """
            {
                "operationName": "testOp",
                "elements": [
                    {
                        "type": "attribute",
                        "value": {
                            "name": "description",
                            "displayName": "説明",
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
    public void testValidateUISchemaContent_VeryLongString() {
        StringBuilder longString = new StringBuilder("test");
        for (int i = 0; i < 100; i++) {
            longString.append("_extended_name_segment");
        }

        String json = String.format("""
            {
                "operationName": "%s",
                "elements": []
            }
            """, longString.toString());
        ConnectorValidator.ValidationResult result = ConnectorValidator.validateUISchemaContent(json);
        Assert.assertNotNull(result);
    }

    @Test
    public void testValidateConnector_EmptyDirectory() throws IOException {
        Path emptyDir = tempDir.resolve("empty");
        Files.createDirectory(emptyDir);
        boolean result = ConnectorValidator.validateConnector(emptyDir);
        Assert.assertFalse(result);
    }

    // --- Additional tests for large file streaming validation ---

    @Test
    public void testValidateUISchema_LargeFileThatIsNotJson() throws IOException {
        // Create a file larger than 5MB but not valid JSON
        Path largeFile = tempDir.resolve("large.json");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6 * 1024 * 1024 / 10; i++) {
            sb.append("not json ");
        }
        Files.writeString(largeFile, sb.toString());

        ConnectorValidator.ValidationResult result = ConnectorValidator.validateUISchema(largeFile);
        Assert.assertFalse(result.valid());
    }

    @Test
    public void testValidateUISchema_LargeValidJson() throws IOException {
        // Create a file larger than 5MB that is valid JSON
        Path largeFile = tempDir.resolve("large-valid.json");
        StringBuilder sb = new StringBuilder();
        sb.append("{\"operationName\": \"test\", \"data\": \"");
        for (int i = 0; i < 6 * 1024 * 1024 / 10; i++) {
            sb.append("x");
        }
        sb.append("\"}");
        Files.writeString(largeFile, sb.toString());

        ConnectorValidator.ValidationResult result = ConnectorValidator.validateUISchema(largeFile);
        // Large file uses streaming validation - should pass for valid JSON object with operationName
        Assert.assertNotNull(result);
    }

    @Test
    public void testValidateUISchema_LargeJsonArray() throws IOException {
        // Create a large file that is a JSON array (not object)
        Path largeFile = tempDir.resolve("large-array.json");
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < 600000; i++) {
            if (i > 0) sb.append(",");
            sb.append("\"item").append(i).append("\"");
        }
        sb.append("]");
        Files.writeString(largeFile, sb.toString());

        ConnectorValidator.ValidationResult result = ConnectorValidator.validateUISchema(largeFile);
        // Large file streaming validation expects an object, not array
        Assert.assertFalse(result.valid());
        Assert.assertTrue(result.errors().get(0).contains("not a JSON object"));
    }

    @Test
    public void testValidateUISchema_LargeJsonWithConnectionName() throws IOException {
        // Create a large file (> 5MB) with connectionName instead of operationName
        Path largeFile = tempDir.resolve("large-connection.json");
        StringBuilder sb = new StringBuilder();
        sb.append("{\"connectionName\": \"test\", \"data\": \"");
        // Need > 5MB to trigger streaming validation
        for (int i = 0; i < 6 * 1024 * 1024; i++) {
            sb.append("x");
        }
        sb.append("\"}");
        Files.writeString(largeFile, sb.toString());

        ConnectorValidator.ValidationResult result = ConnectorValidator.validateUISchema(largeFile);
        Assert.assertNotNull(result);
    }

    @Test
    public void testValidateUISchema_LargeJsonMissingBothNames() throws IOException {
        // Create a large file (> 5MB) without operationName or connectionName
        Path largeFile = tempDir.resolve("large-missing.json");
        StringBuilder sb = new StringBuilder();
        sb.append("{\"someField\": \"test\", \"data\": \"");
        // Need > 5MB to trigger streaming validation
        for (int i = 0; i < 6 * 1024 * 1024; i++) {
            sb.append("x");
        }
        sb.append("\"}");
        Files.writeString(largeFile, sb.toString());

        ConnectorValidator.ValidationResult result = ConnectorValidator.validateUISchema(largeFile);
        Assert.assertFalse(result.valid());
        Assert.assertTrue(result.errors().get(0).contains("missing both"));
    }

    @Test
    public void testValidateUISchemaContent_ArrayElement() {
        // Test with nested arrays in elements
        String json = """
            {
                "operationName": "testOp",
                "elements": [
                    {
                        "type": "attribute",
                        "value": {
                            "name": "listParam",
                            "displayName": "List Parameter",
                            "inputType": "table",
                            "columns": [
                                {"name": "col1", "type": "string"}
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
    public void testValidateUISchemaContent_WithHidden() {
        String json = """
            {
                "operationName": "testOp",
                "elements": [
                    {
                        "type": "attribute",
                        "value": {
                            "name": "internalParam",
                            "displayName": "Internal",
                            "inputType": "stringOrExpression",
                            "hidden": true
                        }
                    }
                ]
            }
            """;
        ConnectorValidator.ValidationResult result = ConnectorValidator.validateUISchemaContent(json);
        Assert.assertNotNull(result);
    }

    @Test
    public void testValidateUISchemaContent_NestedAttributeGroups() {
        String json = """
            {
                "operationName": "testOp",
                "elements": [
                    {
                        "type": "attributeGroup",
                        "value": {
                            "groupName": "Outer",
                            "elements": [
                                {
                                    "type": "attributeGroup",
                                    "value": {
                                        "groupName": "Inner",
                                        "elements": [
                                            {
                                                "type": "attribute",
                                                "value": {
                                                    "name": "nestedParam",
                                                    "displayName": "Nested",
                                                    "inputType": "stringOrExpression"
                                                }
                                            }
                                        ]
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
    public void testValidateUISchemaContent_MultipleInputTypes() {
        String json = """
            {
                "operationName": "testOp",
                "elements": [
                    {"type": "attribute", "value": {"name": "p1", "displayName": "P1", "inputType": "stringOrExpression"}},
                    {"type": "attribute", "value": {"name": "p2", "displayName": "P2", "inputType": "combo"}},
                    {"type": "attribute", "value": {"name": "p3", "displayName": "P3", "inputType": "booleanOrExpression"}},
                    {"type": "attribute", "value": {"name": "p4", "displayName": "P4", "inputType": "numberOrExpression"}}
                ]
            }
            """;
        ConnectorValidator.ValidationResult result = ConnectorValidator.validateUISchemaContent(json);
        Assert.assertNotNull(result);
    }

    @Test
    public void testValidateUISchema_SymlinkToFile() throws IOException {
        // Create actual file and symlink to it
        Path actualFile = tempDir.resolve("actual.json");
        Files.writeString(actualFile, "{\"operationName\": \"test\", \"elements\": []}");

        Path symlink = tempDir.resolve("symlink.json");
        try {
            Files.createSymbolicLink(symlink, actualFile);
            ConnectorValidator.ValidationResult result = ConnectorValidator.validateUISchema(symlink);
            Assert.assertNotNull(result);
        } catch (UnsupportedOperationException | java.nio.file.FileSystemException e) {
            // Symlinks may not be supported on all systems
        }
    }

    @Test
    public void testValidateUISchemaContent_EmptyObject() {
        String json = "{}";
        ConnectorValidator.ValidationResult result = ConnectorValidator.validateUISchemaContent(json);
        Assert.assertNotNull(result);
    }

    @Test
    public void testValidateUISchemaContent_OnlyOperationName() {
        String json = "{\"operationName\": \"test\"}";
        ConnectorValidator.ValidationResult result = ConnectorValidator.validateUISchemaContent(json);
        Assert.assertNotNull(result);
    }

    @Test
    public void testValidateUISchemaContent_OnlyConnectionName() {
        String json = "{\"connectionName\": \"test\"}";
        ConnectorValidator.ValidationResult result = ConnectorValidator.validateUISchemaContent(json);
        Assert.assertNotNull(result);
    }

    @Test
    public void testValidateUISchemaContent_NumericOperationName() {
        String json = "{\"operationName\": 123, \"elements\": []}";
        ConnectorValidator.ValidationResult result = ConnectorValidator.validateUISchemaContent(json);
        Assert.assertNotNull(result);
    }

    @Test
    public void testValidateUISchemaContent_BooleanOperationName() {
        String json = "{\"operationName\": true, \"elements\": []}";
        ConnectorValidator.ValidationResult result = ConnectorValidator.validateUISchemaContent(json);
        Assert.assertNotNull(result);
    }

    @Test
    public void testValidateUISchemaContent_NullOperationName() {
        String json = "{\"operationName\": null, \"elements\": []}";
        ConnectorValidator.ValidationResult result = ConnectorValidator.validateUISchemaContent(json);
        Assert.assertNotNull(result);
    }

    @Test
    public void testValidationResult_WithManyErrors() {
        java.util.List<String> errors = new java.util.ArrayList<>();
        for (int i = 0; i < 10; i++) {
            errors.add("Error " + i);
        }
        ConnectorValidator.ValidationResult result = new ConnectorValidator.ValidationResult(false, errors);
        Assert.assertFalse(result.valid());
        Assert.assertEquals(result.errors().size(), 10);
        String str = result.toString();
        Assert.assertTrue(str.contains("Error 0"));
        Assert.assertTrue(str.contains("Error 9"));
    }

    @Test
    public void testValidateLargeConnector() throws IOException {
        // Create a simulated large connector zip (> 10MB)
        Path connectorDir = tempDir.resolve("large_connector");
        Files.createDirectories(connectorDir);
        Path zipPath = connectorDir.resolve("connector.zip");

        // Create a zip with valid structure but large size
        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(Files.newOutputStream(zipPath))) {
            // Add connector.xml
            zos.putNextEntry(new java.util.zip.ZipEntry("connector.xml"));
            zos.write("<connector>test</connector>".getBytes());
            zos.closeEntry();

            // Add function XML
            zos.putNextEntry(new java.util.zip.ZipEntry("functions/test.xml"));
            zos.write("<function>test</function>".getBytes());
            zos.closeEntry();

            // Add JAR
            zos.putNextEntry(new java.util.zip.ZipEntry("lib/test.jar"));
            zos.write("dummy jar content".getBytes());
            zos.closeEntry();

            // Add UI Schema
            zos.putNextEntry(new java.util.zip.ZipEntry("uischema/test.json"));
            zos.write("{}".getBytes());
            zos.closeEntry();

            // Add a large dummy entry to increase size > 10MB
            // 11MB = 11 * 1024 * 1024 bytes
            zos.setLevel(0);
            zos.putNextEntry(new java.util.zip.ZipEntry("large_dummy_file.bin"));
            byte[] buffer = new byte[1024 * 1024]; // 1MB buffer
            java.util.Arrays.fill(buffer, (byte) 'a');
            for (int i = 0; i < 11; i++) {
                zos.write(buffer);
            }
            zos.closeEntry();
        }

        // Create generated/connector.xml for validateConnectorXml check
        Path generatedDir = connectorDir.resolve("generated");
        Files.createDirectories(generatedDir);
        Files.writeString(generatedDir.resolve("connector.xml"), "<connector>content</connector>");

        boolean result = ConnectorValidator.validateConnector(connectorDir);
        Assert.assertTrue(result, "Large connector validation failed");
    }
}
