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

package io.ballerina.mi.analyzer;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.*;
import io.ballerina.compiler.api.symbols.resourcepath.PathSegmentList;
import io.ballerina.compiler.api.symbols.resourcepath.util.PathSegment;
import io.ballerina.mi.model.Connector;
import io.ballerina.projects.ModuleId;
import io.ballerina.projects.PackageCompilation;
import io.ballerina.projects.PackageDescriptor;
import io.ballerina.projects.PackageName;
import io.ballerina.projects.PackageOrg;
import io.ballerina.projects.PackageVersion;
import io.ballerina.projects.SemanticVersion;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.*;

import static org.mockito.Mockito.*;

/**
 * Tests for BalModuleAnalyzer class.
 */
public class BalModuleAnalyzerTest {

    @BeforeMethod
    public void setUp() {
        Connector.reset();
    }

    @AfterMethod
    public void tearDown() {
        Connector.reset();
    }

    @Test
    public void testBalModuleAnalyzerConstruction() {
        BalModuleAnalyzer analyzer = new BalModuleAnalyzer();
        Assert.assertNotNull(analyzer);
    }

    @Test
    public void testAnalyzerIsInstanceOfAnalyzer() {
        BalModuleAnalyzer analyzer = new BalModuleAnalyzer();
        Assert.assertTrue(analyzer instanceof Analyzer);
    }

    @Test
    public void testAnalyze_NoSymbols() {
        BalModuleAnalyzer analyzer = new BalModuleAnalyzer();

        io.ballerina.projects.Package mockPackage = createMockPackage("testModule", "wso2", "1.0.0");
        SemanticModel mockSemanticModel = mock(SemanticModel.class);
        when(mockSemanticModel.moduleSymbols()).thenReturn(Collections.emptyList());

        PackageCompilation mockCompilation = mock(PackageCompilation.class);
        when(mockCompilation.getSemanticModel(any())).thenReturn(mockSemanticModel);
        when(mockPackage.getCompilation()).thenReturn(mockCompilation);

        analyzer.analyze(mockPackage);

        Connector connector = Connector.getConnector();
        Assert.assertNotNull(connector);
    }

    @Test
    public void testAnalyze_NonFunctionSymbol() {
        BalModuleAnalyzer analyzer = new BalModuleAnalyzer();

        io.ballerina.projects.Package mockPackage = createMockPackage("testModule", "wso2", "1.0.0");
        SemanticModel mockSemanticModel = mock(SemanticModel.class);

        // Create a non-function symbol (e.g., a TypeDefinition)
        TypeDefinitionSymbol mockTypeSymbol = mock(TypeDefinitionSymbol.class);
        when(mockTypeSymbol.kind()).thenReturn(SymbolKind.TYPE_DEFINITION);

        when(mockSemanticModel.moduleSymbols()).thenReturn(Collections.singletonList(mockTypeSymbol));

        PackageCompilation mockCompilation = mock(PackageCompilation.class);
        when(mockCompilation.getSemanticModel(any())).thenReturn(mockSemanticModel);
        when(mockPackage.getCompilation()).thenReturn(mockCompilation);

        analyzer.analyze(mockPackage);

        Connector connector = Connector.getConnector();
        Assert.assertNotNull(connector);
        Assert.assertTrue(connector.isBalModule());
    }

    @Test
    public void testAnalyze_FunctionWithoutOperationAnnotation() {
        BalModuleAnalyzer analyzer = new BalModuleAnalyzer();

        io.ballerina.projects.Package mockPackage = createMockPackage("testModule", "wso2", "1.0.0");
        SemanticModel mockSemanticModel = mock(SemanticModel.class);

        // Create a function symbol without @mi:Operation annotation
        FunctionSymbol mockFunction = mock(FunctionSymbol.class);
        when(mockFunction.kind()).thenReturn(SymbolKind.FUNCTION);
        when(mockFunction.annotations()).thenReturn(Collections.emptyList());

        when(mockSemanticModel.moduleSymbols()).thenReturn(Collections.singletonList(mockFunction));

        PackageCompilation mockCompilation = mock(PackageCompilation.class);
        when(mockCompilation.getSemanticModel(any())).thenReturn(mockSemanticModel);
        when(mockPackage.getCompilation()).thenReturn(mockCompilation);

        analyzer.analyze(mockPackage);

        Connector connector = Connector.getConnector();
        Assert.assertNotNull(connector);
        // No connections should be added since no @mi:Operation annotation
        Assert.assertTrue(connector.getConnections().isEmpty());
    }

    @Test
    public void testAnalyze_FunctionWithOperationAnnotation() {
        BalModuleAnalyzer analyzer = new BalModuleAnalyzer();

        io.ballerina.projects.Package mockPackage = createMockPackage("testModule", "wso2", "1.0.0");
        SemanticModel mockSemanticModel = mock(SemanticModel.class);

        // Create a function symbol with @mi:Operation annotation
        FunctionSymbol mockFunction = createMockFunctionWithOperationAnnotation("testFunction");

        when(mockSemanticModel.moduleSymbols()).thenReturn(Collections.singletonList(mockFunction));

        PackageCompilation mockCompilation = mock(PackageCompilation.class);
        when(mockCompilation.getSemanticModel(any())).thenReturn(mockSemanticModel);
        when(mockPackage.getCompilation()).thenReturn(mockCompilation);

        analyzer.analyze(mockPackage);

        Connector connector = Connector.getConnector();
        Assert.assertNotNull(connector);
        Assert.assertTrue(connector.isBalModule());
    }

