package org.apache.roller.weblogger.ui.struts2.editor;

import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.roller.weblogger.business.BugReportManager;
import org.apache.roller.weblogger.business.WebloggerFactory;
import org.apache.roller.weblogger.ui.struts2.util.UIAction;

/**
 * User-facing action for submitting and managing their own bug reports.
 */
public class BugReportAction extends UIAction {

    private static Log log = LogFactory.getLog(BugReportAction.class);

    private String bugId;
    private String title;
    private String description;
    private String pageUrl;
    private String bugType;
    private List<org.apache.roller.weblogger.pojos.BugReport> myBugReports;

    public BugReportAction() {
        this.actionName = "bugReport";
        this.desiredMenu = "editor";
        this.pageTitle = "bugReport.title";
    }

    @Override
    public boolean isWeblogRequired() { return false; }

    @Override
    public String execute() {
        try {
            BugReportManager mgr = WebloggerFactory.getWeblogger().getBugReportManager();
            setMyBugReports(mgr.getBugReportsByUser(getAuthenticatedUser().getUserName()));
        } catch (Exception ex) {
            log.error("Error loading bug reports", ex);
            addError("Error loading bug reports");
        }
        return LIST;
    }

    public String submit() {
        try {
            BugReportManager mgr = WebloggerFactory.getWeblogger().getBugReportManager();
            org.apache.roller.weblogger.pojos.BugReport bug =
                new org.apache.roller.weblogger.pojos.BugReport();
            bug.setTitle(title);
            bug.setDescription(description);
            bug.setPageUrl(pageUrl);
            bug.setBugType(org.apache.roller.weblogger.pojos.BugReport.BugType.valueOf(
                bugType != null ? bugType : "OTHER"));
            bug.setSubmittedBy(getAuthenticatedUser().getUserName());
            bug.setSubmitterEmail(getAuthenticatedUser().getEmailAddress());
            mgr.submitBugReport(bug);
            WebloggerFactory.getWeblogger().flush();
            addMessage("Bug report submitted successfully!");
        } catch (Exception ex) {
            log.error("Error submitting bug report", ex);
            addError("Error submitting bug report");
        }
        return execute();
    }

    public String delete() {
        try {
            BugReportManager mgr = WebloggerFactory.getWeblogger().getBugReportManager();
            org.apache.roller.weblogger.pojos.BugReport bug = mgr.getBugReport(bugId);
            if (bug != null && bug.getSubmittedBy().equals(getAuthenticatedUser().getUserName())) {
                mgr.deleteBugReport(bugId);
                WebloggerFactory.getWeblogger().flush();
                addMessage("Bug report deleted.");
            } else {
                addError("You can only delete your own bug reports.");
            }
        } catch (Exception ex) {
            log.error("Error deleting bug report", ex);
            addError("Error deleting bug report");
        }
        return execute();
    }

    public String getBugId() { return bugId; }
    public void setBugId(String bugId) { this.bugId = bugId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getPageUrl() { return pageUrl; }
    public void setPageUrl(String pageUrl) { this.pageUrl = pageUrl; }
    public String getBugType() { return bugType; }
    public void setBugType(String bugType) { this.bugType = bugType; }
    public List<org.apache.roller.weblogger.pojos.BugReport> getMyBugReports() { return myBugReports; }
    public void setMyBugReports(List<org.apache.roller.weblogger.pojos.BugReport> r) { this.myBugReports = r; }
}
