package com.toyota.service;

public interface MailSender {
    void sendConnectionFailureNotification(String platformName, int connectionRetryLimit, int retryDelaySeconds);
}
