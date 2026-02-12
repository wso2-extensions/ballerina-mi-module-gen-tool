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

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a function parameter that is a record type.
 * This class stores the individual fields of the record so they can be
 * displayed as separate input fields in the UI instead of a single opaque field.
 *
 * @since 0.5.0
 */
public class RecordFunctionParam extends FunctionParam {

    private List<FunctionParam> recordFieldParams;
    private String recordName;
    private String parentParamPath;

    public RecordFunctionParam(String index, String name, String paramType) {
        super(index, name, paramType);
        recordFieldParams = new ArrayList<>();
    }

    public List<FunctionParam> getRecordFieldParams() {
        return recordFieldParams;
    }

    public void setRecordFieldParams(List<FunctionParam> recordFieldParams) {
        this.recordFieldParams = recordFieldParams;
    }

    public void addRecordFieldParam(FunctionParam functionParam) {
        this.recordFieldParams.add(functionParam);
    }

    public String getRecordName() {
        return recordName;
    }

    public void setRecordName(String recordName) {
        this.recordName = recordName;
    }

    public String getParentParamPath() {
        return parentParamPath;
    }

    public void setParentParamPath(String parentParamPath) {
        this.parentParamPath = parentParamPath;
    }

    @Override
    public void accept(FunctionParamVisitor visitor) {
        visitor.visit(this);
    }
}
