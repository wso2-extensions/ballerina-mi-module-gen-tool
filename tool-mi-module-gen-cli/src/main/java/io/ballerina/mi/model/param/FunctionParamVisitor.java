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

package io.ballerina.mi.model.param;

/**
 * Visitor interface for traversing {@link FunctionParam} hierarchies.
 * <p>
 * This follows the Visitor Pattern to enable operations on the FunctionParam
 * type hierarchy without modifying the param classes. It eliminates the need
 * for {@code instanceof} checks and type-casting scattered across generator
 * and serializer code.
 * </p>
 * <p>
 * Each concrete FunctionParam subclass calls the corresponding visit method,
 * allowing type-safe dispatch based on the actual runtime type.
 * </p>
 *
 * @since 0.6.0
 */
public interface FunctionParamVisitor {

    /**
     * Visits a basic (non-composite) function parameter.
     *
     * @param param The function parameter
     */
    void visit(FunctionParam param);

    /**
     * Visits a record-typed function parameter with nested fields.
     *
     * @param param The record function parameter
     */
    void visit(RecordFunctionParam param);

    /**
     * Visits a union-typed function parameter with member types.
     *
     * @param param The union function parameter
     */
    void visit(UnionFunctionParam param);

    /**
     * Visits an array-typed function parameter.
     *
     * @param param The array function parameter
     */
    void visit(ArrayFunctionParam param);

    /**
     * Visits a map-typed function parameter.
     *
     * @param param The map function parameter
     */
    void visit(MapFunctionParam param);
}
