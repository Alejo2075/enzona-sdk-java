package io.github.alejo2075.enzona.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.alejo2075.enzona.auth.AccessTokenProvider;
import io.github.alejo2075.enzona.exception.AuthenticationException;
import io.github.alejo2075.enzona.exception.NotFoundException;
import io.github.alejo2075.enzona.exception.RateLimitException;
import io.github.alejo2075.enzona.exception.SerializationException;
import io.github.alejo2075.enzona.exception.ServerException;
import io.github.alejo2075.enzona.exception.TokenException;
import io.github.alejo2075.enzona.exception.TransportException;
import io.github.alejo2075.enzona.http.ApiRequest;
import io.github.alejo2075.enzona.http.RetryPolicy;
import io.github.alejo2075.enzona.testsupport.FakeTransport;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.Test;

class RequestExecutorTest {

    private static final String OK = "{\"ok\":true}";
    private static final String GATEWAY_401 =
            "{\"fault\":{\"code\":900901,\"message\":\"Invalid Credentials\"}}";

    /** Hands out t1; after each invalidation the next token (t2, t3...). */
    private static final class Tokens implements AccessTokenProvider {
        final List<String> invalidated = new ArrayList<>();
        RuntimeException failure;

        @Override
        public String accessToken() {
            if (failure != null) {
                throw failure;
            }
            return "t" + (invalidated.size() + 1);
        }

        @Override
        public void invalidate(String rejectedToken) {
            invalidated.add(rejectedToken);
        }
    }

    private static RequestExecutor executor(FakeTransport t, Tokens tokens, RetryPolicy policy) {
        return new RequestExecutor(
                new ClientSettings(
                        t,
                        tokens,
                        URI.create("https://api.enzona.net"),
                        Duration.ofSeconds(9),
                        policy,
                        Clock.systemUTC(),
                        "enzona-sdk-java/test"));
    }

    private static RequestExecutor executor(FakeTransport t) {
        return executor(t, new Tokens(), RetryPolicy.disabled());
    }

    private static final RetryPolicy FAST =
            RetryPolicy.exponentialBackoff(3, Duration.ofMillis(1), Duration.ofMillis(5));

    private static String text(byte[] b) {
        return new String(b, StandardCharsets.UTF_8);
    }

    private static final Operation GET = Operation.get("/payment/v1.0.0/payments/abc", null);
    private static final Operation POST =
            Operation.post("/payment/v1.0.0/payments/abc/complete", null);

    @Test
    void attachesBearerAndStandardHeadersAndMapsTheBody() {
        FakeTransport t = FakeTransport.returning(FakeTransport.json(200, OK));

        String result = executor(t).execute(GET, RequestExecutorTest::text);

        assertEquals(OK, result);
        ApiRequest r = t.lastRequest();
        assertEquals("GET", r.method());
        assertEquals(URI.create("https://api.enzona.net/payment/v1.0.0/payments/abc"), r.uri());
        assertEquals("Bearer t1", r.headers().get("Authorization"));
        assertEquals("application/json", r.headers().get("Accept"));
        assertEquals("enzona-sdk-java/test", r.headers().get("User-Agent"));
        assertEquals(Duration.ofSeconds(9), r.timeout());
        assertNull(r.body());
        assertNull(r.headers().get("Content-Type"));
    }

    @Test
    void serializesTheBodyAsJsonWithContentType() {
        FakeTransport t = FakeTransport.returning(FakeTransport.json(200, OK));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("return_url", "https://shop.example/ok");

        executor(t)
                .execute(
                        Operation.post("/payment/v1.0.0/payments", body),
                        RequestExecutorTest::text);

        assertEquals("application/json", t.lastRequest().headers().get("Content-Type"));
        assertEquals("{\"return_url\":\"https://shop.example/ok\"}", text(t.lastRequest().body()));
    }

