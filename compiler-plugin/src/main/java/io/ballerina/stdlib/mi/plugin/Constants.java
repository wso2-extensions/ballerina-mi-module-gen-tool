package io.ballerina.stdlib.mi.plugin;

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
    public static final String INTEGER_REGEX = "^(-?\\d+|\\$\\{.+\\})$";
    public static final String DECIMAL_REGEX = "^(-?\\d+(\\.\\d+)?|\\$\\{.+\\})$";
    public static final String ATTRIBUTE_SEPARATOR = ",";
    public static final String ATTRIBUTE_GROUP_END = "\n]\n}\n}\n";

}
