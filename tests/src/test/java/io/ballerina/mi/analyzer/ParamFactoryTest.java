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
}
