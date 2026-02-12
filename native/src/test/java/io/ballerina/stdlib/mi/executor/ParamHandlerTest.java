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

package io.ballerina.stdlib.mi.executor;

import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.stdlib.mi.Constants;
import io.ballerina.stdlib.mi.utils.SynapseUtils;
import org.apache.synapse.MessageContext;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ParamHandlerTest {

    @Test
    public void testSetParameters() {
        try (MockedStatic<SynapseUtils> synapseUtilsMock = Mockito.mockStatic(SynapseUtils.class);
             MockedStatic<StringUtils> stringUtilsMock = Mockito.mockStatic(StringUtils.class)) {
             
            MessageContext context = mock(MessageContext.class);
            Object[] args = new Object[2];
            
            // Mock param0 (String)
            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "param0")).thenReturn("p0Name");
            synapseUtilsMock.when(() -> SynapseUtils.lookupTemplateParameter(context, "p0Name")).thenReturn("value0");
            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "paramType0")).thenReturn(Constants.STRING);
            
            BString bStringVal = mock(BString.class);
            stringUtilsMock.when(() -> StringUtils.fromString("value0")).thenReturn(bStringVal);

            // Mock param1 (Int)
            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "param1")).thenReturn("p1Name");
            synapseUtilsMock.when(() -> SynapseUtils.lookupTemplateParameter(context, "p1Name")).thenReturn("123");
            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "paramType1")).thenReturn(Constants.INT);

            ParamHandler handler = new ParamHandler();
            handler.setParameters(args, context);

            Assert.assertEquals(args[0], bStringVal);
            Assert.assertEquals(args[1], 123L);
        }
    }
    
    @Test
    public void testGetParameterTypes() {
       try (MockedStatic<SynapseUtils> synapseUtilsMock = Mockito.mockStatic(SynapseUtils.class);
            MockedStatic<StringUtils> stringUtilsMock = Mockito.mockStatic(StringUtils.class);
            MockedStatic<DataTransformer> dataTransformerMock = Mockito.mockStatic(DataTransformer.class)) {
            
            MessageContext context = mock(MessageContext.class);
            ParamHandler handler = new ParamHandler();

            // Test BOOLEAN
            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "paramBool")).thenReturn("boolName");
            synapseUtilsMock.when(() -> SynapseUtils.lookupTemplateParameter(context, "boolName")).thenReturn("true");
            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "typeBool")).thenReturn(Constants.BOOLEAN);
            
            Object resultBool = handler.getParameter(context, "paramBool", "typeBool", 0);
            Assert.assertEquals(resultBool, true);
            
            // Test FLOAT
            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "paramFloat")).thenReturn("floatName");
            synapseUtilsMock.when(() -> SynapseUtils.lookupTemplateParameter(context, "floatName")).thenReturn("10.5");
            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "typeFloat")).thenReturn(Constants.FLOAT);
            
            Object resultFloat = handler.getParameter(context, "paramFloat", "typeFloat", 0);
            Assert.assertEquals(resultFloat, 10.5);

            // Test JSON (Delegates to DataTransformer)
            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "paramJson")).thenReturn("jsonName");
            synapseUtilsMock.when(() -> SynapseUtils.lookupTemplateParameter(context, "jsonName")).thenReturn("{}");
            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "typeJson")).thenReturn(Constants.JSON);
            
            Object mockJson = new Object();
            dataTransformerMock.when(() -> DataTransformer.getJsonParameter(any())).thenReturn(mockJson);
            
            Object resultJson = handler.getParameter(context, "paramJson", "typeJson", 0);
            Assert.assertSame(resultJson, mockJson);
       }
    }

    @Test
    public void testPrependPathParams() {
        try (MockedStatic<SynapseUtils> synapseUtilsMock = Mockito.mockStatic(SynapseUtils.class);
             MockedStatic<StringUtils> stringUtilsMock = Mockito.mockStatic(StringUtils.class)) {
            
            MessageContext context = mock(MessageContext.class);
            ParamHandler handler = new ParamHandler();
            Object[] args = new Object[] { "arg1" };
            
            // PATH_PARAM_SIZE = 2
            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, Constants.PATH_PARAM_SIZE)).thenReturn("2");
            
            // Path Param 0: int 100
            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "pathParam0")).thenReturn("p0");
            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "pathParamType0")).thenReturn(Constants.INT);
            synapseUtilsMock.when(() -> SynapseUtils.lookupTemplateParameter(context, "p0")).thenReturn("100");
            
            // Path Param 1: string "hello"
            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "pathParam1")).thenReturn("p1");
            synapseUtilsMock.when(() -> SynapseUtils.getPropertyAsString(context, "pathParamType1")).thenReturn(Constants.STRING);
            synapseUtilsMock.when(() -> SynapseUtils.lookupTemplateParameter(context, "p1")).thenReturn("hello");
            BString helloBStr = mock(BString.class);
            stringUtilsMock.when(() -> StringUtils.fromString("hello")).thenReturn(helloBStr);

            Object[] combined = handler.prependPathParams(args, context);
            
            Assert.assertEquals(combined.length, 3);
            Assert.assertEquals(combined[0], 100L);
            Assert.assertEquals(combined[1], helloBStr);
            Assert.assertEquals(combined[2], "arg1");
        }
    }
}
