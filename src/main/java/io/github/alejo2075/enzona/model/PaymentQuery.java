package io.github.alejo2075.enzona.model;

import io.github.alejo2075.enzona.internal.Validation;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Filters and paging of {@code GET /payments}. Every filter is optional.
 *
 * <p>Dates are sent as {@code yyyy-MM-dd}; ENZONA does not document the exact date format (confirm
 * it in the sandbox). {@code offset} is passed through as ENZONA defines it; the Dart SDK treats it
 * as a page index.
 */
public final class PaymentQuery {

    private final String merchantUuid;
    private final Integer limit;
    private final Integer offset;
    private final String merchantOpFilter;
    private final String enzonaOpFilter;
    private final PaymentStatus status;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final SortOrder order;

    private PaymentQuery(Builder b) {
        this.merchantUuid = Validation.optionalText(b.merchantUuid, "merchantUuid", 128);
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
        this.limit = b.limit;
        this.offset = b.offset;
        this.merchantOpFilter = Validation.optionalText(b.merchantOpFilter, "merchantOpFilter", 64);
        this.enzonaOpFilter = Validation.optionalText(b.enzonaOpFilter, "enzonaOpFilter", 64);
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
     * Returns the query parameters in the order they are sent.
     *
     * @return an ordered map of raw (not yet percent-encoded) parameters; empty if there are no
     *     filters
     */
    public Map<String, String> toQueryParameters() {
        Map<String, String> q = new LinkedHashMap<>();
        put(q, "merchant_uuid", merchantUuid);
        put(q, "limit", limit == null ? null : limit.toString());
        put(q, "offset", offset == null ? null : offset.toString());
        put(q, "merchant_op_filter", merchantOpFilter);
        put(q, "enzona_op_filter", enzonaOpFilter);
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

    /** Builder of {@link PaymentQuery}. */
    public static final class Builder {
        private String merchantUuid;
        private Integer limit;
        private Integer offset;
        private String merchantOpFilter;
        private String enzonaOpFilter;
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
         * Filters by the merchant operation identifier ({@code merchant_op_id}).
         *
         * @param merchantOpId the identifier you sent when creating the payment
         * @return this builder
         */
        public Builder merchantOpId(String merchantOpId) {
            this.merchantOpFilter = merchantOpId;
            return this;
        }

        /**
         * Filters by ENZONA's operation identifier.
         *
         * @param enzonaOpId the identifier
         * @return this builder
         */
        public Builder enzonaOpId(String enzonaOpId) {
            this.enzonaOpFilter = enzonaOpId;
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
         * Keeps payments from this date on.
         *
         * @param startDate the first day
         * @return this builder
         */
        public Builder startDate(LocalDate startDate) {
            this.startDate = startDate;
            return this;
        }

        /**
         * Keeps payments up to this date.
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
        public PaymentQuery build() {
            return new PaymentQuery(this);
        }
    }
}
