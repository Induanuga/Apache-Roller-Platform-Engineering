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

import java.util.*;
import java.util.stream.Collectors;

/**
 * Computes recurring keywords using token frequency analysis.
 * Removes stop-words and returns the top-5 most frequent words.
 */
public class KeywordFrequencyIndicator implements DiscussionIndicator {

    private static final Set<String> STOP_WORDS = Set.of(
        "the", "a", "an", "and", "or", "but", "is", "are", "was", "were", "be", "been",
        "being", "have", "has", "had", "do", "does", "did", "will", "would", "should",
        "could", "can", "may", "might", "must", "shall", "of", "at", "by", "for", "with",
        "about", "against", "between", "into", "through", "during", "before", "after",
        "above", "below", "to", "from", "up", "down", "in", "out", "on", "off", "over",
        "under", "again", "further", "then", "once", "here", "there", "when", "where",
        "why", "how", "all", "both", "each", "few", "more", "most", "other", "some",
        "such", "no", "nor", "not", "only", "own", "same", "so", "than", "too", "very",
        "just", "now", "this", "that", "these", "those", "am", "it", "its", "as"
    );

    @Override
    public String getLabel() {
        return "Top Keywords";
    }

    @Override
    public String compute(List<WeblogEntryComment> comments) {
        if (comments == null || comments.isEmpty()) {
            return "No comments to analyze";
        }

        Map<String, Integer> wordFrequency = new HashMap<>();

        for (WeblogEntryComment comment : comments) {
            String content = comment.getContent();
            if (content == null) {
                continue;
            }

            // Tokenize: split on non-letter characters
            String[] tokens = content.toLowerCase().split("[^a-z]+");

            for (String token : tokens) {
                // Filter: must be at least 3 chars and not a stop-word
                if (token.length() >= 3 && !STOP_WORDS.contains(token)) {
                    wordFrequency.put(token, wordFrequency.getOrDefault(token, 0) + 1);
                }
            }
        }

        if (wordFrequency.isEmpty()) {
            return "No significant keywords found";
        }

        // Get top 5 by frequency
        String topKeywords = wordFrequency.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(5)
            .map(e -> e.getKey() + " (" + e.getValue() + ")")
            .collect(Collectors.joining(" | "));

        return topKeywords;
    }
}
