package io.github.alejo2075.enzona.exception;

/** An I/O problem, a timeout or an interruption prevented the request from completing. */
public class TransportException extends EnzonaException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception.
     *
     * @param message the detail message
     */
    public TransportException(String message) {
        super(message);
    }

    /**
     * Creates the exception.
     *
     * @param message the detail message
     * @param cause the underlying cause
     */
    public TransportException(String message, Throwable cause) {
        super(message, cause);
    }
}
