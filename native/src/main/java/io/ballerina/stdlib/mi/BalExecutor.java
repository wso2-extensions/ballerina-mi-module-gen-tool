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

package io.ballerina.stdlib.mi;

import com.google.gson.JsonParser;
import io.ballerina.runtime.api.Module;
import io.ballerina.runtime.api.Runtime;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.utils.JsonUtils;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BDecimal;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.runtime.api.values.BXml;
import io.ballerina.runtime.internal.values.ErrorValue;
import io.ballerina.runtime.internal.values.MapValueImpl;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.data.connector.ConnectorResponse;
import org.apache.synapse.data.connector.DefaultConnectorResponse;
import org.apache.synapse.mediators.elementary.EnrichMediator;
import org.apache.synapse.mediators.elementary.Source;
import org.apache.synapse.mediators.elementary.Target;
import org.apache.synapse.mediators.template.TemplateContext;
import org.apache.synapse.util.MediatorEnrichUtil;
import org.ballerinalang.langlib.value.FromJsonStringWithType;

import java.util.Stack;

import static io.ballerina.stdlib.mi.Constants.*;
import static io.ballerina.stdlib.mi.Constants.ARRAY;

public class BalExecutor {

    private static final String TEMP_RESPONSE_PROPERTY_NAME = "TEMP_BAL_RESPONSE_PROPERTY_";
    protected Log log = LogFactory.getLog(BalExecutor.class);

    public boolean execute(Runtime rt, Object callable, MessageContext context) throws AxisFault, BallerinaExecutionException {
        Object[] args = new Object[Integer.parseInt(context.getProperty(Constants.SIZE).toString())];
        setParameters(args, context);
        try {
            Object result;
            if (callable instanceof Module) {
                result = rt.callFunction((Module) callable, context.getProperty(Constants.FUNCTION_NAME).toString(), null, args);
            } else if (callable instanceof BObject) {
                result = rt.callMethod((BObject) callable, context.getProperty(FUNCTION_NAME).toString(), null, args);
            } else {
                throw new SynapseException("Unsupported callable type: " + callable.getClass().getName());
            }
            if (result instanceof ErrorValue bError) {
                throw new BallerinaExecutionException(bError.getMessage(), bError.fillInStackTrace());
            }
            Object processedResult = processResponse(result);
            ConnectorResponse connectorResponse = new DefaultConnectorResponse();
            if (isOverwriteBody(context)) {
                overwriteBody(context, processedResult);
            } else {
                connectorResponse.setPayload(processedResult);
            }
            context.setVariable(getResultProperty(context), connectorResponse);
        } catch (BError bError) {
            throw new BallerinaExecutionException(bError.getMessage(), bError.fillInStackTrace());
        }
        return true;
    }

    private Object processResponse(Object result) {
        if (result instanceof BXml) {
            return BXmlConverter.toOMElement((BXml) result);
        } else if (result instanceof BDecimal) {
            return ((BDecimal) result).value().toString();
        } else if (result instanceof BString) {
            return ((BString) result).getValue();
        } else if (result instanceof BArray) {
            // Convert BArray to JSON format for MI consumption
            return JsonParser.parseString(TypeConverter.arrayToJsonString((BArray) result));
        } else if (result instanceof BMap) {
            return JsonParser.parseString(((MapValueImpl<?, ?>) result).getJSONString());
        }
        return result;
    }

    protected void overwriteBody(MessageContext messageContext, Object payload) throws AxisFault {
        if (payload == null) {
            return;
        }
        messageContext.setProperty(TEMP_RESPONSE_PROPERTY_NAME + getResultProperty(messageContext), payload);
        Source source = MediatorEnrichUtil.createSourceWithProperty(TEMP_RESPONSE_PROPERTY_NAME + getResultProperty(messageContext));
        Target target = MediatorEnrichUtil.createTargetWithBody();
        doEnrich(messageContext, source, target);
        messageContext.setProperty(TEMP_RESPONSE_PROPERTY_NAME + getResultProperty(messageContext), null);
    }

    private void doEnrich(MessageContext synCtx, Source source, Target target) {
        if (!Boolean.TRUE.equals(synCtx.getProperty("message.builder.invoked"))) {
            MediatorEnrichUtil.buildMessage(synCtx);
        }

        EnrichMediator enrichMediator = new EnrichMediator();
        enrichMediator.setSource(source);
        enrichMediator.setTarget(target);
        enrichMediator.setNativeJsonSupportEnabled(true);
        enrichMediator.mediate(synCtx);
    }

    private void setParameters(Object[] args, MessageContext context) {
        for (int i = 0; i < args.length; i++) {
            Object param = getParameter(context, "param" + i, "paramType" + i, i);
            args[i] = param;
        }
    }

