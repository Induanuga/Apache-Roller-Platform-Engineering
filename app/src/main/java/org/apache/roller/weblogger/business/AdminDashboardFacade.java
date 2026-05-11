
package org.apache.roller.weblogger.business;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.roller.weblogger.WebloggerException;
import org.apache.roller.weblogger.pojos.StatCount;
import org.apache.roller.weblogger.pojos.Weblog;
import org.apache.roller.weblogger.pojos.WeblogEntry;
import org.apache.roller.weblogger.pojos.WeblogEntrySearchCriteria;
import org.apache.roller.weblogger.pojos.WeblogPermission;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FACADE PATTERN — AdminDashboardFacade
 *
 * Aggregates metrics from three separate subsystems:
 *   - UserManager    (user counts, starred blogs/entries)
 *   - WeblogManager  (weblog counts, most-commented weblogs)
 *   - WeblogEntryManager (entry counts, comment counts, categories)
 *
 * The controller (AdminDashboard action) calls only this one class.
 * It never needs to know which manager provides which metric.
 */
public class AdminDashboardFacade {

    private static final Log log = LogFactory.getLog(AdminDashboardFacade.class);

    private final UserManager        userManager;
    private final WeblogManager      weblogManager;
    private final WeblogEntryManager entryManager;

    public AdminDashboardFacade(UserManager userManager,
                                WeblogManager weblogManager,
                                WeblogEntryManager entryManager) {
        this.userManager   = userManager;
        this.weblogManager = weblogManager;
        this.entryManager  = entryManager;
    }

    /**
     * Collect and return all dashboard metrics as one AdminDashboardData object.
     */
    public AdminDashboardData collectMetrics() {
        AdminDashboardData data = new AdminDashboardData();

        // --- Metric 1: Total users (UserManager) ---
        try {
            data.setTotalUsers(userManager.getUserCount());
        } catch (WebloggerException e) {
            log.warn("Could not get user count", e);
        }

        // --- Metric 2: Total weblogs (WeblogManager) ---
        try {
            data.setTotalWeblogs(weblogManager.getWeblogCount());
        } catch (WebloggerException e) {
            log.warn("Could not get weblog count", e);
        }

        // --- Metric 3: Total entries (WeblogEntryManager) ---
        try {
            data.setTotalEntries(entryManager.getEntryCount());
        } catch (WebloggerException e) {
            log.warn("Could not get entry count", e);
        }

        // --- Metric 4: Total comments (WeblogEntryManager) ---
        try {
            data.setTotalComments(entryManager.getCommentCount());
        } catch (WebloggerException e) {
            log.warn("Could not get comment count", e);
        }

        // --- Metric 5: Top performing category by entry count (WeblogEntryManager) ---
        try {
            // Get all published entries and tally by category name
            WeblogEntrySearchCriteria wesc = new WeblogEntrySearchCriteria();
            wesc.setStatus(WeblogEntry.PubStatus.PUBLISHED);
            List<WeblogEntry> allEntries = entryManager.getWeblogEntries(wesc);

            Map<String, Long> catCounts = new HashMap<>();
            for (WeblogEntry entry : allEntries) {
                if (entry.getCategory() != null) {
                    String catName = entry.getCategory().getName();
                    catCounts.put(catName, catCounts.getOrDefault(catName, 0L) + 1L);
                }
            }
            String topCat = null;
            long topCount = 0;
            for (Map.Entry<String, Long> e : catCounts.entrySet()) {
                if (e.getValue() > topCount) {
                    topCount = e.getValue();
                    topCat   = e.getKey();
                }
            }
            data.setTopCategory(topCat != null ? topCat : "N/A");
            data.setTopCategoryCount(topCount);
        } catch (WebloggerException e) {
            log.warn("Could not compute top category", e);
            data.setTopCategory("N/A");
        }

        // --- Metric 6: Most starred blog (UserManager) ---
        try {
            List<Object[]> topWeblogs = userManager.getTopStarredWeblogs(1);
            if (topWeblogs != null && !topWeblogs.isEmpty()) {
                data.setMostStarredBlog((Weblog) topWeblogs.get(0)[0]);
                data.setMostStarredBlogCount((Long) topWeblogs.get(0)[1]);
            }
        } catch (WebloggerException e) {
            log.warn("Could not get most starred blog", e);
        }

        // --- Metric 7: Most starred entry (UserManager) ---
        try {
            List<Object[]> topEntries = userManager.getTopStarredWeblogEntries(1);
            if (topEntries != null && !topEntries.isEmpty()) {
                data.setMostStarredEntry((WeblogEntry) topEntries.get(0)[0]);
                data.setMostStarredEntryCount((Long) topEntries.get(0)[1]);
            }
        } catch (WebloggerException e) {
            log.warn("Could not get most starred entry", e);
        }

        // --- Metric 8: Top 3 most active users by entry count (UserManager + WeblogManager) ---
        try {
            List<Object[]> activeUsers = new ArrayList<>();
            // Get all weblogs; for each weblog, get its users and entry count
            List<Weblog> allWeblogs = weblogManager.getWeblogs(true, true, null, null, 0, -1);
            Map<String, Long> userEntryCounts = new HashMap<>();

            for (Weblog weblog : allWeblogs) {
                List<WeblogPermission> perms = userManager.getWeblogPermissions(weblog);
                long entryCount = entryManager.getEntryCount(weblog);
                for (WeblogPermission perm : perms) {
                    String uname = perm.getUser().getUserName();
                    userEntryCounts.put(uname,
                        userEntryCounts.getOrDefault(uname, 0L) + entryCount);
                }
            }

            // Sort descending and take top 3
            List<Map.Entry<String, Long>> sorted = new ArrayList<>(userEntryCounts.entrySet());
            sorted.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));

            for (int i = 0; i < Math.min(3, sorted.size()); i++) {
                activeUsers.add(new Object[]{ sorted.get(i).getKey(), sorted.get(i).getValue() });
            }
            data.setTopActiveUsers(activeUsers);
        } catch (WebloggerException e) {
            log.warn("Could not compute top active users", e);
            data.setTopActiveUsers(new ArrayList<>());
        }

        // --- Metric 9: Most commented weblog (WeblogManager) ---
        try {
            List<StatCount> mostCommented = weblogManager.getMostCommentedWeblogs(null, null, 0, 1);
            if (mostCommented != null && !mostCommented.isEmpty()) {
                data.setMostCommentedBlogName(mostCommented.get(0).getSubjectNameLong());
                data.setMostCommentedBlogCount(mostCommented.get(0).getCount());

            }
        } catch (WebloggerException e) {
            log.warn("Could not get most commented weblog", e);
        }

        return data;
    }
}
