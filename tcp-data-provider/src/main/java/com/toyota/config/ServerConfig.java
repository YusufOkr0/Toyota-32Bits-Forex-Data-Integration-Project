package com.toyota.config;


import org.aeonbits.owner.Config;

import java.util.List;
import java.util.Set;

@Config.Sources("classpath:application.properties")
public interface ServerConfig extends Config {

    @Key("server.port")
    @DefaultValue("8090")
    int serverPort();

    @Key("tcp.usdtry.bid")
    double usdTryBid();

    @Key("tcp.usdtry.ask")
    double usdTryAsk();

    @Key("tcp.usdtry.min-limit")
    double usdTryMinLimit();

    @Key("tcp.usdtry.max-limit")
    double usdTryMaxLimit();

    @Key("tcp.eurusd.bid")
    double eurUsdBid();

    @Key("tcp.eurusd.ask")
    double eurUsdAsk();

    @Key("tcp.eurusd.min-limit")
    double eurUsdMinLimit();

    @Key("tcp.eurusd.max-limit")
    double eurUsdMaxLimit();

    @Key("tcp.gbpusd.bid")
    double gbpUsdBid();

    @Key("tcp.gbpusd.ask")
    double gbpUsdAsk();

    @Key("tcp.gbpusd.min-limit")
    double gbpUsdMinLimit();

    @Key("tcp.gbpusd.max-limit")
    double gbpUsdMaxLimit();

    @Key("publish.frequency")
    int publishFrequency();

    @Key("currency.pairs")
    List<String> currencyPairs();

    @Key("user.credentials")
    List<String> userCredentials();

}
