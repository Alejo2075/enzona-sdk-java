package io.github.alejo2075.enzonasdk.exception;

/**
 * Represents exceptions that are specific to EnZona API interactions.
 * This class extends {@link Exception} and provides constructors that support both messages and causes.
 *
 * <p>Use this exception to indicate failures related to EnZona API operations, such as network issues, data format errors, or API limitations.</p>
 */
public class EnzonaException extends Exception {

    /**
     * Constructs a new EnzonaException with the specified detail message.
     *
     * @param message the detail message, saved for later retrieval by the getMessage() method
     */
    public EnzonaException(String message) {
        super(message);
    }

    /**
     * Constructs a new EnzonaException with the specified detail message and cause.
     *
     * @param message the detail message, saved for later retrieval by the getMessage() method
     * @param cause   the cause, saved for later retrieval by the getCause() method, which may be null indicating the cause is nonexistent or unknown
     */
    public EnzonaException(String message, Throwable cause) {
        super(message, cause);
    }
}
