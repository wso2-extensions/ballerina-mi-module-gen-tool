/*
 * Copyright (c) 2026, WSO2 LLC. (https://www.wso2.com).
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
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.wso2.integration.connector.core.ConnectException;
import org.wso2.integration.connector.core.connection.ConnectionHandler;

import java.util.Stack;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for BalConnectorConfig class.
 */
public class BalConnectorConfigTest {

    // Test default constructor
    @Test
    public void testDefaultConstructor() {
        BalConnectorConfig config = new BalConnectorConfig();
        Assert.assertNull(config.getOrgName());
        Assert.assertNull(config.getModuleName());
        Assert.assertNull(config.getVersion());
    }

    // Test constructor with ModuleInfo
    @Test
    public void testConstructorWithModuleInfo() {
        try (MockedStatic<Runtime> runtimeMock = Mockito.mockStatic(Runtime.class)) {
            Runtime mockRuntime = mock(Runtime.class);
            runtimeMock.when(() -> Runtime.from(any(Module.class))).thenReturn(mockRuntime);

            ModuleInfo moduleInfo = mock(ModuleInfo.class);
            when(moduleInfo.getOrgName()).thenReturn("testOrg");
            when(moduleInfo.getModuleName()).thenReturn("testModule");
            when(moduleInfo.getModuleVersion()).thenReturn("1.0.0");

            BalConnectorConfig config = new BalConnectorConfig(moduleInfo);

            Assert.assertEquals(config.getOrgName(), "testOrg");
            Assert.assertEquals(config.getModuleName(), "testModule");
            Assert.assertEquals(config.getVersion(), "1.0.0");
        }
    }

    // Test getters and setters
    @Test
    public void testSetAndGetOrgName() {
        BalConnectorConfig config = new BalConnectorConfig();
        config.setOrgName("myOrg");
        Assert.assertEquals(config.getOrgName(), "myOrg");
    }

    @Test
    public void testSetAndGetModuleName() {
        BalConnectorConfig config = new BalConnectorConfig();
        config.setModuleName("myModule");
        Assert.assertEquals(config.getModuleName(), "myModule");
    }

    @Test
    public void testSetAndGetVersion() {
        BalConnectorConfig config = new BalConnectorConfig();
        config.setVersion("2.0.0");
        Assert.assertEquals(config.getVersion(), "2.0.0");
    }

    // Test lookupTemplateParamater with null value (throws ConnectException)
    @Test(expectedExceptions = ConnectException.class, expectedExceptionsMessageRegExp = ".*Required template parameter 'missingParam' is missing.*")
    public void testLookupTemplateParamater_NullValue() throws ConnectException {
        MessageContext ctxt = mock(MessageContext.class);
        Stack<TemplateContext> funcStack = new Stack<>();
        TemplateContext templateContext = mock(TemplateContext.class);
        funcStack.push(templateContext);

        when(ctxt.getProperty(Constants.SYNAPSE_FUNCTION_STACK)).thenReturn(funcStack);
        when(templateContext.getParameterValue("missingParam")).thenReturn(null);

        BalConnectorConfig.lookupTemplateParamater(ctxt, "missingParam");
    }

    // Test lookupTemplateParamater with valid value
    @Test
    public void testLookupTemplateParamater_ValidValue() throws ConnectException {
        MessageContext ctxt = mock(MessageContext.class);
        Stack<TemplateContext> funcStack = new Stack<>();
        TemplateContext templateContext = mock(TemplateContext.class);
        funcStack.push(templateContext);

        when(ctxt.getProperty(Constants.SYNAPSE_FUNCTION_STACK)).thenReturn(funcStack);
        when(templateContext.getParameterValue("testParam")).thenReturn("testValue");

        String result = BalConnectorConfig.lookupTemplateParamater(ctxt, "testParam");
        Assert.assertEquals(result, "testValue");
    }

