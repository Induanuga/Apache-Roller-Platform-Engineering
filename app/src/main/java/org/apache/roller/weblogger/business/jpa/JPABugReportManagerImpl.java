package org.apache.roller.weblogger.business.jpa;

import java.util.Date;
import java.util.List;
import jakarta.persistence.TypedQuery;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.roller.weblogger.WebloggerException;
import org.apache.roller.weblogger.business.BugReportManager;
import org.apache.roller.weblogger.business.notification.EmailNotificationChannel;
import org.apache.roller.weblogger.business.notification.NotificationService;
import org.apache.roller.weblogger.pojos.BugReport;

@com.google.inject.Singleton
public class JPABugReportManagerImpl implements BugReportManager {

    private static final Log log = LogFactory.getFactory().getInstance(JPABugReportManagerImpl.class);

    private final JPAPersistenceStrategy strategy;
    private final NotificationService notificationService;

    @com.google.inject.Inject
    protected JPABugReportManagerImpl(JPAPersistenceStrategy strategy) {
        log.debug("Instantiating JPA BugReport Manager");
        this.strategy = strategy;

        // Set up notification service with email channel
        // To add Slack later: notificationService.registerChannel(new SlackNotificationChannel())
        this.notificationService = new NotificationService();
        this.notificationService.registerChannel(new EmailNotificationChannel());
    }

    @Override
    public void submitBugReport(BugReport bug) throws WebloggerException {
        try {
            strategy.store(bug);
            strategy.flush();
            // Notify admin and confirm to user
            notificationService.notifyAll(bug, "SUBMITTED", "ADMIN");
            notificationService.notifyAll(bug, "SUBMITTED", "USER");
        } catch (Exception e) {
            throw new WebloggerException("Error submitting bug report", e);
        }
    }

    @Override
    public BugReport getBugReport(String id) throws WebloggerException {
        return (BugReport) strategy.load(BugReport.class, id);
    }

    @Override
    public List<BugReport> getAllBugReports() throws WebloggerException {
        TypedQuery<BugReport> query = strategy.getNamedQuery(
            "BugReport.getAll", BugReport.class);
        return query.getResultList();
    }

    @Override
    public List<BugReport> getBugReportsByUser(String username) throws WebloggerException {
        TypedQuery<BugReport> query = strategy.getNamedQuery(
            "BugReport.getByUser", BugReport.class);
        query.setParameter(1, username);
        return query.getResultList();
    }

    @Override
    public void resolveBugReport(String id, String adminNotes) throws WebloggerException {
        try {
            BugReport bug = getBugReport(id);
            if (bug == null) {
                throw new WebloggerException("Bug report not found: " + id);
            }
            bug.setStatus(BugReport.Status.RESOLVED);
            bug.setResolvedDate(new Date());
            bug.setAdminNotes(adminNotes);
            strategy.store(bug);
            strategy.flush();
            // Notify admin and user of resolution
            notificationService.notifyAll(bug, "RESOLVED", "ADMIN");
            notificationService.notifyAll(bug, "RESOLVED", "USER");
        } catch (WebloggerException e) {
            throw e;
        } catch (Exception e) {
            throw new WebloggerException("Error resolving bug report", e);
        }
    }

    @Override
    public void deleteBugReport(String id) throws WebloggerException {
        try {
            BugReport bug = getBugReport(id);
            if (bug == null) {
                throw new WebloggerException("Bug report not found: " + id);
            }
            strategy.remove(bug);
            strategy.flush();
            // Notify admin of deletion
            notificationService.notifyAll(bug, "DELETED", "ADMIN");
        } catch (WebloggerException e) {
            throw e;
        } catch (Exception e) {
            throw new WebloggerException("Error deleting bug report", e);
        }
    }

    @Override
    public void updateBugReport(BugReport bug) throws WebloggerException {
        try {
            strategy.store(bug);
            strategy.flush();
            notificationService.notifyAll(bug, "MODIFIED", "ADMIN");
        } catch (Exception e) {
            throw new WebloggerException("Error updating bug report", e);
        }
    }

    public void release() {}
}
