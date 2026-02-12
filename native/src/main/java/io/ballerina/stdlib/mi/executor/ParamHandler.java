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

package io.ballerina.stdlib.mi.executor;

import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.utils.JsonUtils;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BDecimal;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.runtime.api.values.BXml;
import io.ballerina.stdlib.mi.Constants;
import io.ballerina.stdlib.mi.OMElementConverter;
import io.ballerina.stdlib.mi.utils.SynapseUtils;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;

import static io.ballerina.stdlib.mi.Constants.ARRAY;
import static io.ballerina.stdlib.mi.Constants.BOOLEAN;
import static io.ballerina.stdlib.mi.Constants.DECIMAL;
import static io.ballerina.stdlib.mi.Constants.FLOAT;
import static io.ballerina.stdlib.mi.Constants.INT;
import static io.ballerina.stdlib.mi.Constants.JSON;
import static io.ballerina.stdlib.mi.Constants.MAP;
import static io.ballerina.stdlib.mi.Constants.RECORD;
import static io.ballerina.stdlib.mi.Constants.STRING;
import static io.ballerina.stdlib.mi.Constants.UNION;
import static io.ballerina.stdlib.mi.Constants.XML;

public class ParamHandler {

    private static final Log log = LogFactory.getLog(ParamHandler.class);

    public void setParameters(Object[] args, MessageContext context) {
        for (int i = 0; i < args.length; i++) {
            Object param = getParameter(context, "param" + i, "paramType" + i, i);
            args[i] = param;
        }
    }

    public Object[] prependPathParams(Object[] args, MessageContext context) {
        String pathParamSizeStr = SynapseUtils.getPropertyAsString(context, Constants.PATH_PARAM_SIZE);
        int pathParamSize = 0;
        if (pathParamSizeStr != null) {
            try {
                pathParamSize = Integer.parseInt(pathParamSizeStr);
            } catch (NumberFormatException e) {
                log.warn("Invalid PATH_PARAM_SIZE value '" + pathParamSizeStr +
                        "'. Defaulting path parameter size to 0.", e);
            }
        }

        if (pathParamSize == 0) {
            return args;
        }

        Object[] pathParams = new Object[pathParamSize];
        for (int i = 0; i < pathParamSize; i++) {
            String pathParamName = SynapseUtils.getPropertyAsString(context, "pathParam" + i);
            String pathParamType = SynapseUtils.getPropertyAsString(context, "pathParamType" + i);
            Object pathParamValue = SynapseUtils.lookupTemplateParameter(context, pathParamName);

            if (pathParamValue != null) {
                pathParams[i] = convertPathParam(pathParamValue.toString(), pathParamType);
            } else {
                log.warn("Path parameter '" + pathParamName + "' not found in context");
                pathParams[i] = null;
            }
        }

        Object[] combined = new Object[pathParamSize + args.length];
        System.arraycopy(pathParams, 0, combined, 0, pathParamSize);
        System.arraycopy(args, 0, combined, pathParamSize, args.length);
        return combined;
    }

    public Object convertPathParam(String value, String type) {
        if (type == null) {
            type = Constants.STRING;
        }
        try {
            return switch (type) {
                case Constants.INT -> Long.parseLong(value);
                case Constants.FLOAT -> Double.parseDouble(value);
                case Constants.BOOLEAN -> Boolean.parseBoolean(value);
                case Constants.DECIMAL -> ValueCreator.createDecimalValue(value);
                default -> StringUtils.fromString(value);
            };
        } catch (NumberFormatException e) {
            throw new SynapseException(
                    "Invalid value '" + value + "' for path parameter type '" + type + "'", e);
        }
    }

