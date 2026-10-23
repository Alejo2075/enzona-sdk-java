package io.github.alejo2075.enzona;

import io.github.alejo2075.enzona.internal.JsonCodec;
import io.github.alejo2075.enzona.internal.Operation;
import io.github.alejo2075.enzona.internal.RequestExecutor;
import io.github.alejo2075.enzona.internal.UrlEncoding;
import io.github.alejo2075.enzona.internal.Validation;
import io.github.alejo2075.enzona.model.Refund;
import io.github.alejo2075.enzona.model.RefundPage;
import io.github.alejo2075.enzona.model.RefundQuery;
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

    /**
     * Lists refunds of the merchant. Read-only, so the client's retry policy (if enabled) may
     * repeat it.
     *
     * @param query the filters and paging, see {@link RefundQuery}
     * @return one page of refunds
     */
    public RefundPage list(RefundQuery query) {
        return executor.execute(listOp(query), b -> JsonCodec.read(b, RefundPage.class));
    }

    /**
     * Lists refunds without filters.
     *
     * @return one page of refunds, as ENZONA's defaults define it
     */
    public RefundPage list() {
        return list(RefundQuery.builder().build());
    }

    /**
     * Asynchronous form of {@link #list(RefundQuery)}.
     *
     * @param query the filters and paging
     * @return a future with one page of refunds
     */
    public CompletableFuture<RefundPage> listAsync(RefundQuery query) {
        return executor.executeAsync(listOp(query), b -> JsonCodec.read(b, RefundPage.class));
    }

    private static Operation listOp(RefundQuery query) {
        Objects.requireNonNull(query, "query");
        return Operation.get(PAYMENTS + "/refunds", query.toQueryParameters());
    }

    /**
     * Lists the refunds of one payment. Read-only, so the client's retry policy (if enabled) may
     * repeat it.
     *
     * <p>Only the paging, status, date and order filters of the query are used; the filters that
     * identify the payment or the merchant are ignored because the payment is part of the path.
     *
     * @param transactionUuid the identifier of the payment
     * @param query the paging and filters
     * @return one page of refunds
     */
    public RefundPage listForPayment(String transactionUuid, RefundQuery query) {
        return executor.execute(
                listForPaymentOp(transactionUuid, query), b -> JsonCodec.read(b, RefundPage.class));
    }

    /**
     * Lists the refunds of one payment without filters.
     *
     * @param transactionUuid the identifier of the payment
     * @return one page of refunds
     */
    public RefundPage listForPayment(String transactionUuid) {
        return listForPayment(transactionUuid, RefundQuery.builder().build());
    }

    /**
     * Asynchronous form of {@link #listForPayment(String, RefundQuery)}.
     *
     * @param transactionUuid the identifier of the payment
     * @param query the paging and filters
     * @return a future with one page of refunds
     */
    public CompletableFuture<RefundPage> listForPaymentAsync(
            String transactionUuid, RefundQuery query) {
        return executor.executeAsync(
                listForPaymentOp(transactionUuid, query), b -> JsonCodec.read(b, RefundPage.class));
    }

    private static Operation listForPaymentOp(String transactionUuid, RefundQuery query) {
        Objects.requireNonNull(query, "query");
        Validation.id(transactionUuid, "transactionUuid");
        return Operation.get(
                PAYMENTS + "/" + UrlEncoding.encode(transactionUuid) + "/refunds",
                query.pagingParameters());
    }
}
