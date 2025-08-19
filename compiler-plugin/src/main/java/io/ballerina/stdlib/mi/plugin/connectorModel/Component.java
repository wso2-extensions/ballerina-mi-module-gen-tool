package io.ballerina.stdlib.mi.plugin.connectorModel;

import io.ballerina.stdlib.mi.plugin.Utils;
import org.ballerinalang.diagramutil.connector.models.connector.Type;
import org.ballerinalang.diagramutil.connector.models.connector.types.PathParamType;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Component extends ModelElement {
    private String name;
    private String description = "just a description";
    private ArrayList<Param> params = new ArrayList<>();
    private ArrayList<FunctionParam> balFuncParams = new ArrayList<>();
    private FunctionReturn balFuncReturnType;
    private Connector parent;
    private boolean isConfig = false;
    private String objectTypeName;

    //TODO: New fields - handle access
    public List<PathParamType> pathParams;
    public List<Type> parameters;
    public Type returnType;
    //TODO: remove qualifiers if not needed
    public String[] qualifiers;
    public String documentation;

    public Component(String name) {
        this.name = name;
    }

    public Component(String name, List<Type> queryParams, Type returnType, String[] qualifiers, String documentation) {
        this.name = name;
        this.parameters = queryParams;
        this.returnType = returnType;
        this.qualifiers = qualifiers;
        this.documentation = documentation;
    }

    public Component(String name, List<PathParamType> pathParams, List<Type> queryParams, Type returnType, String[] qualifiers, String documentation) {
        this.name = name;
        this.pathParams = pathParams;
        this.parameters = queryParams;
        this.returnType = returnType;
        this.qualifiers = qualifiers;
        this.documentation = documentation;
    }

    public Component(String name, ArrayList<Param> params) {
        this.name = name;
        this.params = params;
    }

    public ArrayList<Param> getParams() {
        return params;
    }

    public void setParam(Param param) {
        this.params.add(param);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public Connector getParent() {
        return parent;
    }

    public void setParent(Connector parent) {
        this.parent = parent;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return "component";
    }

    public FunctionReturn getBalFuncReturnType() {
        return balFuncReturnType;
    }

    public void setBalFuncReturnType(FunctionReturn returnType) {
        this.balFuncReturnType = returnType;
    }

    public boolean getIsConfig() {
        return isConfig;
    }

    public void setIsConfig(boolean isConfig) {
        this.isConfig = isConfig;
    }

    public String getObjectTypeName() {
        return objectTypeName;
    }

    public void setObjectTypeName(String objectTypeName) {
        this.objectTypeName = objectTypeName;
    }

    public ArrayList<FunctionParam> getBalFuncParams() {
        return balFuncParams;
    }

    public void addBalFuncParams(FunctionParam param) {
        this.balFuncParams.add(param);
    }

    public List<Type> getParameters() {
        return parameters;
    }

    public void generateInstanceXml(File connectorFolder, String templatePath, String typeName) {
        File file = new File(connectorFolder, typeName);
        if (!file.exists()) {
            file.mkdir();
        }
        Utils.generateXmlForConnector(templatePath, "component", file + File.separator + "component", this);
    }

    public void generateTemplateXml(File connectorFolder, String templatePath, String typeName) {
        File file = new File(connectorFolder, typeName);
        if (!file.exists()) {
            file.mkdir();
        }
        Utils.generateXmlForConnector(templatePath, typeName + "_template", file + File.separator + this.getName(), this);
    }

    public void generateUIJson(File connectorFolder, String templatePath, String fileName) {
        File file = new File(connectorFolder, "uischema");
        if (!file.exists()) {
            file.mkdir();
        }
        Utils.generateJsonForConnector(templatePath, "component", file + File.separator + fileName, this);
    }

    public void generateOutputSchemaJson(File connectorFolder) {
        File file = new File(connectorFolder, "outputschema");
        if (!file.exists()) {
            file.mkdir();
        }
//        Utils.generateJson(TYPE_NAME + "_outputschema", file + File.separator + this.name, this);
    }
}
