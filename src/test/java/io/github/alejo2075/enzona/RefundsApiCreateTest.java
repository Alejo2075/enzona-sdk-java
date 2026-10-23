package io.github.alejo2075.enzona;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.alejo2075.enzona.exception.ValidationException;
import io.github.alejo2075.enzona.http.ApiRequest;
import io.github.alejo2075.enzona.model.PaymentStatus;
import io.github.alejo2075.enzona.model.Refund;
import io.github.alejo2075.enzona.model.RefundRequest;
import io.github.alejo2075.enzona.testsupport.Clients;
import io.github.alejo2075.enzona.testsupport.FakeTransport;
import io.github.alejo2075.enzona.testsupport.Fixtures;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class RefundsApiCreateTest {

    private static final String UUID = "9f0b2c0c-4f9e-4d6a-9d3c-1a2b3c4d5e6f";

    private static FakeTransport refund() {
        return Clients.scripted(
                FakeTransport.json(200, Fixtures.text("refunds/refund-response.json")));
    }

    @Test
    void aPartialRefundPostsTheAmountToTheRefundPath() {
        FakeTransport t = refund();

        Refund r =
                Clients.client(t)
                        .refunds()
                        .create(
                                UUID,
                                RefundRequest.builder()
                                        .amount(new BigDecimal("50"))
                                        .description("Partial refund")
                                        .build());

        ApiRequest sent = Clients.apiRequests(t).get(0);
        assertEquals("POST", sent.method());
        assertEquals(
                URI.create("https://api.enzona.net/payment/v1.0.0/payments/" + UUID + "/refund"),
                sent.uri());
        assertEquals(
                "{\"amount\":{\"total\":\"50.00\"},\"description\":\"Partial refund\"}",
                new String(sent.body(), StandardCharsets.UTF_8));
        assertEquals("Bearer test-token", sent.headers().get("Authorization"));
        assertEquals(UUID, r.parentPaymentUuid());
        assertEquals(PaymentStatus.ACCEPTED, r.status());
    }

    @Test
    void aFullRefundSendsAnEmptyObject() {
        FakeTransport t = refund();

        Clients.client(t).refunds().create(UUID);

        assertEquals(
                "{}", new String(Clients.apiRequests(t).get(0).body(), StandardCharsets.UTF_8));
    }

    @Test
    void theIdentifierIsEncodedAndValidated() {
        FakeTransport t = refund();
        EnzonaClient client = Clients.client(t);

        client.refunds().create("a/b");
        assertEquals(
                "/payment/v1.0.0/payments/a%2Fb/refund",
                Clients.apiRequests(t).get(0).uri().getRawPath());
        assertThrows(IllegalArgumentException.class, () -> client.refunds().create(" "));
        assertThrows(NullPointerException.class, () -> client.refunds().create(UUID, null));
        assertEquals(1, Clients.apiRequests(t).size());
    }

    @Test
    void businessErrorsKeepTheEnzonaCode() {
        FakeTransport t =
                Clients.scripted(
                        FakeTransport.json(
                                400,
                                "{\"code\":4068,\"message\":\"Excede la cantidad a devolver\"}"));

        ValidationException e =
                assertThrows(
                        ValidationException.class, () -> Clients.client(t).refunds().create(UUID));

        assertEquals(4068, e.errorCode().getAsInt());
    }

    @Test
    void aRefundIsNeverRetriedEvenWithARetryPolicy() {
        for (Object failure :
                new Object[] {
                    FakeTransport.json(500, "{}"),
                    FakeTransport.json(502, "{}"),
                    FakeTransport.networkError()
                }) {
            FakeTransport t =
                    Clients.scripted(
                            failure,
                            FakeTransport.json(200, Fixtures.text("refunds/refund-response.json")));
            assertThrows(
                    RuntimeException.class,
                    () -> Clients.client(t, Clients.FAST_RETRIES).refunds().create(UUID));
            assertEquals(1, Clients.apiRequests(t).size(), String.valueOf(failure));
        }
    }

    @Test
    void createAsyncReturnsTheRefund() {
        FakeTransport t = refund();

        Refund r = Clients.client(t).refunds().createAsync(UUID, RefundRequest.full()).join();

        assertEquals(UUID, r.parentPaymentUuid());
    }
}
