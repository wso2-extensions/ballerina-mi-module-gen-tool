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

import io.ballerina.runtime.api.Runtime;
import io.ballerina.runtime.api.creators.TypeCreator;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.RecordType;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.*;
import org.apache.synapse.MessageContext;
import org.apache.synapse.data.connector.ConnectorResponse;
import org.apache.synapse.data.connector.DefaultConnectorResponse;
import org.apache.synapse.mediators.template.TemplateContext;
import org.ballerinalang.langlib.value.FromJsonStringWithType;
import org.wso2.integration.connector.core.AbstractConnector;
import org.wso2.integration.connector.core.ConnectException;
import org.wso2.integration.connector.core.connection.ConnectionHandler;

import java.util.Objects;
import java.util.Stack;

import static io.ballerina.stdlib.mi.Constants.*;

public class BalConnectorFunction extends AbstractConnector {
    @Override
    public void connect(MessageContext messageContext) throws ConnectException {
        //TODO: get the runtime, module and client with connection name
//        BObject clientObj = null;
//        try {
//            clientObj = BalConnectorConfig.getClient(messageContext.getProperty("connectionName").toString());
//        } catch (BError clientError) {
//            handleException(clientError.getMessage(), messageContext);
//        }

        String connectorName = BalConnectorConfig.getModule().getName();
        ConnectionHandler handler = ConnectionHandler.getConnectionHandler();
        BalConnectorConnection balConnection = (BalConnectorConnection) handler.getConnection(connectorName, messageContext.getProperty("connectionName").toString());
        BObject clientObj = balConnection.getBalConnectorObj();

        Runtime rt = BalConnectorConfig.getRuntime();
        System.out.println("running function");
        String balFunctionReturnType = messageContext.getProperty(RETURN_TYPE).toString();
        Object[] args = new Object[Integer.parseInt(messageContext.getProperty(Constants.SIZE).toString())];
        setParameters(args, messageContext);
        if (null != clientObj) {
            try {
                Object result = rt.callMethod(clientObj, messageContext.getProperty(METHOD_NAME).toString(), null, args);
                //TODO: Handle other result types
                if (Objects.equals(balFunctionReturnType, XML)) {
                    result = BXmlConverter.toOMElement((BXml) result);
                } else if (Objects.equals(balFunctionReturnType, DECIMAL)) {
                    result = ((BDecimal) result).value().toString();
                } else if (Objects.equals(balFunctionReturnType, STRING)) {
                    result = ((BString) result).getValue();
                } else if (Objects.equals(balFunctionReturnType, ARRAY)) {
                    // Convert BArray to JSON string format for MI consumption
                    result = TypeConverter.arrayToJsonString((BArray) result);
                } else if (result instanceof BArray) {
                    // Handle array return even if type not explicitly set to ARRAY
                    result = TypeConverter.arrayToJsonString((BArray) result);
                } else if (result instanceof BMap || result instanceof BObject) {
                    //TODO: handling Ballerina class objects - eg: covid19 method getGovernmentReportedDataByCountry
                    result = result.toString();
                }
                //TODO: handle conditionally overriding the payload - currently payload is not set
                ConnectorResponse connectorResponse = new DefaultConnectorResponse();
                connectorResponse.setPayload(result);
                messageContext.setVariable(getResultProperty(messageContext), connectorResponse);
            } catch (BError bError) {
                handleException(bError.getMessage(), messageContext);
            }
        }
        //TODO: Handle null client object


    }

    private void setParameters(Object[] args, MessageContext context) {
        for (int i = 0; i < args.length; i++) {
            Object param = getParameter(context, "param" + i, "paramType" + i, i);
            args[i] = param;
        }
    }

    private Object getParameter(MessageContext context, String value, String type, int index) {
        String paramName = context.getProperty(value).toString();
        String paramType = context.getProperty(type).toString();
        Object param = lookupTemplateParameter(context, paramName);
        if (param == null) {
            return null;
        }
        try {
            String stringParam = ((String) param).replaceAll("^\"(.*)\"$", "$1");
            return switch (paramType) {
                //TODO: Revisit handling union and record types
                case BOOLEAN -> Boolean.parseBoolean(stringParam);
                case INT -> Long.parseLong(stringParam);
                case STRING, UNION -> StringUtils.fromString(stringParam);
                case FLOAT -> Double.parseDouble(stringParam);
                case DECIMAL -> ValueCreator.createDecimalValue(stringParam);
                case RECORD -> createRecordValue(stringParam, context, index);
                case ARRAY -> createArrayValue(stringParam, context, index);
                default -> null;
            };
        } catch (Exception e) {
            handleException(e.getMessage(), context);
            return null;
        }
    }

    private Object createRecordValue(String jsonString, MessageContext context, int paramIndex) {
        BString jsonBString = StringUtils.fromString(jsonString);
        String recordName = context.getProperty("recordName" + paramIndex).toString();
        BMap<BString, Object> recValue = ValueCreator.createRecordValue(BalConnectorConfig.getModule(), recordName);
        Type recType = recValue.getType();
        return FromJsonStringWithType.fromJsonStringWithType(jsonBString, ValueCreator.createTypedescValue(recType));
    }

    /**
     * Create a Ballerina array from JSON array string.
     * Delegates to TypeConverter for the actual conversion logic.
     *
     * @param jsonArrayString JSON array string (e.g., "[\"a\", \"b\", \"c\"]")
     * @param context Message context
     * @param paramIndex Parameter index
     * @return Ballerina array (BArray)
     */
    private Object createArrayValue(String jsonArrayString, MessageContext context, int paramIndex) {
        // Get the array element type from context
        String elementType = context.getProperty("arrayElementType" + paramIndex).toString();

        // Use shared TypeConverter for conversion
        return TypeConverter.convertToArray(jsonArrayString, elementType);
    }

    public static Object lookupTemplateParameter(MessageContext ctx, String paramName) {
        Stack funcStack = (Stack) ctx.getProperty(Constants.SYNAPSE_FUNCTION_STACK);
        TemplateContext currentFuncHolder = (TemplateContext) funcStack.peek();
        return currentFuncHolder.getParameterValue(paramName);
    }

    private static String getResultProperty(MessageContext context) {
        return lookupTemplateParameter(context, Constants.RESPONSE_VARIABLE).toString();
    }


    // TODO: Use this to convert JSON string from MI to record in Ballerina
    private static void convertJsonToRecord() {
        String x = "";
        RecordType y = TypeCreator.createRecordType(null, null, 0, true, 0);

        FromJsonStringWithType.fromJsonStringWithType(StringUtils.fromString(x), ValueCreator.createTypedescValue(y));
    }
}
