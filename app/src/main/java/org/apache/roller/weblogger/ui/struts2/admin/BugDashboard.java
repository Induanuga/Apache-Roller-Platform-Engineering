package org.apache.roller.weblogger.ui.struts2.admin;

import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.roller.weblogger.business.BugReportManager;
import org.apache.roller.weblogger.business.WebloggerFactory;
import org.apache.roller.weblogger.ui.struts2.util.UIAction;

/**
 * Admin-facing action for managing all bug reports.
 * Only accessible by Global Admin users.
 */
public class BugDashboard extends UIAction {

    private static Log log = LogFactory.getLog(BugDashboard.class);

    private String bugId;
    private String adminNotes;
    private List<org.apache.roller.weblogger.pojos.BugReport> allBugReports;

    public BugDashboard() {
        this.actionName = "bugDashboard";
        this.desiredMenu = "admin";
        this.pageTitle = "bugDashboard.title";
    }

    /** Show all bug reports */
    @Override
    public boolean isWeblogRequired() { return false; }

    @Override
    public String execute() {
        try {
            BugReportManager mgr = WebloggerFactory.getWeblogger().getBugReportManager();
            setAllBugReports(mgr.getAllBugReports());
        } catch (Exception ex) {
            log.error("Error loading bug reports", ex);
            addError("Error loading all bug reports");
        }
        return LIST;
    }

    /** Mark a bug as resolved */
    public String resolve() {
        try {
            BugReportManager mgr = WebloggerFactory.getWeblogger().getBugReportManager();
            mgr.resolveBugReport(bugId, adminNotes);
            WebloggerFactory.getWeblogger().flush();
            addMessage("Bug report marked as resolved.");
        } catch (Exception ex) {
            log.error("Error resolving bug report", ex);
            addError("Error resolving bug report");
        }
        return execute();
    }

    /** Delete any bug report (admin privilege) */
    public String delete() {
        try {
            BugReportManager mgr = WebloggerFactory.getWeblogger().getBugReportManager();
            mgr.deleteBugReport(bugId);
            WebloggerFactory.getWeblogger().flush();
            addMessage("Bug report deleted.");
        } catch (Exception ex) {
            log.error("Error deleting bug report", ex);
            addError("Error deleting bug report");
        }
        return execute();
    }

    // Getters and Setters
    public String getBugId() { return bugId; }
    public void setBugId(String bugId) { this.bugId = bugId; }
    public String getAdminNotes() { return adminNotes; }
    public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }
    public List<org.apache.roller.weblogger.pojos.BugReport> getAllBugReports() { return allBugReports; }
    public void setAllBugReports(List<org.apache.roller.weblogger.pojos.BugReport> allBugReports) { this.allBugReports = allBugReports; }
}
