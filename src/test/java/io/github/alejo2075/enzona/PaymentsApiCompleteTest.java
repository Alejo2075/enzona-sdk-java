package io.github.alejo2075.enzona;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.alejo2075.enzona.exception.EnzonaApiException;
import io.github.alejo2075.enzona.exception.ValidationException;
import io.github.alejo2075.enzona.http.ApiRequest;
import io.github.alejo2075.enzona.model.Payment;
import io.github.alejo2075.enzona.model.PaymentStatus;
import io.github.alejo2075.enzona.testsupport.Clients;
import io.github.alejo2075.enzona.testsupport.FakeTransport;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PaymentsApiCompleteTest {

    private static final String UUID = "9f0b2c0c-4f9e-4d6a-9d3c-1a2b3c4d5e6f";
    private static final String ACCEPTED =
            "{\"transaction_uuid\":\""
                    + UUID
                    + "\",\"status_code\":1111,\"status_denom\":\"Aceptada\"}";

    @Test
    void postsToCompleteWithAnEmptyJsonObject() {
        FakeTransport t = Clients.scripted(FakeTransport.json(200, ACCEPTED));

        Payment p = Clients.client(t).payments().complete(UUID);

        ApiRequest r = Clients.apiRequests(t).get(0);
        assertEquals("POST", r.method());
        assertEquals(
                URI.create("https://api.enzona.net/payment/v1.0.0/payments/" + UUID + "/complete"),
                r.uri());
        assertEquals("Bearer test-token", r.headers().get("Authorization"));
        assertEquals("application/json", r.headers().get("Content-Type"));
        assertEquals("{}", new String(r.body(), StandardCharsets.UTF_8));
        assertEquals(PaymentStatus.ACCEPTED, p.status());
        assertEquals(UUID, p.transactionUuid());
    }

    @Test
    void theIdentifierIsEncodedAndMustNotBeBlank() {
        FakeTransport t = Clients.scripted(FakeTransport.json(200, ACCEPTED));
        EnzonaClient client = Clients.client(t);

        client.payments().complete("a/b");
        assertEquals(
                "/payment/v1.0.0/payments/a%2Fb/complete",
                Clients.apiRequests(t).get(0).uri().getRawPath());
        assertThrows(IllegalArgumentException.class, () -> client.payments().complete(" "));
        assertThrows(IllegalArgumentException.class, () -> client.payments().completeAsync(null));
        assertEquals(1, Clients.apiRequests(t).size());
    }

    @Test
    void aBusinessErrorKeepsTheEnzonaCode() {
        FakeTransport t =
                Clients.scripted(
                        FakeTransport.json(400, "{\"code\":4065,\"message\":\"No confirmado\"}"));

        ValidationException e =
                assertThrows(
                        ValidationException.class,
                        () -> Clients.client(t).payments().complete(UUID));

        assertEquals(4065, e.errorCode().getAsInt());
    }

    @Test
    void completeIsNeverRetriedEvenWithARetryPolicy() {
        for (Object failure :
                new Object[] {
                    FakeTransport.json(500, "{}"),
                    FakeTransport.json(503, "{}"),
                    FakeTransport.json(429, "{}", Map.of("Retry-After", List.of("0"))),
                    FakeTransport.networkError()
                }) {
            FakeTransport t = Clients.scripted(failure, FakeTransport.json(200, ACCEPTED));
            assertThrows(
                    RuntimeException.class,
                    () -> Clients.client(t, Clients.FAST_RETRIES).payments().complete(UUID));
            assertEquals(1, Clients.apiRequests(t).size(), String.valueOf(failure));
        }
    }

    @Test
    void anExpiredTokenIsRenewedAndTheCompleteRepeatedExactlyOnce() {
        FakeTransport t =
                FakeTransport.returning(
                        FakeTransport.json(200, Clients.TOKEN.replace("test-token", "old")),
                        FakeTransport.json(
                                401,
                                "{\"fault\":{\"code\":900901,\"message\":\"Invalid Credentials\"}}"),
                        FakeTransport.json(200, Clients.TOKEN.replace("test-token", "new")),
                        FakeTransport.json(200, ACCEPTED));

        Payment p = Clients.client(t).payments().complete(UUID);

        assertEquals(PaymentStatus.ACCEPTED, p.status());
        List<ApiRequest> sent = t.requests();
        assertEquals(4, sent.size(), "token, complete (401), token, complete");
        assertEquals("Bearer old", sent.get(1).headers().get("Authorization"));
        assertEquals("Bearer new", sent.get(3).headers().get("Authorization"));
    }

    @Test
    void aSecondRejectionIsReportedAsAnApiException() {
        String rejected = "{\"fault\":{\"code\":900901,\"message\":\"Invalid Credentials\"}}";
        FakeTransport t =
                FakeTransport.returning(
                        FakeTransport.json(200, Clients.TOKEN.replace("test-token", "one")),
                        FakeTransport.json(401, rejected),
                        FakeTransport.json(200, Clients.TOKEN.replace("test-token", "two")),
                        FakeTransport.json(401, rejected));

        EnzonaApiException e =
                assertThrows(
                        EnzonaApiException.class,
                        () -> Clients.client(t).payments().complete(UUID));

        assertEquals(401, e.statusCode());
        long completes =
                t.requests().stream()
                        .filter(r -> r.uri().getRawPath().endsWith("/complete"))
                        .count();
        assertEquals(2, completes);
    }

    @Test
    void failingToRenewTheTokenIsATokenException() {
        FakeTransport t =
                FakeTransport.returning(
                        FakeTransport.json(200, Clients.TOKEN),
                        FakeTransport.json(401, "{\"fault\":{\"code\":900901}}"));

        assertThrows(
                io.github.alejo2075.enzona.exception.TokenException.class,
                () -> Clients.client(t).payments().complete(UUID));
    }

    @Test
    void completeAsyncReturnsThePayment() {
        FakeTransport t = Clients.scripted(FakeTransport.json(200, ACCEPTED));

        assertEquals(
                PaymentStatus.ACCEPTED,
                Clients.client(t).payments().completeAsync(UUID).join().status());
    }
}
