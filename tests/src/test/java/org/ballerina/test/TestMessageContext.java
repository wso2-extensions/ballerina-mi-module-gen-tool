/*
 * Copyright (c) 2025, WSO2 LLC. (http://wso2.com)
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

import org.apache.axis2.context.MessageContext;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2MessageContext;

public class TestMessageContext extends Axis2MessageContext {

    /**
     * Constructor for the Axis2MessageContext inside Synapse
     *
     * @param axisMsgCtx MessageContext representing the relevant Axis MC
     * @param synCfg     SynapseConfiguration describing Synapse
     * @param synEnv     SynapseEnvironment describing the environment of Synapse
     */
    public TestMessageContext(MessageContext axisMsgCtx, SynapseConfiguration synCfg, SynapseEnvironment synEnv) {
        super(axisMsgCtx, synCfg, synEnv);
    }

    public TestMessageContext() {
        super(new MessageContext(), null, null);
    }
}
