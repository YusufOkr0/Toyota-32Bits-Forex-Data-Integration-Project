package com.toyota.datacollector.subscriber.concretes;


import com.toyota.datacollector.coordinator.abstracts.CoordinatorService;
import com.toyota.datacollector.subscriber.abstracts.SubscriberService;

public class RestSubscriberImpl implements SubscriberService {

    private final CoordinatorService coordinator;


    public RestSubscriberImpl(CoordinatorService coordinator) {
        this.coordinator = coordinator;
    }


    @Override
    public void connect(String platformName, String username, String password)  {


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
