package com.toyota.config;


import org.aeonbits.owner.Config;

import java.util.List;
import java.util.Set;

@Config.Sources("classpath:application.properties")
public interface ServerConfig extends Config {

    @Key("server.port")
    @DefaultValue("8090")
    int serverPort();

    @Key("TCP_USDTRY.bid")
    double usdTryBid();

    @Key("TCP_USDTRY.ask")
    double usdTryAsk();

    @Key("TCP_EURUSD.bid")
    double eurUsdBid();

    @Key("TCP_EURUSD.ask")
    double eurUsdAsk();

    @Key("TCP_GBPUSD.bid")
    double gbpUsdBid();

    @Key("TCP_GBPUSD.ask")
    double gbpUsdAsk();

    @Key("publish.frequency")
    int publishFrequency();

    @Key("currency.pairs")
    Set<String> currencyPairs();

    @Key("user.credentials")
    List<String> userCredentials();

}
