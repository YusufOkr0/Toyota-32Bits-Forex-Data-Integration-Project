package com.toyota.service.Impl;

import com.toyota.config.ApplicationConfig;
import com.toyota.service.MailSender;
import jakarta.mail.*;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

public class EmailSenderImpl implements MailSender {

    public static final Logger log = LogManager.getLogger(EmailSenderImpl.class);
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String username;
    private final String password;
    private final String recipientsTo;

    private final Properties properties;

    public EmailSenderImpl(ApplicationConfig appConfig){
        this.username = appConfig.getValue("forex.email.username");
        this.password = appConfig.getValue("forex.email.password");
        this.recipientsTo = appConfig.getValue("forex.email.recipients.to");

        String host = appConfig.getValue("forex.email.host");
        String port = appConfig.getValue("forex.email.port");
        boolean useTls = Boolean.parseBoolean(appConfig.getValue("forex.email.smtp.starttls.enable"));
        boolean useSsl = Boolean.parseBoolean(appConfig.getValue("forex.email.smtp.ssl.enable"));

        this.properties = new Properties();
        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.port", port);
        properties.put("mail.smtp.auth", "true");
        if (useTls) { properties.put("mail.smtp.starttls.enable", "true"); }
        if (useSsl) { properties.put("mail.smtp.ssl.enable", "true"); }

    }


    @Override
    public void sendConnectionFailureNotification(String platformName, int connectionRetryLimit, int retryDelaySeconds) {
        if (recipientsTo == null || recipientsTo.trim().isEmpty()) {
            log.warn("Cannot send connection failure notification for platform '{}': No recipients configured.", platformName);
            return;
        }

        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });


        try {
            MimeMessage e_mail = new MimeMessage(session);

            e_mail.setFrom(new InternetAddress(username));

            e_mail.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientsTo));

            String subject = String.format("CRITICAL ALERT: Connection to %s Platform Failed and Has Been Halted", platformName);
            e_mail.setSubject(subject);

            String timestamp = LocalDateTime.now().format(DTF);

            String content = createHtmlContent(platformName, connectionRetryLimit, retryDelaySeconds, timestamp);
            e_mail.setContent(content, "text/html; charset=utf-8");

            Transport.send(e_mail);

        } catch (MessagingException e) {
            log.error("Failed to send connection failure notification email for platform '{}' to recipients '{}'. Error: {}",
                    platformName, recipientsTo, e.getMessage());
        }
    }

    private String createHtmlContent(String platformName, int limit, int delay, String timestamp) {
        return String.format("""
        <!DOCTYPE html>
        <html>
        <head>
        <title>ALERT: Persistent Platform Connection Failures</title>
        <style>
          body { font-family: sans-serif; line-height: 1.6; }
          .container { padding: 20px; border: 1px solid #ddd; max-width: 600px; margin: 20px auto; background-color: #fff; }
          /* --- Header Background Color Changed to Red --- */
          .header { background-color: #d9534f; /* Red for Alert Emphasis */ color: white; padding: 10px 15px; text-align: center; border-radius: 3px 3px 0 0; }
          .header h1 { margin: 0; font-size: 1.5em; }
          .details { margin-top: 20px; padding: 0 15px; }
          .details p { margin: 10px 0; }
          .details strong { display: inline-block; min-width: 200px; color: #333; font-weight: bold; }
          .platform-name { font-weight: bold; color: #d9534f; } /* Keep red for platform name emphasis */
          .status { margin-top: 15px; padding: 10px; background-color: #fcf8e3; border: 1px solid #faebcc; color: #8a6d3b; border-radius: 3px; font-weight: bold; text-align: center; }
          .impact { margin-top: 20px; padding: 15px; background-color: #f9f9f9; border: 1px solid #eee; color: #555; border-radius: 3px; }
          .impact strong { color: #333; }
          .action { margin-top: 20px; padding: 0 15px; }
          .action ul { padding-left: 20px; margin-top: 5px; }
          .footer { margin-top: 25px; font-size: 0.85em; text-align: center; color: #777; padding-top: 15px; border-top: 1px solid #eee;}
        </style>
        </head>
        <body>
        <div class="container">
          <div class="header"> <!-- This div now has the red background -->
            <h1>PERSISTENT CONNECTION FAILURES</h1>
          </div>
          <div class="details">
            <p>Dear Administrator,</p>
            <p>The Forex Data Collection Service is encountering persistent difficulties connecting to the following platform:</p>
            <p><strong>Affected Platform:</strong> <span class="platform-name">%s</span></p>
            <p>The service reached the configured retry limit after multiple unsuccessful attempts.</p>
            <p><strong>Retry Limit Reached:</strong> %d attempts</p>
            <p><strong>Delay Between Attempts:</strong> %d seconds</p>
            <p><strong>Notification Timestamp:</strong> %s</p>
          </div>
          <div class="status">
             ATTENTION: The service has logged this failure and will **continue attempting to connect** periodically. This notification indicates an ongoing problem requiring investigation.
          </div>
          <div class="impact">
             <strong>Impact:</strong> The recurring inability to establish a connection with the '<span class="platform-name">%s</span>' platform means that <strong>live exchange rate data from this source is currently not being received.</strong> This complete interruption directly affects the accuracy and timeliness of all dependent calculations, reports, and operations that rely on this data feed. The persistence of these failures strongly indicates an underlying issue (e.g., with the platform itself, network connectivity, or configuration) that requires investigation to restore the critical data flow.
          </div>
          <div class="action">
            <p><strong>Recommended Investigation Steps:</strong></p>
            <ul>
              <li>Verify the operational status and accessibility of the '<span class="platform-name">%s</span>' platform externally.</li>
              <li>Check network connectivity (firewalls, routing) between the application server and the platform.</li>
              <li>Review application logs (especially 'CoordinatorImpl' and subscriber logs for '<span class="platform-name">%s</span>') around the time of failure for specific error messages (e.g., connection refused, timeout, authentication errors).</li>
              <li>Confirm authentication credentials for the platform are correct, if applicable.</li>
              <li>Monitor the frequency of these alerts to understand the persistence of the problem.</li>
            </ul>
          </div>
          <div class="footer">
              This is an automated alert generated by the Forex Data Collection Service. Please do not reply directly to this email.
          </div>
        </div>
        </body>
        </html>
        """,
                platformName,
                limit,
                delay,
                timestamp,
                platformName,
                platformName,
                platformName
        );
    }


}
