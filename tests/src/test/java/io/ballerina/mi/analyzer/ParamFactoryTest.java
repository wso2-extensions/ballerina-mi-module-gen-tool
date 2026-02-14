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

package io.ballerina.mi.analyzer;

import io.ballerina.compiler.api.impl.symbols.BallerinaUnionTypeSymbol;
import io.ballerina.compiler.api.symbols.ArrayTypeSymbol;
import io.ballerina.compiler.api.symbols.MapTypeSymbol;
import io.ballerina.compiler.api.symbols.ParameterKind;
import io.ballerina.compiler.api.symbols.ParameterSymbol;
import io.ballerina.compiler.api.symbols.RecordFieldSymbol;
import io.ballerina.compiler.api.symbols.RecordTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
import io.ballerina.mi.model.param.ArrayFunctionParam;
import io.ballerina.mi.model.param.FunctionParam;
import io.ballerina.mi.model.param.MapFunctionParam;
import io.ballerina.mi.model.param.RecordFunctionParam;
import io.ballerina.mi.model.param.UnionFunctionParam;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for ParamFactory class.
 */
public class ParamFactoryTest {

    @Test
    public void testCreateFunctionParam_StringType() {
        ParameterSymbol paramSymbol = mock(ParameterSymbol.class);
        TypeSymbol typeSymbol = mock(TypeSymbol.class);

        when(paramSymbol.typeDescriptor()).thenReturn(typeSymbol);
        when(paramSymbol.getName()).thenReturn(Optional.of("myParam"));
        when(paramSymbol.paramKind()).thenReturn(ParameterKind.REQUIRED);
        when(typeSymbol.typeKind()).thenReturn(TypeDescKind.STRING);

        Optional<FunctionParam> result = ParamFactory.createFunctionParam(paramSymbol, 0);

        Assert.assertTrue(result.isPresent());
        Assert.assertEquals(result.get().getValue(), "myParam");
        Assert.assertEquals(result.get().getParamType(), "string");
    }

    @Test
    public void testCreateFunctionParam_IntType() {
        ParameterSymbol paramSymbol = mock(ParameterSymbol.class);
        TypeSymbol typeSymbol = mock(TypeSymbol.class);

        when(paramSymbol.typeDescriptor()).thenReturn(typeSymbol);
        when(paramSymbol.getName()).thenReturn(Optional.of("count"));
        when(paramSymbol.paramKind()).thenReturn(ParameterKind.REQUIRED);
        when(typeSymbol.typeKind()).thenReturn(TypeDescKind.INT);

        Optional<FunctionParam> result = ParamFactory.createFunctionParam(paramSymbol, 1);

        Assert.assertTrue(result.isPresent());
        Assert.assertEquals(result.get().getValue(), "count");
        Assert.assertEquals(result.get().getParamType(), "int");
    }

    @Test
    public void testCreateFunctionParam_BooleanType() {
        ParameterSymbol paramSymbol = mock(ParameterSymbol.class);
        TypeSymbol typeSymbol = mock(TypeSymbol.class);

        when(paramSymbol.typeDescriptor()).thenReturn(typeSymbol);
        when(paramSymbol.getName()).thenReturn(Optional.of("enabled"));
        when(paramSymbol.paramKind()).thenReturn(ParameterKind.REQUIRED);
        when(typeSymbol.typeKind()).thenReturn(TypeDescKind.BOOLEAN);

        Optional<FunctionParam> result = ParamFactory.createFunctionParam(paramSymbol, 2);

        Assert.assertTrue(result.isPresent());
        Assert.assertEquals(result.get().getValue(), "enabled");
        Assert.assertEquals(result.get().getParamType(), "boolean");
    }

    @Test
    public void testCreateFunctionParam_FloatType() {
        ParameterSymbol paramSymbol = mock(ParameterSymbol.class);
        TypeSymbol typeSymbol = mock(TypeSymbol.class);

        when(paramSymbol.typeDescriptor()).thenReturn(typeSymbol);
        when(paramSymbol.getName()).thenReturn(Optional.of("rate"));
        when(paramSymbol.paramKind()).thenReturn(ParameterKind.REQUIRED);
        when(typeSymbol.typeKind()).thenReturn(TypeDescKind.FLOAT);

        Optional<FunctionParam> result = ParamFactory.createFunctionParam(paramSymbol, 3);

        Assert.assertTrue(result.isPresent());
        Assert.assertEquals(result.get().getValue(), "rate");
        Assert.assertEquals(result.get().getParamType(), "float");
    }

    @Test
    public void testCreateFunctionParam_DecimalType() {
        ParameterSymbol paramSymbol = mock(ParameterSymbol.class);
        TypeSymbol typeSymbol = mock(TypeSymbol.class);

        when(paramSymbol.typeDescriptor()).thenReturn(typeSymbol);
        when(paramSymbol.getName()).thenReturn(Optional.of("price"));
        when(paramSymbol.paramKind()).thenReturn(ParameterKind.REQUIRED);
        when(typeSymbol.typeKind()).thenReturn(TypeDescKind.DECIMAL);

        Optional<FunctionParam> result = ParamFactory.createFunctionParam(paramSymbol, 4);

        Assert.assertTrue(result.isPresent());
        Assert.assertEquals(result.get().getValue(), "price");
        Assert.assertEquals(result.get().getParamType(), "decimal");
    }

    @Test
    public void testCreateFunctionParam_DefaultableParam() {
        ParameterSymbol paramSymbol = mock(ParameterSymbol.class);
        TypeSymbol typeSymbol = mock(TypeSymbol.class);

        when(paramSymbol.typeDescriptor()).thenReturn(typeSymbol);
        when(paramSymbol.getName()).thenReturn(Optional.of("optionalParam"));
        when(paramSymbol.paramKind()).thenReturn(ParameterKind.DEFAULTABLE);
        when(typeSymbol.typeKind()).thenReturn(TypeDescKind.STRING);

        Optional<FunctionParam> result = ParamFactory.createFunctionParam(paramSymbol, 0);

        Assert.assertTrue(result.isPresent());
        Assert.assertEquals(result.get().getParamKind(), ParameterKind.DEFAULTABLE);
    }

    @Test
    public void testCreateFunctionParam_JsonType() {
        ParameterSymbol paramSymbol = mock(ParameterSymbol.class);
        TypeSymbol typeSymbol = mock(TypeSymbol.class);

        when(paramSymbol.typeDescriptor()).thenReturn(typeSymbol);
        when(paramSymbol.getName()).thenReturn(Optional.of("data"));
        when(paramSymbol.paramKind()).thenReturn(ParameterKind.REQUIRED);
        when(typeSymbol.typeKind()).thenReturn(TypeDescKind.JSON);

        Optional<FunctionParam> result = ParamFactory.createFunctionParam(paramSymbol, 0);

        Assert.assertTrue(result.isPresent());
        Assert.assertEquals(result.get().getParamType(), "json");
    }

    @Test
    public void testCreateFunctionParam_XmlType() {
        ParameterSymbol paramSymbol = mock(ParameterSymbol.class);
        TypeSymbol typeSymbol = mock(TypeSymbol.class);

        when(paramSymbol.typeDescriptor()).thenReturn(typeSymbol);
        when(paramSymbol.getName()).thenReturn(Optional.of("payload"));
        when(paramSymbol.paramKind()).thenReturn(ParameterKind.REQUIRED);
        when(typeSymbol.typeKind()).thenReturn(TypeDescKind.XML);

        Optional<FunctionParam> result = ParamFactory.createFunctionParam(paramSymbol, 0);

        Assert.assertTrue(result.isPresent());
        Assert.assertEquals(result.get().getParamType(), "xml");
    }

    @Test
    public void testCreateFunctionParam_RecordType() {
        ParameterSymbol paramSymbol = mock(ParameterSymbol.class);
        RecordTypeSymbol recordTypeSymbol = mock(RecordTypeSymbol.class);

        when(paramSymbol.typeDescriptor()).thenReturn(recordTypeSymbol);
        when(paramSymbol.getName()).thenReturn(Optional.of("config"));
        when(paramSymbol.paramKind()).thenReturn(ParameterKind.REQUIRED);
        when(recordTypeSymbol.typeKind()).thenReturn(TypeDescKind.RECORD);
        when(recordTypeSymbol.getName()).thenReturn(Optional.of("MyConfig"));
        when(recordTypeSymbol.fieldDescriptors()).thenReturn(Collections.emptyMap());

        Optional<FunctionParam> result = ParamFactory.createFunctionParam(paramSymbol, 0);

        Assert.assertTrue(result.isPresent());
        Assert.assertTrue(result.get() instanceof RecordFunctionParam);
        RecordFunctionParam recordParam = (RecordFunctionParam) result.get();
        Assert.assertEquals(recordParam.getRecordName(), "MyConfig");
    }

    @Test
    public void testCreateFunctionParam_RecordWithFields() {
        ParameterSymbol paramSymbol = mock(ParameterSymbol.class);
        RecordTypeSymbol recordTypeSymbol = mock(RecordTypeSymbol.class);
        RecordFieldSymbol fieldSymbol = mock(RecordFieldSymbol.class);
        TypeSymbol fieldTypeSymbol = mock(TypeSymbol.class);

        when(paramSymbol.typeDescriptor()).thenReturn(recordTypeSymbol);
        when(paramSymbol.getName()).thenReturn(Optional.of("user"));
        when(paramSymbol.paramKind()).thenReturn(ParameterKind.REQUIRED);
        when(recordTypeSymbol.typeKind()).thenReturn(TypeDescKind.RECORD);
        when(recordTypeSymbol.getName()).thenReturn(Optional.of("User"));

        Map<String, RecordFieldSymbol> fields = new HashMap<>();
        fields.put("name", fieldSymbol);
        when(recordTypeSymbol.fieldDescriptors()).thenReturn(fields);

        when(fieldSymbol.typeDescriptor()).thenReturn(fieldTypeSymbol);
        when(fieldSymbol.isOptional()).thenReturn(false);
        when(fieldSymbol.hasDefaultValue()).thenReturn(false);
        when(fieldSymbol.documentation()).thenReturn(Optional.empty());
        when(fieldTypeSymbol.typeKind()).thenReturn(TypeDescKind.STRING);

        Optional<FunctionParam> result = ParamFactory.createFunctionParam(paramSymbol, 0);

        Assert.assertTrue(result.isPresent());
        Assert.assertTrue(result.get() instanceof RecordFunctionParam);
        RecordFunctionParam recordParam = (RecordFunctionParam) result.get();
        Assert.assertEquals(recordParam.getRecordFieldParams().size(), 1);
    }

    @Test
    public void testCreateFunctionParam_MapType() {
        ParameterSymbol paramSymbol = mock(ParameterSymbol.class);
        MapTypeSymbol mapTypeSymbol = mock(MapTypeSymbol.class);
        TypeSymbol valueTypeSymbol = mock(TypeSymbol.class);

        when(paramSymbol.typeDescriptor()).thenReturn(mapTypeSymbol);
        when(paramSymbol.getName()).thenReturn(Optional.of("headers"));
        when(paramSymbol.paramKind()).thenReturn(ParameterKind.REQUIRED);
        when(mapTypeSymbol.typeKind()).thenReturn(TypeDescKind.MAP);
        when(mapTypeSymbol.typeParameter()).thenReturn(Optional.of(valueTypeSymbol));
        when(valueTypeSymbol.typeKind()).thenReturn(TypeDescKind.STRING);

        Optional<FunctionParam> result = ParamFactory.createFunctionParam(paramSymbol, 0);

        Assert.assertTrue(result.isPresent());
        Assert.assertTrue(result.get() instanceof MapFunctionParam);
        MapFunctionParam mapParam = (MapFunctionParam) result.get();
        Assert.assertTrue(mapParam.isRenderAsTable());
    }

