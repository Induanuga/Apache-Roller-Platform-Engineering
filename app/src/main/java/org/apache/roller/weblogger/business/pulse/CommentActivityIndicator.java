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

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

/**
 * Computes comment activity level using basic statistics.
 * Calculates total count, average comments per day, and classifies activity as Low/Moderate/High.
 */
public class CommentActivityIndicator implements DiscussionIndicator {

    @Override
    public String getLabel() {
        return "Activity Level";
    }

    @Override
    public String compute(List<WeblogEntryComment> comments) {
        if (comments == null || comments.isEmpty()) {
            return "No comments";
        }

        int total = comments.size();
        
        // Calculate time span from first to last comment
        Timestamp firstTime = comments.get(0).getPostTime();
        Timestamp lastTime = comments.get(comments.size() - 1).getPostTime();
        
        long daysBetween = java.time.Duration.between(firstTime.toInstant(), lastTime.toInstant()).toDays();
        if (daysBetween == 0) {
            daysBetween = 1;  // Avoid division by zero; all comments on same day
        }
        
        double avgPerDay = (double) total / daysBetween;
        
        // Classify activity level
        String level;
        if (avgPerDay < 1.0) {
            level = "Low";
        } else if (avgPerDay <= 5.0) {
            level = "Moderate";
        } else {
            level = "High";
        }
        
        return String.format("%s (%d comments, %.1f per day)", level, total, avgPerDay);
    }
}
