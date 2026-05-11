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
 * Strategy interface for discussion indicators.
 * Each indicator computes a specific metric from a list of comments.
 * 
 * Allows adding/removing indicators without modifying the orchestrating class.
 */
public interface DiscussionIndicator {
    /**
     * @return Human-readable label for this indicator (e.g., "Comment Activity")
     */
    String getLabel();

    /**
     * Computes the indicator value from the given list of comments.
     * 
     * @param comments The list of approved comments to analyze
     * @return A string representation of the computed indicator value
     */
    String compute(List<WeblogEntryComment> comments);
}
