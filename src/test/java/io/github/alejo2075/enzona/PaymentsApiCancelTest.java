package io.github.alejo2075.enzona;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.alejo2075.enzona.exception.NotFoundException;
import io.github.alejo2075.enzona.http.ApiRequest;
import io.github.alejo2075.enzona.model.Payment;
import io.github.alejo2075.enzona.model.PaymentStatus;
import io.github.alejo2075.enzona.testsupport.Clients;
import io.github.alejo2075.enzona.testsupport.FakeTransport;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class PaymentsApiCancelTest {

    private static final String UUID = "9f0b2c0c-4f9e-4d6a-9d3c-1a2b3c4d5e6f";
    private static final String CANCELLED =
            "{\"transaction_uuid\":\""
                    + UUID
                    + "\",\"status_code\":1117,\"status_denom\":\"Cancelada\"}";

    @Test
    void postsToCancelWithAnEmptyJsonObject() {
        FakeTransport t = Clients.scripted(FakeTransport.json(200, CANCELLED));

        Payment p = Clients.client(t).payments().cancel(UUID);

        ApiRequest r = Clients.apiRequests(t).get(0);
        assertEquals("POST", r.method());
        assertEquals(
                URI.create("https://api.enzona.net/payment/v1.0.0/payments/" + UUID + "/cancel"),
                r.uri());
        assertEquals("{}", new String(r.body(), StandardCharsets.UTF_8));
        assertEquals(PaymentStatus.CANCELLED, p.status());
    }

    @Test
    void theIdentifierMustNotBeBlankAndIsEncoded() {
        FakeTransport t = Clients.scripted(FakeTransport.json(200, CANCELLED));
        EnzonaClient client = Clients.client(t);

        assertThrows(IllegalArgumentException.class, () -> client.payments().cancel(""));
        client.payments().cancel("x y");
        assertEquals(
                "/payment/v1.0.0/payments/x%20y/cancel",
                Clients.apiRequests(t).get(0).uri().getRawPath());
    }

    @Test
    void cancelIsNeverRetried() {
        for (Object failure :
                new Object[] {FakeTransport.json(500, "{}"), FakeTransport.networkError()}) {
            FakeTransport t = Clients.scripted(failure, FakeTransport.json(200, CANCELLED));
            assertThrows(
                    RuntimeException.class,
                    () -> Clients.client(t, Clients.FAST_RETRIES).payments().cancel(UUID));
            assertEquals(1, Clients.apiRequests(t).size(), String.valueOf(failure));
        }
    }

    @Test
    void anUnknownPaymentIsNotFound() {
        FakeTransport t = Clients.scripted(FakeTransport.json(404, "{\"code\":4064}"));
        assertThrows(NotFoundException.class, () -> Clients.client(t).payments().cancel(UUID));
    }

    @Test
    void cancelAsyncReturnsThePayment() {
        FakeTransport t = Clients.scripted(FakeTransport.json(200, CANCELLED));

        assertEquals(
                PaymentStatus.CANCELLED,
                Clients.client(t).payments().cancelAsync(UUID).join().status());
    }
}
