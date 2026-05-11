
package org.apache.roller.weblogger.ui.struts2.admin;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.roller.weblogger.business.AdminDashboardData;
import org.apache.roller.weblogger.business.AdminDashboardFacade;
import org.apache.roller.weblogger.business.WebloggerFactory;
import org.apache.roller.weblogger.ui.struts2.util.UIAction;

import java.util.List;

/**
 * Struts2 Action for the Admin Dashboard.
 *
 * Controller responsibilities only:
 *   - Build the Facade (assembles subsystem managers)
 *   - Select the Strategy based on the 'viewMode' request parameter
 *   - Store collected data for the JSP
 *
 * No metric fetching logic lives here — that belongs in AdminDashboardFacade.
 * No display logic lives here — that belongs in DashboardViewStrategy + JSP.
 */
public class AdminDashboard extends UIAction {

    private static final Log log = LogFactory.getLog(AdminDashboard.class);

    /** Request parameter: "minimal" or "full" (default: "full") */
    private String viewMode = "full";

    /** Populated by Facade, read by JSP */
    private AdminDashboardData dashboardData;

    /** Populated by Strategy, read by JSP to decide which sections to show */
    private List<String> visibleMetrics;

    /** Human-readable label of current view mode */
    private String viewLabel;

    public AdminDashboard() {
        this.actionName  = "adminDashboard";
        this.desiredMenu = "admin";
        this.pageTitle   = "adminDashboard.title";
    }

    @Override
    public boolean isWeblogRequired() {
        return false;
    }

    @Override
    public String execute() {
        // 1. Build the Facade — coordinates all subsystem managers
        AdminDashboardFacade facade = new AdminDashboardFacade(
            WebloggerFactory.getWeblogger().getUserManager(),
            WebloggerFactory.getWeblogger().getWeblogManager(),
            WebloggerFactory.getWeblogger().getWeblogEntryManager()
        );

        // 2. Collect all metrics through the Facade
        try {
            dashboardData = facade.collectMetrics();
        } catch (Exception e) {
            log.error("Error collecting dashboard metrics", e);
            addError("Error loading dashboard data.");
            dashboardData = new AdminDashboardData();
        }

        // 3. Select Strategy based on viewMode parameter
        DashboardViewStrategy strategy;
        if ("minimal".equalsIgnoreCase(viewMode)) {
            strategy = new MinimalViewStrategy();
        } else {
            strategy = new FullViewStrategy();
        }

        // 4. Store strategy outputs for the JSP
        visibleMetrics = strategy.getMetricsToDisplay();
        viewLabel      = strategy.getViewLabel();

        return SUCCESS;
    }

    // --- Getters and Setters ---

    public String getViewMode() { return viewMode; }
    public void setViewMode(String viewMode) { this.viewMode = viewMode; }

    public AdminDashboardData getDashboardData() { return dashboardData; }

    public List<String> getVisibleMetrics() { return visibleMetrics; }

    public String getViewLabel() { return viewLabel; }
}
