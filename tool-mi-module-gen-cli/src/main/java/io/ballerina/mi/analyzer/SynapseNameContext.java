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

import io.ballerina.compiler.api.symbols.resourcepath.util.PathSegment;
import io.ballerina.projects.Module;

import java.util.List;
import java.util.Optional;

/**
 * Context object that provides additional information needed for synapse name generation.
 */
public class SynapseNameContext {
    private final Optional<String> operationId;
    private final Optional<List<PathSegment>> resourcePathSegments;
    private final Optional<Module> module;
    
    private SynapseNameContext(Builder builder) {
        this.operationId = builder.operationId;
        this.resourcePathSegments = builder.resourcePathSegments;
        this.module = builder.module;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public Optional<String> getOperationId() {
        return operationId;
    }
    
    public Optional<List<PathSegment>> getResourcePathSegments() {
        return resourcePathSegments;
    }
    
    public Optional<Module> getModule() {
        return module;
    }
    
    public static class Builder {
        private Optional<String> operationId = Optional.empty();
        private Optional<List<PathSegment>> resourcePathSegments = Optional.empty();
        private Optional<Module> module = Optional.empty();
        
        public Builder operationId(String operationId) {
            this.operationId = Optional.ofNullable(operationId);
            return this;
        }
        
        public Builder resourcePathSegments(List<PathSegment> segments) {
            this.resourcePathSegments = Optional.ofNullable(segments);
            return this;
        }
        
        public Builder module(Module module) {
            this.module = Optional.ofNullable(module);
            return this;
        }
        
        public SynapseNameContext build() {
            return new SynapseNameContext(this);
        }
    }
}
