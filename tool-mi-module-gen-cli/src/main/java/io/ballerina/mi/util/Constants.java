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
 
package io.ballerina.mi.util;

import java.io.File;

public class Constants {
    public static final String INIT_FUNCTION_NAME = "init";
    public static final String BOOLEAN = "boolean";
    public static final String INT = "int";
    public static final String STRING = "string";
    public static final String FLOAT = "float";
    public static final String DECIMAL = "decimal";
    public static final String JSON = "json";
    public static final String XML = "xml";
    public static final String UNION = "union";
    public static final String RECORD = "record";
    public static final String ARRAY = "array";
    public static final String ENUM = "enum";
    public static final String MAP = "map";
    public static final String FUNCTION_TEMPLATE_PATH = "balConnector" + File.separator + "functions";
    public static final String JSON_TEMPLATE_PATH = "balConnector" + File.separator + "inputTemplates";
    public static final String ATTRIBUTE_GROUP_TEMPLATE_PATH = JSON_TEMPLATE_PATH + File.separator
            + "attributeGroup.json";
    public static final String ATTRIBUTE_TEMPLATE_PATH = JSON_TEMPLATE_PATH + File.separator + "attribute.json";
    public static final String COMBO_TEMPLATE_PATH = JSON_TEMPLATE_PATH + File.separator
            + "combo.json";
    public static final String TABLE_TEMPLATE_PATH = JSON_TEMPLATE_PATH + File.separator + "table.json";
    public static final String INPUT_TYPE_STRING_OR_EXPRESSION = "stringOrExpression";
    public static final String INPUT_TYPE_BOOLEAN = "boolean";
    public static final String INPUT_TYPE_COMBO = "combo";
    public static final String VALIDATE_TYPE_REGEX = "regex";
    public static final String INTEGER_REGEX = "^(-?\\\\d+|\\\\$\\\\{.+\\\\})$";
    public static final String DECIMAL_REGEX = "^(-?\\\\d+(\\\\.\\\\d+)?|\\\\$\\\\{.+\\\\})$";
    // Optional field regex patterns - allow empty string OR valid values
    public static final String INTEGER_REGEX_OPTIONAL = "^$|^(-?\\\\d+|\\\\$\\\\{.+\\\\})$";
    public static final String DECIMAL_REGEX_OPTIONAL = "^$|^(-?\\\\d+(\\\\.\\\\d+)?|\\\\$\\\\{.+\\\\})$";
    public static final String JSON_OBJECT_REGEX_OPTIONAL = "^$|^(\\\\{[\\\\s\\\\S]*\\\\}|\\\\$\\\\{.+\\\\})$";
    public static final String ATTRIBUTE_SEPARATOR = ",";
    public static final String ATTRIBUTE_GROUP_END = "\n                    ]\n                  }\n                }";
    public static final String CONNECTOR_TARGET_PATH = "CONNECTOR_TARGET_PATH";
}
