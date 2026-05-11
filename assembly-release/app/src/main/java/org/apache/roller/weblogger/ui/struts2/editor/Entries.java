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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.roller.weblogger.WebloggerException;
import org.apache.roller.weblogger.business.WebloggerFactory;
import org.apache.roller.weblogger.business.WeblogEntryManager;
import org.apache.roller.weblogger.pojos.WeblogCategory;
import org.apache.roller.weblogger.pojos.WeblogEntry;
import org.apache.roller.weblogger.pojos.WeblogEntrySearchCriteria;
import org.apache.roller.weblogger.pojos.WeblogPermission;
import org.apache.roller.weblogger.ui.struts2.pagers.EntriesPager;
import org.apache.roller.weblogger.ui.struts2.util.KeyValueObject;
import org.apache.roller.weblogger.ui.struts2.util.UIAction;
import org.apache.struts2.convention.annotation.AllowedMethods;
// ADDED THESE IMPORTS:
import org.apache.roller.weblogger.ui.viewmodel.WeblogEntriesViewModel;
import org.apache.roller.weblogger.ui.viewmodel.WeblogEntriesViewModel.FilterCriteria;
import org.apache.roller.weblogger.ui.viewmodel.PaginationHelper;

/**
 * A list view of entries in a weblog.
 */
// TODO: make this work @AllowedMethods({"execute"})
public class Entries extends UIAction {
    
    private static Log log = LogFactory.getLog(Entries.class);
    
    // number of comments to show per page
    private static final int COUNT = 30;
    // ADDED THIS FIELD:
    // ViewModel for presentation logic
    private WeblogEntriesViewModel viewModel = null;
    
    // bean for managing submitted data
    private EntriesBean bean = new EntriesBean();
    
    // pager for the entries we are viewing
    private EntriesPager pager = null;
    
    // first entry in the list
    private WeblogEntry firstEntry = null;
    
    // last entry in the list
    private WeblogEntry lastEntry = null;
    
    
    public Entries() {
        this.actionName = "entries";
        this.desiredMenu = "editor";
        this.pageTitle = "weblogEntryQuery.title";
    }
    
    
    @Override
    public List<String> requiredWeblogPermissionActions() {
        return Collections.singletonList(WeblogPermission.POST);
    }
    
    
    // @Override
    // public String execute() {
        
    //     if (log.isDebugEnabled()) {
    //         log.debug("entries bean is ...\n"+getBean().toString());
    //     }
        
    //     try {
    //         // Use DTO to encapsulate entry search and filtering logic
    //         EntriesManagementDTO entriesMgmt = new EntriesManagementDTO(getActionWeblog());
    //         entriesMgmt.setStatus(getBean().getStatus());
    //         entriesMgmt.setCategoryName(getBean().getCategoryName());
    //         entriesMgmt.setTags(getBean().getTags());
    //         entriesMgmt.setText(getBean().getText());
    //         entriesMgmt.setStartDate(getBean().getStartDate());
    //         entriesMgmt.setEndDate(getBean().getEndDate());
    //         entriesMgmt.setSortBy(getBean().getSortBy());
    //         entriesMgmt.setPageNum(getBean().getPage());

    //         // Search entries via DTO (abstracts all business manager access)
    //         entriesMgmt.searchEntries();

    //         List<WeblogEntry> entries = entriesMgmt.getEntries();
    //         if (!entries.isEmpty()) {
    //             setFirstEntry(entries.get(0));
    //             setLastEntry(entries.get(entries.size()-1));
    //         }

    //         // build entries pager
    //         String baseUrl = buildBaseUrl();
    //         setPager(new EntriesPager(baseUrl, getBean().getPage(), entries, entriesMgmt.hasMore()));
    //     } catch (WebloggerException ex) {
    //         log.error("Error looking up entries", ex);
    //         addError("Error looking up entries");
    //     }
                
    //     return LIST;
    // }
    @Override
    public String execute() {
        
        if (log.isDebugEnabled()) {
            log.debug("entries bean is ...\n" + getBean().toString());
        }
        
        try {
            // 1. Build filter criteria from request bean
            FilterCriteria criteria = buildFilterCriteria();
            
            // 2. Load entries using existing business logic
            List<WeblogEntry> entries = loadEntries(criteria);
            
            // 3. Create ViewModel - delegates ALL presentation logic
            viewModel = new WeblogEntriesViewModel(
                getActionWeblog(),
                entries,
                WebloggerFactory.getWeblogger().getUrlStrategy(),
                criteria,
                getBean().getPage(),
                COUNT
            );
            
            // 4. Keep existing pager for backward compatibility (for now)
            String baseUrl = buildBaseUrl();
            boolean hasMore = viewModel.getPagination().hasNextPage();
            setPager(new EntriesPager(baseUrl, getBean().getPage(), 
                                    viewModel.getPageEntries(), hasMore));
            
            // 5. Set first/last entries from ViewModel
            List<WeblogEntry> pageEntries = viewModel.getPageEntries();
            if (!pageEntries.isEmpty()) {
                setFirstEntry(pageEntries.get(0));
                setLastEntry(pageEntries.get(pageEntries.size() - 1));
            }
            
            return LIST;
            
        } catch (WebloggerException ex) {
            log.error("Error looking up entries", ex);
            addError("Error looking up entries");
            return ERROR;
        }
    }
    /**
     * Build filter criteria from the request bean.
     * Maps request parameters to FilterCriteria object.
     */
    private FilterCriteria buildFilterCriteria() {
        FilterCriteria criteria = new FilterCriteria();
        criteria.setCategoryName(getBean().getCategoryName());
        criteria.setStatus(getBean().getStatus());
        
        // Map tags from bean
        if (getBean().getTags() != null && !getBean().getTags().isEmpty()) {
            criteria.setTags(getBean().getTagsAsString());
        }
        
        // Map search text
        criteria.setSearchText(getBean().getText());
        
        // Map date range
        criteria.setStartDate(getBean().getStartDate());
        criteria.setEndDate(getBean().getEndDate());
        
        // Map sorting
        if (getBean().getSortBy() != null) {
            criteria.setSortBy(getBean().getSortBy().name());
            criteria.setAscending(false); // Default to descending
        }
        
        return criteria;
    }

