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
 
package io.ballerina.mi.model.attribute;

import java.util.List;

public class Table extends Element {

    @Override
    public String getElementType() {
        return "table";
    }

    private final String name;
    private final String displayName;
    private final String title;
    private final String description;
    private final String tableKey;
    private final String tableValue;
    private final List<Element> elements;
    private final String enableCondition;
    private final Boolean required;

    public Table(String name, String displayName, String title, String description,
                 String tableKey, String tableValue, List<Element> elements,
                 String enableCondition, Boolean required) {
        this.name = name;
        this.displayName = displayName;
        this.title = title;
        this.description = description;
        this.tableKey = tableKey;
        this.tableValue = tableValue;
        this.elements = elements;
        this.enableCondition = enableCondition;
        this.required = required;
    }

    @Override
    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getTableKey() {
        return tableKey;
    }

    public String getTableValue() {
        return tableValue;
    }

    public List<Element> getElements() {
        return elements;
    }

    public String getEnableCondition() {
        return enableCondition;
    }

    public Boolean getRequired() {
        return required;
    }
}
