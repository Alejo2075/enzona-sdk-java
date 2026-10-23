package io.github.alejo2075.enzona.exception;

/**
 * Base class of every exception thrown by this SDK.
 *
 * <p>The SDK only throws unchecked exceptions. Messages never contain credentials, tokens, request
 * headers or request bodies.
 */
public class EnzonaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates an exception with a message.
     *
     * @param message the detail message
     */
    public EnzonaException(String message) {
        super(message);
    }

    /**
     * Creates an exception with a message and a cause.
     *
     * @param message the detail message
     * @param cause the underlying cause
     */
    public EnzonaException(String message, Throwable cause) {
        super(message, cause);
    }
}
