package io.ballerina.stdlib.mi.plugin.connectorModel;

import io.ballerina.compiler.api.impl.symbols.BallerinaUnionTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;

import java.util.List;

public class UnionDataType implements DataType {
    BallerinaUnionTypeSymbol unionSymbol;

    public UnionDataType(BallerinaUnionTypeSymbol unionSymbol) {
        this.unionSymbol = unionSymbol;
    }

    public void setUnionSymbol(BallerinaUnionTypeSymbol unionSymbol) {
        this.unionSymbol = unionSymbol;
    }

    public BallerinaUnionTypeSymbol getUnionSymbol() {
        return this.unionSymbol;
    }

    @Override
    public void getSynapseRepresentation() {
        //TODO: or originalMemberList?
        List<TypeSymbol> memberTypeList = unionSymbol.memberTypeDescriptors();

        for (TypeSymbol member : memberTypeList) {
                   //TODO: COMPLETE-handle for both reference type and other types
//            if (member instanceof BallerinaTypeReferenceTypeSymbol memberReference) {
//                //TODO: COMPLETE-only handles record type, what others can get a reference type?
//                if (memberReference.typeDescriptor() instanceof BallerinaRecordTypeSymbol recordMember) {
//                    Map<String, RecordFieldSymbol> recordMemberFields = recordMember.fieldDescriptors();
//                    for (Map.Entry<String, RecordFieldSymbol> recordField : recordMemberFields.entrySet()) {
//
//                    }
//                }
//            }
//                    (((BallerinaTypeReferenceTypeSymbol) member).typeDescriptor().fieldDescriptors())
//            component.setReturnTypeMemberList(member.typeKind().getName());

        }
    }
}
