package io.github.alejo2075.enzona.internal;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Monetary amounts as ENZONA expects them: decimal strings with exactly two decimals ({@code
 * "10.00"}). The SDK never rounds: a value that needs more than two decimals is rejected.
 */
public final class Money {

    private static final int SCALE = 2;

    private Money() {}

    /**
     * Normalizes an amount to two decimals without rounding.
     *
     * @param value the amount
     * @param field the field name, used in the error message
     * @return the same amount with scale 2
     * @throws IllegalArgumentException if the amount has more than two significant decimals
     */
    public static BigDecimal normalize(BigDecimal value, String field) {
        BigDecimal stripped = value.stripTrailingZeros();
        if (stripped.scale() > SCALE) {
            throw new IllegalArgumentException(
                    field + " must have at most two decimals but was " + value.toPlainString());
        }
        return value.setScale(SCALE, RoundingMode.UNNECESSARY);
    }

    /**
     * Formats an amount for the wire.
     *
     * @param value the amount
     * @return the plain decimal text with exactly two decimals
     * @throws IllegalArgumentException if the amount has more than two significant decimals
     */
    public static String format(BigDecimal value) {
        return normalize(value, "amount").toPlainString();
    }
}
