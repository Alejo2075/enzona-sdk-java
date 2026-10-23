package io.github.alejo2075.enzona.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;

/**
 * Breakdown of a payment amount. Every value is a monetary amount with two decimals.
 *
 * <p>Requests always carry {@code shipping}, {@code tax}, {@code discount} and {@code tip}. The
 * refund fields are only present in refund responses.
 *
 * @param shipping shipping cost
 * @param tax total tax, equal to the sum of the item taxes
 * @param discount discount subtracted from the total
 * @param tip tip added to the total
 * @param refunded amount refunded so far, in refund responses
 * @param totalRefunded total amount refunded, in refund responses
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AmountDetails(
        BigDecimal shipping,
        BigDecimal tax,
        BigDecimal discount,
        BigDecimal tip,
        BigDecimal refunded,
        BigDecimal totalRefunded) {}
