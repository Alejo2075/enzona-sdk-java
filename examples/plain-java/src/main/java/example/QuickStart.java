package example;

// snippet:imports:start
import io.github.alejo2075.enzona.Environment;
import io.github.alejo2075.enzona.EnzonaClient;
import io.github.alejo2075.enzona.model.CreatePaymentRequest;
import io.github.alejo2075.enzona.model.Payment;
import io.github.alejo2075.enzona.model.PaymentItem;
import java.math.BigDecimal;
// snippet:imports:end

/**
 * Quick start. Run it with the sandbox credentials of your ENZONA application:
 *
 * <pre>
 * export ENZONA_CONSUMER_KEY=...
 * export ENZONA_CONSUMER_SECRET=...
 * </pre>
 *
 * The code between the snippet markers is copied verbatim into the README.
 */
public final class QuickStart {

    private QuickStart() {}

    public static EnzonaClient newClient() {
        // snippet:client:start
        EnzonaClient client = EnzonaClient.builder()
                .credentials(System.getenv("ENZONA_CONSUMER_KEY"), System.getenv("ENZONA_CONSUMER_SECRET"))
                .environment(Environment.SANDBOX)
                .build();
        // snippet:client:end
        return client;
    }

    public static Payment createPayment(EnzonaClient client) {
        // snippet:start
        Payment payment = client.payments().create(
                CreatePaymentRequest.builder()
                        .description("Order #123")
                        .currency("CUP")
                        .item(PaymentItem.of("T-shirt", 2, new BigDecimal("50.00"), new BigDecimal("2.00")))
                        .shipping(new BigDecimal("10.00"))
                        .merchantOpId("123456789012")
                        .returnUrl("https://shop.example/enzona/return?order=123")
                        .cancelUrl("https://shop.example/enzona/cancel?order=123")
                        .build());

        System.out.println("Send the buyer to: " + payment.confirmationUrl().orElseThrow());
        System.out.println("Keep this next to your order: " + payment.transactionUuid());
        // snippet:end
        return payment;
    }

    public static void main(String[] args) {
        createPayment(newClient());
    }
}
