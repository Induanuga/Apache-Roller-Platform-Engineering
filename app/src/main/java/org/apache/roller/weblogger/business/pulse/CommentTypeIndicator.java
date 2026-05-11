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
import java.util.regex.Pattern;

/**
 * Classifies comment types using keyword/regex matching.
 * Categories: Questions, Feedback/Appreciation, Debate, Other.
 */
public class CommentTypeIndicator implements DiscussionIndicator {

    private static final Pattern DEBATE_PATTERN = Pattern.compile(
        "\\b(disagree|wrong|incorrect|however|but|actually|contrary|opposed)\\b",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern FEEDBACK_PATTERN = Pattern.compile(
        "\\b(thanks|thank you|great|awesome|helpful|love|excellent|appreciate|good)\\b",
        Pattern.CASE_INSENSITIVE
    );

    @Override
    public String getLabel() {
        return "Response Types";
    }

    @Override
    public String compute(List<WeblogEntryComment> comments) {
        if (comments == null || comments.isEmpty()) {
            return "No comments to classify";
        }

        int questions = 0;
        int feedback = 0;
        int debate = 0;
        int other = 0;

        for (WeblogEntryComment comment : comments) {
            String content = comment.getContent();
            if (content == null) {
                other++;
                continue;
            }

            boolean classified = false;
            
            if (content.contains("?")) {
                questions++;
                classified = true;
            }
            
            if (FEEDBACK_PATTERN.matcher(content).find()) {
                feedback++;
                classified = true;
            }
            
            if (DEBATE_PATTERN.matcher(content).find()) {
                debate++;
                classified = true;
            }
            
            if (!classified) {
                other++;
            }
        }

        return String.format("Questions: %d | Feedback/Appreciation: %d | Debate: %d | Other: %d",
            questions, feedback, debate, other);
    }
}
