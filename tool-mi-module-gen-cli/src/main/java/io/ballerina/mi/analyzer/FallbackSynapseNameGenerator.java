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
 * software distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.mi.analyzer;

import io.ballerina.compiler.api.symbols.FunctionSymbol;
import io.ballerina.mi.model.FunctionType;
import io.ballerina.mi.util.Utils;

import java.util.Optional;

/**
 * Fallback synapse name generator that uses heuristics from parameter/return types.
 * This has the lowest priority (3) and is used when other methods fail.
 */
public class FallbackSynapseNameGenerator implements SynapseNameGenerator {
    
    @Override
    public int getPriority() {
        return 3; // Lowest priority - fallback
    }
    
    @Override
    public Optional<String> generate(FunctionSymbol functionSymbol, FunctionType functionType, SynapseNameContext context) {
        // Use the existing Utils.generateSynapseName as fallback
        String name = Utils.generateSynapseName(functionSymbol, functionType);
        return Optional.of(name);
    }
}
