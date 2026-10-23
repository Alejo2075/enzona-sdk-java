package io.github.alejo2075.enzona.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.alejo2075.enzona.internal.Validation;
import java.math.BigDecimal;

/**
 * A line of a payment you are about to create.
 *
 * <p>{@code tax} is the tax of the <em>whole line</em> (not per unit); ENZONA requires the sum of
 * the line taxes to equal the {@code tax} of the amount breakdown, which {@link
 * CreatePaymentRequest.Builder} takes care of.
 *
 * @param name the item name
 * @param description the item description, may be {@code null}
 * @param quantity the number of units, at least 1
 * @param price the unit price with at most two decimals
 * @param tax the tax of the whole line with at most two decimals
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PaymentItem(
        String name, String description, int quantity, BigDecimal price, BigDecimal tax) {

    /**
     * Creates a validated item without a description.
     *
     * @param name the item name
     * @param quantity the number of units, at least 1
     * @param price the unit price, not negative, at most two decimals
     * @param tax the tax of the whole line, not negative, at most two decimals
     * @return the item
     * @throws IllegalArgumentException if a value is invalid
     */
    public static PaymentItem of(String name, int quantity, BigDecimal price, BigDecimal tax) {
        return of(name, null, quantity, price, tax);
    }

    /**
     * Creates a validated item.
     *
     * @param name the item name
     * @param description the item description, may be {@code null}
     * @param quantity the number of units, at least 1
     * @param price the unit price, not negative, at most two decimals
     * @param tax the tax of the whole line, not negative, at most two decimals
     * @return the item
     * @throws IllegalArgumentException if a value is invalid
     */
    public static PaymentItem of(
            String name, String description, int quantity, BigDecimal price, BigDecimal tax) {
        if (quantity < 1) {
            throw new IllegalArgumentException("item quantity must be at least 1");
        }
        return new PaymentItem(
                Validation.text(name, "item name", 255),
                Validation.optionalText(description, "item description", 1000),
                quantity,
                Validation.money(price, "item price"),
                Validation.money(tax, "item tax"));
    }
}