    @Test
    public void testCreateFunctionParam_ArrayType() {
        ParameterSymbol paramSymbol = mock(ParameterSymbol.class);
        ArrayTypeSymbol arrayTypeSymbol = mock(ArrayTypeSymbol.class);
        TypeSymbol elementTypeSymbol = mock(TypeSymbol.class);

        when(paramSymbol.typeDescriptor()).thenReturn(arrayTypeSymbol);
        when(paramSymbol.getName()).thenReturn(Optional.of("items"));
        when(paramSymbol.paramKind()).thenReturn(ParameterKind.REQUIRED);
        when(arrayTypeSymbol.typeKind()).thenReturn(TypeDescKind.ARRAY);
        when(arrayTypeSymbol.memberTypeDescriptor()).thenReturn(elementTypeSymbol);
        when(elementTypeSymbol.typeKind()).thenReturn(TypeDescKind.STRING);

        Optional<FunctionParam> result = ParamFactory.createFunctionParam(paramSymbol, 0);

        Assert.assertTrue(result.isPresent());
        Assert.assertTrue(result.get() instanceof ArrayFunctionParam);
        ArrayFunctionParam arrayParam = (ArrayFunctionParam) result.get();
        Assert.assertTrue(arrayParam.isRenderAsTable());
    }

    @Test
    public void testCreateFunctionParam_ArrayWithIntElements() {
        ParameterSymbol paramSymbol = mock(ParameterSymbol.class);
        ArrayTypeSymbol arrayTypeSymbol = mock(ArrayTypeSymbol.class);
        TypeSymbol elementTypeSymbol = mock(TypeSymbol.class);

        when(paramSymbol.typeDescriptor()).thenReturn(arrayTypeSymbol);
        when(paramSymbol.getName()).thenReturn(Optional.of("numbers"));
        when(paramSymbol.paramKind()).thenReturn(ParameterKind.REQUIRED);
        when(arrayTypeSymbol.typeKind()).thenReturn(TypeDescKind.ARRAY);
        when(arrayTypeSymbol.memberTypeDescriptor()).thenReturn(elementTypeSymbol);
        when(elementTypeSymbol.typeKind()).thenReturn(TypeDescKind.INT);

        Optional<FunctionParam> result = ParamFactory.createFunctionParam(paramSymbol, 0);

        Assert.assertTrue(result.isPresent());
        Assert.assertTrue(result.get() instanceof ArrayFunctionParam);
    }

    @Test
    public void testCreateFunctionParam_2DArray() {
        ParameterSymbol paramSymbol = mock(ParameterSymbol.class);
        ArrayTypeSymbol outerArrayType = mock(ArrayTypeSymbol.class);
        ArrayTypeSymbol innerArrayType = mock(ArrayTypeSymbol.class);
        TypeSymbol innerElementType = mock(TypeSymbol.class);

        when(paramSymbol.typeDescriptor()).thenReturn(outerArrayType);
        when(paramSymbol.getName()).thenReturn(Optional.of("matrix"));
        when(paramSymbol.paramKind()).thenReturn(ParameterKind.REQUIRED);
        when(outerArrayType.typeKind()).thenReturn(TypeDescKind.ARRAY);
        when(outerArrayType.memberTypeDescriptor()).thenReturn(innerArrayType);
        when(innerArrayType.typeKind()).thenReturn(TypeDescKind.ARRAY);
        when(innerArrayType.memberTypeDescriptor()).thenReturn(innerElementType);
        when(innerElementType.typeKind()).thenReturn(TypeDescKind.INT);

        Optional<FunctionParam> result = ParamFactory.createFunctionParam(paramSymbol, 0);

        Assert.assertTrue(result.isPresent());
        Assert.assertTrue(result.get() instanceof ArrayFunctionParam);
        ArrayFunctionParam arrayParam = (ArrayFunctionParam) result.get();
        Assert.assertTrue(arrayParam.is2DArray());
    }

    @Test
    public void testCreateFunctionParam_UnionType() {
        ParameterSymbol paramSymbol = mock(ParameterSymbol.class);
        UnionTypeSymbol unionTypeSymbol = mock(UnionTypeSymbol.class);
        TypeSymbol stringMember = mock(TypeSymbol.class);
        TypeSymbol intMember = mock(TypeSymbol.class);

        when(paramSymbol.typeDescriptor()).thenReturn(unionTypeSymbol);
        when(paramSymbol.getName()).thenReturn(Optional.of("value"));
        when(paramSymbol.paramKind()).thenReturn(ParameterKind.REQUIRED);
        when(unionTypeSymbol.typeKind()).thenReturn(TypeDescKind.UNION);
        when(unionTypeSymbol.memberTypeDescriptors()).thenReturn(List.of(stringMember, intMember));

        when(stringMember.typeKind()).thenReturn(TypeDescKind.STRING);
        when(stringMember.getName()).thenReturn(Optional.empty());
        when(intMember.typeKind()).thenReturn(TypeDescKind.INT);
        when(intMember.getName()).thenReturn(Optional.empty());

        Optional<FunctionParam> result = ParamFactory.createFunctionParam(paramSymbol, 0);

        Assert.assertTrue(result.isPresent());
        Assert.assertTrue(result.get() instanceof UnionFunctionParam);
        UnionFunctionParam unionParam = (UnionFunctionParam) result.get();
        Assert.assertEquals(unionParam.getUnionMemberParams().size(), 2);
    }

    @Test
    public void testCreateFunctionParam_OptionalUnion() {
        ParameterSymbol paramSymbol = mock(ParameterSymbol.class);
        UnionTypeSymbol unionTypeSymbol = mock(UnionTypeSymbol.class);
        TypeSymbol stringMember = mock(TypeSymbol.class);
        TypeSymbol nilMember = mock(TypeSymbol.class);

        when(paramSymbol.typeDescriptor()).thenReturn(unionTypeSymbol);
        when(paramSymbol.getName()).thenReturn(Optional.of("optionalValue"));
        when(paramSymbol.paramKind()).thenReturn(ParameterKind.REQUIRED);
        when(unionTypeSymbol.typeKind()).thenReturn(TypeDescKind.UNION);
        when(unionTypeSymbol.memberTypeDescriptors()).thenReturn(List.of(stringMember, nilMember));

        when(stringMember.typeKind()).thenReturn(TypeDescKind.STRING);
        when(stringMember.getName()).thenReturn(Optional.empty());
        when(nilMember.typeKind()).thenReturn(TypeDescKind.NIL);
        when(nilMember.getName()).thenReturn(Optional.empty());

        Optional<FunctionParam> result = ParamFactory.createFunctionParam(paramSymbol, 0);

        Assert.assertTrue(result.isPresent());
        // Single non-nil member should be simplified to FunctionParam
        Assert.assertTrue(result.get() instanceof FunctionParam);
        Assert.assertFalse(result.get().isRequired());
    }

    @Test
    public void testCreateFunctionParam_UnsupportedType() {
        ParameterSymbol paramSymbol = mock(ParameterSymbol.class);
        TypeSymbol typeSymbol = mock(TypeSymbol.class);

        when(paramSymbol.typeDescriptor()).thenReturn(typeSymbol);
        when(paramSymbol.getName()).thenReturn(Optional.of("unknown"));
        when(paramSymbol.paramKind()).thenReturn(ParameterKind.REQUIRED);
        when(typeSymbol.typeKind()).thenReturn(TypeDescKind.FUNCTION);

        Optional<FunctionParam> result = ParamFactory.createFunctionParam(paramSymbol, 0);

        Assert.assertFalse(result.isPresent());
    }

    @Test
    public void testCreateFunctionParam_MapWithRecordValue() {
        ParameterSymbol paramSymbol = mock(ParameterSymbol.class);
        MapTypeSymbol mapTypeSymbol = mock(MapTypeSymbol.class);
        RecordTypeSymbol valueRecordType = mock(RecordTypeSymbol.class);
        RecordFieldSymbol fieldSymbol = mock(RecordFieldSymbol.class);
        TypeSymbol fieldTypeSymbol = mock(TypeSymbol.class);

        when(paramSymbol.typeDescriptor()).thenReturn(mapTypeSymbol);
        when(paramSymbol.getName()).thenReturn(Optional.of("records"));
        when(paramSymbol.paramKind()).thenReturn(ParameterKind.REQUIRED);
        when(mapTypeSymbol.typeKind()).thenReturn(TypeDescKind.MAP);
        when(mapTypeSymbol.typeParameter()).thenReturn(Optional.of(valueRecordType));
        when(valueRecordType.typeKind()).thenReturn(TypeDescKind.RECORD);

        // Properly mock the field symbol with its type descriptor
        when(fieldSymbol.typeDescriptor()).thenReturn(fieldTypeSymbol);
        when(fieldSymbol.isOptional()).thenReturn(false);
        when(fieldSymbol.documentation()).thenReturn(Optional.empty());
        when(fieldTypeSymbol.typeKind()).thenReturn(TypeDescKind.STRING);

        Map<String, RecordFieldSymbol> fields = new HashMap<>();
        fields.put("field", fieldSymbol);
        when(valueRecordType.fieldDescriptors()).thenReturn(fields);

        Optional<FunctionParam> result = ParamFactory.createFunctionParam(paramSymbol, 0);

        Assert.assertTrue(result.isPresent());
        Assert.assertTrue(result.get() instanceof MapFunctionParam);
        MapFunctionParam mapParam = (MapFunctionParam) result.get();
        Assert.assertTrue(mapParam.isRenderAsTable());
    }

    @Test
    public void testCreateFunctionParam_DefaultableMapParam() {
        ParameterSymbol paramSymbol = mock(ParameterSymbol.class);
        MapTypeSymbol mapTypeSymbol = mock(MapTypeSymbol.class);
        TypeSymbol valueTypeSymbol = mock(TypeSymbol.class);

        when(paramSymbol.typeDescriptor()).thenReturn(mapTypeSymbol);
        when(paramSymbol.getName()).thenReturn(Optional.of("optionalMap"));
        when(paramSymbol.paramKind()).thenReturn(ParameterKind.DEFAULTABLE);
        when(mapTypeSymbol.typeKind()).thenReturn(TypeDescKind.MAP);
        when(mapTypeSymbol.typeParameter()).thenReturn(Optional.of(valueTypeSymbol));
        when(valueTypeSymbol.typeKind()).thenReturn(TypeDescKind.STRING);

        Optional<FunctionParam> result = ParamFactory.createFunctionParam(paramSymbol, 0);

        Assert.assertTrue(result.isPresent());
        Assert.assertTrue(result.get() instanceof MapFunctionParam);
        MapFunctionParam mapParam = (MapFunctionParam) result.get();
        Assert.assertFalse(mapParam.isRequired());
    }

