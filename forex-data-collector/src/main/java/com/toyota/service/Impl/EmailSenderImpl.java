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
                    platformName, recipientsTo, e.getMessage(), e);
        }
    }

    private String createHtmlContent(String platformName, int limit, int delay, String timestamp) {
        return String.format("""
        <!DOCTYPE html>
        <html>
        <head>
        <title>CRITICAL ALERT: Platform Connection Failure</title>
        <style>
          body { font-family: sans-serif; line-height: 1.6; }
          .container { padding: 20px; border: 1px solid #ddd; max-width: 600px; margin: 20px auto; }
          .header { background-color: #d9534f; color: white; padding: 10px 15px; text-align: center; border-radius: 3px 3px 0 0; }
          .details { margin-top: 20px; padding: 0 15px; }
          .details strong { display: inline-block; min-width: 180px; color: #555;}
          .platform-name { font-weight: bold; color: #c9302c; }
          .impact { margin-top: 20px; padding: 15px; background-color: #fcf8e3; border: 1px solid #faebcc; color: #8a6d3b; border-radius: 3px; }
          .action { margin-top: 20px; padding: 0 15px; }
          .action ul { padding-left: 20px; }
          .footer { margin-top: 25px; font-size: 0.85em; text-align: center; color: #777; padding-top: 15px; border-top: 1px solid #eee;}
        </style>
        </head>
        <body>
        <div class="container">
          <div class="header">
            <h1>CRITICAL CONNECTION FAILURE</h1>
          </div>
          <div class="details">
            <p>Dear Administrator,</p>
            <p>The Forex Data Collection Service has reached the maximum retry limit while attempting to connect to the following platform and has <strong>halted further attempts</strong></p>
            <p><strong>Failed Platform:</strong> <span class="platform-name">%s</span></p>
            <p><strong>Maximum Retry Count:</strong> %d</p>
            <p><strong>Retry Delay:</strong> %d seconds</p>
            <p><strong>Failure Timestamp:</strong> %s</p>
          </div>
          <div class="impact">
            <strong>Impact:</strong> Live exchange rate data from this platform will not be available to our system until the issue is resolved. This may affect currency calculations and related operations.
          </div>
          <div class="action">
            <p><strong>Please Take Immediate Action:</strong></p>
            <ul>
              <li>Check the status of the '%s' platform (Is the service running?).</li>
              <li>Review the application logs (especially 'CoordinatorImpl' and related subscriber logs) in detail.</li>
            </ul>
          </div>
          <div class="footer">
              This is an automated alert message. Please do not reply.
          </div>
        </div>
        </body>
        </html>
        """,
                platformName,
                limit,
                delay,
                timestamp,
                platformName
        );
    }


}
