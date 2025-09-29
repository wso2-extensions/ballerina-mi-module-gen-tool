package io.ballerina.stdlib.mi.plugin;

import java.io.File;

public class Constants {
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
    public static final String RECORD_FIELD = "field";
    public static final String INTERSECTION = "intersection";
    public static final String ENUM = "enum";
    public static final String FUNCTION_TEMPLATE_PATH = "balConnector" + File.separator + "functions";
    public static final String JSON_TEMPLATE_PATH = "balConnector" + File.separator + "inputTemplates";
    public static final String ATTRIBUTE_GROUP_TEMPLATE_PATH = JSON_TEMPLATE_PATH + File.separator + "attributeGroup.txt";
    public static final String ATTRIBUTE_TEMPLATE_PATH = JSON_TEMPLATE_PATH + File.separator + "attribute.txt";
    public static final String COMBO_ATTRIBUTE_TEMPLATE_PATH = JSON_TEMPLATE_PATH + File.separator + "comboAttribute.txt";
    public static final String INPUT_TYPE_STRING = "string";
    public static final String INPUT_TYPE_BOOLEAN = "boolean";
    public static final String INPUT_TYPE_COMBO = "combo";
    public static final String INIT_FUNCTION_NAME = "init";

}
