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
import io.ballerina.runtime.api.Module;
import io.ballerina.runtime.api.Runtime;
import io.ballerina.runtime.api.creators.TypeCreator;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import org.apache.commons.collections.map.HashedMap;
import org.apache.synapse.MessageContext;
import org.apache.synapse.mediators.template.TemplateContext;
import org.ballerinalang.langlib.value.FromJsonStringWithType;
import org.wso2.integration.connector.core.AbstractConnector;
import org.wso2.integration.connector.core.ConnectException;
import org.wso2.integration.connector.core.connection.ConnectionHandler;

import java.util.Stack;

import static io.ballerina.stdlib.mi.Constants.*;

public class BalConnectorConfig extends AbstractConnector {
    private static volatile Runtime rt = null;
    private static Module module = null;

    public BalConnectorConfig() {
        if (rt == null) {
            synchronized (BalConnectorConfig.class) {
                if (rt == null) {
                    System.out.println("runtime created");
                    ModuleInfo moduleInfo = new ModuleInfo();
                    init(moduleInfo);
                }
            }
        }
    }

    @Override
    public void connect(MessageContext messageContext) throws ConnectException {
        /**
         * TODO:
         * Get the connection name
         * There are several ways to create RecordValue
         * Handle connectionType
         * How to access the connection local entry from the message context
         */
        // get name parameter value
        //TODO: Make any updated to support multiple clients
        String connectorName = module.getName();
        String connectionName = lookupTemplateParamater(messageContext, "name");
        //TODO: read parameter args and default values
        //TODO: Use the NodeParser API to generate the argument values

        //TODO: Set fields not set inside the method
        BObject clientObject = null;
        ConnectionHandler handler = ConnectionHandler.getConnectionHandler();
        if (!handler.checkIfConnectionExists(connectorName, connectionName)) {
            try {

                // Using json string to create a record value
//                BString jsonString = StringUtils.fromString("");
//                BMap<BString, Object> recValue = ValueCreator.createRecordValue(module, "ConnectionConfig");
////                TypeCreator.createRecordType(module, StringUtils.fromString("https://disease.sh"))
//                Type type = recValue.getType();
//                Object o = FromJsonStringWithType.fromJsonStringWithType(jsonString, ValueCreator.createTypedescValue(type));

                Object[] args = new Object[Integer.parseInt(messageContext.getProperty(Constants.SIZE).toString())];
                setParameters(args, messageContext);
                clientObject = ValueCreator.createObjectValue(module, messageContext.getProperty("objectTypeName").toString(), args);
            } catch (BError clientError) {
                handleException(clientError.getMessage(), messageContext);
            }
            BalConnectorConnection balConnection = new BalConnectorConnection(module, messageContext.getProperty("objectTypeName").toString(), clientObject);
            try {
                handler.createConnection(connectorName, connectionName, balConnection, messageContext);
            } catch (NoSuchMethodError e) {
                handler.createConnection(connectorName, connectionName, balConnection);
            }
        }
        messageContext.setProperty("connectionName", connectionName);
        }

    private void init (ModuleInfo moduleInfo){
        module = new Module(moduleInfo.getOrgName(), moduleInfo.getModuleName(), moduleInfo.getModuleVersion());
        rt = Runtime.from(module);
        rt.init();
        rt.start();
    }

    public static Runtime getRuntime() {
        return rt;
    }

    public static Module getModule() {
        return module;
    }

    public static String lookupTemplateParamater (MessageContext ctxt, String paramName){
        Stack<TemplateContext> funcStack = (Stack) ctxt.getProperty(Constants.SYNAPSE_FUNCTION_STACK);
        TemplateContext currentFuncHolder = funcStack.peek();
        return currentFuncHolder.getParameterValue(paramName).toString();
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
        Object param = lookupTemplateParamater(context, paramName);
        String stringParam = ((String) param).replaceAll("^\"(.*)\"$", "$1");
        //TODO: check handling null parameters
//        if (param == null) {
//            log.error("Error in getting the ballerina function parameter: " + paramName);
//            return null;
//        }
        String paramType = context.getProperty(type).toString();
        return switch (paramType) {
            //TODO: Revisit handling union and record types
            case BOOLEAN -> Boolean.parseBoolean(stringParam);
            case INT -> Long.parseLong(stringParam);
            case STRING, UNION -> StringUtils.fromString(stringParam);
            case FLOAT -> Double.parseDouble(stringParam);
            case DECIMAL -> ValueCreator.createDecimalValue(stringParam);
            case RECORD -> createRecordValue(stringParam, context, index);
            default -> null;
        };
    }

    private Object createRecordValue(String jsonString, MessageContext context, int paramIndex) {
        String recordName = context.getProperty("recordName" + paramIndex).toString();
        BMap<BString, Object> recValue = ValueCreator.createRecordValue(BalConnectorConfig.getModule(), recordName);
        Type recType = recValue.getType();
        return FromJsonStringWithType.fromJsonStringWithType(StringUtils.fromString(jsonString), ValueCreator.createTypedescValue(recType));
    }
}
