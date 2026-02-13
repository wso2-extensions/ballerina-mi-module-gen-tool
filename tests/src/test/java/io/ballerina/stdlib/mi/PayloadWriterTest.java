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

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.soap.SOAPBody;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.AxisFault;
import org.apache.synapse.MessageContext;
import org.apache.synapse.commons.json.JsonUtil;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.xml.namespace.QName;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for PayloadWriter utility class.
 */
public class PayloadWriterTest {

    @Test
    public void testOverwriteBody_NullPayload() throws AxisFault {
        MessageContext messageContext = mock(MessageContext.class);
        PayloadWriter.overwriteBody(messageContext, null);
        Mockito.verifyNoInteractions(messageContext);
    }

    @Test
    public void testOverwriteBody_StringPayload() throws AxisFault {
        try (MockedStatic<JsonUtil> jsonUtilMock = Mockito.mockStatic(JsonUtil.class)) {
            Axis2MessageContext synCtx = mock(Axis2MessageContext.class);
            org.apache.axis2.context.MessageContext axis2MsgCtx = mock(org.apache.axis2.context.MessageContext.class);
            when(synCtx.getAxis2MessageContext()).thenReturn(axis2MsgCtx);

            SOAPEnvelope envelope = mock(SOAPEnvelope.class);
            SOAPBody body = mock(SOAPBody.class);
            when(axis2MsgCtx.getEnvelope()).thenReturn(envelope);
            when(envelope.getBody()).thenReturn(body);

            String payload = "Hello World";
            PayloadWriter.overwriteBody(synCtx, payload);

            jsonUtilMock.verify(() -> JsonUtil.removeJsonPayload(axis2MsgCtx));
            verify(axis2MsgCtx).setProperty("messageType", "text/plain");
            verify(axis2MsgCtx).setProperty("contentType", "text/plain");
            verify(axis2MsgCtx).removeProperty("NO_ENTITY_BODY");
        }
    }

    @Test
    public void testOverwriteBody_JsonPayload() throws AxisFault {
        try (MockedStatic<JsonUtil> jsonUtilMock = Mockito.mockStatic(JsonUtil.class)) {
            Axis2MessageContext synCtx = mock(Axis2MessageContext.class);
            org.apache.axis2.context.MessageContext axis2MsgCtx = mock(org.apache.axis2.context.MessageContext.class);
            when(synCtx.getAxis2MessageContext()).thenReturn(axis2MsgCtx);

            JsonObject jsonPayload = new JsonObject();
            jsonPayload.addProperty("key", "value");

            jsonUtilMock.when(() -> JsonUtil.getNewJsonPayload(any(), anyString(), anyBoolean(), anyBoolean()))
                        .thenReturn(mock(OMElement.class));

            PayloadWriter.overwriteBody(synCtx, jsonPayload);

            verify(axis2MsgCtx).setProperty("messageType", "application/json");
            verify(axis2MsgCtx).setProperty("contentType", "application/json");
            verify(axis2MsgCtx).removeProperty("NO_ENTITY_BODY");
        }
    }

    @Test
    public void testOverwriteBody_XmlPayload() throws AxisFault {
        try (MockedStatic<JsonUtil> jsonUtilMock = Mockito.mockStatic(JsonUtil.class)) {
            Axis2MessageContext synCtx = mock(Axis2MessageContext.class);
            org.apache.axis2.context.MessageContext axis2MsgCtx = mock(org.apache.axis2.context.MessageContext.class);
            when(synCtx.getAxis2MessageContext()).thenReturn(axis2MsgCtx);

            SOAPEnvelope envelope = mock(SOAPEnvelope.class);
            SOAPBody body = mock(SOAPBody.class);
            when(axis2MsgCtx.getEnvelope()).thenReturn(envelope);
            when(envelope.getBody()).thenReturn(body);

            OMFactory factory = OMAbstractFactory.getOMFactory();
            OMElement xmlPayload = factory.createOMElement(new QName("root"));
            OMElement child = factory.createOMElement(new QName("child"));
            child.setText("value");
            xmlPayload.addChild(child);

            PayloadWriter.overwriteBody(synCtx, xmlPayload);

            jsonUtilMock.verify(() -> JsonUtil.removeJsonPayload(axis2MsgCtx));
            verify(axis2MsgCtx).setProperty("messageType", "application/xml");
            verify(axis2MsgCtx).setProperty("contentType", "application/xml");
            verify(body).addChild(xmlPayload);
            verify(axis2MsgCtx).removeProperty("NO_ENTITY_BODY");
        }
    }

    @Test(expectedExceptions = AxisFault.class, expectedExceptionsMessageRegExp = ".*Unsupported payload type.*")
    public void testOverwriteBody_UnsupportedType() throws AxisFault {
        Axis2MessageContext synCtx = mock(Axis2MessageContext.class);
        org.apache.axis2.context.MessageContext axis2MsgCtx = mock(org.apache.axis2.context.MessageContext.class);
        when(synCtx.getAxis2MessageContext()).thenReturn(axis2MsgCtx);

        PayloadWriter.overwriteBody(synCtx, Integer.valueOf(123));
    }

    @Test
    public void testOverwriteBody_JsonPrimitive() throws AxisFault {
        try (MockedStatic<JsonUtil> jsonUtilMock = Mockito.mockStatic(JsonUtil.class)) {
            Axis2MessageContext synCtx = mock(Axis2MessageContext.class);
            org.apache.axis2.context.MessageContext axis2MsgCtx = mock(org.apache.axis2.context.MessageContext.class);
            when(synCtx.getAxis2MessageContext()).thenReturn(axis2MsgCtx);

            JsonPrimitive jsonPayload = new JsonPrimitive("simple string value");

            jsonUtilMock.when(() -> JsonUtil.getNewJsonPayload(any(), anyString(), anyBoolean(), anyBoolean()))
                        .thenReturn(mock(OMElement.class));

            PayloadWriter.overwriteBody(synCtx, jsonPayload);

            verify(axis2MsgCtx).setProperty("messageType", "application/json");
            verify(axis2MsgCtx).setProperty("contentType", "application/json");
        }
    }
}
