package com.toyota.entity;

/**
 * Enum class to manage server response messages for the FxDataServer.
 * Each enum constant represents a specific response message with a predefined format.
 */
public enum ServerResponse {
    ERROR_NOT_CONNECTED("ERROR|Not Authenticated"),
    ERROR_INVALID_COMMAND("ERROR|Invalid command. Please enter one of these: connect,disconnect,subscribe,unsubscribe"),
    ERROR_INVALID_CREDENTIALS("ERROR|Invalid credentials"),
    ERROR_INVALID_CURRENCY_PAIR("ERROR|Invalid currency pair: %s"),
    ERROR_INVALID_MESSAGE_FORMAT("ERROR|Invalid message format"),
    ERROR_CLIENT_ALREADY_HAS_A_SESSION("ERROR|User already logged in from another session"),
    INFO_NOT_SUBSCRIBED("INFO|Not subscribed to currency pair: %s"),
    INFO_ALREADY_SUBSCRIBED("INFO|Already subscribed to currency pair: %s"),
    INFO_CLIENT_ALREADY_CONNECTED("INFO|User already connected"),
    SUCCESS_SUBSCRIBED("SUCCESS|Subscribed to currency pair: %s"),
    SUCCESS_UNSUBSCRIBED("SUCCESS|Unsubscribed from currency pair: %s"),
    SUCCESS_CONNECTED("SUCCESS|CONNECTED");

    private final String messageTemplate;

    ServerResponse(String messageTemplate) {
        this.messageTemplate = messageTemplate;
    }

    public String getMessage(String... args) {
        return String.format(messageTemplate, (Object[]) args);
    }
}