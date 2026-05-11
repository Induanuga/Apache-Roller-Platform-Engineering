package org.apache.roller.weblogger.business;

import org.apache.roller.weblogger.pojos.Weblog;
import org.apache.roller.weblogger.pojos.WeblogEntry;
import java.util.List;

/**
 * Plain data holder for Admin Dashboard metrics.
 * Populated by AdminDashboardFacade, consumed by the view strategy and JSP.
 * Contains NO business logic.
 */
public class AdminDashboardData {

    private long totalUsers;
    private long totalWeblogs;
    private long totalEntries;
    private long totalComments;

    private String topCategory;
    private long   topCategoryCount;

    private Weblog mostStarredBlog;
    private long   mostStarredBlogCount;

    // Top 3 most active users: Object[] { userName(String), entryCount(Long) }
    private List<Object[]> topActiveUsers;

    // Most starred entry: Object[] { WeblogEntry, Long starCount }
    private WeblogEntry mostStarredEntry;
    private long        mostStarredEntryCount;

    // Most commented weblog
    private String mostCommentedBlogName;
    private long   mostCommentedBlogCount;

    // --- Getters and Setters ---

    public long getTotalUsers() { return totalUsers; }
    public void setTotalUsers(long totalUsers) { this.totalUsers = totalUsers; }

    public long getTotalWeblogs() { return totalWeblogs; }
    public void setTotalWeblogs(long totalWeblogs) { this.totalWeblogs = totalWeblogs; }

    public long getTotalEntries() { return totalEntries; }
    public void setTotalEntries(long totalEntries) { this.totalEntries = totalEntries; }

    public long getTotalComments() { return totalComments; }
    public void setTotalComments(long totalComments) { this.totalComments = totalComments; }

    public String getTopCategory() { return topCategory; }
    public void setTopCategory(String topCategory) { this.topCategory = topCategory; }

    public long getTopCategoryCount() { return topCategoryCount; }
    public void setTopCategoryCount(long topCategoryCount) { this.topCategoryCount = topCategoryCount; }

    public Weblog getMostStarredBlog() { return mostStarredBlog; }
    public void setMostStarredBlog(Weblog mostStarredBlog) { this.mostStarredBlog = mostStarredBlog; }

    public long getMostStarredBlogCount() { return mostStarredBlogCount; }
    public void setMostStarredBlogCount(long mostStarredBlogCount) { this.mostStarredBlogCount = mostStarredBlogCount; }

    public List<Object[]> getTopActiveUsers() { return topActiveUsers; }
    public void setTopActiveUsers(List<Object[]> topActiveUsers) { this.topActiveUsers = topActiveUsers; }

    public WeblogEntry getMostStarredEntry() { return mostStarredEntry; }
    public void setMostStarredEntry(WeblogEntry mostStarredEntry) { this.mostStarredEntry = mostStarredEntry; }

    public long getMostStarredEntryCount() { return mostStarredEntryCount; }
    public void setMostStarredEntryCount(long mostStarredEntryCount) { this.mostStarredEntryCount = mostStarredEntryCount; }

    public String getMostCommentedBlogName() { return mostCommentedBlogName; }
    public void setMostCommentedBlogName(String mostCommentedBlogName) { this.mostCommentedBlogName = mostCommentedBlogName; }

    public long getMostCommentedBlogCount() { return mostCommentedBlogCount; }
    public void setMostCommentedBlogCount(long mostCommentedBlogCount) { this.mostCommentedBlogCount = mostCommentedBlogCount; }
}
