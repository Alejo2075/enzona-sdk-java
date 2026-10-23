package io.github.alejo2075.enzona.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * A refund as ENZONA returns it when it is created, fetched or listed.
 *
 * <p>Like {@link Payment}, every field is optional on read and identifiers are strings because the
 * API is inconsistent about numbers and strings. In refunds {@code items[].name} arrives as an
 * array of strings (see {@link PaymentLineItem}).
 *
 * @param transactionUuid identifier of the refund transaction
 * @param transactionCode ENZONA's transaction code
 * @param transactionSignature signature of the transaction
 * @param transactionDenom description of the transaction type
 * @param transactionDescription description of the transaction
 * @param transactionCreatedAt creation date of the transaction, as sent
 * @param transactionUpdatedAt last update of the transaction, as sent
 * @param transactionStatusCode status code reported as {@code transaction_status_code}
 * @param statusCode the raw status code
 * @param statusDenoms the status description (a string, or a list in some responses)
 * @param parentPaymentUuid identifier of the refunded payment
 * @param commerceRefundId your identifier of the refund, when you sent one
 * @param currency the currency code
 * @param createdAt creation date, as sent
 * @param updatedAt last update, as sent
 * @param description the refund description
 * @param invoiceNumber the invoice number of the refunded payment
 * @param terminalId the terminal identifier
 * @param amount the refunded total and its breakdown, including the amounts refunded so far
 * @param items the refunded lines
 * @param links the actions available for the refund
 * @param username the buyer's user name
 * @param name the buyer's first name
 * @param lastname the buyer's last name
 * @param avatar the buyer's avatar URL
 * @param refundName first name of the person who received the refund
 * @param refundLastname last name of the person who received the refund
 * @param refundAvatar avatar URL of the person who received the refund
 */
public record Refund(
        String transactionUuid,
        String transactionCode,
        String transactionSignature,
        String transactionDenom,
        String transactionDescription,
        String transactionCreatedAt,
        String transactionUpdatedAt,
        Integer transactionStatusCode,
        Integer statusCode,
        @JsonProperty("status_denom") List<String> statusDenoms,
        String parentPaymentUuid,
        String commerceRefundId,
        String currency,
        String createdAt,
        @JsonAlias("update_at") String updatedAt,
        String description,
        String invoiceNumber,
        String terminalId,
        PaymentAmount amount,
        List<PaymentLineItem> items,
        List<PaymentLink> links,
        String username,
        String name,
        String lastname,
        String avatar,
        String refundName,
        String refundLastname,
        String refundAvatar) {

    /**
     * Returns the status descriptions.
     *
     * @return the descriptions, never {@code null}
     */
    public List<String> statusDenoms() {
        return statusDenoms == null ? List.of() : statusDenoms;
    }

    /**
     * Returns the refunded lines.
     *
     * @return the lines, never {@code null}
     */
    public List<PaymentLineItem> items() {
        return items == null ? List.of() : items;
    }

    /**
     * Returns the action links of the refund.
     *
     * @return the links, never {@code null}
     */
    public List<PaymentLink> links() {
        return links == null ? List.of() : links;
    }

    /**
     * Returns the status as an enum, using {@code status_code} and falling back to {@code
     * transaction_status_code}.
     *
     * @return the status; {@link PaymentStatus#UNKNOWN} if no code is present or it is not known
     */
    public PaymentStatus status() {
        return PaymentStatus.fromCode(statusCode != null ? statusCode : transactionStatusCode);
    }

    /**
     * Returns the first status description.
     *
     * @return the text, or {@code null} if there is none
     */
    public String statusDenom() {
        return statusDenoms().isEmpty() ? null : statusDenoms().get(0);
    }
}
