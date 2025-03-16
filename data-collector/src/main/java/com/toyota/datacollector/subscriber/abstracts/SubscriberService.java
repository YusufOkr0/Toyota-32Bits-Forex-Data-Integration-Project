package com.toyota.datacollector.subscriber.abstracts;


public interface SubscriberService  {

    void connect(String platformName, String username, String password);

    void disConnect(String platformName, String username, String password);

    void subscribe(String platformName, String rateName);

    void unSubscribe(String platformName, String rateName);


}
