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
import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axiom.soap.SOAP12Constants;
import org.apache.axiom.soap.SOAPBody;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.AxisFault;
import org.apache.synapse.MessageContext;
import org.apache.synapse.commons.json.JsonUtil;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.util.AXIOMUtils;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.annotations.Test;

import javax.xml.namespace.QName;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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

    // Test checkAndReplaceEnvelope with null firstChild (throws AxisFault)
    @Test(expectedExceptions = AxisFault.class, expectedExceptionsMessageRegExp = ".*Generated content is not a valid XML payload.*")
    public void testOverwriteBody_XmlPayload_NullFirstChild() throws AxisFault {
        try (MockedStatic<JsonUtil> jsonUtilMock = Mockito.mockStatic(JsonUtil.class)) {
            Axis2MessageContext synCtx = mock(Axis2MessageContext.class);
            org.apache.axis2.context.MessageContext axis2MsgCtx = mock(org.apache.axis2.context.MessageContext.class);
            when(synCtx.getAxis2MessageContext()).thenReturn(axis2MsgCtx);

            // Create an OMElement with no child elements (firstElement will be null)
            OMFactory factory = OMAbstractFactory.getOMFactory();
            OMElement xmlPayload = factory.createOMElement(new QName("root"));
            // Intentionally not adding any child elements

            PayloadWriter.overwriteBody(synCtx, xmlPayload);
        }
    }

    // Test checkAndReplaceEnvelope with SOAP11 envelope
    @Test
    public void testOverwriteBody_XmlPayload_Soap11Envelope() throws AxisFault {
        try (MockedStatic<JsonUtil> jsonUtilMock = Mockito.mockStatic(JsonUtil.class);
             MockedStatic<AXIOMUtils> axiomUtilsMock = Mockito.mockStatic(AXIOMUtils.class)) {

            Axis2MessageContext synCtx = mock(Axis2MessageContext.class);
            org.apache.axis2.context.MessageContext axis2MsgCtx = mock(org.apache.axis2.context.MessageContext.class);
            when(synCtx.getAxis2MessageContext()).thenReturn(axis2MsgCtx);

            SOAPEnvelope existingEnvelope = mock(SOAPEnvelope.class);
            SOAPBody body = mock(SOAPBody.class);
            when(axis2MsgCtx.getEnvelope()).thenReturn(existingEnvelope);
            when(existingEnvelope.getBody()).thenReturn(body);

            // Create OMElement with SOAP11 Envelope as first child
            OMFactory factory = OMAbstractFactory.getOMFactory();
            OMElement xmlPayload = factory.createOMElement(new QName("wrapper"));
            OMElement soap11Envelope = factory.createOMElement(new QName(SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI, "Envelope"));
            xmlPayload.addChild(soap11Envelope);

            // Mock AXIOMUtils to return a valid SOAPEnvelope
            SOAPEnvelope newSoapEnvelope = mock(SOAPEnvelope.class);
            axiomUtilsMock.when(() -> AXIOMUtils.getSOAPEnvFromOM(any(OMElement.class))).thenReturn(newSoapEnvelope);

            PayloadWriter.overwriteBody(synCtx, xmlPayload);

            // Verify envelope replacement occurred
            verify(newSoapEnvelope).buildWithAttachments();
            verify(synCtx).setEnvelope(newSoapEnvelope);
            // Body.addChild should NOT be called since we're replacing the envelope
            verify(body, never()).addChild(any(OMElement.class));
            verify(axis2MsgCtx).setProperty("messageType", "application/xml");
        }
    }

    // Test checkAndReplaceEnvelope with SOAP12 envelope
    @Test
    public void testOverwriteBody_XmlPayload_Soap12Envelope() throws AxisFault {
        try (MockedStatic<JsonUtil> jsonUtilMock = Mockito.mockStatic(JsonUtil.class);
             MockedStatic<AXIOMUtils> axiomUtilsMock = Mockito.mockStatic(AXIOMUtils.class)) {

            Axis2MessageContext synCtx = mock(Axis2MessageContext.class);
            org.apache.axis2.context.MessageContext axis2MsgCtx = mock(org.apache.axis2.context.MessageContext.class);
            when(synCtx.getAxis2MessageContext()).thenReturn(axis2MsgCtx);

            SOAPEnvelope existingEnvelope = mock(SOAPEnvelope.class);
            SOAPBody body = mock(SOAPBody.class);
            when(axis2MsgCtx.getEnvelope()).thenReturn(existingEnvelope);
            when(existingEnvelope.getBody()).thenReturn(body);

            // Create OMElement with SOAP12 Envelope as first child
            OMFactory factory = OMAbstractFactory.getOMFactory();
            OMElement xmlPayload = factory.createOMElement(new QName("wrapper"));
            OMElement soap12Envelope = factory.createOMElement(new QName(SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI, "Envelope"));
            xmlPayload.addChild(soap12Envelope);

            // Mock AXIOMUtils to return a valid SOAPEnvelope
            SOAPEnvelope newSoapEnvelope = mock(SOAPEnvelope.class);
            axiomUtilsMock.when(() -> AXIOMUtils.getSOAPEnvFromOM(any(OMElement.class))).thenReturn(newSoapEnvelope);

            PayloadWriter.overwriteBody(synCtx, xmlPayload);

            // Verify envelope replacement occurred
            verify(newSoapEnvelope).buildWithAttachments();
            verify(synCtx).setEnvelope(newSoapEnvelope);
            // Body.addChild should NOT be called since we're replacing the envelope
            verify(body, never()).addChild(any(OMElement.class));
            verify(axis2MsgCtx).setProperty("messageType", "application/xml");
        }
    }

    // Test checkAndReplaceEnvelope when AXIOMUtils returns null SOAPEnvelope
    @Test
    public void testOverwriteBody_XmlPayload_NullSoapEnvelope() throws AxisFault {
        try (MockedStatic<JsonUtil> jsonUtilMock = Mockito.mockStatic(JsonUtil.class);
             MockedStatic<AXIOMUtils> axiomUtilsMock = Mockito.mockStatic(AXIOMUtils.class)) {

            Axis2MessageContext synCtx = mock(Axis2MessageContext.class);
            org.apache.axis2.context.MessageContext axis2MsgCtx = mock(org.apache.axis2.context.MessageContext.class);
            when(synCtx.getAxis2MessageContext()).thenReturn(axis2MsgCtx);

            SOAPEnvelope existingEnvelope = mock(SOAPEnvelope.class);
            SOAPBody body = mock(SOAPBody.class);
            when(axis2MsgCtx.getEnvelope()).thenReturn(existingEnvelope);
            when(existingEnvelope.getBody()).thenReturn(body);

            // Create OMElement with SOAP11 Envelope as first child
            OMFactory factory = OMAbstractFactory.getOMFactory();
            OMElement xmlPayload = factory.createOMElement(new QName("wrapper"));
            OMElement soap11Envelope = factory.createOMElement(new QName(SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI, "Envelope"));
            xmlPayload.addChild(soap11Envelope);

            // Mock AXIOMUtils to return null SOAPEnvelope
            axiomUtilsMock.when(() -> AXIOMUtils.getSOAPEnvFromOM(any(OMElement.class))).thenReturn(null);

            PayloadWriter.overwriteBody(synCtx, xmlPayload);

            // Verify setEnvelope was NOT called since soapEnvelope was null
            verify(synCtx, never()).setEnvelope(any());
            verify(axis2MsgCtx).setProperty("messageType", "application/xml");
        }
    }

    // Test checkAndReplaceEnvelope with non-SOAP envelope element (return false)
    @Test
    public void testOverwriteBody_XmlPayload_NonSoapEnvelope() throws AxisFault {
        try (MockedStatic<JsonUtil> jsonUtilMock = Mockito.mockStatic(JsonUtil.class)) {
            Axis2MessageContext synCtx = mock(Axis2MessageContext.class);
            org.apache.axis2.context.MessageContext axis2MsgCtx = mock(org.apache.axis2.context.MessageContext.class);
            when(synCtx.getAxis2MessageContext()).thenReturn(axis2MsgCtx);

            SOAPEnvelope envelope = mock(SOAPEnvelope.class);
            SOAPBody body = mock(SOAPBody.class);
            when(axis2MsgCtx.getEnvelope()).thenReturn(envelope);
            when(envelope.getBody()).thenReturn(body);

            // Create OMElement with non-Envelope first child (different local name)
            OMFactory factory = OMAbstractFactory.getOMFactory();
            OMElement xmlPayload = factory.createOMElement(new QName("root"));
            OMElement child = factory.createOMElement(new QName("NotAnEnvelope"));
            child.setText("value");
            xmlPayload.addChild(child);

            PayloadWriter.overwriteBody(synCtx, xmlPayload);

            // Verify body.addChild was called since it's not a SOAP envelope
            verify(body).addChild(xmlPayload);
            verify(axis2MsgCtx).setProperty("messageType", "application/xml");
        }
    }

    // Test String payload with null content (tests getTextElement null branch)
    // Note: This is tricky because overwriteBody checks for null payload first
    // The getTextElement method is private and handles null internally
    // We need to pass a non-null String that results in null after internal processing
    // Since the method directly uses the String content, we test with empty string
    @Test
    public void testOverwriteBody_EmptyStringPayload() throws AxisFault {
        try (MockedStatic<JsonUtil> jsonUtilMock = Mockito.mockStatic(JsonUtil.class)) {
            Axis2MessageContext synCtx = mock(Axis2MessageContext.class);
            org.apache.axis2.context.MessageContext axis2MsgCtx = mock(org.apache.axis2.context.MessageContext.class);
            when(synCtx.getAxis2MessageContext()).thenReturn(axis2MsgCtx);

            SOAPEnvelope envelope = mock(SOAPEnvelope.class);
            SOAPBody body = mock(SOAPBody.class);
            when(axis2MsgCtx.getEnvelope()).thenReturn(envelope);
            when(envelope.getBody()).thenReturn(body);

            // Empty string payload
            String payload = "";
            PayloadWriter.overwriteBody(synCtx, payload);

            jsonUtilMock.verify(() -> JsonUtil.removeJsonPayload(axis2MsgCtx));
            verify(axis2MsgCtx).setProperty("messageType", "text/plain");
            verify(axis2MsgCtx).setProperty("contentType", "text/plain");
            verify(axis2MsgCtx).removeProperty("NO_ENTITY_BODY");
        }
    }

    // Test checkAndReplaceEnvelope with "Envelope" local name but wrong namespace
    @Test
    public void testOverwriteBody_XmlPayload_EnvelopeWrongNamespace() throws AxisFault {
        try (MockedStatic<JsonUtil> jsonUtilMock = Mockito.mockStatic(JsonUtil.class)) {
            Axis2MessageContext synCtx = mock(Axis2MessageContext.class);
            org.apache.axis2.context.MessageContext axis2MsgCtx = mock(org.apache.axis2.context.MessageContext.class);
            when(synCtx.getAxis2MessageContext()).thenReturn(axis2MsgCtx);

            SOAPEnvelope envelope = mock(SOAPEnvelope.class);
            SOAPBody body = mock(SOAPBody.class);
            when(axis2MsgCtx.getEnvelope()).thenReturn(envelope);
            when(envelope.getBody()).thenReturn(body);

            // Create OMElement with "Envelope" as local name but wrong namespace
            OMFactory factory = OMAbstractFactory.getOMFactory();
            OMElement xmlPayload = factory.createOMElement(new QName("root"));
            OMElement fakeEnvelope = factory.createOMElement(new QName("http://wrong.namespace.uri", "Envelope"));
            fakeEnvelope.setText("value");
            xmlPayload.addChild(fakeEnvelope);

            PayloadWriter.overwriteBody(synCtx, xmlPayload);

            // Verify body.addChild was called since namespace doesn't match SOAP11 or SOAP12
            verify(body).addChild(xmlPayload);
            verify(axis2MsgCtx).setProperty("messageType", "application/xml");
        }
    }
}
