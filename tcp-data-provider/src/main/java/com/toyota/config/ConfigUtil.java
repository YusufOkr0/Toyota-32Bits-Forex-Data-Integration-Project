package com.toyota.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigUtil {

    private static final String CONFIG_FILE_NAME = "application.properties";
    private final Properties properties;

    public ConfigUtil() {
        this.properties = loadPropertiesFromFile();
    }

    private Properties loadPropertiesFromFile() {
        Properties props = new Properties();

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE_NAME)) {
            if (inputStream == null) {
                throw new RuntimeException("Configuration file not found in classpath: " + CONFIG_FILE_NAME);
            }
            props.load(inputStream);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load configuration file: " + CONFIG_FILE_NAME, e);
        }
        return props;
    }

    public String getStringValue(String key) {

        String envKey = key.toUpperCase().replace('.', '_');
        String envValue = System.getenv(envKey);
        if (envValue != null) {
            return envValue;
        }

        String propValue = properties.getProperty(key);
        if (propValue == null) {
            throw new IllegalStateException("Configuration value not found for key: '" + key);
        }
        return propValue;
    }

    public int getIntValue(String key) {
        String value = getStringValue(key);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    String.format("Invalid integer value for key '%s': '%s'", key, value), e);
        }
    }
}