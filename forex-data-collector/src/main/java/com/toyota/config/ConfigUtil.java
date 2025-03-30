package com.toyota.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigUtil {
    private static final String CONFIG_FILE_PATH = "application.properties";
    private static Properties properties = null;

    private ConfigUtil() {
    }

    private static void loadProperties() {
        if (properties == null) {
            properties = new Properties();
            try (InputStream inputStream = ConfigUtil.class.getClassLoader().getResourceAsStream(CONFIG_FILE_PATH)) {
                if (inputStream == null) {
                    throw new IllegalStateException("Configuration file not found: " + CONFIG_FILE_PATH);
                }
                properties.load(inputStream);
            } catch (IOException e) {
                throw new RuntimeException("Failed to load configuration file: " + CONFIG_FILE_PATH, e);
            }
        }
    }

    public static String getValue(String key) {

        String envKey = key.toUpperCase().replace(".", "_");
        String envValue = System.getenv(envKey);

        if (envValue != null) {
            return envValue;
        }

        loadProperties();
        String propValue = properties.getProperty(key);
        if (propValue == null) {
            throw new IllegalStateException("Configuration value not found for key: " + key);
        }
        return propValue;
    }


    public static int getIntValue(String key) {
        String value = getValue(key);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid integer value for " + key + ": " + value, e);
        }
    }

}