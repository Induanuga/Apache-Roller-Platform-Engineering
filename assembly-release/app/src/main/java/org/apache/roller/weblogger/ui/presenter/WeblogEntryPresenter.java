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
 * limitations under the License.
 */

package org.apache.roller.weblogger.ui.presenter;

import org.apache.commons.lang3.StringUtils;
import org.apache.roller.util.DateUtil;
import org.apache.roller.weblogger.config.WebloggerConfig;
import org.apache.roller.weblogger.pojos.WeblogEntry;
import org.apache.roller.weblogger.util.HTMLSanitizer;
import org.apache.roller.weblogger.util.I18nMessages;
import org.apache.roller.weblogger.util.Utilities;

import java.util.List;
import java.util.StringTokenizer;

/**
 * Presenter for WeblogEntry - handles all presentation/display logic.
 * Extracted from WeblogEntry POJO to separate presentation concerns.
 */
public class WeblogEntryPresenter {
    
    private static final char TITLE_SEPARATOR =
        WebloggerConfig.getBooleanProperty("weblogentry.title.useUnderscoreSeparator") ? '_' : '-';
    
    private final WeblogEntry entry;
    
    public WeblogEntryPresenter(WeblogEntry entry) {
        this.entry = entry;
    }
    
    /**
     * Create anchor for weblog entry, based on title or text.
     * MOVED FROM: WeblogEntry.createAnchorBase() (lines 805-839)
     */
    public String createAnchorBase() {
        
        // Use title (minus non-alphanumeric characters)
        String base = null;
        if (!StringUtils.isEmpty(entry.getTitle())) {
            base = Utilities.replaceNonAlphanumeric(entry.getTitle(), ' ').trim();    
        }
        
        // If we still have no base, then try text (minus non-alphanumerics)
        if (StringUtils.isEmpty(base) && !StringUtils.isEmpty(entry.getText())) {
            base = Utilities.replaceNonAlphanumeric(entry.getText(), ' ').trim();  
        }
        
        if (!StringUtils.isEmpty(base)) {
            
            // Use only the first 4 words
            StringTokenizer toker = new StringTokenizer(base);
            String tmp = null;
            int count = 0;
            while (toker.hasMoreTokens() && count < 5) {
                String s = toker.nextToken();
                s = s.toLowerCase();
                tmp = (tmp == null) ? s : tmp + TITLE_SEPARATOR + s;
                count++;
            }
            base = tmp;
        }
        
        // No title or text, so instead we will use the items date
        // in YYYYMMDD format as the base anchor
        else {
            base = DateUtil.format8chars(entry.getPubTime());
        }
        
        return base;
    }
    
    /**
     * Get the right transformed display content depending on the situation.
     * MOVED FROM: WeblogEntry.displayContent() (lines 980-1012)
     *
     * If the readMoreLink is specified then we assume the caller wants to
     * prefer summary over content and we include a "Read More" link at the
     * end of the summary if it exists. Otherwise, if the readMoreLink is
     * empty or null then we assume the caller prefers content over summary.
     */
    public String displayContent(String readMoreLink) {
        
        String displayContent;
        
        if (readMoreLink == null || readMoreLink.isBlank() || "nil".equals(readMoreLink)) {
            
            // no readMore link means permalink, so prefer text over summary
            if (StringUtils.isNotEmpty(entry.getText())) {
                displayContent = entry.getTransformedText();
            } else {
                displayContent = entry.getTransformedSummary();
            }
            
        } else {
            // not a permalink, so prefer summary over text
            // include a "read more" link if needed
            if (StringUtils.isNotEmpty(entry.getSummary())) {
                displayContent = entry.getTransformedSummary();
                
                if (StringUtils.isNotEmpty(entry.getText())) {
                    // add read more
                    List<String> args = List.of(readMoreLink);
                    
                    // TODO: we need a more appropriate way to get the view locale here
                    String readMore = I18nMessages.getMessages(
                        entry.getWebsite().getLocaleInstance()
                    ).getString("macro.weblog.readMoreLink", args);
                    
                    displayContent += readMore;
                }
            } else {
                displayContent = entry.getTransformedText();
            }
        }
        
        return HTMLSanitizer.conditionallySanitize(displayContent);
    }
    
    /**
     * Get the right transformed display content.
     * MOVED FROM: WeblogEntry.getDisplayContent() (lines 1018-1020)
     */
    public String getDisplayContent() { 
        return displayContent(null);
    }
    
    /**
     * Get transformed text (delegates to entry's business logic).
     * Convenience method for display purposes.
     */
    public String getTransformedText() {
        return entry.getTransformedText();
    }
    
    /**
     * Get transformed summary (delegates to entry's business logic).
     * Convenience method for display purposes.
     */
    public String getTransformedSummary() {
        return entry.getTransformedSummary();
    }
    
    /**
     * Generate excerpt from entry text.
     * New method - provides common display functionality.
     */
    public String generateExcerpt(int maxLength) {
        if (entry == null || entry.getText() == null) {
            return "";
        }
        
        String text = entry.getText();
        
        // Strip HTML tags
        text = text.replaceAll("<[^>]+>", "").trim();
        
        if (text.length() <= maxLength) {
            return text;
        }
        
        // Find last space before maxLength
        int lastSpace = text.lastIndexOf(' ', maxLength - 3);
        if (lastSpace > 0) {
            return text.substring(0, lastSpace) + "...";
        }
        
        return text.substring(0, maxLength - 3) + "...";
    }
    
    /**
     * Calculate estimated read time in minutes.
     * New method - provides common display functionality.
     */
    public int calculateReadTime() {
        if (entry == null || entry.getText() == null) {
            return 0;
        }
        
        // Average reading speed: 200 words per minute
        String text = entry.getText().replaceAll("<[^>]+>", "");
        String[] words = text.split("\\s+");
        int wordCount = words.length;
        int minutes = wordCount / 200;
        
        return Math.max(1, minutes); // At least 1 minute
    }
    
    /**
     * Get word count.
     * New method - provides common display functionality.
     */
    public int getWordCount() {
        if (entry == null || entry.getText() == null) {
            return 0;
        }
        
        String text = entry.getText().replaceAll("<[^>]+>", "");
        String[] words = text.split("\\s+");
        return words.length;
    }
    
    /**
     * Get CSS class based on entry status.
     * New method - provides common display functionality.
     */
    public String getStatusCssClass() {
        if (entry == null) {
            return "entry-draft";
        }
        
        switch (entry.getStatus()) {
            case PUBLISHED:
                return "entry-published";
            case DRAFT:
                return "entry-draft";
            case PENDING:
                return "entry-pending";
            case SCHEDULED:
                return "entry-scheduled";
            default:
                return "entry-unknown";
        }
    }
    
    /**
     * Check if entry is new (published within last 7 days).
     * New method - provides common display functionality.
     */
    public boolean isNew() {
        if (entry == null || entry.getPubTime() == null) {
            return false;
        }
        
        long sevenDaysAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000);
        return entry.getPubTime().getTime() > sevenDaysAgo;
    }
    
    /**
     * Get the underlying entry (for accessing domain data).
     */
    public WeblogEntry getEntry() {
        return entry;
    }
}