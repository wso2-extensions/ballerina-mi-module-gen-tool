package io.ballerina.stdlib.mi.plugin.connectorModel;

import io.ballerina.compiler.api.symbols.TypeSymbol;

public class FunctionReturn {
    private String returnType;
    private DataType dataType;


    public FunctionReturn(String returnType) {
        this.returnType = returnType;
    }

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public DataType getDataType() {
        return dataType;
    }

    public void setDataType(DataType dataType) {
        this.dataType = dataType;
    }
}
