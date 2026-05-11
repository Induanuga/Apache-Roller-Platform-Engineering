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
import org.apache.roller.weblogger.business.WeblogEntryManager;
import org.apache.roller.weblogger.business.WebloggerFactory;
import org.apache.roller.weblogger.business.pulse.ConversationBreakdown;
import org.apache.roller.weblogger.business.pulse.ConversationBreakdownFactory;
import org.apache.roller.weblogger.business.pulse.ConversationBreakdownService;
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
 * Struts2 action for the Conversation Breakdown dashboard (Feature 6B).
 *
 * Allows a weblog author to:
 *   1. Select a blog entry from a dropdown.
 *   2. Choose an insight method: "lightweight" (default, free, fast) or "llm" (GroqCloud AI).
 *   3. View a structured breakdown: major themes, representative comments, overall recap.
 *
 * The method selection maps directly to a {@link ConversationBreakdownService} strategy,
 * obtained via {@link ConversationBreakdownFactory}. This keeps the action free of
 * any concrete service dependencies.
 */
public class ConversationBreakdownAction extends UIAction {

    private static final long serialVersionUID = 1L;
    private static final Log log = LogFactory.getLog(ConversationBreakdownAction.class);

    // Form inputs
    private String entryId;
    private String method = ConversationBreakdownFactory.METHOD_LIGHTWEIGHT; // default

    // View data
    private List<KeyValueObject> weblogEntries;
    private WeblogEntry selectedEntry;
    private ConversationBreakdown breakdown;

    public ConversationBreakdownAction() {
        this.actionName = "conversationBreakdown";
        this.desiredMenu = "editor";
        this.pageTitle = "conversationBreakdown.title";
    }

    @Override
    public List<String> requiredWeblogPermissionActions() {
        return Collections.singletonList(WeblogPermission.POST);
    }

    @Override
    public String execute() {
        try {
            WeblogEntryManager wmgr = WebloggerFactory.getWeblogger().getWeblogEntryManager();
            loadWeblogEntries(wmgr);

            if (!StringUtils.isEmpty(entryId)) {
                selectedEntry = wmgr.getWeblogEntry(entryId);

                if (selectedEntry != null) {
                    // Fetch approved comments
                    CommentSearchCriteria csc = new CommentSearchCriteria();
                    csc.setWeblog(getActionWeblog());
                    csc.setEntry(selectedEntry);
                    csc.setStatus(WeblogEntryComment.ApprovalStatus.APPROVED);
                    List<WeblogEntryComment> comments = wmgr.getComments(csc);

                    // Select strategy via Factory — Strategy pattern in action
                    ConversationBreakdownService service =
                        ConversationBreakdownFactory.getService(method);

                    breakdown = service.generate(comments);
                } else {
                    addError("conversationBreakdown.error.entryNotFound");
                }
            }

        } catch (WebloggerException ex) {
            log.error("Error in Conversation Breakdown action", ex);
            addError("conversationBreakdown.error.general");
        }

        return "list";
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void loadWeblogEntries(WeblogEntryManager wmgr) throws WebloggerException {
        WeblogEntrySearchCriteria wesc = new WeblogEntrySearchCriteria();
        wesc.setWeblog(getActionWeblog());
        wesc.setStatus(WeblogEntry.PubStatus.PUBLISHED);
        wesc.setMaxResults(500);

        List<WeblogEntry> entries = wmgr.getWeblogEntries(wesc);
        weblogEntries = new ArrayList<>();
        for (WeblogEntry entry : entries) {
            weblogEntries.add(new KeyValueObject(entry.getId(), entry.getTitle()));
        }
    }

    // -------------------------------------------------------------------------
    // Getters / Setters
    // -------------------------------------------------------------------------

    public String getEntryId() { return entryId; }
    public void setEntryId(String entryId) { this.entryId = entryId; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public List<KeyValueObject> getWeblogEntries() { return weblogEntries; }
    public WeblogEntry getSelectedEntry() { return selectedEntry; }
    public ConversationBreakdown getBreakdown() { return breakdown; }
}