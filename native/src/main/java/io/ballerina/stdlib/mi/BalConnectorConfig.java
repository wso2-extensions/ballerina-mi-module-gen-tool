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
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BObject;
import org.apache.synapse.MessageContext;
import org.apache.synapse.mediators.template.TemplateContext;
import org.wso2.integration.connector.core.AbstractConnector;
import org.wso2.integration.connector.core.ConnectException;
import org.wso2.integration.connector.core.connection.ConnectionHandler;

import java.util.Stack;

public class BalConnectorConfig extends AbstractConnector {
    private static volatile Runtime rt = null;
    private static Module module = null;
    private String orgName;
    private String moduleName;
    private String version;
    private final BalExecutor balExecutor = new BalExecutor();

    public BalConnectorConfig() {
    }

    // This constructor is added to test the connector
    public BalConnectorConfig(ModuleInfo moduleInfo) {
        this.orgName = moduleInfo.getOrgName();
        this.moduleName = moduleInfo.getModuleName();
        this.version = moduleInfo.getModuleVersion();
        init();
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
        if (rt == null) {
            synchronized (BalConnectorConfig.class) {
                if (rt == null) {
                    init();
                }
            }
        }
        String connectorName = module.getName();
        String connectionName = lookupTemplateParamater(messageContext, "name");
        String connectionType = lookupTemplateParamater(messageContext, "connectionType");
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

                Object[] args = new Object[Integer.parseInt(messageContext.getProperty(connectionType + "_" + Constants.SIZE).toString())];
                setParameters(args, messageContext, connectionType);
                clientObject = ValueCreator.createObjectValue(module, messageContext.getProperty(connectionType + "_objectTypeName").toString(), args);
            } catch (BError clientError) {
                handleException(clientError.getMessage(), messageContext);
            }
            BalConnectorConnection balConnection = new BalConnectorConnection(module, messageContext.getProperty(connectionType + "_objectTypeName").toString(), clientObject);
            try {
                handler.createConnection(connectorName, connectionName, balConnection, messageContext);
            } catch (NoSuchMethodError e) {
                handler.createConnection(connectorName, connectionName, balConnection);
            }
        }
        messageContext.setProperty("connectionName", connectionName);
        }

    private void init() {
        module = new Module(orgName, moduleName, version);
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

    public static String lookupTemplateParamater(MessageContext ctxt, String paramName) throws ConnectException {
        Stack<TemplateContext> funcStack = (Stack) ctxt.getProperty(Constants.SYNAPSE_FUNCTION_STACK);
        TemplateContext currentFuncHolder = funcStack.peek();
        Object value = currentFuncHolder.getParameterValue(paramName);
        if (value == null) {
            throw new ConnectException("Required template parameter '" + paramName + "' is missing");
        }
        return value.toString();
    }

    private void setParameters(Object[] args, MessageContext context, String connectionType) {
        for (int i = 0; i < args.length; i++) {
            Object param = balExecutor.getParameter(context, connectionType + "_param" + i, connectionType + "_paramType" + i, i);
            //TODO: check handling null parameters
//            if (param == null) {
//                return false;
//            }
            args[i] = param;
        }
    }

    public String getOrgName() {
        return orgName;
    }

    public void setOrgName(String orgName) {
        this.orgName = orgName;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
