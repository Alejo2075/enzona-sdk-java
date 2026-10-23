package io.github.alejo2075.enzona.testsupport;

import io.github.alejo2075.enzona.EnzonaClient;
import io.github.alejo2075.enzona.http.ApiRequest;
import io.github.alejo2075.enzona.http.RetryPolicy;
import java.time.Duration;
import java.util.List;

/** Builds clients wired to an in-memory transport. */
public final class Clients {

    public static final String TOKEN =
            "{\"access_token\":\"test-token\",\"token_type\":\"Bearer\",\"expires_in\":3600}";

    public static final RetryPolicy FAST_RETRIES =
            RetryPolicy.exponentialBackoff(3, Duration.ofMillis(1), Duration.ofMillis(5));

    private Clients() {}

    /** A transport that answers the token request first and then the given outcomes. */
    public static FakeTransport scripted(Object... apiOutcomes) {
        FakeTransport t = FakeTransport.returning(FakeTransport.json(200, TOKEN));
        for (Object o : apiOutcomes) {
            t.thenReturn(o);
        }
        return t;
    }

    public static EnzonaClient client(FakeTransport transport) {
        return client(transport, RetryPolicy.disabled());
    }

    public static EnzonaClient client(FakeTransport transport, RetryPolicy retries) {
        return EnzonaClient.builder()
                .credentials("key", "secret")
                .transport(transport)
                .retryPolicy(retries)
                .build();
    }

    /** The requests sent to the API, without the initial token request. */
    public static List<ApiRequest> apiRequests(FakeTransport transport) {
        List<ApiRequest> all = transport.requests();
        return all.subList(1, all.size());
    }
}
