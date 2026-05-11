package org.apache.roller.weblogger.business.notification;

import org.apache.roller.weblogger.pojos.BugReport;

/**
 * Interface for notification channels.
 * 
 * DESIGN JUSTIFICATION:
 * This interface is the key to extensibility. To add a new notification
 * channel (Slack, MS Teams, SMS etc.), simply implement this interface.
 * No existing code needs to change - Open/Closed Principle.
 * 
 * Example: Adding Slack later =
 *   class SlackNotificationChannel implements NotificationChannel { ... }
 * That's it. Nothing else changes.
 */
public interface NotificationChannel {

    /**
     * Send a notification about a bug report event.
     * @param bug    The bug report involved
     * @param event  The event type: "SUBMITTED", "RESOLVED", "DELETED"
     * @param target Who to notify: "ADMIN" or "USER"
     */
    void sendNotification(BugReport bug, String event, String target) throws Exception;

    /**
     * Human-readable name of this channel (for logging/debugging)
     */
    String getChannelName();
}
