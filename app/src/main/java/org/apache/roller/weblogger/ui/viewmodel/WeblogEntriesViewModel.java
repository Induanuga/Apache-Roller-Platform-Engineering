package org.apache.roller.weblogger.ui.viewmodel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.roller.weblogger.business.URLStrategy;
import org.apache.roller.weblogger.pojos.WeblogEntry;
import org.apache.roller.weblogger.pojos.Weblog;

/**
 * ViewModel for weblog entries display.
 * Handles all presentation data transformation and view logic.
 */
public class WeblogEntriesViewModel {
    
    private final Weblog weblog;
    private final List<WeblogEntry> entries;
    private final PaginationHelper paginationHelper;
    private final EntryDisplayFormatter formatter;
    private final FilterCriteria filterCriteria;
    
    /**
     * Constructor for WeblogEntriesViewModel.
     */
    public WeblogEntriesViewModel(
            Weblog weblog,
            List<WeblogEntry> entries,
            URLStrategy urlStrategy,
            FilterCriteria filterCriteria,
            int currentPage,
            int pageSize) {
        
        this.weblog = weblog;
        this.entries = entries != null ? entries : new ArrayList<>();
        this.filterCriteria = filterCriteria != null ? filterCriteria : new FilterCriteria();
        this.formatter = new EntryDisplayFormatter(urlStrategy);
        this.paginationHelper = new PaginationHelper(
            pageSize,
            currentPage,
            this.entries.size()
        );
    }
    
    /**
     * Get entries for current page.
     * 
     * @return List of entries to display on current page
     */
    public List<WeblogEntry> getPageEntries() {
        return paginationHelper.sliceForPage(entries);
    }
    
    /**
     * Get formatted entry data for display.
     * 
     * @return List of entry display objects
     */
    public List<EntryDisplayData> getDisplayEntries() {
        return getPageEntries().stream()
            .map(this::createDisplayData)
            .collect(Collectors.toList());
    }
    
    /**
     * Create display data object for an entry.
     */
    private EntryDisplayData createDisplayData(WeblogEntry entry) {
        EntryDisplayData data = new EntryDisplayData();
        data.setEntry(entry);
        data.setPermalink(formatter.generatePermalink(entry));
        data.setEditUrl(formatter.generateEditURL(entry));
        data.setFormattedDate(formatter.formatPublicationDate(entry));
        data.setExcerpt(formatter.generateExcerpt(entry, 200));
        data.setStatusClass(formatter.getStatusCssClass(entry));
        data.setIsNew(formatter.isNewEntry(entry));
        data.setFormattedTags(formatter.formatTags(entry, ", "));
        return data;
    }
    
    /**
     * Get pagination information.
     * 
     * @return Pagination helper
     */
    public PaginationHelper getPagination() {
        return paginationHelper;
    }
    
    /**
     * Get filter information.
     * 
     * @return Current filter criteria
     */
    public FilterCriteria getFilter() {
        return filterCriteria;
    }
    
    /**
     * Get weblog information.
     * 
     * @return Current weblog
     */
    public Weblog getWeblog() {
        return weblog;
    }
    
    /**
     * Get summary statistics.
     * 
     * @return Map of statistics
     */
    public Map<String, Object> getSummaryStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalEntries", entries.size());
        stats.put("publishedCount", countByStatus(WeblogEntry.PubStatus.PUBLISHED));
        stats.put("draftCount", countByStatus(WeblogEntry.PubStatus.DRAFT));
        stats.put("scheduledCount", countByStatus(WeblogEntry.PubStatus.SCHEDULED));
        stats.put("currentPage", paginationHelper.getDisplayPageNumber());
        stats.put("totalPages", paginationHelper.getTotalPages());
        return stats;
    }
    
    /**
     * Count entries by status.
     */
    private long countByStatus(WeblogEntry.PubStatus status) {
        return entries.stream()
            .filter(e -> e.getStatus() == status)
            .count();
    }
    
    /**
     * Check if any entries exist.
     */
    public boolean hasEntries() {
        return !entries.isEmpty();
    }
    
    /**
     * Check if filter is active.
     */
    public boolean isFiltered() {
        return filterCriteria.isActive();
    }
    
    /**
     * Inner class representing display data for an entry.
     */
    public static class EntryDisplayData {
        private WeblogEntry entry;
        private String permalink;
        private String editUrl;
        private String formattedDate;
        private String excerpt;
        private String statusClass;
        private boolean isNew;
        private String formattedTags;
        
        // Getters and setters
        public WeblogEntry getEntry() { return entry; }
        public void setEntry(WeblogEntry entry) { this.entry = entry; }
        
        public String getPermalink() { return permalink; }
        public void setPermalink(String permalink) { this.permalink = permalink; }
        
        public String getEditUrl() { return editUrl; }
        public void setEditUrl(String editUrl) { this.editUrl = editUrl; }
        
        public String getFormattedDate() { return formattedDate; }
        public void setFormattedDate(String formattedDate) { this.formattedDate = formattedDate; }
        
        public String getExcerpt() { return excerpt; }
        public void setExcerpt(String excerpt) { this.excerpt = excerpt; }
        
        public String getStatusClass() { return statusClass; }
        public void setStatusClass(String statusClass) { this.statusClass = statusClass; }
        
        public boolean isNew() { return isNew; }
        public void setIsNew(boolean isNew) { this.isNew = isNew; }
        
        public String getFormattedTags() { return formattedTags; }
        public void setFormattedTags(String formattedTags) { this.formattedTags = formattedTags; }
    }
    
    // /**
    //  * Filter criteria class.
    //  */
    // public static class FilterCriteria {
    //     private String categoryName;
    //     private String status;
    //     private String sortBy;
    //     private boolean ascending;
        
    //     public boolean isActive() {
    //         return categoryName != null || status != null;
    //     }
        
    //     // Getters and setters
    //     public String getCategoryName() { return categoryName; }
    //     public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
        
    //     public String getStatus() { return status; }
    //     public void setStatus(String status) { this.status = status; }
        
    //     public String getSortBy() { return sortBy; }
    //     public void setSortBy(String sortBy) { this.sortBy = sortBy; }
        
    //     public boolean isAscending() { return ascending; }
    //     public void setAscending(boolean ascending) { this.ascending = ascending; }
    // }
    /**
 * Filter criteria class - enhanced for Entries.java compatibility.
 */
public static class FilterCriteria {
    private String categoryName;
    private String status;
    private String sortBy;
    private boolean ascending;
    private String tags;              // ADD THIS
    private String searchText;         // ADD THIS
    private java.util.Date startDate;  // ADD THIS
    private java.util.Date endDate;    // ADD THIS
    
    public boolean isActive() {
        return categoryName != null || status != null || 
               tags != null || searchText != null ||
               startDate != null || endDate != null;
    }
    
    // ADD ALL THESE GETTERS AND SETTERS:
    
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    
    public String getSearchText() { return searchText; }
    public void setSearchText(String searchText) { this.searchText = searchText; }
    
    public java.util.Date getStartDate() { return startDate; }
    public void setStartDate(java.util.Date startDate) { this.startDate = startDate; }
    
    public java.util.Date getEndDate() { return endDate; }
    public void setEndDate(java.util.Date endDate) { this.endDate = endDate; }
    
    // Existing getters/setters for categoryName, status, sortBy, ascending...
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getSortBy() { return sortBy; }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }
    
    public boolean isAscending() { return ascending; }
    public void setAscending(boolean ascending) { this.ascending = ascending; }
}
}