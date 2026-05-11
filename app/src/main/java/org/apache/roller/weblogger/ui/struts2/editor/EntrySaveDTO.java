/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  The ASF licenses this file to You
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.roller.weblogger.WebloggerException;
import org.apache.roller.weblogger.business.WebloggerFactory;
import org.apache.roller.weblogger.business.WeblogEntryManager;
import org.apache.roller.weblogger.business.search.IndexManager;
import org.apache.roller.weblogger.pojos.Weblog;
import org.apache.roller.weblogger.pojos.WeblogEntry;
import org.apache.roller.weblogger.util.cache.CacheManager;
import org.apache.roller.weblogger.util.MailUtil;

/**
 * Data Transfer Object for Entry Save operations.
 * Encapsulates entry persistence, indexing, cache invalidation, and notification logic,
 * abstracting all business manager calls from the UI action layer.
 * Removes layer violation where Struts2 action directly accessed multiple managers
 * (WeblogEntryManager, IndexManager, AutopingManager) and performed cache/index operations.
 */
public class EntrySaveDTO {

    private static final Log log = LogFactory.getLog(EntrySaveDTO.class);

    private Weblog weblog;
    private WeblogEntry entry;

    /**
     * Constructor for entry save operations.
     * @param weblog The weblog the entry belongs to
     * @param entry The entry to save
     */
    public EntrySaveDTO(Weblog weblog, WeblogEntry entry) {
        this.weblog = weblog;
        this.entry = entry;
    }

    /**
     * Save entry to database with all related operations.
     * Encapsulates:
     * - WeblogEntryManager.saveWeblogEntry()
     * - Database flush
     * - IndexManager operations (add/remove based on status)
     * - CacheManager invalidation
     * - AutopingManager queueing
     * - Pending entry email notification
     *
     * @return true if save successful, false otherwise
     * @throws WebloggerException if save fails
     */
    public boolean saveEntry() throws WebloggerException {
        try {
            WeblogEntryManager weblogEntryManager = WebloggerFactory.getWeblogger()
                    .getWeblogEntryManager();

            IndexManager indexMgr = WebloggerFactory.getWeblogger()
                    .getIndexManager();

            if (log.isDebugEnabled()) {
                log.debug("Saving entry with status: " + entry.getStatus());
                log.debug("Entry pubtime = " + entry.getPubTime());
            }

            // Persist entry to database
            weblogEntryManager.saveWeblogEntry(entry);
            WebloggerFactory.getWeblogger().flush();

            // Update search index based on entry status
            if (entry.isPublished()) {
                // Add published entry to search index
                indexMgr.addEntryReIndexOperation(entry);
            } else {
                // Remove unpublished entry from search index (for edits)
                indexMgr.removeEntryIndexOperation(entry);
            }

            // Invalidate cache for this entry
            CacheManager.invalidate(entry);

            // Queue applicable pings for published entries
            if (entry.isPublished()) {
                WebloggerFactory.getWeblogger().getAutopingManager()
                        .queueApplicableAutoPings(entry);
            }

            // Send notification for pending entries
            if (entry.isPending() && MailUtil.isMailConfigured()) {
                MailUtil.sendPendingEntryNotice(entry);
            }

            return true;
        } catch (WebloggerException ex) {
            log.error("Error saving entry", ex);
            throw ex;
        }
    }

    /**
     * Get the entry being saved.
     * @return The WeblogEntry object
     */
    public WeblogEntry getEntry() {
        return entry;
    }

    /**
     * Set the entry to save.
     * @param entry The WeblogEntry to set
     */
    public void setEntry(WeblogEntry entry) {
        this.entry = entry;
    }

    /**
     * Get the weblog the entry belongs to.
     * @return The Weblog object
     */
    public Weblog getWeblog() {
        return weblog;
    }
}