    @Test
    public void testAnalyze_FunctionWithMixedAnnotations() {
        BalModuleAnalyzer analyzer = new BalModuleAnalyzer();

        io.ballerina.projects.Package mockPackage = createMockPackage("testModule", "wso2", "1.0.0");
        SemanticModel mockSemanticModel = mock(SemanticModel.class);

        // Create annotation that is not "Operation"
        AnnotationSymbol otherAnnotation = mock(AnnotationSymbol.class);
        when(otherAnnotation.getName()).thenReturn(Optional.of("SomeOtherAnnotation"));

        // Create the Operation annotation
        AnnotationSymbol operationAnnotation = mock(AnnotationSymbol.class);
        when(operationAnnotation.getName()).thenReturn(Optional.of("Operation"));

        FunctionSymbol mockFunction = mock(FunctionSymbol.class);
        when(mockFunction.kind()).thenReturn(SymbolKind.FUNCTION);
        when(mockFunction.annotations()).thenReturn(Arrays.asList(otherAnnotation, operationAnnotation));
        when(mockFunction.getName()).thenReturn(Optional.of("mixedAnnotationFunction"));

        // Mock the function type descriptor
        FunctionTypeSymbol functionTypeSymbol = mock(FunctionTypeSymbol.class);
        when(functionTypeSymbol.params()).thenReturn(Optional.empty());
        when(functionTypeSymbol.returnTypeDescriptor()).thenReturn(Optional.empty());
        when(mockFunction.typeDescriptor()).thenReturn(functionTypeSymbol);
        when(mockFunction.documentation()).thenReturn(Optional.empty());

        // Mock qualifiers for FunctionType
        when(mockFunction.qualifiers()).thenReturn(Collections.emptyList());

        when(mockSemanticModel.moduleSymbols()).thenReturn(Collections.singletonList(mockFunction));

        PackageCompilation mockCompilation = mock(PackageCompilation.class);
        when(mockCompilation.getSemanticModel(any())).thenReturn(mockSemanticModel);
        when(mockPackage.getCompilation()).thenReturn(mockCompilation);

        analyzer.analyze(mockPackage);

        Connector connector = Connector.getConnector();
        Assert.assertNotNull(connector);
    }

    @Test
    public void testAnalyze_FunctionWithEmptyName() {
        BalModuleAnalyzer analyzer = new BalModuleAnalyzer();

        io.ballerina.projects.Package mockPackage = createMockPackage("testModule", "wso2", "1.0.0");
        SemanticModel mockSemanticModel = mock(SemanticModel.class);

        // Create a function with Operation annotation but no name
        AnnotationSymbol operationAnnotation = mock(AnnotationSymbol.class);
        when(operationAnnotation.getName()).thenReturn(Optional.of("Operation"));

        FunctionSymbol mockFunction = mock(FunctionSymbol.class);
        when(mockFunction.kind()).thenReturn(SymbolKind.FUNCTION);
        when(mockFunction.annotations()).thenReturn(Collections.singletonList(operationAnnotation));
        when(mockFunction.getName()).thenReturn(Optional.empty()); // No name

        when(mockSemanticModel.moduleSymbols()).thenReturn(Collections.singletonList(mockFunction));

        PackageCompilation mockCompilation = mock(PackageCompilation.class);
        when(mockCompilation.getSemanticModel(any())).thenReturn(mockSemanticModel);
        when(mockPackage.getCompilation()).thenReturn(mockCompilation);

        analyzer.analyze(mockPackage);

        Connector connector = Connector.getConnector();
        Assert.assertNotNull(connector);
        // Function should be skipped since it has no name
    }

    @Test
    public void testAnalyze_FunctionWithDocumentation() {
        BalModuleAnalyzer analyzer = new BalModuleAnalyzer();

        io.ballerina.projects.Package mockPackage = createMockPackage("testModule", "wso2", "1.0.0");
        SemanticModel mockSemanticModel = mock(SemanticModel.class);

        // Create a function with documentation
        AnnotationSymbol operationAnnotation = mock(AnnotationSymbol.class);
        when(operationAnnotation.getName()).thenReturn(Optional.of("Operation"));

        FunctionSymbol mockFunction = mock(FunctionSymbol.class);
        when(mockFunction.kind()).thenReturn(SymbolKind.FUNCTION);
        when(mockFunction.annotations()).thenReturn(Collections.singletonList(operationAnnotation));
        when(mockFunction.getName()).thenReturn(Optional.of("documentedFunction"));

        // Mock documentation
        Documentation documentation = mock(Documentation.class);
        when(documentation.description()).thenReturn(Optional.of("This is the function description"));
        when(mockFunction.documentation()).thenReturn(Optional.of(documentation));

        // Mock the function type descriptor
        FunctionTypeSymbol functionTypeSymbol = mock(FunctionTypeSymbol.class);
        when(functionTypeSymbol.params()).thenReturn(Optional.empty());
        when(functionTypeSymbol.returnTypeDescriptor()).thenReturn(Optional.empty());
        when(mockFunction.typeDescriptor()).thenReturn(functionTypeSymbol);
        when(mockFunction.qualifiers()).thenReturn(Collections.emptyList());

        when(mockSemanticModel.moduleSymbols()).thenReturn(Collections.singletonList(mockFunction));

        PackageCompilation mockCompilation = mock(PackageCompilation.class);
        when(mockCompilation.getSemanticModel(any())).thenReturn(mockSemanticModel);
        when(mockPackage.getCompilation()).thenReturn(mockCompilation);

        analyzer.analyze(mockPackage);

        Connector connector = Connector.getConnector();
        Assert.assertNotNull(connector);
    }

