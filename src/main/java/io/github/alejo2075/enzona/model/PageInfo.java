package io.github.alejo2075.enzona.model;

import java.math.BigDecimal;

/**
 * Paging information of a list response.
 *
 * <p>The exact shape is not documented publicly; these are the fields reported by the community
 * SDKs and every one of them may be {@code null}.
 *
 * @param first link to the first page
 * @param prev link to the previous page
 * @param next link to the next page
 * @param last link to the last page
 * @param total number of results
 * @param totalAmount sum of the amounts of all results
 */
public record PageInfo(
        String first, String prev, String next, String last, Long total, BigDecimal totalAmount) {}
