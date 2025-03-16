package com.toyota.datacollector.entity;


public enum RateStatus {

    SUBSCRIBED("subscribed"),
    UNSUBSCRIBED("unsubscribed"),
    UPDATED("updated");

    private final String status;

    RateStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
