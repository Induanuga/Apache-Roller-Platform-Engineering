package org.apache.roller.weblogger.ui.viewmodel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.roller.weblogger.business.URLStrategy;
import org.apache.roller.weblogger.pojos.WeblogEntry;
import org.apache.roller.weblogger.pojos.Weblog;

/**
 * Handles all display formatting and URL generation for weblog entries.
 * Extracts presentation concerns from UI actions.
 */
public class EntryDisplayFormatter {
    
    private final URLStrategy urlStrategy;
    private final SimpleDateFormat dateFormat;
    
    public EntryDisplayFormatter(URLStrategy urlStrategy) {
        this.urlStrategy = urlStrategy;
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm");
    }
    
    /**
     * Generate permalink URL for an entry.
     * 
     * @param entry Weblog entry
     * @return Formatted URL string
     */
    public String generatePermalink(WeblogEntry entry) {
        if (entry == null || urlStrategy == null) {
            return "";
        }
        return urlStrategy.getWeblogEntryURL(
            entry.getWebsite(), 
            null,  // locale (null for default)
            entry.getAnchor(), 
            true
        );
    }
    
    /**
     * Generate edit URL for an entry.
     * 
     * @param entry Weblog entry
     * @return Edit URL string
     */
    public String generateEditURL(WeblogEntry entry) {
        if (entry == null) {
            return "";
        }
        return String.format(
            "/roller-ui/authoring/entryEdit.rol?weblog=%s&amp;entryId=%s",
            entry.getWebsite().getHandle(),
            entry.getId()
        );
    }
    
    /**
     * Generate preview URL for an entry.
     * 
     * @param entry Weblog entry
     * @return Preview URL string
     */
    public String generatePreviewURL(WeblogEntry entry) {
        if (entry == null || urlStrategy == null) {
            return "";
        }
        // Preview URLs may not be available in this version
        // Return permalink as fallback
        return generatePermalink(entry);
    }
    
    /**
     * Format entry publication date.
     * 
     * @param entry Weblog entry
     * @return Formatted date string
     */
    public String formatPublicationDate(WeblogEntry entry) {
        if (entry == null || entry.getPubTime() == null) {
            return "";
        }
        return dateFormat.format(entry.getPubTime());
    }
    
    /**
     * Format entry update date.
     * 
     * @param entry Weblog entry
     * @return Formatted date string
     */
    public String formatUpdateDate(WeblogEntry entry) {
        if (entry == null || entry.getUpdateTime() == null) {
            return "";
        }
        return dateFormat.format(entry.getUpdateTime());
    }
    
    /**
     * Generate display title (truncated if needed).
     * 
     * @param entry Weblog entry
     * @param maxLength Maximum title length
     * @return Formatted title
     */
    public String formatTitle(WeblogEntry entry, int maxLength) {
        if (entry == null || entry.getTitle() == null) {
            return "";
        }
        
        String title = entry.getTitle();
        if (title.length() <= maxLength) {
            return title;
        }
        
        return title.substring(0, maxLength - 3) + "...";
    }
    
    /**
     * Generate excerpt from entry text.
     * 
     * @param entry Weblog entry
     * @param maxLength Maximum excerpt length
     * @return Formatted excerpt
     */
    public String generateExcerpt(WeblogEntry entry, int maxLength) {
        if (entry == null || entry.getText() == null) {
            return "";
        }
        
        // Strip HTML tags
        String plainText = entry.getText().replaceAll("<[^>]+>", "");
        plainText = plainText.trim();
        
        if (plainText.length() <= maxLength) {
            return plainText;
        }
        
        // Find last space before maxLength
        int lastSpace = plainText.lastIndexOf(' ', maxLength - 3);
        if (lastSpace > 0) {
            return plainText.substring(0, lastSpace) + "...";
        }
        
        return plainText.substring(0, maxLength - 3) + "...";
    }
    
    /**
     * Generate CSS class for entry status.
     * 
     * @param entry Weblog entry
     * @return CSS class name
     */
    public String getStatusCssClass(WeblogEntry entry) {
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
     * Check if entry should display as new (published within last 7 days).
     * 
     * @param entry Weblog entry
     * @return true if entry is new
     */
    public boolean isNewEntry(WeblogEntry entry) {
        if (entry == null || entry.getPubTime() == null) {
            return false;
        }
        
        long sevenDaysAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000);
        return entry.getPubTime().getTime() > sevenDaysAgo;
    }
    
    /**
     * Generate tag display string.
     * 
     * @param entry Weblog entry
     * @param separator Tag separator (e.g., ", " or " | ")
     * @return Formatted tag string
     */
    public String formatTags(WeblogEntry entry, String separator) {
        if (entry == null || entry.getTags() == null || entry.getTags().isEmpty()) {
            return "";
        }
        
        // Convert Set<WeblogEntryTag> to comma-separated string
        return entry.getTags().stream()
            .map(tag -> tag.getName())
            .collect(Collectors.joining(separator));
    }
    
    /**
     * Generate a base URL with query parameters.
     * 
     * @param baseAction Base action name
     * @param weblogHandle Weblog handle
     * @param params Query parameters
     * @param urlStrategy URL strategy instance
     * @return Formatted URL
     */
    public String generateActionURL(String baseAction, String weblogHandle, 
                                    Map<String, String> params, URLStrategy urlStrategy) {
        if (urlStrategy == null) {
            return "";
        }
        return urlStrategy.getActionURL(baseAction, "/roller-ui/authoring", 
                                        weblogHandle, params, false);
    }
}