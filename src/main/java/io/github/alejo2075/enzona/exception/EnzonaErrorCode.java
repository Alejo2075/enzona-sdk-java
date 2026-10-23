package io.github.alejo2075.enzona.exception;

/**
 * Catalog of the numeric error codes observed from ENZONA and from its WSO2 gateway.
 *
 * <p>The list is informative and not exhaustive: codes the SDK does not know map to {@link
 * #UNKNOWN}, and {@link EnzonaApiException#errorCode()} always keeps the raw value. The gateway
 * codes are WSO2 defaults and still need to be confirmed against the sandbox.
 */
public enum EnzonaErrorCode {

    /** The user has no access to the requested resource. */
    ACCESS_DENIED(4001),

    /** The card does not exist. */
    CARD_NOT_FOUND(4003),

    /** The transaction could not be performed. */
    TRANSACTION_NOT_PERFORMED(4004),

    /** The identity of the user is not recognised. */
    USER_NOT_FOUND(4008),

    /** The payment password is wrong. */
    WRONG_PAYMENT_PASSWORD(4009),

    /** The payment identification code does not exist. */
    PAYMENT_CODE_NOT_FOUND(4014),

    /** The currency is not configured in the system. */
    CURRENCY_NOT_CONFIGURED(4016),

    /** The amount is empty or zero. */
    AMOUNT_EMPTY_OR_ZERO(4023),

    /** Input parameters are empty or incorrect. */
    INVALID_INPUT_PARAMETERS(4046),

    /** The merchant does not exist. */
    MERCHANT_NOT_FOUND(4058),

    /** The transaction does not exist. */
    TRANSACTION_NOT_FOUND(4064),

    /** The payment to refund is not confirmed. */
    PAYMENT_NOT_CONFIRMED(4065),

    /** There is no payment for the transaction. */
    PAYMENT_NOT_FOUND(4066),

    /** The payment was already refunded in full. */
    PAYMENT_ALREADY_FULLY_REFUNDED(4067),

    /** The maximum refundable amount was exceeded. */
    REFUND_EXCEEDS_REFUNDABLE(4068),

    /** The refund amount is greater than the payment total. */
    REFUND_EXCEEDS_PAYMENT(4069),

    /** The refund amount cannot be zero. */
    REFUND_AMOUNT_ZERO(4070),

    /** The transaction is not a refund. */
    NOT_A_REFUND_TRANSACTION(4071),

    /** The transaction is not a payment to a merchant. */
    NOT_A_MERCHANT_PAYMENT(4072),

    /** There is no checkout for this UUID. */
    CHECKOUT_NOT_FOUND(4073),

    /** The QR code does not exist. */
    QR_NOT_FOUND(4077),

    /** The QR code was already used. */
    QR_ALREADY_USED(4078),

    /** The refund structure is malformed. */
    INVALID_REFUND_FORMAT(4081),

    /** The payment structure is malformed. */
    INVALID_PAYMENT_STRUCTURE(4082),

    /** The sum of item taxes does not match the tax in the amount details. */
    ITEM_TAX_MISMATCH(4083),

    /** The sum of the amount details does not match the total. */
    AMOUNT_DETAILS_MISMATCH(4084),

    /** The data structure is wrong. */
    INVALID_DATA_STRUCTURE(4092),

    /** The transaction cannot be performed right now. */
    SYSTEM_FAILURE(5001),

    /** The connection with the bank failed. */
    BANK_CONNECTION_FAILURE(5002),

    /** The bank answered incorrectly. */
    INVALID_BANK_RESPONSE(5003),

    /** Gateway: the credentials or the access token are invalid. */
    GATEWAY_INVALID_CREDENTIALS(900901),

    /** Gateway: the credentials are missing. */
    GATEWAY_MISSING_CREDENTIALS(900902),

    /** Gateway: the request was throttled. */
    GATEWAY_THROTTLED(900800),

    /** A code that is not in this catalog. */
    UNKNOWN(-1);

    private final int code;

    EnzonaErrorCode(int code) {
        this.code = code;
    }

    /**
     * Returns the numeric code sent by the server.
     *
     * @return the code, or -1 for {@link #UNKNOWN}
     */
    public int code() {
        return code;
    }

    /**
     * Looks a code up.
     *
     * @param code the numeric code received
     * @return the matching entry, or {@link #UNKNOWN}
     */
    public static EnzonaErrorCode of(int code) {
        for (EnzonaErrorCode c : values()) {
            if (c.code == code) {
                return c;
            }
        }
        return UNKNOWN;
    }
}