    @Test
    public void testAnalyze_FunctionWithParameters() {
        BalModuleAnalyzer analyzer = new BalModuleAnalyzer();

        io.ballerina.projects.Package mockPackage = createMockPackage("testModule", "wso2", "1.0.0");
        SemanticModel mockSemanticModel = mock(SemanticModel.class);

        FunctionSymbol mockFunction = createMockFunctionWithParameters("parameterizedFunction",
                Arrays.asList("param1", "param2"));

        when(mockSemanticModel.moduleSymbols()).thenReturn(Collections.singletonList(mockFunction));

        PackageCompilation mockCompilation = mock(PackageCompilation.class);
        when(mockCompilation.getSemanticModel(any())).thenReturn(mockSemanticModel);
        when(mockPackage.getCompilation()).thenReturn(mockCompilation);

        analyzer.analyze(mockPackage);

        Connector connector = Connector.getConnector();
        Assert.assertNotNull(connector);
    }

    @Test
    public void testAnalyze_ResourceFunctionWithPathParams() {
        BalModuleAnalyzer analyzer = new BalModuleAnalyzer();

        io.ballerina.projects.Package mockPackage = createMockPackage("testModule", "wso2", "1.0.0");
        SemanticModel mockSemanticModel = mock(SemanticModel.class);

        // Create a resource method with path parameters
        ResourceMethodSymbol mockResourceMethod = createMockResourceMethod("getUser",
                Arrays.asList("[string userId]", "profile"));

        when(mockSemanticModel.moduleSymbols()).thenReturn(Collections.singletonList(mockResourceMethod));

        PackageCompilation mockCompilation = mock(PackageCompilation.class);
        when(mockCompilation.getSemanticModel(any())).thenReturn(mockSemanticModel);
        when(mockPackage.getCompilation()).thenReturn(mockCompilation);

        analyzer.analyze(mockPackage);

        Connector connector = Connector.getConnector();
        Assert.assertNotNull(connector);
    }

    @Test
    public void testAnalyze_ResourceFunctionWithMultiplePathParams() {
        BalModuleAnalyzer analyzer = new BalModuleAnalyzer();

        io.ballerina.projects.Package mockPackage = createMockPackage("testModule", "wso2", "1.0.0");
        SemanticModel mockSemanticModel = mock(SemanticModel.class);

        // Create a resource method with multiple path parameters
        ResourceMethodSymbol mockResourceMethod = createMockResourceMethod("getOrder",
                Arrays.asList("[string customerId]", "orders", "[int orderId]"));

        when(mockSemanticModel.moduleSymbols()).thenReturn(Collections.singletonList(mockResourceMethod));

        PackageCompilation mockCompilation = mock(PackageCompilation.class);
        when(mockCompilation.getSemanticModel(any())).thenReturn(mockSemanticModel);
        when(mockPackage.getCompilation()).thenReturn(mockCompilation);

        analyzer.analyze(mockPackage);

        Connector connector = Connector.getConnector();
        Assert.assertNotNull(connector);
    }

    @Test
    public void testAnalyze_ConnectorWithDotInModuleName() {
        BalModuleAnalyzer analyzer = new BalModuleAnalyzer();

        // Module name with dots
        io.ballerina.projects.Package mockPackage = createMockPackage("test.module.name", "wso2", "1.0.0");
        SemanticModel mockSemanticModel = mock(SemanticModel.class);

        FunctionSymbol mockFunction = createMockFunctionWithOperationAnnotation("dotFunction");

        when(mockSemanticModel.moduleSymbols()).thenReturn(Collections.singletonList(mockFunction));

        PackageCompilation mockCompilation = mock(PackageCompilation.class);
        when(mockCompilation.getSemanticModel(any())).thenReturn(mockSemanticModel);
        when(mockPackage.getCompilation()).thenReturn(mockCompilation);

        analyzer.analyze(mockPackage);

        Connector connector = Connector.getConnector();
        Assert.assertNotNull(connector);
        Assert.assertTrue(connector.getModuleName().contains("."));
    }

