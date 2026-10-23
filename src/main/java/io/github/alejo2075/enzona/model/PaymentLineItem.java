package io.github.alejo2075.enzona.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;

/**
 * A line of a payment as ENZONA returns it.
 *
 * <p>ENZONA sends {@code name} as a string in payments and as an array of strings in refunds, so it
 * is exposed as a list; {@link #name()} gives the first one.
 *
 * @param names the name or names of the item
 * @param description the item description
 * @param quantity the quantity
 * @param price the unit price
 * @param tax the tax of the line
 */
public record PaymentLineItem(
        @JsonProperty("name") List<String> names,
        String description,
        Integer quantity,
        BigDecimal price,
        BigDecimal tax) {

    /**
     * Returns the names of the item.
     *
     * @return the names, never {@code null}
     */
    public List<String> names() {
        return names == null ? List.of() : names;
    }

    /**
     * Returns the first name of the item.
     *
     * @return the name, or {@code null} if there is none
     */
    public String name() {
        return names().isEmpty() ? null : names().get(0);
    }
}
