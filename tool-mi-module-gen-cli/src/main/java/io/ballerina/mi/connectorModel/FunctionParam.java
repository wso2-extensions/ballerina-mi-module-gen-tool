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

import io.ballerina.compiler.api.symbols.ParameterKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;

public class FunctionParam extends Param {

    private final String paramType;
    private ParameterKind paramKind;
    private TypeSymbol typeSymbol;
    private boolean required;
    private String enableCondition;

    public FunctionParam(String index, String name, String paramType) {
        super(index, name);
        this.paramType = paramType;
        this.required = true;
    }

    public String getParamType() {
        return paramType;
    }

    public void setParamKind(ParameterKind paramKind) {
        this.paramKind = paramKind;
    }

    public ParameterKind getParamKind() {
        return paramKind;
    }

    public TypeSymbol getTypeSymbol() {
        return typeSymbol;
    }

    public void setTypeSymbol(TypeSymbol typeSymbol) {
        this.typeSymbol = typeSymbol;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public String getEnableCondition() {
        return enableCondition;
    }

    public void setEnableCondition(String enableCondition) {
        this.enableCondition = enableCondition;
    }
}