    @Test
    public void testCreateFunctionParam_DefaultableArrayParam() {
        ParameterSymbol paramSymbol = mock(ParameterSymbol.class);
        ArrayTypeSymbol arrayTypeSymbol = mock(ArrayTypeSymbol.class);
        TypeSymbol elementTypeSymbol = mock(TypeSymbol.class);

        when(paramSymbol.typeDescriptor()).thenReturn(arrayTypeSymbol);
        when(paramSymbol.getName()).thenReturn(Optional.of("optionalArray"));
        when(paramSymbol.paramKind()).thenReturn(ParameterKind.DEFAULTABLE);
        when(arrayTypeSymbol.typeKind()).thenReturn(TypeDescKind.ARRAY);
        when(arrayTypeSymbol.memberTypeDescriptor()).thenReturn(elementTypeSymbol);
        when(elementTypeSymbol.typeKind()).thenReturn(TypeDescKind.STRING);

        Optional<FunctionParam> result = ParamFactory.createFunctionParam(paramSymbol, 0);

        Assert.assertTrue(result.isPresent());
        Assert.assertTrue(result.get() instanceof ArrayFunctionParam);
        ArrayFunctionParam arrayParam = (ArrayFunctionParam) result.get();
        Assert.assertFalse(arrayParam.isRequired());
    }

    @Test
    public void testCreateFunctionParam_DefaultableRecordParam() {
        ParameterSymbol paramSymbol = mock(ParameterSymbol.class);
        RecordTypeSymbol recordTypeSymbol = mock(RecordTypeSymbol.class);

        when(paramSymbol.typeDescriptor()).thenReturn(recordTypeSymbol);
        when(paramSymbol.getName()).thenReturn(Optional.of("optionalRecord"));
        when(paramSymbol.paramKind()).thenReturn(ParameterKind.DEFAULTABLE);
        when(recordTypeSymbol.typeKind()).thenReturn(TypeDescKind.RECORD);
        when(recordTypeSymbol.getName()).thenReturn(Optional.of("Config"));
        when(recordTypeSymbol.fieldDescriptors()).thenReturn(Collections.emptyMap());

        Optional<FunctionParam> result = ParamFactory.createFunctionParam(paramSymbol, 0);

        Assert.assertTrue(result.isPresent());
        Assert.assertTrue(result.get() instanceof RecordFunctionParam);
        RecordFunctionParam recordParam = (RecordFunctionParam) result.get();
        Assert.assertFalse(recordParam.isRequired());
    }

    @Test
    public void testCreateFunctionParam_UnionWithAllNilMembers() {
        ParameterSymbol paramSymbol = mock(ParameterSymbol.class);
        UnionTypeSymbol unionTypeSymbol = mock(UnionTypeSymbol.class);
        TypeSymbol nilMember = mock(TypeSymbol.class);

        when(paramSymbol.typeDescriptor()).thenReturn(unionTypeSymbol);
        when(paramSymbol.getName()).thenReturn(Optional.of("nilOnly"));
        when(paramSymbol.paramKind()).thenReturn(ParameterKind.REQUIRED);
        when(unionTypeSymbol.typeKind()).thenReturn(TypeDescKind.UNION);
        when(unionTypeSymbol.memberTypeDescriptors()).thenReturn(List.of(nilMember));

        when(nilMember.typeKind()).thenReturn(TypeDescKind.NIL);
        when(nilMember.getName()).thenReturn(Optional.empty());

        Optional<FunctionParam> result = ParamFactory.createFunctionParam(paramSymbol, 0);

        // Union with only nil members should return empty
        Assert.assertFalse(result.isPresent());
    }

    @Test
    public void testCreateFunctionParam_ArrayWithUnionElements() {
        ParameterSymbol paramSymbol = mock(ParameterSymbol.class);
        ArrayTypeSymbol arrayTypeSymbol = mock(ArrayTypeSymbol.class);
        UnionTypeSymbol elementUnionType = mock(UnionTypeSymbol.class);
        TypeSymbol stringMember = mock(TypeSymbol.class);
        TypeSymbol intMember = mock(TypeSymbol.class);

        when(paramSymbol.typeDescriptor()).thenReturn(arrayTypeSymbol);
        when(paramSymbol.getName()).thenReturn(Optional.of("mixedArray"));
        when(paramSymbol.paramKind()).thenReturn(ParameterKind.REQUIRED);
        when(arrayTypeSymbol.typeKind()).thenReturn(TypeDescKind.ARRAY);
        when(arrayTypeSymbol.memberTypeDescriptor()).thenReturn(elementUnionType);
        when(elementUnionType.typeKind()).thenReturn(TypeDescKind.UNION);
        when(elementUnionType.memberTypeDescriptors()).thenReturn(List.of(stringMember, intMember));

        when(stringMember.typeKind()).thenReturn(TypeDescKind.STRING);
        when(intMember.typeKind()).thenReturn(TypeDescKind.INT);

        Optional<FunctionParam> result = ParamFactory.createFunctionParam(paramSymbol, 0);

        Assert.assertTrue(result.isPresent());
        Assert.assertTrue(result.get() instanceof ArrayFunctionParam);
        ArrayFunctionParam arrayParam = (ArrayFunctionParam) result.get();
        Assert.assertTrue(arrayParam.isUnionArray());
    }

    @Test
    public void testCreateFunctionParam_ByteArrayType() {
        ParameterSymbol paramSymbol = mock(ParameterSymbol.class);
        ArrayTypeSymbol arrayTypeSymbol = mock(ArrayTypeSymbol.class);
        TypeSymbol elementTypeSymbol = mock(TypeSymbol.class);

        when(paramSymbol.typeDescriptor()).thenReturn(arrayTypeSymbol);
        when(paramSymbol.getName()).thenReturn(Optional.of("data"));
        when(paramSymbol.paramKind()).thenReturn(ParameterKind.REQUIRED);
        when(arrayTypeSymbol.typeKind()).thenReturn(TypeDescKind.ARRAY);
        when(arrayTypeSymbol.memberTypeDescriptor()).thenReturn(elementTypeSymbol);
        when(elementTypeSymbol.typeKind()).thenReturn(TypeDescKind.BYTE);

        Optional<FunctionParam> result = ParamFactory.createFunctionParam(paramSymbol, 0);

        Assert.assertTrue(result.isPresent());
        Assert.assertTrue(result.get() instanceof ArrayFunctionParam);
    }

    @Test
    public void testCreateFunctionParam_AnydataType() {
        ParameterSymbol paramSymbol = mock(ParameterSymbol.class);
        TypeSymbol typeSymbol = mock(TypeSymbol.class);

        when(paramSymbol.typeDescriptor()).thenReturn(typeSymbol);
        when(paramSymbol.getName()).thenReturn(Optional.of("anyData"));
        when(paramSymbol.paramKind()).thenReturn(ParameterKind.REQUIRED);
        when(typeSymbol.typeKind()).thenReturn(TypeDescKind.ANYDATA);

        Optional<FunctionParam> result = ParamFactory.createFunctionParam(paramSymbol, 0);

        // ANYDATA is not a supported simple type
        Assert.assertFalse(result.isPresent());
    }

    @Test
    public void testCreateFunctionParam_ArrayWithRecordElements() {
        ParameterSymbol paramSymbol = mock(ParameterSymbol.class);
        ArrayTypeSymbol arrayTypeSymbol = mock(ArrayTypeSymbol.class);
        RecordTypeSymbol elementRecordType = mock(RecordTypeSymbol.class);
        RecordFieldSymbol fieldSymbol = mock(RecordFieldSymbol.class);
        TypeSymbol fieldTypeSymbol = mock(TypeSymbol.class);

        when(paramSymbol.typeDescriptor()).thenReturn(arrayTypeSymbol);
        when(paramSymbol.getName()).thenReturn(Optional.of("users"));
        when(paramSymbol.paramKind()).thenReturn(ParameterKind.REQUIRED);
        when(arrayTypeSymbol.typeKind()).thenReturn(TypeDescKind.ARRAY);
        when(arrayTypeSymbol.memberTypeDescriptor()).thenReturn(elementRecordType);
        when(elementRecordType.typeKind()).thenReturn(TypeDescKind.RECORD);

        // Setup record fields
        when(fieldSymbol.typeDescriptor()).thenReturn(fieldTypeSymbol);
        when(fieldSymbol.isOptional()).thenReturn(false);
        when(fieldSymbol.documentation()).thenReturn(Optional.empty());
        when(fieldTypeSymbol.typeKind()).thenReturn(TypeDescKind.STRING);

        Map<String, RecordFieldSymbol> fields = new HashMap<>();
        fields.put("name", fieldSymbol);
        when(elementRecordType.fieldDescriptors()).thenReturn(fields);

        Optional<FunctionParam> result = ParamFactory.createFunctionParam(paramSymbol, 0);

        Assert.assertTrue(result.isPresent());
        Assert.assertTrue(result.get() instanceof ArrayFunctionParam);
        ArrayFunctionParam arrayParam = (ArrayFunctionParam) result.get();
        Assert.assertTrue(arrayParam.isRenderAsTable());
        Assert.assertEquals(arrayParam.getElementFieldParams().size(), 1);
    }

    @Test
    public void testCreateFunctionParam_RecordWithNestedMapField() {
        ParameterSymbol paramSymbol = mock(ParameterSymbol.class);
        RecordTypeSymbol recordTypeSymbol = mock(RecordTypeSymbol.class);
        RecordFieldSymbol mapFieldSymbol = mock(RecordFieldSymbol.class);
        MapTypeSymbol mapTypeSymbol = mock(MapTypeSymbol.class);
        TypeSymbol valueTypeSymbol = mock(TypeSymbol.class);

        when(paramSymbol.typeDescriptor()).thenReturn(recordTypeSymbol);
        when(paramSymbol.getName()).thenReturn(Optional.of("config"));
        when(paramSymbol.paramKind()).thenReturn(ParameterKind.REQUIRED);
        when(recordTypeSymbol.typeKind()).thenReturn(TypeDescKind.RECORD);
        when(recordTypeSymbol.getName()).thenReturn(Optional.of("Config"));

        // Map field setup
        when(mapFieldSymbol.typeDescriptor()).thenReturn(mapTypeSymbol);
        when(mapFieldSymbol.isOptional()).thenReturn(false);
        when(mapFieldSymbol.hasDefaultValue()).thenReturn(false);
        when(mapFieldSymbol.documentation()).thenReturn(Optional.empty());
        when(mapTypeSymbol.typeKind()).thenReturn(TypeDescKind.MAP);
        when(mapTypeSymbol.typeParameter()).thenReturn(Optional.of(valueTypeSymbol));
        when(valueTypeSymbol.typeKind()).thenReturn(TypeDescKind.STRING);

        Map<String, RecordFieldSymbol> fields = new HashMap<>();
        fields.put("headers", mapFieldSymbol);
        when(recordTypeSymbol.fieldDescriptors()).thenReturn(fields);

        Optional<FunctionParam> result = ParamFactory.createFunctionParam(paramSymbol, 0);

        Assert.assertTrue(result.isPresent());
        Assert.assertTrue(result.get() instanceof RecordFunctionParam);
        RecordFunctionParam recordParam = (RecordFunctionParam) result.get();
        Assert.assertEquals(recordParam.getRecordFieldParams().size(), 1);
        Assert.assertTrue(recordParam.getRecordFieldParams().get(0) instanceof MapFunctionParam);
    }

