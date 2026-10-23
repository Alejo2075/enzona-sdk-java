package example;

import io.github.alejo2075.enzona.EnzonaClient;
import io.github.alejo2075.enzona.model.CreatePaymentRequest;
import io.github.alejo2075.enzona.model.Payment;
import io.github.alejo2075.enzona.model.PaymentItem;
import java.math.BigDecimal;
import java.net.URI;

/** Creates a payment with the given client and returns the URL the buyer must visit. */
public final class PaymentDemo {

    private PaymentDemo() {}

    public static URI createPaymentUrl(EnzonaClient client) {
        Payment payment = client.payments().create(
                CreatePaymentRequest.builder()
                        .currency("CUP")
                        .description("Test payment")
                        .item(PaymentItem.of("Course", 1, new BigDecimal("250.00"), BigDecimal.ZERO))
                        .merchantOpId("123456789012")
                        .returnUrl("https://shop.example/enzona/return?order=1")
                        .cancelUrl("https://shop.example/enzona/cancel?order=1")
                        .build());
        return payment.confirmationUrl().orElseThrow();
    }

    public static void main(String[] args) {
        EnzonaClient client = EnzonaClient.builder()
                .credentialsFromEnvironment()
                .build();
        System.out.println(createPaymentUrl(client));
    }
}
