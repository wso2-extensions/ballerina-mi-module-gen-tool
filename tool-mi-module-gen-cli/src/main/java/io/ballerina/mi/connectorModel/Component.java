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

package io.ballerina.mi.connectorModel;

import io.ballerina.mi.ConnectorSerializer;

import org.ballerinalang.diagramutil.connector.models.connector.Type;
import org.ballerinalang.diagramutil.connector.models.connector.types.PathParamType;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Component extends ModelElement {
    private String name;
    private final String documentation;
    //TODO: remove if not needed
    private String objectTypeName;
    private final FunctionType functionType;
    //TODO: remove
    private final String index;
    private Connection parent;
    private final ArrayList<Param> params = new ArrayList<>();
    private final List<FunctionParam> functionParams = new ArrayList<>();
    private final List<PathParamType> pathParams;
    private final List<Type> queryParams;
    private final String returnType;

    public Component(String name, String documentation, FunctionType functionType, String index, List<PathParamType> pathParams, List<Type> queryParams, String returnType) {
        this.name = name;
        this.documentation = documentation;
        this.functionType = functionType;
        this.index = index;
        this.pathParams = pathParams;
        this.queryParams = queryParams;
        this.returnType = returnType;
    }

    public ArrayList<Param> getParams() {
        return params;
    }

    public void setParam(Param param) {
        this.params.add(param);
    }

    public void setFunctionParam(FunctionParam functionParam) {
        this.functionParams.add(functionParam);
    }

    public List<FunctionParam> getFunctionParams() {
        return functionParams;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Connection getParent() {
        return parent;
    }

    public void setParent(Connection parent) {
        this.parent = parent;
    }

    public String getType() {
        return "component";
    }

    public String getObjectTypeName() {
        return objectTypeName;
    }

    public void setObjectTypeName(String objectTypeName) {
        this.objectTypeName = objectTypeName;
    }

    public List<Type> getQueryParams() {
        return queryParams;
    }

    public List<PathParamType> getPathParams() {
        return pathParams;
    }

    public String getDocumentation() {
        return documentation;
    }

    public String getReturnType() {
        return returnType;
    }

    public FunctionType getFunctionType() {
        return functionType;
    }

    public void generateTemplateXml(File connectorFolder, String templatePath, String typeName) {
        File file = new File(connectorFolder, typeName);
        if (!file.exists()) {
            file.mkdir();
        }
        ConnectorSerializer.generateXmlForConnector(templatePath, typeName + "_template", file + File.separator + this.getName(), this);
    }

    public void generateUIJson(File connectorFolder, String templatePath, String fileName) {
        File file = new File(connectorFolder, "uischema");
        if (!file.exists()) {
            file.mkdir();
        }
        ConnectorSerializer.generateJsonForConnector(templatePath, "component", file + File.separator + fileName, this);
    }

    public void generateOutputSchemaJson(File connectorFolder) {
        File file = new File(connectorFolder, "outputschema");
        if (!file.exists()) {
            if (!file.mkdir()) {
                throw new RuntimeException("Failed to create directory: " + file.getAbsolutePath());
            }
        }
//        Utils.generateJson(TYPE_NAME + "_outputschema", file + File.separator + this.name, this);
    }

    public String getIndex() {
        return index;
    }
}
