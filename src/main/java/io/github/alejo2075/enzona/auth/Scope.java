package io.github.alejo2075.enzona.auth;

import java.util.Objects;

/**
 * An OAuth2 scope requested for the access token. It decides which ENZONA APIs the token can use.
 */
public final class Scope {

    /** Scope of the Payment API ({@code enzona_business_payment}). */
    public static final Scope PAYMENT = new Scope("enzona_business_payment");

    /** Scope of the QR API ({@code enzona_business_qr}). */
    public static final Scope QR = new Scope("enzona_business_qr");

    private final String value;

    private Scope(String value) {
        this.value = value;
    }

    /**
     * Creates a scope with a custom name.
     *
     * @param value the scope name, without spaces
     * @return the scope
     * @throws IllegalArgumentException if the value is blank or contains whitespace
     */
    public static Scope of(String value) {
        if (value == null || value.isBlank() || value.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("scope must be a non-blank word without spaces");
        }
        return new Scope(value);
    }

    /**
     * Returns the scope name.
     *
     * @return the name as sent to the token endpoint
     */
    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Scope && value.equals(((Scope) o).value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
