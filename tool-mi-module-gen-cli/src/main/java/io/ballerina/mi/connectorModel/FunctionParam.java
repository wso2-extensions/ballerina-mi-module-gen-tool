package io.ballerina.mi.connectorModel;

public class FunctionParam extends Param {

    private final String paramType;

    public FunctionParam(String index, String name, String paramType) {
        super(index, name);
        this.paramType = paramType;
    }

    public String getParamType() {
        return paramType;
    }
}