    public Object getParameter(MessageContext context, String value, String type, int index) {
        String paramName = SynapseUtils.getPropertyAsString(context, value);
        if (paramName == null) {
            log.error("Parameter definition property '" + value + "' not found in context. Check if the generated XML artifacts are correct.");
            throw new SynapseException("Parameter definition property '" + value + "' is missing");
        }

        Object param = SynapseUtils.lookupTemplateParameter(context, paramName);

        String paramType;
        if (value.matches("param\\d+Union.*")) {
            paramType = type;
        } else {
            paramType = SynapseUtils.getPropertyAsString(context, type);
            if (paramType == null) {
                log.warn("Parameter type property '" + type + "' not found in context. Defaulting to STRING.");
                paramType = Constants.STRING;
            }
        }
        if (param == null) {
            if (UNION.equals(paramType)) {
                return getUnionParameter(paramName, context, index);
            } else if (RECORD.equals(paramType)) {
                return DataTransformer.createRecordValue(null, paramName, context, index);
            }
            return null;
        }

        try {
            Object result = switch (paramType) {
                case BOOLEAN -> Boolean.parseBoolean((String) param);
                case INT -> Long.parseLong((String) param);
                case STRING -> StringUtils.fromString((String) param);
                case FLOAT -> Double.parseDouble((String) param);
                case DECIMAL -> ValueCreator.createDecimalValue((String) param);
                case JSON -> DataTransformer.getJsonParameter(param);
                case XML -> getBXmlParameter(context, value);
                case RECORD -> DataTransformer.createRecordValue((String) param, paramName, context, index);
                case ARRAY -> getArrayParameter((String) param, context, value);
                case MAP -> DataTransformer.getMapParameter(param, context, value);
                case UNION -> getUnionParameter(paramName, context, index);
                default -> null;
            };
            return result;
        } catch (Exception e) {
            log.error("Error in getParameter for " + paramName + " (type: " + paramType + "): " + e.getMessage(), e);
            throw new SynapseException("Failed to process parameter " + paramName, e);
        }
    }

    private Object getUnionParameter(String paramName, MessageContext context, int index) {
        Object paramType = SynapseUtils.lookupTemplateParameter(context, paramName + "DataType");
        if (paramType instanceof String typeStr) {
            String unionParamName = "param" + index + "Union" + org.apache.commons.lang3.StringUtils.capitalize(typeStr);
            return getParameter(context, unionParamName, typeStr, -1);
        }
        return null;
    }

    private Object getArrayParameter(String jsonArrayString, MessageContext context, String valueKey) {
        int paramIndex = -1;
        String indexStr = valueKey.replaceAll("\\D+", "");
        if (!indexStr.isEmpty()) {
            try {
                paramIndex = Integer.parseInt(indexStr);
            } catch (NumberFormatException e) {
                log.error("Invalid parameter index in valueKey: " + valueKey, e);
                return null;
            }
        } else {
            log.error("No digits found in valueKey: " + valueKey);
            return null;
        }

        String elementType = context.getProperty("arrayElementType" + paramIndex).toString();

        String cleanedJson = SynapseUtils.cleanupJsonString(jsonArrayString);

        if ("record".equals(elementType)) {
            try {
                return JsonUtils.parse(cleanedJson);
            } catch (Exception e) {
                log.error("Failed to parse record array JSON: " + e.getMessage(), e);
                throw new SynapseException("Failed to parse record array: " + e.getMessage(), e);
            }
        }

        if ("float".equals(elementType)) {
            try {
                Object parsed = JsonUtils.parse(cleanedJson);
                if (parsed instanceof BArray array) {
                    return convertDecimalArrayToFloatArray(array);
                }
                return parsed;
            } catch (Exception e) {
                log.error("Failed to parse float array JSON: " + e.getMessage(), e);
                throw new SynapseException("Failed to parse float array: " + e.getMessage(), e);
            }
        }

        if ("array".equals(elementType)) {
            try {
                Object parsed = JsonUtils.parse(cleanedJson);
                if (parsed instanceof BArray outerArray) {
                    if (outerArray.size() > 0) {
                        Object firstElement = outerArray.get(0);
                        if (firstElement instanceof BMap firstRow) {
                            BString innerArrayKey = StringUtils.fromString("innerArray");
                            if (firstRow.containsKey(innerArrayKey)) {
                                String transformed = DataTransformer.transformNestedTableTo2DArray(outerArray);
                                return JsonUtils.parse(transformed);
                            }
                        } else if (firstElement instanceof BArray firstRow) {
                            if (firstRow.size() >= 2) {
                                String transformed = DataTransformer.transformMIStudioNestedTableTo2DArray(outerArray, context);
                                return JsonUtils.parse(transformed);
                            }
                        }
                    }
                }
                return parsed;
            } catch (Exception e) {
                log.error("Failed to parse 2D array JSON: " + e.getMessage(), e);
                throw new SynapseException("Failed to parse 2D array: " + e.getMessage(), e);
            }
        }

        if ("union".equals(elementType)) {
            try {
                Object parsed = JsonUtils.parse(cleanedJson);
                if (parsed instanceof BArray array) {
                    if (array.size() > 0 && array.get(0) instanceof BMap) {
                        String transformed = transformUnionTableToArray(array);
                        return JsonUtils.parse(transformed);
                    }
                }
                return parsed;
            } catch (Exception e) {
                log.error("Failed to parse union array JSON: " + e.getMessage(), e);
                throw new SynapseException("Failed to parse union array: " + e.getMessage(), e);
            }
        }
        
        try {
            Object parsed = JsonUtils.parse(cleanedJson);
            if (parsed instanceof BArray array) {
                if (array.size() > 0 && array.get(0) instanceof BMap) {
                     String transformed = DataTransformer.transformTableArrayToSimpleArray(array);
                     return JsonUtils.parse(transformed);
                }
            }
            return parsed; 
        } catch (Exception e) {
            log.error("Failed to parse array JSON: " + e.getMessage(), e);
            throw new SynapseException("Failed to parse array: " + e.getMessage(), e);
        }
    }
    