    // Test connect with missing paramSize
    @Test(expectedExceptions = ConnectException.class, expectedExceptionsMessageRegExp = ".*Required property 'TestConnection_paramSize' is missing.*")
    public void testConnect_MissingParamSize() throws ConnectException {
        try (MockedStatic<Runtime> runtimeMock = Mockito.mockStatic(Runtime.class);
             MockedStatic<ConnectionHandler> handlerMock = Mockito.mockStatic(ConnectionHandler.class)) {

            // Setup runtime
            Runtime mockRuntime = mock(Runtime.class);
            runtimeMock.when(() -> Runtime.from(any(Module.class))).thenReturn(mockRuntime);

            // Setup connection handler
            ConnectionHandler mockHandler = mock(ConnectionHandler.class);
            handlerMock.when(ConnectionHandler::getConnectionHandler).thenReturn(mockHandler);
            when(mockHandler.checkIfConnectionExists(anyString(), anyString())).thenReturn(false);

            // Create connector with ModuleInfo
            ModuleInfo moduleInfo = mock(ModuleInfo.class);
            when(moduleInfo.getOrgName()).thenReturn("testOrg");
            when(moduleInfo.getModuleName()).thenReturn("testModule");
            when(moduleInfo.getModuleVersion()).thenReturn("1.0.0");

            BalConnectorConfig config = new BalConnectorConfig(moduleInfo);

            // Setup message context with template parameters but missing paramSize
            MessageContext msgCtx = mock(MessageContext.class);
            Stack<TemplateContext> funcStack = new Stack<>();
            TemplateContext templateContext = mock(TemplateContext.class);
            funcStack.push(templateContext);

            when(msgCtx.getProperty(Constants.SYNAPSE_FUNCTION_STACK)).thenReturn(funcStack);
            when(templateContext.getParameterValue("name")).thenReturn("testConnection");
            when(templateContext.getParameterValue("connectionType")).thenReturn("TestConnection");
            when(msgCtx.getProperty("TestConnection_paramSize")).thenReturn(null);

            config.connect(msgCtx);
        }
    }

    // Test connect with invalid paramSize (NumberFormatException)
    @Test(expectedExceptions = ConnectException.class, expectedExceptionsMessageRegExp = ".*Invalid value for property 'TestConnection_paramSize'.*")
    public void testConnect_InvalidParamSize() throws ConnectException {
        try (MockedStatic<Runtime> runtimeMock = Mockito.mockStatic(Runtime.class);
             MockedStatic<ConnectionHandler> handlerMock = Mockito.mockStatic(ConnectionHandler.class)) {

            // Setup runtime
            Runtime mockRuntime = mock(Runtime.class);
            runtimeMock.when(() -> Runtime.from(any(Module.class))).thenReturn(mockRuntime);

            // Setup connection handler
            ConnectionHandler mockHandler = mock(ConnectionHandler.class);
            handlerMock.when(ConnectionHandler::getConnectionHandler).thenReturn(mockHandler);
            when(mockHandler.checkIfConnectionExists(anyString(), anyString())).thenReturn(false);

            // Create connector with ModuleInfo
            ModuleInfo moduleInfo = mock(ModuleInfo.class);
            when(moduleInfo.getOrgName()).thenReturn("testOrg");
            when(moduleInfo.getModuleName()).thenReturn("testModule");
            when(moduleInfo.getModuleVersion()).thenReturn("1.0.0");

            BalConnectorConfig config = new BalConnectorConfig(moduleInfo);

            // Setup message context with invalid paramSize
            MessageContext msgCtx = mock(MessageContext.class);
            Stack<TemplateContext> funcStack = new Stack<>();
            TemplateContext templateContext = mock(TemplateContext.class);
            funcStack.push(templateContext);

            when(msgCtx.getProperty(Constants.SYNAPSE_FUNCTION_STACK)).thenReturn(funcStack);
            when(templateContext.getParameterValue("name")).thenReturn("testConnection");
            when(templateContext.getParameterValue("connectionType")).thenReturn("TestConnection");
            when(msgCtx.getProperty("TestConnection_paramSize")).thenReturn("not_a_number");

            config.connect(msgCtx);
        }
    }

