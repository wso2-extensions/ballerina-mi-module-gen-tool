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

package io.ballerina.mi.connectorModel;

import io.ballerina.compiler.api.symbols.TypeSymbol;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a function parameter that is an array type.
 * This class stores the element type information and the fields of record-typed elements
 * so they can be displayed as a table UI with columns in the MI Studio.
 *
 * @since 0.5.0
 */
public class ArrayFunctionParam extends FunctionParam {

    private TypeSymbol elementTypeSymbol;
    private List<FunctionParam> elementFieldParams;
    private boolean renderAsTable;

    public ArrayFunctionParam(String index, String name, String paramType) {
        super(index, name, paramType);
        this.elementFieldParams = new ArrayList<>();
        this.renderAsTable = false;
    }

    public TypeSymbol getElementTypeSymbol() {
        return elementTypeSymbol;
    }

    public void setElementTypeSymbol(TypeSymbol elementTypeSymbol) {
        this.elementTypeSymbol = elementTypeSymbol;
    }

    public List<FunctionParam> getElementFieldParams() {
        return elementFieldParams;
    }

    public void setElementFieldParams(List<FunctionParam> elementFieldParams) {
        this.elementFieldParams = elementFieldParams;
    }

    public void addElementFieldParam(FunctionParam functionParam) {
        this.elementFieldParams.add(functionParam);
    }

    public boolean isRenderAsTable() {
        return renderAsTable;
    }

    public void setRenderAsTable(boolean renderAsTable) {
        this.renderAsTable = renderAsTable;
    }
}
