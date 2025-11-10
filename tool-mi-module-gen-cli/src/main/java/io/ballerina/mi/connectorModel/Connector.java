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

import io.ballerina.mi.util.Constants;
import io.ballerina.mi.util.Utils;
import io.ballerina.projects.PackageDescriptor;

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

    // Commented out: Uses Utils.generateXmlForConnector which is commented out
//    public void generateInstanceXml(File folder) {
//        Utils.generateXmlForConnector("balConnector", TYPE_NAME, folder + File.separator + TYPE_NAME, this);
//    }

    // Commented out: Uses Utils.generateXmlForConnector which is commented out
//    public void generateFunctionsXml(File connectorFolder, String templatePath, String typeName) {
//        File file = new File(connectorFolder, typeName);
//        if (!file.exists()) {
//            file.mkdir();
//        }
//        Utils.generateXmlForConnector(templatePath, "component", file + File.separator + "component", this);
//    }

    // Commented out: Uses Utils.generateXmlForConnector which is commented out
//    public void generateConfigInstanceXml(File connectorFolder, String templatePath, String typeName) {
//        File file = new File(connectorFolder, typeName);
//        if (!file.exists()) {
//            file.mkdir();
//        }
//        Utils.generateXmlForConnector(templatePath, "component", file + File.separator + "component", this);
//    }

    // Commented out: Uses Utils.generateXmlForConnector which is commented out
//    public void generateConfigTemplateXml(File connectorFolder, String templatePath, String typeName) {
//        File file = new File(connectorFolder, typeName);
//        if (!file.exists()) {
//            file.mkdir();
//        }
//        Utils.generateXmlForConnector(templatePath, typeName + "_template", file + File.separator + Constants.INIT_FUNCTION_NAME, this);
//    }
}
