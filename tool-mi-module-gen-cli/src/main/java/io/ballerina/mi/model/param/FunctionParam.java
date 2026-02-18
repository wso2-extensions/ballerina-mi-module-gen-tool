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

package io.ballerina.mi.model.param;

import io.ballerina.compiler.api.symbols.ParameterKind;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.mi.util.Utils;

public class FunctionParam extends Param {

    private final String paramType;
    private ParameterKind paramKind;
    private TypeSymbol typeSymbol;
    private boolean required;
    private String enableCondition;
    private String defaultValue;
    private String displayTypeName;  // For unions/records, stores the actual type name (e.g., "Xgafv" instead of "union")

    // Pre-computed values from TypeSymbol to allow releasing the heavy compiler references
    private TypeDescKind resolvedTypeKind;
    private String resolvedTypeName;

    public FunctionParam(String index, String name, String paramType) {
        super(index, name);
        this.paramType = paramType;
        this.required = true;
        this.defaultValue = "";
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
        if (typeSymbol != null) {
            this.resolvedTypeKind = Utils.getActualTypeKind(typeSymbol);
            this.resolvedTypeName = typeSymbol.getName().orElse(null);
        }
    }

    public TypeDescKind getResolvedTypeKind() {
        return resolvedTypeKind;
    }

    public String getResolvedTypeName() {
        return resolvedTypeName;
    }

    /**
     * Clears the TypeSymbol reference to allow the Ballerina compiler's semantic model
     * to be garbage collected. Pre-computed values (resolvedTypeKind, resolvedTypeName)
     * remain available. Should be called before serialization.
     */
    public void clearTypeSymbol() {
        this.typeSymbol = null;
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

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getDisplayTypeName() {
        return displayTypeName;
    }

    public void setDisplayTypeName(String displayTypeName) {
        this.displayTypeName = displayTypeName;
    }

    /**
     * Accepts a visitor for double-dispatch (Visitor Pattern).
     * Subclasses override this to dispatch to the correct visit() overload.
     *
     * @param visitor The visitor to accept
     */
    public void accept(FunctionParamVisitor visitor) {
        visitor.visit(this);
    }
}
