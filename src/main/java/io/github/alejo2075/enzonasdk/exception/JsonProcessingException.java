package io.github.alejo2075.enzonasdk.exception;

public class JsonProcessingException extends RuntimeException {

    /**
     * Constructs a new JsonProcessingException with the specified detail message.
     *
     * @param message the detail message.
     */
    public JsonProcessingException(String message) {
        super(message);
    }

    /**
     * Constructs a new JsonProcessingException with the specified detail message and cause.
     *
     * @param message the detail message (which is saved for later retrieval by the {@link #getMessage()} method).
     * @param cause   the cause (which is saved for later retrieval by the {@link #getCause()} method). (A {@code null} value is permitted, and indicates that the cause is nonexistent or unknown.)
     */
    public JsonProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
