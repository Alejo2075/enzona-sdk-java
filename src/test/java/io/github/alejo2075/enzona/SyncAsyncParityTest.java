package io.github.alejo2075.enzona;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.github.alejo2075.enzona.model.CreatePaymentRequest;
import io.github.alejo2075.enzona.model.PaymentItem;
import io.github.alejo2075.enzona.model.PaymentQuery;
import io.github.alejo2075.enzona.model.RefundQuery;
import io.github.alejo2075.enzona.model.RefundRequest;
import io.github.alejo2075.enzona.testsupport.Clients;
import io.github.alejo2075.enzona.testsupport.Fixtures;
import io.github.alejo2075.enzona.testsupport.TestServer;
import io.github.alejo2075.enzona.testsupport.TestServer.Stub;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

/**
 * Every operation gives the same result in its blocking and its asynchronous form, over real HTTP.
 */
class SyncAsyncParityTest {

    private static final String UUID = "9f0b2c0c-4f9e-4d6a-9d3c-1a2b3c4d5e6f";

    private static Stub answer(TestServer.Recorded req) {
        String path = req.rawPath();
        if (path.equals("/token")) {
            return Stub.json(200, Clients.TOKEN);
        }
        if (path.endsWith("/refunds")) {
            return Stub.json(200, Fixtures.text("refunds/refunds-list.json"));
        }
        if (path.contains("/payments/refund/") || path.endsWith("/refund")) {
            return Stub.json(200, Fixtures.text("refunds/refund-response.json"));
        }
        if (path.endsWith("/payments") && req.method().equals("GET")) {
            return Stub.json(200, Fixtures.text("payments/list-response.json"));
        }
        if (path.endsWith("/payments") || path.endsWith("/complete") || path.endsWith("/cancel")) {
            return Stub.json(200, Fixtures.text("payments/create-response.json"));
        }
        return Stub.json(200, Fixtures.text("payments/payment-details.json"));
    }

    @Test
    void blockingAndAsyncFormsReturnEqualResults() {
        try (TestServer server = TestServer.start(SyncAsyncParityTest::answer)) {
            EnzonaClient client =
                    EnzonaClient.builder()
                            .credentials("key", "secret")
                            .baseUrl(server.baseUri().toString())
                            .allowInsecureBaseUrlForTesting()
                            .build();
            CreatePaymentRequest request =
                    CreatePaymentRequest.builder()
                            .currency("CUP")
                            .merchantOpId("123456789012")
                            .item(PaymentItem.of("A", 1, new BigDecimal("10"), BigDecimal.ZERO))
                            .returnUrl("https://shop.example/ok")
                            .cancelUrl("https://shop.example/no")
                            .build();
            PaymentQuery pq = PaymentQuery.builder().limit(2).build();
            RefundQuery rq = RefundQuery.builder().limit(2).build();

            Map<String, Supplier<Object>> blocking = new LinkedHashMap<>();
            Map<String, Supplier<CompletableFuture<?>>> async = new LinkedHashMap<>();
            blocking.put("payments.create", () -> client.payments().create(request));
            async.put("payments.create", () -> client.payments().createAsync(request));
            blocking.put("payments.get", () -> client.payments().get(UUID));
            async.put("payments.get", () -> client.payments().getAsync(UUID));
            blocking.put("payments.list", () -> client.payments().list(pq));
            async.put("payments.list", () -> client.payments().listAsync(pq));
            blocking.put("payments.complete", () -> client.payments().complete(UUID));
            async.put("payments.complete", () -> client.payments().completeAsync(UUID));
            blocking.put("payments.cancel", () -> client.payments().cancel(UUID));
            async.put("payments.cancel", () -> client.payments().cancelAsync(UUID));
            blocking.put(
                    "refunds.create", () -> client.refunds().create(UUID, RefundRequest.full()));
            async.put(
                    "refunds.create",
                    () -> client.refunds().createAsync(UUID, RefundRequest.full()));
            blocking.put("refunds.get", () -> client.refunds().get(UUID));
            async.put("refunds.get", () -> client.refunds().getAsync(UUID));
            blocking.put("refunds.list", () -> client.refunds().list(rq));
            async.put("refunds.list", () -> client.refunds().listAsync(rq));
            blocking.put("refunds.listForPayment", () -> client.refunds().listForPayment(UUID, rq));
            async.put(
                    "refunds.listForPayment", () -> client.refunds().listForPaymentAsync(UUID, rq));

            assertEquals(async.keySet(), blocking.keySet());
            for (String name : blocking.keySet()) {
                Object sync = blocking.get(name).get();
                Object viaFuture = async.get(name).get().join();
                assertNotNull(sync, name);
                assertEquals(sync, viaFuture, name);
            }
            long tokenRequests =
                    server.requests().stream().filter(r -> r.rawPath().equals("/token")).count();
            assertEquals(1, tokenRequests, "the token is shared by every call");
        }
    }
}
