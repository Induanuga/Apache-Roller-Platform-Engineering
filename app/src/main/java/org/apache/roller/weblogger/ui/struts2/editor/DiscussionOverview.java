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
package org.apache.roller.weblogger.ui.struts2.editor;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.roller.weblogger.WebloggerException;
import org.apache.roller.weblogger.business.WebloggerFactory;
import org.apache.roller.weblogger.business.WeblogEntryManager;
import org.apache.roller.weblogger.business.pulse.DiscussionAnalyzer;
import org.apache.roller.weblogger.business.pulse.DiscussionResult;
import org.apache.roller.weblogger.pojos.CommentSearchCriteria;
import org.apache.roller.weblogger.pojos.WeblogEntry;
import org.apache.roller.weblogger.pojos.WeblogEntryComment;
import org.apache.roller.weblogger.pojos.WeblogEntrySearchCriteria;
import org.apache.roller.weblogger.pojos.WeblogPermission;
import org.apache.roller.weblogger.ui.struts2.util.KeyValueObject;
import org.apache.roller.weblogger.ui.struts2.util.UIAction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Struts2 action for the Discussion Overview (Community Pulse Dashboard).
 * 
 * Shows a high-level snapshot of comment activity for a selected weblog entry,
 * using classical lightweight indicators (no LLM/DL).
 */
public class DiscussionOverview extends UIAction {

    private static final long serialVersionUID = 1L;
    private static Log log = LogFactory.getLog(DiscussionOverview.class);

    // Selected entry ID from the request
    private String entryId;

    // List of all weblog entries (for dropdown selection)
    private List<KeyValueObject> weblogEntries;

    // Selected entry object
    private WeblogEntry selectedEntry;

    // Analysis results from DiscussionAnalyzer
    private List<DiscussionResult> indicatorResults;

    // Analyzer instance
    private final DiscussionAnalyzer analyzer = new DiscussionAnalyzer();

    public DiscussionOverview() {
        this.actionName = "discussionOverview";
        this.desiredMenu = "editor";
        this.pageTitle = "discussionOverview.title";
    }

    @Override
    public List<String> requiredWeblogPermissionActions() {
        return Collections.singletonList(WeblogPermission.POST);
    }

    @Override
    public String execute() {
        try {
            WeblogEntryManager wmgr = WebloggerFactory.getWeblogger().getWeblogEntryManager();

            // Load all entries for the dropdown
            loadWeblogEntries(wmgr);

            // If an entry is selected, analyze its comments
            if (!StringUtils.isEmpty(entryId)) {
                selectedEntry = wmgr.getWeblogEntry(entryId);
                
                if (selectedEntry != null) {
                    // Fetch approved comments for this entry
                    CommentSearchCriteria csc = new CommentSearchCriteria();
                    csc.setWeblog(getActionWeblog());
                    csc.setEntry(selectedEntry);
                    csc.setStatus(WeblogEntryComment.ApprovalStatus.APPROVED);
                    
                    List<WeblogEntryComment> comments = wmgr.getComments(csc);
                    
                    // Analyze comments using the business layer
                    indicatorResults = analyzer.analyze(comments);
                } else {
                    addError("discussionOverview.error.entryNotFound");
                }
            }

        } catch (WebloggerException ex) {
            log.error("Error in Discussion Overview", ex);
            addError("discussionOverview.error.general");
        }

        return "list";
    }

    /**
     * Loads all published entries for the current weblog into a dropdown-friendly list.
     */
    private void loadWeblogEntries(WeblogEntryManager wmgr) throws WebloggerException {
        WeblogEntrySearchCriteria wesc = new WeblogEntrySearchCriteria();
        wesc.setWeblog(getActionWeblog());
        wesc.setStatus(WeblogEntry.PubStatus.PUBLISHED);
        wesc.setMaxResults(500);  // Reasonable limit for dropdown

        List<WeblogEntry> entries = wmgr.getWeblogEntries(wesc);
        
        weblogEntries = new ArrayList<>();
        for (WeblogEntry entry : entries) {
            weblogEntries.add(new KeyValueObject(entry.getId(), entry.getTitle()));
        }
    }

    // Getters and setters for Struts2

    public String getEntryId() {
        return entryId;
    }

    public void setEntryId(String entryId) {
        this.entryId = entryId;
    }

    public List<KeyValueObject> getWeblogEntries() {
        return weblogEntries;
    }

    public WeblogEntry getSelectedEntry() {
        return selectedEntry;
    }

    public List<DiscussionResult> getIndicatorResults() {
        return indicatorResults;
    }
}
