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

import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.runtime.api.values.BXml;
import io.ballerina.runtime.api.values.BXmlItem;
import io.ballerina.runtime.internal.values.XmlSequence;
import org.apache.axiom.om.OMElement;
import org.apache.commons.lang3.tuple.Pair;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.xml.namespace.QName;
import java.util.AbstractMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BXmlConverterTest {

    @Test
    public void testExtractNamespace_WithNamespacedValue() {
        Pair<String, String> result = BXmlConverter.extractNamespace("{urn:test}name");
        Assert.assertEquals(result.getLeft(), "urn:test");
        Assert.assertEquals(result.getRight(), "name");
    }

    @Test
    public void testExtractNamespace_WithoutNamespace() {
        Pair<String, String> result = BXmlConverter.extractNamespace("plainName");
        Assert.assertEquals(result.getLeft(), "");
        Assert.assertEquals(result.getRight(), "plainName");
    }

    @Test
    public void testToOMElement_UnsupportedBXmlType_ReturnsNull() {
        BXml unsupported = mock(BXml.class);
        Assert.assertNull(BXmlConverter.toOMElement(unsupported));
    }

    @Test
    public void testToOMElement_EmptyXmlSequence_ReturnsNull() {
        XmlSequence sequence = mock(XmlSequence.class);
        when(sequence.isEmpty()).thenReturn(true);

        Assert.assertNull(BXmlConverter.toOMElement(sequence));
    }

    @Test
    public void testToOMElement_WithNamespaceAndAttribute() {
        BXmlItem xmlItem = mock(BXmlItem.class);
        when(xmlItem.getQName()).thenReturn(new QName("urn:root", "root", "r"));

        BMap<BString, BString> attributes = mock(BMap.class);
        BString nsDeclKey = BXmlItem.XMLNS_PREFIX;
        BString nsDeclVal = StringUtils.fromString("urn:attr");
        BString attrKey = StringUtils.fromString("{urn:attr}id");
        BString attrVal = StringUtils.fromString("123");

        Set<Map.Entry<BString, BString>> entrySet = new LinkedHashSet<>();
        entrySet.add(new AbstractMap.SimpleEntry<>(nsDeclKey, nsDeclVal));
        entrySet.add(new AbstractMap.SimpleEntry<>(attrKey, attrVal));
        when(attributes.entrySet()).thenReturn(entrySet);
        when(xmlItem.getAttributesMap()).thenReturn(attributes);

        BXml children = mock(BXml.class);
        when(children.size()).thenReturn(0);
        when(xmlItem.children()).thenReturn(children);

        OMElement omElement = BXmlConverter.toOMElement(xmlItem);

        Assert.assertNotNull(omElement);
        Assert.assertEquals(omElement.getLocalName(), "root");
        Assert.assertEquals(omElement.getNamespace().getNamespaceURI(), "urn:root");
        Assert.assertNotNull(omElement.getAttributeValue(new QName("urn:attr", "id")));
    }
}
