package org.apache.roller.weblogger.ui.viewmodel;

import java.util.List;
import org.apache.roller.weblogger.pojos.WeblogEntry;

/**
 * Encapsulates all pagination logic for weblog entries.
 * This class handles page calculations, navigation, and entry slicing.
 */
public class PaginationHelper {
    
    private final int pageSize;
    private final int currentPage;
    private final int totalEntries;
    
    /**
     * Constructor for pagination helper.
     * 
     * @param pageSize Number of entries per page
     * @param currentPage Current page number (0-indexed)
     * @param totalEntries Total number of entries available
     */
    public PaginationHelper(int pageSize, int currentPage, int totalEntries) {
        this.pageSize = Math.max(1, pageSize);
        this.currentPage = Math.max(0, currentPage);
        this.totalEntries = Math.max(0, totalEntries);
    }
    
    /**
     * Calculate total number of pages.
     * 
     * @return Total pages
     */
    public int getTotalPages() {
        if (totalEntries == 0 || pageSize == 0) {
            return 0;
        }
        return (int) Math.ceil((double) totalEntries / pageSize);
    }
    
    /**
     * Check if there is a next page.
     * 
     * @return true if next page exists
     */
    public boolean hasNextPage() {
        return currentPage < getTotalPages() - 1;
    }
    
    /**
     * Check if there is a previous page.
     * 
     * @return true if previous page exists
     */
    public boolean hasPreviousPage() {
        return currentPage > 0;
    }
    
    /**
     * Get the starting index for current page.
     * 
     * @return Start index (0-based)
     */
    public int getStartIndex() {
        return currentPage * pageSize;
    }
    
    /**
     * Get the ending index for current page.
     * 
     * @return End index (exclusive)
     */
    public int getEndIndex() {
        int endIndex = (currentPage + 1) * pageSize;
        return Math.min(endIndex, totalEntries);
    }
    
    /**
     * Get page window for navigation (e.g., [1, 2, 3, 4, 5]).
     * 
     * @param windowSize Size of the page window
     * @return Array of page numbers to display in pagination controls
     */
    public int[] getPageWindow(int windowSize) {
        int totalPages = getTotalPages();
        if (totalPages == 0) {
            return new int[0];
        }
        
        int halfWindow = windowSize / 2;
        int startPage = Math.max(0, currentPage - halfWindow);
        int endPage = Math.min(totalPages, startPage + windowSize);
        
        // Adjust if we're near the end
        if (endPage - startPage < windowSize) {
            startPage = Math.max(0, endPage - windowSize);
        }
        
        int[] window = new int[endPage - startPage];
        for (int i = 0; i < window.length; i++) {
            window[i] = startPage + i;
        }
        return window;
    }
    
    /**
     * Slice a list of entries for the current page.
     * 
     * @param allEntries Complete list of entries
     * @return Sublist for current page
     */
    public <T> List<T> sliceForPage(List<T> allEntries) {
        if (allEntries == null || allEntries.isEmpty()) {
            return List.of();
        }
        
        int start = getStartIndex();
        int end = Math.min(getEndIndex(), allEntries.size());
        
        if (start >= allEntries.size()) {
            return List.of();
        }
        
        return allEntries.subList(start, end);
    }
    
    // Getters
    public int getPageSize() {
        return pageSize;
    }
    
    public int getCurrentPage() {
        return currentPage;
    }
    
    public int getTotalEntries() {
        return totalEntries;
    }
    
    /**
     * Get human-readable page number (1-indexed).
     */
    public int getDisplayPageNumber() {
        return currentPage + 1;
    }
}