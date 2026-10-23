package io.github.alejo2075.enzona;

import io.github.alejo2075.enzona.auth.AccessTokenProvider;
import io.github.alejo2075.enzona.auth.ClientCredentials;
import io.github.alejo2075.enzona.auth.OAuthTokenProvider;
import io.github.alejo2075.enzona.auth.Scope;
import io.github.alejo2075.enzona.http.HttpTransport;
import io.github.alejo2075.enzona.http.JdkHttpTransport;
import io.github.alejo2075.enzona.http.RetryPolicy;
import io.github.alejo2075.enzona.internal.ClientSettings;
import io.github.alejo2075.enzona.internal.RequestExecutor;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Entry point of the SDK: a thread-safe client for the ENZONA API.
 *
 * <p>Create one instance, reuse it for the whole application and share it between threads. It
 * caches the OAuth2 access token, so creating a client per request would ask for a new token every
 * time. There is nothing to close.
 *
 * <pre>{@code
 * EnzonaClient client = EnzonaClient.builder()
 *     .credentials(System.getenv("ENZONA_CONSUMER_KEY"), System.getenv("ENZONA_CONSUMER_SECRET"))
 *     .environment(Environment.SANDBOX)
 *     .build();
 * }</pre>
 *
 * <p>This is an unofficial, community-maintained SDK. Never put the consumer secret in code that
 * runs on a customer's device.
 */
public final class EnzonaClient {

    private final Environment environment;
    private final ClientSettings settings;
    private final RequestExecutor executor;

    private EnzonaClient(Environment environment, ClientSettings settings) {
        this.environment = environment;
        this.settings = settings;
        this.executor = new RequestExecutor(settings);
    }

    /**
     * Starts building a client.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the environment this client was built for.
     *
     * @return the environment, or {@code null} if a custom base URL is used
     */
    public Environment environment() {
        return environment;
    }

    /**
     * Returns the API root used by this client.
     *
     * @return the root URL without trailing slash
     */
    public String apiRoot() {
        return settings.apiRoot().toString();
    }

    RequestExecutor executor() {
        return executor;
    }

    ClientSettings settings() {
        return settings;
    }

    @Override
    public String toString() {
        return "EnzonaClient[apiRoot=" + settings.apiRoot() + ", tokens=" + settings.tokens() + "]";
    }

    /** Builder of {@link EnzonaClient}. */
    public static final class Builder {

        private ClientCredentials credentials;
        private AccessTokenProvider tokenProvider;
        private Environment environment = Environment.PRODUCTION;
        private String baseUrl;
        private boolean allowInsecure;
        private List<Scope> scopes = List.of(Scope.PAYMENT);
        private String scopeSeparator = " ";
        private Duration connectTimeout = JdkHttpTransport.DEFAULT_CONNECT_TIMEOUT;
        private Duration requestTimeout = Duration.ofSeconds(30);
        private RetryPolicy retryPolicy = RetryPolicy.disabled();
        private HttpTransport transport;
        private Clock clock = Clock.systemUTC();

        private Builder() {}

        /**
         * Sets the consumer key and secret used to request access tokens.
         *
         * @param credentials the credentials of the selected environment
         * @return this builder
         */
        public Builder credentials(ClientCredentials credentials) {
            this.credentials = Objects.requireNonNull(credentials, "credentials");
            return this;
        }

        /**
         * Sets the consumer key and secret used to request access tokens.
         *
         * @param consumerKey the consumer key
         * @param consumerSecret the consumer secret
         * @return this builder
         */
        public Builder credentials(String consumerKey, String consumerSecret) {
            return credentials(ClientCredentials.of(consumerKey, consumerSecret));
        }

        /**
         * Reads the credentials from {@code ENZONA_CONSUMER_KEY} and {@code
         * ENZONA_CONSUMER_SECRET}.
         *
         * @return this builder
         * @throws IllegalStateException if a variable is missing
         */
        public Builder credentialsFromEnvironment() {
            return credentials(ClientCredentials.fromEnvironment());
        }

        /**
         * Replaces the built-in OAuth2 token handling, for example to share a token between
         * processes. Use it instead of {@link #credentials(ClientCredentials)}, not together.
         *
         * @param provider the provider
         * @return this builder
         */
        public Builder accessTokenProvider(AccessTokenProvider provider) {
            this.tokenProvider = Objects.requireNonNull(provider, "provider");
            return this;
        }

        /**
         * Selects the environment. The default is {@link Environment#PRODUCTION}.
         *
         * @param environment the environment
         * @return this builder
         */
        public Builder environment(Environment environment) {
            this.environment = Objects.requireNonNull(environment, "environment");
            this.baseUrl = null;
            return this;
        }

        /**
         * Overrides the API root, for tests or a controlled gateway. It must be HTTPS unless {@link
         * #allowInsecureBaseUrlForTesting()} is called.
         *
         * @param baseUrl the root URL, e.g. {@code https://gateway.internal}
         * @return this builder
         */
        public Builder baseUrl(String baseUrl) {
            this.baseUrl = Objects.requireNonNull(baseUrl, "baseUrl");
            return this;
        }

