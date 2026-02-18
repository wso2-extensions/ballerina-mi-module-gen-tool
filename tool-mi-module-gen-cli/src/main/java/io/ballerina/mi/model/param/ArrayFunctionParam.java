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

package io.ballerina.mi.model.param;

import io.ballerina.compiler.api.symbols.ArrayTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.mi.util.Utils;

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
    private boolean is2DArray;
    private boolean isUnionArray;
    private List<String> unionMemberTypeNames;
    private TypeSymbol innerElementTypeSymbol;

    // Pre-computed values from TypeSymbol
    private TypeDescKind elementTypeKind;
    private TypeDescKind innerElementTypeKind;
    private String arrayElementTypeName;

    public ArrayFunctionParam(String index, String name, String paramType) {
        super(index, name, paramType);
        this.elementFieldParams = new ArrayList<>();
        this.renderAsTable = false;
        this.is2DArray = false;
        this.isUnionArray = false;
        this.unionMemberTypeNames = new ArrayList<>();
    }

    public TypeSymbol getElementTypeSymbol() {
        return elementTypeSymbol;
    }

    public void setElementTypeSymbol(TypeSymbol elementTypeSymbol) {
        this.elementTypeSymbol = elementTypeSymbol;
        if (elementTypeSymbol != null) {
            this.elementTypeKind = Utils.getActualTypeKind(elementTypeSymbol);
        }
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

    public boolean is2DArray() {
        return is2DArray;
    }

    public void set2DArray(boolean is2DArray) {
        this.is2DArray = is2DArray;
    }

    public boolean isUnionArray() {
        return isUnionArray;
    }

    public void setUnionArray(boolean isUnionArray) {
        this.isUnionArray = isUnionArray;
    }

    public List<String> getUnionMemberTypeNames() {
        return unionMemberTypeNames;
    }

    public void setUnionMemberTypeNames(List<String> unionMemberTypeNames) {
        this.unionMemberTypeNames = unionMemberTypeNames;
    }

    public TypeSymbol getInnerElementTypeSymbol() {
        return innerElementTypeSymbol;
    }

    public void setInnerElementTypeSymbol(TypeSymbol innerElementTypeSymbol) {
        this.innerElementTypeSymbol = innerElementTypeSymbol;
        if (innerElementTypeSymbol != null) {
            this.innerElementTypeKind = Utils.getActualTypeKind(innerElementTypeSymbol);
        }
    }

    public TypeDescKind getElementTypeKind() {
        return elementTypeKind;
    }

    public TypeDescKind getInnerElementTypeKind() {
        return innerElementTypeKind;
    }

    public String getArrayElementTypeName() {
        return arrayElementTypeName;
    }

    /**
     * Pre-computes the array element type name from the main TypeSymbol.
     * Must be called after setTypeSymbol().
     */
    public void preComputeArrayElementType() {
        TypeSymbol ts = getTypeSymbol();
        if (ts == null) return;
        TypeSymbol actual = Utils.getActualTypeSymbol(ts);
        if (actual instanceof ArrayTypeSymbol arrayTs) {
            TypeSymbol memberType = arrayTs.memberTypeDescriptor();
            TypeDescKind memberKind = Utils.getActualTypeKind(memberType);
            this.arrayElementTypeName = Utils.getParamTypeName(memberKind);
        }
    }

    @Override
    public void clearTypeSymbol() {
        super.clearTypeSymbol();
        this.elementTypeSymbol = null;
        this.innerElementTypeSymbol = null;
    }

    @Override
    public void accept(FunctionParamVisitor visitor) {
        visitor.visit(this);
    }
}
