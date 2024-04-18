package io.github.alejo2075.enzonasdk.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.github.alejo2075.enzonasdk.exception.JsonProcessingException;

/**
 * Provides utility methods for converting between JSON strings and Java objects.
 * This class configures the ObjectMapper to not fail on unknown properties and to format the JSON output.
 *
 * <p>Exception handling is centralized via the JsonProcessingException, providing a clear protocol for error management across JSON processing tasks.</p>
 */
public class JsonUtil {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
    }

    /**
     * Deserializes a JSON string into an object of the specified Java class.
     *
     * @param json      the JSON string to be deserialized
     * @param valueType the class of type T to which json is to be deserialized
     * @param <T>       the type parameter indicating the type of the Java object to be returned
     * @return an instance of {@code T} populated with data converted from the JSON string
     * @throws JsonProcessingException if JSON to Java object deserialization fails
     */
    public static <T> T fromJson(String json, Class<T> valueType) {
        try {
            return objectMapper.readValue(json, valueType);
        } catch (Exception e) {
            throw new JsonProcessingException("Failed to deserialize JSON to " + valueType.getSimpleName(), e);
        }
    }

    /**
     * Serializes an object into its JSON string representation.
     *
     * @param value the Java object to be serialized
     * @return a JSON string representation of {@code value}
     * @throws JsonProcessingException if object to JSON serialization fails
     */
    public static String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new JsonProcessingException("Failed to serialize object to JSON", e);
        }
    }
}