    // Test connect with missing objectTypeName
    @Test(expectedExceptions = ConnectException.class, expectedExceptionsMessageRegExp = ".*Required property 'TestConnection_objectTypeName' is missing.*")
    public void testConnect_MissingObjectTypeName() throws ConnectException {
        try (MockedStatic<Runtime> runtimeMock = Mockito.mockStatic(Runtime.class);
             MockedStatic<ConnectionHandler> handlerMock = Mockito.mockStatic(ConnectionHandler.class)) {

            // Setup runtime
            Runtime mockRuntime = mock(Runtime.class);
            runtimeMock.when(() -> Runtime.from(any(Module.class))).thenReturn(mockRuntime);

            // Setup connection handler
            ConnectionHandler mockHandler = mock(ConnectionHandler.class);
            handlerMock.when(ConnectionHandler::getConnectionHandler).thenReturn(mockHandler);
            when(mockHandler.checkIfConnectionExists(anyString(), anyString())).thenReturn(false);

            // Create connector with ModuleInfo
            ModuleInfo moduleInfo = mock(ModuleInfo.class);
            when(moduleInfo.getOrgName()).thenReturn("testOrg");
            when(moduleInfo.getModuleName()).thenReturn("testModule");
            when(moduleInfo.getModuleVersion()).thenReturn("1.0.0");

            BalConnectorConfig config = new BalConnectorConfig(moduleInfo);

            // Setup message context with valid paramSize but missing objectTypeName
            MessageContext msgCtx = mock(MessageContext.class);
            Stack<TemplateContext> funcStack = new Stack<>();
            TemplateContext templateContext = mock(TemplateContext.class);
            funcStack.push(templateContext);

            when(msgCtx.getProperty(Constants.SYNAPSE_FUNCTION_STACK)).thenReturn(funcStack);
            when(templateContext.getParameterValue("name")).thenReturn("testConnection");
            when(templateContext.getParameterValue("connectionType")).thenReturn("TestConnection");
            when(msgCtx.getProperty("TestConnection_paramSize")).thenReturn("0");
            when(msgCtx.getProperty("TestConnection_objectTypeName")).thenReturn(null);

            config.connect(msgCtx);
        }
    }

    // Test connect with BError thrown during client creation
    @Test(expectedExceptions = ConnectException.class, expectedExceptionsMessageRegExp = ".*Ballerina client error.*")
    public void testConnect_BErrorDuringClientCreation() throws ConnectException {
        try (MockedStatic<Runtime> runtimeMock = Mockito.mockStatic(Runtime.class);
             MockedStatic<ConnectionHandler> handlerMock = Mockito.mockStatic(ConnectionHandler.class);
             MockedStatic<ValueCreator> valueCreatorMock = Mockito.mockStatic(ValueCreator.class)) {

            // Setup runtime
            Runtime mockRuntime = mock(Runtime.class);
            runtimeMock.when(() -> Runtime.from(any(Module.class))).thenReturn(mockRuntime);

            // Setup connection handler
            ConnectionHandler mockHandler = mock(ConnectionHandler.class);
            handlerMock.when(ConnectionHandler::getConnectionHandler).thenReturn(mockHandler);
            when(mockHandler.checkIfConnectionExists(anyString(), anyString())).thenReturn(false);

            // Setup BError
            BError mockError = mock(BError.class);
            when(mockError.getMessage()).thenReturn("Ballerina client error");
            when(mockError.toString()).thenReturn("BError: Ballerina client error");

            // Mock ValueCreator to throw BError
            valueCreatorMock.when(() -> ValueCreator.createObjectValue(any(Module.class), anyString(), any(Object[].class)))
                    .thenThrow(mockError);

            // Create connector with ModuleInfo
            ModuleInfo moduleInfo = mock(ModuleInfo.class);
            when(moduleInfo.getOrgName()).thenReturn("testOrg");
            when(moduleInfo.getModuleName()).thenReturn("testModule");
            when(moduleInfo.getModuleVersion()).thenReturn("1.0.0");

            BalConnectorConfig config = new BalConnectorConfig(moduleInfo);

            // Setup message context with all required properties
            MessageContext msgCtx = mock(MessageContext.class);
            Stack<TemplateContext> funcStack = new Stack<>();
            TemplateContext templateContext = mock(TemplateContext.class);
            funcStack.push(templateContext);

            when(msgCtx.getProperty(Constants.SYNAPSE_FUNCTION_STACK)).thenReturn(funcStack);
            when(templateContext.getParameterValue("name")).thenReturn("testConnection");
            when(templateContext.getParameterValue("connectionType")).thenReturn("TestConnection");
            when(msgCtx.getProperty("TestConnection_paramSize")).thenReturn("0");
            when(msgCtx.getProperty("TestConnection_objectTypeName")).thenReturn("TestClient");

            config.connect(msgCtx);
        }
    }

