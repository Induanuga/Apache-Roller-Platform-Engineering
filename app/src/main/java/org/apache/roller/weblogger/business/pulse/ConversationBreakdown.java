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

import java.util.List;

/**
 * POJO representing the full conversation breakdown result for a blog entry's comments.
 *
 * Holds:
 *   - a list of major themes, each with a label and representative comment excerpts
 *   - a short overall recap of the conversation
 *
 * This is a plain data holder — no logic — passed from the service layer to the view.
 */
public class ConversationBreakdown {

    private final List<CommentTheme> themes;
    private final String overallRecap;
    private final String methodUsed;

    public ConversationBreakdown(List<CommentTheme> themes, String overallRecap, String methodUsed) {
        this.themes = themes;
        this.overallRecap = overallRecap;
        this.methodUsed = methodUsed;
    }

    public List<CommentTheme> getThemes() {
        return themes;
    }

    public String getOverallRecap() {
        return overallRecap;
    }

    /** Label identifying which method produced this result (e.g., "Lightweight" or "LLM"). */
    public String getMethodUsed() {
        return methodUsed;
    }

    // -------------------------------------------------------------------------

    /**
     * Represents a single major theme discovered in the comment section.
     * Contains a human-readable label and a small list of representative comment excerpts.
     */
    public static class CommentTheme {
        private final String themeLabel;
        private final List<String> representativeComments;

        public CommentTheme(String themeLabel, List<String> representativeComments) {
            this.themeLabel = themeLabel;
            this.representativeComments = representativeComments;
        }

        public String getThemeLabel() {
            return themeLabel;
        }

        public List<String> getRepresentativeComments() {
            return representativeComments;
        }
    }
}