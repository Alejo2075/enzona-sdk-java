package io.github.alejo2075.enzona.auth;

import io.github.alejo2075.enzona.exception.EnzonaException;
import io.github.alejo2075.enzona.exception.TokenException;
import io.github.alejo2075.enzona.http.ApiRequest;
import io.github.alejo2075.enzona.http.ApiResponse;
import io.github.alejo2075.enzona.http.HttpTransport;
import io.github.alejo2075.enzona.internal.JsonCodec;
import io.github.alejo2075.enzona.internal.UrlEncoding;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

/**
 * Obtains and caches ENZONA access tokens with the OAuth2 <em>client credentials</em> grant.
 *
 * <p>The token is requested with {@code POST /token} using HTTP Basic authentication and kept until
 * shortly before it expires ({@code expires_in} minus a safety margin). ENZONA has no refresh
 * token, so a new token is simply requested again.
 *
 * <p>The blocking {@link #accessToken()} lets one thread renew the token while the others wait for
 * it. {@link #accessTokenAsync()} shares one in-flight request among async callers. Neither the
 * secret nor the token ever appear in {@link #toString()} or in exception messages.
 */
public final class OAuthTokenProvider implements AccessTokenProvider {

    /** Expiry assumed when the token response carries no {@code expires_in}. */
    private static final Duration DEFAULT_LIFETIME = Duration.ofMinutes(5);

    private final ClientCredentials credentials;
    private final HttpTransport transport;
    private final URI tokenEndpoint;
    private final List<Scope> scopes;
    private final String scopeSeparator;
    private final Clock clock;
    private final Duration refreshMargin;
    private final Duration requestTimeout;

    private final Object lock = new Object();
    private volatile Token cached;
    private CompletableFuture<Token> inFlight;

    private OAuthTokenProvider(Builder b) {
        this.credentials = Objects.requireNonNull(b.credentials, "credentials");
        this.transport = Objects.requireNonNull(b.transport, "transport");
        this.tokenEndpoint = Objects.requireNonNull(b.tokenEndpoint, "tokenEndpoint");
        this.scopes = List.copyOf(b.scopes);
        this.scopeSeparator = b.scopeSeparator;
        this.clock = b.clock;
        this.refreshMargin = b.refreshMargin;
        this.requestTimeout = b.requestTimeout;
    }

    /**
     * Starts building a provider.
     *
     * @param credentials the consumer key and secret
     * @param transport the transport used to call the token endpoint
     * @param tokenEndpoint the absolute URL of {@code /token}
     * @return a builder
     */
    public static Builder builder(
            ClientCredentials credentials, HttpTransport transport, URI tokenEndpoint) {
        Builder b = new Builder();
        b.credentials = credentials;
        b.transport = transport;
        b.tokenEndpoint = tokenEndpoint;
        return b;
    }

    @Override
    public String accessToken() {
        Token t = cached;
        if (t != null && t.isUsableAt(clock.instant())) {
            return t.value;
        }
        synchronized (lock) {
            t = cached;
            if (t != null && t.isUsableAt(clock.instant())) {
                return t.value;
            }
            ApiResponse response;
            try {
                response = transport.send(tokenRequest());
            } catch (EnzonaException e) {
                throw new TokenException("The token request could not be completed", e);
            }
            t = parse(response);
            cached = t;
            return t.value;
        }
    }

    @Override
    public CompletableFuture<String> accessTokenAsync() {
        Token t = cached;
        if (t != null && t.isUsableAt(clock.instant())) {
            return CompletableFuture.completedFuture(t.value);
        }
        CompletableFuture<Token> pending;
        synchronized (lock) {
            t = cached;
            if (t != null && t.isUsableAt(clock.instant())) {
                return CompletableFuture.completedFuture(t.value);
            }
            if (inFlight == null) {
                CompletableFuture<Token> request =
                        transport
                                .sendAsync(tokenRequest())
                                .handle(
                                        (response, error) -> {
                                            if (error != null) {
                                                Throwable cause =
                                                        error instanceof CompletionException
                                                                        && error.getCause() != null
                                                                ? error.getCause()
                                                                : error;
                                                throw new TokenException(
                                                        "The token request could not be"
                                                                + " completed",
                                                        cause);
                                            }
                                            return parse(response);
                                        });
                inFlight = request;
                request.whenComplete(
                        (token, error) -> {
                            synchronized (lock) {
                                if (inFlight == request) {
                                    inFlight = null;
                                }
                                if (token != null) {
                                    cached = token;
                                }
                            }
                        });
            }
            pending = inFlight;
        }
        return pending.thenApply(token -> token.value);
    }

    @Override
    public void invalidate(String rejectedToken) {
        synchronized (lock) {
            Token t = cached;
            if (t != null && t.value.equals(rejectedToken)) {
                cached = null;
            }
        }
    }

