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
import io.ballerina.compiler.api.symbols.resourcepath.util.PathSegment;
import io.ballerina.mi.model.FunctionType;
import io.ballerina.mi.util.Utils;

import java.util.List;
import java.util.Optional;

/**
 * Synapse name generator that uses resource path segments.
 * This has priority 2, used when operationId is not available.
 */
public class PathSegmentSynapseNameGenerator implements SynapseNameGenerator {
    
    @Override
    public int getPriority() {
        return 2; // Second priority
    }
    
    @Override
    public Optional<String> generate(FunctionSymbol functionSymbol, FunctionType functionType, SynapseNameContext context) {
        // Only works for resource functions
        if (functionType != FunctionType.RESOURCE) {
            return Optional.empty();
        }
        
        Optional<List<PathSegment>> pathSegmentsOpt = context.getResourcePathSegments();
        if (pathSegmentsOpt.isEmpty() || pathSegmentsOpt.get().isEmpty()) {
            return Optional.empty();
        }
        
        List<PathSegment> pathSegments = pathSegmentsOpt.get();
        String httpMethod = functionSymbol.getName().orElse("resource").toLowerCase();
        
        List<String> literalSegments = new java.util.ArrayList<>();
        List<String> pathParamNames = new java.util.ArrayList<>();
        
        for (PathSegment segment : pathSegments) {
            String sig = segment.signature();
            if (sig == null || sig.isEmpty()) {
                continue;
            }
            
            if (sig.startsWith("[") && sig.endsWith("]")) {
                // Path parameter segment
                String inside = sig.substring(1, sig.length() - 1).trim();
                String paramName = inside;
                int lastSpace = inside.lastIndexOf(' ');
                if (lastSpace >= 0 && lastSpace + 1 < inside.length()) {
                    paramName = inside.substring(lastSpace + 1);
                }
                if (!paramName.isEmpty()) {
                    pathParamNames.add(paramName);
                }
            } else {
                // Literal path segment
                literalSegments.add(sig);
            }
        }
        
        StringBuilder nameBuilder = new StringBuilder();
        nameBuilder.append(httpMethod);
        
        // Add PascalCase literal segments
        for (String lit : literalSegments) {
            nameBuilder.append(toPascalCaseSegment(lit));
        }
        
        // Add By<ParamName> for each path parameter
        for (String paramName : pathParamNames) {
            if (paramName.isEmpty()) {
                continue;
            }
            nameBuilder.append("By");
            nameBuilder.append(Character.toUpperCase(paramName.charAt(0)));
            if (paramName.length() > 1) {
                nameBuilder.append(paramName.substring(1));
            }
        }
        
        return Optional.of(Utils.sanitizeXmlName(nameBuilder.toString()));
    }
    
    /**
     * Convert a path segment (e.g. "users", "user-threads") to PascalCase ("Users", "UserThreads").
     */
    private static String toPascalCaseSegment(String segment) {
        if (segment == null || segment.isEmpty()) {
            return "";
        }
        String[] parts = segment.split("[-_\\s]+");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            sb.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                sb.append(part.substring(1).toLowerCase());
            }
        }
        return sb.toString();
    }
}
