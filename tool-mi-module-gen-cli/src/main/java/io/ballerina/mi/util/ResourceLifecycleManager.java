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

package io.ballerina.mi.util;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Deterministic resource lifecycle manager using a stack-based (LIFO) cleanup strategy.
 * <p>
 * Instead of relying on non-deterministic {@code System.gc()} hints (which the JVM may
 * ignore) or the deprecated {@code System.runFinalization()}, this class uses an
 * {@link ArrayDeque} as a LIFO stack to register cleanup actions that are executed
 * deterministically when {@link #cleanup()} is called.
 * </p>
 * <h3>How it works</h3>
 * <ul>
 *   <li>Components register cleanup lambdas via {@link #register(Runnable)} as they
 *       complete processing (e.g., "clear FunctionParam list after XML is written").</li>
 *   <li>When a phase boundary is reached, {@link #cleanup()} pops and executes all
 *       registered actions in LIFO order, ensuring child resources are freed before parents.</li>
 *   <li>After cleanup, references are nullified and become immediately eligible for GC
 *       on the next natural collection cycle — no explicit GC hints needed.</li>
 * </ul>
 * <h3>Example</h3>
 * <pre>{@code
 * ResourceLifecycleManager lifecycle = new ResourceLifecycleManager();
 *
 * // Phase 1: Generate component files
 * for (Component c : components) {
 *     generateFiles(c);
 *     lifecycle.register(() -> c.getFunctionParams().clear());
 * }
 * // Execute all cleanups deterministically
 * lifecycle.cleanup();
 *
 * // Phase 2: Generate connector-level files
 * generateConnectorFiles(connector);
 * lifecycle.register(() -> connector.clearComponentData());
 * lifecycle.cleanup();
 * }</pre>
 *
 * @since 0.6.0
 */
public class ResourceLifecycleManager {

    private final Deque<Runnable> cleanupStack = new ArrayDeque<>();

    /**
     * Registers a cleanup action to be executed on the next {@link #cleanup()} call.
     * Actions are stored in a stack (LIFO), so the last registered action runs first.
     *
     * @param action The cleanup action (e.g., clearing a list, nullifying a reference)
     */
    public void register(Runnable action) {
        cleanupStack.push(action);
    }

    /**
     * Executes all registered cleanup actions in LIFO order and clears the stack.
     * <p>
     * This method is deterministic: after it returns, all registered resources
     * have been cleaned up and their references are eligible for GC.
     * </p>
     */
    public void cleanup() {
        while (!cleanupStack.isEmpty()) {
            cleanupStack.pop().run();
        }
    }

    /**
     * Returns the number of pending cleanup actions.
     *
     * @return The number of registered but unexecuted cleanup actions
     */
    public int pendingCount() {
        return cleanupStack.size();
    }

    /**
     * Returns true if there are no pending cleanup actions.
     *
     * @return true if no cleanup actions are registered
     */
    public boolean isEmpty() {
        return cleanupStack.isEmpty();
    }
}
