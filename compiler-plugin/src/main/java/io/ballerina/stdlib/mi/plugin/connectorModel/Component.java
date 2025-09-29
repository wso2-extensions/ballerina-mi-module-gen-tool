package io.ballerina.stdlib.mi.plugin.connectorModel;

import io.ballerina.stdlib.mi.plugin.Utils;
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
    //TODO: remove if not needed
    private final String index;
    private Connection parent;
    private final ArrayList<Param> params = new ArrayList<>();
    private final List<PathParamType> pathParams;
    private final List<Type> queryParams;
    private final Type returnType;

    public Component(String name, String documentation, FunctionType functionType, String index, List<PathParamType> pathParams, List<Type> queryParams, Type returnType) {
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

    public Type getReturnType() {
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

    public String getIndex() {
        return index;
    }
}