    @Test
    void encodesQueryNamesAndValues() {
        FakeTransport t = FakeTransport.returning(FakeTransport.json(200, OK));
        Map<String, String> q = new LinkedHashMap<>();
        q.put("merchant_uuid", "a b&c");
        q.put("limit", "10");

        executor(t)
                .execute(Operation.get("/payment/v1.0.0/payments", q), RequestExecutorTest::text);

        assertEquals(
                "/payment/v1.0.0/payments?merchant_uuid=a%20b%26c&limit=10",
                t.lastRequest().uri().getRawPath() + "?" + t.lastRequest().uri().getRawQuery());
    }

    @Test
    void errorsAreMappedToTypedExceptions() {
        assertThrows(
                NotFoundException.class,
                () ->
                        executor(
                                        FakeTransport.returning(
                                                FakeTransport.json(404, "{\"code\":4064}")))
                                .execute(GET, RequestExecutorTest::text));
        assertThrows(
                ServerException.class,
                () ->
                        executor(FakeTransport.returning(FakeTransport.json(500, "{}")))
                                .execute(GET, RequestExecutorTest::text));
    }

    @Test
    void aGatewayRejectionRenewsTheTokenAndRepeatsTheCallOnce() {
        FakeTransport t =
                FakeTransport.returning(
                        FakeTransport.json(401, GATEWAY_401), FakeTransport.json(200, OK));
        Tokens tokens = new Tokens();

        String result =
                executor(t, tokens, RetryPolicy.disabled()).execute(GET, RequestExecutorTest::text);

        assertEquals(OK, result);
        assertEquals(List.of("t1"), tokens.invalidated);
        assertEquals("Bearer t1", t.requests().get(0).headers().get("Authorization"));
        assertEquals("Bearer t2", t.requests().get(1).headers().get("Authorization"));
    }

    @Test
    void aSecondRejectionIsReportedAfterExactlyTwoRequests() {
        FakeTransport t = FakeTransport.returning(FakeTransport.json(401, GATEWAY_401));

        assertThrows(
                AuthenticationException.class,
                () -> executor(t).execute(GET, RequestExecutorTest::text));
        assertEquals(2, t.requests().size());
    }

    @Test
    void moneyOperationsAreRepeatedOnlyOnceAfterATokenRejection() {
        FakeTransport t =
                FakeTransport.returning(
                        FakeTransport.json(401, GATEWAY_401), FakeTransport.json(200, OK));

        executor(t).execute(POST, RequestExecutorTest::text);

        assertEquals(2, t.requests().size());
        assertEquals("POST", t.requests().get(1).method());
    }

    @Test
    void a401WithAnEnzonaBusinessCodeIsNotRepeated() {
        FakeTransport t =
                FakeTransport.returning(
                        FakeTransport.json(401, "{\"code\":4001,\"message\":\"Acceso denegado\"}"));
        Tokens tokens = new Tokens();

        AuthenticationException e =
                assertThrows(
                        AuthenticationException.class,
                        () ->
                                executor(t, tokens, RetryPolicy.disabled())
                                        .execute(POST, RequestExecutorTest::text));

        assertEquals(4001, e.errorCode().getAsInt());
        assertEquals(1, t.requests().size());
        assertTrue(tokens.invalidated.isEmpty());
    }

    @Test
    void aFailureToGetATokenIsReportedUnchanged() {
        Tokens tokens = new Tokens();
        tokens.failure = new TokenException("rejected", 401);
        FakeTransport t = new FakeTransport();

        assertThrows(
                TokenException.class,
                () ->
                        executor(t, tokens, RetryPolicy.disabled())
                                .execute(GET, RequestExecutorTest::text));
        assertTrue(t.requests().isEmpty());
    }

    @Test
    void anEmptyTokenIsRejectedBeforeSending() {
        AccessTokenProvider blank = AccessTokenProvider.fixed("x");
        AccessTokenProvider empty =
                new AccessTokenProvider() {
                    @Override
                    public String accessToken() {
                        return " ";
                    }

                    @Override
                    public void invalidate(String rejectedToken) {}
                };
        FakeTransport t = new FakeTransport();
        RequestExecutor e =
                new RequestExecutor(
                        new ClientSettings(
                                t,
                                empty,
                                URI.create("https://api.enzona.net"),
                                null,
                                RetryPolicy.disabled(),
                                Clock.systemUTC(),
                                "ua"));

        assertThrows(IllegalStateException.class, () -> e.execute(GET, RequestExecutorTest::text));
        assertTrue(t.requests().isEmpty());
        assertEquals("x", blank.accessToken());
    }

