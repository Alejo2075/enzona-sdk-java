package io.github.alejo2075.enzona.exception;

/**
 * The OAuth2 access token could not be obtained.
 *
 * <p>The message never contains the consumer secret or the token endpoint response body.
 */
public class TokenException extends EnzonaException {

    private static final long serialVersionUID = 1L;

    /** HTTP status of the token endpoint, or 0 when no response was received. */
    private final int statusCode;

    /**
     * Creates the exception.
     *
     * @param message the detail message
     * @param statusCode the HTTP status of the token endpoint, or 0 if there was none
     */
    public TokenException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    /**
     * Creates the exception.
     *
     * @param message the detail message
     * @param cause the underlying cause
     */
    public TokenException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 0;
    }

    /**
     * Returns the HTTP status answered by the token endpoint.
     *
     * @return the status, or 0 if no response was received
     */
    public int statusCode() {
        return statusCode;
    }
}