    @Test
    public void testAnalyze_FunctionNameConflictsWithParameter() {
        BalModuleAnalyzer analyzer = new BalModuleAnalyzer();

        io.ballerina.projects.Package mockPackage = createMockPackage("testModule", "wso2", "1.0.0");
        SemanticModel mockSemanticModel = mock(SemanticModel.class);

        // Create a function where the function name might conflict with a parameter name
        FunctionSymbol mockFunction = createMockFunctionWithParameters("conflictName",
                Arrays.asList("conflictName", "otherParam"));

        when(mockSemanticModel.moduleSymbols()).thenReturn(Collections.singletonList(mockFunction));

        PackageCompilation mockCompilation = mock(PackageCompilation.class);
        when(mockCompilation.getSemanticModel(any())).thenReturn(mockSemanticModel);
        when(mockPackage.getCompilation()).thenReturn(mockCompilation);

        analyzer.analyze(mockPackage);

        Connector connector = Connector.getConnector();
        Assert.assertNotNull(connector);
    }

    @Test
    public void testAnalyze_MultipleFunctions() {
        BalModuleAnalyzer analyzer = new BalModuleAnalyzer();

        io.ballerina.projects.Package mockPackage = createMockPackage("testModule", "wso2", "1.0.0");
        SemanticModel mockSemanticModel = mock(SemanticModel.class);

        FunctionSymbol mockFunction1 = createMockFunctionWithOperationAnnotation("function1");
        FunctionSymbol mockFunction2 = createMockFunctionWithOperationAnnotation("function2");
        FunctionSymbol mockFunction3 = createMockFunctionWithOperationAnnotation("function3");

        when(mockSemanticModel.moduleSymbols()).thenReturn(Arrays.asList(mockFunction1, mockFunction2, mockFunction3));

        PackageCompilation mockCompilation = mock(PackageCompilation.class);
        when(mockCompilation.getSemanticModel(any())).thenReturn(mockSemanticModel);
        when(mockPackage.getCompilation()).thenReturn(mockCompilation);

        analyzer.analyze(mockPackage);

        Connector connector = Connector.getConnector();
        Assert.assertNotNull(connector);
    }

    @Test
    public void testAnalyze_FunctionWithReturnType() {
        BalModuleAnalyzer analyzer = new BalModuleAnalyzer();

        io.ballerina.projects.Package mockPackage = createMockPackage("testModule", "wso2", "1.0.0");
        SemanticModel mockSemanticModel = mock(SemanticModel.class);

        // Create a function with return type
        AnnotationSymbol operationAnnotation = mock(AnnotationSymbol.class);
        when(operationAnnotation.getName()).thenReturn(Optional.of("Operation"));

        FunctionSymbol mockFunction = mock(FunctionSymbol.class);
        when(mockFunction.kind()).thenReturn(SymbolKind.FUNCTION);
        when(mockFunction.annotations()).thenReturn(Collections.singletonList(operationAnnotation));
        when(mockFunction.getName()).thenReturn(Optional.of("functionWithReturn"));
        when(mockFunction.documentation()).thenReturn(Optional.empty());
        when(mockFunction.qualifiers()).thenReturn(Collections.emptyList());

        // Mock the function type descriptor with return type
        FunctionTypeSymbol functionTypeSymbol = mock(FunctionTypeSymbol.class);
        when(functionTypeSymbol.params()).thenReturn(Optional.empty());

        TypeSymbol returnType = mock(TypeSymbol.class);
        when(returnType.typeKind()).thenReturn(TypeDescKind.STRING);
        when(functionTypeSymbol.returnTypeDescriptor()).thenReturn(Optional.of(returnType));
        when(mockFunction.typeDescriptor()).thenReturn(functionTypeSymbol);

        when(mockSemanticModel.moduleSymbols()).thenReturn(Collections.singletonList(mockFunction));

        PackageCompilation mockCompilation = mock(PackageCompilation.class);
        when(mockCompilation.getSemanticModel(any())).thenReturn(mockSemanticModel);
        when(mockPackage.getCompilation()).thenReturn(mockCompilation);

        analyzer.analyze(mockPackage);

        Connector connector = Connector.getConnector();
        Assert.assertNotNull(connector);
    }

    @Test
    public void testAnalyze_ResourceFunctionWithNonPathParamSegments() {
        BalModuleAnalyzer analyzer = new BalModuleAnalyzer();

        io.ballerina.projects.Package mockPackage = createMockPackage("testModule", "wso2", "1.0.0");
        SemanticModel mockSemanticModel = mock(SemanticModel.class);

        // Create a resource method with only literal path segments (no path params)
        ResourceMethodSymbol mockResourceMethod = createMockResourceMethod("listUsers",
                Arrays.asList("users", "list", "all"));

        when(mockSemanticModel.moduleSymbols()).thenReturn(Collections.singletonList(mockResourceMethod));

        PackageCompilation mockCompilation = mock(PackageCompilation.class);
        when(mockCompilation.getSemanticModel(any())).thenReturn(mockSemanticModel);
        when(mockPackage.getCompilation()).thenReturn(mockCompilation);

        analyzer.analyze(mockPackage);

        Connector connector = Connector.getConnector();
        Assert.assertNotNull(connector);
    }

