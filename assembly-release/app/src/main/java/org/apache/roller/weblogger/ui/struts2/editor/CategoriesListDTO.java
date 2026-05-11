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

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.roller.weblogger.WebloggerException;
import org.apache.roller.weblogger.business.WebloggerFactory;
import org.apache.roller.weblogger.business.WeblogEntryManager;
import org.apache.roller.weblogger.pojos.Weblog;
import org.apache.roller.weblogger.pojos.WeblogCategory;

/**
 * Data Transfer Object for Categories listing and retrieval operations.
 * Encapsulates category retrieval logic and business manager access,
 * abstracting these calls from the UI action layer.
 * Removes layer violation where Struts2 action directly accessed
 * WeblogEntryManager for category listing.
 */
public class CategoriesListDTO {

    private static final Log log = LogFactory.getLog(CategoriesListDTO.class);

    private Weblog weblog;
    private List<WeblogCategory> allCategories;

    /**
     * Constructor for category listing.
     * @param weblog The weblog whose categories to list
     */
    public CategoriesListDTO(Weblog weblog) {
        this.weblog = weblog;
        this.allCategories = new ArrayList<>();
    }

    /**
     * Load all categories for the weblog from business manager.
     * Encapsulates all business manager access and error handling.
     * @return true if categories loaded successfully, false otherwise
     */
    public boolean loadAllCategories() {
        try {
            WeblogEntryManager wmgr = WebloggerFactory.getWeblogger().getWeblogEntryManager();
            allCategories = wmgr.getWeblogCategories(weblog);
            return true;
        } catch (WebloggerException ex) {
            log.error("Error building categories list for weblog: " + weblog.getHandle(), ex);
            return false;
        }
    }

    /**
     * Get all categories for the weblog.
     * @return List of WeblogCategory objects
     */
    public List<WeblogCategory> getAllCategories() {
        return allCategories;
    }

    /**
     * Set all categories for the weblog.
     * @param allCategories List of WeblogCategory objects
     */
    public void setAllCategories(List<WeblogCategory> allCategories) {
        this.allCategories = allCategories;
    }

    /**
     * Get the weblog.
     * @return The Weblog object
     */
    public Weblog getWeblog() {
        return weblog;
    }
}
