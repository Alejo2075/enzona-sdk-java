package io.github.alejo2075.enzona.model;

import java.util.List;

/**
 * A page of refunds returned by the refund list endpoints.
 *
 * @param refunds the refunds of this page
 * @param pagination paging information, or {@code null} if ENZONA sent none
 */
public record RefundPage(List<Refund> refunds, PageInfo pagination) {

    /**
     * Returns the refunds of this page.
     *
     * @return the refunds, never {@code null}
     */
    @Override
    public List<Refund> refunds() {
        return refunds == null ? List.of() : refunds;
    }
}
