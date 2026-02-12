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

import java.util.Optional;

/**
 * Interface for generating synapse names for Ballerina functions.
 * Implementations should have different priority levels where lower numbers indicate higher priority.
 */
public interface SynapseNameGenerator {
    
    /**
     * Get the priority level of this generator. Lower numbers indicate higher priority.
     * Generators are tried in ascending order of priority.
     * 
     * @return priority level (1 = highest priority)
     */
    int getPriority();
    
    /**
     * Attempt to generate a synapse name for the given function symbol.
     * 
     * @param functionSymbol The function symbol to generate a name for
     * @param functionType The type of function (RESOURCE, REMOTE, FUNCTION, etc.)
     * @param context Context object containing additional information needed for name generation
     * @return Optional containing the generated name if successful, empty otherwise
     */
    Optional<String> generate(FunctionSymbol functionSymbol, FunctionType functionType, SynapseNameContext context);
}
