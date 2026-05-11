
package org.apache.roller.weblogger.ui.struts2.admin;

import java.util.Arrays;
import java.util.List;

/**
 * STRATEGY PATTERN — Minimal View
 *
 * Shows only the two required minimal metrics:
 *   1. Total users
 *   2. Top performing category
 */
public class MinimalViewStrategy implements DashboardViewStrategy {

    @Override
    public List<String> getMetricsToDisplay() {
        return Arrays.asList("totalUsers", "topCategory");
    }

    @Override
    public String getViewLabel() {
        return "Minimal View";
    }
}