        /**
         * Allows a plain {@code http} base URL. Only for loopback test servers.
         *
         * @return this builder
         */
        public Builder allowInsecureBaseUrlForTesting() {
            this.allowInsecure = true;
            return this;
        }

        /**
         * Sets the scopes requested for the access token. The default is {@link Scope#PAYMENT}.
         *
         * @param scopes at least one scope
         * @return this builder
         */
        public Builder scopes(Scope... scopes) {
            if (scopes == null || scopes.length == 0) {
                throw new IllegalArgumentException("at least one scope is required");
            }
            this.scopes = List.of(scopes);
            return this;
        }

        /**
         * Sets the text placed between several scopes (a space by default).
         *
         * @param separator the separator
         * @return this builder
         */
        public Builder scopeSeparator(String separator) {
            this.scopeSeparator = Objects.requireNonNull(separator, "separator");
            return this;
        }

        /**
         * Sets the connect timeout. The default is 5 seconds.
         *
         * @param timeout a positive duration
         * @return this builder
         */
        public Builder connectTimeout(Duration timeout) {
            this.connectTimeout = positive(timeout, "connectTimeout");
            return this;
        }

        /**
         * Sets the timeout of every request. The default is 30 seconds.
         *
         * @param timeout a positive duration
         * @return this builder
         */
        public Builder requestTimeout(Duration timeout) {
            this.requestTimeout = positive(timeout, "requestTimeout");
            return this;
        }

        /**
         * Sets the retry policy for read-only operations. Retries are off by default and never
         * apply to operations that create or change a payment.
         *
         * @param policy the policy
         * @return this builder
         */
        public Builder retryPolicy(RetryPolicy policy) {
            this.retryPolicy = Objects.requireNonNull(policy, "policy");
            return this;
        }

        /**
         * Replaces the HTTP transport, for example with a test double.
         *
         * @param transport the transport
         * @return this builder
         */
        public Builder transport(HttpTransport transport) {
            this.transport = Objects.requireNonNull(transport, "transport");
            return this;
        }

        /**
         * Sets the clock used for token expiry and {@code Retry-After} dates. Meant for tests.
         *
         * @param clock the clock
         * @return this builder
         */
        public Builder clock(Clock clock) {
            this.clock = Objects.requireNonNull(clock, "clock");
            return this;
        }

        /**
         * Builds the client.
         *
         * @return the client
         * @throws IllegalStateException if neither credentials nor a token provider were set, or
         *     both were
         * @throws IllegalArgumentException if the base URL is not acceptable
         */
        public EnzonaClient build() {
            if (credentials == null && tokenProvider == null) {
                throw new IllegalStateException(
                        "Set credentials(...) or accessTokenProvider(...) on the builder");
            }
            if (credentials != null && tokenProvider != null) {
                throw new IllegalStateException(
                        "Use either credentials(...) or accessTokenProvider(...), not both");
            }
            URI root =
                    baseUrl == null
                            ? environment.apiRoot()
                            : validateBaseUrl(baseUrl, allowInsecure);
            HttpTransport t =
                    transport != null
                            ? transport
                            : new JdkHttpTransport(connectTimeout, requestTimeout);
            AccessTokenProvider tokens = tokenProvider;
            if (tokens == null) {
                tokens =
                        OAuthTokenProvider.builder(credentials, t, URI.create(root + "/token"))
                                .scopes(scopes)
                                .scopeSeparator(scopeSeparator)
                                .clock(clock)
                                .requestTimeout(requestTimeout)
                                .build();
            }
            ClientSettings settings =
                    new ClientSettings(
                            t,
                            tokens,
                            root,
                            requestTimeout,
                            retryPolicy,
                            clock,
                            "enzona-sdk-java/" + EnzonaVersion.get());
            return new EnzonaClient(baseUrl == null ? environment : null, settings);
        }

        private static Duration positive(Duration d, String name) {
            Objects.requireNonNull(d, name);
            if (d.isZero() || d.isNegative()) {
                throw new IllegalArgumentException(name + " must be positive");
            }
            return d;
        }

        private static URI validateBaseUrl(String raw, boolean allowInsecure) {
            URI uri;
            try {
                uri = new URI(raw.trim());
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException("baseUrl is not a valid URL");
            }
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            boolean https = scheme.equals("https");
            if (!https && !(allowInsecure && scheme.equals("http"))) {
                throw new IllegalArgumentException(
                        "baseUrl must use https (plain http is only allowed with"
                                + " allowInsecureBaseUrlForTesting())");
            }
            if (uri.getHost() == null
                    || uri.getUserInfo() != null
                    || uri.getRawQuery() != null
                    || uri.getRawFragment() != null) {
                throw new IllegalArgumentException(
                        "baseUrl must have a host and no user info, query or fragment");
            }
            String s = uri.toString();
            while (s.endsWith("/")) {
                s = s.substring(0, s.length() - 1);
            }
            return URI.create(s);
        }
    }
}
