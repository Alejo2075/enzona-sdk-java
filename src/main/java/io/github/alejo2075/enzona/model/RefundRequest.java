package io.github.alejo2075.enzona.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.alejo2075.enzona.internal.Validation;
import java.math.BigDecimal;

/**
 * The body of a refund ({@code POST /payments/{uuid}/refund}).
 *
 * <p>Without an amount the whole payment is refunded; with an amount only that part is. Amounts are
 * never rounded: more than two decimals are rejected.
 *
 * @param amount the amount to refund, or {@code null} for a full refund
 * @param description the reason shown to the buyer, optional
 * @param commerceRefundId your own identifier of the refund, optional (it can be used to find the
 *     refund later; ENZONA documents no idempotency for it)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RefundRequest(
        RefundAmount amount,
        String description,
        @JsonProperty("commerce_refund_id") String commerceRefundId) {

    /**
     * The amount of a partial refund.
     *
     * @param total the amount to refund, with two decimals
     */
    public record RefundAmount(BigDecimal total) {}

    /**
     * A request that refunds the whole payment.
     *
     * @return the request
     */
    public static RefundRequest full() {
        return new RefundRequest(null, null, null);
    }

    /**
     * Starts building a request.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /** Builder of {@link RefundRequest}. */
    public static final class Builder {
        private BigDecimal amount;
        private String description;
        private String commerceRefundId;

        private Builder() {}

        /**
         * Refunds only part of the payment. Leave it unset for a full refund.
         *
         * @param amount greater than zero, at most two decimals
         * @return this builder
         */
        public Builder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        /**
         * Sets the reason of the refund.
         *
         * @param description the description
         * @return this builder
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Sets your own identifier of the refund.
         *
         * @param commerceRefundId the identifier
         * @return this builder
         */
        public Builder commerceRefundId(String commerceRefundId) {
            this.commerceRefundId = commerceRefundId;
            return this;
        }

        /**
         * Validates the data and builds the request.
         *
         * @return the request
         * @throws IllegalArgumentException if the amount is zero, negative or has more than two
         *     decimals
         */
        public RefundRequest build() {
            RefundAmount refundAmount = null;
            if (amount != null) {
                BigDecimal total = Validation.money(amount, "amount");
                if (total.signum() == 0) {
                    throw new IllegalArgumentException("amount must be greater than 0");
                }
                refundAmount = new RefundAmount(total);
            }
            return new RefundRequest(
                    refundAmount,
                    Validation.optionalText(description, "description", 1000),
                    Validation.optionalText(commerceRefundId, "commerceRefundId", 128));
        }
    }
}
