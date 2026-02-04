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

package io.ballerina.mi.connectorModel;

import io.ballerina.compiler.api.symbols.TypeSymbol;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a function parameter that is a map type.
 * This class stores the key and value type information and the fields of record-typed values
 * so they can be displayed as a table UI with columns in the MI Studio.
 *
 * @since 0.5.0
 */
public class MapFunctionParam extends FunctionParam {

    private TypeSymbol keyTypeSymbol;
    private TypeSymbol valueTypeSymbol;
    private List<FunctionParam> valueFieldParams;
    private boolean renderAsTable;

    public MapFunctionParam(String index, String name, String paramType) {
        super(index, name, paramType);
        this.valueFieldParams = new ArrayList<>();
        this.renderAsTable = false;
    }

    public TypeSymbol getKeyTypeSymbol() {
        return keyTypeSymbol;
    }

    public void setKeyTypeSymbol(TypeSymbol keyTypeSymbol) {
        this.keyTypeSymbol = keyTypeSymbol;
    }

    public TypeSymbol getValueTypeSymbol() {
        return valueTypeSymbol;
    }

    public void setValueTypeSymbol(TypeSymbol valueTypeSymbol) {
        this.valueTypeSymbol = valueTypeSymbol;
    }

    public List<FunctionParam> getValueFieldParams() {
        return valueFieldParams;
    }

    public void setValueFieldParams(List<FunctionParam> valueFieldParams) {
        this.valueFieldParams = valueFieldParams;
    }

    public void addValueFieldParam(FunctionParam functionParam) {
        this.valueFieldParams.add(functionParam);
    }

    public boolean isRenderAsTable() {
        return renderAsTable;
    }

    public void setRenderAsTable(boolean renderAsTable) {
        this.renderAsTable = renderAsTable;
    }
}
