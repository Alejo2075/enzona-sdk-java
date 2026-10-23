package io.github.alejo2075.enzona.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.alejo2075.enzona.internal.Money;
import io.github.alejo2075.enzona.internal.Validation;
import java.math.BigDecimal;
import java.net.URI;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * The data of a payment to create ({@code POST /payments}).
 *
 * <p>Always create it with {@link #builder()}. The builder validates the data and computes the
 * amount the way ENZONA checks it on the server: the item taxes must add up to the tax of the
 * breakdown and the breakdown must add up to the total, otherwise ENZONA answers with error 4083 or
 * 4084.
 *
 * <pre>{@code
 * total = Σ(price × quantity) + tax + shipping + tip − discount      tax = Σ(item tax)
 * }</pre>
 *
 * <p>Amounts are never rounded: a value with more than two decimals is rejected.
 *
 * @param merchantUuid identifier of the merchant, optional (see {@code docs/api-coverage.md})
 * @param description description shown to the buyer
 * @param currency the currency code, for example {@code CUP}
 * @param amount the total and its breakdown
 * @param items the lines of the payment, may be empty
 * @param merchantOpId your own identifier of the operation
 * @param invoiceNumber your invoice number
 * @param returnUrl where the buyer is sent after confirming the payment
 * @param cancelUrl where the buyer is sent if they cancel
 * @param terminalId the terminal that originates the payment
 * @param buyerIdentityCode identity code of the buyer, when the payment is assigned to one
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CreatePaymentRequest(
        String merchantUuid,
        String description,
        String currency,
        PaymentAmount amount,
        @JsonInclude(JsonInclude.Include.NON_EMPTY) List<PaymentItem> items,
        String merchantOpId,
        String invoiceNumber,
        URI returnUrl,
        URI cancelUrl,
        String terminalId,
        String buyerIdentityCode) {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final long TWELVE_DIGITS = 1_000_000_000_000L;

    /**
     * Starts building a request.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Generates a random 12-digit operation identifier. ENZONA documents the merchant operation id
     * as a 12-digit number in its SDKs; uniqueness is probabilistic, so keep the one you use next
     * to your order.
     *
     * @return twelve decimal digits
     */
    public static String newMerchantOpId() {
        return String.format("%012d", RANDOM.nextLong(TWELVE_DIGITS));
    }

    /** Builder of {@link CreatePaymentRequest}. */
    public static final class Builder {
        private String merchantUuid;
        private String description;
        private String currency;
        private final List<PaymentItem> items = new ArrayList<>();
        private BigDecimal shipping = BigDecimal.ZERO;
        private BigDecimal tip = BigDecimal.ZERO;
        private BigDecimal discount = BigDecimal.ZERO;
        private BigDecimal total;
        private String merchantOpId;
        private String invoiceNumber;
        private URI returnUrl;
        private URI cancelUrl;
        private String terminalId;
        private String buyerIdentityCode;

        private Builder() {}

        /**
         * Sets the merchant identifier.
         *
         * @param merchantUuid the merchant UUID
         * @return this builder
         */
        public Builder merchantUuid(String merchantUuid) {
            this.merchantUuid = merchantUuid;
            return this;
        }

        /**
         * Sets the description shown to the buyer.
         *
         * @param description the description
         * @return this builder
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Sets the currency.
         *
         * @param currency a three-letter code such as {@code CUP}
         * @return this builder
         */
        public Builder currency(String currency) {
            this.currency = currency;
            return this;
        }

        /**
         * Adds a line to the payment.
         *
         * @param item the item
         * @return this builder
         */
        public Builder item(PaymentItem item) {
            this.items.add(Objects.requireNonNull(item, "item"));
            return this;
        }

        /**
         * Adds several lines to the payment.
         *
         * @param items the items
         * @return this builder
         */
        public Builder items(Collection<PaymentItem> items) {
            Objects.requireNonNull(items, "items").forEach(this::item);
            return this;
        }

        /**
         * Sets the shipping cost (default zero).
         *
         * @param shipping a non-negative amount with at most two decimals
         * @return this builder
         */
        public Builder shipping(BigDecimal shipping) {
            this.shipping = shipping;
            return this;
        }

        /**
         * Sets the tip (default zero).
         *
         * @param tip a non-negative amount with at most two decimals
         * @return this builder
         */
        public Builder tip(BigDecimal tip) {
            this.tip = tip;
            return this;
        }

        /**
         * Sets the discount subtracted from the total (default zero).
         *
         * @param discount a non-negative amount with at most two decimals
         * @return this builder
         */
        public Builder discount(BigDecimal discount) {
            this.discount = discount;
            return this;
        }

        /**
         * Sets the total explicitly. It is required when there are no items. When there are items,
         * the total is computed and, if you also set it, it must be equal to the computed one.
         *
         * @param total the total with at most two decimals
         * @return this builder
         */
        public Builder total(BigDecimal total) {
            this.total = total;
            return this;
        }

        /**
         * Sets your identifier of the operation.
         *
         * @param merchantOpId the identifier
         * @return this builder
         */
        public Builder merchantOpId(String merchantOpId) {
            this.merchantOpId = merchantOpId;
            return this;
        }

        /**
         * Uses a freshly generated 12-digit operation identifier, see {@link
         * CreatePaymentRequest#newMerchantOpId()}.
         *
         * @return this builder
         */
        public Builder generateMerchantOpId() {
            this.merchantOpId = CreatePaymentRequest.newMerchantOpId();
            return this;
        }

        /**
         * Sets your invoice number.
         *
         * @param invoiceNumber the invoice number
         * @return this builder
         */
        public Builder invoiceNumber(String invoiceNumber) {
            this.invoiceNumber = invoiceNumber;
            return this;
        }

        /**
         * Sets where the buyer returns after confirming.
         *
         * @param returnUrl an absolute http(s) URL
         * @return this builder
         */
        public Builder returnUrl(URI returnUrl) {
            this.returnUrl = returnUrl;
            return this;
        }

        /**
         * Sets where the buyer returns after confirming.
         *
         * @param returnUrl an absolute http(s) URL
         * @return this builder
         */
        public Builder returnUrl(String returnUrl) {
            this.returnUrl = Validation.httpUri(returnUrl, "returnUrl");
            return this;
        }

        /**
         * Sets where the buyer returns after cancelling.
         *
         * @param cancelUrl an absolute http(s) URL
         * @return this builder
         */
        public Builder cancelUrl(URI cancelUrl) {
            this.cancelUrl = cancelUrl;
            return this;
        }

        /**
         * Sets where the buyer returns after cancelling.
         *
         * @param cancelUrl an absolute http(s) URL
         * @return this builder
         */
        public Builder cancelUrl(String cancelUrl) {
            this.cancelUrl = Validation.httpUri(cancelUrl, "cancelUrl");
            return this;
        }

        /**
         * Sets the terminal identifier.
         *
         * @param terminalId the terminal
         * @return this builder
         */
        public Builder terminalId(String terminalId) {
            this.terminalId = terminalId;
            return this;
        }

        /**
         * Assigns the payment to a buyer by identity code.
         *
         * @param buyerIdentityCode the identity code
         * @return this builder
         */
        public Builder buyerIdentityCode(String buyerIdentityCode) {
            this.buyerIdentityCode = buyerIdentityCode;
            return this;
        }

        /**
         * Validates the data and builds the request.
         *
         * @return the request
         * @throws IllegalArgumentException if a required value is missing, a value is invalid, an
         *     amount has more than two decimals, or an explicit total differs from the computed one
         */
        public CreatePaymentRequest build() {
            String cur = Validation.currency(currency);
            String opId = Validation.text(merchantOpId, "merchantOpId", 64);
            URI ret = Validation.httpUri(returnUrl, "returnUrl");
            URI cancel = Validation.httpUri(cancelUrl, "cancelUrl");
            BigDecimal ship = Validation.money(shipping, "shipping");
            BigDecimal tp = Validation.money(tip, "tip");
            BigDecimal disc = Validation.money(discount, "discount");

            BigDecimal tax = BigDecimal.ZERO;
            BigDecimal subtotal = BigDecimal.ZERO;
            for (PaymentItem item : items) {
                tax = tax.add(item.tax());
                subtotal = subtotal.add(item.price().multiply(BigDecimal.valueOf(item.quantity())));
            }
            BigDecimal computed = subtotal.add(tax).add(ship).add(tp).subtract(disc);

            BigDecimal finalTotal;
            if (items.isEmpty()) {
                if (total == null) {
                    throw new IllegalArgumentException(
                            "total is required when the payment has no items");
                }
                finalTotal = Validation.money(total, "total");
            } else {
                finalTotal = Money.normalize(computed, "total");
                if (total != null && Money.normalize(total, "total").compareTo(finalTotal) != 0) {
                    throw new IllegalArgumentException(
                            "total "
                                    + total.toPlainString()
                                    + " does not match the amount computed from the items: "
                                    + finalTotal.toPlainString());
                }
            }
            if (finalTotal.signum() <= 0) {
                throw new IllegalArgumentException("total must be greater than 0");
            }
            PaymentAmount amount =
                    new PaymentAmount(
                            finalTotal,
                            new AmountDetails(
                                    ship, Money.normalize(tax, "tax"), disc, tp, null, null));
            return new CreatePaymentRequest(
                    Validation.optionalText(merchantUuid, "merchantUuid", 128),
                    Validation.optionalText(description, "description", 1000),
                    cur,
                    amount,
                    List.copyOf(items),
                    opId,
                    Validation.optionalText(invoiceNumber, "invoiceNumber", 64),
                    ret,
                    cancel,
                    Validation.optionalText(terminalId, "terminalId", 64),
                    Validation.optionalText(buyerIdentityCode, "buyerIdentityCode", 128));
        }
    }
}
