package io.ballerina.stdlib.mi.plugin.connectorModel.attributeModel;

public class Combo extends Element {
    private final String name;
    private final String displayName;
    private final String inputType;
    private final String comboValues;
    private final Boolean required;
    private final String helpTip;

    public Combo (String name, String displayName, String inputType, String comboValues, Boolean required,
                  String helpTip) {
        this.name = name;
        this.displayName = displayName;
        this.inputType = inputType;
        this.comboValues = comboValues;
        this.required = required;
        this.helpTip = helpTip;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getInputType() {
        return inputType;
    }

    public String getComboValues() {
        return comboValues;
    }

    public Boolean getRequired() {
        return required;
    }

    public String getHelpTip() {
        return helpTip;
    }
}
