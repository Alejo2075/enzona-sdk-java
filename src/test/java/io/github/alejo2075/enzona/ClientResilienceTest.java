package io.github.alejo2075.enzona;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.alejo2075.enzona.exception.EnzonaApiException;
import io.github.alejo2075.enzona.exception.EnzonaException;
import io.github.alejo2075.enzona.exception.RateLimitException;
import io.github.alejo2075.enzona.exception.TokenException;
import io.github.alejo2075.enzona.exception.TransportException;
import io.github.alejo2075.enzona.http.RetryPolicy;
import io.github.alejo2075.enzona.testsupport.Clients;
import io.github.alejo2075.enzona.testsupport.Fixtures;
import io.github.alejo2075.enzona.testsupport.TestServer;
import io.github.alejo2075.enzona.testsupport.TestServer.Recorded;
import io.github.alejo2075.enzona.testsupport.TestServer.Stub;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** Behaviour against a real loopback server: timeouts, interruption, redirects, Retry-After. */
class ClientResilienceTest {

    private static EnzonaClient.Builder against(TestServer server) {
        return EnzonaClient.builder()
                .credentials("key", "secret")
                .baseUrl(server.baseUri().toString())
                .allowInsecureBaseUrlForTesting();
    }

    private static Stub tokenOr(Recorded req, Stub api) {
        return req.rawPath().equals("/token") ? Stub.json(200, Clients.TOKEN) : api;
    }

    @Test
    void aSlowServerTimesOutAsATransportException() {
        try (TestServer server =
                TestServer.start(
                        req ->
                                tokenOr(
                                        req,
                                        Stub.json(
                                                        200,
                                                        Fixtures.text(
                                                                "payments/payment-details.json"))
                                                .delayed(1500)))) {
            EnzonaClient client = against(server).requestTimeout(Duration.ofMillis(300)).build();

            long start = System.nanoTime();
            assertThrows(TransportException.class, () -> client.payments().get("p-1"));
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;

            assertTrue(elapsedMs < 1400, "timed out after " + elapsedMs + " ms");
        }
    }

    @Test
    void aSlowTokenEndpointTimesOutAsATokenException() {
        try (TestServer server =
                TestServer.start(req -> Stub.json(200, Clients.TOKEN).delayed(1500))) {
            EnzonaClient client = against(server).requestTimeout(Duration.ofMillis(300)).build();

            TokenException e =
                    assertThrows(TokenException.class, () -> client.payments().get("p-1"));

            assertInstanceOf(TransportException.class, e.getCause());
        }
    }

    @Test
    void anInterruptedThreadKeepsItsInterruptFlagAndFailsWithATypedException() {
        try (TestServer server =
                TestServer.start(
                        req ->
                                tokenOr(
                                        req,
                                        Stub.json(
                                                200,
                                                Fixtures.text("payments/payment-details.json"))))) {
            EnzonaClient client = against(server).build();
            Thread.currentThread().interrupt();
            try {
                assertThrows(EnzonaException.class, () -> client.payments().get("p-1"));
                assertTrue(Thread.currentThread().isInterrupted(), "interrupt flag must survive");
            } finally {
                Thread.interrupted();
            }
        }
    }

    @Test
    void redirectsAreNeverFollowedSoTheBearerCannotReachAnotherHost() {
        try (TestServer elsewhere = TestServer.start(Stub.json(200, "{}"));
                TestServer server =
                        TestServer.start(
                                req ->
                                        tokenOr(
                                                req,
                                                Stub.of(
                                                        302,
                                                        Map.of(
                                                                "Location",
                                                                elsewhere.baseUri() + "stolen"),
                                                        "")))) {
            EnzonaClient client = against(server).build();

            EnzonaApiException e =
                    assertThrows(EnzonaApiException.class, () -> client.payments().get("p-1"));

            assertEquals(302, e.statusCode());
            assertTrue(elsewhere.requests().isEmpty(), "the redirect target must not be contacted");
        }
    }

    @Test
    void aReadIsRetriedAfterARateLimitWithRetryAfterWhenPolicyAllows() {
        AtomicInteger apiCalls = new AtomicInteger();
        try (TestServer server =
                TestServer.start(
                        req -> {
                            if (req.rawPath().equals("/token")) {
                                return Stub.json(200, Clients.TOKEN);
                            }
                            return apiCalls.getAndIncrement() == 0
                                    ? Stub.of(429, Map.of("Retry-After", "0"), "{\"code\":900800}")
                                    : Stub.json(
                                            200, Fixtures.text("payments/payment-details.json"));
                        })) {
            EnzonaClient client =
                    against(server)
                            .retryPolicy(
                                    RetryPolicy.exponentialBackoff(
                                            2, Duration.ofMillis(1), Duration.ofSeconds(2)))
                            .build();

            assertEquals("123456789012", client.payments().get("p-1").merchantOpId());
            assertEquals(2, apiCalls.get(), "the 429 and the retry");
        }
    }

    @Test
    void aRetryAfterBeyondTheLimitIsReportedWithoutWaiting() {
        AtomicInteger apiCalls = new AtomicInteger();
        try (TestServer server =
                TestServer.start(
                        req -> {
                            if (req.rawPath().equals("/token")) {
                                return Stub.json(200, Clients.TOKEN);
                            }
                            apiCalls.incrementAndGet();
                            return Stub.of(429, Map.of("Retry-After", "3600"), "{}");
                        })) {
            EnzonaClient client =
                    against(server)
                            .retryPolicy(
                                    RetryPolicy.exponentialBackoff(
                                            3, Duration.ofMillis(1), Duration.ofSeconds(1)))
                            .build();

            RateLimitException e =
                    assertThrows(RateLimitException.class, () -> client.payments().get("p-1"));

            assertEquals(Duration.ofHours(1), e.retryAfter().orElseThrow());
            assertEquals(1, apiCalls.get());
        }
    }

    @Test
    void aMoneyOperationIsNotRetriedAfterARateLimit() {
        AtomicInteger apiCalls = new AtomicInteger();
        try (TestServer server =
                TestServer.start(
                        req -> {
                            if (req.rawPath().equals("/token")) {
                                return Stub.json(200, Clients.TOKEN);
                            }
                            apiCalls.incrementAndGet();
                            return Stub.of(429, Map.of("Retry-After", "0"), "{}");
                        })) {
            EnzonaClient client =
                    against(server)
                            .retryPolicy(
                                    RetryPolicy.exponentialBackoff(
                                            3, Duration.ofMillis(1), Duration.ofSeconds(1)))
                            .build();

            assertThrows(RateLimitException.class, () -> client.payments().complete("p-1"));
            assertEquals(1, apiCalls.get());
        }
    }

    @Test
    void identifiersCannotEscapeTheirPathSegmentOnTheWire() {
        try (TestServer server =
                TestServer.start(
                        req ->
                                tokenOr(
                                        req,
                                        Stub.json(
                                                200,
                                                Fixtures.text("payments/payment-details.json"))))) {
            EnzonaClient client = against(server).build();

            client.payments().get("../../token?x=1#frag");

            Recorded api = server.requests().get(1);
            assertEquals("/payment/v1.0.0/payments/..%2F..%2Ftoken%3Fx%3D1%23frag", api.rawPath());
            assertEquals(2, server.requests().size());
        }
    }
}