    // Test connect when connection already exists
    @Test
    public void testConnect_ConnectionExists() throws ConnectException {
        try (MockedStatic<Runtime> runtimeMock = Mockito.mockStatic(Runtime.class);
             MockedStatic<ConnectionHandler> handlerMock = Mockito.mockStatic(ConnectionHandler.class)) {

            // Setup runtime
            Runtime mockRuntime = mock(Runtime.class);
            runtimeMock.when(() -> Runtime.from(any(Module.class))).thenReturn(mockRuntime);

            // Setup connection handler - connection already exists
            ConnectionHandler mockHandler = mock(ConnectionHandler.class);
            handlerMock.when(ConnectionHandler::getConnectionHandler).thenReturn(mockHandler);
            when(mockHandler.checkIfConnectionExists(anyString(), anyString())).thenReturn(true);

            // Create connector with ModuleInfo
            ModuleInfo moduleInfo = mock(ModuleInfo.class);
            when(moduleInfo.getOrgName()).thenReturn("testOrg");
            when(moduleInfo.getModuleName()).thenReturn("testModule");
            when(moduleInfo.getModuleVersion()).thenReturn("1.0.0");

            BalConnectorConfig config = new BalConnectorConfig(moduleInfo);

            // Setup message context
            MessageContext msgCtx = mock(MessageContext.class);
            Stack<TemplateContext> funcStack = new Stack<>();
            TemplateContext templateContext = mock(TemplateContext.class);
            funcStack.push(templateContext);

            when(msgCtx.getProperty(Constants.SYNAPSE_FUNCTION_STACK)).thenReturn(funcStack);
            when(templateContext.getParameterValue("name")).thenReturn("existingConnection");
            when(templateContext.getParameterValue("connectionType")).thenReturn("TestConnection");

            config.connect(msgCtx);

            // Verify connectionName was set
            verify(msgCtx).setProperty("connectionName", "existingConnection");
        }
    }

    // Test connect with successful connection creation
    @Test
    public void testConnect_SuccessfulConnection() throws ConnectException {
        try (MockedStatic<Runtime> runtimeMock = Mockito.mockStatic(Runtime.class);
             MockedStatic<ConnectionHandler> handlerMock = Mockito.mockStatic(ConnectionHandler.class);
             MockedStatic<ValueCreator> valueCreatorMock = Mockito.mockStatic(ValueCreator.class)) {

            // Setup runtime
            Runtime mockRuntime = mock(Runtime.class);
            runtimeMock.when(() -> Runtime.from(any(Module.class))).thenReturn(mockRuntime);

            // Setup connection handler
            ConnectionHandler mockHandler = mock(ConnectionHandler.class);
            handlerMock.when(ConnectionHandler::getConnectionHandler).thenReturn(mockHandler);
            when(mockHandler.checkIfConnectionExists(anyString(), anyString())).thenReturn(false);

            // Setup ValueCreator
            BObject mockClientObject = mock(BObject.class);
            valueCreatorMock.when(() -> ValueCreator.createObjectValue(any(Module.class), anyString(), any(Object[].class)))
                    .thenReturn(mockClientObject);

            // Create connector with ModuleInfo
            ModuleInfo moduleInfo = mock(ModuleInfo.class);
            when(moduleInfo.getOrgName()).thenReturn("testOrg");
            when(moduleInfo.getModuleName()).thenReturn("testModule");
            when(moduleInfo.getModuleVersion()).thenReturn("1.0.0");

            BalConnectorConfig config = new BalConnectorConfig(moduleInfo);

            // Setup message context with all required properties
            MessageContext msgCtx = mock(MessageContext.class);
            Stack<TemplateContext> funcStack = new Stack<>();
            TemplateContext templateContext = mock(TemplateContext.class);
            funcStack.push(templateContext);

            when(msgCtx.getProperty(Constants.SYNAPSE_FUNCTION_STACK)).thenReturn(funcStack);
            when(templateContext.getParameterValue("name")).thenReturn("newConnection");
            when(templateContext.getParameterValue("connectionType")).thenReturn("TestConnection");
            when(msgCtx.getProperty("TestConnection_paramSize")).thenReturn("0");
            when(msgCtx.getProperty("TestConnection_objectTypeName")).thenReturn("TestClient");

            config.connect(msgCtx);

            // Verify connectionName was set
            verify(msgCtx).setProperty("connectionName", "newConnection");
        }
    }

