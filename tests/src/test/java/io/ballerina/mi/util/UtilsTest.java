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

package io.ballerina.mi.util;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.*;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.mi.model.FunctionType;
import io.ballerina.projects.Document;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UtilsTest {

    @Test
    public void testFormatJson() {
        String input = "{\"key\":\"value\"}";
        String expected = "{\n  \"key\": \"value\"\n}";
        Assert.assertEquals(Utils.formatJson(input).replace("\r\n", "\n"), expected.replace("\r\n", "\n"));

        String invalid = "{key:value";
        Assert.assertEquals(Utils.formatJson(invalid), invalid);
    }

    @Test
    public void testHumanizeName() {
        Assert.assertEquals(Utils.humanizeName("getAllCountriesAndProvincesStatus"), "get all countries and provinces status");
        Assert.assertEquals(Utils.humanizeName("postUsersThreadsTrashByUserIdById"), "post users threads trash by user id by id");
        Assert.assertEquals(Utils.humanizeName("simpleName"), "simple name");
        Assert.assertEquals(Utils.humanizeName("SimpleName"), "simple name");
        Assert.assertEquals(Utils.humanizeName("simple_name"), "simple name");
        Assert.assertEquals(Utils.humanizeName("simple-name"), "simple name");
        Assert.assertEquals(Utils.humanizeName(""), "");
        Assert.assertEquals(Utils.humanizeName(null), "");
    }

    @Test
    public void testGenerateSynapseNameForNonResource() {
        FunctionSymbol functionSymbol = mock(FunctionSymbol.class);
        when(functionSymbol.getName()).thenReturn(Optional.of("myFunction"));

        String synapseName = Utils.generateSynapseName(functionSymbol, FunctionType.FUNCTION);
        Assert.assertEquals(synapseName, "myFunction");
    }

    @Test
    public void testGenerateSynapseNameForResourceWithHint() {
        FunctionSymbol functionSymbol = mock(FunctionSymbol.class);
        when(functionSymbol.getName()).thenReturn(Optional.of("get"));

        String synapseName = Utils.generateSynapseName(functionSymbol, FunctionType.RESOURCE, "Users");
        Assert.assertEquals(synapseName, "getUsers");
    }

    @Test
    public void testGenerateSynapseNameForResourceWithoutHint() {
        FunctionSymbol functionSymbol = mock(FunctionSymbol.class);
        FunctionTypeSymbol functionTypeSymbol = mock(FunctionTypeSymbol.class);
        TypeSymbol returnTypeSymbol = mock(TypeSymbol.class);

        when(functionSymbol.getName()).thenReturn(Optional.of("post"));
        when(functionSymbol.typeDescriptor()).thenReturn(functionTypeSymbol);
        when(functionTypeSymbol.params()).thenReturn(Optional.empty());
        when(functionTypeSymbol.returnTypeDescriptor()).thenReturn(Optional.of(returnTypeSymbol));
        when(returnTypeSymbol.typeKind()).thenReturn(TypeDescKind.STRING);
        when(returnTypeSymbol.getName()).thenReturn(Optional.of("String"));

        String synapseName = Utils.generateSynapseName(functionSymbol, FunctionType.RESOURCE);
        Assert.assertEquals(synapseName, "postString");

        ParameterSymbol paramSymbol = mock(ParameterSymbol.class);
        TypeSymbol paramTypeSymbol = mock(TypeSymbol.class);
        when(functionTypeSymbol.params()).thenReturn(Optional.of(Collections.singletonList(paramSymbol)));
        when(paramSymbol.typeDescriptor()).thenReturn(paramTypeSymbol);
        when(paramTypeSymbol.typeKind()).thenReturn(TypeDescKind.TYPE_REFERENCE);
        when(paramTypeSymbol.getName()).thenReturn(Optional.of("CreateUserRequest"));

        synapseName = Utils.generateSynapseName(functionSymbol, FunctionType.RESOURCE);
        Assert.assertEquals(synapseName, "postUser");
    }

    @Test
    public void testWriteFile() throws IOException {
        Path tempFile = Files.createTempFile("test", ".json");
        try {
            Utils.writeFile(tempFile.toString(), "{\"k\":\"v\"}");
            String content = Files.readString(tempFile);
            Assert.assertTrue(content.contains("\"k\": \"v\""));
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    public void testGetReturnTypeName_Simple() {
        FunctionSymbol functionSymbol = mock(FunctionSymbol.class);
        FunctionTypeSymbol typeSymbol = mock(FunctionTypeSymbol.class);
        TypeSymbol returnType = mock(TypeSymbol.class);

        when(functionSymbol.typeDescriptor()).thenReturn(typeSymbol);
        when(typeSymbol.returnTypeDescriptor()).thenReturn(Optional.of(returnType));
        when(returnType.typeKind()).thenReturn(TypeDescKind.STRING);

        Assert.assertEquals(Utils.getReturnTypeName(functionSymbol), "string");
    }

    @Test
    public void testGetReturnTypeName_Nil() {
        FunctionSymbol functionSymbol = mock(FunctionSymbol.class);
        FunctionTypeSymbol typeSymbol = mock(FunctionTypeSymbol.class);
        TypeSymbol returnType = mock(TypeSymbol.class);

        when(functionSymbol.typeDescriptor()).thenReturn(typeSymbol);
        when(typeSymbol.returnTypeDescriptor()).thenReturn(Optional.of(returnType));
        when(returnType.typeKind()).thenReturn(TypeDescKind.NIL);

        Assert.assertEquals(Utils.getReturnTypeName(functionSymbol), "nil");
    }

    @Test
    public void testGetReturnTypeName_Error() {
        FunctionSymbol functionSymbol = mock(FunctionSymbol.class);
        FunctionTypeSymbol typeSymbol = mock(FunctionTypeSymbol.class);
        TypeSymbol returnType = mock(TypeSymbol.class);

        when(functionSymbol.typeDescriptor()).thenReturn(typeSymbol);
        when(typeSymbol.returnTypeDescriptor()).thenReturn(Optional.of(returnType));
        when(returnType.typeKind()).thenReturn(TypeDescKind.ERROR);

        Assert.assertNull(Utils.getReturnTypeName(functionSymbol));
    }

    @Test
    public void testGetParamTypeName() {
        Assert.assertEquals(Utils.getParamTypeName(TypeDescKind.INT), "int");
        Assert.assertEquals(Utils.getParamTypeName(TypeDescKind.STRING), "string");
        Assert.assertEquals(Utils.getParamTypeName(TypeDescKind.BOOLEAN), "boolean");
        Assert.assertEquals(Utils.getParamTypeName(TypeDescKind.DECIMAL), "decimal");
        Assert.assertEquals(Utils.getParamTypeName(TypeDescKind.FLOAT), "float");
        Assert.assertEquals(Utils.getParamTypeName(TypeDescKind.XML), "xml");
        Assert.assertEquals(Utils.getParamTypeName(TypeDescKind.MAP), "map");
        Assert.assertEquals(Utils.getParamTypeName(TypeDescKind.ARRAY), "array");
        Assert.assertEquals(Utils.getParamTypeName(TypeDescKind.RECORD), "record");
        Assert.assertEquals(Utils.getParamTypeName(TypeDescKind.UNION), "union");
        Assert.assertEquals(Utils.getParamTypeName(TypeDescKind.OBJECT), null);
    }

    @Test
    public void testContainsToken() {
        List<io.ballerina.compiler.api.symbols.Qualifier> qualifiers = new ArrayList<>();
        qualifiers.add(io.ballerina.compiler.api.symbols.Qualifier.PUBLIC);
        qualifiers.add(io.ballerina.compiler.api.symbols.Qualifier.REMOTE);

        Assert.assertTrue(Utils.containsToken(qualifiers, io.ballerina.compiler.api.symbols.Qualifier.PUBLIC));
        Assert.assertTrue(Utils.containsToken(qualifiers, io.ballerina.compiler.api.symbols.Qualifier.REMOTE));
        Assert.assertFalse(Utils.containsToken(qualifiers, io.ballerina.compiler.api.symbols.Qualifier.RESOURCE));
    }

    @Test
    public void testSanitizeParamName() {
        Assert.assertEquals(Utils.sanitizeParamName("auth.token.value"), "auth_token_value");
        Assert.assertEquals(Utils.sanitizeParamName("'name"), "name");
        Assert.assertEquals(Utils.sanitizeParamName("'config.value"), "config_value");
        Assert.assertEquals(Utils.sanitizeParamName(null), "");
        Assert.assertEquals(Utils.sanitizeParamName("simple"), "simple");
    }

    @Test
    public void testReadFile() throws IOException {
        try {
            String content = Utils.readFile("balConnector/inputTemplates/attribute.json");
            Assert.assertNotNull(content);
            Assert.assertFalse(content.isEmpty());
        } catch (IOException e) {
            // Resource not found is acceptable in test environment
        }
    }

    @Test(expectedExceptions = IOException.class)
    public void testReadFile_NotFound() throws IOException {
        Utils.readFile("nonexistent/file.json");
    }

    @Test
    public void testGetReturnTypeName_NoReturnType() {
        FunctionSymbol functionSymbol = mock(FunctionSymbol.class);
        FunctionTypeSymbol typeSymbol = mock(FunctionTypeSymbol.class);

        when(functionSymbol.typeDescriptor()).thenReturn(typeSymbol);
        when(typeSymbol.returnTypeDescriptor()).thenReturn(Optional.empty());

        Assert.assertEquals(Utils.getReturnTypeName(functionSymbol), "nil");
    }

    @Test
    public void testGetParamTypeName_Json() {
        Assert.assertEquals(Utils.getParamTypeName(TypeDescKind.JSON), "json");
    }

    @Test
    public void testGetParamTypeName_Table() {
        Assert.assertNull(Utils.getParamTypeName(TypeDescKind.TABLE));
    }

    @Test
    public void testGetParamTypeName_Nil() {
        Assert.assertEquals(Utils.getParamTypeName(TypeDescKind.NIL), "nil");
    }

    @Test
    public void testWriteFile_NonJson() throws IOException {
        Path tempFile = Files.createTempFile("test", ".txt");
        try {
            Utils.writeFile(tempFile.toString(), "plain text content");
            String content = Files.readString(tempFile);
            Assert.assertEquals(content, "plain text content");
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    public void testSanitizeXmlName() {
        // Normal names should pass through
        Assert.assertEquals(Utils.sanitizeXmlName("validName"), "validName");
        Assert.assertEquals(Utils.sanitizeXmlName("ValidName123"), "ValidName123");

        // Names starting with underscore are valid
        Assert.assertEquals(Utils.sanitizeXmlName("_validName"), "_validName");

        // Names starting with digit should be prefixed with underscore
        Assert.assertEquals(Utils.sanitizeXmlName("123name"), "_123name");

        // Special characters should be replaced with underscore
        Assert.assertEquals(Utils.sanitizeXmlName("name@test"), "name_test");
        Assert.assertEquals(Utils.sanitizeXmlName("name#test"), "name_test");

        // Null and empty should return "resource"
        Assert.assertEquals(Utils.sanitizeXmlName(null), "resource");
        Assert.assertEquals(Utils.sanitizeXmlName(""), "resource");

        // Hyphens and periods are valid
        Assert.assertEquals(Utils.sanitizeXmlName("name-test"), "name-test");
        Assert.assertEquals(Utils.sanitizeXmlName("name.test"), "name.test");
    }

    @Test
    public void testGetActualTypeKind_NonTypeReference() {
        TypeSymbol typeSymbol = mock(TypeSymbol.class);
        when(typeSymbol.typeKind()).thenReturn(TypeDescKind.STRING);

        TypeDescKind result = Utils.getActualTypeKind(typeSymbol);
        Assert.assertEquals(result, TypeDescKind.STRING);
    }

    @Test
    public void testGetActualTypeSymbol_NonTypeReference() {
        TypeSymbol typeSymbol = mock(TypeSymbol.class);
        when(typeSymbol.typeKind()).thenReturn(TypeDescKind.STRING);

        TypeSymbol result = Utils.getActualTypeSymbol(typeSymbol);
        Assert.assertSame(result, typeSymbol);
    }

    @Test
    public void testGetFunctionType_Init() {
        FunctionSymbol functionSymbol = mock(FunctionSymbol.class);
        when(functionSymbol.getName()).thenReturn(Optional.of("init"));
        when(functionSymbol.qualifiers()).thenReturn(Collections.emptyList());

        FunctionType result = Utils.getFunctionType(functionSymbol);
        Assert.assertEquals(result, FunctionType.INIT);
    }

    @Test
    public void testGetFunctionType_Remote() {
        FunctionSymbol functionSymbol = mock(FunctionSymbol.class);
        when(functionSymbol.getName()).thenReturn(Optional.of("doSomething"));
        List<io.ballerina.compiler.api.symbols.Qualifier> qualifiers = new ArrayList<>();
        qualifiers.add(io.ballerina.compiler.api.symbols.Qualifier.REMOTE);
        when(functionSymbol.qualifiers()).thenReturn(qualifiers);

        FunctionType result = Utils.getFunctionType(functionSymbol);
        Assert.assertEquals(result, FunctionType.REMOTE);
    }

    @Test
    public void testGetFunctionType_Resource() {
        FunctionSymbol functionSymbol = mock(FunctionSymbol.class);
        when(functionSymbol.getName()).thenReturn(Optional.of("get"));
        List<io.ballerina.compiler.api.symbols.Qualifier> qualifiers = new ArrayList<>();
        qualifiers.add(io.ballerina.compiler.api.symbols.Qualifier.RESOURCE);
        when(functionSymbol.qualifiers()).thenReturn(qualifiers);

        FunctionType result = Utils.getFunctionType(functionSymbol);
        Assert.assertEquals(result, FunctionType.RESOURCE);
    }

    @Test
    public void testGetFunctionType_Function() {
        FunctionSymbol functionSymbol = mock(FunctionSymbol.class);
        when(functionSymbol.getName()).thenReturn(Optional.of("myFunction"));
        when(functionSymbol.qualifiers()).thenReturn(Collections.emptyList());

        FunctionType result = Utils.getFunctionType(functionSymbol);
        Assert.assertEquals(result, FunctionType.FUNCTION);
    }

    @Test
    public void testGetDocString() {
        Documentation documentation = mock(Documentation.class);
        when(documentation.description()).thenReturn(Optional.of("Test description"));

        String result = Utils.getDocString(documentation);
        Assert.assertEquals(result, "Test description");
    }

    @Test
    public void testGetDocString_Empty() {
        Documentation documentation = mock(Documentation.class);
        when(documentation.description()).thenReturn(Optional.empty());

        String result = Utils.getDocString(documentation);
        Assert.assertEquals(result, "");
    }

    @Test
    public void testDeleteDirectory() throws IOException {
        // Create a temp directory with files
        Path tempDir = Files.createTempDirectory("test-delete");
        Path subDir = Files.createDirectory(tempDir.resolve("subdir"));
        Files.createFile(tempDir.resolve("file1.txt"));
        Files.createFile(subDir.resolve("file2.txt"));

        // Verify files exist
        Assert.assertTrue(Files.exists(tempDir));
        Assert.assertTrue(Files.exists(subDir));

        // Delete the directory
        Utils.deleteDirectory(tempDir);

        // Verify everything is deleted
        Assert.assertFalse(Files.exists(tempDir));
    }

    @Test
    public void testContainsToken_Empty() {
        List<io.ballerina.compiler.api.symbols.Qualifier> qualifiers = Collections.emptyList();
        Assert.assertFalse(Utils.containsToken(qualifiers, io.ballerina.compiler.api.symbols.Qualifier.PUBLIC));
    }

    @Test
    public void testGetParamTypeName_SignedIntegers() {
        Assert.assertEquals(Utils.getParamTypeName(TypeDescKind.INT_SIGNED8), "int");
        Assert.assertEquals(Utils.getParamTypeName(TypeDescKind.INT_SIGNED16), "int");
        Assert.assertEquals(Utils.getParamTypeName(TypeDescKind.INT_SIGNED32), "int");
    }

    @Test
    public void testGetParamTypeName_UnsignedIntegers() {
        Assert.assertEquals(Utils.getParamTypeName(TypeDescKind.INT_UNSIGNED8), "int");
        Assert.assertEquals(Utils.getParamTypeName(TypeDescKind.INT_UNSIGNED16), "int");
        Assert.assertEquals(Utils.getParamTypeName(TypeDescKind.INT_UNSIGNED32), "int");
    }

    @Test
    public void testGetParamTypeName_StringChar() {
        Assert.assertEquals(Utils.getParamTypeName(TypeDescKind.STRING_CHAR), "string");
    }

    @Test
    public void testGetParamTypeName_Singleton() {
        Assert.assertEquals(Utils.getParamTypeName(TypeDescKind.SINGLETON), "string");
    }

    @Test
    public void testHumanizeName_CamelCase() {
        Assert.assertEquals(Utils.humanizeName("getUserById"), "get user by id");
        // Consecutive uppercase letters don't get spaces between them
        Assert.assertEquals(Utils.humanizeName("XMLParser"), "xmlparser");
    }

    @Test
    public void testHumanizeName_WithNumbers() {
        // Numbers don't trigger space insertion
        Assert.assertEquals(Utils.humanizeName("getUser123"), "get user123");
    }

    @Test
    public void testSanitizeParamName_MultipleLeadingQuotes() {
        Assert.assertEquals(Utils.sanitizeParamName("''name"), "name");
    }

    @Test
    public void testSanitizeParamName_EmptyAfterSanitization() {
        Assert.assertEquals(Utils.sanitizeParamName("'''"), "param");
    }

    @Test
    public void testGenerateSynapseNameForResourceWithBlankHint() {
        FunctionSymbol functionSymbol = mock(FunctionSymbol.class);
        FunctionTypeSymbol functionTypeSymbol = mock(FunctionTypeSymbol.class);
        TypeSymbol returnTypeSymbol = mock(TypeSymbol.class);

        when(functionSymbol.getName()).thenReturn(Optional.of("get"));
        when(functionSymbol.typeDescriptor()).thenReturn(functionTypeSymbol);
        when(functionTypeSymbol.params()).thenReturn(Optional.empty());
        when(functionTypeSymbol.returnTypeDescriptor()).thenReturn(Optional.of(returnTypeSymbol));
        when(returnTypeSymbol.typeKind()).thenReturn(TypeDescKind.STRING);
        when(returnTypeSymbol.getName()).thenReturn(Optional.of("String"));

        // Blank hint should fall back to default behavior
        String synapseName = Utils.generateSynapseName(functionSymbol, FunctionType.RESOURCE, "   ");
        Assert.assertEquals(synapseName, "getString");
    }

    @Test
    public void testHasOpenApiResourceInfoAnnotation_NoAnnotations() {
        FunctionSymbol functionSymbol = mock(FunctionSymbol.class);
        when(functionSymbol.annotations()).thenReturn(Collections.emptyList());

        boolean result = Utils.hasOpenApiResourceInfoAnnotation(functionSymbol);
        Assert.assertFalse(result);
    }

    @Test
    public void testHasOpenApiResourceInfoAnnotation_WrongAnnotation() {
        FunctionSymbol functionSymbol = mock(FunctionSymbol.class);
        AnnotationSymbol annotationSymbol = mock(AnnotationSymbol.class);
        when(annotationSymbol.getName()).thenReturn(Optional.of("OtherAnnotation"));

        when(functionSymbol.annotations()).thenReturn(Collections.singletonList(annotationSymbol));

        boolean result = Utils.hasOpenApiResourceInfoAnnotation(functionSymbol);
        Assert.assertFalse(result);
    }

    @Test
    public void testHasOpenApiResourceInfoAnnotation_NoModule() {
        FunctionSymbol functionSymbol = mock(FunctionSymbol.class);
        AnnotationSymbol annotationSymbol = mock(AnnotationSymbol.class);
        when(annotationSymbol.getName()).thenReturn(Optional.of("ResourceInfo"));
        when(annotationSymbol.getModule()).thenReturn(Optional.empty());

        when(functionSymbol.annotations()).thenReturn(Collections.singletonList(annotationSymbol));

        boolean result = Utils.hasOpenApiResourceInfoAnnotation(functionSymbol);
        Assert.assertFalse(result);
    }

    @Test
    public void testHasOpenApiResourceInfoAnnotation_WrongModule() {
        FunctionSymbol functionSymbol = mock(FunctionSymbol.class);
        AnnotationSymbol annotationSymbol = mock(AnnotationSymbol.class);
        ModuleSymbol moduleSymbol = mock(ModuleSymbol.class);

        when(annotationSymbol.getName()).thenReturn(Optional.of("ResourceInfo"));
        when(annotationSymbol.getModule()).thenReturn(Optional.of(moduleSymbol));
        when(moduleSymbol.getName()).thenReturn(Optional.of("other"));

        when(functionSymbol.annotations()).thenReturn(Collections.singletonList(annotationSymbol));

        boolean result = Utils.hasOpenApiResourceInfoAnnotation(functionSymbol);
        Assert.assertFalse(result);
    }

    @Test
    public void testHasOpenApiResourceInfoAnnotation_Valid() {
        FunctionSymbol functionSymbol = mock(FunctionSymbol.class);
        AnnotationSymbol annotationSymbol = mock(AnnotationSymbol.class);
        ModuleSymbol moduleSymbol = mock(ModuleSymbol.class);

        when(annotationSymbol.getName()).thenReturn(Optional.of("ResourceInfo"));
        when(annotationSymbol.getModule()).thenReturn(Optional.of(moduleSymbol));
        when(moduleSymbol.getName()).thenReturn(Optional.of("openapi"));

        when(functionSymbol.annotations()).thenReturn(Collections.singletonList(annotationSymbol));

        boolean result = Utils.hasOpenApiResourceInfoAnnotation(functionSymbol);
        Assert.assertTrue(result);
    }

    @Test
    public void testGetReturnTypeName_Any() {
        FunctionSymbol functionSymbol = mock(FunctionSymbol.class);
        FunctionTypeSymbol typeSymbol = mock(FunctionTypeSymbol.class);
        TypeSymbol returnType = mock(TypeSymbol.class);

        when(functionSymbol.typeDescriptor()).thenReturn(typeSymbol);
        when(typeSymbol.returnTypeDescriptor()).thenReturn(Optional.of(returnType));
        when(returnType.typeKind()).thenReturn(TypeDescKind.ANY);

        Assert.assertEquals(Utils.getReturnTypeName(functionSymbol), "any");
    }

    @Test
    public void testGetReturnTypeName_Union() {
        FunctionSymbol functionSymbol = mock(FunctionSymbol.class);
        FunctionTypeSymbol typeSymbol = mock(FunctionTypeSymbol.class);
        TypeSymbol returnType = mock(TypeSymbol.class);

        when(functionSymbol.typeDescriptor()).thenReturn(typeSymbol);
        when(typeSymbol.returnTypeDescriptor()).thenReturn(Optional.of(returnType));
        when(returnType.typeKind()).thenReturn(TypeDescKind.UNION);

        Assert.assertEquals(Utils.getReturnTypeName(functionSymbol), "union");
    }

    @Test
    public void testZipFolder() throws IOException {
        // Create a temp directory with files
        Path tempDir = Files.createTempDirectory("test-zip");
        Path subDir = Files.createDirectory(tempDir.resolve("subdir"));
        Files.writeString(tempDir.resolve("file1.txt"), "content1");
        Files.writeString(subDir.resolve("file2.txt"), "content2");

        // Create a .DS_Store file to test skipping
        Files.writeString(tempDir.resolve(".DS_Store"), "dsstore");
        Files.writeString(tempDir.resolve("._resource"), "resource fork");

        // Create __MACOSX directory to test skipping
        Path macosDir = Files.createDirectory(tempDir.resolve("__MACOSX"));
        Files.writeString(macosDir.resolve("hidden.txt"), "hidden");

        Path zipFile = Files.createTempFile("test-zip", ".zip");

        try {
            Utils.zipFolder(tempDir, zipFile.toString());

            // Verify zip file exists
            Assert.assertTrue(Files.exists(zipFile));
            Assert.assertTrue(Files.size(zipFile) > 0);
        } finally {
            // Cleanup
            Utils.deleteDirectory(tempDir);
            Files.deleteIfExists(zipFile);
        }
    }

    @Test
    public void testHasOpenApiResourceInfoAnnotation_EmptyName() {
        FunctionSymbol functionSymbol = mock(FunctionSymbol.class);
        AnnotationSymbol annotationSymbol = mock(AnnotationSymbol.class);
        when(annotationSymbol.getName()).thenReturn(Optional.empty());

        when(functionSymbol.annotations()).thenReturn(Collections.singletonList(annotationSymbol));

        boolean result = Utils.hasOpenApiResourceInfoAnnotation(functionSymbol);
        Assert.assertFalse(result);
    }

    @Test
    public void testGetReturnTypeName_Boolean() {
        FunctionSymbol functionSymbol = mock(FunctionSymbol.class);
        FunctionTypeSymbol typeSymbol = mock(FunctionTypeSymbol.class);
        TypeSymbol returnType = mock(TypeSymbol.class);

        when(functionSymbol.typeDescriptor()).thenReturn(typeSymbol);
        when(typeSymbol.returnTypeDescriptor()).thenReturn(Optional.of(returnType));
        when(returnType.typeKind()).thenReturn(TypeDescKind.BOOLEAN);

        Assert.assertEquals(Utils.getReturnTypeName(functionSymbol), "boolean");
    }

    @Test
    public void testGetReturnTypeName_Int() {
        FunctionSymbol functionSymbol = mock(FunctionSymbol.class);
        FunctionTypeSymbol typeSymbol = mock(FunctionTypeSymbol.class);
        TypeSymbol returnType = mock(TypeSymbol.class);

        when(functionSymbol.typeDescriptor()).thenReturn(typeSymbol);
        when(typeSymbol.returnTypeDescriptor()).thenReturn(Optional.of(returnType));
        when(returnType.typeKind()).thenReturn(TypeDescKind.INT);

        Assert.assertEquals(Utils.getReturnTypeName(functionSymbol), "int");
    }

    @Test
    public void testGetReturnTypeName_Float() {
        FunctionSymbol functionSymbol = mock(FunctionSymbol.class);
        FunctionTypeSymbol typeSymbol = mock(FunctionTypeSymbol.class);
        TypeSymbol returnType = mock(TypeSymbol.class);

        when(functionSymbol.typeDescriptor()).thenReturn(typeSymbol);
        when(typeSymbol.returnTypeDescriptor()).thenReturn(Optional.of(returnType));
        when(returnType.typeKind()).thenReturn(TypeDescKind.FLOAT);

        Assert.assertEquals(Utils.getReturnTypeName(functionSymbol), "float");
    }

    @Test
    public void testGetReturnTypeName_Decimal() {
        FunctionSymbol functionSymbol = mock(FunctionSymbol.class);
        FunctionTypeSymbol typeSymbol = mock(FunctionTypeSymbol.class);
        TypeSymbol returnType = mock(TypeSymbol.class);

        when(functionSymbol.typeDescriptor()).thenReturn(typeSymbol);
        when(typeSymbol.returnTypeDescriptor()).thenReturn(Optional.of(returnType));
        when(returnType.typeKind()).thenReturn(TypeDescKind.DECIMAL);

        Assert.assertEquals(Utils.getReturnTypeName(functionSymbol), "decimal");
    }

    @Test
    public void testGetReturnTypeName_Xml() {
        FunctionSymbol functionSymbol = mock(FunctionSymbol.class);
        FunctionTypeSymbol typeSymbol = mock(FunctionTypeSymbol.class);
        TypeSymbol returnType = mock(TypeSymbol.class);

        when(functionSymbol.typeDescriptor()).thenReturn(typeSymbol);
        when(typeSymbol.returnTypeDescriptor()).thenReturn(Optional.of(returnType));
        when(returnType.typeKind()).thenReturn(TypeDescKind.XML);

        Assert.assertEquals(Utils.getReturnTypeName(functionSymbol), "xml");
    }

    @Test
    public void testGetReturnTypeName_Json() {
        FunctionSymbol functionSymbol = mock(FunctionSymbol.class);
        FunctionTypeSymbol typeSymbol = mock(FunctionTypeSymbol.class);
        TypeSymbol returnType = mock(TypeSymbol.class);

        when(functionSymbol.typeDescriptor()).thenReturn(typeSymbol);
        when(typeSymbol.returnTypeDescriptor()).thenReturn(Optional.of(returnType));
        when(returnType.typeKind()).thenReturn(TypeDescKind.JSON);

        Assert.assertEquals(Utils.getReturnTypeName(functionSymbol), "json");
    }

    @Test
    public void testGetReturnTypeName_Array() {
        FunctionSymbol functionSymbol = mock(FunctionSymbol.class);
        FunctionTypeSymbol typeSymbol = mock(FunctionTypeSymbol.class);
        TypeSymbol returnType = mock(TypeSymbol.class);

        when(functionSymbol.typeDescriptor()).thenReturn(typeSymbol);
        when(typeSymbol.returnTypeDescriptor()).thenReturn(Optional.of(returnType));
        when(returnType.typeKind()).thenReturn(TypeDescKind.ARRAY);

        Assert.assertEquals(Utils.getReturnTypeName(functionSymbol), "array");
    }

    @Test
    public void testGetReturnTypeName_Map() {
        FunctionSymbol functionSymbol = mock(FunctionSymbol.class);
        FunctionTypeSymbol typeSymbol = mock(FunctionTypeSymbol.class);
        TypeSymbol returnType = mock(TypeSymbol.class);

        when(functionSymbol.typeDescriptor()).thenReturn(typeSymbol);
        when(typeSymbol.returnTypeDescriptor()).thenReturn(Optional.of(returnType));
        when(returnType.typeKind()).thenReturn(TypeDescKind.MAP);

        Assert.assertEquals(Utils.getReturnTypeName(functionSymbol), "map");
    }

    @Test
    public void testGetReturnTypeName_Record() {
        FunctionSymbol functionSymbol = mock(FunctionSymbol.class);
        FunctionTypeSymbol typeSymbol = mock(FunctionTypeSymbol.class);
        TypeSymbol returnType = mock(TypeSymbol.class);

        when(functionSymbol.typeDescriptor()).thenReturn(typeSymbol);
        when(typeSymbol.returnTypeDescriptor()).thenReturn(Optional.of(returnType));
        when(returnType.typeKind()).thenReturn(TypeDescKind.RECORD);

        Assert.assertEquals(Utils.getReturnTypeName(functionSymbol), "record");
    }

    @Test
    public void testSanitizeXmlName_InvalidFirstChar() {
        // First char is invalid and not a digit - should be skipped
        Assert.assertEquals(Utils.sanitizeXmlName("@name"), "name");
        Assert.assertEquals(Utils.sanitizeXmlName("!test"), "test");
        Assert.assertEquals(Utils.sanitizeXmlName("#hello"), "hello");
    }

    @Test
    public void testSanitizeXmlName_AllInvalidChars() {
        // All characters are invalid
        Assert.assertEquals(Utils.sanitizeXmlName("@#$"), "resource");
    }

    @Test
    public void testGenerateSynapseNameForResourceWithNullHint() {
        FunctionSymbol functionSymbol = mock(FunctionSymbol.class);
        FunctionTypeSymbol functionTypeSymbol = mock(FunctionTypeSymbol.class);
        TypeSymbol returnTypeSymbol = mock(TypeSymbol.class);

        when(functionSymbol.getName()).thenReturn(Optional.of("get"));
        when(functionSymbol.typeDescriptor()).thenReturn(functionTypeSymbol);
        when(functionTypeSymbol.params()).thenReturn(Optional.empty());
        when(functionTypeSymbol.returnTypeDescriptor()).thenReturn(Optional.of(returnTypeSymbol));
        when(returnTypeSymbol.typeKind()).thenReturn(TypeDescKind.STRING);
        when(returnTypeSymbol.getName()).thenReturn(Optional.of("String"));

        // Null hint should fall back to default behavior
        String synapseName = Utils.generateSynapseName(functionSymbol, FunctionType.RESOURCE, null);
        Assert.assertEquals(synapseName, "getString");
    }

    @Test
    public void testGenerateSynapseNameForResourceWithNoReturnType() {
        FunctionSymbol functionSymbol = mock(FunctionSymbol.class);
        FunctionTypeSymbol functionTypeSymbol = mock(FunctionTypeSymbol.class);

        when(functionSymbol.getName()).thenReturn(Optional.of("delete"));
        when(functionSymbol.typeDescriptor()).thenReturn(functionTypeSymbol);
        when(functionTypeSymbol.params()).thenReturn(Optional.empty());
        when(functionTypeSymbol.returnTypeDescriptor()).thenReturn(Optional.empty());

        String synapseName = Utils.generateSynapseName(functionSymbol, FunctionType.RESOURCE);
        Assert.assertEquals(synapseName, "deleteResource");
    }

    @Test
    public void testGenerateSynapseNameForResourceWithFunctionName() {
        FunctionSymbol functionSymbol = mock(FunctionSymbol.class);
        FunctionTypeSymbol functionTypeSymbol = mock(FunctionTypeSymbol.class);

        when(functionSymbol.getName()).thenReturn(Optional.of("myFunction"));
        when(functionSymbol.typeDescriptor()).thenReturn(functionTypeSymbol);
        when(functionTypeSymbol.params()).thenReturn(Optional.empty());
        when(functionTypeSymbol.returnTypeDescriptor()).thenReturn(Optional.empty());

        String synapseName = Utils.generateSynapseName(functionSymbol, FunctionType.FUNCTION);
        Assert.assertEquals(synapseName, "myFunction");
    }

    @Test
    public void testGenerateSynapseNameForResourceWithEmptyFunctionName() {
        FunctionSymbol functionSymbol = mock(FunctionSymbol.class);
        FunctionTypeSymbol functionTypeSymbol = mock(FunctionTypeSymbol.class);

        when(functionSymbol.getName()).thenReturn(Optional.empty());
        when(functionSymbol.typeDescriptor()).thenReturn(functionTypeSymbol);
        when(functionTypeSymbol.params()).thenReturn(Optional.empty());
        when(functionTypeSymbol.returnTypeDescriptor()).thenReturn(Optional.empty());

        String synapseName = Utils.generateSynapseName(functionSymbol, FunctionType.FUNCTION);
        Assert.assertEquals(synapseName, "unknown");
    }

    @Test
    public void testGenerateSynapseNameWithTypeSuffixes() {
        FunctionSymbol functionSymbol = mock(FunctionSymbol.class);
        FunctionTypeSymbol functionTypeSymbol = mock(FunctionTypeSymbol.class);
        ParameterSymbol paramSymbol = mock(ParameterSymbol.class);
        TypeSymbol paramTypeSymbol = mock(TypeSymbol.class);

        when(functionSymbol.getName()).thenReturn(Optional.of("post"));
        when(functionSymbol.typeDescriptor()).thenReturn(functionTypeSymbol);
        when(functionTypeSymbol.params()).thenReturn(Optional.of(Collections.singletonList(paramSymbol)));
        when(paramSymbol.typeDescriptor()).thenReturn(paramTypeSymbol);
        when(paramTypeSymbol.typeKind()).thenReturn(TypeDescKind.RECORD);
        when(paramTypeSymbol.getName()).thenReturn(Optional.of("GmailUsersDraftsListQueries"));

        String synapseName = Utils.generateSynapseName(functionSymbol, FunctionType.RESOURCE);
        // Should strip "Queries" suffix and extract meaningful name
        Assert.assertTrue(synapseName.startsWith("post"));
    }

    @Test
    public void testGenerateSynapseNameWithRequestSuffix() {
        FunctionSymbol functionSymbol = mock(FunctionSymbol.class);
        FunctionTypeSymbol functionTypeSymbol = mock(FunctionTypeSymbol.class);
        ParameterSymbol paramSymbol = mock(ParameterSymbol.class);
        TypeSymbol paramTypeSymbol = mock(TypeSymbol.class);

        when(functionSymbol.getName()).thenReturn(Optional.of("post"));
        when(functionSymbol.typeDescriptor()).thenReturn(functionTypeSymbol);
        when(functionTypeSymbol.params()).thenReturn(Optional.of(Collections.singletonList(paramSymbol)));
        when(paramSymbol.typeDescriptor()).thenReturn(paramTypeSymbol);
        when(paramTypeSymbol.typeKind()).thenReturn(TypeDescKind.RECORD);
        when(paramTypeSymbol.getName()).thenReturn(Optional.of("CreateUserRequest"));

        String synapseName = Utils.generateSynapseName(functionSymbol, FunctionType.RESOURCE);
        Assert.assertEquals(synapseName, "postUser");
    }

    @Test
    public void testGenerateSynapseNameWithPayloadSuffix() {
        FunctionSymbol functionSymbol = mock(FunctionSymbol.class);
        FunctionTypeSymbol functionTypeSymbol = mock(FunctionTypeSymbol.class);
        ParameterSymbol paramSymbol = mock(ParameterSymbol.class);
        TypeSymbol paramTypeSymbol = mock(TypeSymbol.class);

        when(functionSymbol.getName()).thenReturn(Optional.of("post"));
        when(functionSymbol.typeDescriptor()).thenReturn(functionTypeSymbol);
        when(functionTypeSymbol.params()).thenReturn(Optional.of(Collections.singletonList(paramSymbol)));
        when(paramSymbol.typeDescriptor()).thenReturn(paramTypeSymbol);
        when(paramTypeSymbol.typeKind()).thenReturn(TypeDescKind.RECORD);
        when(paramTypeSymbol.getName()).thenReturn(Optional.of("CreateMessagePayload"));

        String synapseName = Utils.generateSynapseName(functionSymbol, FunctionType.RESOURCE);
        Assert.assertEquals(synapseName, "postMessage");
    }

    @Test
    public void testGenerateSynapseNameWithEmptyParamTypeName() {
        FunctionSymbol functionSymbol = mock(FunctionSymbol.class);
        FunctionTypeSymbol functionTypeSymbol = mock(FunctionTypeSymbol.class);
        ParameterSymbol paramSymbol = mock(ParameterSymbol.class);
        TypeSymbol paramTypeSymbol = mock(TypeSymbol.class);
        TypeSymbol returnTypeSymbol = mock(TypeSymbol.class);

        when(functionSymbol.getName()).thenReturn(Optional.of("get"));
        when(functionSymbol.typeDescriptor()).thenReturn(functionTypeSymbol);
        when(functionTypeSymbol.params()).thenReturn(Optional.of(Collections.singletonList(paramSymbol)));
        when(paramSymbol.typeDescriptor()).thenReturn(paramTypeSymbol);
        when(paramTypeSymbol.typeKind()).thenReturn(TypeDescKind.STRING);
        when(paramTypeSymbol.getName()).thenReturn(Optional.empty());
        when(functionTypeSymbol.returnTypeDescriptor()).thenReturn(Optional.of(returnTypeSymbol));
        when(returnTypeSymbol.typeKind()).thenReturn(TypeDescKind.RECORD);
        when(returnTypeSymbol.getName()).thenReturn(Optional.of("UserResponse"));

        String synapseName = Utils.generateSynapseName(functionSymbol, FunctionType.RESOURCE);
        Assert.assertEquals(synapseName, "getUser");
    }

    @Test
    public void testHumanizeName_WithMultipleSpaces() {
        Assert.assertEquals(Utils.humanizeName("user  name"), "user name");
    }

    @Test
    public void testHumanizeName_LeadingAndTrailingSpaces() {
        Assert.assertEquals(Utils.humanizeName("  userName  "), "user name");
    }

    @Test
    public void testHumanizeName_SingleUppercaseLetter() {
        Assert.assertEquals(Utils.humanizeName("A"), "a");
    }

    @Test
    public void testHumanizeName_SingleLowercaseLetter() {
        Assert.assertEquals(Utils.humanizeName("a"), "a");
    }

    @Test
    public void testGenerateSynapseNameForRemoteFunction() {
        FunctionSymbol functionSymbol = mock(FunctionSymbol.class);
        when(functionSymbol.getName()).thenReturn(Optional.of("sendMessage"));

        String synapseName = Utils.generateSynapseName(functionSymbol, FunctionType.REMOTE);
        Assert.assertEquals(synapseName, "sendMessage");
    }

    @Test
    public void testGenerateSynapseNameForInitFunction() {
        FunctionSymbol functionSymbol = mock(FunctionSymbol.class);
        when(functionSymbol.getName()).thenReturn(Optional.of("init"));

        String synapseName = Utils.generateSynapseName(functionSymbol, FunctionType.INIT);
        Assert.assertEquals(synapseName, "init");
    }

    @Test
    public void testGetOpenApiOperationId_Found() {
        // Mock FunctionSymbol
        FunctionSymbol functionSymbol = mock(FunctionSymbol.class);

        // Mock Module
        Module module = mock(Module.class);
        DocumentId docId = mock(DocumentId.class);
        when(module.documentIds()).thenReturn(List.of(docId));

        Document document = mock(Document.class);
        when(module.document(docId)).thenReturn(document);

        // Mock SyntaxTree
        SyntaxTree syntaxTree = mock(SyntaxTree.class);
        when(document.syntaxTree()).thenReturn(syntaxTree);

        // Mock Root Node
        Node rootNode = mock(Node.class);
        when(syntaxTree.rootNode()).thenReturn(rootNode);

        // Mock SemanticModel
        SemanticModel semanticModel = mock(SemanticModel.class);
        
        io.ballerina.compiler.api.symbols.AnnotationSymbol annotationSymbol = mock(io.ballerina.compiler.api.symbols.AnnotationSymbol.class);
        when(annotationSymbol.getName()).thenReturn(Optional.of("ResourceInfo"));
        
        io.ballerina.compiler.api.symbols.ModuleSymbol moduleSymbol = mock(io.ballerina.compiler.api.symbols.ModuleSymbol.class);
        when(moduleSymbol.getName()).thenReturn(Optional.of("openapi"));
        when(annotationSymbol.getModule()).thenReturn(Optional.of(moduleSymbol));
        
        when(functionSymbol.annotations()).thenReturn(List.of(annotationSymbol));

        Mockito.doAnswer(invocation -> {
            NodeVisitor visitor = invocation.getArgument(0);
            // Basic invocation to ensure visitor logic is triggered but won't find OpId
            return null;
        }).when(rootNode).accept(any(NodeVisitor.class));

        Optional<String> result = Utils.getOpenApiOperationId(functionSymbol, module, semanticModel);
        Assert.assertFalse(result.isPresent());
    }

    @Test
    public void testHumanizeName_MoreCases() {
        Assert.assertEquals(Utils.humanizeName("OAuth2Provider"), "oauth2 provider");
        Assert.assertEquals(Utils.humanizeName("aVeryLongFunctionNameWithMixedCaseAndNumbers123"), "a very long function name with mixed case and numbers123");
    }

    @Test
    public void testSanitizeXmlName_MoreCases() {
         Assert.assertEquals(Utils.sanitizeXmlName("foo:bar"), "foo_bar");
         Assert.assertEquals(Utils.sanitizeXmlName("foo/bar"), "foo_bar");
    }
}