    @Test
    void moneyOperationsAreNeverRetriedEvenWithARetryPolicy() {
        for (Object failure :
                new Object[] {
                    FakeTransport.json(500, "{}"),
                    FakeTransport.json(429, "{}", Map.of("Retry-After", List.of("0"))),
                    FakeTransport.networkError()
                }) {
            FakeTransport t = FakeTransport.returning(failure, FakeTransport.json(200, OK));
            assertThrows(
                    RuntimeException.class,
                    () -> executor(t, new Tokens(), FAST).execute(POST, RequestExecutorTest::text));
            assertEquals(1, t.requests().size(), String.valueOf(failure));
        }
    }

    @Test
    void readsAreNotRetriedByDefault() {
        FakeTransport t =
                FakeTransport.returning(FakeTransport.json(500, "{}"), FakeTransport.json(200, OK));

        assertThrows(
                ServerException.class, () -> executor(t).execute(GET, RequestExecutorTest::text));
        assertEquals(1, t.requests().size());
    }

    @Test
    void readsAreRetriedOnServerErrorsRateLimitsAndNetworkFailuresWhenEnabled() {
        FakeTransport t =
                FakeTransport.returning(
                        FakeTransport.json(500, "{}"),
                        FakeTransport.json(429, "{}", Map.of("Retry-After", List.of("0"))),
                        FakeTransport.networkError(),
                        FakeTransport.json(200, OK));
        RetryPolicy four =
                RetryPolicy.exponentialBackoff(4, Duration.ofMillis(1), Duration.ofMillis(5));

        String result = executor(t, new Tokens(), four).execute(GET, RequestExecutorTest::text);

        assertEquals(OK, result);
        assertEquals(4, t.requests().size());
    }

    @Test
    void clientErrorsOnReadsAreNeverRetried() {
        FakeTransport t =
                FakeTransport.returning(FakeTransport.json(404, "{}"), FakeTransport.json(200, OK));

        assertThrows(
                NotFoundException.class,
                () -> executor(t, new Tokens(), FAST).execute(GET, RequestExecutorTest::text));
        assertEquals(1, t.requests().size());
    }

    @Test
    void aRetryAfterBeyondTheMaximumIsReportedInsteadOfRetried() {
        FakeTransport t =
                FakeTransport.returning(
                        FakeTransport.json(429, "{}", Map.of("Retry-After", List.of("3600"))),
                        FakeTransport.json(200, OK));

        assertThrows(
                RateLimitException.class,
                () -> executor(t, new Tokens(), FAST).execute(GET, RequestExecutorTest::text));
        assertEquals(1, t.requests().size());
    }

    @Test
    void retriesStopAtTheMaximumNumberOfAttempts() {
        FakeTransport t = FakeTransport.returning(FakeTransport.json(503, "{}"));

        assertThrows(
                ServerException.class,
                () -> executor(t, new Tokens(), FAST).execute(GET, RequestExecutorTest::text));
        assertEquals(3, t.requests().size());
    }

