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
import io.ballerina.mi.model.param.FunctionParam;
import io.ballerina.mi.model.param.Param;
import io.ballerina.mi.model.param.ResourcePathSegment;
import io.ballerina.mi.util.Utils;
import org.ballerinalang.diagramutil.connector.models.connector.Type;
import org.ballerinalang.diagramutil.connector.models.connector.types.PathParamType;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Component extends ModelElement {
    private String name;
    private final String documentation;
    // Object type used for object-attached function dispatch when applicable.
    private String objectTypeName;
    private final FunctionType functionType;
    // Stable positional identifier used in generated property naming and ordering.
    private final String index;
    private Connection parent;
    private final ArrayList<Param> params = new ArrayList<>();
    private final List<FunctionParam> functionParams = new ArrayList<>();
    private final List<PathParamType> pathParams;
    private final List<Type> queryParams;
    private final String returnType;
    // For resource functions: the HTTP accessor (get, post, put, delete, etc.)
    private String resourceAccessor;
    // For resource functions: all path segments (both static and dynamic)
    private List<ResourcePathSegment> resourcePathSegments;
    // Flag to indicate if the component name was derived from OpenAPI operationId
    private boolean hasOperationId = false;

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

    /**
     * Get the resource accessor (HTTP method) for resource functions.
     * @return The accessor (e.g., "get", "post", "put", "delete") or null if not a resource function
     */
    public String getResourceAccessor() {
        return resourceAccessor;
    }

    /**
     * Set the resource accessor (HTTP method) for resource functions.
     * @param resourceAccessor The accessor (e.g., "get", "post", "put", "delete")
     */
    public void setResourceAccessor(String resourceAccessor) {
        this.resourceAccessor = resourceAccessor;
    }

    /**
     * Check if this component represents a resource function.
     * Used by Handlebars templates to conditionally include resource-specific properties.
     * @return true if this is a resource function, false otherwise
     */
    public boolean isResourceFunction() {
        return functionType == FunctionType.RESOURCE;
    }

    /**
     * Get the number of path parameters for resource functions.
     * @return The count of path parameters
     */
    public int getPathParamSize() {
        return pathParams != null ? pathParams.size() : 0;
    }

    /**
     * Get the resource path segments for resource functions.
     * @return List of path segments (both static and dynamic)
     */
    public List<ResourcePathSegment> getResourcePathSegments() {
        return resourcePathSegments;
    }

    /**
     * Set the resource path segments for resource functions.
     * @param resourcePathSegments List of path segments
     */
    public void setResourcePathSegments(List<ResourcePathSegment> resourcePathSegments) {
        this.resourcePathSegments = resourcePathSegments;
    }

    /**
     * Check if the component name was derived from OpenAPI operationId.
     * @return true if name came from operationId, false otherwise
     */
    public boolean hasOperationId() {
        return hasOperationId;
    }

    /**
     * Set whether the component name was derived from OpenAPI operationId.
     * @param hasOperationId true if name came from operationId
     */
    public void setHasOperationId(boolean hasOperationId) {
        this.hasOperationId = hasOperationId;
    }

    /**
     * Generates the JVM method name for invoking this resource function.
     * The format follows Ballerina's internal encoding:
     * $<accessor>$<segment1>[$[$<param1>]]$<segment2>...
     *
     * Examples:
     * - resource function get items() -> $get$items
     * - resource function get items/[string itemId]() -> $get$items$$itemId
     * - resource function get users/[string userId]/drafts() -> $get$users$$userId$drafts
     *
     * @return The JVM method name for runtime invocation
     */
    public String getJvmMethodName() {
        if (!isResourceFunction() || resourceAccessor == null) {
            return null;
        }

        StringBuilder methodName = new StringBuilder();
        methodName.append("$").append(resourceAccessor);

        if (resourcePathSegments != null) {
            for (ResourcePathSegment segment : resourcePathSegments) {
                methodName.append(segment.toJvmMethodNameComponent());
            }
        }

        return methodName.toString();
    }

    /**
     * Get the number of resource path segments.
     * Used for XML template generation.
     * @return The count of path segments
     */
    public int getResourcePathSegmentSize() {
        return resourcePathSegments != null ? resourcePathSegments.size() : 0;
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

    /**
     * Human-friendly display name for UI schemas.
     * For resource functions:
     *   1. If name came from operationId -> humanize the operationId
     *   2. If no operationId but documentation exists -> use documentation (first line/sentence)
     *   3. Otherwise -> humanize the generated name (from path segments)
     * For remote/other functions:
     *   - Always humanize the function name (function names are already meaningful)
     */
    public String getDisplayName() {
        // For non-resource functions, always humanize the name
        // Remote function names are already meaningful (e.g., getAllContinentsStatus)
        if (functionType != FunctionType.RESOURCE) {
            return Utils.humanizeName(this.name);
        }

        // For resource functions with operationId, humanize the operationId
        if (hasOperationId) {
            return Utils.humanizeName(this.name);
        }

        // For resource functions without operationId, use documentation if available
        // This avoids confusing titles like "get api 3 serverinfo" from path segments
        if (documentation != null && !documentation.trim().isEmpty()) {
            String doc = documentation.trim();
            // Use first line or first sentence (whichever is shorter)
            int newlineIndex = doc.indexOf('\n');
            int periodIndex = doc.indexOf('.');

            if (newlineIndex > 0 && (periodIndex < 0 || newlineIndex < periodIndex)) {
                return doc.substring(0, newlineIndex).trim();
            } else if (periodIndex > 0) {
                return doc.substring(0, periodIndex + 1).trim();
            }
            // If no newline or period, use the whole doc (up to reasonable length)
            return doc.length() > 100 ? doc.substring(0, 100).trim() + "..." : doc;
        }

        // Fallback: humanize the generated name
        return Utils.humanizeName(this.name);
    }
}