    @Test
    public void testCreateFunctionParam_RecordWithNestedArrayField() {
        ParameterSymbol paramSymbol = mock(ParameterSymbol.class);
        RecordTypeSymbol recordTypeSymbol = mock(RecordTypeSymbol.class);
        RecordFieldSymbol arrayFieldSymbol = mock(RecordFieldSymbol.class);
        ArrayTypeSymbol arrayTypeSymbol = mock(ArrayTypeSymbol.class);
        TypeSymbol elementTypeSymbol = mock(TypeSymbol.class);

        when(paramSymbol.typeDescriptor()).thenReturn(recordTypeSymbol);
        when(paramSymbol.getName()).thenReturn(Optional.of("data"));
        when(paramSymbol.paramKind()).thenReturn(ParameterKind.REQUIRED);
        when(recordTypeSymbol.typeKind()).thenReturn(TypeDescKind.RECORD);
        when(recordTypeSymbol.getName()).thenReturn(Optional.of("Data"));

        // Array field setup
        when(arrayFieldSymbol.typeDescriptor()).thenReturn(arrayTypeSymbol);
        when(arrayFieldSymbol.isOptional()).thenReturn(false);
        when(arrayFieldSymbol.hasDefaultValue()).thenReturn(false);
        when(arrayFieldSymbol.documentation()).thenReturn(Optional.empty());
        when(arrayTypeSymbol.typeKind()).thenReturn(TypeDescKind.ARRAY);
        when(arrayTypeSymbol.memberTypeDescriptor()).thenReturn(elementTypeSymbol);
        when(elementTypeSymbol.typeKind()).thenReturn(TypeDescKind.STRING);

        Map<String, RecordFieldSymbol> fields = new HashMap<>();
        fields.put("items", arrayFieldSymbol);
        when(recordTypeSymbol.fieldDescriptors()).thenReturn(fields);

        Optional<FunctionParam> result = ParamFactory.createFunctionParam(paramSymbol, 0);

        Assert.assertTrue(result.isPresent());
        Assert.assertTrue(result.get() instanceof RecordFunctionParam);
        RecordFunctionParam recordParam = (RecordFunctionParam) result.get();
        Assert.assertEquals(recordParam.getRecordFieldParams().size(), 1);
        Assert.assertTrue(recordParam.getRecordFieldParams().get(0) instanceof ArrayFunctionParam);
    }

    @Test
    public void testCreateFunctionParam_RecordWithNestedUnionField() {
        ParameterSymbol paramSymbol = mock(ParameterSymbol.class);
        RecordTypeSymbol recordTypeSymbol = mock(RecordTypeSymbol.class);
        RecordFieldSymbol unionFieldSymbol = mock(RecordFieldSymbol.class);
        // Must use BallerinaUnionTypeSymbol as the code checks for this specific type
        BallerinaUnionTypeSymbol unionTypeSymbol = mock(BallerinaUnionTypeSymbol.class);
        TypeSymbol stringMember = mock(TypeSymbol.class);
        TypeSymbol intMember = mock(TypeSymbol.class);

        when(paramSymbol.typeDescriptor()).thenReturn(recordTypeSymbol);
        when(paramSymbol.getName()).thenReturn(Optional.of("request"));
        when(paramSymbol.paramKind()).thenReturn(ParameterKind.REQUIRED);
        when(recordTypeSymbol.typeKind()).thenReturn(TypeDescKind.RECORD);
        when(recordTypeSymbol.getName()).thenReturn(Optional.of("Request"));

        // Union field setup
        when(unionFieldSymbol.typeDescriptor()).thenReturn(unionTypeSymbol);
        when(unionFieldSymbol.isOptional()).thenReturn(false);
        when(unionFieldSymbol.hasDefaultValue()).thenReturn(false);
        when(unionFieldSymbol.documentation()).thenReturn(Optional.empty());
        when(unionTypeSymbol.typeKind()).thenReturn(TypeDescKind.UNION);
        when(unionTypeSymbol.memberTypeDescriptors()).thenReturn(List.of(stringMember, intMember));

        when(stringMember.typeKind()).thenReturn(TypeDescKind.STRING);
        when(stringMember.getName()).thenReturn(Optional.empty());
        when(intMember.typeKind()).thenReturn(TypeDescKind.INT);
        when(intMember.getName()).thenReturn(Optional.empty());

        Map<String, RecordFieldSymbol> fields = new HashMap<>();
        fields.put("value", unionFieldSymbol);
        when(recordTypeSymbol.fieldDescriptors()).thenReturn(fields);

        Optional<FunctionParam> result = ParamFactory.createFunctionParam(paramSymbol, 0);

        Assert.assertTrue(result.isPresent());
        Assert.assertTrue(result.get() instanceof RecordFunctionParam);
        RecordFunctionParam recordParam = (RecordFunctionParam) result.get();
        Assert.assertEquals(recordParam.getRecordFieldParams().size(), 1);
        Assert.assertTrue(recordParam.getRecordFieldParams().get(0) instanceof UnionFunctionParam);
    }

    @Test
    public void testCreateFunctionParam_RecordWithOptionalField() {
        ParameterSymbol paramSymbol = mock(ParameterSymbol.class);
        RecordTypeSymbol recordTypeSymbol = mock(RecordTypeSymbol.class);
        RecordFieldSymbol optionalFieldSymbol = mock(RecordFieldSymbol.class);
        TypeSymbol fieldTypeSymbol = mock(TypeSymbol.class);

        when(paramSymbol.typeDescriptor()).thenReturn(recordTypeSymbol);
        when(paramSymbol.getName()).thenReturn(Optional.of("config"));
        when(paramSymbol.paramKind()).thenReturn(ParameterKind.REQUIRED);
        when(recordTypeSymbol.typeKind()).thenReturn(TypeDescKind.RECORD);
        when(recordTypeSymbol.getName()).thenReturn(Optional.of("Config"));

        // Optional field (isOptional = true)
        when(optionalFieldSymbol.typeDescriptor()).thenReturn(fieldTypeSymbol);
        when(optionalFieldSymbol.isOptional()).thenReturn(true);
        when(optionalFieldSymbol.hasDefaultValue()).thenReturn(false);
        when(optionalFieldSymbol.documentation()).thenReturn(Optional.empty());
        when(fieldTypeSymbol.typeKind()).thenReturn(TypeDescKind.STRING);

        Map<String, RecordFieldSymbol> fields = new HashMap<>();
        fields.put("optionalField", optionalFieldSymbol);
        when(recordTypeSymbol.fieldDescriptors()).thenReturn(fields);

        Optional<FunctionParam> result = ParamFactory.createFunctionParam(paramSymbol, 0);

        Assert.assertTrue(result.isPresent());
        Assert.assertTrue(result.get() instanceof RecordFunctionParam);
        RecordFunctionParam recordParam = (RecordFunctionParam) result.get();
        Assert.assertEquals(recordParam.getRecordFieldParams().size(), 1);
        Assert.assertFalse(recordParam.getRecordFieldParams().get(0).isRequired());
    }

    @Test
    public void testCreateFunctionParam_RecordWithFieldHavingDefaultValue() {
        ParameterSymbol paramSymbol = mock(ParameterSymbol.class);
        RecordTypeSymbol recordTypeSymbol = mock(RecordTypeSymbol.class);
        RecordFieldSymbol defaultFieldSymbol = mock(RecordFieldSymbol.class);
        TypeSymbol fieldTypeSymbol = mock(TypeSymbol.class);

        when(paramSymbol.typeDescriptor()).thenReturn(recordTypeSymbol);
        when(paramSymbol.getName()).thenReturn(Optional.of("config"));
        when(paramSymbol.paramKind()).thenReturn(ParameterKind.REQUIRED);
        when(recordTypeSymbol.typeKind()).thenReturn(TypeDescKind.RECORD);
        when(recordTypeSymbol.getName()).thenReturn(Optional.of("Config"));

        // Field with default value
        when(defaultFieldSymbol.typeDescriptor()).thenReturn(fieldTypeSymbol);
        when(defaultFieldSymbol.isOptional()).thenReturn(false);
        when(defaultFieldSymbol.hasDefaultValue()).thenReturn(true);
        when(defaultFieldSymbol.documentation()).thenReturn(Optional.empty());
        when(fieldTypeSymbol.typeKind()).thenReturn(TypeDescKind.INT);

        Map<String, RecordFieldSymbol> fields = new HashMap<>();
        fields.put("port", defaultFieldSymbol);
        when(recordTypeSymbol.fieldDescriptors()).thenReturn(fields);

        Optional<FunctionParam> result = ParamFactory.createFunctionParam(paramSymbol, 0);

        Assert.assertTrue(result.isPresent());
        Assert.assertTrue(result.get() instanceof RecordFunctionParam);
        RecordFunctionParam recordParam = (RecordFunctionParam) result.get();
        Assert.assertEquals(recordParam.getRecordFieldParams().size(), 1);
        // Field with default value should not be required
        Assert.assertFalse(recordParam.getRecordFieldParams().get(0).isRequired());
    }

    @Test
    public void testCreateFunctionParam_RecordWithNestedRecord() {
        ParameterSymbol paramSymbol = mock(ParameterSymbol.class);
        RecordTypeSymbol recordTypeSymbol = mock(RecordTypeSymbol.class);
        RecordFieldSymbol nestedRecordFieldSymbol = mock(RecordFieldSymbol.class);
        RecordTypeSymbol nestedRecordType = mock(RecordTypeSymbol.class);
        RecordFieldSymbol innerFieldSymbol = mock(RecordFieldSymbol.class);
        TypeSymbol innerFieldTypeSymbol = mock(TypeSymbol.class);

        when(paramSymbol.typeDescriptor()).thenReturn(recordTypeSymbol);
        when(paramSymbol.getName()).thenReturn(Optional.of("config"));
        when(paramSymbol.paramKind()).thenReturn(ParameterKind.REQUIRED);
        when(recordTypeSymbol.typeKind()).thenReturn(TypeDescKind.RECORD);
        when(recordTypeSymbol.getName()).thenReturn(Optional.of("Config"));

        // Nested record field
        when(nestedRecordFieldSymbol.typeDescriptor()).thenReturn(nestedRecordType);
        when(nestedRecordFieldSymbol.isOptional()).thenReturn(false);
        when(nestedRecordFieldSymbol.hasDefaultValue()).thenReturn(false);
        when(nestedRecordFieldSymbol.documentation()).thenReturn(Optional.empty());
        when(nestedRecordType.typeKind()).thenReturn(TypeDescKind.RECORD);
        when(nestedRecordType.getName()).thenReturn(Optional.of("AuthConfig"));

        // Inner field
        when(innerFieldSymbol.typeDescriptor()).thenReturn(innerFieldTypeSymbol);
        when(innerFieldSymbol.isOptional()).thenReturn(false);
        when(innerFieldSymbol.hasDefaultValue()).thenReturn(false);
        when(innerFieldSymbol.documentation()).thenReturn(Optional.empty());
        when(innerFieldTypeSymbol.typeKind()).thenReturn(TypeDescKind.STRING);

        Map<String, RecordFieldSymbol> nestedFields = new HashMap<>();
        nestedFields.put("token", innerFieldSymbol);
        when(nestedRecordType.fieldDescriptors()).thenReturn(nestedFields);

        Map<String, RecordFieldSymbol> fields = new HashMap<>();
        fields.put("auth", nestedRecordFieldSymbol);
        when(recordTypeSymbol.fieldDescriptors()).thenReturn(fields);

        Optional<FunctionParam> result = ParamFactory.createFunctionParam(paramSymbol, 0);

        Assert.assertTrue(result.isPresent());
        Assert.assertTrue(result.get() instanceof RecordFunctionParam);
        RecordFunctionParam recordParam = (RecordFunctionParam) result.get();
        Assert.assertEquals(recordParam.getRecordFieldParams().size(), 1);
        Assert.assertTrue(recordParam.getRecordFieldParams().get(0) instanceof RecordFunctionParam);
        RecordFunctionParam nestedParam = (RecordFunctionParam) recordParam.getRecordFieldParams().get(0);
        Assert.assertEquals(nestedParam.getRecordFieldParams().size(), 1);
    }

