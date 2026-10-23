package io.github.alejo2075.enzona.model;

/**
 * Status of a payment or refund, from the numeric {@code status_code} ENZONA returns.
 *
 * <p>A code this SDK version does not know maps to {@link #UNKNOWN} instead of failing, so new
 * codes never break existing integrations. Treat {@code UNKNOWN} as "not paid". The raw number is
 * always available from {@link Payment#statusCode()}.
 */
public enum PaymentStatus {
    /** 1111: the transaction was accepted (a payment that has been completed). */
    ACCEPTED(1111),
    /** 1112: the transaction failed. */
    FAILED(1112),
    /** 1113: waiting for the buyer to confirm, or for the merchant to complete. */
    PENDING(1113),
    /** 1114: the transaction was reversed. */
    REVERSED(1114),
    /** 1115: the payment was refunded. */
    REFUNDED(1115),
    /** 1116: the buyer confirmed the payment; the merchant still has to complete it. */
    CONFIRMED(1116),
    /** 1117: the payment was cancelled. */
    CANCELLED(1117),
    /** 1118: activated. */
    ACTIVATED(1118),
    /** 1119: deactivated. */
    DEACTIVATED(1119),
    /** A code this SDK version does not know. */
    UNKNOWN(-1);

    private final int code;

    PaymentStatus(int code) {
        this.code = code;
    }

    /**
     * Returns the numeric code used on the wire.
     *
     * @return the code, or -1 for {@link #UNKNOWN}
     */
    public int code() {
        return code;
    }

    /**
     * Looks a status up by its numeric code.
     *
     * @param code the {@code status_code}, may be {@code null}
     * @return the matching status, or {@link #UNKNOWN}
     */
    public static PaymentStatus fromCode(Integer code) {
        if (code != null) {
            for (PaymentStatus s : values()) {
                if (s.code == code) {
                    return s;
                }
            }
        }
        return UNKNOWN;
    }
}
