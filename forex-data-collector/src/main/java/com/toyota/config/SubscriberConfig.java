package com.toyota.config;

import com.toyota.exception.MissingConfigurationException;

import java.util.List;
import java.util.Map;


public class SubscriberConfig {
    private String platformName;
    private String className;
    private int connectionRetryLimit;
    private int retryDelaySeconds;
    private List<String> exchangeRates;
    private Map<String,Object> properties;

    public SubscriberConfig(String platformName, String className, List<String> exchangeRates, Map<String, Object> properties) {
        this.platformName = platformName;
        this.className = className;
        this.exchangeRates = exchangeRates;
        this.properties = properties;
    }

    public SubscriberConfig() {
    }

    @SuppressWarnings("unchecked")
    public <T> T getProperty(String key,Class<T> type){
        Object value = properties.get(key);
        if (value == null) {
            throw new MissingConfigurationException(String.format("Missing required property: '%s' ",key));
        }
        if(!type.isInstance(value)){
            throw new IllegalArgumentException(String.format("Property '%s' is not of type '%s'.",key, type.getSimpleName()));
        }
        return (T) value;
    }

    public String getPlatformName() {
        return platformName;
    }

    public void setPlatformName(String platformName) {
        this.platformName = platformName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public List<String> getExchangeRates() {
        return exchangeRates;
    }

    public void setExchangeRates(List<String> exchangeRates) {
        this.exchangeRates = exchangeRates;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public int getConnectionRetryLimit() {
        return connectionRetryLimit;
    }

    public void setConnectionRetryLimit(int connectionRetryLimit) {
        this.connectionRetryLimit = connectionRetryLimit;
    }

    public int getRetryDelaySeconds() {
        return retryDelaySeconds;
    }

    public void setRetryDelaySeconds(int retryDelaySeconds) {
        this.retryDelaySeconds = retryDelaySeconds;
    }
}
