package org.apache.roller.weblogger.business;

import org.apache.roller.weblogger.pojos.BugReport;
import java.util.List;

/**
 * Interface for managing bug reports.
 * Keeps business logic separate from persistence - follows Single Responsibility Principle.
 */
public interface BugReportManager {

    /** Submit a new bug report */
    void submitBugReport(BugReport bug) throws Exception;

    /** Get a single bug report by ID */
    BugReport getBugReport(String id) throws Exception;

    /** Get all bug reports (admin use) */
    List<BugReport> getAllBugReports() throws Exception;

    /** Get all bug reports submitted by a specific user */
    List<BugReport> getBugReportsByUser(String username) throws Exception;

    /** Mark a bug as resolved (admin only) */
    void resolveBugReport(String id, String adminNotes) throws Exception;

    /** Delete a bug report */
    void deleteBugReport(String id) throws Exception;

    /** Update an existing bug report */
    void updateBugReport(BugReport bug) throws Exception;
}
