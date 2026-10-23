package io.github.alejo2075.enzona;

import java.net.URI;

/** The ENZONA environment a client talks to. Each one has its own consumer key and secret. */
public enum Environment {

    /** Real payments: {@code https://api.enzona.net}. */
    PRODUCTION("https://api.enzona.net"),

    /** Test environment: {@code https://apisandbox.enzona.net}. */
    SANDBOX("https://apisandbox.enzona.net");

    private final URI apiRoot;

    Environment(String apiRoot) {
        this.apiRoot = URI.create(apiRoot);
    }

    /**
     * Returns the API root, without trailing slash.
     *
     * @return the root URL; the token endpoint is {@code /token} below it
     */
    public URI apiRoot() {
        return apiRoot;
    }
}
