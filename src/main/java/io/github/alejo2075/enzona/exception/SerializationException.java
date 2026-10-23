package io.github.alejo2075.enzona.exception;

/** A request could not be serialized or a response could not be parsed. */
public class SerializationException extends EnzonaException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception.
     *
     * @param message the detail message
     */
    public SerializationException(String message) {
        super(message);
    }

    /**
     * Creates the exception.
     *
     * @param message the detail message
     * @param cause the underlying cause
     */
    public SerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
