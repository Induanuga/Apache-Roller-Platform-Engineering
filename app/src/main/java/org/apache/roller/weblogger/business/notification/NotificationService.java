package org.apache.roller.weblogger.business.notification;

import org.apache.roller.weblogger.pojos.BugReport;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Manages all notification channels.
 *
 * DESIGN JUSTIFICATION:
 * NotificationService holds a list of NotificationChannels.
 * To add Slack/Teams, just register a new channel - no other code changes.
 * This is the Observer Pattern in action.
 */
public class NotificationService {

    private static final Logger log = Logger.getLogger(NotificationService.class.getName());
    private final List<NotificationChannel> channels = new ArrayList<>();

    /** Register a notification channel (Email, Slack, Teams, etc.) */
    public void registerChannel(NotificationChannel channel) {
        channels.add(channel);
        log.info("Registered notification channel: " + channel.getChannelName());
    }

    /** Notify all registered channels about a bug event */
    public void notifyAll(BugReport bug, String event, String target) {
        for (NotificationChannel channel : channels) {
            try {
                channel.sendNotification(bug, event, target);
            } catch (Exception e) {
                // One channel failing should NOT stop others
                log.warning("Channel " + channel.getChannelName() + " failed: " + e.getMessage());
            }
        }
    }
}
