
package org.apache.roller.weblogger.ui.struts2.admin;

import java.util.Arrays;
import java.util.List;

/**
 * STRATEGY PATTERN — Full View
 *
 * Shows all available dashboard metrics.
 */
public class FullViewStrategy implements DashboardViewStrategy {

    @Override
    public List<String> getMetricsToDisplay() {
        return Arrays.asList(
            "totalUsers",
            "totalWeblogs",
            "totalEntries",
            "totalComments",
            "topCategory",
            "mostStarredBlog",
            "mostStarredEntry",
            "topActiveUsers",
            "mostCommentedBlog"
        );
    }

    @Override
    public String getViewLabel() {
        return "Full View";
    }
}
