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

import io.ballerina.mi.generator.ConnectorSerializer;
import io.ballerina.mi.model.param.*;
import io.ballerina.mi.util.Constants;
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
    private boolean generationAborted = false;
    private String abortionReason;

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

    public boolean isGenerationAborted() {
        return generationAborted;
    }

    public void setGenerationAborted(boolean generationAborted, String reason) {
        this.generationAborted = generationAborted;
        this.abortionReason = reason;
    }

    public String getAbortionReason() {
        return abortionReason;
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

    /**
     * Clears the heavy component and connection data from the Connector model.
     * After all XML/JSON files have been generated, the component tree (with its
     * deeply nested FunctionParam graphs) is no longer needed. Clearing it frees
     * significant memory for large connectors before the ZIP packaging step.
     * Metadata fields (orgName, moduleName, version, etc.) are preserved.
     */
    public void clearComponentData() {
        for (Connection connection : connections) {
            connection.getComponents().clear();
        }
        connections.clear();
        components.clear();
    }

    /**
     * Pre-computes TypeSymbol-derived values and clears TypeSymbol references
     * to allow the Ballerina compiler's semantic model to be garbage collected.
     * Must be called after analysis is complete but before serialization starts.
     */
    public void clearTypeSymbols() {
        for (Connection connection : connections) {
            if (connection.getInitComponent() != null) {
                clearTypeSymbolsFromComponent(connection.getInitComponent());
            }
            for (Component component : connection.getComponents()) {
                clearTypeSymbolsFromComponent(component);
            }
        }
    }

    private static void clearTypeSymbolsFromComponent(Component component) {
        if (component.getFunctionParams() != null) {
            for (FunctionParam param : component.getFunctionParams()) {
                clearTypeSymbolsFromParam(param);
            }
        }
    }

    /**
     * Clears TypeSymbol references from a FunctionParam tree using iterative DFS
     * (ArrayDeque as an explicit stack) instead of recursive calls.
     * <p>
     * This avoids StackOverflowError on deeply nested record types (depth > 20)
     * and uses the FunctionParamVisitor for type-safe dispatch without instanceof.
     * </p>
     */
    private static void clearTypeSymbolsFromParam(FunctionParam root) {
        java.util.Deque<FunctionParam> stack = new java.util.ArrayDeque<>();
        stack.push(root);

        FunctionParamVisitor collector = new FunctionParamVisitor() {
            @Override
            public void visit(FunctionParam param) {
                // Base FunctionParam — no children to collect
            }

            @Override
            public void visit(RecordFunctionParam param) {
                if (param.getRecordFieldParams() != null) {
                    for (FunctionParam field : param.getRecordFieldParams()) {
                        stack.push(field);
                    }
                }
            }

            @Override
            public void visit(UnionFunctionParam param) {
                if (param.getUnionMemberParams() != null) {
                    for (FunctionParam member : param.getUnionMemberParams()) {
                        stack.push(member);
                    }
                }
            }

            @Override
            public void visit(ArrayFunctionParam param) {
                param.preComputeArrayElementType();
                if (param.getElementFieldParams() != null) {
                    for (FunctionParam field : param.getElementFieldParams()) {
                        stack.push(field);
                    }
                }
            }

            @Override
            public void visit(MapFunctionParam param) {
                if (param.getValueFieldParams() != null) {
                    for (FunctionParam field : param.getValueFieldParams()) {
                        stack.push(field);
                    }
                }
            }
        };

        while (!stack.isEmpty()) {
            FunctionParam current = stack.pop();
            // Visitor dispatches to the correct visit() overload based on runtime type
            current.accept(collector);
            current.clearTypeSymbol();
        }
    }
}
