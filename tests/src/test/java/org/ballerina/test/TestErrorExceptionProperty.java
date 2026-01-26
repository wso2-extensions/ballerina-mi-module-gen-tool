/*
 * Copyright (c) 2026, WSO2 LLC. (http://wso2.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ballerina.test;

import io.ballerina.stdlib.mi.BalConnectorConfig;
import io.ballerina.stdlib.mi.BalConnectorFunction;
import io.ballerina.stdlib.mi.BallerinaExecutionException;
import io.ballerina.stdlib.mi.ModuleInfo;
import org.apache.synapse.SynapseConstants;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Test class to verify ERROR_EXCEPTION property is set when Ballerina exceptions occur.
 * This ensures that the actual exception object is available in the message context
 * for error handling in MI fault sequences.
 */
public class TestErrorExceptionProperty {
    private static final String CONNECTION_NAME = "testErrorExceptionConnection";

    @BeforeClass
    public void setupRuntime() throws Exception {
        // Initialize BalConnectorConfig with the unionProject module which has error-throwing functions
        ModuleInfo moduleInfo = new ModuleInfo("testOrg", "unionProject", "1");
        BalConnectorConfig config = new BalConnectorConfig(moduleInfo);

        // Create a context for connection initialization
        TestMessageContext initContext = ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .isConnection(true)
                .objectTypeName("UnionClient")
                .addParameter("serviceUrl", "string", "http://test.api.com")
                .addParameter("connectionType", "string", "UNIONPROJECT_UNIONCLIENT")
                .build();

        initContext.setProperty("UNIONPROJECT_UNIONCLIENT_objectTypeName", "UnionClient");
        initContext.setProperty("UNIONPROJECT_UNIONCLIENT_paramSize", 1);
        initContext.setProperty("UNIONPROJECT_UNIONCLIENT_paramFunctionName", "init");
        initContext.setProperty("UNIONPROJECT_UNIONCLIENT_param0", "serviceUrl");
        initContext.setProperty("UNIONPROJECT_UNIONCLIENT_paramType0", "string");

        config.connect(initContext);
    }

    @Test(description = "Test ERROR_EXCEPTION property is set when BallerinaExecutionException is thrown")
    public void testErrorExceptionPropertySetOnBallerinaException() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        TestMessageContext context = ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("processWithError")
                .returnType("union")
                .addParameter("shouldThrowError", "boolean", "true")
                .build();

        context.setProperty("param0", "shouldThrowError");
        context.setProperty("paramType0", "boolean");
        context.setProperty("paramFunctionName", "processWithError");
        context.setProperty("paramSize", 1);
        context.setProperty("returnType", "union");

        try {
            connector.connect(context);
            Assert.fail("Expected an exception to be thrown");
        } catch (Throwable t) {
            // Verify the exception was thrown as expected (message now contains the actual Ballerina error)
            Assert.assertNotNull(t.getMessage(), "Exception message should not be null");
            Assert.assertTrue(t.getMessage().contains("Operation failed") || t.getMessage().contains("Invalid input"),
                    "Exception message should contain Ballerina error details");

            // Verify ERROR_EXCEPTION property is set on the message context
            Object errorException = context.getProperty(SynapseConstants.ERROR_EXCEPTION);
            Assert.assertNotNull(errorException,
                    "ERROR_EXCEPTION property should be set when exception occurs");

            // Verify the ERROR_EXCEPTION contains the actual exception
            Assert.assertTrue(errorException instanceof Exception,
                    "ERROR_EXCEPTION should be an Exception instance");

            // Verify the cause is BallerinaExecutionException
            Assert.assertEquals(t.getCause().getClass(), BallerinaExecutionException.class,
                    "Cause should be BallerinaExecutionException");
        }
    }

    @Test(description = "Test ERROR_EXCEPTION property contains BallerinaExecutionException with correct message")
    public void testErrorExceptionPropertyContainsCorrectException() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        TestMessageContext context = ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("processWithError")
                .returnType("union")
                .addParameter("shouldThrowError", "boolean", "true")
                .build();

        context.setProperty("param0", "shouldThrowError");
        context.setProperty("paramType0", "boolean");
        context.setProperty("paramFunctionName", "processWithError");
        context.setProperty("paramSize", 1);
        context.setProperty("returnType", "union");

        try {
            connector.connect(context);
            Assert.fail("Expected an exception to be thrown");
        } catch (Throwable t) {
            // Get the ERROR_EXCEPTION from message context
            Object errorException = context.getProperty(SynapseConstants.ERROR_EXCEPTION);
            Assert.assertNotNull(errorException, "ERROR_EXCEPTION should not be null");

            // Cast to Exception and verify message
            Exception exception = (Exception) errorException;
            Assert.assertNotNull(exception.getMessage(),
                    "Exception message should not be null");

            // The exception should be either AxisFault or BallerinaExecutionException
            boolean isValidExceptionType =
                    errorException instanceof BallerinaExecutionException ||
                    errorException instanceof org.apache.axis2.AxisFault;
            Assert.assertTrue(isValidExceptionType,
                    "ERROR_EXCEPTION should be BallerinaExecutionException or AxisFault");
        }
    }

    @Test(description = "Test ERROR_EXCEPTION property is NOT set when execution succeeds")
    public void testErrorExceptionPropertyNotSetOnSuccess() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        TestMessageContext context = ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("processWithError")
                .returnType("union")
                .addParameter("shouldThrowError", "boolean", "false")
                .build();

        context.setProperty("param0", "shouldThrowError");
        context.setProperty("paramType0", "boolean");
        context.setProperty("paramFunctionName", "processWithError");
        context.setProperty("paramSize", 1);
        context.setProperty("returnType", "union");

        // Execute successfully - should not throw
        connector.connect(context);

        // Verify ERROR_EXCEPTION is NOT set when execution succeeds
        Object errorException = context.getProperty(SynapseConstants.ERROR_EXCEPTION);
        Assert.assertNull(errorException,
                "ERROR_EXCEPTION property should NOT be set when execution succeeds");
    }

    @Test(description = "Test ERROR_EXCEPTION property preserves exception stack trace")
    public void testErrorExceptionPropertyPreservesStackTrace() throws Exception {
        BalConnectorFunction connector = new BalConnectorFunction();

        TestMessageContext context = ConnectorContextBuilder.connectorContext()
                .connectionName(CONNECTION_NAME)
                .methodName("processWithError")
                .returnType("union")
                .addParameter("shouldThrowError", "boolean", "true")
                .build();

        context.setProperty("param0", "shouldThrowError");
        context.setProperty("paramType0", "boolean");
        context.setProperty("paramFunctionName", "processWithError");
        context.setProperty("paramSize", 1);
        context.setProperty("returnType", "union");

        try {
            connector.connect(context);
            Assert.fail("Expected an exception to be thrown");
        } catch (Throwable t) {
            // Get the ERROR_EXCEPTION from message context
            Object errorException = context.getProperty(SynapseConstants.ERROR_EXCEPTION);
            Assert.assertNotNull(errorException, "ERROR_EXCEPTION should not be null");

            // Verify stack trace is available
            Exception exception = (Exception) errorException;
            StackTraceElement[] stackTrace = exception.getStackTrace();
            Assert.assertNotNull(stackTrace, "Stack trace should not be null");
            Assert.assertTrue(stackTrace.length > 0,
                    "Stack trace should contain elements for debugging");
        }
    }
}
