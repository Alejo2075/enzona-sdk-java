package com.desoft.enzonasdk.exception;

/**
 * Custom exception class for handling errors related to Enzona API interactions.
 */
public class EnzonaException extends Exception {

    /**
     * Constructs a new EnzonaException with the specified detail message.
     *
     * @param message the detail message.
     */
    public EnzonaException(String message) {
        super(message);
    }

    /**
     * Constructs a new EnzonaException with the specified detail message and cause.
     *
     * @param message the detail message (which is saved for later retrieval by the {@link #getMessage()} method).
     * @param cause   the cause (which is saved for later retrieval by the {@link #getCause()} method). (A {@code null} value is permitted, and indicates that the cause is nonexistent or unknown.)
     */
    public EnzonaException(String message, Throwable cause) {
        super(message, cause);
    }
}
