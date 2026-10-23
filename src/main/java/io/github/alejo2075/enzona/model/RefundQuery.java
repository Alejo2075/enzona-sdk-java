package io.github.alejo2075.enzona.model;

import io.github.alejo2075.enzona.internal.Validation;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Filters and paging of the refund lists. Every filter is optional.
 *
 * <p>{@code merchantUuid}, {@code transactionUuid} and {@code commerceRefundId} only apply to
 * {@link io.github.alejo2075.enzona.RefundsApi#list(RefundQuery)}; the list of one payment's
 * refunds ignores them because the payment is part of the path. Dates are sent as {@code
 * yyyy-MM-dd} (the exact format is unconfirmed).
 */
public final class RefundQuery {

    private final String merchantUuid;
    private final String transactionUuid;
    private final String commerceRefundId;
    private final Integer limit;
    private final Integer offset;
    private final PaymentStatus status;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final SortOrder order;

    private RefundQuery(Builder b) {
        if (b.limit != null && b.limit < 1) {
            throw new IllegalArgumentException("limit must be at least 1");
        }
        if (b.offset != null && b.offset < 0) {
            throw new IllegalArgumentException("offset must not be negative");
        }
        if (b.status == PaymentStatus.UNKNOWN) {
            throw new IllegalArgumentException("status UNKNOWN cannot be used as a filter");
        }
        if (b.startDate != null && b.endDate != null && b.endDate.isBefore(b.startDate)) {
            throw new IllegalArgumentException("endDate must not be before startDate");
        }
        this.merchantUuid = Validation.optionalText(b.merchantUuid, "merchantUuid", 128);
        this.transactionUuid = Validation.optionalText(b.transactionUuid, "transactionUuid", 128);
        this.commerceRefundId =
                Validation.optionalText(b.commerceRefundId, "commerceRefundId", 128);
        this.limit = b.limit;
        this.offset = b.offset;
        this.status = b.status;
        this.startDate = b.startDate;
        this.endDate = b.endDate;
        this.order = b.order;
    }

    /**
     * Starts building a query.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the query parameters for {@code GET /payments/refunds}.
     *
     * @return an ordered map of raw (not yet percent-encoded) parameters
     */
    public Map<String, String> toQueryParameters() {
        Map<String, String> q = new LinkedHashMap<>();
        put(q, "merchant_uuid", merchantUuid);
        put(q, "transaction_uuid", transactionUuid);
        put(q, "commerce_refund_id", commerceRefundId);
        q.putAll(pagingParameters());
        return q;
    }

    /**
     * Returns only the paging, status, date and order parameters, which are the ones accepted by
     * {@code GET /payments/{uuid}/refunds}.
     *
     * @return an ordered map of raw parameters
     */
    public Map<String, String> pagingParameters() {
        Map<String, String> q = new LinkedHashMap<>();
        put(q, "limit", limit == null ? null : limit.toString());
        put(q, "offset", offset == null ? null : offset.toString());
        put(q, "status_filter", status == null ? null : Integer.toString(status.code()));
        put(q, "start_date_filter", startDate == null ? null : startDate.toString());
        put(q, "end_date_filter", endDate == null ? null : endDate.toString());
        put(q, "order_filter", order == null ? null : order.wireValue());
        return q;
    }

    private static void put(Map<String, String> q, String name, String value) {
        if (value != null) {
            q.put(name, value);
        }
    }

    /** Builder of {@link RefundQuery}. */
    public static final class Builder {
        private String merchantUuid;
        private String transactionUuid;
        private String commerceRefundId;
        private Integer limit;
        private Integer offset;
        private PaymentStatus status;
        private LocalDate startDate;
        private LocalDate endDate;
        private SortOrder order;

        private Builder() {}

        /**
         * Restricts the list to one merchant.
         *
         * @param merchantUuid the merchant identifier
         * @return this builder
         */
        public Builder merchantUuid(String merchantUuid) {
            this.merchantUuid = merchantUuid;
            return this;
        }

        /**
         * Restricts the list to the refunds of one payment (only for the general list).
         *
         * @param transactionUuid the payment identifier
         * @return this builder
         */
        public Builder transactionUuid(String transactionUuid) {
            this.transactionUuid = transactionUuid;
            return this;
        }

        /**
         * Finds refunds by your own refund identifier (only for the general list).
         *
         * @param commerceRefundId the identifier you sent when refunding
         * @return this builder
         */
        public Builder commerceRefundId(String commerceRefundId) {
            this.commerceRefundId = commerceRefundId;
            return this;
        }

        /**
         * Sets the page size.
         *
         * @param limit at least 1
         * @return this builder
         */
        public Builder limit(int limit) {
            this.limit = limit;
            return this;
        }

        /**
         * Sets the page offset.
         *
         * @param offset not negative
         * @return this builder
         */
        public Builder offset(int offset) {
            this.offset = offset;
            return this;
        }

        /**
         * Filters by status.
         *
         * @param status a known status, not {@link PaymentStatus#UNKNOWN}
         * @return this builder
         */
        public Builder status(PaymentStatus status) {
            this.status = status;
            return this;
        }

        /**
         * Keeps refunds from this date on.
         *
         * @param startDate the first day
         * @return this builder
         */
        public Builder startDate(LocalDate startDate) {
            this.startDate = startDate;
            return this;
        }

        /**
         * Keeps refunds up to this date.
         *
         * @param endDate the last day, not before the start date
         * @return this builder
         */
        public Builder endDate(LocalDate endDate) {
            this.endDate = endDate;
            return this;
        }

        /**
         * Sets the sort order.
         *
         * @param order ascending or descending
         * @return this builder
         */
        public Builder order(SortOrder order) {
            this.order = order;
            return this;
        }

        /**
         * Builds the query.
         *
         * @return the query
         * @throws IllegalArgumentException if a value is out of range
         */
        public RefundQuery build() {
            return new RefundQuery(this);
        }
    }
}