    @Test
    public void testAnalyze_AnnotationWithEmptyName() {
        BalModuleAnalyzer analyzer = new BalModuleAnalyzer();

        io.ballerina.projects.Package mockPackage = createMockPackage("testModule", "wso2", "1.0.0");
        SemanticModel mockSemanticModel = mock(SemanticModel.class);

        // Create annotation with empty name
        AnnotationSymbol emptyNameAnnotation = mock(AnnotationSymbol.class);
        when(emptyNameAnnotation.getName()).thenReturn(Optional.empty());

        FunctionSymbol mockFunction = mock(FunctionSymbol.class);
        when(mockFunction.kind()).thenReturn(SymbolKind.FUNCTION);
        when(mockFunction.annotations()).thenReturn(Collections.singletonList(emptyNameAnnotation));

        when(mockSemanticModel.moduleSymbols()).thenReturn(Collections.singletonList(mockFunction));

        PackageCompilation mockCompilation = mock(PackageCompilation.class);
        when(mockCompilation.getSemanticModel(any())).thenReturn(mockSemanticModel);
        when(mockPackage.getCompilation()).thenReturn(mockCompilation);

        analyzer.analyze(mockPackage);

        Connector connector = Connector.getConnector();
        Assert.assertNotNull(connector);
    }

    @Test
    public void testAnalyze_PathParamWithTypeAndName() {
        BalModuleAnalyzer analyzer = new BalModuleAnalyzer();

        io.ballerina.projects.Package mockPackage = createMockPackage("testModule", "wso2", "1.0.0");
        SemanticModel mockSemanticModel = mock(SemanticModel.class);

        // Create a resource method with path param that has type and name like "[int userId]"
        ResourceMethodSymbol mockResourceMethod = createMockResourceMethod("getUserById",
                Arrays.asList("users", "[int userId]"));

        when(mockSemanticModel.moduleSymbols()).thenReturn(Collections.singletonList(mockResourceMethod));

        PackageCompilation mockCompilation = mock(PackageCompilation.class);
        when(mockCompilation.getSemanticModel(any())).thenReturn(mockSemanticModel);
        when(mockPackage.getCompilation()).thenReturn(mockCompilation);

        analyzer.analyze(mockPackage);

        Connector connector = Connector.getConnector();
        Assert.assertNotNull(connector);
    }

    @Test
    public void testAnalyze_PathParamNameExtraction_NoSpace() {
        BalModuleAnalyzer analyzer = new BalModuleAnalyzer();

        io.ballerina.projects.Package mockPackage = createMockPackage("testModule", "wso2", "1.0.0");
        SemanticModel mockSemanticModel = mock(SemanticModel.class);

        // Create a resource method with path param that has no space (edge case)
        ResourceMethodSymbol mockResourceMethod = createMockResourceMethod("getItem",
                Arrays.asList("[itemId]")); // No type, just name

        when(mockSemanticModel.moduleSymbols()).thenReturn(Collections.singletonList(mockResourceMethod));

        PackageCompilation mockCompilation = mock(PackageCompilation.class);
        when(mockCompilation.getSemanticModel(any())).thenReturn(mockSemanticModel);
        when(mockPackage.getCompilation()).thenReturn(mockCompilation);

        analyzer.analyze(mockPackage);

        Connector connector = Connector.getConnector();
        Assert.assertNotNull(connector);
    }

    @Test
    public void testAnalyze_ResourceFunctionWithMatchingFunctionParam() {
        BalModuleAnalyzer analyzer = new BalModuleAnalyzer();

        io.ballerina.projects.Package mockPackage = createMockPackage("testModule", "wso2", "1.0.0");
        SemanticModel mockSemanticModel = mock(SemanticModel.class);

        // Create a resource method where path param matches a function parameter
        AnnotationSymbol operationAnnotation = mock(AnnotationSymbol.class);
        when(operationAnnotation.getName()).thenReturn(Optional.of("Operation"));

        ResourceMethodSymbol mockResourceMethod = mock(ResourceMethodSymbol.class);
        when(mockResourceMethod.kind()).thenReturn(SymbolKind.RESOURCE_METHOD);
        when(mockResourceMethod.annotations()).thenReturn(Collections.singletonList(operationAnnotation));
        when(mockResourceMethod.getName()).thenReturn(Optional.of("getUserDetails"));
        when(mockResourceMethod.documentation()).thenReturn(Optional.empty());
        when(mockResourceMethod.qualifiers()).thenReturn(Collections.singletonList(Qualifier.RESOURCE));

        // Create path segments with [string userId]
        PathSegment pathSegment = mock(PathSegment.class);
        when(pathSegment.signature()).thenReturn("[string userId]");

        PathSegmentList pathSegmentList = mock(PathSegmentList.class);
        when(pathSegmentList.list()).thenReturn(Collections.singletonList(pathSegment));
        when(mockResourceMethod.resourcePath()).thenReturn(pathSegmentList);

        // Create matching parameter
        ParameterSymbol paramSymbol = mock(ParameterSymbol.class);
        when(paramSymbol.getName()).thenReturn(Optional.of("userId"));
        TypeSymbol paramType = mock(TypeSymbol.class);
        when(paramType.typeKind()).thenReturn(TypeDescKind.STRING);
        when(paramSymbol.typeDescriptor()).thenReturn(paramType);

        FunctionTypeSymbol functionTypeSymbol = mock(FunctionTypeSymbol.class);
        when(functionTypeSymbol.params()).thenReturn(Optional.of(Collections.singletonList(paramSymbol)));
        when(functionTypeSymbol.returnTypeDescriptor()).thenReturn(Optional.empty());
        when(mockResourceMethod.typeDescriptor()).thenReturn(functionTypeSymbol);

        when(mockSemanticModel.moduleSymbols()).thenReturn(Collections.singletonList(mockResourceMethod));

        PackageCompilation mockCompilation = mock(PackageCompilation.class);
        when(mockCompilation.getSemanticModel(any())).thenReturn(mockSemanticModel);
        when(mockPackage.getCompilation()).thenReturn(mockCompilation);

        analyzer.analyze(mockPackage);

        Connector connector = Connector.getConnector();
        Assert.assertNotNull(connector);
    }

