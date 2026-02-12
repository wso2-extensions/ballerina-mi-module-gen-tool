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

/**
 * Abstract base class for all connector model elements that can be
 * rendered through Handlebars templates.
 * <p>
 * Subclasses include {@link Connector} (the top-level connector model)
 * and {@link Component} (individual functions/resources within a connector).
 * </p>
 * <p>
 * This class provides common identity methods (type, name) used by
 * the template engine and artifact generators to produce XML and JSON
 * output files.
 * </p>
 *
 * @since 0.5.0
 */
public abstract class ModelElement {

    /**
     * Returns the element type identifier used in template rendering.
     * Subclasses should override this to provide a meaningful type string.
     *
     * @return The element type, or null if not applicable
     */
    public String getType() {
        return null;
    }

    /**
     * Returns the element name used in file naming and template rendering.
     * Subclasses should override this to provide a meaningful name.
     *
     * @return The element name, or null if not applicable
     */
    public String getName() {
        return null;
    }
}
