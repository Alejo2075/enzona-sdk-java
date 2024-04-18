package io.github.alejo2075.enzonasdk.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.github.alejo2075.enzonasdk.exception.JsonProcessingException;

/**
 * Utility class for JSON serialization and deserialization with enhanced exception handling.
 */
public class JsonUtil {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
    }

    /**
     * Converts a JSON string to a Java object.
     *
     * @param json      The JSON string to convert.
     * @param valueType The class of the Java type to convert to.
     * @param <T>       The type of the Java object.
     * @return An instance of {@code T} populated with the data from the JSON string.
     * @throws JsonProcessingException If the conversion fails due to invalid JSON structure or schema mismatches.
     */
    public static <T> T fromJson(String json, Class<T> valueType) {
        try {
            return objectMapper.readValue(json, valueType);
        } catch (Exception e) {
            throw new JsonProcessingException("Failed to deserialize JSON to " + valueType.getSimpleName(), e);
        }
    }

    /**
     * Converts a Java object to a JSON string.
     *
     * @param value The Java object to convert.
     * @return A JSON string representation of the Java object.
     * @throws JsonProcessingException If the conversion fails due to serialization issues.
     */
    public static String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new JsonProcessingException("Failed to serialize object to JSON", e);
        }
    }
}
