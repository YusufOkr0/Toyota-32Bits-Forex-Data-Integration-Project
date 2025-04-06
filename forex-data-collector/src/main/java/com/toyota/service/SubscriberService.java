package com.toyota.service;


public interface SubscriberService  {

    void connect(String platformName);

    void disConnect();

    void subscribe(String platformName, String rateName);

    void unSubscribe(String platformName, String rateName);


}
