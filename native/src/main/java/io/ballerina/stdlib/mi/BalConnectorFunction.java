package io.ballerina.stdlib.mi;

import io.ballerina.runtime.api.Runtime;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import org.apache.synapse.MessageContext;
import org.apache.synapse.data.connector.ConnectorResponse;
import org.apache.synapse.data.connector.DefaultConnectorResponse;
import org.apache.synapse.mediators.template.TemplateContext;
import org.ballerinalang.langlib.value.FromJsonStringWithType;
import org.wso2.integration.connector.core.AbstractConnector;
import org.wso2.integration.connector.core.ConnectException;
import org.wso2.integration.connector.core.connection.ConnectionHandler;

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
        String balFunctionReturnType = messageContext.getProperty(Constants.RETURN_TYPE).toString();
        Object[] args = new Object[Integer.parseInt(messageContext.getProperty(Constants.SIZE).toString())];
        setParameters(args, messageContext);
        if (null != clientObj) {
            try {
                Object result = rt.callMethod(clientObj, messageContext.getProperty(Constants.METHOD_NAME).toString(), null, args);
                //TODO: Handle other result types
                if (result instanceof BMap) {
                    result = result.toString();
                }
                //TODO: handle conditionally overriding the payload
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
            //TODO: check handling null parameters
//            if (param == null) {
//                return false;
//            }
            args[i] = param;
        }
    }

    private Object getParameter(MessageContext context, String value, String type, int index) {
        String paramName = context.getProperty(value).toString();
        Object param = lookupTemplateParameter(context, paramName);
        //TODO: check handling null parameters
//        if (param == null) {
//            log.error("Error in getting the ballerina function parameter: " + paramName);
//            return null;
//        }
        String paramType = context.getProperty(type).toString();
        return switch (paramType) {
            //TODO: Revisit handling union and record types
            case BOOLEAN -> Boolean.parseBoolean((String) param);
            case INT -> Long.parseLong((String) param);
            case STRING, UNION -> StringUtils.fromString((String) param);
            case FLOAT -> Double.parseDouble((String) param);
            case DECIMAL -> ValueCreator.createDecimalValue((String) param);
            case RECORD -> createRecordValue((String) param, context, index);
            default -> null;
        };
    }

    private Object createRecordValue(String jsonString, MessageContext context, int paramIndex) {
        BString jsonBString = StringUtils.fromString(jsonString);
        String recordName = context.getProperty("recordName" + paramIndex).toString();
        BMap<BString, Object> recValue = ValueCreator.createRecordValue(BalConnectorConfig.getModule(), recordName);
        Type recType = recValue.getType();
        return FromJsonStringWithType.fromJsonStringWithType(jsonBString, ValueCreator.createTypedescValue(recType));
    }

    public static Object lookupTemplateParameter(MessageContext ctx, String paramName) {
        Stack funcStack = (Stack) ctx.getProperty(Constants.SYNAPSE_FUNCTION_STACK);
        TemplateContext currentFuncHolder = (TemplateContext) funcStack.peek();
        return currentFuncHolder.getParameterValue(paramName);
    }

    private static String getResultProperty(MessageContext context) {
        return lookupTemplateParameter(context, Constants.RESPONSE_VARIABLE).toString();
    }
}
