package io.ballerina.stdlib.mi.plugin.connectorModel;

import io.ballerina.projects.PackageDescriptor;
import io.ballerina.stdlib.mi.plugin.Constants;
import io.ballerina.stdlib.mi.plugin.Utils;

import java.io.File;
import java.util.ArrayList;

public class Connector extends ModelElement {
    public static final String TYPE_NAME = "connector";
    public static final String TEMP_PATH = "connector";
    public static final String SMALL_ICON_NAME = "icon-small.png";
    public static final String LARGE_ICON_NAME = "icon-large.png";
    public static final String LIB_PATH = "lib";
    private static Connector connector = null;
    private final ArrayList<Connection> connections = new ArrayList<>();
    private String description = "helps to connect with external systems";
    private String iconPath;
    private String version;
    private final String orgName;
    private final String moduleName;
    private final String majorVersion;

    public String getOrgName() {
        return orgName;
    }

    public String getModuleName() {
        return moduleName;
    }

    public String getMajorVersion() {
        return majorVersion;
    }

    private Connector(String moduleName, String orgName, String majorVersion) {
        this.moduleName = moduleName;
        this.orgName = orgName;
        this.majorVersion = majorVersion;
    }

    public ArrayList<Connection> getConnections() {
        return connections;
    }

    public void setConnection(Connection connection) {
        this.connections.add(connection);
    }

    public static Connector getConnector(PackageDescriptor descriptor) {
        String orgName = descriptor.org().value();
        String moduleName = descriptor.name().value();
        String majorVersion = String.valueOf(descriptor.version().value().major());
        if (connector == null) {
            connector = new Connector(moduleName, orgName, majorVersion);
        }
        return connector;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIconPath() {
        return iconPath;
    }

    public void setIconPath(String iconPath) {
        this.iconPath = iconPath;
    }


    public String getZipFileName() {
        //TODO: also include org in the zip file name
        return "ballerina" + "-" + TYPE_NAME + "-" +  getModuleName() + "-" + getVersion() + ".zip";
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public void generateInstanceXml(File folder) {
        Utils.generateXmlForConnector("balConnector", TYPE_NAME, folder + File.separator + TYPE_NAME, this);
    }

    public void generateFunctionsXml(File connectorFolder, String templatePath, String typeName) {
        File file = new File(connectorFolder, typeName);
        if (!file.exists()) {
            file.mkdir();
        }
        Utils.generateXmlForConnector(templatePath, "component", file + File.separator + "component", this);
    }

    public void generateConfigInstanceXml(File connectorFolder, String templatePath, String typeName) {
        File file = new File(connectorFolder, typeName);
        if (!file.exists()) {
            file.mkdir();
        }
        Utils.generateXmlForConnector(templatePath, "component", file + File.separator + "component", this);
    }

    public void generateConfigTemplateXml(File connectorFolder, String templatePath, String typeName) {
        File file = new File(connectorFolder, typeName);
        if (!file.exists()) {
            file.mkdir();
        }
        Utils.generateXmlForConnector(templatePath, typeName + "_template", file + File.separator + Constants.INIT_FUNCTION_NAME, this);
    }
}
