package io.github.alejo2075.enzona.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Objects;

/**
 * The consumer key and consumer secret of an ENZONA application, as shown in the ENZONA API Store.
 *
 * <p>Sandbox and production use different pairs. They are sent only to the token endpoint, as HTTP
 * Basic authentication, never in a request body. Keep them in environment variables or a secret
 * manager and never ship them to a browser or mobile client.
 *
 * <p>{@link #toString()} redacts the secret.
 */
public final class ClientCredentials {

    private final String consumerKey;
    private final String consumerSecret;

    private ClientCredentials(String consumerKey, String consumerSecret) {
        this.consumerKey = requireText(consumerKey, "consumerKey");
        this.consumerSecret = requireText(consumerSecret, "consumerSecret");
    }

    /**
     * Creates credentials.
     *
     * @param consumerKey the consumer key
     * @param consumerSecret the consumer secret
     * @return the credentials
     * @throws IllegalArgumentException if a value is null or blank
     */
    public static ClientCredentials of(String consumerKey, String consumerSecret) {
        return new ClientCredentials(consumerKey, consumerSecret);
    }

    /**
     * Reads credentials from the {@code ENZONA_CONSUMER_KEY} and {@code ENZONA_CONSUMER_SECRET}
     * environment variables.
     *
     * @return the credentials
     * @throws IllegalStateException if a variable is missing or blank
     */
    public static ClientCredentials fromEnvironment() {
        String key = System.getenv("ENZONA_CONSUMER_KEY");
        String secret = System.getenv("ENZONA_CONSUMER_SECRET");
        if (key == null || key.isBlank() || secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "ENZONA_CONSUMER_KEY and ENZONA_CONSUMER_SECRET environment variables must"
                            + " be set");
        }
        return new ClientCredentials(key, secret);
    }

    /**
     * Returns the consumer key.
     *
     * @return the consumer key
     */
    public String consumerKey() {
        return consumerKey;
    }

    /**
     * Returns the consumer secret. Handle with care: do not log it.
     *
     * @return the consumer secret
     */
    public String consumerSecret() {
        return consumerSecret;
    }

    /** Value of the {@code Authorization} header for the token endpoint. */
    String basicAuthorization() {
        String pair = consumerKey + ":" + consumerSecret;
        return "Basic " + Base64.getEncoder().encodeToString(pair.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ClientCredentials)) {
            return false;
        }
        ClientCredentials other = (ClientCredentials) o;
        return consumerKey.equals(other.consumerKey)
                && MessageDigest.isEqual(
                        consumerSecret.getBytes(StandardCharsets.UTF_8),
                        other.consumerSecret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public int hashCode() {
        return Objects.hash(consumerKey);
    }

    @Override
    public String toString() {
        return "ClientCredentials[consumerKey=" + consumerKey + ", consumerSecret=****]";
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