    @Test
    public void testAnalyze_ResourceFunctionWithNonMatchingPathParam() {
        BalModuleAnalyzer analyzer = new BalModuleAnalyzer();

        io.ballerina.projects.Package mockPackage = createMockPackage("testModule", "wso2", "1.0.0");
        SemanticModel mockSemanticModel = mock(SemanticModel.class);

        // Create a resource method where path param does NOT match any function parameter
        AnnotationSymbol operationAnnotation = mock(AnnotationSymbol.class);
        when(operationAnnotation.getName()).thenReturn(Optional.of("Operation"));

        ResourceMethodSymbol mockResourceMethod = mock(ResourceMethodSymbol.class);
        when(mockResourceMethod.kind()).thenReturn(SymbolKind.RESOURCE_METHOD);
        when(mockResourceMethod.annotations()).thenReturn(Collections.singletonList(operationAnnotation));
        when(mockResourceMethod.getName()).thenReturn(Optional.of("getNonMatching"));
        when(mockResourceMethod.documentation()).thenReturn(Optional.empty());
        when(mockResourceMethod.qualifiers()).thenReturn(Collections.singletonList(Qualifier.RESOURCE));

        // Create path segments with [string pathParam]
        PathSegment pathSegment = mock(PathSegment.class);
        when(pathSegment.signature()).thenReturn("[string pathParam]");

        PathSegmentList pathSegmentList = mock(PathSegmentList.class);
        when(pathSegmentList.list()).thenReturn(Collections.singletonList(pathSegment));
        when(mockResourceMethod.resourcePath()).thenReturn(pathSegmentList);

        // Create a parameter with different name (not matching pathParam)
        ParameterSymbol paramSymbol = mock(ParameterSymbol.class);
        when(paramSymbol.getName()).thenReturn(Optional.of("differentName"));
        TypeSymbol paramType = mock(TypeSymbol.class);
        when(paramType.typeKind()).thenReturn(TypeDescKind.STRING);
        when(paramSymbol.typeDescriptor()).thenReturn(paramType);

        FunctionTypeSymbol functionTypeSymbol = mock(FunctionTypeSymbol.class);
        when(functionTypeSymbol.params()).thenReturn(Optional.of(Collections.singletonList(paramSymbol)));
        when(functionTypeSymbol.returnTypeDescriptor()).thenReturn(Optional.empty());
        when(mockResourceMethod.typeDescriptor()).thenReturn(functionTypeSymbol);

        when(mockSemanticModel.moduleSymbols()).thenReturn(Collections.singletonList(mockResourceMethod));

        PackageCompilation mockCompilation = mock(PackageCompilation.class);
        when(mockCompilation.getSemanticModel(any())).thenReturn(mockSemanticModel);
        when(mockPackage.getCompilation()).thenReturn(mockCompilation);

        analyzer.analyze(mockPackage);

        Connector connector = Connector.getConnector();
        Assert.assertNotNull(connector);
    }

    @Test
    public void testAnalyze_ParameterWithoutName() {
        BalModuleAnalyzer analyzer = new BalModuleAnalyzer();

        io.ballerina.projects.Package mockPackage = createMockPackage("testModule", "wso2", "1.0.0");
        SemanticModel mockSemanticModel = mock(SemanticModel.class);

        AnnotationSymbol operationAnnotation = mock(AnnotationSymbol.class);
        when(operationAnnotation.getName()).thenReturn(Optional.of("Operation"));

        FunctionSymbol mockFunction = mock(FunctionSymbol.class);
        when(mockFunction.kind()).thenReturn(SymbolKind.FUNCTION);
        when(mockFunction.annotations()).thenReturn(Collections.singletonList(operationAnnotation));
        when(mockFunction.getName()).thenReturn(Optional.of("funcWithUnnamedParam"));
        when(mockFunction.documentation()).thenReturn(Optional.empty());
        when(mockFunction.qualifiers()).thenReturn(Collections.emptyList());

        // Create parameter without name
        ParameterSymbol paramSymbol = mock(ParameterSymbol.class);
        when(paramSymbol.getName()).thenReturn(Optional.empty()); // No name
        TypeSymbol paramType = mock(TypeSymbol.class);
        when(paramType.typeKind()).thenReturn(TypeDescKind.STRING);
        when(paramSymbol.typeDescriptor()).thenReturn(paramType);

        FunctionTypeSymbol functionTypeSymbol = mock(FunctionTypeSymbol.class);
        when(functionTypeSymbol.params()).thenReturn(Optional.of(Collections.singletonList(paramSymbol)));
        when(functionTypeSymbol.returnTypeDescriptor()).thenReturn(Optional.empty());
        when(mockFunction.typeDescriptor()).thenReturn(functionTypeSymbol);

        when(mockSemanticModel.moduleSymbols()).thenReturn(Collections.singletonList(mockFunction));

        PackageCompilation mockCompilation = mock(PackageCompilation.class);
        when(mockCompilation.getSemanticModel(any())).thenReturn(mockSemanticModel);
        when(mockPackage.getCompilation()).thenReturn(mockCompilation);

        analyzer.analyze(mockPackage);

        Connector connector = Connector.getConnector();
        Assert.assertNotNull(connector);
    }

