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
 * software distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.mi.analyzer;

import io.ballerina.compiler.api.symbols.FunctionSymbol;
import io.ballerina.mi.model.FunctionType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Manages multiple synapse name generators and tries them in priority order.
 * Higher priority generators (lower priority numbers) are tried first.
 */
public class SynapseNameGeneratorManager {
    
    private final List<SynapseNameGenerator> generators;
    
    public SynapseNameGeneratorManager() {
        this.generators = new ArrayList<>();
        // Register generators in order of priority (will be sorted by priority)
        generators.add(new OperationIdSynapseNameGenerator());
        generators.add(new PathSegmentSynapseNameGenerator());
        generators.add(new FallbackSynapseNameGenerator());
        
        // Sort by priority (ascending - lower numbers first)
        generators.sort(Comparator.comparingInt(SynapseNameGenerator::getPriority));
    }
    
    /**
     * Generate a synapse name using the registered generators in priority order.
     * Returns the first successful result.
     * 
     * @param functionSymbol The function symbol to generate a name for
     * @param functionType The type of function
     * @param context The context containing additional information
     * @return The generated synapse name, or empty if no generator succeeded
     */
    public Optional<String> generateSynapseName(FunctionSymbol functionSymbol, 
                                                 FunctionType functionType, 
                                                 SynapseNameContext context) {
        for (SynapseNameGenerator generator : generators) {
            Optional<String> result = generator.generate(functionSymbol, functionType, context);
            if (result.isPresent() && !result.get().isEmpty()) {
                return result;
            }
        }
        return Optional.empty();
    }
}
