package example;

import io.github.alejo2075.enzona.EnzonaClient;
import io.github.alejo2075.enzona.model.CreatePaymentRequest;
import io.github.alejo2075.enzona.model.Payment;
import io.github.alejo2075.enzona.model.PaymentItem;
import io.github.alejo2075.enzona.model.PaymentStatus;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Map;

/**
 * The complete payment flow of a shop. ENZONA has no signed webhooks and does not add the payment
 * identifier to your {@code returnUrl}, so the shop keeps it next to the order.
 */
public final class PaymentFlow {

    /** What the shop remembers about an order. */
    public record Order(
            String id,
            BigDecimal total,
            String currency,
            String merchantOpId,
            String transactionUuid) {}

    /** The result of handling the buyer's return. */
    public enum Outcome {
        /** The payment is completed (or already was). */
        PAID,
        /** The buyer has not confirmed the payment in ENZONA. */
        NOT_CONFIRMED,
        /** The payment does not match the order. Do not deliver anything. */
        MISMATCH
    }

    private PaymentFlow() {}

    /** Step 1: create the payment and remember its identifier. Returns where to send the buyer. */
    public static URI checkout(EnzonaClient client, Map<String, Order> orders, String orderId) {
        // snippet:checkout:start
        Payment payment = client.payments().create(
                CreatePaymentRequest.builder()
                        .currency("CUP")
                        .description("Order " + orderId)
                        .item(PaymentItem.of("Course", 1, new BigDecimal("250.00"), BigDecimal.ZERO))
                        .generateMerchantOpId()
                        .returnUrl("https://shop.example/enzona/return?order=" + orderId)
                        .cancelUrl("https://shop.example/enzona/cancel?order=" + orderId)
                        .build());

        // ENZONA does not put the uuid in the return URL: store it with the order now.
        orders.put(orderId, new Order(orderId, payment.amount().total(), payment.currency(),
                payment.merchantOpId(), payment.transactionUuid()));

        return payment.confirmationUrl().orElseThrow();
        // snippet:checkout:end
    }

    /** Step 2: the buyer came back. Never trust the redirect: look the payment up and verify it. */
    public static Outcome onBuyerReturned(EnzonaClient client, Map<String, Order> orders, String orderId) {
        // snippet:return:start
        Order order = orders.get(orderId);
        Payment payment = client.payments().get(order.transactionUuid());

        if (payment.status() == PaymentStatus.ACCEPTED) {
            return Outcome.PAID; // already completed: handling the return twice is harmless
        }
        if (payment.status() != PaymentStatus.CONFIRMED) {
            return Outcome.NOT_CONFIRMED;
        }
        boolean matches = order.currency().equals(payment.currency())
                && order.merchantOpId().equals(payment.merchantOpId())
                && order.total().compareTo(payment.amount().total()) == 0;
        if (!matches) {
            return Outcome.MISMATCH;
        }

        // Completing the payment is what charges the buyer. It is never retried by the SDK.
        Payment completed = client.payments().complete(order.transactionUuid());
        return completed.status() == PaymentStatus.ACCEPTED ? Outcome.PAID : Outcome.NOT_CONFIRMED;
        // snippet:return:end
    }
}
