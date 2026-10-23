package io.github.alejo2075.enzona.auth;

import java.util.concurrent.CompletableFuture;

/**
 * Supplies the OAuth2 Bearer token used on every API call.
 *
 * <p>The default implementation is {@link OAuthTokenProvider}. A custom one can share a token
 * between processes or use a token obtained elsewhere. Implementations must be thread-safe, are
 * called for every request and must never log the token.
 */
public interface AccessTokenProvider {

    /**
     * Returns a valid access token, requesting a new one if needed.
     *
     * @return the token without the {@code Bearer } prefix
     * @throws io.github.alejo2075.enzona.exception.TokenException if no token can be obtained
     */
    String accessToken();

    /**
     * Returns a valid access token without blocking the calling thread when a new one is needed.
     *
     * @return a future completed with the token, or exceptionally with a {@link
     *     io.github.alejo2075.enzona.exception.TokenException}
     */
    default CompletableFuture<String> accessTokenAsync() {
        try {
            return CompletableFuture.completedFuture(accessToken());
        } catch (RuntimeException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Tells the provider that the API rejected a token (HTTP 401), so the next call asks for a new
     * one. The token is discarded only if it is still the one in use, so a token that another
     * thread has just renewed is kept.
     *
     * @param rejectedToken the token the API rejected
     */
    void invalidate(String rejectedToken);

    /**
     * Creates a provider that always returns the same token and cannot renew it.
     *
     * @param token the token
     * @return the provider
     * @throws IllegalArgumentException if the token is null or blank
     */
    static AccessTokenProvider fixed(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("token must not be blank");
        }
        return new AccessTokenProvider() {
            @Override
            public String accessToken() {
                return token;
            }

            @Override
            public void invalidate(String rejectedToken) {
                // A fixed token cannot be renewed.
            }

            @Override
            public String toString() {
                return "AccessTokenProvider[token=****]";
            }
        };
    }
}