    @Test
    public void testAnalyze_PathParamWithEmptyBrackets() {
        BalModuleAnalyzer analyzer = new BalModuleAnalyzer();

        io.ballerina.projects.Package mockPackage = createMockPackage("testModule", "wso2", "1.0.0");
        SemanticModel mockSemanticModel = mock(SemanticModel.class);

        // Edge case: path param with empty brackets
        ResourceMethodSymbol mockResourceMethod = createMockResourceMethod("emptyBrackets",
                Arrays.asList("[ ]")); // Nearly empty brackets

        when(mockSemanticModel.moduleSymbols()).thenReturn(Collections.singletonList(mockResourceMethod));

        PackageCompilation mockCompilation = mock(PackageCompilation.class);
        when(mockCompilation.getSemanticModel(any())).thenReturn(mockSemanticModel);
        when(mockPackage.getCompilation()).thenReturn(mockCompilation);

        analyzer.analyze(mockPackage);

        Connector connector = Connector.getConnector();
        Assert.assertNotNull(connector);
    }

    @Test
    public void testAnalyze_ResourceFunctionThrowsException() {
        BalModuleAnalyzer analyzer = new BalModuleAnalyzer();

        io.ballerina.projects.Package mockPackage = createMockPackage("testModule", "wso2", "1.0.0");
        SemanticModel mockSemanticModel = mock(SemanticModel.class);

        // Create a resource method that throws exception when getting resourcePath
        AnnotationSymbol operationAnnotation = mock(AnnotationSymbol.class);
        when(operationAnnotation.getName()).thenReturn(Optional.of("Operation"));

        ResourceMethodSymbol mockResourceMethod = mock(ResourceMethodSymbol.class);
        when(mockResourceMethod.kind()).thenReturn(SymbolKind.RESOURCE_METHOD);
        when(mockResourceMethod.annotations()).thenReturn(Collections.singletonList(operationAnnotation));
        when(mockResourceMethod.getName()).thenReturn(Optional.of("errorFunction"));
        when(mockResourceMethod.documentation()).thenReturn(Optional.empty());
        when(mockResourceMethod.qualifiers()).thenReturn(Collections.singletonList(Qualifier.RESOURCE));

        // Make resourcePath throw an exception
        when(mockResourceMethod.resourcePath()).thenThrow(new RuntimeException("Path extraction error"));

        FunctionTypeSymbol functionTypeSymbol = mock(FunctionTypeSymbol.class);
        when(functionTypeSymbol.params()).thenReturn(Optional.empty());
        when(functionTypeSymbol.returnTypeDescriptor()).thenReturn(Optional.empty());
        when(mockResourceMethod.typeDescriptor()).thenReturn(functionTypeSymbol);

        when(mockSemanticModel.moduleSymbols()).thenReturn(Collections.singletonList(mockResourceMethod));

        PackageCompilation mockCompilation = mock(PackageCompilation.class);
        when(mockCompilation.getSemanticModel(any())).thenReturn(mockSemanticModel);
        when(mockPackage.getCompilation()).thenReturn(mockCompilation);

        // Should not throw, should continue without path parameters
        analyzer.analyze(mockPackage);

        Connector connector = Connector.getConnector();
        Assert.assertNotNull(connector);
    }

    // Helper methods

    private io.ballerina.projects.Package createMockPackage(String moduleName, String orgName, String version) {
        io.ballerina.projects.Package mockPackage = mock(io.ballerina.projects.Package.class);
        PackageDescriptor mockDescriptor = mock(PackageDescriptor.class);
        PackageOrg mockOrg = mock(PackageOrg.class);
        PackageName mockName = mock(PackageName.class);
        PackageVersion mockVersion = mock(PackageVersion.class);

        when(mockOrg.value()).thenReturn(orgName);
        when(mockName.value()).thenReturn(moduleName);

        String[] versionParts = version.split("\\.");
        int major = versionParts.length > 0 ? Integer.parseInt(versionParts[0]) : 1;
        int minor = versionParts.length > 1 ? Integer.parseInt(versionParts[1]) : 0;
        int patch = versionParts.length > 2 ? Integer.parseInt(versionParts[2]) : 0;
        SemanticVersion semVer = SemanticVersion.from(major + "." + minor + "." + patch);
        when(mockVersion.value()).thenReturn(semVer);

        when(mockDescriptor.org()).thenReturn(mockOrg);
        when(mockDescriptor.name()).thenReturn(mockName);
        when(mockDescriptor.version()).thenReturn(mockVersion);
        when(mockPackage.descriptor()).thenReturn(mockDescriptor);

        io.ballerina.projects.Module mockDefaultModule = mock(io.ballerina.projects.Module.class);
        ModuleId mockModuleId = mock(ModuleId.class);
        when(mockDefaultModule.moduleId()).thenReturn(mockModuleId);
        when(mockPackage.getDefaultModule()).thenReturn(mockDefaultModule);

        return mockPackage;
    }

