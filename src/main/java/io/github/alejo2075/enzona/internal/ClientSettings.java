package io.github.alejo2075.enzona.internal;

import io.github.alejo2075.enzona.auth.AccessTokenProvider;
import io.github.alejo2075.enzona.http.HttpTransport;
import io.github.alejo2075.enzona.http.RetryPolicy;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;

/**
 * Immutable configuration shared by every operation of a client.
 *
 * @param transport sends the HTTP requests
 * @param tokens supplies the Bearer token
 * @param apiRoot the API root without trailing slash, e.g. {@code https://api.enzona.net}
 * @param requestTimeout the per-request timeout, may be {@code null} for the transport default
 * @param retryPolicy the retry policy for read-only operations
 * @param clock the clock used to interpret {@code Retry-After} dates
 * @param userAgent the {@code User-Agent} header value
 */
public record ClientSettings(
        HttpTransport transport,
        AccessTokenProvider tokens,
        URI apiRoot,
        Duration requestTimeout,
        RetryPolicy retryPolicy,
        Clock clock,
        String userAgent) {}
