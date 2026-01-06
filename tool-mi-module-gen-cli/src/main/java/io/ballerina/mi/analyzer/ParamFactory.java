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
import io.ballerina.compiler.api.symbols.ParameterKind;
import io.ballerina.compiler.api.symbols.ParameterSymbol;
import io.ballerina.compiler.api.symbols.RecordFieldSymbol;
import io.ballerina.compiler.api.symbols.RecordTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.mi.connectorModel.FunctionParam;
import io.ballerina.mi.connectorModel.RecordFunctionParam;
import io.ballerina.mi.connectorModel.UnionFunctionParam;
import io.ballerina.mi.util.Utils;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
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
            if (actualTypeKind == TypeDescKind.RECORD) {
                return createRecordFunctionParam(parameterSymbol, index);
            }
            FunctionParam functionParam = new FunctionParam(Integer.toString(index), parameterSymbol.getName().orElseThrow(), paramType);
            functionParam.setParamKind(parameterSymbol.paramKind());
            functionParam.setTypeSymbol(typeSymbol);
            return Optional.of(functionParam);
        }
        return Optional.empty();
    }

    private static Optional<FunctionParam> createRecordFunctionParam(ParameterSymbol parameterSymbol, int index) {
        String paramName = parameterSymbol.getName().orElseThrow();
        TypeSymbol typeSymbol = parameterSymbol.typeDescriptor();
        TypeSymbol actualTypeSymbol = Utils.getActualTypeSymbol(typeSymbol);

        RecordFunctionParam recordParam = new RecordFunctionParam(Integer.toString(index), paramName, TypeDescKind.RECORD.getName());
        recordParam.setParamKind(parameterSymbol.paramKind());
        recordParam.setTypeSymbol(typeSymbol);
        recordParam.setRecordName(actualTypeSymbol.getName().orElse(paramName));

        // Set required based on parameter kind
        if (parameterSymbol.paramKind() == ParameterKind.DEFAULTABLE) {
            recordParam.setRequired(false);
        }

        // Extract record fields if the actual type is a RecordTypeSymbol
        if (actualTypeSymbol instanceof RecordTypeSymbol recordTypeSymbol) {
            // For top-level record params, use empty parent path so field names don't include the param name
            // For example, fields will be "authConfig.token" instead of "config.authConfig.token"
            String parentPath = "";  // Top-level record should not include param name in field paths
            recordParam.setParentParamPath("");  // Top-level has no parent
            populateRecordFieldParams(recordParam, recordTypeSymbol, parentPath);
        }

        return Optional.of(recordParam);
    }

    private static void populateRecordFieldParams(RecordFunctionParam recordParam, RecordTypeSymbol recordTypeSymbol, String parentPath) {
        Map<String, RecordFieldSymbol> fieldDescriptors = recordTypeSymbol.fieldDescriptors();
        int fieldIndex = 0;

        for (Map.Entry<String, RecordFieldSymbol> entry : fieldDescriptors.entrySet()) {
            String fieldName = entry.getKey();
            RecordFieldSymbol fieldSymbol = entry.getValue();
            TypeSymbol fieldTypeSymbol = fieldSymbol.typeDescriptor();
            TypeDescKind fieldTypeKind = Utils.getActualTypeKind(fieldTypeSymbol);
            String fieldType = Utils.getParamTypeName(fieldTypeKind);

            if (fieldType != null) {
                FunctionParam fieldParam;

                // Build qualified name for this field
                String qualifiedFieldName = buildQualifiedName(parentPath, fieldName);

                // Handle different field types appropriately
                if (fieldTypeKind == TypeDescKind.UNION) {
                    // Create UnionFunctionParam for union type fields
                    UnionFunctionParam unionFieldParam = new UnionFunctionParam(Integer.toString(fieldIndex), qualifiedFieldName, fieldType);
                    unionFieldParam.setTypeSymbol(fieldTypeSymbol);
                    TypeSymbol actualTypeSymbol = Utils.getActualTypeSymbol(fieldTypeSymbol);
                    if (actualTypeSymbol instanceof BallerinaUnionTypeSymbol ballerinaUnionTypeSymbol) {
                        populateUnionMemberParams(fieldName, ballerinaUnionTypeSymbol, unionFieldParam);
                    }
                    // Skip empty unions (all members are nil or unsupported types)
                    if (unionFieldParam.getUnionMemberParams().isEmpty()) {
                        continue;
                    }
                    fieldParam = unionFieldParam;
                } else if (fieldTypeKind == TypeDescKind.RECORD) {
                    // Create RecordFunctionParam for nested record fields
                    TypeSymbol actualTypeSymbol = Utils.getActualTypeSymbol(fieldTypeSymbol);
                    RecordFunctionParam nestedRecordParam = new RecordFunctionParam(Integer.toString(fieldIndex), qualifiedFieldName, fieldType);
                    nestedRecordParam.setTypeSymbol(fieldTypeSymbol);
                    nestedRecordParam.setRecordName(actualTypeSymbol.getName().orElse(fieldName));
                    nestedRecordParam.setParentParamPath(parentPath);
                    if (actualTypeSymbol instanceof RecordTypeSymbol nestedRecordTypeSymbol) {
                        // Recursive call with extended parent path
                        String nestedParentPath = buildQualifiedName(parentPath, fieldName);
                        populateRecordFieldParams(nestedRecordParam, nestedRecordTypeSymbol, nestedParentPath);
                    }
                    fieldParam = nestedRecordParam;
                } else {
                    fieldParam = new FunctionParam(Integer.toString(fieldIndex), qualifiedFieldName, fieldType);
                    fieldParam.setTypeSymbol(fieldTypeSymbol);
                }

                // Check if field is optional (has ? suffix) or has a default value
                boolean isOptional = fieldSymbol.isOptional() || fieldSymbol.hasDefaultValue();
                fieldParam.setRequired(!isOptional);

                // Get field description from documentation if available
                fieldSymbol.documentation().ifPresent(doc ->
                    doc.description().ifPresent(fieldParam::setDescription));

                recordParam.addRecordFieldParam(fieldParam);
                fieldIndex++;
            }
        }
    }

    private static Optional<FunctionParam> createUnionFunctionParam(ParameterSymbol parameterSymbol, int index) {
        String paramName = parameterSymbol.getName().orElseThrow();
        UnionFunctionParam functionParam = new UnionFunctionParam(Integer.toString(index), paramName, TypeDescKind.UNION.getName());
        functionParam.setParamKind(parameterSymbol.paramKind());
        TypeSymbol typeSymbol = parameterSymbol.typeDescriptor();
        functionParam.setTypeSymbol(typeSymbol);
        TypeSymbol actualTypeSymbol = Utils.getActualTypeSymbol(typeSymbol);
        if (actualTypeSymbol instanceof BallerinaUnionTypeSymbol ballerinaUnionTypeSymbol) {
            populateUnionMemberParams(paramName, ballerinaUnionTypeSymbol, functionParam);
        }
        // Skip empty unions (all members are nil or unsupported types)
        if (functionParam.getUnionMemberParams().isEmpty()) {
            return Optional.empty();
        }
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
                        // Use record name if available, otherwise use generic "Record" + index
                        actualParamType = memberTypeSymbol.getName().orElse("Record" + memberIndex);
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

    /**
     * Builds qualified parameter name by combining parent path and field name.
     *
     * @param parentPath Parent path (may be empty for top-level)
     * @param fieldName Current field name
     * @return Qualified name using dot notation (e.g., "config.host")
     */
    private static String buildQualifiedName(String parentPath, String fieldName) {
        if (parentPath == null || parentPath.isEmpty()) {
            return fieldName;
        }
        return parentPath + "." + fieldName;
    }
}