    private FunctionSymbol createMockFunctionWithOperationAnnotation(String functionName) {
        AnnotationSymbol operationAnnotation = mock(AnnotationSymbol.class);
        when(operationAnnotation.getName()).thenReturn(Optional.of("Operation"));

        FunctionSymbol mockFunction = mock(FunctionSymbol.class);
        when(mockFunction.kind()).thenReturn(SymbolKind.FUNCTION);
        when(mockFunction.annotations()).thenReturn(Collections.singletonList(operationAnnotation));
        when(mockFunction.getName()).thenReturn(Optional.of(functionName));
        when(mockFunction.documentation()).thenReturn(Optional.empty());
        when(mockFunction.qualifiers()).thenReturn(Collections.emptyList());

        FunctionTypeSymbol functionTypeSymbol = mock(FunctionTypeSymbol.class);
        when(functionTypeSymbol.params()).thenReturn(Optional.empty());
        when(functionTypeSymbol.returnTypeDescriptor()).thenReturn(Optional.empty());
        when(mockFunction.typeDescriptor()).thenReturn(functionTypeSymbol);

        return mockFunction;
    }

    private FunctionSymbol createMockFunctionWithParameters(String functionName, List<String> paramNames) {
        AnnotationSymbol operationAnnotation = mock(AnnotationSymbol.class);
        when(operationAnnotation.getName()).thenReturn(Optional.of("Operation"));

        FunctionSymbol mockFunction = mock(FunctionSymbol.class);
        when(mockFunction.kind()).thenReturn(SymbolKind.FUNCTION);
        when(mockFunction.annotations()).thenReturn(Collections.singletonList(operationAnnotation));
        when(mockFunction.getName()).thenReturn(Optional.of(functionName));
        when(mockFunction.documentation()).thenReturn(Optional.empty());
        when(mockFunction.qualifiers()).thenReturn(Collections.emptyList());

        List<ParameterSymbol> params = new ArrayList<>();
        for (String paramName : paramNames) {
            ParameterSymbol param = mock(ParameterSymbol.class);
            when(param.getName()).thenReturn(Optional.of(paramName));
            TypeSymbol paramType = mock(TypeSymbol.class);
            when(paramType.typeKind()).thenReturn(TypeDescKind.STRING);
            when(param.typeDescriptor()).thenReturn(paramType);
            params.add(param);
        }

        FunctionTypeSymbol functionTypeSymbol = mock(FunctionTypeSymbol.class);
        when(functionTypeSymbol.params()).thenReturn(Optional.of(params));
        when(functionTypeSymbol.returnTypeDescriptor()).thenReturn(Optional.empty());
        when(mockFunction.typeDescriptor()).thenReturn(functionTypeSymbol);

        return mockFunction;
    }

    private ResourceMethodSymbol createMockResourceMethod(String methodName, List<String> pathSegments) {
        AnnotationSymbol operationAnnotation = mock(AnnotationSymbol.class);
        when(operationAnnotation.getName()).thenReturn(Optional.of("Operation"));

        ResourceMethodSymbol mockResourceMethod = mock(ResourceMethodSymbol.class);
        when(mockResourceMethod.kind()).thenReturn(SymbolKind.RESOURCE_METHOD);
        when(mockResourceMethod.annotations()).thenReturn(Collections.singletonList(operationAnnotation));
        when(mockResourceMethod.getName()).thenReturn(Optional.of(methodName));
        when(mockResourceMethod.documentation()).thenReturn(Optional.empty());
        when(mockResourceMethod.qualifiers()).thenReturn(Collections.singletonList(Qualifier.RESOURCE));

        // Create path segments
        List<PathSegment> segments = new ArrayList<>();
        for (String seg : pathSegments) {
            PathSegment segment = mock(PathSegment.class);
            when(segment.signature()).thenReturn(seg);
            segments.add(segment);
        }

        PathSegmentList pathSegmentList = mock(PathSegmentList.class);
        when(pathSegmentList.list()).thenReturn(segments);
        when(mockResourceMethod.resourcePath()).thenReturn(pathSegmentList);

        FunctionTypeSymbol functionTypeSymbol = mock(FunctionTypeSymbol.class);
        when(functionTypeSymbol.params()).thenReturn(Optional.empty());
        when(functionTypeSymbol.returnTypeDescriptor()).thenReturn(Optional.empty());
        when(mockResourceMethod.typeDescriptor()).thenReturn(functionTypeSymbol);

        return mockResourceMethod;
    }
}
