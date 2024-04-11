package com.desoft.enzonasdk.util;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Utility class for JSON serialization and deserialization.
 */
public class JsonUtil {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Converts a JSON string to a Java object.
     *
     * @param json      The JSON string to convert.
     * @param valueType The class of the Java type to convert to.
     * @param <T>       The type of the Java object.
     * @return An instance of {@code T} populated with the data from the JSON string.
     * @throws RuntimeException If the conversion fails.
     */
    public static <T> T fromJson(String json, Class<T> valueType) {
        try {
            return objectMapper.readValue(json, valueType);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize JSON: " + e.getMessage(), e);
        }
    }

    /**
     * Converts a Java object to a JSON string.
     *
     * @param value The Java object to convert.
     * @return A JSON string representation of the Java object.
     * @throws RuntimeException If the conversion fails.
     */
    public static String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize object to JSON: " + e.getMessage(), e);
        }
    }
}
