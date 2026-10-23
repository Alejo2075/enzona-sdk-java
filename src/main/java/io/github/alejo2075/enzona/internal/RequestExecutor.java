package io.github.alejo2075.enzona.internal;

import io.github.alejo2075.enzona.exception.AuthenticationException;
import io.github.alejo2075.enzona.exception.EnzonaApiException;
import io.github.alejo2075.enzona.exception.RateLimitException;
import io.github.alejo2075.enzona.exception.ServerException;
import io.github.alejo2075.enzona.exception.TransportException;
import io.github.alejo2075.enzona.http.ApiRequest;
import io.github.alejo2075.enzona.http.ApiResponse;
import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Builds requests from {@link Operation}s, adds the Bearer token, sends them, maps errors and
 * applies the retry policy.
 *
 * <p>Two mechanisms look alike but are different:
 *
 * <ul>
 *   <li><b>Token renewal.</b> If the gateway answers 401 because the access token is no longer
 *       valid, the token is discarded and the call is repeated <em>once</em> with a new one. This
 *       also applies to operations that move money: the gateway rejects an invalid token before the
 *       request reaches ENZONA, so nothing was executed. A 401 carrying an ENZONA business code is
 *       not repeated.
 *   <li><b>Retries.</b> Opt-in and limited to read-only operations.
 * </ul>
 */
public final class RequestExecutor {

    /** Error codes at or above this value come from the WSO2 gateway, not from ENZONA. */
    private static final int GATEWAY_CODE_FLOOR = 900_000;

    private final ClientSettings settings;

    /**
     * Creates an executor.
     *
     * @param settings the client settings
     */
    public RequestExecutor(ClientSettings settings) {
        this.settings = settings;
    }

    /**
     * Executes an operation synchronously.
     *
     * @param op the operation
     * @param mapper converts the successful response body into the result
     * @param <T> the result type
     * @return the result
     */
    public <T> T execute(Operation op, Function<byte[], T> mapper) {
        String token = requireToken(settings.tokens().accessToken());
        boolean renewed = false;
        int attempt = 1;
        while (true) {
            ApiRequest request = buildRequest(op, token);
            Throwable failure;
            try {
                ApiResponse response = settings.transport().send(request);
                if (response.isSuccessful()) {
                    return mapper.apply(response.body());
                }
                EnzonaApiException api = ErrorMapper.map(response, settings.clock());
                if (!renewed && isGatewayTokenRejection(api)) {
                    renewed = true;
                    settings.tokens().invalidate(token);
                    token = requireToken(settings.tokens().accessToken());
                    continue;
                }
                failure = api;
            } catch (TransportException e) {
                failure = e;
            }
            Duration wait = retryDelay(op, attempt, failure);
            if (wait == null || Thread.currentThread().isInterrupted()) {
                throw (RuntimeException) failure;
            }
            sleep(wait, request);
            attempt++;
        }
    }

    /**
     * Executes an operation asynchronously. The returned future completes exceptionally with the
     * typed SDK exception (not wrapped).
     *
     * @param op the operation
     * @param mapper converts the successful response body into the result
     * @param <T> the result type
     * @return the future result
     */
    public <T> CompletableFuture<T> executeAsync(Operation op, Function<byte[], T> mapper) {
        CompletableFuture<T> result = new CompletableFuture<>();
        CompletableFuture<String> tokenFuture;
        try {
            tokenFuture = settings.tokens().accessTokenAsync();
        } catch (RuntimeException e) {
            result.completeExceptionally(e);
            return result;
        }
        tokenFuture.whenComplete(
                (token, error) -> {
                    if (error != null) {
                        result.completeExceptionally(unwrap(error));
                        return;
                    }
                    try {
                        attemptAsync(op, requireToken(token), false, mapper, 1, result);
                    } catch (RuntimeException e) {
                        result.completeExceptionally(e);
                    }
                });
        return result;
    }

