package com.toyota.datacollector.config;

import lombok.Data;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@Data
public class ConfigLoader {

    @Value("${tcp.platform.host}")
    private String tcpServerHost;

    @Value("${tcp.platform.port}")
    private int tcpServerPort;

    @Value("#{'${tcp.platform.rates}'.split(',')}")
    private List<String> tcpServerRates;



}
