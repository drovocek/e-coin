package com.company.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class PropertiesUtils {

    private static final Properties PROPERTIES;

    static {
        try {
            PROPERTIES = loadProperties();
            PROPERTIES.forEach((key, val) -> {
                if (val instanceof String) {
                    String replaced = ((String) val).replace("${PROJECT_DIR}", System.getProperty("user.dir"));
                    PROPERTIES.put(key, replaced);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private PropertiesUtils() {
        throw new UnsupportedOperationException();
    }

    private static Properties loadProperties() throws IOException {
        InputStream inputStream = PropertiesUtils.class
                .getClassLoader()
                .getResourceAsStream("application.properties");
        Properties properties = new Properties();
        properties.load(inputStream);
        return properties;
    }

    public static <T> T getProperty(String key) {
        @SuppressWarnings("unchecked")
        T val = (T) PROPERTIES.get(key);
        return val;
    }
}
