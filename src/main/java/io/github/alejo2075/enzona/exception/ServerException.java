package io.github.alejo2075.enzona.exception;

import java.util.Map;

/** HTTP 5xx: ENZONA or its gateway failed. */
public class ServerException extends EnzonaApiException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception.
     *
     * @param statusCode the HTTP status code
     * @param errorCode the numeric error code, may be {@code null}
     * @param apiMessage the API message, may be {@code null}
     * @param description the gateway description, may be {@code null}
     * @param rateLimitHeaders non-sensitive rate limiting headers
     */
    public ServerException(
            int statusCode,
            Integer errorCode,
            String apiMessage,
            String description,
            Map<String, String> rateLimitHeaders) {
        super(statusCode, errorCode, apiMessage, description, rateLimitHeaders);
    }
}
