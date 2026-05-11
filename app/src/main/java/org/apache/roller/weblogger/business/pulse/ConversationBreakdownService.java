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

import org.apache.roller.weblogger.pojos.WeblogEntryComment;

import java.util.List;

/**
 * Strategy interface for generating a conversation breakdown from a list of comments.
 *
 * Design pattern: Strategy
 *   - Decouples "how to generate a breakdown" from the action/controller that requests it.
 *   - New methods (e.g., a local open-source model, a different LLM provider) can be
 *     plugged in by implementing this interface without touching any other class.
 *
 * Two concrete implementations are provided:
 *   1. {@link LightweightBreakdownService}  — classical TF-IDF + keyword clustering, no API calls
 *   2. {@link LLMBreakdownService}           — delegates to GroqCloud AI for richer results
 *
 * The active strategy is selected at request time via {@link ConversationBreakdownFactory}.
 */
public interface ConversationBreakdownService {

    /**
     * Generates a structured conversation breakdown from the given comment list.
     *
     * @param comments Approved comments for a single weblog entry
     * @return A {@link ConversationBreakdown} containing themes, representative comments,
     *         and an overall recap
     */
    ConversationBreakdown generate(List<WeblogEntryComment> comments);

    /**
     * Human-readable name identifying this strategy (shown in the UI).
     */
    String getMethodName();
}