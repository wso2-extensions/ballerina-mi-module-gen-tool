package io.ballerina.stdlib.mi.plugin.connectorModel.attributeModel;

public class AttributeGroup extends Element {
    private final String name;

    public AttributeGroup (String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
