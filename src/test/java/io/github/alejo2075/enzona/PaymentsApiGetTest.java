package io.github.alejo2075.enzona;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.alejo2075.enzona.exception.NotFoundException;
import io.github.alejo2075.enzona.exception.SerializationException;
import io.github.alejo2075.enzona.exception.ServerException;
import io.github.alejo2075.enzona.http.ApiRequest;
import io.github.alejo2075.enzona.model.Payment;
import io.github.alejo2075.enzona.model.PaymentStatus;
import io.github.alejo2075.enzona.testsupport.Clients;
import io.github.alejo2075.enzona.testsupport.FakeTransport;
import io.github.alejo2075.enzona.testsupport.Fixtures;
import java.math.BigDecimal;
import java.net.URI;
import org.junit.jupiter.api.Test;

class PaymentsApiGetTest {

    private static final String UUID = "9f0b2c0c-4f9e-4d6a-9d3c-1a2b3c4d5e6f";

    private static FakeTransport details() {
        return Clients.scripted(
                FakeTransport.json(200, Fixtures.text("payments/payment-details.json")));
    }

    @Test
    void getsThePaymentByUuidWithBearerAndNoBody() {
        FakeTransport t = details();

        Payment p = Clients.client(t).payments().get(UUID);

        ApiRequest r = Clients.apiRequests(t).get(0);
        assertEquals("GET", r.method());
        assertEquals(URI.create("https://api.enzona.net/payment/v1.0.0/payments/" + UUID), r.uri());
        assertEquals("Bearer test-token", r.headers().get("Authorization"));
        assertNull(r.body());
        assertEquals(PaymentStatus.CONFIRMED, p.status());
        assertEquals(new BigDecimal("112"), p.amount().total());
        assertEquals("123456789012", p.merchantOpId());
    }

    @Test
    void theIdentifierCannotChangeThePath() {
        FakeTransport t = details();

        Clients.client(t).payments().get("../refunds/x?y=1");

        assertEquals(
                "/payment/v1.0.0/payments/..%2Frefunds%2Fx%3Fy%3D1",
                Clients.apiRequests(t).get(0).uri().getRawPath());
    }

    @Test
    void aBlankIdentifierIsRejectedWithoutCallingTheApi() {
        FakeTransport t = details();
        EnzonaClient client = Clients.client(t);

        assertThrows(IllegalArgumentException.class, () -> client.payments().get(" "));
        assertThrows(IllegalArgumentException.class, () -> client.payments().get(null));
        assertThrows(IllegalArgumentException.class, () -> client.payments().getAsync(""));
        assertEquals(0, t.requests().size());
    }

    @Test
    void anUnknownPaymentIsANotFoundExceptionWithTheEnzonaCode() {
        FakeTransport t =
                Clients.scripted(
                        FakeTransport.json(
                                404, "{\"code\":4064,\"message\":\"Recurso no disponible\"}"));

        NotFoundException e =
                assertThrows(NotFoundException.class, () -> Clients.client(t).payments().get(UUID));

        assertEquals(4064, e.errorCode().getAsInt());
    }

    @Test
    void readsAreRetriedWhenARetryPolicyIsEnabled() {
        FakeTransport t =
                Clients.scripted(
                        FakeTransport.json(503, "{}"),
                        FakeTransport.networkError(),
                        FakeTransport.json(200, Fixtures.text("payments/payment-details.json")));

        Payment p = Clients.client(t, Clients.FAST_RETRIES).payments().get(UUID);

        assertEquals(PaymentStatus.CONFIRMED, p.status());
        assertEquals(3, Clients.apiRequests(t).size());
    }

    @Test
    void readsAreNotRetriedByDefault() {
        FakeTransport t =
                Clients.scripted(
                        FakeTransport.json(503, "{}"),
                        FakeTransport.json(200, Fixtures.text("payments/payment-details.json")));

        assertThrows(ServerException.class, () -> Clients.client(t).payments().get(UUID));
        assertEquals(1, Clients.apiRequests(t).size());
    }

    @Test
    void anUnreadableBodyIsASerializationException() {
        FakeTransport t = Clients.scripted(FakeTransport.json(200, "<html>proxy page</html>"));

        assertThrows(SerializationException.class, () -> Clients.client(t).payments().get(UUID));
    }

    @Test
    void getAsyncReturnsThePayment() {
        FakeTransport t = details();

        Payment p = Clients.client(t).payments().getAsync(UUID).join();

        assertEquals(PaymentStatus.CONFIRMED, p.status());
    }
}
