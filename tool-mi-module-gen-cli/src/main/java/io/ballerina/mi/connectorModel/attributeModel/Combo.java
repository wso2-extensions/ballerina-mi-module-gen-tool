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
 
package io.ballerina.mi.connectorModel.attributeModel;

public class Combo extends Element {
    private final String name;
    private final String displayName;
    private final String inputType;
    private final String comboValues;
    private final Boolean required;
    private final String helpTip;
    private final String defaultValue;
    private final String enableCondition;

    public Combo (String name, String displayName, String inputType, String comboValues,String defaultValue, Boolean required,
                  String enableCondition, String helpTip) {
        this.name = name;
        this.displayName = displayName;
        this.inputType = inputType;
        this.comboValues = comboValues;
        this.defaultValue = defaultValue;
        this.required = required;
        this.enableCondition = enableCondition;
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

    public String getDefaultValue() {
        return defaultValue;
    }
}
