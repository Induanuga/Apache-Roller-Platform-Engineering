package org.apache.roller.weblogger.business.notification;

import org.apache.roller.weblogger.pojos.BugReport;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

public class EmailNotificationChannel implements NotificationChannel {

    private static final Logger log = Logger.getLogger(EmailNotificationChannel.class.getName());
    private static final String API_KEY = System.getenv("SENDINBLUE_API_KEY");
    private static final String ADMIN_EMAIL = "sherley21.vizag@gmail.com";
    private static final String FROM_EMAIL = "sherley21.vizag@gmail.com";
    private static final String FROM_NAME = "Bug Tracker";
    private static final String ENDPOINT = "https://api.brevo.com/v3/smtp/email";

    @Override
    public String getChannelName() { return "email"; }

    @Override
    public void sendNotification(BugReport bug, String event, String target) throws Exception {
        String toEmail;
        String subject;
        String body;
        if ("ADMIN".equals(target)) {
            toEmail = ADMIN_EMAIL;
            subject = "[Bug Tracker] Bug " + event + ": " + bug.getTitle();
            body = buildAdminEmailBody(bug, event);
        } else {
            toEmail = bug.getSubmitterEmail();
            subject = "[Bug Tracker] Your bug report status: " + bug.getStatus();
            body = buildUserEmailBody(bug, event);
        }
        sendEmail(toEmail, subject, body);
    }

    private String buildAdminEmailBody(BugReport bug, String event) {
        return "A bug report has been " + event + ".\n\n"
            + "Title: " + bug.getTitle() + "\n"
            + "Type: " + bug.getBugType() + "\n"
            + "Status: " + bug.getStatus() + "\n"
            + "Submitted by: " + bug.getSubmittedBy() + "\n"
            + "Email: " + bug.getSubmitterEmail() + "\n"
            + "Description: " + bug.getDescription() + "\n"
            + "Page URL: " + bug.getPageUrl() + "\n";
    }

    private String buildUserEmailBody(BugReport bug, String event) {
        String msg = "Hello " + bug.getSubmittedBy() + ",\n\n"
            + "Your bug report has been " + event + ".\n\n"
            + "Title: " + bug.getTitle() + "\n"
            + "Status: " + bug.getStatus() + "\n";
        if (bug.getAdminNotes() != null && !bug.getAdminNotes().isEmpty()) {
            msg += "Admin notes: " + bug.getAdminNotes() + "\n";
        }
        msg += "\nThank you for helping improve the platform.";
        return msg;
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private void sendEmail(String toEmail, String subject, String body) throws Exception {
        String json = "{"
            + "\"sender\":{\"name\":\"" + escapeJson(FROM_NAME) + "\",\"email\":\"" + escapeJson(FROM_EMAIL) + "\"},"
            + "\"to\":[{\"email\":\"" + escapeJson(toEmail) + "\"}],"
            + "\"subject\":\"" + escapeJson(subject) + "\","
            + "\"textContent\":\"" + escapeJson(body) + "\""
            + "}";

        URL url = new URL(ENDPOINT);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("api-key", API_KEY);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(json.getBytes(StandardCharsets.UTF_8));
        }
        int responseCode = conn.getResponseCode();
        if (responseCode == 200 || responseCode == 201) {
            log.info("Email sent via Brevo to " + toEmail + " | Subject: " + subject);
        } else {
            log.warning("Brevo email failed to " + toEmail + " | Response: " + responseCode);
        }
    }
}
