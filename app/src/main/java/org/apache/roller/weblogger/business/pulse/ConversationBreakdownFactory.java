/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 */
package org.apache.roller.weblogger.business.pulse;

/**
 * Factory for obtaining a {@link ConversationBreakdownService} instance.
 *
 * Design pattern: Factory Method
 *   - Centralises the creation logic for breakdown strategies.
 *   - The calling code (Struts2 action) never constructs concrete service classes directly;
 *     it only calls {@code ConversationBreakdownFactory.getService(method)}.
 *   - Adding a new strategy requires only: (1) a new implementing class, and
 *     (2) a new case in {@link #getService(String)}.
 *
 * The method string is passed from the UI form, so the user can switch strategies
 * on demand without any server restart.
 */
public class ConversationBreakdownFactory {

    /** String constant used in the UI/form to select the lightweight strategy. */
    public static final String METHOD_LIGHTWEIGHT = "lightweight";

    /** String constant used in the UI/form to select the LLM strategy. */
    public static final String METHOD_LLM = "llm";

    // GroqCloud API key — stored here so only this factory manages it.
    // In production this should come from roller.properties or an env variable.
    private static final String GroqCloud_API_KEY =
        System.getProperty("groqcloud.api.key",
            System.getenv("GROQCloud_API_KEY") != null
                ? System.getenv("GROQCloud_API_KEY")
                : "");

    /**
     * Returns the appropriate service for the given method string.
     *
     * @param method One of {@link #METHOD_LIGHTWEIGHT} or {@link #METHOD_LLM}.
     *               Defaults to lightweight if null or unrecognised.
     * @return A ready-to-use {@link ConversationBreakdownService}
     */
    public static ConversationBreakdownService getService(String method) {
        if (METHOD_LLM.equalsIgnoreCase(method)) {
            return new LLMBreakdownService(GroqCloud_API_KEY);
        }
        // Default: lightweight — safe, fast, free
        return new LightweightBreakdownService();
    }

    private ConversationBreakdownFactory() {
        // Utility class — no instantiation
    }
}