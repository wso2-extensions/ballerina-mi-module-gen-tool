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
 * Synapse name generator that uses operationId from @openapi:ResourceInfo annotation.
 * This has the highest priority (1) as it uses explicit user-provided operation IDs.
 */
public class OperationIdSynapseNameGenerator implements SynapseNameGenerator {
    
    @Override
    public int getPriority() {
        return 1; // Highest priority
    }
    
    @Override
    public Optional<String> generate(FunctionSymbol functionSymbol, FunctionType functionType, SynapseNameContext context) {
        Optional<String> operationIdOpt = context.getOperationId();
        if (operationIdOpt.isEmpty() || operationIdOpt.get().isEmpty()) {
            return Optional.empty();
        }
        
        String operationId = operationIdOpt.get();
        // Use operationId as-is without any modifications or splitting
        // OperationIds from OpenAPI specs are typically valid and should be preserved exactly
        return Optional.of(operationId);
    }
}
