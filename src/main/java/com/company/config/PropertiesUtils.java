package com.company.config;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public final class PropertiesUtils {

    private static final Map<String, Object> PROPERTIES;

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

    private static Map<String, Object> loadProperties() throws IOException {
        Yaml yaml = new Yaml();
        InputStream inputStream = PropertiesUtils.class
                .getClassLoader()
                .getResourceAsStream("application.yaml");
        return yaml.load(inputStream);
    }

    public static <T> T getProperty(String key) {
        @SuppressWarnings("unchecked")
        T val = (T) PROPERTIES.get(key);
        return val;
    }
}
