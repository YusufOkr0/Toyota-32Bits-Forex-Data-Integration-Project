package com.toyota.config;


import com.toyota.exception.ConfigFileLoadingException;
import com.toyota.exception.ConfigFileNotFoundException;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;


/**
 * Singleton configuration loader that reads application settings from a properties file.
 * <p>
 * Configuration values can be overridden by environment variables using an uppercase format
 * with dots replaced by underscores. For example, the property key {@code exchange.rates}
 * can be overridden by the environment variable {@code EXCHANGE_RATES}.
 * </p>
 *
 * <p>
 * The default configuration file is {@code application.properties} and it must be located in the classpath.
 * </p>
 */
public class ApplicationConfig {

    private static final String CONFIG_FILE_NAME = "application.properties";
    private final Properties properties = new Properties();

    private static ApplicationConfig instance;

    private ApplicationConfig() {
        loadProperties();
    }

    public static ApplicationConfig getInstance() {
        if (instance == null) {
            instance = new ApplicationConfig();
        }
        return instance;
    }

    public String getValue(String key) {
        String envKey = key.toUpperCase().replace(".", "_");
        String envValue = System.getenv(envKey);
        if (envValue != null) {
            return envValue;
        }

        String propValue = properties.getProperty(key);
        if (propValue == null) {
            throw new IllegalStateException("Configuration value not found for key: " + key);
        }

        return propValue;
    }

    public int getIntValue(String key) {
        String value = getValue(key);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(String.format("Invalid integer value for %s: %s", key, value));
        }
    }

    public List<String> getExchangeRates() {
        String exchangeRates = getValue("exchange.rates");
        if (exchangeRates != null && !exchangeRates.isBlank()) {
            return Arrays.asList(exchangeRates.split(","));
        }
        return new ArrayList<>();
    }

    private void loadProperties() {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE_NAME)) {
            if (inputStream == null) {
                throw new ConfigFileNotFoundException("Configuration file not found: " + CONFIG_FILE_NAME);
            }
            properties.load(inputStream);
        } catch (IOException e) {
            throw new ConfigFileLoadingException("Failed to load configuration file: " + CONFIG_FILE_NAME);
        }
    }
}