    private String transformUnionTableToArray(BArray array) {
        BString typeKey = StringUtils.fromString("type");
        BString valueFieldName = StringUtils.fromString("value");
        StringBuilder jsonBuilder = new StringBuilder("[");

        for (int i = 0; i < array.size(); i++) {
            if (i > 0) jsonBuilder.append(",");
            Object element = array.get(i);
            if (element instanceof BMap row) {
                Object typeObj = row.get(typeKey);
                Object valueObj = row.get(valueFieldName);
                String type = typeObj != null ? typeObj.toString() : "string";
                String value = valueObj != null ? valueObj.toString() : "";
                switch (type) {
                    case "int":
                        try {
                            Long.parseLong(value);
                            jsonBuilder.append(value);
                        } catch (NumberFormatException e) {
                            jsonBuilder.append("\"").append(escapeJsonString(value)).append("\"");
                        }
                        break;
                    case "float":
                    case "decimal":
                        try {
                            Double.parseDouble(value);
                            jsonBuilder.append(value);
                        } catch (NumberFormatException e) {
                            jsonBuilder.append("\"").append(escapeJsonString(value)).append("\"");
                        }
                        break;
                    case "boolean":
                        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
                            jsonBuilder.append(value.toLowerCase());
                        } else {
                            jsonBuilder.append("\"").append(escapeJsonString(value)).append("\"");
                        }
                        break;
                    default:
                        jsonBuilder.append("\"").append(escapeJsonString(value)).append("\"");
                        break;
                }
            }
        }
        jsonBuilder.append("]");
        return jsonBuilder.toString();
    }

    private BArray convertDecimalArrayToFloatArray(BArray array) {
        double[] doubleArray = new double[array.size()];
        for (int i = 0; i < array.size(); i++) {
            Object value = array.get(i);
            if (value instanceof BDecimal) {
                doubleArray[i] = ((BDecimal) value).decimalValue().doubleValue();
            } else if (value instanceof Number) {
                doubleArray[i] = ((Number) value).doubleValue();
            } else {
                try {
                    doubleArray[i] = Double.parseDouble(value.toString());
                } catch (NumberFormatException e) {
                     log.warn("Failed to convert value to double: " + value);
                     doubleArray[i] = 0.0;
                }
            }
        }
        return ValueCreator.createArrayValue(doubleArray);
    }
    
    // Quick helper for escaping JSON strings for union table transformation
    private String escapeJsonString(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    private BXml getBXmlParameter(MessageContext context, String parameterName) {
        OMElement omElement = getOMElement(context, parameterName);
        if (omElement == null) return null;
        return OMElementConverter.toBXml(omElement);
    }

    private OMElement getOMElement(MessageContext ctx, String value) {
        String param = ctx.getProperty(value).toString();
        Object paramValue = SynapseUtils.lookupTemplateParameter(ctx, param);
        if (paramValue != null) {
            if (paramValue instanceof OMElement) return (OMElement) paramValue;
            try {
                return AXIOMUtil.stringToOM((String) SynapseUtils.lookupTemplateParameter(ctx, param));
            } catch (Exception ignored) {}
        }
        log.error("Error in getting the OMElement");
        return null;
    }
}
