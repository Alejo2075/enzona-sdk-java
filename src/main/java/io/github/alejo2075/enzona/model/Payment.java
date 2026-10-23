package io.github.alejo2075.enzona.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.Optional;

/**
 * A payment as ENZONA returns it when it is created, fetched, completed, cancelled or listed.
 *
 * <p>Every field is optional on read: ENZONA does not always send all of them and a missing one is
 * {@code null}. Identifiers and codes are strings because the API sends the same field sometimes as
 * a number and sometimes as a string. Dates are kept as the text ENZONA sends (the format is not
 * documented).
 *
 * <p>Use {@link #confirmationUrl()} to send the buyer to ENZONA and {@link #status()} to read the
 * state.
 *
 * @param transactionUuid identifier of the payment, used by every other operation
 * @param transactionCode ENZONA's transaction code
 * @param transactionSignature signature of the transaction
 * @param transactionDenom description of the transaction type
 * @param transactionDescription description of the transaction
 * @param transactionCreatedAt creation date of the transaction, as sent
 * @param transactionUpdatedAt last update of the transaction, as sent
 * @param currency the currency code, for example {@code CUP}
 * @param createdAt creation date, as sent
 * @param updatedAt last update, as sent (ENZONA spells the field both {@code updated_at} and {@code
 *     update_at})
 * @param statusCode the raw status code, see {@link PaymentStatus}
 * @param statusDenoms the status description (a string, or a list in some responses)
 * @param description the payment description
 * @param invoiceNumber the invoice number sent when the payment was created
 * @param merchantOpId the merchant's operation identifier sent when the payment was created
 * @param terminalId the terminal identifier
 * @param amount the total and its breakdown
 * @param items the lines of the payment
 * @param links the actions available for the payment
 * @param commission the commission charged to the merchant
 * @param buyerIdentityCode the buyer's identity code, when the payment was assigned to one
 * @param merchantUuid the merchant identifier
 * @param merchantName the merchant name
 * @param merchantAlias the merchant alias
 * @param merchantAvatar the merchant avatar URL
 * @param refundedAmount the amount refunded so far
 * @param username the buyer's user name
 * @param name the buyer's first name
 * @param lastname the buyer's last name
 * @param avatar the buyer's avatar URL
 * @param returnUrl the URL the buyer returns to after paying
 * @param cancelUrl the URL the buyer returns to after cancelling
 */
public record Payment(
        String transactionUuid,
        String transactionCode,
        String transactionSignature,
        String transactionDenom,
        String transactionDescription,
        String transactionCreatedAt,
        String transactionUpdatedAt,
        String currency,
        String createdAt,
        @JsonAlias("update_at") String updatedAt,
        Integer statusCode,
        @JsonProperty("status_denom") List<String> statusDenoms,
        String description,
        String invoiceNumber,
        String merchantOpId,
        String terminalId,
        PaymentAmount amount,
        List<PaymentLineItem> items,
        List<PaymentLink> links,
        BigDecimal commission,
        String buyerIdentityCode,
        String merchantUuid,
        String merchantName,
        String merchantAlias,
        String merchantAvatar,
        BigDecimal refundedAmount,
        String username,
        String name,
        String lastname,
        String avatar,
        String returnUrl,
        String cancelUrl) {

    /**
     * Returns the status descriptions.
     *
     * @return the descriptions, never {@code null}
     */
    public List<String> statusDenoms() {
        return statusDenoms == null ? List.of() : statusDenoms;
    }

    /**
     * Returns the lines of the payment.
     *
     * @return the lines, never {@code null}
     */
    public List<PaymentLineItem> items() {
        return items == null ? List.of() : items;
    }

    /**
     * Returns the action links of the payment.
     *
     * @return the links, never {@code null}
     */
    public List<PaymentLink> links() {
        return links == null ? List.of() : links;
    }

    /**
     * Returns the status as an enum.
     *
     * @return the status; {@link PaymentStatus#UNKNOWN} if the code is missing or not known
     */
    public PaymentStatus status() {
        return PaymentStatus.fromCode(statusCode);
    }

    /**
     * Returns the first status description.
     *
     * @return the text, or {@code null} if there is none
     */
    public String statusDenom() {
        return statusDenoms().isEmpty() ? null : statusDenoms().get(0);
    }

    /**
     * Finds an action link by its relation.
     *
     * @param rel the relation, such as {@code confirm}; compared ignoring case
     * @return the link, if the payment has one
     */
    public Optional<PaymentLink> link(String rel) {
        return links().stream()
                .filter(l -> l.rel() != null && l.rel().equalsIgnoreCase(rel))
                .findFirst();
    }

    /**
     * Returns the URL the buyer must visit to confirm the payment, taken from the {@code confirm}
     * link.
     *
     * @return the URL, or empty if the payment has no usable {@code confirm} link
     */
    public Optional<URI> confirmationUrl() {
        return link("confirm")
                .map(PaymentLink::href)
                .filter(h -> h != null && !h.isBlank())
                .flatMap(
                        h -> {
                            try {
                                return Optional.of(URI.create(h));
                            } catch (IllegalArgumentException e) {
                                return Optional.empty();
                            }
                        });
    }
}
