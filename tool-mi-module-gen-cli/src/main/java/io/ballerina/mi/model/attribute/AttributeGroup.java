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

public class AttributeGroup extends Element {

    @Override
    public String getElementType() {
        return "attributeGroup";
    }

    private final String name;

    private final boolean collapsed;

    private String enableCondition;

    public AttributeGroup (String name) {
        this.name = name;
        this.collapsed = false;
    }

    public AttributeGroup (String name, boolean collapsed) {
        this.name = name;
        this.collapsed = collapsed;
    }

    public AttributeGroup (String name, boolean collapsed, String enableCondition) {
        this.name = name;
        this.collapsed = collapsed;
        this.enableCondition = enableCondition;
    }

    @Override
    public String getName() {
        return name;
    }

    public boolean isCollapsed() {
        return collapsed;
    }

    public String getEnableCondition() {
        return enableCondition;
    }

    public void setEnableCondition(String enableCondition) {
        this.enableCondition = enableCondition;
    }
}
