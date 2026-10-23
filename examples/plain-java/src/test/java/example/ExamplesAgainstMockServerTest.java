package example;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import example.PaymentFlow.Order;
import example.PaymentFlow.Outcome;
import io.github.alejo2075.enzona.EnzonaClient;
import io.github.alejo2075.enzona.model.Payment;
import io.github.alejo2075.enzona.model.PaymentStatus;
import io.github.alejo2075.enzona.model.Refund;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Uses only the public, installed SDK jar and a local mock of ENZONA. No ENZONA account and no money
 * are involved.
 */
class ExamplesAgainstMockServerTest {

    private MockEnzona enzona;

    @BeforeEach
    void start() throws IOException {
        enzona = new MockEnzona();
    }

    @AfterEach
    void stop() {
        enzona.close();
    }

    private EnzonaClient client() {
        return EnzonaClient.builder()
                .credentials("test-key", "test-secret-not-real")
                .baseUrl(enzona.baseUrl())
                .allowInsecureBaseUrlForTesting() // only because the mock server has no TLS
                .build();
    }

    @Test
    void theQuickStartCreatesAPaymentAndGivesTheConfirmationUrl() {
        Payment payment = QuickStart.createPayment(client());

        assertEquals("mock-1", payment.transactionUuid());
        assertEquals("https://enzona.example/confirm/mock-1", payment.confirmationUrl().orElseThrow().toString());
        assertEquals(PaymentStatus.PENDING, payment.status());
        // 2 x 50.00 + 2.00 tax + 10.00 shipping, computed by the SDK
        assertTrue(enzona.bodies.get(1).contains("\"total\":\"112.00\""), enzona.bodies.get(1));
    }

    @Test
    void theCompleteFlowChargesTheBuyerOnlyAfterVerification() {
        EnzonaClient client = client();
        Map<String, Order> orders = new HashMap<>();

        assertNotNull(PaymentFlow.checkout(client, orders, "A-1"));
        String uuid = orders.get("A-1").transactionUuid();

        // The buyer comes back without having confirmed: nothing is completed.
        assertEquals(Outcome.NOT_CONFIRMED, PaymentFlow.onBuyerReturned(client, orders, "A-1"));
        assertEquals(0, enzona.count("POST /payment/v1.0.0/payments/" + uuid + "/complete"));

        // The buyer confirms in ENZONA and comes back: now the payment is completed.
        enzona.buyerConfirms(uuid);
        assertEquals(Outcome.PAID, PaymentFlow.onBuyerReturned(client, orders, "A-1"));
        assertEquals(1, enzona.count("POST /payment/v1.0.0/payments/" + uuid + "/complete"));

        // Handling the return a second time is harmless: no second completion.
        assertEquals(Outcome.PAID, PaymentFlow.onBuyerReturned(client, orders, "A-1"));
        assertEquals(1, enzona.count("POST /payment/v1.0.0/payments/" + uuid + "/complete"));
    }

    @Test
    void aPaymentThatDoesNotMatchTheOrderIsNeverCompleted() {
        EnzonaClient client = client();
        Map<String, Order> orders = new HashMap<>();
        PaymentFlow.checkout(client, orders, "A-2");
        Order real = orders.get("A-2");
        enzona.buyerConfirms(real.transactionUuid());
        // The shop's records say the order was for a different amount.
        orders.put("A-2", new Order("A-2", new BigDecimal("1.00"), real.currency(), real.merchantOpId(), real.transactionUuid()));

        assertEquals(Outcome.MISMATCH, PaymentFlow.onBuyerReturned(client, orders, "A-2"));
        assertEquals(0, enzona.count("POST /payment/v1.0.0/payments/" + real.transactionUuid() + "/complete"));
    }

    @Test
    void refundsFullAndPartial() {
        EnzonaClient client = client();

        Refund part = Refunds.refundPart(client, "mock-9");
        assertEquals("mock-9", part.parentPaymentUuid());
        assertEquals(new BigDecimal("50.00"), part.amount().total());
        assertTrue(enzona.bodies.get(enzona.bodies.size() - 1).contains("\"total\":\"50.00\""));

        assertEquals("mock-9", Refunds.refundAll(client, "mock-9").parentPaymentUuid());
        assertEquals(1, Refunds.refundsOf(client, "mock-9").refunds().size());
    }

    @Test
    void advancedUsageHandlesAsyncAndErrorsByCode() {
        EnzonaClient client = client();
        Payment created = QuickStart.createPayment(client);

        assertEquals(PaymentStatus.PENDING, AdvancedUsage.statusAsync(client, created.transactionUuid()).join());

        enzona.force("POST /payment/v1.0.0/payments/p/refund", 400, "{\"code\":4067,\"message\":\"x\"}");
        assertEquals("Already refunded", AdvancedUsage.refundSafely(client, "p"));

        enzona.force("POST /payment/v1.0.0/payments/q/refund", 400, "{\"code\":4070,\"message\":\"x\"}");
        assertEquals("ENZONA rejected the refund, code OptionalInt[4070]", AdvancedUsage.refundSafely(client, "q"));

        enzona.force("POST /payment/v1.0.0/payments/r/refund", 429, "{\"code\":900800}");
        assertTrue(AdvancedUsage.refundSafely(client, "r").startsWith("Too many requests"));

        assertEquals("Refunded: refund-of-ok", AdvancedUsage.refundSafely(client, "ok"));
    }

    @Test
    void anInvalidTokenIsReportedAsAnAuthenticationProblem() {
        enzona.force("POST /token", 401, "{\"error\":\"invalid_client\"}");

        assertEquals(
                "Check the consumer key, the secret and the environment",
                AdvancedUsage.refundSafely(client(), "x"));
    }
}
