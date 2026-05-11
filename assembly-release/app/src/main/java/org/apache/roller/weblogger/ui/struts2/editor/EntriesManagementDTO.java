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
import org.apache.roller.weblogger.pojos.WeblogEntry;
import org.apache.roller.weblogger.pojos.WeblogEntrySearchCriteria;

/**
 * Data Transfer Object for Entries Management operations.
 * Encapsulates entry search and filtering logic, abstracting
 * business manager calls from the UI action layer.
 * Removes layer violation where Struts2 action directly accessed
 * WeblogEntryManager for entry retrieval and category listing.
 */
public class EntriesManagementDTO {

    private static final Log log = LogFactory.getLog(EntriesManagementDTO.class);
    private static final int COUNT = 30;

    private Weblog weblog;
    private String status;
    private String categoryName;
    private List<String> tags;
    private String text;
    private java.util.Date startDate;
    private java.util.Date endDate;
    private WeblogEntrySearchCriteria.SortBy sortBy;
    private int pageNum;

    private List<WeblogEntry> entries;
    private boolean hasMore;

    public EntriesManagementDTO(Weblog weblog) {
        this.weblog = weblog;
        this.entries = new ArrayList<>();
        this.hasMore = false;
    }

    /**
     * Search for entries based on configured search criteria.
     * Encapsulates all business manager access and search logic.
     */
    public void searchEntries() throws WebloggerException {
        try {
            WeblogEntryManager wmgr = WebloggerFactory.getWeblogger().getWeblogEntryManager();
            WeblogEntrySearchCriteria wesc = new WeblogEntrySearchCriteria();
            wesc.setWeblog(weblog);
            wesc.setStartDate(startDate);
            wesc.setEndDate(endDate);
            wesc.setCatName(categoryName);
            wesc.setTags(tags);
            wesc.setStatus("ALL".equals(status) ? null : WeblogEntry.PubStatus.valueOf(status));
            wesc.setText(text);
            wesc.setSortBy(sortBy);
            wesc.setOffset(pageNum * COUNT);
            wesc.setMaxResults(COUNT + 1);

            List<WeblogEntry> rawEntries = wmgr.getWeblogEntries(wesc);
            entries = new ArrayList<>();
            entries.addAll(rawEntries);

            if (!entries.isEmpty()) {
                log.debug("query found " + rawEntries.size() + " results");

                if (rawEntries.size() > COUNT) {
                    entries.remove(entries.size() - 1);
                    hasMore = true;
                } else {
                    hasMore = false;
                }
            }
        } catch (WebloggerException ex) {
            log.error("Error looking up entries", ex);
            throw ex;
        }
    }

    /**
     * Load all categories for the weblog.
     * Encapsulates category retrieval logic.
     */
    public List<WeblogCategory> loadCategories() {
        List<WeblogCategory> cats = new ArrayList<>();

        // Add "Any" category as default option
        WeblogCategory tmpCat = new WeblogCategory();
        tmpCat.setName("Any");
        cats.add(tmpCat);

        try {
            WeblogEntryManager wmgr = WebloggerFactory.getWeblogger().getWeblogEntryManager();
            List<WeblogCategory> weblogCats = wmgr.getWeblogCategories(weblog);
            cats.addAll(weblogCats);
        } catch (WebloggerException ex) {
            log.error("Error getting category list for weblog - " + weblog.getHandle(), ex);
        }

        return cats;
    }

    // Setters for search criteria
    public void setStatus(String status) {
        this.status = status;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setStartDate(java.util.Date startDate) {
        this.startDate = startDate;
    }

    public void setEndDate(java.util.Date endDate) {
        this.endDate = endDate;
    }

    public void setSortBy(WeblogEntrySearchCriteria.SortBy sortBy) {
        this.sortBy = sortBy;
    }

    public void setPageNum(int pageNum) {
        this.pageNum = pageNum;
    }

    // Getters
    public List<WeblogEntry> getEntries() {
        return entries;
    }

    public boolean hasMore() {
        return hasMore;
    }

    public Weblog getWeblog() {
        return weblog;
    }

    public static int getCountPerPage() {
        return COUNT;
    }
}
