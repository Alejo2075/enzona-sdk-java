package io.github.alejo2075.enzona.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.alejo2075.enzona.exception.TokenException;
import io.github.alejo2075.enzona.http.ApiRequest;
import io.github.alejo2075.enzona.http.ApiResponse;
import io.github.alejo2075.enzona.http.HttpTransport;
import io.github.alejo2075.enzona.http.JdkHttpTransport;
import io.github.alejo2075.enzona.testsupport.FakeTransport;
import io.github.alejo2075.enzona.testsupport.MutableClock;
import io.github.alejo2075.enzona.testsupport.TestServer;
import io.github.alejo2075.enzona.testsupport.TestServer.Stub;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class OAuthTokenProviderTest {

    private static final URI ENDPOINT = URI.create("https://api.enzona.net/token");
    private static final ClientCredentials CREDENTIALS =
            ClientCredentials.of("key", "TOPSECRET-VALUE");

    private static String token(String value, String expiresIn) {
        return "{\"access_token\":\""
                + value
                + "\",\"scope\":\"enzona_business_payment\",\"token_type\":\"Bearer\","
                + "\"expires_in\":"
                + expiresIn
                + "}";
    }

    private static OAuthTokenProvider.Builder provider(HttpTransport t, MutableClock clock) {
        return OAuthTokenProvider.builder(CREDENTIALS, t, ENDPOINT).clock(clock);
    }

    @Test
    void sendsBasicAuthFormBodyAndScopeToTheTokenEndpoint() {
        FakeTransport t = FakeTransport.returning(FakeTransport.json(200, token("t1", "3600")));

        String value = provider(t, MutableClock.startingAt2026()).build().accessToken();

        assertEquals("t1", value);
        ApiRequest r = t.lastRequest();
        assertEquals("POST", r.method());
        assertEquals(ENDPOINT, r.uri());
        assertEquals("Basic a2V5OlRPUFNFQ1JFVC1WQUxVRQ==", r.headers().get("Authorization"));
        assertEquals("application/x-www-form-urlencoded", r.headers().get("Content-Type"));
        assertEquals(
                "grant_type=client_credentials&scope=enzona_business_payment",
                new String(r.body(), StandardCharsets.UTF_8));
    }

    @Test
    void severalScopesAreJoinedWithTheConfiguredSeparator() {
        FakeTransport spaced = FakeTransport.returning(FakeTransport.json(200, token("t", "60")));
        provider(spaced, MutableClock.startingAt2026())
                .scopes(List.of(Scope.PAYMENT, Scope.QR))
                .build()
                .accessToken();
        assertEquals(
                "grant_type=client_credentials&scope=enzona_business_payment%20enzona_business_qr",
                new String(spaced.lastRequest().body(), StandardCharsets.UTF_8));

        FakeTransport commas = FakeTransport.returning(FakeTransport.json(200, token("t", "60")));
        provider(commas, MutableClock.startingAt2026())
                .scopes(List.of(Scope.PAYMENT, Scope.QR))
                .scopeSeparator(",")
                .build()
                .accessToken();
        assertEquals(
                "grant_type=client_credentials&scope=enzona_business_payment%2Cenzona_business_qr",
                new String(commas.lastRequest().body(), StandardCharsets.UTF_8));
    }

    @Test
    void theTokenIsCachedUntilTheSafetyMarginBeforeExpiry() {
        MutableClock clock = MutableClock.startingAt2026();
        FakeTransport t =
                FakeTransport.returning(
                        FakeTransport.json(200, token("first", "3600")),
                        FakeTransport.json(200, token("second", "3600")));
        OAuthTokenProvider p = provider(t, clock).build();

        assertEquals("first", p.accessToken());
        clock.advance(Duration.ofSeconds(3539)); // 3600 - 60 margin = 3540
        assertEquals("first", p.accessToken());
        assertEquals(1, t.requests().size());

        clock.advance(Duration.ofSeconds(2));
        assertEquals("second", p.accessToken());
        assertEquals(2, t.requests().size());
    }

    @Test
    void aLifetimeShorterThanTheMarginIsRenewedAtHalfItsLife() {
        MutableClock clock = MutableClock.startingAt2026();
        FakeTransport t =
                FakeTransport.returning(
                        FakeTransport.json(200, token("a", "30")),
                        FakeTransport.json(200, token("b", "30")));
        OAuthTokenProvider p = provider(t, clock).build();

        assertEquals("a", p.accessToken());
        clock.advance(Duration.ofSeconds(14));
        assertEquals("a", p.accessToken());
        clock.advance(Duration.ofSeconds(2));
        assertEquals("b", p.accessToken());
    }

    @Test
    void expiresInSentAsStringOrMissingIsHandled() {
        MutableClock clock = MutableClock.startingAt2026();
        FakeTransport asString =
                FakeTransport.returning(FakeTransport.json(200, token("s", "\"600\"")));
        OAuthTokenProvider p1 = provider(asString, clock).build();
        p1.accessToken();
        clock.advance(Duration.ofSeconds(500));
        p1.accessToken();
        assertEquals(1, asString.requests().size());

        FakeTransport missing =
                FakeTransport.returning(FakeTransport.json(200, "{\"access_token\":\"m\"}"));
        OAuthTokenProvider p2 = provider(missing, clock).build();
        p2.accessToken();
        clock.advance(Duration.ofMinutes(3)); // default lifetime is 5 minutes minus the margin
        p2.accessToken();
        assertEquals(1, missing.requests().size());
        clock.advance(Duration.ofMinutes(2));
        p2.accessToken();
        assertEquals(2, missing.requests().size());
    }

    @Test
    void invalidateDiscardsOnlyTheRejectedToken() {
        FakeTransport t =
                FakeTransport.returning(
                        FakeTransport.json(200, token("old", "3600")),
                        FakeTransport.json(200, token("new", "3600")));
        OAuthTokenProvider p = provider(t, MutableClock.startingAt2026()).build();

        assertEquals("old", p.accessToken());
        p.invalidate("some-other-token");
        assertEquals("old", p.accessToken());
        assertEquals(1, t.requests().size());

        p.invalidate("old");
        assertEquals("new", p.accessToken());
        // a late 401 for the old token must not throw the renewed one away
        p.invalidate("old");
        assertEquals("new", p.accessToken());
        assertEquals(2, t.requests().size());
    }

    @Test
    void aRejectedRequestThrowsWithoutLeakingTheBodyOrTheSecret() {
        FakeTransport t =
                FakeTransport.returning(
                        FakeTransport.json(
                                401,
                                "{\"error\":\"invalid_client\",\"echo\":\"TOPSECRET-VALUE\"}"));

        TokenException e =
                assertThrows(
                        TokenException.class,
                        () -> provider(t, MutableClock.startingAt2026()).build().accessToken());

        assertEquals(401, e.statusCode());
        assertTrue(e.getMessage().contains("HTTP 401"));
        assertFalse(e.getMessage().contains("TOPSECRET"));
        assertFalse(e.getMessage().contains("invalid_client"));
    }

    @Test
    void malformedOrIncompleteResponsesAreTokenExceptions() {
        for (String body : new String[] {"<html>oops</html>", "{}", "{\"access_token\":\" \"}"}) {
            FakeTransport t = FakeTransport.returning(FakeTransport.json(200, body));
            assertThrows(
                    TokenException.class,
                    () -> provider(t, MutableClock.startingAt2026()).build().accessToken(),
                    body);
        }
    }

    @Test
    void networkFailuresAreWrappedAsTokenExceptions() {
        FakeTransport t = FakeTransport.returning(FakeTransport.networkError());
        TokenException e =
                assertThrows(
                        TokenException.class,
                        () -> provider(t, MutableClock.startingAt2026()).build().accessToken());
        assertEquals(0, e.statusCode());
        assertInstanceOf(
                io.github.alejo2075.enzona.exception.TransportException.class, e.getCause());
    }

    @Test
    void failuresAreNotCached() {
        FakeTransport t =
                FakeTransport.returning(
                        FakeTransport.json(500, "{}"),
                        FakeTransport.json(200, token("ok", "3600")));
        OAuthTokenProvider p = provider(t, MutableClock.startingAt2026()).build();

        assertThrows(TokenException.class, p::accessToken);
        assertEquals("ok", p.accessToken());
    }

    @Test
    void manyThreadsShareASingleTokenRequest() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(16);
        try (TestServer server =
                TestServer.start(Stub.json(200, token("shared", "3600")).delayed(200))) {
            OAuthTokenProvider p =
                    OAuthTokenProvider.builder(
                                    CREDENTIALS,
                                    new JdkHttpTransport(
                                            Duration.ofSeconds(2), Duration.ofSeconds(5)),
                                    server.baseUri().resolve("/token"))
                            .build();

            List<Future<String>> results = new ArrayList<>();
            for (int i = 0; i < 16; i++) {
                results.add(pool.submit(p::accessToken));
            }
            for (Future<String> f : results) {
                assertEquals("shared", f.get(10, TimeUnit.SECONDS));
            }
            assertEquals(1, server.requests().size());
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void asyncCallersShareOneInFlightRequestAndThenTheCache() {
        PendingTransport t = new PendingTransport();
        OAuthTokenProvider p = provider(t, MutableClock.startingAt2026()).build();

        CompletableFuture<String> a = p.accessTokenAsync();
        CompletableFuture<String> b = p.accessTokenAsync();
        assertEquals(1, t.sent);
        assertFalse(a.isDone());

        t.pending.complete(FakeTransport.json(200, token("async", "3600")));

        assertEquals("async", a.join());
        assertEquals("async", b.join());
        assertEquals("async", p.accessTokenAsync().join());
        assertEquals("async", p.accessToken());
        assertEquals(1, t.sent);
    }

    @Test
    void asyncWorksWhenTheTransportAnswersImmediately() {
        FakeTransport t =
                FakeTransport.returning(
                        FakeTransport.json(200, token("instant", "3600")),
                        FakeTransport.json(200, token("renewed", "3600")));
        MutableClock clock = MutableClock.startingAt2026();
        OAuthTokenProvider p = provider(t, clock).build();

        assertEquals("instant", p.accessTokenAsync().join());
        assertEquals("instant", p.accessTokenAsync().join());
        assertEquals(1, t.requests().size());

        clock.advance(Duration.ofHours(2));
        assertEquals("renewed", p.accessTokenAsync().join());
        assertEquals(2, t.requests().size());
    }

    @Test
    void asyncFailureThatCompletesImmediatelyDoesNotLeaveARequestInFlight() {
        FakeTransport t =
                FakeTransport.returning(
                        FakeTransport.json(500, "{}"),
                        FakeTransport.json(200, token("ok", "3600")));
        OAuthTokenProvider p = provider(t, MutableClock.startingAt2026()).build();

        CompletionException e =
                assertThrows(CompletionException.class, () -> p.accessTokenAsync().join());
        assertInstanceOf(TokenException.class, e.getCause());
        assertEquals("ok", p.accessTokenAsync().join());
    }

    @Test
    void asyncFailuresAreTokenExceptionsAndDoNotStickToTheProvider() {
        PendingTransport t = new PendingTransport();
        OAuthTokenProvider p = provider(t, MutableClock.startingAt2026()).build();

        CompletableFuture<String> first = p.accessTokenAsync();
        t.pending.completeExceptionally(new java.io.IOException("reset"));
        CompletionException e = assertThrows(CompletionException.class, first::join);
        assertInstanceOf(TokenException.class, e.getCause());

        t.pending = new CompletableFuture<>();
        CompletableFuture<String> second = p.accessTokenAsync();
        t.pending.complete(FakeTransport.json(200, token("again", "3600")));
        assertEquals("again", second.join());
        assertEquals(2, t.sent);
    }

    @Test
    void toStringShowsNeitherSecretNorToken() {
        FakeTransport t =
                FakeTransport.returning(FakeTransport.json(200, token("SECRET-TOKEN", "60")));
        OAuthTokenProvider p = provider(t, MutableClock.startingAt2026()).build();
        p.accessToken();

        assertFalse(p.toString().contains("TOPSECRET"));
        assertFalse(p.toString().contains("SECRET-TOKEN"));
        assertSame(p, p);
    }

    @Test
    void builderValidatesItsArguments() {
        FakeTransport t = new FakeTransport();
        assertThrows(
                IllegalArgumentException.class,
                () -> OAuthTokenProvider.builder(CREDENTIALS, t, ENDPOINT).scopes(List.of()));
        assertThrows(
                IllegalArgumentException.class,
                () -> OAuthTokenProvider.builder(CREDENTIALS, t, ENDPOINT).scopeSeparator(""));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        OAuthTokenProvider.builder(CREDENTIALS, t, ENDPOINT)
                                .refreshMargin(Duration.ofSeconds(-1)));
    }

    /** A transport whose async response is completed by the test. */
    private static final class PendingTransport implements HttpTransport {
        volatile CompletableFuture<ApiResponse> pending = new CompletableFuture<>();
        volatile int sent;

        @Override
        public ApiResponse send(ApiRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CompletableFuture<ApiResponse> sendAsync(ApiRequest request) {
            sent++;
            return pending;
        }
    }
}