    @Test
    public void testCreateFunctionParam_MapWithNoValueType() {
        ParameterSymbol paramSymbol = mock(ParameterSymbol.class);
        MapTypeSymbol mapTypeSymbol = mock(MapTypeSymbol.class);

        when(paramSymbol.typeDescriptor()).thenReturn(mapTypeSymbol);
        when(paramSymbol.getName()).thenReturn(Optional.of("anyMap"));
        when(paramSymbol.paramKind()).thenReturn(ParameterKind.REQUIRED);
        when(mapTypeSymbol.typeKind()).thenReturn(TypeDescKind.MAP);
        when(mapTypeSymbol.typeParameter()).thenReturn(Optional.empty());

        Optional<FunctionParam> result = ParamFactory.createFunctionParam(paramSymbol, 0);

        Assert.assertTrue(result.isPresent());
        Assert.assertTrue(result.get() instanceof MapFunctionParam);
        MapFunctionParam mapParam = (MapFunctionParam) result.get();
        // Without value type, render as table should be false
        Assert.assertFalse(mapParam.isRenderAsTable());
    }

    @Test
    public void testCreateFunctionParam_MapWithComplexValueType() {
        ParameterSymbol paramSymbol = mock(ParameterSymbol.class);
        MapTypeSymbol mapTypeSymbol = mock(MapTypeSymbol.class);
        MapTypeSymbol nestedMapType = mock(MapTypeSymbol.class);

        when(paramSymbol.typeDescriptor()).thenReturn(mapTypeSymbol);
        when(paramSymbol.getName()).thenReturn(Optional.of("nestedMap"));
        when(paramSymbol.paramKind()).thenReturn(ParameterKind.REQUIRED);
        when(mapTypeSymbol.typeKind()).thenReturn(TypeDescKind.MAP);
        when(mapTypeSymbol.typeParameter()).thenReturn(Optional.of(nestedMapType));
        when(nestedMapType.typeKind()).thenReturn(TypeDescKind.MAP);

        Optional<FunctionParam> result = ParamFactory.createFunctionParam(paramSymbol, 0);

        Assert.assertTrue(result.isPresent());
        Assert.assertTrue(result.get() instanceof MapFunctionParam);
        MapFunctionParam mapParam = (MapFunctionParam) result.get();
        // Nested map should not render as table
        Assert.assertFalse(mapParam.isRenderAsTable());
    }

    @Test
    public void testCreateFunctionParam_ArrayWithBooleanElements() {
        ParameterSymbol paramSymbol = mock(ParameterSymbol.class);
        ArrayTypeSymbol arrayTypeSymbol = mock(ArrayTypeSymbol.class);
        TypeSymbol elementTypeSymbol = mock(TypeSymbol.class);

        when(paramSymbol.typeDescriptor()).thenReturn(arrayTypeSymbol);
        when(paramSymbol.getName()).thenReturn(Optional.of("flags"));
        when(paramSymbol.paramKind()).thenReturn(ParameterKind.REQUIRED);
        when(arrayTypeSymbol.typeKind()).thenReturn(TypeDescKind.ARRAY);
        when(arrayTypeSymbol.memberTypeDescriptor()).thenReturn(elementTypeSymbol);
        when(elementTypeSymbol.typeKind()).thenReturn(TypeDescKind.BOOLEAN);

        Optional<FunctionParam> result = ParamFactory.createFunctionParam(paramSymbol, 0);

        Assert.assertTrue(result.isPresent());
        Assert.assertTrue(result.get() instanceof ArrayFunctionParam);
        ArrayFunctionParam arrayParam = (ArrayFunctionParam) result.get();
        Assert.assertTrue(arrayParam.isRenderAsTable());
    }

    @Test
    public void testCreateFunctionParam_ArrayWithFloatElements() {
        ParameterSymbol paramSymbol = mock(ParameterSymbol.class);
        ArrayTypeSymbol arrayTypeSymbol = mock(ArrayTypeSymbol.class);
        TypeSymbol elementTypeSymbol = mock(TypeSymbol.class);

        when(paramSymbol.typeDescriptor()).thenReturn(arrayTypeSymbol);
        when(paramSymbol.getName()).thenReturn(Optional.of("rates"));
        when(paramSymbol.paramKind()).thenReturn(ParameterKind.REQUIRED);
        when(arrayTypeSymbol.typeKind()).thenReturn(TypeDescKind.ARRAY);
        when(arrayTypeSymbol.memberTypeDescriptor()).thenReturn(elementTypeSymbol);
        when(elementTypeSymbol.typeKind()).thenReturn(TypeDescKind.FLOAT);

        Optional<FunctionParam> result = ParamFactory.createFunctionParam(paramSymbol, 0);

        Assert.assertTrue(result.isPresent());
        Assert.assertTrue(result.get() instanceof ArrayFunctionParam);
        ArrayFunctionParam arrayParam = (ArrayFunctionParam) result.get();
        Assert.assertTrue(arrayParam.isRenderAsTable());
    }

    @Test
    public void testCreateFunctionParam_ArrayWithDecimalElements() {
        ParameterSymbol paramSymbol = mock(ParameterSymbol.class);
        ArrayTypeSymbol arrayTypeSymbol = mock(ArrayTypeSymbol.class);
        TypeSymbol elementTypeSymbol = mock(TypeSymbol.class);

        when(paramSymbol.typeDescriptor()).thenReturn(arrayTypeSymbol);
        when(paramSymbol.getName()).thenReturn(Optional.of("prices"));
        when(paramSymbol.paramKind()).thenReturn(ParameterKind.REQUIRED);
        when(arrayTypeSymbol.typeKind()).thenReturn(TypeDescKind.ARRAY);
        when(arrayTypeSymbol.memberTypeDescriptor()).thenReturn(elementTypeSymbol);
        when(elementTypeSymbol.typeKind()).thenReturn(TypeDescKind.DECIMAL);

        Optional<FunctionParam> result = ParamFactory.createFunctionParam(paramSymbol, 0);

        Assert.assertTrue(result.isPresent());
        Assert.assertTrue(result.get() instanceof ArrayFunctionParam);
        ArrayFunctionParam arrayParam = (ArrayFunctionParam) result.get();
        Assert.assertTrue(arrayParam.isRenderAsTable());
    }

    @Test
    public void testCreateFunctionParam_ArrayWithComplexElements() {
        ParameterSymbol paramSymbol = mock(ParameterSymbol.class);
        ArrayTypeSymbol arrayTypeSymbol = mock(ArrayTypeSymbol.class);
        TypeSymbol elementTypeSymbol = mock(TypeSymbol.class);

        when(paramSymbol.typeDescriptor()).thenReturn(arrayTypeSymbol);
        when(paramSymbol.getName()).thenReturn(Optional.of("jsonArray"));
        when(paramSymbol.paramKind()).thenReturn(ParameterKind.REQUIRED);
        when(arrayTypeSymbol.typeKind()).thenReturn(TypeDescKind.ARRAY);
        when(arrayTypeSymbol.memberTypeDescriptor()).thenReturn(elementTypeSymbol);
        when(elementTypeSymbol.typeKind()).thenReturn(TypeDescKind.JSON);

        Optional<FunctionParam> result = ParamFactory.createFunctionParam(paramSymbol, 0);

        Assert.assertTrue(result.isPresent());
        Assert.assertTrue(result.get() instanceof ArrayFunctionParam);
        ArrayFunctionParam arrayParam = (ArrayFunctionParam) result.get();
        // JSON array should not render as table
        Assert.assertFalse(arrayParam.isRenderAsTable());
    }

    @Test
    public void testCreateFunctionParam_MapWithIntValueType() {
        ParameterSymbol paramSymbol = mock(ParameterSymbol.class);
        MapTypeSymbol mapTypeSymbol = mock(MapTypeSymbol.class);
        TypeSymbol valueTypeSymbol = mock(TypeSymbol.class);

        when(paramSymbol.typeDescriptor()).thenReturn(mapTypeSymbol);
        when(paramSymbol.getName()).thenReturn(Optional.of("counts"));
        when(paramSymbol.paramKind()).thenReturn(ParameterKind.REQUIRED);
        when(mapTypeSymbol.typeKind()).thenReturn(TypeDescKind.MAP);
        when(mapTypeSymbol.typeParameter()).thenReturn(Optional.of(valueTypeSymbol));
        when(valueTypeSymbol.typeKind()).thenReturn(TypeDescKind.INT);

        Optional<FunctionParam> result = ParamFactory.createFunctionParam(paramSymbol, 0);

        Assert.assertTrue(result.isPresent());
        Assert.assertTrue(result.get() instanceof MapFunctionParam);
        MapFunctionParam mapParam = (MapFunctionParam) result.get();
        Assert.assertTrue(mapParam.isRenderAsTable());
    }

    @Test
    public void testCreateFunctionParam_MapWithBooleanValueType() {
        ParameterSymbol paramSymbol = mock(ParameterSymbol.class);
        MapTypeSymbol mapTypeSymbol = mock(MapTypeSymbol.class);
        TypeSymbol valueTypeSymbol = mock(TypeSymbol.class);

        when(paramSymbol.typeDescriptor()).thenReturn(mapTypeSymbol);
        when(paramSymbol.getName()).thenReturn(Optional.of("flags"));
        when(paramSymbol.paramKind()).thenReturn(ParameterKind.REQUIRED);
        when(mapTypeSymbol.typeKind()).thenReturn(TypeDescKind.MAP);
        when(mapTypeSymbol.typeParameter()).thenReturn(Optional.of(valueTypeSymbol));
        when(valueTypeSymbol.typeKind()).thenReturn(TypeDescKind.BOOLEAN);

        Optional<FunctionParam> result = ParamFactory.createFunctionParam(paramSymbol, 0);

        Assert.assertTrue(result.isPresent());
        Assert.assertTrue(result.get() instanceof MapFunctionParam);
        MapFunctionParam mapParam = (MapFunctionParam) result.get();
        Assert.assertTrue(mapParam.isRenderAsTable());
    }

    @Test
    public void testCreateFunctionParam_MapWithFloatValueType() {
        ParameterSymbol paramSymbol = mock(ParameterSymbol.class);
        MapTypeSymbol mapTypeSymbol = mock(MapTypeSymbol.class);
        TypeSymbol valueTypeSymbol = mock(TypeSymbol.class);

        when(paramSymbol.typeDescriptor()).thenReturn(mapTypeSymbol);
        when(paramSymbol.getName()).thenReturn(Optional.of("rates"));
        when(paramSymbol.paramKind()).thenReturn(ParameterKind.REQUIRED);
        when(mapTypeSymbol.typeKind()).thenReturn(TypeDescKind.MAP);
        when(mapTypeSymbol.typeParameter()).thenReturn(Optional.of(valueTypeSymbol));
        when(valueTypeSymbol.typeKind()).thenReturn(TypeDescKind.FLOAT);

        Optional<FunctionParam> result = ParamFactory.createFunctionParam(paramSymbol, 0);

        Assert.assertTrue(result.isPresent());
        Assert.assertTrue(result.get() instanceof MapFunctionParam);
        MapFunctionParam mapParam = (MapFunctionParam) result.get();
        Assert.assertTrue(mapParam.isRenderAsTable());
    }

