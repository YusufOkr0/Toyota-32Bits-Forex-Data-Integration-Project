package com.toyota.service;


/**
 * Defines a contract for services responsible for sending email notifications.
 * This interface is primarily focused on sending operational alerts, such as
 * connection failures, to designated recipients.
 */
public interface MailSender {
    /**
     * Sends a notification indicating that attempts to connect to a specific
     * platform have repeatedly failed, reaching a configured retry limit.
     *
     * <p>This notification signals a persistent problem requiring attention.
     * Crucially, sending this notification does <b>not</b> necessarily mean that the system
     * has stopped attempting to connect; it indicates that a significant failure
     * threshold has been reached, even if retries continue according to the
     * system's logic.</p>
     *
     * @param platformName         The name of the platform experiencing persistent
     *                             connection issues for which the retry limit was reached.
     * @param connectionRetryLimit The specific number of consecutive failed connection attempts
     *                             that triggered this notification.
     * @param retryDelaySeconds    The configured delay in seconds that was used between
     *                             the failed connection attempts leading up to this notification,
     *                             providing context on the retry strategy.
     */
    void sendConnectionFailureNotification(String platformName, int connectionRetryLimit, int retryDelaySeconds);
}
