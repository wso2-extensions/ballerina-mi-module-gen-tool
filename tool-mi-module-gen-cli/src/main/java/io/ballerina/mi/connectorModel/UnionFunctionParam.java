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

import java.util.ArrayList;
import java.util.List;

public class UnionFunctionParam extends FunctionParam {

    private List<FunctionParam> unionMemberParams;

    public UnionFunctionParam(String index, String name, String paramType) {
        super(index, name, paramType);
        unionMemberParams = new ArrayList<>();
    }

    public List<FunctionParam> getUnionMemberParams() {
        return unionMemberParams;
    }

    public void setUnionMemberParams(List<FunctionParam> unionMemberParams) {
        this.unionMemberParams = unionMemberParams;
    }

    public void addUnionMemberParam(FunctionParam functionParam) {
        functionParam.setRequired(isRequired());
        this.unionMemberParams.add(functionParam);
    }

    @Override
    public void setRequired(boolean required) {
        super.setRequired(required);
        for (FunctionParam memberParam : unionMemberParams) {
            memberParam.setRequired(required);
        }
    }
}