    private <T> void attemptAsync(
            Operation op,
            String token,
            boolean renewed,
            Function<byte[], T> mapper,
            int attempt,
            CompletableFuture<T> result) {
        ApiRequest request;
        CompletableFuture<ApiResponse> sent;
        try {
            request = buildRequest(op, token);
            sent = settings.transport().sendAsync(request);
        } catch (RuntimeException e) {
            result.completeExceptionally(e);
            return;
        }
        sent.whenComplete(
                (response, error) -> {
                    try {
                        Throwable failure;
                        if (error != null) {
                            failure = unwrap(error);
                        } else if (response.isSuccessful()) {
                            result.complete(mapper.apply(response.body()));
                            return;
                        } else {
                            EnzonaApiException api = ErrorMapper.map(response, settings.clock());
                            if (!renewed && isGatewayTokenRejection(api)) {
                                settings.tokens().invalidate(token);
                                settings.tokens()
                                        .accessTokenAsync()
                                        .whenComplete(
                                                (fresh, tokenError) -> {
                                                    if (tokenError != null) {
                                                        result.completeExceptionally(
                                                                unwrap(tokenError));
                                                        return;
                                                    }
                                                    try {
                                                        attemptAsync(
                                                                op,
                                                                requireToken(fresh),
                                                                true,
                                                                mapper,
                                                                attempt,
                                                                result);
                                                    } catch (RuntimeException e) {
                                                        result.completeExceptionally(e);
                                                    }
                                                });
                                return;
                            }
                            failure = api;
                        }
                        Duration wait = retryDelay(op, attempt, failure);
                        if (wait == null) {
                            result.completeExceptionally(failure);
                            return;
                        }
                        CompletableFuture.delayedExecutor(wait.toMillis(), TimeUnit.MILLISECONDS)
                                .execute(
                                        () ->
                                                attemptAsync(
                                                        op,
                                                        token,
                                                        renewed,
                                                        mapper,
                                                        attempt + 1,
                                                        result));
                    } catch (Throwable t) {
                        result.completeExceptionally(t);
                    }
                });
    }

    /** A 401 from the gateway (no ENZONA business code) means the token is no longer accepted. */
    private static boolean isGatewayTokenRejection(EnzonaApiException e) {
        if (!(e instanceof AuthenticationException)) {
            return false;
        }
        return e.errorCode().isEmpty() || e.errorCode().getAsInt() >= GATEWAY_CODE_FLOOR;
    }

    private Duration retryDelay(Operation op, int attempt, Throwable failure) {
        if (!op.safeToRetry() || !settings.retryPolicy().enabled()) {
            return null;
        }
        Duration retryAfter = null;
        if (failure instanceof RateLimitException) {
            retryAfter = ((RateLimitException) failure).retryAfter().orElse(null);
        } else if (!(failure instanceof ServerException || failure instanceof TransportException)) {
            return null;
        }
        return settings.retryPolicy().delayBeforeRetry(attempt, retryAfter);
    }

    private static Throwable unwrap(Throwable t) {
        return (t instanceof CompletionException && t.getCause() != null) ? t.getCause() : t;
    }

    private static void sleep(Duration wait, ApiRequest request) {
        try {
            Thread.sleep(wait.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TransportException(
                    "HTTP request "
                            + request.method()
                            + " "
                            + request.uri().getRawPath()
                            + " was interrupted while waiting to retry",
                    e);
        }
    }

    private static String requireToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("The AccessTokenProvider returned an empty token");
        }
        return token;
    }

    private ApiRequest buildRequest(Operation op, String token) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Accept", "application/json");
        headers.put("User-Agent", settings.userAgent());
        headers.put("Authorization", "Bearer " + token);
        byte[] body = null;
        if (op.body() != null) {
            body = JsonCodec.write(op.body());
            headers.put("Content-Type", "application/json");
        }
        return new ApiRequest(op.method(), buildUri(op), headers, body, settings.requestTimeout());
    }

    private URI buildUri(Operation op) {
        StringBuilder sb = new StringBuilder(settings.apiRoot().toString()).append(op.path());
        if (op.query() != null && !op.query().isEmpty()) {
            char sep = '?';
            for (Map.Entry<String, String> e : op.query().entrySet()) {
                sb.append(sep)
                        .append(UrlEncoding.encode(e.getKey()))
                        .append('=')
                        .append(UrlEncoding.encode(e.getValue()));
                sep = '&';
            }
        }
        return URI.create(sb.toString());
    }
}
