package io.github.alejo2075.enzona;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.alejo2075.enzona.exception.ServerException;
import io.github.alejo2075.enzona.exception.TransportException;
import io.github.alejo2075.enzona.exception.ValidationException;
import io.github.alejo2075.enzona.http.ApiRequest;
import io.github.alejo2075.enzona.model.CreatePaymentRequest;
import io.github.alejo2075.enzona.model.Payment;
import io.github.alejo2075.enzona.model.PaymentItem;
import io.github.alejo2075.enzona.model.PaymentStatus;
import io.github.alejo2075.enzona.testsupport.Clients;
import io.github.alejo2075.enzona.testsupport.FakeTransport;
import io.github.alejo2075.enzona.testsupport.Fixtures;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.Test;

class PaymentsApiCreateTest {

    private static CreatePaymentRequest request() {
        return CreatePaymentRequest.builder()
                .description("Order #123")
                .currency("CUP")
                .item(
                        PaymentItem.of(
                                "T-shirt", "Blue", 2, new BigDecimal("50"), new BigDecimal("2")))
                .shipping(new BigDecimal("10"))
                .merchantOpId("123456789012")
                .invoiceNumber("1001")
                .returnUrl("https://shop.example/enzona/return")
                .cancelUrl("https://shop.example/enzona/cancel")
                .terminalId("12")
                .build();
    }

    @Test
    void postsToPaymentsWithBearerAndJsonAndParsesTheResponse() {
        FakeTransport t =
                Clients.scripted(
                        FakeTransport.json(200, Fixtures.text("payments/create-response.json")));

        Payment p = Clients.client(t).payments().create(request());

        ApiRequest r = Clients.apiRequests(t).get(0);
        assertEquals("POST", r.method());
        assertEquals(URI.create("https://api.enzona.net/payment/v1.0.0/payments"), r.uri());
        assertEquals("Bearer test-token", r.headers().get("Authorization"));
        assertEquals("application/json", r.headers().get("Content-Type"));
        String body = new String(r.body(), StandardCharsets.UTF_8);
        assertTrue(body.contains("\"merchant_op_id\":\"123456789012\""), body);
        assertTrue(body.contains("\"total\":\"112.00\""), body);

        assertEquals("9f0b2c0c-4f9e-4d6a-9d3c-1a2b3c4d5e6f", p.transactionUuid());
        assertEquals(PaymentStatus.PENDING, p.status());
        assertTrue(p.confirmationUrl().isPresent());
    }

    @Test
    void sandboxEnvironmentUsesTheSandboxHost() {
        FakeTransport t =
                Clients.scripted(
                        FakeTransport.json(200, Fixtures.text("payments/create-response.json")));
        EnzonaClient client =
                EnzonaClient.builder()
                        .credentials("key", "secret")
                        .environment(Environment.SANDBOX)
                        .transport(t)
                        .build();

        client.payments().create(request());

        assertEquals("https://apisandbox.enzona.net/token", t.requests().get(0).uri().toString());
        assertEquals(
                "https://apisandbox.enzona.net/payment/v1.0.0/payments",
                Clients.apiRequests(t).get(0).uri().toString());
    }

    @Test
    void serverSideValidationErrorsAreTypedWithTheEnzonaCode() {
        FakeTransport t =
                Clients.scripted(
                        FakeTransport.json(
                                400,
                                "{\"code\":4084,\"message\":\"Formato incorrecto en la estructura"
                                        + " del pago\"}"));

        ValidationException e =
                assertThrows(
                        ValidationException.class,
                        () -> Clients.client(t).payments().create(request()));

        assertEquals(4084, e.errorCode().getAsInt());
    }

    @Test
    void createIsNeverRetriedEvenWithARetryPolicy() {
        for (Object failure :
                new Object[] {FakeTransport.json(500, "{}"), FakeTransport.networkError()}) {
            FakeTransport t =
                    Clients.scripted(
                            failure,
                            FakeTransport.json(
                                    200, Fixtures.text("payments/create-response.json")));
            assertThrows(
                    RuntimeException.class,
                    () -> Clients.client(t, Clients.FAST_RETRIES).payments().create(request()));
            assertEquals(1, Clients.apiRequests(t).size(), String.valueOf(failure));
        }
    }

    @Test
    void serverFailureIsAServerException() {
        FakeTransport t = Clients.scripted(FakeTransport.json(503, "{}"));
        assertThrows(ServerException.class, () -> Clients.client(t).payments().create(request()));
    }

    @Test
    void createAsyncReturnsTheSamePayment() {
        FakeTransport t =
                Clients.scripted(
                        FakeTransport.json(200, Fixtures.text("payments/create-response.json")));

        Payment p = Clients.client(t).payments().createAsync(request()).join();

        assertEquals("9f0b2c0c-4f9e-4d6a-9d3c-1a2b3c4d5e6f", p.transactionUuid());
    }

    @Test
    void createAsyncReportsTransportFailuresAsTypedExceptions() {
        FakeTransport t = Clients.scripted(FakeTransport.networkError());

        CompletionException e =
                assertThrows(
                        CompletionException.class,
                        () -> Clients.client(t).payments().createAsync(request()).join());
        assertInstanceOf(TransportException.class, e.getCause());
    }

    @Test
    void aNullRequestIsRejected() {
        assertThrows(
                NullPointerException.class,
                () -> Clients.client(Clients.scripted()).payments().create(null));
    }
}
