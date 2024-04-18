package io.github.alejo2075.enzonasdk.exception;

/**
 * Custom exception class for handling JSON processing errors.
 * This class extends {@link RuntimeException} and is used to indicate errors in JSON serialization and deserialization processes.
 *
 * <p>This exception can be thrown when the JSON processing in {@link io.github.alejo2075.enzonasdk.util.JsonUtil} fails due to invalid JSON, schema mismatches, etc.</p>
 */
public class JsonProcessingException extends RuntimeException {

    /**
     * Constructs a new JsonProcessingException with the specified detail message.
     *
     * @param message the detail message, saved for later retrieval by the getMessage() method
     */
    public JsonProcessingException(String message) {
        super(message);
    }

    /**
     * Constructs a new JsonProcessingException with the specified detail message and cause.
     *
     * @param message the detail message, saved for later retrieval by the getMessage() method
     * @param cause   the cause, saved for later retrieval by the getCause() method, which may be null indicating the cause is nonexistent or unknown
     */
    public JsonProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