    // Test connect with NoSuchMethodError fallback (3-arg createConnection)
    @Test
    public void testConnect_NoSuchMethodErrorFallback() throws ConnectException {
        try (MockedStatic<Runtime> runtimeMock = Mockito.mockStatic(Runtime.class);
             MockedStatic<ConnectionHandler> handlerMock = Mockito.mockStatic(ConnectionHandler.class);
             MockedStatic<ValueCreator> valueCreatorMock = Mockito.mockStatic(ValueCreator.class)) {

            // Setup runtime
            Runtime mockRuntime = mock(Runtime.class);
            runtimeMock.when(() -> Runtime.from(any(Module.class))).thenReturn(mockRuntime);

            // Setup connection handler - throw NoSuchMethodError on 4-arg createConnection
            ConnectionHandler mockHandler = mock(ConnectionHandler.class);
            handlerMock.when(ConnectionHandler::getConnectionHandler).thenReturn(mockHandler);
            when(mockHandler.checkIfConnectionExists(anyString(), anyString())).thenReturn(false);
            doThrow(new NoSuchMethodError("Method not found")).when(mockHandler)
                    .createConnection(anyString(), anyString(), any(BalConnectorConnection.class), any(MessageContext.class));

            // Setup ValueCreator
            BObject mockClientObject = mock(BObject.class);
            valueCreatorMock.when(() -> ValueCreator.createObjectValue(any(Module.class), anyString(), any(Object[].class)))
                    .thenReturn(mockClientObject);

            // Create connector with ModuleInfo
            ModuleInfo moduleInfo = mock(ModuleInfo.class);
            when(moduleInfo.getOrgName()).thenReturn("testOrg");
            when(moduleInfo.getModuleName()).thenReturn("testModule");
            when(moduleInfo.getModuleVersion()).thenReturn("1.0.0");

            BalConnectorConfig config = new BalConnectorConfig(moduleInfo);

            // Setup message context with all required properties
            MessageContext msgCtx = mock(MessageContext.class);
            Stack<TemplateContext> funcStack = new Stack<>();
            TemplateContext templateContext = mock(TemplateContext.class);
            funcStack.push(templateContext);

            when(msgCtx.getProperty(Constants.SYNAPSE_FUNCTION_STACK)).thenReturn(funcStack);
            when(templateContext.getParameterValue("name")).thenReturn("fallbackConnection");
            when(templateContext.getParameterValue("connectionType")).thenReturn("TestConnection");
            when(msgCtx.getProperty("TestConnection_paramSize")).thenReturn("0");
            when(msgCtx.getProperty("TestConnection_objectTypeName")).thenReturn("TestClient");

            config.connect(msgCtx);

            // Verify both createConnection methods were called (first 4-arg failed, then 3-arg succeeded)
            verify(mockHandler).createConnection(anyString(), anyString(), any(BalConnectorConnection.class), any(MessageContext.class));
            verify(mockHandler).createConnection(anyString(), anyString(), any(BalConnectorConnection.class));
            verify(msgCtx).setProperty("connectionName", "fallbackConnection");
        }
    }

