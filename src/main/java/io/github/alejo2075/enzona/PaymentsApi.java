package io.github.alejo2075.enzona;

import io.github.alejo2075.enzona.internal.JsonCodec;
import io.github.alejo2075.enzona.internal.Operation;
import io.github.alejo2075.enzona.internal.RequestExecutor;
import io.github.alejo2075.enzona.internal.UrlEncoding;
import io.github.alejo2075.enzona.internal.Validation;
import io.github.alejo2075.enzona.model.CreatePaymentRequest;
import io.github.alejo2075.enzona.model.Payment;
import io.github.alejo2075.enzona.model.PaymentPage;
import io.github.alejo2075.enzona.model.PaymentQuery;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Payments of the ENZONA Payment API v1.0.0. Obtain it from {@link EnzonaClient#payments()}.
 *
 * <p>Operations that create or change a payment ({@code create}, {@code complete}, {@code cancel})
 * are <strong>never retried</strong> automatically, even if a retry policy is configured, because
 * ENZONA documents no idempotency guarantee for them.
 */
public final class PaymentsApi {

    private static final String PAYMENTS = Operation.PAYMENT_API + "/payments";

    private final RequestExecutor executor;

    PaymentsApi(RequestExecutor executor) {
        this.executor = executor;
    }

    /**
     * Creates a payment in the {@code pending} state.
     *
     * <p>Send the buyer to {@link Payment#confirmationUrl()} and keep {@link
     * Payment#transactionUuid()} next to your order. The buyer coming back to your {@code
     * returnUrl} proves nothing by itself: fetch the payment, check it and then {@code complete}
     * it.
     *
     * <p>If this call fails with a transport error the outcome is unknown: the payment may exist.
     * Look it up by your {@code merchantOpId} before creating another.
     *
     * @param request the payment to create
     * @return the created payment
     * @throws io.github.alejo2075.enzona.exception.ValidationException if ENZONA rejects the data
     * @throws io.github.alejo2075.enzona.exception.EnzonaException on any other failure
     */
    public Payment create(CreatePaymentRequest request) {
        return executor.execute(createOp(request), b -> JsonCodec.read(b, Payment.class));
    }

    /**
     * Asynchronous form of {@link #create(CreatePaymentRequest)}.
     *
     * @param request the payment to create
     * @return a future with the created payment
     */
    public CompletableFuture<Payment> createAsync(CreatePaymentRequest request) {
        return executor.executeAsync(createOp(request), b -> JsonCodec.read(b, Payment.class));
    }

    private static Operation createOp(CreatePaymentRequest request) {
        Objects.requireNonNull(request, "request");
        return Operation.post(PAYMENTS, request);
    }

    /**
     * Fetches a payment. This is a read-only call, so it is the only payment operation that the
     * client's retry policy (if enabled) may repeat.
     *
     * <p>Use it after the buyer returns to your {@code returnUrl}: compare {@link
     * Payment#amount()}, {@link Payment#currency()} and {@link Payment#merchantOpId()} with your
     * order before you {@code complete} the payment.
     *
     * @param transactionUuid the identifier returned when the payment was created
     * @return the payment
     * @throws io.github.alejo2075.enzona.exception.NotFoundException if the payment does not exist
     * @throws IllegalArgumentException if the identifier is blank
     */
    public Payment get(String transactionUuid) {
        return executor.execute(getOp(transactionUuid), b -> JsonCodec.read(b, Payment.class));
    }

    /**
     * Asynchronous form of {@link #get(String)}.
     *
     * @param transactionUuid the identifier returned when the payment was created
     * @return a future with the payment
     */
    public CompletableFuture<Payment> getAsync(String transactionUuid) {
        return executor.executeAsync(getOp(transactionUuid), b -> JsonCodec.read(b, Payment.class));
    }

    private static Operation getOp(String transactionUuid) {
        return Operation.get(paymentPath(transactionUuid, ""), null);
    }

    private static String paymentPath(String transactionUuid, String suffix) {
        Validation.id(transactionUuid, "transactionUuid");
        return PAYMENTS + "/" + UrlEncoding.encode(transactionUuid) + suffix;
    }

    /**
     * Lists payments, newest or oldest first depending on the query. Read-only, so the client's
     * retry policy (if enabled) may repeat it.
     *
     * @param query the filters and paging, see {@link PaymentQuery}
     * @return one page of payments
     */
    public PaymentPage list(PaymentQuery query) {
        return executor.execute(listOp(query), b -> JsonCodec.read(b, PaymentPage.class));
    }

    /**
     * Lists payments without filters.
     *
     * @return one page of payments, as ENZONA's defaults define it
     */
    public PaymentPage list() {
        return list(PaymentQuery.builder().build());
    }

    /**
     * Asynchronous form of {@link #list(PaymentQuery)}.
     *
     * @param query the filters and paging
     * @return a future with one page of payments
     */
    public CompletableFuture<PaymentPage> listAsync(PaymentQuery query) {
        return executor.executeAsync(listOp(query), b -> JsonCodec.read(b, PaymentPage.class));
    }

    private static Operation listOp(PaymentQuery query) {
        Objects.requireNonNull(query, "query");
        return Operation.get(PAYMENTS, query.toQueryParameters());
    }

    /**
     * Completes a payment the buyer has confirmed, which is what actually charges the buyer.
     *
     * <p>Call it only after you fetched the payment with {@link #get(String)} and checked its
     * status, amount, currency and {@code merchantOpId} against your own order. A payment that is
     * never completed is not charged.
     *
     * <p>This call is <strong>never retried</strong>. If it fails with a transport error the
     * outcome is unknown: fetch the payment and look at its status before trying again.
     *
     * @param transactionUuid the identifier returned when the payment was created
     * @return the completed payment
     * @throws io.github.alejo2075.enzona.exception.EnzonaApiException if ENZONA rejects it, for
     *     example because the buyer did not confirm yet
     */
    public Payment complete(String transactionUuid) {
        return executor.execute(completeOp(transactionUuid), b -> JsonCodec.read(b, Payment.class));
    }

    /**
     * Asynchronous form of {@link #complete(String)}.
     *
     * @param transactionUuid the identifier returned when the payment was created
     * @return a future with the completed payment
     */
    public CompletableFuture<Payment> completeAsync(String transactionUuid) {
        return executor.executeAsync(
                completeOp(transactionUuid), b -> JsonCodec.read(b, Payment.class));
    }

    private static Operation completeOp(String transactionUuid) {
        // An empty JSON object keeps gateways that insist on a JSON body satisfied (TODO: VERIFY
        // WITH ENZONA whether a body is needed at all).
        return Operation.post(paymentPath(transactionUuid, "/complete"), Map.of());
    }

    /**
     * Cancels a payment that has not been completed.
     *
     * <p>This call is <strong>never retried</strong>. If it fails with a transport error the
     * outcome is unknown: fetch the payment and look at its status before trying again.
     *
     * @param transactionUuid the identifier returned when the payment was created
     * @return the cancelled payment
     * @throws io.github.alejo2075.enzona.exception.EnzonaApiException if ENZONA rejects it, for
     *     example because the payment was already completed
     */
    public Payment cancel(String transactionUuid) {
        return executor.execute(cancelOp(transactionUuid), b -> JsonCodec.read(b, Payment.class));
    }

    /**
     * Asynchronous form of {@link #cancel(String)}.
     *
     * @param transactionUuid the identifier returned when the payment was created
     * @return a future with the cancelled payment
     */
    public CompletableFuture<Payment> cancelAsync(String transactionUuid) {
        return executor.executeAsync(
                cancelOp(transactionUuid), b -> JsonCodec.read(b, Payment.class));
    }

    private static Operation cancelOp(String transactionUuid) {
        return Operation.post(paymentPath(transactionUuid, "/cancel"), Map.of());
    }
}
