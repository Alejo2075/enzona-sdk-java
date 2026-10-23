package example;

import io.github.alejo2075.enzona.EnzonaClient;
import io.github.alejo2075.enzona.model.Refund;
import io.github.alejo2075.enzona.model.RefundPage;
import io.github.alejo2075.enzona.model.RefundRequest;
import java.math.BigDecimal;

/** Refunding a completed payment, fully or in part. */
public final class Refunds {

    private Refunds() {}

    public static Refund refundPart(EnzonaClient client, String transactionUuid) {
        // snippet:partial:start
        Refund refund = client.refunds().create(
                transactionUuid,
                RefundRequest.builder()
                        .amount(new BigDecimal("50.00"))
                        .description("Damaged item")
                        .build());
        System.out.println("Refund " + refund.transactionUuid() + ": " + refund.status());
        // snippet:partial:end
        return refund;
    }

    public static Refund refundAll(EnzonaClient client, String transactionUuid) {
        // snippet:full:start
        return client.refunds().create(transactionUuid);
        // snippet:full:end
    }

    public static RefundPage refundsOf(EnzonaClient client, String transactionUuid) {
        // snippet:list:start
        RefundPage page = client.refunds().listForPayment(transactionUuid);
        page.refunds().forEach(r -> System.out.println(r.transactionUuid() + " " + r.statusDenom()));
        // snippet:list:end
        return page;
    }
}
