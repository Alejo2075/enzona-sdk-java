package io.github.alejo2075.enzona.model;

import java.util.List;

/**
 * A page of payments returned by {@code GET /payments}.
 *
 * @param payments the payments of this page
 * @param pagination paging information, or {@code null} if ENZONA sent none
 */
public record PaymentPage(List<Payment> payments, PageInfo pagination) {

    /**
     * Returns the payments of this page.
     *
     * @return the payments, never {@code null}
     */
    @Override
    public List<Payment> payments() {
        return payments == null ? List.of() : payments;
    }
}
