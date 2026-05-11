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
import org.apache.roller.weblogger.pojos.Weblog;
import org.apache.roller.weblogger.pojos.WeblogCategory;
import org.apache.roller.weblogger.util.cache.CacheManager;

/**
 * Data Transfer Object for Category Management operations.
 * Encapsulates category retrieval, creation, and persistence logic,
 * abstracting business manager calls from the UI action layer.
 * Removes layer violation where Struts2 action directly accessed
 * WeblogEntryManager for category operations and directly performed
 * cache invalidation logic.
 */
public class CategoryManagementDTO {

    private static final Log log = LogFactory.getLog(CategoryManagementDTO.class);

    private Weblog weblog;
    private WeblogCategory category;
    private String categoryId;

    /**
     * Constructor for editing existing category.
     * @param weblog The weblog owning the category
     * @param categoryId The ID of category to edit
     */
    public CategoryManagementDTO(Weblog weblog, String categoryId) {
        this.weblog = weblog;
        this.categoryId = categoryId;
        this.category = null;
    }

    /**
     * Constructor for creating new category.
     * @param weblog The weblog for which to create category
     */
    public CategoryManagementDTO(Weblog weblog) {
        this.weblog = weblog;
        this.categoryId = null;
        this.category = new WeblogCategory();
        this.category.setWeblog(weblog);
    }

    /**
     * Load an existing category by ID from business manager.
     * Abstracts business manager access from action layer.
     * @return true if category loaded successfully, false otherwise
     */
    public boolean loadCategory() {
        try {
            WeblogEntryManager wmgr = WebloggerFactory.getWeblogger().getWeblogEntryManager();
            category = wmgr.getWeblogCategory(categoryId);
            return category != null;
        } catch (WebloggerException ex) {
            log.error("Error looking up category with ID: " + categoryId, ex);
            return false;
        }
    }

    /**
     * Check if category name already exists in weblog.
     * Used for validation to prevent duplicate category names.
     * @param categoryName The name to check
     * @param excludeId If provided, excludes this category ID from check (for edits)
     * @return true if name exists and is different from excludeId, false otherwise
     */
    public boolean categoryNameExists(String categoryName, String excludeId) {
        WeblogCategory existing = weblog.getWeblogCategory(categoryName);
        if (existing != null && !existing.getId().equals(excludeId)) {
            return true;
        }
        return false;
    }

    /**
     * Check if this is a new (unsaved) category.
     * @return true if category ID is null/empty, false otherwise
     */
    public boolean isNewCategory() {
        return categoryId == null || categoryId.trim().isEmpty();
    }

    /**
     * Save category to database.
     * Handles both new category creation and existing category updates.
     * Encapsulates all POJO persistence logic and cache invalidation.
     * @return true if save successful, false otherwise
     * @throws WebloggerException if save fails
     */
    public boolean saveCategory() throws WebloggerException {
        try {
            WeblogEntryManager wmgr = WebloggerFactory.getWeblogger().getWeblogEntryManager();

            // For new categories, add to weblog and calculate position
            if (isNewCategory()) {
                weblog.addCategory(category);
                category.calculatePosition();
            }

            // Persist category to database
            wmgr.saveWeblogCategory(category);

            // Flush changes to database
            WebloggerFactory.getWeblogger().flush();

            // Invalidate cache for this weblog
            CacheManager.invalidate(weblog);

            return true;
        } catch (WebloggerException ex) {
            log.error("Error saving category", ex);
            throw ex;
        }
    }

    /**
     * Get the loaded or created category POJO.
     * @return The WeblogCategory object
     */
    public WeblogCategory getCategory() {
        return category;
    }

    /**
     * Set the category POJO.
     * @param category The WeblogCategory to set
     */
    public void setCategory(WeblogCategory category) {
        this.category = category;
    }

    /**
     * Get the weblog this category belongs to.
     * @return The Weblog object
     */
    public Weblog getWeblog() {
        return weblog;
    }
}