    private ApiRequest tokenRequest() {
        StringBuilder form = new StringBuilder("grant_type=client_credentials");
        if (!scopes.isEmpty()) {
            String joined =
                    scopes.stream().map(Scope::value).collect(Collectors.joining(scopeSeparator));
            form.append("&scope=").append(UrlEncoding.encode(joined));
        }
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", credentials.basicAuthorization());
        headers.put("Content-Type", "application/x-www-form-urlencoded");
        headers.put("Accept", "application/json");
        return new ApiRequest(
                "POST",
                tokenEndpoint,
                headers,
                form.toString().getBytes(StandardCharsets.UTF_8),
                requestTimeout);
    }

    private Token parse(ApiResponse response) {
        if (!response.isSuccessful()) {
            throw new TokenException(
                    "The token endpoint rejected the request (HTTP "
                            + response.statusCode()
                            + "); check the consumer key, the secret and the environment",
                    response.statusCode());
        }
        TokenResponse body;
        try {
            body = JsonCodec.read(response.body(), TokenResponse.class);
        } catch (EnzonaException e) {
            throw new TokenException("The token endpoint returned an unreadable response", e);
        }
        if (body.accessToken() == null || body.accessToken().isBlank()) {
            throw new TokenException(
                    "The token endpoint response has no access_token", response.statusCode());
        }
        Duration lifetime =
                body.expiresIn() == null ? DEFAULT_LIFETIME : Duration.ofSeconds(body.expiresIn());
        Duration usable =
                lifetime.compareTo(refreshMargin) > 0
                        ? lifetime.minus(refreshMargin)
                        : lifetime.dividedBy(2);
        return new Token(body.accessToken(), clock.instant().plus(usable));
    }

    @Override
    public String toString() {
        return "OAuthTokenProvider[endpoint=" + tokenEndpoint + ", scopes=" + scopes + "]";
    }

    /** Response of the token endpoint. */
    record TokenResponse(String accessToken, Long expiresIn) {}

    /** A cached token and the instant after which it must be renewed. */
    private static final class Token {
        private final String value;
        private final Instant refreshAt;

        Token(String value, Instant refreshAt) {
            this.value = value;
            this.refreshAt = refreshAt;
        }

        boolean isUsableAt(Instant now) {
            return now.isBefore(refreshAt);
        }

        @Override
        public String toString() {
            return "Token[****]";
        }
    }

    /** Builder of {@link OAuthTokenProvider}. */
    public static final class Builder {
        private ClientCredentials credentials;
        private HttpTransport transport;
        private URI tokenEndpoint;
        private final List<Scope> scopes = new ArrayList<>(List.of(Scope.PAYMENT));
        private String scopeSeparator = " ";
        private Clock clock = Clock.systemUTC();
        private Duration refreshMargin = Duration.ofSeconds(60);
        private Duration requestTimeout;

        private Builder() {}

        /**
         * Sets the scopes requested for the token. The default is {@link Scope#PAYMENT}.
         *
         * @param scopes at least one scope
         * @return this builder
         */
        public Builder scopes(List<Scope> scopes) {
            if (scopes == null || scopes.isEmpty()) {
                throw new IllegalArgumentException("at least one scope is required");
            }
            this.scopes.clear();
            this.scopes.addAll(scopes);
            return this;
        }

        /**
         * Sets the text placed between several scopes. OAuth2 uses a space (the default); a
         * WordPress integration in the wild used a comma, which has not been confirmed with ENZONA.
         *
         * @param separator the separator
         * @return this builder
         */
        public Builder scopeSeparator(String separator) {
            if (separator == null || separator.isEmpty()) {
                throw new IllegalArgumentException("separator must not be empty");
            }
            this.scopeSeparator = separator;
            return this;
        }

        /**
         * Sets the clock used to decide when the token expires. Meant for tests.
         *
         * @param clock the clock
         * @return this builder
         */
        public Builder clock(Clock clock) {
            this.clock = Objects.requireNonNull(clock, "clock");
            return this;
        }

        /**
         * Sets how long before its expiry a token is renewed. The default is 60 seconds.
         *
         * @param margin a non-negative duration
         * @return this builder
         */
        public Builder refreshMargin(Duration margin) {
            if (margin == null || margin.isNegative()) {
                throw new IllegalArgumentException("refreshMargin must not be negative");
            }
            this.refreshMargin = margin;
            return this;
        }

        /**
         * Sets the timeout of the token request. By default the transport default applies.
         *
         * @param timeout the timeout
         * @return this builder
         */
        public Builder requestTimeout(Duration timeout) {
            this.requestTimeout = timeout;
            return this;
        }

        /**
         * Builds the provider.
         *
         * @return the provider
         */
        public OAuthTokenProvider build() {
            return new OAuthTokenProvider(this);
        }
    }
}
