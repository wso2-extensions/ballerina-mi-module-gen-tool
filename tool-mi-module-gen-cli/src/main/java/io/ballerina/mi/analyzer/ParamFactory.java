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

import io.ballerina.compiler.api.impl.symbols.BallerinaUnionTypeSymbol;
import io.ballerina.compiler.api.symbols.ParameterSymbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.mi.connectorModel.FunctionParam;
import io.ballerina.mi.connectorModel.UnionFunctionParam;
import io.ballerina.mi.util.Utils;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

/**
 * Factory class responsible for creating {@link FunctionParam} instances from Ballerina
 * {@link ParameterSymbol} objects. This factory handles the conversion of various parameter
 * types including simple types and union types.
 *
 * <p>The factory analyzes the type descriptor of each parameter and creates appropriate
 * FunctionParam instances, with special handling for union types that may contain multiple
 * member types.</p>
 *
 * @since 0.4.3
 */
public class ParamFactory {

    /**
     * Creates a {@link FunctionParam} from a {@link ParameterSymbol}.
     *
     * <p>This method analyzes the parameter's type descriptor and creates an appropriate
     * FunctionParam instance.
     *
     * @param parameterSymbol the Ballerina parameter symbol containing type and name information
     * @param index           the zero-based index position of the parameter in the function signature
     * @return an {@link Optional} containing the created {@link FunctionParam} if the parameter
     * type is supported, or {@link Optional#empty()} if the type is not supported
     */
    public static Optional<FunctionParam> createFunctionParam(ParameterSymbol parameterSymbol, int index) {
        TypeSymbol typeSymbol = parameterSymbol.typeDescriptor();
        TypeDescKind actualTypeKind = Utils.getActualTypeKind(typeSymbol);
        String paramType = Utils.getParamTypeName(actualTypeKind);

        if (paramType != null) {
            if (actualTypeKind == TypeDescKind.UNION) {
                return createUnionFunctionParam(parameterSymbol, index);
            }
            FunctionParam functionParam = new FunctionParam(Integer.toString(index), parameterSymbol.getName().orElseThrow(), paramType);
            functionParam.setParamKind(parameterSymbol.paramKind());
            functionParam.setTypeSymbol(typeSymbol);
            return Optional.of(functionParam);
        }
        return Optional.empty();
    }

    private static Optional<FunctionParam> createUnionFunctionParam(ParameterSymbol parameterSymbol, int index) {
        String paramName = parameterSymbol.getName().orElseThrow();
        UnionFunctionParam functionParam = new UnionFunctionParam(Integer.toString(index), paramName, TypeDescKind.UNION.getName());
        functionParam.setParamKind(parameterSymbol.paramKind());
        functionParam.setTypeSymbol(parameterSymbol.typeDescriptor());
        populateUnionMemberParams(paramName, (BallerinaUnionTypeSymbol) parameterSymbol.typeDescriptor(), functionParam);
        return Optional.of(functionParam);
    }

    private static void populateUnionMemberParams(String paramName, BallerinaUnionTypeSymbol ballerinaUnionTypeSymbol, UnionFunctionParam functionParam) {
        int memberIndex = 0;
        for (TypeSymbol memberTypeSymbol : ballerinaUnionTypeSymbol.memberTypeDescriptors()) {
            TypeDescKind actualTypeKind = Utils.getActualTypeKind(memberTypeSymbol);
            String paramType = Utils.getParamTypeName(actualTypeKind);
            if (paramType != null) {
                if (TypeDescKind.NIL.getName().equals(paramType)) {
                    functionParam.setRequired(false);
                } else {
                    String actualParamType;
                    if (TypeDescKind.RECORD.getName().equals(paramType)) {
                        actualParamType = memberTypeSymbol.getName().orElseThrow();
                    } else {
                        actualParamType = paramType;
                    }
                    String memberParamName = paramName + StringUtils.capitalize(actualParamType);
                    FunctionParam memberParam = new FunctionParam(Integer.toString(memberIndex), memberParamName, paramType);
                    memberParam.setTypeSymbol(memberTypeSymbol);
                    memberParam.setEnableCondition("[{\"" + paramName + "DataType\": \"" + actualParamType + "\"}]");
                    functionParam.addUnionMemberParam(memberParam);
                    memberIndex++;
                }
            }
        }
    }
}
