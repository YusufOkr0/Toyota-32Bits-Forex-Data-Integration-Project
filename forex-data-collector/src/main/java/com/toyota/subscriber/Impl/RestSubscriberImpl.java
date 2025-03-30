package com.toyota.subscriber.Impl;

import com.toyota.config.ConfigUtil;
import com.toyota.coordinator.CoordinatorService;
import com.toyota.subscriber.SubscriberService;


import java.util.HashSet;
import java.util.Set;

public class RestSubscriberImpl implements SubscriberService {

    private final String BASE_URL;
    private String API_KEY;

    private final Set<String> receivedRates;
    private final CoordinatorService coordinator;

    public RestSubscriberImpl(CoordinatorService coordinator) {
        this.coordinator = coordinator;
        this.receivedRates = new HashSet<>();

        this.BASE_URL = ConfigUtil.getValue("rest.platform.base.url");
    }

    @Override
    public void connect(String platformName, String username, String password) {

    }

    @Override
    public void disConnect(String platformName, String username, String password) {

    }

    @Override
    public void subscribe(String platformName, String rateName) {
    }

    @Override
    public void unSubscribe(String platformName, String rateName) {

    }


}