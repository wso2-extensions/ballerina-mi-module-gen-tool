package io.ballerina.stdlib.mi.plugin.connectorModel;

import io.ballerina.compiler.api.impl.symbols.BallerinaRecordTypeSymbol;

public class RecordDataType implements DataType {
    private BallerinaRecordTypeSymbol recordSymbol;
    private String recordName;

    public RecordDataType(BallerinaRecordTypeSymbol recordSymbol, String recordName) {
        this.recordSymbol = recordSymbol;
        this.recordName = recordName;
    }

    public void setRecordSymbol(BallerinaRecordTypeSymbol recordSymbol) {
        this.recordSymbol = recordSymbol;
    }

    public BallerinaRecordTypeSymbol getRecordSymbol() {
        return this.recordSymbol;
    }

    @Override
    public void getSynapseRepresentation() {

    }

    public String getRecordName() {
        return recordName;
    }
}