    @Test
    void anInterruptedWaitKeepsTheInterruptFlagAndFails() {
        FakeTransport t =
                FakeTransport.returning(FakeTransport.json(500, "{}"), FakeTransport.json(200, OK));
        RetryPolicy slow =
                RetryPolicy.exponentialBackoff(2, Duration.ofSeconds(5), Duration.ofSeconds(5));
        Thread.currentThread().interrupt();
        try {
            assertThrows(
                    ServerException.class,
                    () -> executor(t, new Tokens(), slow).execute(GET, RequestExecutorTest::text));
            assertTrue(Thread.currentThread().isInterrupted());
            assertEquals(1, t.requests().size());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void mapperFailuresPropagateUnchanged() {
        FakeTransport t = FakeTransport.returning(FakeTransport.json(200, "x"));
        SerializationException boom = new SerializationException("bad");

        SerializationException e =
                assertThrows(
                        SerializationException.class,
                        () ->
                                executor(t)
                                        .execute(
                                                GET,
                                                body -> {
                                                    throw boom;
                                                }));
        assertEquals(boom, e);
    }

    // ----- async -----

    @Test
    void asyncSuccessAndHeaders() {
        FakeTransport t = FakeTransport.returning(FakeTransport.json(200, OK));

        String result = executor(t).executeAsync(GET, RequestExecutorTest::text).join();

        assertEquals(OK, result);
        assertEquals("Bearer t1", t.lastRequest().headers().get("Authorization"));
    }

    @Test
    void asyncErrorsCompleteExceptionallyWithTheTypedException() {
        FakeTransport t = FakeTransport.returning(FakeTransport.json(404, "{\"code\":4064}"));

        CompletionException e =
                assertThrows(
                        CompletionException.class,
                        () -> executor(t).executeAsync(GET, RequestExecutorTest::text).join());
        assertInstanceOf(NotFoundException.class, e.getCause());
    }

    @Test
    void asyncRenewsTheTokenOnceAfterAGatewayRejection() {
        FakeTransport t =
                FakeTransport.returning(
                        FakeTransport.json(401, GATEWAY_401), FakeTransport.json(200, OK));
        Tokens tokens = new Tokens();

        String result =
                executor(t, tokens, RetryPolicy.disabled())
                        .executeAsync(POST, RequestExecutorTest::text)
                        .join();

        assertEquals(OK, result);
        assertEquals(List.of("t1"), tokens.invalidated);
        assertEquals("Bearer t2", t.requests().get(1).headers().get("Authorization"));
        assertEquals(2, t.requests().size());
    }

    @Test
    void asyncSecondRejectionIsReported() {
        FakeTransport t = FakeTransport.returning(FakeTransport.json(401, GATEWAY_401));

        CompletionException e =
                assertThrows(
                        CompletionException.class,
                        () -> executor(t).executeAsync(GET, RequestExecutorTest::text).join());
        assertInstanceOf(AuthenticationException.class, e.getCause());
        assertEquals(2, t.requests().size());
    }

    @Test
    void asyncMoneyOperationsAreNeverRetried() {
        FakeTransport t =
                FakeTransport.returning(FakeTransport.json(500, "{}"), FakeTransport.json(200, OK));

        assertThrows(
                CompletionException.class,
                () ->
                        executor(t, new Tokens(), FAST)
                                .executeAsync(POST, RequestExecutorTest::text)
                                .join());
        assertEquals(1, t.requests().size());
    }

    @Test
    void asyncReadsAreRetriedWhenEnabled() {
        FakeTransport t =
                FakeTransport.returning(
                        FakeTransport.json(500, "{}"),
                        FakeTransport.networkError(),
                        FakeTransport.json(200, OK));

        String result =
                executor(t, new Tokens(), FAST).executeAsync(GET, RequestExecutorTest::text).join();

        assertEquals(OK, result);
        assertEquals(3, t.requests().size());
    }

    @Test
    void asyncTokenFailureCompletesExceptionally() {
        Tokens tokens = new Tokens();
        tokens.failure = new TokenException("rejected", 401);

        CompletionException e =
                assertThrows(
                        CompletionException.class,
                        () ->
                                executor(new FakeTransport(), tokens, RetryPolicy.disabled())
                                        .executeAsync(GET, RequestExecutorTest::text)
                                        .join());
        assertInstanceOf(TokenException.class, e.getCause());
    }

    @Test
    void asyncTransportFailureIsReportedAsTransportException() {
        FakeTransport t = FakeTransport.returning(FakeTransport.networkError());

        CompletionException e =
                assertThrows(
                        CompletionException.class,
                        () -> executor(t).executeAsync(GET, RequestExecutorTest::text).join());
        assertInstanceOf(TransportException.class, e.getCause());
    }

    @Test
    void asyncMapperFailureCompletesExceptionally() {
        FakeTransport t = FakeTransport.returning(FakeTransport.json(200, "x"));

        CompletableFuture<String> f =
                executor(t)
                        .executeAsync(
                                GET,
                                body -> {
                                    throw new SerializationException("bad");
                                });

        assertTrue(f.isCompletedExceptionally());
    }
}
