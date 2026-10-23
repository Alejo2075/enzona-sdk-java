package io.github.alejo2075.enzona;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.alejo2075.enzona.testsupport.Fixtures;
import io.github.alejo2075.enzona.testsupport.TestServer;
import io.github.alejo2075.enzona.testsupport.TestServer.Recorded;
import io.github.alejo2075.enzona.testsupport.TestServer.Stub;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/** Clients configured with different credentials share nothing, even under heavy concurrency. */
class ClientIsolationTest {

    private static String basic(String key, String secret) {
        return "Basic "
                + Base64.getEncoder()
                        .encodeToString((key + ":" + secret).getBytes(StandardCharsets.UTF_8));
    }

    /** The token endpoint hands out a token named after the caller; the API echoes who called. */
    private static Stub answer(Recorded req) {
        if (req.rawPath().equals("/token")) {
            String user =
                    req.header("Authorization").equals(basic("key-a", "secret-a")) ? "A" : "B";
            return Stub.json(200, "{\"access_token\":\"token-" + user + "\",\"expires_in\":3600}")
                    .delayed(50);
        }
        return Stub.json(200, Fixtures.text("payments/payment-details.json"));
    }

    @Test
    void eachClientUsesItsOwnCredentialsAndItsOwnToken() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(24);
        try (TestServer server = TestServer.start(ClientIsolationTest::answer)) {
            String root = server.baseUri().toString();
            EnzonaClient a =
                    EnzonaClient.builder()
                            .credentials("key-a", "secret-a")
                            .baseUrl(root)
                            .allowInsecureBaseUrlForTesting()
                            .build();
            EnzonaClient b =
                    EnzonaClient.builder()
                            .credentials("key-b", "secret-b")
                            .baseUrl(root)
                            .allowInsecureBaseUrlForTesting()
                            .build();

            List<Future<?>> work = new ArrayList<>();
            for (int i = 0; i < 48; i++) {
                EnzonaClient client = i % 2 == 0 ? a : b;
                work.add(pool.submit(() -> client.payments().get("p-" + System.nanoTime())));
            }
            for (Future<?> f : work) {
                f.get(20, TimeUnit.SECONDS);
            }

            List<Recorded> all = server.requests();
            long tokenRequests = all.stream().filter(r -> r.rawPath().equals("/token")).count();
            assertEquals(2, tokenRequests, "one token request per client, however many threads");

            int checked = 0;
            for (Recorded r : all) {
                if (r.rawPath().equals("/token")) {
                    continue;
                }
                String bearer = r.header("Authorization");
                assertTrue(
                        bearer.equals("Bearer token-A") || bearer.equals("Bearer token-B"), bearer);
                checked++;
            }
            assertEquals(48, checked);
            long withA =
                    all.stream()
                            .filter(r -> "Bearer token-A".equals(r.header("Authorization")))
                            .count();
            long withB =
                    all.stream()
                            .filter(r -> "Bearer token-B".equals(r.header("Authorization")))
                            .count();
            assertEquals(24, withA);
            assertEquals(24, withB);
        } finally {
            pool.shutdownNow();
        }
    }
}
