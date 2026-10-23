package io.github.alejo2075.enzona;

import io.github.alejo2075.enzona.internal.JsonCodec;
import io.github.alejo2075.enzona.internal.Operation;
import io.github.alejo2075.enzona.internal.RequestExecutor;
import io.github.alejo2075.enzona.internal.UrlEncoding;
import io.github.alejo2075.enzona.internal.Validation;
import io.github.alejo2075.enzona.model.Refund;
import io.github.alejo2075.enzona.model.RefundRequest;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Refunds of the ENZONA Payment API v1.0.0. Obtain it from {@link EnzonaClient#refunds()}.
 *
 * <p>Creating a refund moves money back to the buyer, so it is <strong>never retried</strong>
 * automatically, even if a retry policy is configured.
 */
public final class RefundsApi {

    private static final String PAYMENTS = Operation.PAYMENT_API + "/payments";

    private final RequestExecutor executor;

    RefundsApi(RequestExecutor executor) {
        this.executor = executor;
    }

    /**
     * Refunds a payment, fully or in part.
     *
     * <p>If this call fails with a transport error the outcome is unknown: list the refunds of the
     * payment before trying again, or the buyer could be refunded twice.
     *
     * @param transactionUuid the identifier of the payment to refund
     * @param request {@link RefundRequest#full()} or a request with an amount
     * @return the refund
     * @throws io.github.alejo2075.enzona.exception.EnzonaApiException if ENZONA rejects it, for
     *     example because the payment is not confirmed or the amount exceeds what can be refunded
     */
    public Refund create(String transactionUuid, RefundRequest request) {
        return executor.execute(
                createOp(transactionUuid, request), b -> JsonCodec.read(b, Refund.class));
    }

    /**
     * Refunds the whole payment.
     *
     * @param transactionUuid the identifier of the payment to refund
     * @return the refund
     */
    public Refund create(String transactionUuid) {
        return create(transactionUuid, RefundRequest.full());
    }

    /**
     * Asynchronous form of {@link #create(String, RefundRequest)}.
     *
     * @param transactionUuid the identifier of the payment to refund
     * @param request the refund to make
     * @return a future with the refund
     */
    public CompletableFuture<Refund> createAsync(String transactionUuid, RefundRequest request) {
        return executor.executeAsync(
                createOp(transactionUuid, request), b -> JsonCodec.read(b, Refund.class));
    }

    private static Operation createOp(String transactionUuid, RefundRequest request) {
        Objects.requireNonNull(request, "request");
        Validation.id(transactionUuid, "transactionUuid");
        return Operation.post(
                PAYMENTS + "/" + UrlEncoding.encode(transactionUuid) + "/refund", request);
    }

    /**
     * Fetches a refund. Read-only, so the client's retry policy (if enabled) may repeat it.
     *
     * @param refundUuid the {@code transaction_uuid} of the refund (not of the payment)
     * @return the refund
     * @throws io.github.alejo2075.enzona.exception.NotFoundException if the refund does not exist
     */
    public Refund get(String refundUuid) {
        return executor.execute(getOp(refundUuid), b -> JsonCodec.read(b, Refund.class));
    }

    /**
     * Asynchronous form of {@link #get(String)}.
     *
     * @param refundUuid the {@code transaction_uuid} of the refund
     * @return a future with the refund
     */
    public CompletableFuture<Refund> getAsync(String refundUuid) {
        return executor.executeAsync(getOp(refundUuid), b -> JsonCodec.read(b, Refund.class));
    }

    private static Operation getOp(String refundUuid) {
        Validation.id(refundUuid, "refundUuid");
        // The path uses the singular "refund" here, unlike the list ("refunds").
        return Operation.get(PAYMENTS + "/refund/" + UrlEncoding.encode(refundUuid), null);
    }
}
