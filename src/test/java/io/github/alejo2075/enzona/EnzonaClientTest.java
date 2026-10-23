package io.github.alejo2075.enzona;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.alejo2075.enzona.auth.AccessTokenProvider;
import io.github.alejo2075.enzona.auth.Scope;
import io.github.alejo2075.enzona.http.RetryPolicy;
import io.github.alejo2075.enzona.internal.Operation;
import io.github.alejo2075.enzona.testsupport.FakeTransport;
import io.github.alejo2075.enzona.testsupport.TestServer;
import io.github.alejo2075.enzona.testsupport.TestServer.Stub;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class EnzonaClientTest {

    private static final String TOKEN_JSON =
            "{\"access_token\":\"live-token\",\"token_type\":\"Bearer\",\"expires_in\":3600}";

    private static EnzonaClient.Builder withCredentials() {
        return EnzonaClient.builder().credentials("key", "TOPSECRET");
    }

    @Test
    void productionIsTheDefaultEnvironment() {
        EnzonaClient c = withCredentials().build();
        assertEquals(Environment.PRODUCTION, c.environment());
        assertEquals("https://api.enzona.net", c.apiRoot());
    }

    @Test
    void sandboxUsesItsOwnHost() {
        EnzonaClient c = withCredentials().environment(Environment.SANDBOX).build();
        assertEquals(Environment.SANDBOX, c.environment());
        assertEquals("https://apisandbox.enzona.net", c.apiRoot());
    }

    @Test
    void aCustomBaseUrlIsNormalizedAndHasNoEnvironment() {
        EnzonaClient c = withCredentials().baseUrl("https://gateway.internal//").build();
        assertEquals("https://gateway.internal", c.apiRoot());
        assertNull(c.environment());
    }

    @Test
    void selectingAnEnvironmentDropsAnEarlierBaseUrl() {
        EnzonaClient c =
                withCredentials()
                        .baseUrl("https://gateway.internal")
                        .environment(Environment.SANDBOX)
                        .build();
        assertEquals("https://apisandbox.enzona.net", c.apiRoot());
    }

    @Test
    void plainHttpAndSuspiciousUrlsAreRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> withCredentials().baseUrl("http://api.enzona.net").build());
        assertThrows(
                IllegalArgumentException.class, () -> withCredentials().baseUrl("ftp://x").build());
        assertThrows(
                IllegalArgumentException.class,
                () -> withCredentials().baseUrl("https://user:pw@api.enzona.net").build());
        assertThrows(
                IllegalArgumentException.class,
                () -> withCredentials().baseUrl("https://api.enzona.net?x=1").build());
        assertThrows(
                IllegalArgumentException.class,
                () -> withCredentials().baseUrl("not a url").build());
        assertThrows(
                IllegalArgumentException.class,
                () -> withCredentials().baseUrl("https://").build());
    }

    @Test
    void plainHttpIsAllowedOnlyWhenExplicitlyRequested() {
        EnzonaClient c =
                withCredentials()
                        .baseUrl("http://127.0.0.1:8080")
                        .allowInsecureBaseUrlForTesting()
                        .build();
        assertEquals("http://127.0.0.1:8080", c.apiRoot());
    }

    @Test
    void credentialsOrATokenProviderAreRequiredButNotBoth() {
        assertThrows(IllegalStateException.class, () -> EnzonaClient.builder().build());
        assertThrows(
                IllegalStateException.class,
                () ->
                        withCredentials()
                                .accessTokenProvider(AccessTokenProvider.fixed("t"))
                                .build());
        assertEquals(
                "https://api.enzona.net",
                EnzonaClient.builder()
                        .accessTokenProvider(AccessTokenProvider.fixed("t"))
                        .build()
                        .apiRoot());
    }

    @Test
    void timeoutsMustBePositive() {
        assertThrows(
                IllegalArgumentException.class,
                () -> withCredentials().connectTimeout(Duration.ZERO));
        assertThrows(
                IllegalArgumentException.class,
                () -> withCredentials().requestTimeout(Duration.ofSeconds(-1)));
        assertThrows(IllegalArgumentException.class, () -> withCredentials().scopes());
    }

    @Test
    void toStringNeverShowsTheSecret() {
        EnzonaClient c = withCredentials().build();
        assertFalse(c.toString().contains("TOPSECRET"));
        assertTrue(c.toString().contains("https://api.enzona.net"));
    }

    @Test
    void endToEndRequestsATokenThenCallsTheApiWithIt() {
        try (TestServer server =
                TestServer.start(
                        req ->
                                req.rawPath().equals("/token")
                                        ? Stub.json(200, TOKEN_JSON)
                                        : Stub.json(200, "{\"ok\":true}"))) {
            EnzonaClient c =
                    withCredentials()
                            .baseUrl(server.baseUri().toString())
                            .allowInsecureBaseUrlForTesting()
                            .scopes(Scope.PAYMENT, Scope.QR)
                            .build();

            String body =
                    c.executor()
                            .execute(
                                    Operation.get("/payment/v1.0.0/payments/abc", null),
                                    b -> new String(b, StandardCharsets.UTF_8));
            c.executor()
                    .execute(
                            Operation.get("/payment/v1.0.0/payments/abc", null),
                            b -> new String(b, StandardCharsets.UTF_8));

            assertEquals("{\"ok\":true}", body);
            assertEquals(3, server.requests().size(), "one token request, two API calls");
            var token = server.requests().get(0);
            assertEquals("POST", token.method());
            assertEquals("/token", token.rawPath());
            assertEquals("Basic a2V5OlRPUFNFQ1JFVA==", token.header("Authorization"));
            assertEquals(
                    "grant_type=client_credentials&scope=enzona_business_payment%20enzona_business_qr",
                    token.bodyText());
            var api = server.requests().get(1);
            assertEquals("GET", api.method());
            assertEquals("/payment/v1.0.0/payments/abc", api.rawPath());
            assertEquals("Bearer live-token", api.header("Authorization"));
            assertEquals("enzona-sdk-java/" + EnzonaVersion.get(), api.header("User-Agent"));
            assertEquals("application/json", api.header("Accept"));
        }
    }

    @Test
    void clientsWithDifferentCredentialsShareNothing() {
        FakeTransport a = FakeTransport.returning(FakeTransport.json(200, tokenFor("token-a")));
        FakeTransport b = FakeTransport.returning(FakeTransport.json(200, tokenFor("token-b")));
        EnzonaClient clientA =
                EnzonaClient.builder().credentials("A", "secret-a").transport(a).build();
        EnzonaClient clientB =
                EnzonaClient.builder().credentials("B", "secret-b").transport(b).build();
        Operation op = Operation.get("/payment/v1.0.0/payments/x", null);

        // both transports answer the token request first, then the API call with the same script
        a.thenReturn(FakeTransport.json(200, "{}"));
        b.thenReturn(FakeTransport.json(200, "{}"));
        clientA.executor().execute(op, bytes -> "");
        clientB.executor().execute(op, bytes -> "");

        assertEquals("Bearer token-a", a.lastRequest().headers().get("Authorization"));
        assertEquals("Bearer token-b", b.lastRequest().headers().get("Authorization"));
        assertEquals("Basic QTpzZWNyZXQtYQ==", a.requests().get(0).headers().get("Authorization"));
        assertEquals("Basic QjpzZWNyZXQtYg==", b.requests().get(0).headers().get("Authorization"));
    }

    @Test
    void retryPolicyAndClockAreAccepted() {
        EnzonaClient c =
                withCredentials()
                        .retryPolicy(
                                RetryPolicy.exponentialBackoff(
                                        2, Duration.ofMillis(1), Duration.ofMillis(2)))
                        .clock(java.time.Clock.systemUTC())
                        .scopeSeparator(",")
                        .build();
        assertTrue(c.settings().retryPolicy().enabled());
    }

    private static String tokenFor(String value) {
        return "{\"access_token\":\"" + value + "\",\"expires_in\":3600}";
    }
}
