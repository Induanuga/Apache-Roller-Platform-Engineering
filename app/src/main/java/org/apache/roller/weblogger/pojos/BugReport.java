package org.apache.roller.weblogger.pojos;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

/**
 * Represents a bug report submitted by a user.
 * Mapped to bug_report table via BugReport.orm.xml
 */
public class BugReport implements Serializable {

    public enum Status { OPEN, RESOLVED }
    public enum BugType { BROKEN_BUTTON, BROKEN_LINK, UI_ISSUE, PERFORMANCE, DATA_ISSUE, SECURITY, OTHER }

    private String id;
    private String title;
    private String description;
    private String pageUrl;
    private BugType bugType;
    private Status status;
    private String submittedBy;
    private String submitterEmail;
    private Date submittedDate;
    private Date resolvedDate;
    private String adminNotes;

    public BugReport() {
        this.id = UUID.randomUUID().toString();
        this.status = Status.OPEN;
        this.submittedDate = new Date();
        this.bugType = BugType.OTHER;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getPageUrl() { return pageUrl; }
    public void setPageUrl(String pageUrl) { this.pageUrl = pageUrl; }
    public BugType getBugType() { return bugType; }
    public void setBugType(BugType bugType) { this.bugType = bugType; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public String getSubmittedBy() { return submittedBy; }
    public void setSubmittedBy(String submittedBy) { this.submittedBy = submittedBy; }
    public String getSubmitterEmail() { return submitterEmail; }
    public void setSubmitterEmail(String submitterEmail) { this.submitterEmail = submitterEmail; }
    public Date getSubmittedDate() { return submittedDate; }
    public void setSubmittedDate(Date submittedDate) { this.submittedDate = submittedDate; }
    public Date getResolvedDate() { return resolvedDate; }
    public void setResolvedDate(Date resolvedDate) { this.resolvedDate = resolvedDate; }
    public String getAdminNotes() { return adminNotes; }
    public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }
    public boolean isResolved() { return Status.RESOLVED.equals(this.status); }
}
