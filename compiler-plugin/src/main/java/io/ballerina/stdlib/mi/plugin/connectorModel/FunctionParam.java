package io.ballerina.stdlib.mi.plugin.connectorModel;

public class FunctionParam extends Param {
    private String paramType;
    private DataType dataType;

    public FunctionParam(String index, String name, String paramType) {
        super(index, name);
        this.paramType = paramType;
    }

    public String getParamType() {
        return paramType;
    }

    public void setParamType(String paramType) {
        this.paramType = paramType;
    }

    public DataType getDataType() {
        return dataType;
    }

    public void setDataType(DataType dataType) {
        this.dataType = dataType;
    }
}