    @Test
    public void testCreateFunctionParam_MapWithDecimalValueType() {
        ParameterSymbol paramSymbol = mock(ParameterSymbol.class);
        MapTypeSymbol mapTypeSymbol = mock(MapTypeSymbol.class);
        TypeSymbol valueTypeSymbol = mock(TypeSymbol.class);

        when(paramSymbol.typeDescriptor()).thenReturn(mapTypeSymbol);
        when(paramSymbol.getName()).thenReturn(Optional.of("prices"));
        when(paramSymbol.paramKind()).thenReturn(ParameterKind.REQUIRED);
        when(mapTypeSymbol.typeKind()).thenReturn(TypeDescKind.MAP);
        when(mapTypeSymbol.typeParameter()).thenReturn(Optional.of(valueTypeSymbol));
        when(valueTypeSymbol.typeKind()).thenReturn(TypeDescKind.DECIMAL);

        Optional<FunctionParam> result = ParamFactory.createFunctionParam(paramSymbol, 0);

        Assert.assertTrue(result.isPresent());
        Assert.assertTrue(result.get() instanceof MapFunctionParam);
        MapFunctionParam mapParam = (MapFunctionParam) result.get();
        Assert.assertTrue(mapParam.isRenderAsTable());
    }

    @Test
    public void testCreateFunctionParam_MapWithEmptyRecordValue() {
        ParameterSymbol paramSymbol = mock(ParameterSymbol.class);
        MapTypeSymbol mapTypeSymbol = mock(MapTypeSymbol.class);
        RecordTypeSymbol valueRecordType = mock(RecordTypeSymbol.class);

        when(paramSymbol.typeDescriptor()).thenReturn(mapTypeSymbol);
        when(paramSymbol.getName()).thenReturn(Optional.of("emptyRecords"));
        when(paramSymbol.paramKind()).thenReturn(ParameterKind.REQUIRED);
        when(mapTypeSymbol.typeKind()).thenReturn(TypeDescKind.MAP);
        when(mapTypeSymbol.typeParameter()).thenReturn(Optional.of(valueRecordType));
        when(valueRecordType.typeKind()).thenReturn(TypeDescKind.RECORD);
        when(valueRecordType.fieldDescriptors()).thenReturn(Collections.emptyMap());

        Optional<FunctionParam> result = ParamFactory.createFunctionParam(paramSymbol, 0);

        Assert.assertTrue(result.isPresent());
        Assert.assertTrue(result.get() instanceof MapFunctionParam);
        MapFunctionParam mapParam = (MapFunctionParam) result.get();
        // Empty record should not render as table
        Assert.assertFalse(mapParam.isRenderAsTable());
    }

    @Test
    public void testCreateFunctionParam_ArrayWithEmptyRecordElements() {
        ParameterSymbol paramSymbol = mock(ParameterSymbol.class);
        ArrayTypeSymbol arrayTypeSymbol = mock(ArrayTypeSymbol.class);
        RecordTypeSymbol elementRecordType = mock(RecordTypeSymbol.class);

        when(paramSymbol.typeDescriptor()).thenReturn(arrayTypeSymbol);
        when(paramSymbol.getName()).thenReturn(Optional.of("emptyRecords"));
        when(paramSymbol.paramKind()).thenReturn(ParameterKind.REQUIRED);
        when(arrayTypeSymbol.typeKind()).thenReturn(TypeDescKind.ARRAY);
        when(arrayTypeSymbol.memberTypeDescriptor()).thenReturn(elementRecordType);
        when(elementRecordType.typeKind()).thenReturn(TypeDescKind.RECORD);
        when(elementRecordType.fieldDescriptors()).thenReturn(Collections.emptyMap());

        Optional<FunctionParam> result = ParamFactory.createFunctionParam(paramSymbol, 0);

        Assert.assertTrue(result.isPresent());
        Assert.assertTrue(result.get() instanceof ArrayFunctionParam);
        ArrayFunctionParam arrayParam = (ArrayFunctionParam) result.get();
        // Empty record should not render as table
        Assert.assertFalse(arrayParam.isRenderAsTable());
    }

    @Test
    public void testCreateFunctionParam_UnionWithRecordMember() {
        ParameterSymbol paramSymbol = mock(ParameterSymbol.class);
        UnionTypeSymbol unionTypeSymbol = mock(UnionTypeSymbol.class);
        RecordTypeSymbol recordMember = mock(RecordTypeSymbol.class);
        TypeSymbol stringMember = mock(TypeSymbol.class);

        when(paramSymbol.typeDescriptor()).thenReturn(unionTypeSymbol);
        when(paramSymbol.getName()).thenReturn(Optional.of("mixed"));
        when(paramSymbol.paramKind()).thenReturn(ParameterKind.REQUIRED);
        when(unionTypeSymbol.typeKind()).thenReturn(TypeDescKind.UNION);
        when(unionTypeSymbol.memberTypeDescriptors()).thenReturn(List.of(recordMember, stringMember));

        when(recordMember.typeKind()).thenReturn(TypeDescKind.RECORD);
        when(recordMember.getName()).thenReturn(Optional.of("MyRecord"));
        when(recordMember.fieldDescriptors()).thenReturn(Collections.emptyMap());
        when(stringMember.typeKind()).thenReturn(TypeDescKind.STRING);
        when(stringMember.getName()).thenReturn(Optional.empty());

        Optional<FunctionParam> result = ParamFactory.createFunctionParam(paramSymbol, 0);

        Assert.assertTrue(result.isPresent());
        Assert.assertTrue(result.get() instanceof UnionFunctionParam);
        UnionFunctionParam unionParam = (UnionFunctionParam) result.get();
        Assert.assertEquals(unionParam.getUnionMemberParams().size(), 2);
    }

    @Test
    public void testCreateFunctionParam_UnionWithNestedUnionMember() {
        ParameterSymbol paramSymbol = mock(ParameterSymbol.class);
        UnionTypeSymbol unionTypeSymbol = mock(UnionTypeSymbol.class);
        UnionTypeSymbol nestedUnionMember = mock(UnionTypeSymbol.class);
        TypeSymbol stringMember = mock(TypeSymbol.class);
        TypeSymbol intMember = mock(TypeSymbol.class);

        when(paramSymbol.typeDescriptor()).thenReturn(unionTypeSymbol);
        when(paramSymbol.getName()).thenReturn(Optional.of("complex"));
        when(paramSymbol.paramKind()).thenReturn(ParameterKind.REQUIRED);
        when(unionTypeSymbol.typeKind()).thenReturn(TypeDescKind.UNION);
        when(unionTypeSymbol.memberTypeDescriptors()).thenReturn(List.of(nestedUnionMember, stringMember));

        when(nestedUnionMember.typeKind()).thenReturn(TypeDescKind.UNION);
        when(nestedUnionMember.getName()).thenReturn(Optional.of("NestedUnion"));
        when(nestedUnionMember.memberTypeDescriptors()).thenReturn(List.of(intMember));
        when(stringMember.typeKind()).thenReturn(TypeDescKind.STRING);
        when(stringMember.getName()).thenReturn(Optional.empty());
        when(intMember.typeKind()).thenReturn(TypeDescKind.INT);
        when(intMember.getName()).thenReturn(Optional.empty());

        Optional<FunctionParam> result = ParamFactory.createFunctionParam(paramSymbol, 0);

        Assert.assertTrue(result.isPresent());
        Assert.assertTrue(result.get() instanceof UnionFunctionParam);
        UnionFunctionParam unionParam = (UnionFunctionParam) result.get();
        Assert.assertEquals(unionParam.getUnionMemberParams().size(), 2);
    }

    @Test
    public void testCreateFunctionParam_RecordWithFieldDocumentation() {
        ParameterSymbol paramSymbol = mock(ParameterSymbol.class);
        RecordTypeSymbol recordTypeSymbol = mock(RecordTypeSymbol.class);
        RecordFieldSymbol fieldSymbol = mock(RecordFieldSymbol.class);
        TypeSymbol fieldTypeSymbol = mock(TypeSymbol.class);
        io.ballerina.compiler.api.symbols.Documentation docSymbol = mock(io.ballerina.compiler.api.symbols.Documentation.class);

        when(paramSymbol.typeDescriptor()).thenReturn(recordTypeSymbol);
        when(paramSymbol.getName()).thenReturn(Optional.of("config"));
        when(paramSymbol.paramKind()).thenReturn(ParameterKind.REQUIRED);
        when(recordTypeSymbol.typeKind()).thenReturn(TypeDescKind.RECORD);
        when(recordTypeSymbol.getName()).thenReturn(Optional.of("Config"));

        when(fieldSymbol.typeDescriptor()).thenReturn(fieldTypeSymbol);
        when(fieldSymbol.isOptional()).thenReturn(false);
        when(fieldSymbol.hasDefaultValue()).thenReturn(false);
        when(fieldSymbol.documentation()).thenReturn(Optional.of(docSymbol));
        when(docSymbol.description()).thenReturn(Optional.of("This is a host field"));
        when(fieldTypeSymbol.typeKind()).thenReturn(TypeDescKind.STRING);

        Map<String, RecordFieldSymbol> fields = new HashMap<>();
        fields.put("host", fieldSymbol);
        when(recordTypeSymbol.fieldDescriptors()).thenReturn(fields);

        Optional<FunctionParam> result = ParamFactory.createFunctionParam(paramSymbol, 0);

        Assert.assertTrue(result.isPresent());
        Assert.assertTrue(result.get() instanceof RecordFunctionParam);
        RecordFunctionParam recordParam = (RecordFunctionParam) result.get();
        Assert.assertEquals(recordParam.getRecordFieldParams().size(), 1);
        Assert.assertEquals(recordParam.getRecordFieldParams().get(0).getDescription(), "This is a host field");
    }

    @Test
    public void testCreateFunctionParam_UnionWithDuplicateTypes() {
        ParameterSymbol paramSymbol = mock(ParameterSymbol.class);
        UnionTypeSymbol unionTypeSymbol = mock(UnionTypeSymbol.class);
        TypeSymbol stringMember1 = mock(TypeSymbol.class);
        TypeSymbol stringMember2 = mock(TypeSymbol.class);

        when(paramSymbol.typeDescriptor()).thenReturn(unionTypeSymbol);
        when(paramSymbol.getName()).thenReturn(Optional.of("value"));
        when(paramSymbol.paramKind()).thenReturn(ParameterKind.REQUIRED);
        when(unionTypeSymbol.typeKind()).thenReturn(TypeDescKind.UNION);
        when(unionTypeSymbol.memberTypeDescriptors()).thenReturn(List.of(stringMember1, stringMember2));

        // Both are STRING type - duplicates should be handled
        when(stringMember1.typeKind()).thenReturn(TypeDescKind.STRING);
        when(stringMember1.getName()).thenReturn(Optional.empty());
        when(stringMember2.typeKind()).thenReturn(TypeDescKind.STRING);
        when(stringMember2.getName()).thenReturn(Optional.empty());

        Optional<FunctionParam> result = ParamFactory.createFunctionParam(paramSymbol, 0);

        Assert.assertTrue(result.isPresent());
        // Single string member should be simplified
        Assert.assertTrue(result.get() instanceof FunctionParam);
        Assert.assertEquals(result.get().getParamType(), "string");
    }

