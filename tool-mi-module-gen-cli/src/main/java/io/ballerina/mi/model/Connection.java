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

package io.ballerina.mi.model;

import java.util.ArrayList;

public class Connection {
    private final Connector parent;
    private final String connectionType;
    private String description;
    private final String objectTypeName;
    // Stable positional identifier used in generated config/template property names.
    private final String index;
    private final ArrayList<Component> components = new ArrayList<>();
    private Component initComponent;


    public Connection(Connector connector, String connectionType, String objectTypeName, String index) {
        this.parent = connector;
        this.connectionType = connectionType;
        this.objectTypeName = objectTypeName;
        this.index = index;
    }

    public ArrayList<Component> getComponents() {
        return components;
    }

    public void setComponent(Component component) {
        component.setParent(this);
        this.components.add(component);
    }

    public Connector getParent() {
        return parent;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getConnectionType() {
        return connectionType;
    }

    public String getObjectTypeName() {
        return objectTypeName;
    }

    public String getIndex() {
        return index;
    }

    public Component getInitComponent() {
        return initComponent;
    }

    public void setInitComponent(Component initComponent) {
        initComponent.setParent(this);
        this.initComponent = initComponent;
    }
}