    /**
     * Load entries from business layer based on filter criteria.
     * Simplified - no pagination logic here anymore.
     */
    private List<WeblogEntry> loadEntries(FilterCriteria criteria) throws WebloggerException {
        WeblogEntryManager wmgr = WebloggerFactory.getWeblogger().getWeblogEntryManager();
        
        // Build search criteria
        WeblogEntrySearchCriteria wesc = new WeblogEntrySearchCriteria();
        wesc.setWeblog(getActionWeblog());
        wesc.setStartDate(criteria.getStartDate());
        wesc.setEndDate(criteria.getEndDate());
        wesc.setCatName(criteria.getCategoryName());
        
        // Handle tags
        if (criteria.getTags() != null && !criteria.getTags().isEmpty()) {
            wesc.setTags(getBean().getTags()); // Use original tags list
        }
        
        // Handle status
        String status = criteria.getStatus();
        wesc.setStatus("ALL".equals(status) ? null : WeblogEntry.PubStatus.valueOf(status));
        
        wesc.setText(criteria.getSearchText());
        
        // Handle sorting
        if (criteria.getSortBy() != null) {
            wesc.setSortBy(getBean().getSortBy()); // Use original SortBy enum
        }
        
        // Important: Load ALL entries, let ViewModel handle pagination
        wesc.setOffset(0);
        wesc.setMaxResults(-1); // No limit - or use a reasonable limit like 1000
        
        return wmgr.getWeblogEntries(wesc);
    }
    // use the action data to build a url representing this action, including query data
    private String buildBaseUrl() {
        
        Map<String, String> params = new HashMap<>();
        
        if(!StringUtils.isEmpty(getBean().getCategoryName())) {
            params.put("bean.categoryPath", getBean().getCategoryName());
        }
        if(!StringUtils.isEmpty(getBean().getTagsAsString())) {
            params.put("bean.tagsAsString", getBean().getTagsAsString());
        }
        if(!StringUtils.isEmpty(getBean().getText())) {
            params.put("bean.text", getBean().getText());
        }
        if(!StringUtils.isEmpty(getBean().getStartDateString())) {
            params.put("bean.startDateString", getBean().getStartDateString());
        }
        if(!StringUtils.isEmpty(getBean().getEndDateString())) {
            params.put("bean.endDateString", getBean().getEndDateString());
        }
        if(!StringUtils.isEmpty(getBean().getStatus())) {
            params.put("bean.status", getBean().getStatus());
        }
        if(getBean().getSortBy() != null) {
            params.put("bean.sortBy", getBean().getSortBy().toString());
        }

        return WebloggerFactory.getWeblogger().getUrlStrategy().getActionURL("entries", "/roller-ui/authoring", 
                getActionWeblog().getHandle(), params, false);
    }
    
    
    /**
     * Get the list of all categories for the action weblog, not including root.
     */
    public List<WeblogCategory> getCategories() {
        // Use DTO to load categories (abstracts business manager access)
        EntriesManagementDTO entriesMgmt = new EntriesManagementDTO(getActionWeblog());
        return entriesMgmt.loadCategories();
    }
    
    
    public List<KeyValueObject> getSortByOptions() {
        List<KeyValueObject> opts = new ArrayList<>();
        
        opts.add(new KeyValueObject(WeblogEntrySearchCriteria.SortBy.PUBLICATION_TIME.name(), getText("weblogEntryQuery.label.pubTime")));
        opts.add(new KeyValueObject(WeblogEntrySearchCriteria.SortBy.UPDATE_TIME.name(), getText("weblogEntryQuery.label.updateTime")));
        
        return opts;
    }
    
    public List<KeyValueObject> getStatusOptions() {
        List<KeyValueObject> opts = new ArrayList<>();
        
        opts.add(new KeyValueObject("ALL", getText("weblogEntryQuery.label.allEntries")));
        opts.add(new KeyValueObject("DRAFT", getText("weblogEntryQuery.label.draftOnly")));
        opts.add(new KeyValueObject("PUBLISHED", getText("weblogEntryQuery.label.publishedOnly")));
        opts.add(new KeyValueObject("PENDING", getText("weblogEntryQuery.label.pendingOnly")));
        opts.add(new KeyValueObject("SCHEDULED", getText("weblogEntryQuery.label.scheduledOnly")));
        
        return opts;
    }
    
    
    public EntriesBean getBean() {
        return bean;
    }

    public void setBean(EntriesBean bean) {
        this.bean = bean;
    }

    public WeblogEntry getFirstEntry() {
        return firstEntry;
    }

    public void setFirstEntry(WeblogEntry firstEntry) {
        this.firstEntry = firstEntry;
    }

    public WeblogEntry getLastEntry() {
        return lastEntry;
    }

    public void setLastEntry(WeblogEntry lastEntry) {
        this.lastEntry = lastEntry;
    }

    public EntriesPager getPager() {
        return pager;
    }

    public void setPager(EntriesPager pager) {
        this.pager = pager;
    }
    /**
     * Get the ViewModel for use in JSP.
     * 
     * @return WeblogEntriesViewModel containing all presentation logic
     */
    public WeblogEntriesViewModel getViewModel() {
        return viewModel;
    }
    
}