    @Test
    public void testCreateFunctionParam_RecordWithUnsupportedFieldType() {
        ParameterSymbol paramSymbol = mock(ParameterSymbol.class);
        RecordTypeSymbol recordTypeSymbol = mock(RecordTypeSymbol.class);
        RecordFieldSymbol unsupportedFieldSymbol = mock(RecordFieldSymbol.class);
        TypeSymbol unsupportedTypeSymbol = mock(TypeSymbol.class);

        when(paramSymbol.typeDescriptor()).thenReturn(recordTypeSymbol);
        when(paramSymbol.getName()).thenReturn(Optional.of("config"));
        when(paramSymbol.paramKind()).thenReturn(ParameterKind.REQUIRED);
        when(recordTypeSymbol.typeKind()).thenReturn(TypeDescKind.RECORD);
        when(recordTypeSymbol.getName()).thenReturn(Optional.of("Config"));

        // Field with unsupported type (FUNCTION)
        when(unsupportedFieldSymbol.typeDescriptor()).thenReturn(unsupportedTypeSymbol);
        when(unsupportedFieldSymbol.isOptional()).thenReturn(false);
        when(unsupportedFieldSymbol.hasDefaultValue()).thenReturn(false);
        when(unsupportedFieldSymbol.documentation()).thenReturn(Optional.empty());
        when(unsupportedTypeSymbol.typeKind()).thenReturn(TypeDescKind.FUNCTION);

        Map<String, RecordFieldSymbol> fields = new HashMap<>();
        fields.put("callback", unsupportedFieldSymbol);
        when(recordTypeSymbol.fieldDescriptors()).thenReturn(fields);

        Optional<FunctionParam> result = ParamFactory.createFunctionParam(paramSymbol, 0);

        Assert.assertTrue(result.isPresent());
        Assert.assertTrue(result.get() instanceof RecordFunctionParam);
        RecordFunctionParam recordParam = (RecordFunctionParam) result.get();
        // Unsupported field types should be skipped
        Assert.assertEquals(recordParam.getRecordFieldParams().size(), 0);
    }

    @Test
    public void testCreateFunctionParam_RecordWithNestedArray2D() {
        ParameterSymbol paramSymbol = mock(ParameterSymbol.class);
        RecordTypeSymbol recordTypeSymbol = mock(RecordTypeSymbol.class);
        RecordFieldSymbol arrayFieldSymbol = mock(RecordFieldSymbol.class);
        ArrayTypeSymbol outerArrayType = mock(ArrayTypeSymbol.class);
        ArrayTypeSymbol innerArrayType = mock(ArrayTypeSymbol.class);
        TypeSymbol elementTypeSymbol = mock(TypeSymbol.class);

        when(paramSymbol.typeDescriptor()).thenReturn(recordTypeSymbol);
        when(paramSymbol.getName()).thenReturn(Optional.of("data"));
        when(paramSymbol.paramKind()).thenReturn(ParameterKind.REQUIRED);
        when(recordTypeSymbol.typeKind()).thenReturn(TypeDescKind.RECORD);
        when(recordTypeSymbol.getName()).thenReturn(Optional.of("Data"));

        // 2D array field
        when(arrayFieldSymbol.typeDescriptor()).thenReturn(outerArrayType);
        when(arrayFieldSymbol.isOptional()).thenReturn(false);
        when(arrayFieldSymbol.hasDefaultValue()).thenReturn(false);
        when(arrayFieldSymbol.documentation()).thenReturn(Optional.empty());
        when(outerArrayType.typeKind()).thenReturn(TypeDescKind.ARRAY);
        when(outerArrayType.memberTypeDescriptor()).thenReturn(innerArrayType);
        when(innerArrayType.typeKind()).thenReturn(TypeDescKind.ARRAY);
        when(innerArrayType.memberTypeDescriptor()).thenReturn(elementTypeSymbol);
        when(elementTypeSymbol.typeKind()).thenReturn(TypeDescKind.INT);

        Map<String, RecordFieldSymbol> fields = new HashMap<>();
        fields.put("matrix", arrayFieldSymbol);
        when(recordTypeSymbol.fieldDescriptors()).thenReturn(fields);

        Optional<FunctionParam> result = ParamFactory.createFunctionParam(paramSymbol, 0);

        Assert.assertTrue(result.isPresent());
        Assert.assertTrue(result.get() instanceof RecordFunctionParam);
        RecordFunctionParam recordParam = (RecordFunctionParam) result.get();
        Assert.assertEquals(recordParam.getRecordFieldParams().size(), 1);
        Assert.assertTrue(recordParam.getRecordFieldParams().get(0) instanceof ArrayFunctionParam);
        ArrayFunctionParam arrayParam = (ArrayFunctionParam) recordParam.getRecordFieldParams().get(0);
        Assert.assertTrue(arrayParam.is2DArray());
    }

    @Test
    public void testCreateFunctionParam_RecordWithUnionArrayField() {
        ParameterSymbol paramSymbol = mock(ParameterSymbol.class);
        RecordTypeSymbol recordTypeSymbol = mock(RecordTypeSymbol.class);
        RecordFieldSymbol arrayFieldSymbol = mock(RecordFieldSymbol.class);
        ArrayTypeSymbol arrayTypeSymbol = mock(ArrayTypeSymbol.class);
        UnionTypeSymbol unionElementType = mock(UnionTypeSymbol.class);
        TypeSymbol stringMember = mock(TypeSymbol.class);
        TypeSymbol intMember = mock(TypeSymbol.class);

        when(paramSymbol.typeDescriptor()).thenReturn(recordTypeSymbol);
        when(paramSymbol.getName()).thenReturn(Optional.of("data"));
        when(paramSymbol.paramKind()).thenReturn(ParameterKind.REQUIRED);
        when(recordTypeSymbol.typeKind()).thenReturn(TypeDescKind.RECORD);
        when(recordTypeSymbol.getName()).thenReturn(Optional.of("Data"));

        // Union array field
        when(arrayFieldSymbol.typeDescriptor()).thenReturn(arrayTypeSymbol);
        when(arrayFieldSymbol.isOptional()).thenReturn(false);
        when(arrayFieldSymbol.hasDefaultValue()).thenReturn(false);
        when(arrayFieldSymbol.documentation()).thenReturn(Optional.empty());
        when(arrayTypeSymbol.typeKind()).thenReturn(TypeDescKind.ARRAY);
        when(arrayTypeSymbol.memberTypeDescriptor()).thenReturn(unionElementType);
        when(unionElementType.typeKind()).thenReturn(TypeDescKind.UNION);
        when(unionElementType.memberTypeDescriptors()).thenReturn(List.of(stringMember, intMember));
        when(stringMember.typeKind()).thenReturn(TypeDescKind.STRING);
        when(intMember.typeKind()).thenReturn(TypeDescKind.INT);

        Map<String, RecordFieldSymbol> fields = new HashMap<>();
        fields.put("mixedArray", arrayFieldSymbol);
        when(recordTypeSymbol.fieldDescriptors()).thenReturn(fields);

        Optional<FunctionParam> result = ParamFactory.createFunctionParam(paramSymbol, 0);

        Assert.assertTrue(result.isPresent());
        Assert.assertTrue(result.get() instanceof RecordFunctionParam);
        RecordFunctionParam recordParam = (RecordFunctionParam) result.get();
        Assert.assertEquals(recordParam.getRecordFieldParams().size(), 1);
        Assert.assertTrue(recordParam.getRecordFieldParams().get(0) instanceof ArrayFunctionParam);
        ArrayFunctionParam arrayParam = (ArrayFunctionParam) recordParam.getRecordFieldParams().get(0);
        Assert.assertTrue(arrayParam.isUnionArray());
    }

    @Test
    public void testCreateFunctionParam_RecordWithMapHavingRecordValue() {
        ParameterSymbol paramSymbol = mock(ParameterSymbol.class);
        RecordTypeSymbol recordTypeSymbol = mock(RecordTypeSymbol.class);
        RecordFieldSymbol mapFieldSymbol = mock(RecordFieldSymbol.class);
        MapTypeSymbol mapTypeSymbol = mock(MapTypeSymbol.class);
        RecordTypeSymbol valueRecordType = mock(RecordTypeSymbol.class);
        RecordFieldSymbol innerFieldSymbol = mock(RecordFieldSymbol.class);
        TypeSymbol innerFieldTypeSymbol = mock(TypeSymbol.class);

        when(paramSymbol.typeDescriptor()).thenReturn(recordTypeSymbol);
        when(paramSymbol.getName()).thenReturn(Optional.of("config"));
        when(paramSymbol.paramKind()).thenReturn(ParameterKind.REQUIRED);
        when(recordTypeSymbol.typeKind()).thenReturn(TypeDescKind.RECORD);
        when(recordTypeSymbol.getName()).thenReturn(Optional.of("Config"));

        // Map field with record value
        when(mapFieldSymbol.typeDescriptor()).thenReturn(mapTypeSymbol);
        when(mapFieldSymbol.isOptional()).thenReturn(false);
        when(mapFieldSymbol.hasDefaultValue()).thenReturn(false);
        when(mapFieldSymbol.documentation()).thenReturn(Optional.empty());
        when(mapTypeSymbol.typeKind()).thenReturn(TypeDescKind.MAP);
        when(mapTypeSymbol.typeParameter()).thenReturn(Optional.of(valueRecordType));
        when(valueRecordType.typeKind()).thenReturn(TypeDescKind.RECORD);

        // Inner record fields
        when(innerFieldSymbol.typeDescriptor()).thenReturn(innerFieldTypeSymbol);
        when(innerFieldSymbol.isOptional()).thenReturn(false);
        when(innerFieldSymbol.documentation()).thenReturn(Optional.empty());
        when(innerFieldTypeSymbol.typeKind()).thenReturn(TypeDescKind.STRING);

        Map<String, RecordFieldSymbol> innerFields = new HashMap<>();
        innerFields.put("name", innerFieldSymbol);
        when(valueRecordType.fieldDescriptors()).thenReturn(innerFields);

        Map<String, RecordFieldSymbol> fields = new HashMap<>();
        fields.put("users", mapFieldSymbol);
        when(recordTypeSymbol.fieldDescriptors()).thenReturn(fields);

        Optional<FunctionParam> result = ParamFactory.createFunctionParam(paramSymbol, 0);

        Assert.assertTrue(result.isPresent());
        Assert.assertTrue(result.get() instanceof RecordFunctionParam);
        RecordFunctionParam recordParam = (RecordFunctionParam) result.get();
        Assert.assertEquals(recordParam.getRecordFieldParams().size(), 1);
        Assert.assertTrue(recordParam.getRecordFieldParams().get(0) instanceof MapFunctionParam);
        MapFunctionParam mapParam = (MapFunctionParam) recordParam.getRecordFieldParams().get(0);
        Assert.assertTrue(mapParam.isRenderAsTable());
        Assert.assertEquals(mapParam.getValueFieldParams().size(), 1);
    }

