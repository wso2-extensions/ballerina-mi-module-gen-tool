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
import io.ballerina.compiler.api.symbols.*;
import io.ballerina.mi.model.param.*;
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
            if (actualTypeKind == TypeDescKind.MAP) {
                return createMapFunctionParam(parameterSymbol, index);
            }
            if (actualTypeKind == TypeDescKind.ARRAY) {
                return createArrayFunctionParam(parameterSymbol, index);
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
        // Try to get name from the original type symbol first (for type references like ConnectionConfig)
        // Fall back to actual type symbol name, then parameter name
        String recordName = typeSymbol.getName()
                .or(() -> actualTypeSymbol.getName())
                .orElse(paramName);
        recordParam.setRecordName(recordName);

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
            populateRecordFieldParams(recordParam, recordTypeSymbol, parentPath, new int[]{MAX_FIELD_BUDGET});
        }

        return Optional.of(recordParam);
    }

    // Maximum total field count per record tree to prevent OOM from exponential expansion.
    // Records like OAuth2 ConnectionConfig have recursive types (ClientConfiguration → auth → ClientConfiguration).
    // Without a budget, depth 20 can produce millions of fields. With a budget of 200 fields,
    // we get full expansion for ~2-3 levels of nesting (practical for user configuration) then stop.
    // Kept low to prevent OOM on large connectors (e.g. Gmail with 32 components × multiple record params).
    private static final int MAX_FIELD_BUDGET = 200;

    private static void populateRecordFieldParams(RecordFunctionParam recordParam, RecordTypeSymbol recordTypeSymbol, String parentPath, int[] fieldBudget) {
        // Budget exhausted - stop expanding. Remaining nested records are left as opaque fields.
        if (fieldBudget[0] <= 0) {
            return;
        }

        Map<String, RecordFieldSymbol> fieldDescriptors = recordTypeSymbol.fieldDescriptors();
        int fieldIndex = 0;

        for (Map.Entry<String, RecordFieldSymbol> entry : fieldDescriptors.entrySet()) {
            String fieldName = entry.getKey();
            RecordFieldSymbol fieldSymbol = entry.getValue();
            TypeSymbol fieldTypeSymbol = fieldSymbol.typeDescriptor();
            TypeDescKind fieldTypeKind = Utils.getActualTypeKind(fieldTypeSymbol);
            String fieldType = Utils.getParamTypeName(fieldTypeKind);

            if (fieldType != null) {
                // Decrement budget for each field we create
                fieldBudget[0]--;
                if (fieldBudget[0] < 0) {
                    return; // Budget exhausted mid-record
                }

                FunctionParam fieldParam;

                // Build qualified name for this field
                String qualifiedFieldName = buildQualifiedName(parentPath, fieldName);

                // Check if field is optional (has ? suffix) or has a default value
                boolean isOptional = fieldSymbol.isOptional() || fieldSymbol.hasDefaultValue();
                // Propagate parent's optionality: if parent is optional (not required), this field is also optional
                boolean isEffectiveRequired = !isOptional && recordParam.isRequired();

                // Handle different field types appropriately
                if (fieldTypeKind == TypeDescKind.UNION) {
                    // Create UnionFunctionParam for union type fields
                    UnionFunctionParam unionFieldParam = new UnionFunctionParam(Integer.toString(fieldIndex), qualifiedFieldName, fieldType);
                    unionFieldParam.setTypeSymbol(fieldTypeSymbol);
                    // Set required status BEFORE recursion so it propagates to members
                    unionFieldParam.setRequired(isEffectiveRequired);

                    TypeSymbol actualTypeSymbol = Utils.getActualTypeSymbol(fieldTypeSymbol);
                    if (actualTypeSymbol instanceof BallerinaUnionTypeSymbol ballerinaUnionTypeSymbol) {
                        // Use qualifiedFieldName to ensure enable conditions are properly scoped for nested fields
                        populateUnionMemberParams(qualifiedFieldName, ballerinaUnionTypeSymbol, unionFieldParam, fieldBudget);
                    }
                    // Skip empty unions (all members are nil or unsupported types)
                    if (unionFieldParam.getUnionMemberParams().isEmpty()) {
                        fieldBudget[0]++; // Refund the budget for skipped field
                        continue;
                    }
                    // If there's only one non-nil member, convert to a regular FunctionParam instead of UnionFunctionParam
                    if (unionFieldParam.getUnionMemberParams().size() == 1 && !(unionFieldParam.getUnionMemberParams().getFirst() instanceof UnionFunctionParam)) {
                        FunctionParam singleMember = unionFieldParam.getUnionMemberParams().getFirst();
                        fieldParam = new FunctionParam(Integer.toString(fieldIndex), qualifiedFieldName, singleMember.getParamType());
                        fieldParam.setTypeSymbol(singleMember.getTypeSymbol());
                        fieldParam.setRequired(unionFieldParam.isRequired());
                    } else {
                        fieldParam = unionFieldParam;
                    }
                } else if (fieldTypeKind == TypeDescKind.RECORD) {
                    // Create RecordFunctionParam for nested record fields
                    TypeSymbol actualTypeSymbol = Utils.getActualTypeSymbol(fieldTypeSymbol);
                    RecordFunctionParam nestedRecordParam = new RecordFunctionParam(Integer.toString(fieldIndex), qualifiedFieldName, fieldType);
                    nestedRecordParam.setTypeSymbol(fieldTypeSymbol);
                    nestedRecordParam.setRecordName(actualTypeSymbol.getName().orElse(fieldName));
                    nestedRecordParam.setParentParamPath(parentPath);
                    // Set required status BEFORE recursion so it propagates to children
                    nestedRecordParam.setRequired(isEffectiveRequired);

                    if (actualTypeSymbol instanceof RecordTypeSymbol nestedRecordTypeSymbol) {
                        String nestedParentPath = buildQualifiedName(parentPath, fieldName);
                        populateRecordFieldParams(nestedRecordParam, nestedRecordTypeSymbol, nestedParentPath, fieldBudget);
                    }
                    fieldParam = nestedRecordParam;
                } else if (fieldTypeKind == TypeDescKind.MAP) {
                    TypeSymbol actualFieldType = Utils.getActualTypeSymbol(fieldTypeSymbol);
                    MapFunctionParam mapFieldParam = new MapFunctionParam(
                        Integer.toString(fieldIndex), qualifiedFieldName, fieldType);
                    mapFieldParam.setTypeSymbol(fieldTypeSymbol);
                    mapFieldParam.setRequired(isEffectiveRequired);

                    if (actualFieldType instanceof MapTypeSymbol mapTypeSymbol) {
                        Optional<TypeSymbol> valueTypeOpt = mapTypeSymbol.typeParameter();
                        if (valueTypeOpt.isPresent()) {
                            TypeSymbol valueType = valueTypeOpt.get();
                            mapFieldParam.setValueTypeSymbol(valueType);
                            TypeDescKind valueTypeKind = Utils.getActualTypeKind(valueType);
                            boolean shouldRender = shouldMapRenderAsTable(valueType, valueTypeKind);
                            mapFieldParam.setRenderAsTable(shouldRender);
                            if (shouldRender && valueTypeKind == TypeDescKind.RECORD) {
                                TypeSymbol actualValueType = Utils.getActualTypeSymbol(valueType);
                                if (actualValueType instanceof RecordTypeSymbol recordType) {
                                    populateMapValueFields(mapFieldParam, recordType);
                                }
                            }
                        }
                    }
                    fieldParam = mapFieldParam;
                } else if (fieldTypeKind == TypeDescKind.ARRAY) {
                    TypeSymbol actualFieldType = Utils.getActualTypeSymbol(fieldTypeSymbol);
                    ArrayFunctionParam arrayFieldParam = new ArrayFunctionParam(
                        Integer.toString(fieldIndex), qualifiedFieldName, fieldType);
                    arrayFieldParam.setTypeSymbol(fieldTypeSymbol);
                    arrayFieldParam.setRequired(isEffectiveRequired);

                    if (actualFieldType instanceof ArrayTypeSymbol arrayTypeSymbol) {
                        TypeSymbol elementType = arrayTypeSymbol.memberTypeDescriptor();
                        arrayFieldParam.setElementTypeSymbol(elementType);
                        TypeDescKind elementTypeKind = Utils.getActualTypeKind(elementType);
                        boolean shouldRender = shouldArrayRenderAsTable(elementType, elementTypeKind);
                        arrayFieldParam.setRenderAsTable(shouldRender);
                        if (shouldRender && elementTypeKind == TypeDescKind.RECORD) {
                            TypeSymbol actualElementType = Utils.getActualTypeSymbol(elementType);
                            if (actualElementType instanceof RecordTypeSymbol recordType) {
                                populateArrayElementFields(arrayFieldParam, recordType);
                            }
                        }
                        if (shouldRender && elementTypeKind == TypeDescKind.ARRAY) {
                            arrayFieldParam.set2DArray(true);
                            TypeSymbol actualElementType = Utils.getActualTypeSymbol(elementType);
                            if (actualElementType instanceof ArrayTypeSymbol innerArrayType) {
                                arrayFieldParam.setInnerElementTypeSymbol(innerArrayType.memberTypeDescriptor());
                            }
                        }
                        if (shouldRender && elementTypeKind == TypeDescKind.UNION) {
                            arrayFieldParam.setUnionArray(true);
                            TypeSymbol actualElementType = Utils.getActualTypeSymbol(elementType);
                            if (actualElementType instanceof UnionTypeSymbol unionType) {
                                java.util.List<String> memberNames = new java.util.ArrayList<>();
                                for (TypeSymbol member : unionType.memberTypeDescriptors()) {
                                    TypeDescKind memberKind = Utils.getActualTypeKind(member);
                                    if (memberKind != TypeDescKind.NIL) {
                                        String memberName = Utils.getParamTypeName(memberKind);
                                        if (memberName != null && !memberNames.contains(memberName)) {
                                            memberNames.add(memberName);
                                        }
                                    }
                                }
                                arrayFieldParam.setUnionMemberTypeNames(memberNames);
                            }
                        }
                    }
                    fieldParam = arrayFieldParam;
                } else {
                    fieldParam = new FunctionParam(Integer.toString(fieldIndex), qualifiedFieldName, fieldType);
                    fieldParam.setTypeSymbol(fieldTypeSymbol);
                    fieldParam.setRequired(isEffectiveRequired);
                }

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
            populateUnionMemberParams(paramName, ballerinaUnionTypeSymbol, functionParam, new int[]{MAX_FIELD_BUDGET});
        }
        functionParam.setTypeSymbol(parameterSymbol.typeDescriptor());
        // Only try the second approach if the first one didn't find any members
        if (functionParam.getUnionMemberParams().isEmpty()) {
            // Resolve type references to get the actual union type symbol
            actualTypeSymbol = Utils.getActualTypeSymbol(parameterSymbol.typeDescriptor());
            // Check for UnionTypeSymbol interface instead of concrete class to handle all union type implementations
            if (actualTypeSymbol instanceof UnionTypeSymbol unionTypeSymbol) {
                populateUnionMemberParams(paramName, unionTypeSymbol, functionParam, new int[]{MAX_FIELD_BUDGET});
            }
        }
        // Skip empty unions (all members are nil or unsupported types)
        // Note: Check AFTER both population attempts to ensure we try all possible ways to extract union members
        if (functionParam.getUnionMemberParams().isEmpty()) {
            return Optional.empty();
        }
        // If there's only one non-nil member, return it as a regular FunctionParam instead of a UnionFunctionParam
        // This avoids generating a pointless combobox with a single selectable value (e.g., for optional types like string?)
        // However, if the single member is itself a UnionFunctionParam, we must NOT simplify it to a generic FunctionParam.
        if (functionParam.getUnionMemberParams().size() == 1 && !(functionParam.getUnionMemberParams().getFirst() instanceof UnionFunctionParam)) {
            FunctionParam singleMember = functionParam.getUnionMemberParams().getFirst();
            // Create a new function param with the original parameter's properties
            FunctionParam simplifiedParam = new FunctionParam(Integer.toString(index), paramName, singleMember.getParamType());
            simplifiedParam.setParamKind(parameterSymbol.paramKind());
            simplifiedParam.setTypeSymbol(singleMember.getTypeSymbol());
            // If the union was optional (nil was one of the members), the result should also be optional
            simplifiedParam.setRequired(functionParam.isRequired());
            return Optional.of(simplifiedParam);
        }
        return Optional.of(functionParam);
    }

    private static void populateUnionMemberParams(String paramName, UnionTypeSymbol unionTypeSymbol, UnionFunctionParam functionParam, int[] fieldBudget) {
        // Budget exhausted - stop expanding
        if (fieldBudget[0] <= 0) {
            return;
        }
        int memberIndex = 0;
        java.util.Set<String> seenTypes = new java.util.HashSet<>();  // Track seen actualParamType values to avoid duplicates

        // Pre-scan for Nil type to determine if the union is optional
        // This ensures functionParam.isRequired() is correct before we process any members
        for (TypeSymbol memberTypeSymbol : unionTypeSymbol.memberTypeDescriptors()) {
            TypeDescKind actualTypeKind = Utils.getActualTypeKind(memberTypeSymbol);
            if (actualTypeKind == TypeDescKind.NIL) {
                functionParam.setRequired(false);
                break;
            }
        }

        for (TypeSymbol memberTypeSymbol : unionTypeSymbol.memberTypeDescriptors()) {
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
                    } else if (TypeDescKind.UNION.getName().equals(paramType)) {
                        // For union types, try to get the type name (e.g., type alias name)
                        // If no name is available, use generic "Union" + index
                        actualParamType = memberTypeSymbol.getName().orElse("Union" + memberIndex);
                    } else {
                        actualParamType = paramType;
                    }

                    // Skip if we've already added this type (e.g., multiple singleton strings "1", "2" both map to "string")
                    if (seenTypes.contains(actualParamType)) {
                        continue;
                    }
                    seenTypes.add(actualParamType);

                    String memberParamName = paramName + StringUtils.capitalize(actualParamType);

                    // If the member type is itself a union, create a UnionFunctionParam recursively
                    FunctionParam memberParam;
                    TypeSymbol actualMemberTypeSymbol = Utils.getActualTypeSymbol(memberTypeSymbol);
                    if (actualTypeKind == TypeDescKind.UNION && actualMemberTypeSymbol instanceof UnionTypeSymbol memberUnionSymbol) {
                        UnionFunctionParam memberUnionParam = new UnionFunctionParam(Integer.toString(memberIndex), memberParamName, paramType);
                        memberUnionParam.setTypeSymbol(memberTypeSymbol);
                        memberUnionParam.setDisplayTypeName(actualParamType);
                        // Propagate parent's optionality
                        memberUnionParam.setRequired(functionParam.isRequired());
                        populateUnionMemberParams(memberParamName, memberUnionSymbol, memberUnionParam, fieldBudget);
                        memberParam = memberUnionParam;
                    } else if (actualTypeKind == TypeDescKind.RECORD && actualMemberTypeSymbol instanceof RecordTypeSymbol recordTypeSymbol) {
                        RecordFunctionParam recordParam = new RecordFunctionParam(Integer.toString(memberIndex), memberParamName, paramType);
                        recordParam.setTypeSymbol(memberTypeSymbol);
                        recordParam.setDisplayTypeName(actualParamType);
                        recordParam.setRecordName(actualParamType);
                        // Propagate parent's optionality
                        recordParam.setRequired(functionParam.isRequired());
                        // Use original paramName as parent path for fields so they are generated as "paramName.field"
                        populateRecordFieldParams(recordParam, recordTypeSymbol, paramName, fieldBudget);
                        memberParam = recordParam;
                    } else {
                        memberParam = new FunctionParam(Integer.toString(memberIndex), memberParamName, paramType);
                        memberParam.setTypeSymbol(memberTypeSymbol);
                        memberParam.setDisplayTypeName(actualParamType);
                        // Propagate parent's optionality
                        memberParam.setRequired(functionParam.isRequired());
                    }
                    // Use sanitized parameter name in enable condition for consistency
                    String sanitizedParamName = io.ballerina.mi.util.Utils.sanitizeParamName(paramName);
                    memberParam.setEnableCondition("[{\"" + sanitizedParamName + "DataType\": \"" + actualParamType + "\"}]");
                    functionParam.addUnionMemberParam(memberParam);
                    memberIndex++;
                }
            }
        }
    }

    private static Optional<FunctionParam> createMapFunctionParam(ParameterSymbol parameterSymbol, int index) {
        String paramName = parameterSymbol.getName().orElseThrow();
        TypeSymbol typeSymbol = parameterSymbol.typeDescriptor();
        TypeSymbol actualTypeSymbol = Utils.getActualTypeSymbol(typeSymbol);

        MapFunctionParam mapParam = new MapFunctionParam(Integer.toString(index), paramName, TypeDescKind.MAP.getName());
        mapParam.setParamKind(parameterSymbol.paramKind());
        mapParam.setTypeSymbol(typeSymbol);

        // Set required based on parameter kind
        if (parameterSymbol.paramKind() == ParameterKind.DEFAULTABLE) {
            mapParam.setRequired(false);
        }

        // Extract key and value type information
        if (actualTypeSymbol instanceof MapTypeSymbol mapTypeSymbol) {
            Optional<TypeSymbol> valueTypeOpt = mapTypeSymbol.typeParameter();
            if (valueTypeOpt.isPresent()) {
                TypeSymbol valueType = valueTypeOpt.get();
                mapParam.setValueTypeSymbol(valueType);

                // Determine if we should render as table
                TypeDescKind valueTypeKind = Utils.getActualTypeKind(valueType);
                boolean shouldRenderAsTable = shouldMapRenderAsTable(valueType, valueTypeKind);
                mapParam.setRenderAsTable(shouldRenderAsTable);

                // If value is a record and we're rendering as table, expand its fields
                if (shouldRenderAsTable && valueTypeKind == TypeDescKind.RECORD) {
                    TypeSymbol actualValueType = Utils.getActualTypeSymbol(valueType);
                    if (actualValueType instanceof RecordTypeSymbol recordTypeSymbol) {
                        populateMapValueFields(mapParam, recordTypeSymbol);
                    }
                }
            }
        }

        return Optional.of(mapParam);
    }

    private static Optional<FunctionParam> createArrayFunctionParam(ParameterSymbol parameterSymbol, int index) {
        String paramName = parameterSymbol.getName().orElseThrow();
        TypeSymbol typeSymbol = parameterSymbol.typeDescriptor();
        TypeSymbol actualTypeSymbol = Utils.getActualTypeSymbol(typeSymbol);

        ArrayFunctionParam arrayParam = new ArrayFunctionParam(Integer.toString(index), paramName, TypeDescKind.ARRAY.getName());
        arrayParam.setParamKind(parameterSymbol.paramKind());
        arrayParam.setTypeSymbol(typeSymbol);

        // Set required based on parameter kind
        if (parameterSymbol.paramKind() == ParameterKind.DEFAULTABLE) {
            arrayParam.setRequired(false);
        }

        // Extract element type information
        if (actualTypeSymbol instanceof ArrayTypeSymbol arrayTypeSymbol) {
            TypeSymbol elementType = arrayTypeSymbol.memberTypeDescriptor();
            arrayParam.setElementTypeSymbol(elementType);

            // Determine if we should render as table
            TypeDescKind elementTypeKind = Utils.getActualTypeKind(elementType);
            boolean shouldRenderAsTable = shouldArrayRenderAsTable(elementType, elementTypeKind);
            arrayParam.setRenderAsTable(shouldRenderAsTable);

            if (shouldRenderAsTable) {
                if (elementTypeKind == TypeDescKind.RECORD) {
                    // Record array - expand its fields
                    TypeSymbol actualElementType = Utils.getActualTypeSymbol(elementType);
                    if (actualElementType instanceof RecordTypeSymbol recordTypeSymbol) {
                        populateArrayElementFields(arrayParam, recordTypeSymbol);
                    }
                } else if (elementTypeKind == TypeDescKind.ARRAY) {
                    // 2D array (e.g., string[][], int[][]) - extract inner element type
                    arrayParam.set2DArray(true);
                    TypeSymbol actualElementType = Utils.getActualTypeSymbol(elementType);
                    if (actualElementType instanceof ArrayTypeSymbol innerArrayType) {
                        TypeSymbol innerElementType = innerArrayType.memberTypeDescriptor();
                        arrayParam.setInnerElementTypeSymbol(innerElementType);
                    }
                } else if (elementTypeKind == TypeDescKind.UNION) {
                    // Union type array (e.g., (string|int)[]) - extract union member type names
                    arrayParam.setUnionArray(true);
                    TypeSymbol actualElementType = Utils.getActualTypeSymbol(elementType);
                    if (actualElementType instanceof UnionTypeSymbol unionType) {
                        java.util.List<String> memberNames = new java.util.ArrayList<>();
                        for (TypeSymbol member : unionType.memberTypeDescriptors()) {
                            TypeDescKind memberKind = Utils.getActualTypeKind(member);
                            if (memberKind != TypeDescKind.NIL) {
                                String memberName = Utils.getParamTypeName(memberKind);
                                if (memberName != null && !memberNames.contains(memberName)) {
                                    memberNames.add(memberName);
                                }
                            }
                        }
                        arrayParam.setUnionMemberTypeNames(memberNames);
                    }
                }
            }
        }

        return Optional.of(arrayParam);
    }

    private static boolean shouldMapRenderAsTable(TypeSymbol valueType, TypeDescKind valueTypeKind) {
        // Render as table for simple value types
        if (valueTypeKind == TypeDescKind.STRING ||
            valueTypeKind == TypeDescKind.INT ||
            valueTypeKind == TypeDescKind.BOOLEAN ||
            valueTypeKind == TypeDescKind.FLOAT ||
            valueTypeKind == TypeDescKind.DECIMAL) {
            return true;
        }

        // For record values, render as table if field count is reasonable
        if (valueTypeKind == TypeDescKind.RECORD) {
            TypeSymbol actualType = Utils.getActualTypeSymbol(valueType);
            if (actualType instanceof RecordTypeSymbol recordType) {
                int fieldCount = recordType.fieldDescriptors().size();
                return fieldCount > 0;
            }
        }

        return false;  // Complex types, nested maps, unions, etc. use JSON input
    }

    private static boolean shouldArrayRenderAsTable(TypeSymbol elementType, TypeDescKind elementTypeKind) {
        // Render as table for simple element types
        if (elementTypeKind == TypeDescKind.STRING ||
            elementTypeKind == TypeDescKind.INT ||
            elementTypeKind == TypeDescKind.BOOLEAN ||
            elementTypeKind == TypeDescKind.FLOAT ||
            elementTypeKind == TypeDescKind.DECIMAL) {
            return true;
        }

        // For record elements, render as table if field count is reasonable
        if (elementTypeKind == TypeDescKind.RECORD) {
            TypeSymbol actualType = Utils.getActualTypeSymbol(elementType);
            if (actualType instanceof RecordTypeSymbol recordType) {
                int fieldCount = recordType.fieldDescriptors().size();
                return fieldCount > 0;
            }
        }

        // 2D arrays (e.g., string[][], int[][]) - render as nested table
        if (elementTypeKind == TypeDescKind.ARRAY) {
            return true;
        }

        // Union type arrays (e.g., (string|int)[]) - render as table with type dropdown
        if (elementTypeKind == TypeDescKind.UNION) {
            return true;
        }

        return false;  // Other complex types use JSON input
    }

    private static void populateMapValueFields(MapFunctionParam mapParam, RecordTypeSymbol recordTypeSymbol) {
        Map<String, RecordFieldSymbol> fieldDescriptors = recordTypeSymbol.fieldDescriptors();
        int fieldIndex = 0;

        for (Map.Entry<String, RecordFieldSymbol> entry : fieldDescriptors.entrySet()) {
            String fieldName = entry.getKey();
            RecordFieldSymbol fieldSymbol = entry.getValue();
            TypeSymbol fieldTypeSymbol = fieldSymbol.typeDescriptor();
            TypeDescKind fieldTypeKind = Utils.getActualTypeKind(fieldTypeSymbol);
            String fieldType = Utils.getParamTypeName(fieldTypeKind);

            if (fieldType != null) {
                FunctionParam fieldParam = new FunctionParam(Integer.toString(fieldIndex), fieldName, fieldType);
                fieldParam.setTypeSymbol(fieldTypeSymbol);
                fieldParam.setRequired(!fieldSymbol.isOptional());

                fieldSymbol.documentation().ifPresent(doc ->
                    doc.description().ifPresent(fieldParam::setDescription));

                mapParam.addValueFieldParam(fieldParam);
                fieldIndex++;
            }
        }
    }

    private static void populateArrayElementFields(ArrayFunctionParam arrayParam, RecordTypeSymbol recordTypeSymbol) {
        Map<String, RecordFieldSymbol> fieldDescriptors = recordTypeSymbol.fieldDescriptors();
        int fieldIndex = 0;

        for (Map.Entry<String, RecordFieldSymbol> entry : fieldDescriptors.entrySet()) {
            String fieldName = entry.getKey();
            RecordFieldSymbol fieldSymbol = entry.getValue();
            TypeSymbol fieldTypeSymbol = fieldSymbol.typeDescriptor();
            TypeDescKind fieldTypeKind = Utils.getActualTypeKind(fieldTypeSymbol);
            String fieldType = Utils.getParamTypeName(fieldTypeKind);

            if (fieldType != null) {
                FunctionParam fieldParam = new FunctionParam(Integer.toString(fieldIndex), fieldName, fieldType);
                fieldParam.setTypeSymbol(fieldTypeSymbol);
                fieldParam.setRequired(!fieldSymbol.isOptional());

                fieldSymbol.documentation().ifPresent(doc ->
                    doc.description().ifPresent(fieldParam::setDescription));

                arrayParam.addElementFieldParam(fieldParam);
                fieldIndex++;
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
