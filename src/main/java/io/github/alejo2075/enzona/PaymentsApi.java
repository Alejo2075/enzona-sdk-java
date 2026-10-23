package io.github.alejo2075.enzona;

import io.github.alejo2075.enzona.internal.JsonCodec;
import io.github.alejo2075.enzona.internal.Operation;
import io.github.alejo2075.enzona.internal.RequestExecutor;
import io.github.alejo2075.enzona.internal.UrlEncoding;
import io.github.alejo2075.enzona.internal.Validation;
import io.github.alejo2075.enzona.model.CreatePaymentRequest;
import io.github.alejo2075.enzona.model.Payment;
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
}
