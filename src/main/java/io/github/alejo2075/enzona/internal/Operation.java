package io.github.alejo2075.enzona.internal;

import java.util.Map;

/**
 * Description of one API operation, independent of any client configuration.
 *
 * @param method the HTTP method
 * @param path the already percent-encoded path from the API root, starting with {@code /} (it
 *     includes the API prefix such as {@code /payment/v1.0.0})
 * @param query query parameters (raw values, encoded when the URI is built), may be {@code null}
 * @param body the request body object serialized as JSON, or {@code null} for no body
 * @param safeToRetry {@code true} only for read-only operations
 */
public record Operation(
        String method, String path, Map<String, String> query, Object body, boolean safeToRetry) {

    /** Path prefix of the Payment API. */
    public static final String PAYMENT_API = "/payment/v1.0.0";

    /**
     * A read-only {@code GET}; these are the only operations that can be retried.
     *
     * @param path the encoded path
     * @param query the query parameters, may be {@code null}
     * @return the operation
     */
    public static Operation get(String path, Map<String, String> query) {
        return new Operation("GET", path, query, null, true);
    }

    /**
     * A {@code POST} that changes state. It is never retried.
     *
     * @param path the encoded path
     * @param body the JSON body, or {@code null} for none
     * @return the operation
     */
    public static Operation post(String path, Object body) {
        return new Operation("POST", path, null, body, false);
    }
}
