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
 
package io.ballerina.mi.analyzer;

import io.ballerina.compiler.api.symbols.*;

import io.ballerina.mi.connectorModel.*;
import io.ballerina.mi.util.Utils;
import io.ballerina.projects.Package;

import java.io.PrintStream;
import java.util.*;

public class BalModuleAnalyzer implements Analyzer {

    private final PrintStream printStream;

    public BalModuleAnalyzer() {
        this.printStream = System.out;
    }

    @Override
    public void analyze(Package compilePackage) {

        Connector connector = Connector.getConnector(compilePackage.descriptor());
        connector.setBalModule(true);
        // Get all symbols from the module and filter for functions
        Collection<Symbol> allSymbols = compilePackage.getCompilation().getSemanticModel(compilePackage.getDefaultModule().moduleId()).moduleSymbols();
        for (Symbol symbol : allSymbols) {
            if (symbol.kind() == SymbolKind.FUNCTION && symbol instanceof FunctionSymbol functionSymbol) {
                analyzeFunctionForMIOperation(functionSymbol, connector);
            }
        }
    }

    private void analyzeFunctionForMIOperation(FunctionSymbol functionSymbol, Connector connector) {
        // Check if function has @mi:Operation annotation
        List<AnnotationSymbol> annotations = functionSymbol.annotations();

        boolean hasOperationAnnotation = false;
        for (AnnotationSymbol annotationSymbol : annotations) {
            Optional<String> annotationName = annotationSymbol.getName();
            if (annotationName.isPresent() && annotationName.get().equals("Operation")) {
                hasOperationAnnotation = true;
                break;
            }
        }

        if (!hasOperationAnnotation) {
            return;
        }

        Optional<String> functionName = functionSymbol.getName();
        if (functionName.isEmpty()) {
            return;
        }

        printStream.println("Found MI operation: " + functionName.get());

        Connection connection = new Connection(connector, null, null, null);
        // Create component
        Optional<Documentation> documentation = functionSymbol.documentation();
        String documentationString = documentation.map(Utils::getDocString).orElse("");
        FunctionType functionType = Utils.getFunctionType(functionSymbol);
        String returnTypeName = Utils.getReturnTypeName(functionSymbol);

//        List<PathParamType> pathParams = new ArrayList<>(GeneratorUtils.getPathParameters(
//                functionSymbol. .relativeResourcePath()));
//        List<Type> queryParams = new ArrayList<>(GeneratorUtils.getFunctionParameters(
//                functionSignature.parameters(),
//                functionDefinition.metadata(), semanticModel));

        Component component = new Component(functionName.get(), documentationString, functionType, "0", Collections.emptyList(), Collections.emptyList(), returnTypeName);

        // Extract parameters
        int noOfParams = 0;
        Optional<List<ParameterSymbol>> params = functionSymbol.typeDescriptor().params();
        if (params.isPresent()) {
            List<ParameterSymbol> parameterSymbols = params.get();
            noOfParams = parameterSymbols.size();

            for (int i = 0; i < noOfParams; i++) {
                ParamFactory.createFunctionParam(parameterSymbols.get(i), i).ifPresent(component::setFunctionParam);
            }
        }

        Param sizeParam = new Param("paramSize", Integer.toString(noOfParams));
        Param functionNameParam = new Param("paramFunctionName", component.getName());
        component.setParam(sizeParam);
        component.setParam(functionNameParam);

        connection.setComponent(component);
        connector.setConnection(connection);
    }
}
