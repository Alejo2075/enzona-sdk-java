package io.github.alejo2075.enzona.model;

import java.math.BigDecimal;

/**
 * Total and breakdown of a payment.
 *
 * @param total the amount the buyer pays, with two decimals
 * @param details the breakdown into shipping, tax, discount and tip
 */
public record PaymentAmount(BigDecimal total, AmountDetails details) {}
