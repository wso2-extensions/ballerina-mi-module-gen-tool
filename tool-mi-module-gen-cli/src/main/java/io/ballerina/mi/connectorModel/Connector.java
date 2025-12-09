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
import io.ballerina.mi.util.Constants;
import io.ballerina.mi.util.Utils;
import io.ballerina.projects.PackageDescriptor;
import io.ballerina.projects.SemanticVersion;

import java.io.File;
import java.util.ArrayList;

public class Connector extends ModelElement {
    public static final String TYPE_NAME = "connector";
    public static final String TEMP_PATH = "connector";
    public static final String ICON_FOLDER = "icon";
    public static final String SMALL_ICON_NAME = "icon-small.png";
    public static final String LARGE_ICON_NAME = "icon-large.png";
    public static final String LIB_PATH = "lib";
    private static Connector connector = null;
    private final ArrayList<Connection> connections = new ArrayList<>();
    private final ArrayList<Component> components = new ArrayList<>();
    private String description = "helps to connect with external systems";
    private String iconPath;
    private String version;
    private final String orgName;
    private final String moduleName;
    private final String majorVersion;
    private boolean isBalModule;

    public String getOrgName() {
        return orgName;
    }

    public String getModuleName() {
        return moduleName;
    }

    public String getMajorVersion() {
        return majorVersion;
    }

    private Connector(String moduleName, String orgName, String version, String majorVersion) {
        this.moduleName = moduleName;
        this.orgName = orgName;
        this.version = version;
        this.majorVersion = majorVersion;
    }

    public ArrayList<Connection> getConnections() {
        return connections;
    }

    public void setConnection(Connection connection) {

        this.connections.add(connection);
        components.addAll(connection.getComponents());
    }

    public static Connector getConnector(PackageDescriptor descriptor) {
        String orgName = descriptor.org().value();
        String moduleName = descriptor.name().value();
        SemanticVersion version = descriptor.version().value();
        String majorVersion = String.valueOf(version.major());
        if (connector == null) {
            connector = new Connector(moduleName, orgName, version.toString(), majorVersion);
        }
        return connector;
    }

    public static Connector getConnector() {
        if (connector == null) {
            throw new IllegalStateException("Connector has not been initialized");
        }
        return connector;
    }

    public static void reset() {
        connector = null;
    }

    public boolean isBalModule() {
        return isBalModule;
    }

    public void setBalModule(boolean balModule) {
        isBalModule = balModule;
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
        if (isBalModule) {
            return getModuleName() + "-" + TYPE_NAME + "-" + getVersion() + ".zip";
        }
        return "ballerina" + "-" + TYPE_NAME + "-" + getModuleName() + "-" + getVersion() + ".zip";
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public ArrayList<Component> getComponents() {
        return components;
    }

    public void generateInstanceXml(File folder) {
        ConnectorSerializer.generateXmlForConnector("balConnector", TYPE_NAME, folder + File.separator + TYPE_NAME, this);
    }

    public void generateFunctionsXml(File connectorFolder, String templatePath, String typeName) {
        File file = new File(connectorFolder, typeName);
        if (!file.exists()) {
            file.mkdir();
        }
        ConnectorSerializer.generateXmlForConnector(templatePath, "component", file + File.separator + "component", this);
    }

    public void generateConfigInstanceXml(File connectorFolder, String templatePath, String typeName) {
        File file = new File(connectorFolder, typeName);
        if (!file.exists()) {
            file.mkdir();
        }
        ConnectorSerializer.generateXmlForConnector(templatePath, "component", file + File.separator + "component", this);
    }

    public void generateConfigTemplateXml(File connectorFolder, String templatePath, String typeName) {
        File file = new File(connectorFolder, typeName);
        if (!file.exists()) {
            file.mkdir();
        }
        ConnectorSerializer.generateXmlForConnector(templatePath, typeName + "_template", file + File.separator + Constants.INIT_FUNCTION_NAME, this);
    }
}
