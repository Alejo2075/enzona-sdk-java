package io.github.alejo2075.enzona;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.alejo2075.enzona.exception.NotFoundException;
import io.github.alejo2075.enzona.http.ApiRequest;
import io.github.alejo2075.enzona.model.Refund;
import io.github.alejo2075.enzona.testsupport.Clients;
import io.github.alejo2075.enzona.testsupport.FakeTransport;
import io.github.alejo2075.enzona.testsupport.Fixtures;
import java.net.URI;
import org.junit.jupiter.api.Test;

class RefundsApiGetTest {

    private static final String UUID = "7a6b5c4d-3e2f-4a1b-8c9d-0e1f2a3b4c5d";

    private static FakeTransport refund() {
        return Clients.scripted(
                FakeTransport.json(200, Fixtures.text("refunds/refund-response.json")));
    }

    @Test
    void getsTheRefundFromTheSingularRefundPath() {
        FakeTransport t = refund();

        Refund r = Clients.client(t).refunds().get(UUID);

        ApiRequest sent = Clients.apiRequests(t).get(0);
        assertEquals("GET", sent.method());
        assertEquals(
                URI.create("https://api.enzona.net/payment/v1.0.0/payments/refund/" + UUID),
                sent.uri());
        assertNull(sent.body());
        assertEquals(UUID, r.transactionUuid());
    }

    @Test
    void theIdentifierIsEncodedAndValidated() {
        FakeTransport t = refund();
        EnzonaClient client = Clients.client(t);

        client.refunds().get("../x");
        assertEquals(
                "/payment/v1.0.0/payments/refund/..%2Fx",
                Clients.apiRequests(t).get(0).uri().getRawPath());
        assertThrows(IllegalArgumentException.class, () -> client.refunds().get(""));
        assertThrows(IllegalArgumentException.class, () -> client.refunds().getAsync(null));
    }

    @Test
    void anUnknownRefundIsNotFound() {
        FakeTransport t = Clients.scripted(FakeTransport.json(404, "{\"code\":4064}"));
        assertThrows(NotFoundException.class, () -> Clients.client(t).refunds().get(UUID));
    }

    @Test
    void readsAreRetriedOnlyWhenAPolicyIsEnabled() {
        FakeTransport enabled =
                Clients.scripted(
                        FakeTransport.json(503, "{}"),
                        FakeTransport.json(200, Fixtures.text("refunds/refund-response.json")));
        Clients.client(enabled, Clients.FAST_RETRIES).refunds().get(UUID);
        assertEquals(2, Clients.apiRequests(enabled).size());

        FakeTransport disabled =
                Clients.scripted(
                        FakeTransport.json(503, "{}"),
                        FakeTransport.json(200, Fixtures.text("refunds/refund-response.json")));
        assertThrows(RuntimeException.class, () -> Clients.client(disabled).refunds().get(UUID));
        assertEquals(1, Clients.apiRequests(disabled).size());
    }

    @Test
    void getAsyncReturnsTheRefund() {
        assertEquals(
                UUID, Clients.client(refund()).refunds().getAsync(UUID).join().transactionUuid());
    }
}