    public Object getParameter(MessageContext context, String value, String type, int index) {
        String paramName = context.getProperty(value).toString();
        Object param = lookupTemplateParameter(context, paramName);
        String paramType;
        if (value.matches("param\\d+Union.*")) {
            paramType = type;
        } else {
            paramType = context.getProperty(type).toString();
        }
        if (param == null && !UNION.equals(paramType)) {
            log.error("Error in getting the ballerina function parameter: " + paramName);
            throw new SynapseException("Parameter '" + paramName + "' is missing");
        }
        return switch (paramType) {
            case BOOLEAN -> Boolean.parseBoolean((String) param);
            case INT -> Long.parseLong((String) param);
            case STRING -> StringUtils.fromString((String) param);
            case FLOAT -> Double.parseDouble((String) param);
            case DECIMAL -> ValueCreator.createDecimalValue((String) param);
            case JSON -> getJsonParameter(param);
            case XML -> getBXmlParameter(context, value);
            case RECORD -> createRecordValue((String) param, context, index);
            case ARRAY -> getArrayParameter((String) param, context, value);
            case MAP -> getMapParameter(param);
            case UNION -> getUnionParameter(paramName, context, index);
            default -> null;
        };
    }

    private Object getUnionParameter(String paramName, MessageContext context, int index) {
        Object paramType = lookupTemplateParameter(context, paramName + "DataType");
        if (paramType instanceof String typeStr) {
            String unionParamName = "param" + index + "Union" + org.apache.commons.lang3.StringUtils.capitalize(typeStr);
            return getParameter(context, unionParamName, typeStr, -1);
        }
        return null;
    }

    private Object createRecordValue(String jsonString, MessageContext context, int paramIndex) {
        if (jsonString.startsWith("'") && jsonString.endsWith("'")) {
            jsonString = jsonString.substring(1, jsonString.length() - 1);
        }
        BString jsonBString = StringUtils.fromString(jsonString);
        String recordName = context.getProperty("param" + paramIndex + "_recordName").toString();
        BMap<BString, Object> recValue = ValueCreator.createRecordValue(BalConnectorConfig.getModule(), recordName);
        Type recType = recValue.getType();
        return FromJsonStringWithType.fromJsonStringWithType(jsonBString, ValueCreator.createTypedescValue(recType));
    }

    private BXml getBXmlParameter(MessageContext context, String parameterName) {
        OMElement omElement = getOMElement(context, parameterName);
        if (omElement == null) {
            return null;
        }
        return OMElementConverter.toBXml(omElement);
    }

    private OMElement getOMElement(MessageContext ctx, String value) {
        String param = ctx.getProperty(value).toString();
        Object paramValue = lookupTemplateParameter(ctx, param);
        if (paramValue != null) {
            if (paramValue instanceof OMElement) {
                return (OMElement) paramValue;
            } else {
                try {
                    return AXIOMUtil.stringToOM((String) lookupTemplateParameter(ctx, param));
                } catch (Exception ignored) {
                }
            }
        }
        log.error("Error in getting the OMElement");
        return null;
    }

    public static Object lookupTemplateParameter(MessageContext ctx, String paramName) {
        Stack funcStack = (Stack) ctx.getProperty(Constants.SYNAPSE_FUNCTION_STACK);
        TemplateContext currentFuncHolder = (TemplateContext) funcStack.peek();
        return currentFuncHolder.getParameterValue(paramName);
    }

    private Object getJsonParameter(Object param) {
        if (param instanceof String strParam) {
            if (strParam.startsWith("'") && strParam.endsWith("'")) {
                strParam = strParam.substring(1, strParam.length() - 1);
            }
            return JsonUtils.parse(strParam);
        } else {
            return JsonUtils.parse(param.toString());
        }
    }

    private BMap getMapParameter(Object param) {
        Object parsed;
        if (param instanceof String strParam) {
            if (strParam.startsWith("'") && strParam.endsWith("'")) {
                strParam = strParam.substring(1, strParam.length() - 1);
            }
            parsed = JsonUtils.parse(strParam);
        } else {
            parsed = JsonUtils.parse(param.toString());
        }

        // Validate that the parsed result is a BMap, not a BArray
        if (parsed instanceof BMap) {
            return (BMap) parsed;
        } else {
            throw new SynapseException("Map parameter must be a JSON object, not an array");
        }
    }

    /**
     * Get array parameter from context and convert to Ballerina array.
     * Delegates to TypeConverter for the actual conversion logic.
     *
     * @param jsonArrayString JSON array string
     * @param context         Message context
     * @param valueKey        The property key like "param0" used to extract the parameter
     */
    private Object getArrayParameter(String jsonArrayString, MessageContext context, String valueKey) {
        // Extract parameter index from valueKey (e.g., "param0" -> 0)
        int paramIndex = -1;
        String indexStr = valueKey.replaceAll("\\D+", "");
        if (!indexStr.isEmpty()) {
            try {
                paramIndex = Integer.parseInt(indexStr);
            } catch (NumberFormatException e) {
                log.error("Invalid parameter index in valueKey: " + valueKey, e);
                // Optionally, handle error (e.g., throw, return null, etc.)
                return null;
            }
        } else {
            log.error("No digits found in valueKey: " + valueKey);
            return null;
        }

        // Get the array element type from context
        String elementType = context.getProperty("arrayElementType" + paramIndex).toString();

        // Use shared TypeConverter for conversion
        return TypeConverter.convertToArray(jsonArrayString, elementType);
    }

    private static String getResultProperty(MessageContext context) {
        return lookupTemplateParameter(context, Constants.RESPONSE_VARIABLE).toString();
    }

    private static boolean isOverwriteBody(MessageContext context) {
        return Boolean.parseBoolean((String) lookupTemplateParameter(context, Constants.OVERWRITE_BODY));
    }
}
