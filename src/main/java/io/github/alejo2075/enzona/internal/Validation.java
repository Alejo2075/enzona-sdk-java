package io.github.alejo2075.enzona.internal;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.regex.Pattern;

/** Argument validation shared by the request types. */
public final class Validation {

    private static final Pattern CURRENCY = Pattern.compile("[A-Z]{3}");

    private Validation() {}

    /**
     * Requires a non-blank string whose length is within bounds.
     *
     * @param value the value
     * @param name the field name used in the message
     * @param maxLength the maximum length
     * @return the value
     */
    public static String text(String value, String name, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        if (value.length() > maxLength) {
            throw new IllegalArgumentException(
                    name + " must be at most " + maxLength + " characters");
        }
        return value;
    }

    /**
     * Requires a non-blank string (no length limit).
     *
     * @param value the value
     * @param name the field name
     * @return the value
     */
    public static String text(String value, String name) {
        return text(value, name, Integer.MAX_VALUE);
    }

    /**
     * Requires a non-blank identifier that can safely be placed in a URL path after encoding.
     *
     * @param value the value
     * @param name the field name
     * @return the value
     */
    public static String id(String value, String name) {
        return text(value, name, 128);
    }

    /**
     * Normalizes an ISO 4217 style currency code to upper case and checks its shape.
     *
     * @param value the code, for example {@code cup} or {@code CUP}
     * @return the upper-case code
     */
    public static String currency(String value) {
        String code = text(value, "currency", 3).toUpperCase(Locale.ROOT);
        if (!CURRENCY.matcher(code).matches()) {
            throw new IllegalArgumentException("currency must be a three-letter code");
        }
        return code;
    }

    /**
     * Requires an absolute http(s) URI with a host.
     *
     * @param value the URI text
     * @param name the field name
     * @return the URI
     */
    public static URI httpUri(String value, String name) {
        text(value, name);
        try {
            return httpUri(new URI(value.trim()), name);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(name + " must be a valid http(s) URL");
        }
    }

    /**
     * Requires an absolute http(s) URI with a host.
     *
     * @param value the URI
     * @param name the field name
     * @return the URI
     */
    public static URI httpUri(URI value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " is required");
        }
        String scheme = value.getScheme() == null ? "" : value.getScheme().toLowerCase(Locale.ROOT);
        if ((scheme.equals("http") || scheme.equals("https")) && value.getHost() != null) {
            return value;
        }
        throw new IllegalArgumentException(name + " must be a valid http(s) URL");
    }

    /**
     * Requires a text that is a plain string; used by the optional fields that may be absent.
     *
     * @param value the value, may be {@code null}
     * @param name the field name
     * @param maxLength the maximum length
     * @return the value, or {@code null} if absent or blank
     */
    public static String optionalText(String value, String name, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return text(value, name, maxLength);
    }

    /**
     * Requires a non-negative amount with at most two decimals and returns it with scale two.
     *
     * @param value the amount
     * @param name the field name
     * @return the amount with scale two
     */
    public static BigDecimal money(BigDecimal value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " is required");
        }
        if (value.signum() < 0) {
            throw new IllegalArgumentException(name + " must not be negative");
        }
        return Money.normalize(value, name);
    }
}