    // Test getRuntime and getModule static methods
    @Test
    public void testGetRuntimeAndModule() {
        try (MockedStatic<Runtime> runtimeMock = Mockito.mockStatic(Runtime.class)) {
            Runtime mockRuntime = mock(Runtime.class);
            runtimeMock.when(() -> Runtime.from(any(Module.class))).thenReturn(mockRuntime);

            ModuleInfo moduleInfo = mock(ModuleInfo.class);
            when(moduleInfo.getOrgName()).thenReturn("testOrg");
            when(moduleInfo.getModuleName()).thenReturn("testModule");
            when(moduleInfo.getModuleVersion()).thenReturn("1.0.0");

            new BalConnectorConfig(moduleInfo);

            Assert.assertNotNull(BalConnectorConfig.getRuntime());
            Assert.assertNotNull(BalConnectorConfig.getModule());
        }
    }

    // Test connect with parameters (setParameters coverage)
    @Test
    public void testConnect_WithParameters() throws ConnectException {
        try (MockedStatic<Runtime> runtimeMock = Mockito.mockStatic(Runtime.class);
             MockedStatic<ConnectionHandler> handlerMock = Mockito.mockStatic(ConnectionHandler.class);
             MockedStatic<ValueCreator> valueCreatorMock = Mockito.mockStatic(ValueCreator.class)) {

            // Setup runtime
            Runtime mockRuntime = mock(Runtime.class);
            runtimeMock.when(() -> Runtime.from(any(Module.class))).thenReturn(mockRuntime);

            // Setup connection handler
            ConnectionHandler mockHandler = mock(ConnectionHandler.class);
            handlerMock.when(ConnectionHandler::getConnectionHandler).thenReturn(mockHandler);
            when(mockHandler.checkIfConnectionExists(anyString(), anyString())).thenReturn(false);

            // Setup ValueCreator
            BObject mockClientObject = mock(BObject.class);
            valueCreatorMock.when(() -> ValueCreator.createObjectValue(any(Module.class), anyString(), any(Object[].class)))
                    .thenReturn(mockClientObject);

            // Create connector with ModuleInfo
            ModuleInfo moduleInfo = mock(ModuleInfo.class);
            when(moduleInfo.getOrgName()).thenReturn("testOrg");
            when(moduleInfo.getModuleName()).thenReturn("testModule");
            when(moduleInfo.getModuleVersion()).thenReturn("1.0.0");

            BalConnectorConfig config = new BalConnectorConfig(moduleInfo);

            // Setup message context with 3 parameters
            MessageContext msgCtx = mock(MessageContext.class);
            Stack<TemplateContext> funcStack = new Stack<>();
            TemplateContext templateContext = mock(TemplateContext.class);
            funcStack.push(templateContext);

            when(msgCtx.getProperty(Constants.SYNAPSE_FUNCTION_STACK)).thenReturn(funcStack);
            when(templateContext.getParameterValue("name")).thenReturn("paramConnection");
            when(templateContext.getParameterValue("connectionType")).thenReturn("TestConnection");
            when(msgCtx.getProperty("TestConnection_paramSize")).thenReturn("3");
            when(msgCtx.getProperty("TestConnection_objectTypeName")).thenReturn("TestClient");
            when(msgCtx.getProperty("TestConnection_param0")).thenReturn("string:value0");
            when(msgCtx.getProperty("TestConnection_paramType0")).thenReturn("string");
            when(msgCtx.getProperty("TestConnection_param1")).thenReturn("int:42");
            when(msgCtx.getProperty("TestConnection_paramType1")).thenReturn("int");
            when(msgCtx.getProperty("TestConnection_param2")).thenReturn("boolean:true");
            when(msgCtx.getProperty("TestConnection_paramType2")).thenReturn("boolean");

            config.connect(msgCtx);

            verify(msgCtx).setProperty("connectionName", "paramConnection");
        }
    }
}