    @Test
    public void testCreateFunctionParam_RecordWithArrayHavingRecordElements() {
        ParameterSymbol paramSymbol = mock(ParameterSymbol.class);
        RecordTypeSymbol recordTypeSymbol = mock(RecordTypeSymbol.class);
        RecordFieldSymbol arrayFieldSymbol = mock(RecordFieldSymbol.class);
        ArrayTypeSymbol arrayTypeSymbol = mock(ArrayTypeSymbol.class);
        RecordTypeSymbol elementRecordType = mock(RecordTypeSymbol.class);
        RecordFieldSymbol innerFieldSymbol = mock(RecordFieldSymbol.class);
        TypeSymbol innerFieldTypeSymbol = mock(TypeSymbol.class);

        when(paramSymbol.typeDescriptor()).thenReturn(recordTypeSymbol);
        when(paramSymbol.getName()).thenReturn(Optional.of("config"));
        when(paramSymbol.paramKind()).thenReturn(ParameterKind.REQUIRED);
        when(recordTypeSymbol.typeKind()).thenReturn(TypeDescKind.RECORD);
        when(recordTypeSymbol.getName()).thenReturn(Optional.of("Config"));

        // Array field with record elements
        when(arrayFieldSymbol.typeDescriptor()).thenReturn(arrayTypeSymbol);
        when(arrayFieldSymbol.isOptional()).thenReturn(false);
        when(arrayFieldSymbol.hasDefaultValue()).thenReturn(false);
        when(arrayFieldSymbol.documentation()).thenReturn(Optional.empty());
        when(arrayTypeSymbol.typeKind()).thenReturn(TypeDescKind.ARRAY);
        when(arrayTypeSymbol.memberTypeDescriptor()).thenReturn(elementRecordType);
        when(elementRecordType.typeKind()).thenReturn(TypeDescKind.RECORD);

        // Inner record fields
        when(innerFieldSymbol.typeDescriptor()).thenReturn(innerFieldTypeSymbol);
        when(innerFieldSymbol.isOptional()).thenReturn(false);
        when(innerFieldSymbol.documentation()).thenReturn(Optional.empty());
        when(innerFieldTypeSymbol.typeKind()).thenReturn(TypeDescKind.STRING);

        Map<String, RecordFieldSymbol> innerFields = new HashMap<>();
        innerFields.put("name", innerFieldSymbol);
        when(elementRecordType.fieldDescriptors()).thenReturn(innerFields);

        Map<String, RecordFieldSymbol> fields = new HashMap<>();
        fields.put("users", arrayFieldSymbol);
        when(recordTypeSymbol.fieldDescriptors()).thenReturn(fields);

        Optional<FunctionParam> result = ParamFactory.createFunctionParam(paramSymbol, 0);

        Assert.assertTrue(result.isPresent());
        Assert.assertTrue(result.get() instanceof RecordFunctionParam);
        RecordFunctionParam recordParam = (RecordFunctionParam) result.get();
        Assert.assertEquals(recordParam.getRecordFieldParams().size(), 1);
        Assert.assertTrue(recordParam.getRecordFieldParams().get(0) instanceof ArrayFunctionParam);
        ArrayFunctionParam arrayParam = (ArrayFunctionParam) recordParam.getRecordFieldParams().get(0);
        Assert.assertTrue(arrayParam.isRenderAsTable());
        Assert.assertEquals(arrayParam.getElementFieldParams().size(), 1);
    }

    @Test
    public void testCreateFunctionParam_RecordNameFromTypeSymbol() {
        ParameterSymbol paramSymbol = mock(ParameterSymbol.class);
        RecordTypeSymbol recordTypeSymbol = mock(RecordTypeSymbol.class);

        when(paramSymbol.typeDescriptor()).thenReturn(recordTypeSymbol);
        when(paramSymbol.getName()).thenReturn(Optional.of("config"));
        when(paramSymbol.paramKind()).thenReturn(ParameterKind.REQUIRED);
        when(recordTypeSymbol.typeKind()).thenReturn(TypeDescKind.RECORD);
        // No name on type symbol - should fall back to param name
        when(recordTypeSymbol.getName()).thenReturn(Optional.empty());
        when(recordTypeSymbol.fieldDescriptors()).thenReturn(Collections.emptyMap());

        Optional<FunctionParam> result = ParamFactory.createFunctionParam(paramSymbol, 0);

        Assert.assertTrue(result.isPresent());
        Assert.assertTrue(result.get() instanceof RecordFunctionParam);
        RecordFunctionParam recordParam = (RecordFunctionParam) result.get();
        // Should use param name when record name not available
        Assert.assertEquals(recordParam.getRecordName(), "config");
    }

    @Test
    public void testCreateFunctionParam_UnionSingleMemberIsUnion() {
        ParameterSymbol paramSymbol = mock(ParameterSymbol.class);
        UnionTypeSymbol unionTypeSymbol = mock(UnionTypeSymbol.class);
        UnionTypeSymbol nestedUnion = mock(UnionTypeSymbol.class);
        TypeSymbol nilMember = mock(TypeSymbol.class);
        TypeSymbol stringMember = mock(TypeSymbol.class);
        TypeSymbol intMember = mock(TypeSymbol.class);

        when(paramSymbol.typeDescriptor()).thenReturn(unionTypeSymbol);
        when(paramSymbol.getName()).thenReturn(Optional.of("value"));
        when(paramSymbol.paramKind()).thenReturn(ParameterKind.REQUIRED);
        when(unionTypeSymbol.typeKind()).thenReturn(TypeDescKind.UNION);
        when(unionTypeSymbol.memberTypeDescriptors()).thenReturn(List.of(nestedUnion, nilMember));

        when(nestedUnion.typeKind()).thenReturn(TypeDescKind.UNION);
        when(nestedUnion.getName()).thenReturn(Optional.of("InnerUnion"));
        when(nestedUnion.memberTypeDescriptors()).thenReturn(List.of(stringMember, intMember));
        when(nilMember.typeKind()).thenReturn(TypeDescKind.NIL);
        when(nilMember.getName()).thenReturn(Optional.empty());
        when(stringMember.typeKind()).thenReturn(TypeDescKind.STRING);
        when(stringMember.getName()).thenReturn(Optional.empty());
        when(intMember.typeKind()).thenReturn(TypeDescKind.INT);
        when(intMember.getName()).thenReturn(Optional.empty());

        Optional<FunctionParam> result = ParamFactory.createFunctionParam(paramSymbol, 0);

        Assert.assertTrue(result.isPresent());
        // Single member that is itself a union should NOT be simplified
        Assert.assertTrue(result.get() instanceof UnionFunctionParam);
    }

    @Test
    public void testCreateFunctionParam_RecordWithUnionFieldSingleNonNilMember() {
        ParameterSymbol paramSymbol = mock(ParameterSymbol.class);
        RecordTypeSymbol recordTypeSymbol = mock(RecordTypeSymbol.class);
        RecordFieldSymbol unionFieldSymbol = mock(RecordFieldSymbol.class);
        // Must use BallerinaUnionTypeSymbol as the code checks for this specific type
        BallerinaUnionTypeSymbol unionTypeSymbol = mock(BallerinaUnionTypeSymbol.class);
        TypeSymbol stringMember = mock(TypeSymbol.class);
        TypeSymbol nilMember = mock(TypeSymbol.class);

        when(paramSymbol.typeDescriptor()).thenReturn(recordTypeSymbol);
        when(paramSymbol.getName()).thenReturn(Optional.of("request"));
        when(paramSymbol.paramKind()).thenReturn(ParameterKind.REQUIRED);
        when(recordTypeSymbol.typeKind()).thenReturn(TypeDescKind.RECORD);
        when(recordTypeSymbol.getName()).thenReturn(Optional.of("Request"));

        // Union field with single non-nil member
        when(unionFieldSymbol.typeDescriptor()).thenReturn(unionTypeSymbol);
        when(unionFieldSymbol.isOptional()).thenReturn(false);
        when(unionFieldSymbol.hasDefaultValue()).thenReturn(false);
        when(unionFieldSymbol.documentation()).thenReturn(Optional.empty());
        when(unionTypeSymbol.typeKind()).thenReturn(TypeDescKind.UNION);
        when(unionTypeSymbol.memberTypeDescriptors()).thenReturn(List.of(stringMember, nilMember));

        when(stringMember.typeKind()).thenReturn(TypeDescKind.STRING);
        when(stringMember.getName()).thenReturn(Optional.empty());
        when(nilMember.typeKind()).thenReturn(TypeDescKind.NIL);
        when(nilMember.getName()).thenReturn(Optional.empty());

        Map<String, RecordFieldSymbol> fields = new HashMap<>();
        fields.put("optionalValue", unionFieldSymbol);
        when(recordTypeSymbol.fieldDescriptors()).thenReturn(fields);

        Optional<FunctionParam> result = ParamFactory.createFunctionParam(paramSymbol, 0);

        Assert.assertTrue(result.isPresent());
        Assert.assertTrue(result.get() instanceof RecordFunctionParam);
        RecordFunctionParam recordParam = (RecordFunctionParam) result.get();
        Assert.assertEquals(recordParam.getRecordFieldParams().size(), 1);
        // Single non-nil member union should be simplified to FunctionParam
        FunctionParam fieldParam = recordParam.getRecordFieldParams().get(0);
        Assert.assertFalse(fieldParam instanceof UnionFunctionParam);
        Assert.assertEquals(fieldParam.getParamType(), "string");
    }

    @Test
    public void testCreateFunctionParam_RecordWithUnionFieldAllNilMembers() {
        ParameterSymbol paramSymbol = mock(ParameterSymbol.class);
        RecordTypeSymbol recordTypeSymbol = mock(RecordTypeSymbol.class);
        RecordFieldSymbol unionFieldSymbol = mock(RecordFieldSymbol.class);
        // Must use BallerinaUnionTypeSymbol as the code checks for this specific type
        BallerinaUnionTypeSymbol unionTypeSymbol = mock(BallerinaUnionTypeSymbol.class);
        TypeSymbol nilMember = mock(TypeSymbol.class);

        when(paramSymbol.typeDescriptor()).thenReturn(recordTypeSymbol);
        when(paramSymbol.getName()).thenReturn(Optional.of("request"));
        when(paramSymbol.paramKind()).thenReturn(ParameterKind.REQUIRED);
        when(recordTypeSymbol.typeKind()).thenReturn(TypeDescKind.RECORD);
        when(recordTypeSymbol.getName()).thenReturn(Optional.of("Request"));

        // Union field with only nil members
        when(unionFieldSymbol.typeDescriptor()).thenReturn(unionTypeSymbol);
        when(unionFieldSymbol.isOptional()).thenReturn(false);
        when(unionFieldSymbol.hasDefaultValue()).thenReturn(false);
        when(unionFieldSymbol.documentation()).thenReturn(Optional.empty());
        when(unionTypeSymbol.typeKind()).thenReturn(TypeDescKind.UNION);
        when(unionTypeSymbol.memberTypeDescriptors()).thenReturn(List.of(nilMember));

        when(nilMember.typeKind()).thenReturn(TypeDescKind.NIL);
        when(nilMember.getName()).thenReturn(Optional.empty());

        Map<String, RecordFieldSymbol> fields = new HashMap<>();
        fields.put("nilOnly", unionFieldSymbol);
        when(recordTypeSymbol.fieldDescriptors()).thenReturn(fields);

        Optional<FunctionParam> result = ParamFactory.createFunctionParam(paramSymbol, 0);

        Assert.assertTrue(result.isPresent());
        Assert.assertTrue(result.get() instanceof RecordFunctionParam);
        RecordFunctionParam recordParam = (RecordFunctionParam) result.get();
        // Empty union field should be skipped
        Assert.assertEquals(recordParam.getRecordFieldParams().size(), 0);
    }
}
