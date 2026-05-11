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

import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates multiple discussion indicators.
 * Uses the Strategy pattern to apply each registered indicator to a list of comments.
 * 
 * This class implements a lightweight Template Method within analyze():
 * the skeleton (iterate indicators, collect results) is fixed;
 * the variation comes from each indicator's compute() implementation.
 */
public class DiscussionAnalyzer {

    private final List<DiscussionIndicator> indicators;

    /**
     * Constructor initializes the list of indicators to be used in analysis.
     */
    public DiscussionAnalyzer() {
        this.indicators = List.of(
            new CommentActivityIndicator(),
            new CommentTypeIndicator(),
            new KeywordFrequencyIndicator()
        );
    }

    /**
     * Analyzes a list of comments using all registered indicators.
     * 
     * @param comments The list of comments to analyze
     * @return A list of DiscussionResult objects, one per indicator
     */
    public List<DiscussionResult> analyze(List<WeblogEntryComment> comments) {
        List<DiscussionResult> results = new ArrayList<>();
        
        for (DiscussionIndicator indicator : indicators) {
            String label = indicator.getLabel();
            String value = indicator.compute(comments);
            results.add(new DiscussionResult(label, value));
        }
        
        return results;
    }
}
