package io.github.alejo2075.enzona.exception;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * ENZONA (or its API gateway) answered with a non-successful HTTP status.
 *
 * <p>Application logic should branch on {@link #errorCode()} or {@link #knownErrorCode()}, never on
 * the text of {@link #getMessage()}: ENZONA messages are written in Spanish and may change.
 */
public class EnzonaApiException extends EnzonaException {

    private static final long serialVersionUID = 1L;

    /** HTTP status code. */
    private final int statusCode;

    /** Numeric error code, may be null. */
    private final Integer errorCode;

    /** Message sent by the API, may be null. */
    private final String apiMessage;

    /** Gateway description, may be null. */
    private final String description;

    /** Non-sensitive rate limiting headers. */
    private final LinkedHashMap<String, String> rateLimitHeaders;

    /**
     * Creates an API exception.
     *
     * @param statusCode the HTTP status code
     * @param errorCode the numeric code of the error body, or {@code null} if there was none
     * @param apiMessage the message sent by the API, may be {@code null}
     * @param description the longer description sent by the gateway, may be {@code null}
     * @param rateLimitHeaders non-sensitive rate limiting headers (lower-case names), may be empty
     */
    public EnzonaApiException(
            int statusCode,
            Integer errorCode,
            String apiMessage,
            String description,
            Map<String, String> rateLimitHeaders) {
        super(describe(statusCode, errorCode, apiMessage));
        this.statusCode = statusCode;
        this.errorCode = errorCode;
        this.apiMessage = apiMessage;
        this.description = description;
        this.rateLimitHeaders = new LinkedHashMap<String, String>();
        if (rateLimitHeaders != null) {
            this.rateLimitHeaders.putAll(rateLimitHeaders);
        }
    }

    /**
     * Returns the HTTP status code.
     *
     * @return the status code
     */
    public int statusCode() {
        return statusCode;
    }

    /**
     * Returns the numeric error code of the response body (ENZONA {@code code} or gateway {@code
     * fault.code}).
     *
     * @return the code, or empty when the body had none
     */
    public OptionalInt errorCode() {
        return errorCode == null ? OptionalInt.empty() : OptionalInt.of(errorCode);
    }

    /**
     * Returns the error code as an entry of the catalog.
     *
     * @return the catalog entry, or empty when there is no code; unknown codes map to {@link
     *     EnzonaErrorCode#UNKNOWN}
     */
    public Optional<EnzonaErrorCode> knownErrorCode() {
        return errorCode == null ? Optional.empty() : Optional.of(EnzonaErrorCode.of(errorCode));
    }

    /**
     * Returns the message sent by the API.
     *
     * @return the API message, or {@code null}
     */
    public String apiMessage() {
        return apiMessage;
    }

    /**
     * Returns the description sent by the API gateway ({@code fault.description}).
     *
     * @return the description, or {@code null}
     */
    public String description() {
        return description;
    }

    /**
     * Returns rate limiting headers received with the response (such as {@code retry-after}).
     *
     * @return an unmodifiable map with lower-case header names
     */
    public Map<String, String> rateLimitHeaders() {
        return Collections.unmodifiableMap(rateLimitHeaders);
    }

    private static String describe(int statusCode, Integer errorCode, String apiMessage) {
        StringBuilder sb = new StringBuilder("ENZONA API error (HTTP ").append(statusCode);
        if (errorCode != null) {
            sb.append(", code=").append(errorCode);
        }
        sb.append(')');
        if (apiMessage != null && !apiMessage.isBlank()) {
            sb.append(": ").append(apiMessage);
        }
        return sb.toString();
    }
}
