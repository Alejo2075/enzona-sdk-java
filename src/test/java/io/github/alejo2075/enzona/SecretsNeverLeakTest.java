package io.github.alejo2075.enzona;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.alejo2075.enzona.auth.AccessTokenProvider;
import io.github.alejo2075.enzona.auth.ClientCredentials;
import io.github.alejo2075.enzona.exception.EnzonaException;
import io.github.alejo2075.enzona.exception.TokenException;
import io.github.alejo2075.enzona.http.ApiRequest;
import io.github.alejo2075.enzona.model.CreatePaymentRequest;
import io.github.alejo2075.enzona.model.PaymentItem;
import io.github.alejo2075.enzona.testsupport.Clients;
import io.github.alejo2075.enzona.testsupport.FakeTransport;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The consumer secret and the access token must never show up in text the SDK produces. */
class SecretsNeverLeakTest {

    private static final String SECRET = "SUPER-SECRET-VALUE-123";
    private static final String TOKEN = "ACCESS-TOKEN-VALUE-XYZ";
    private static final String TOKEN_JSON =
            "{\"access_token\":\"" + TOKEN + "\",\"token_type\":\"Bearer\",\"expires_in\":3600}";

    private static void assertClean(String text, String where) {
        assertFalse(text.contains(SECRET), where + " leaks the secret: " + text);
        assertFalse(text.contains(TOKEN), where + " leaks the token: " + text);
    }

    private static String stackTrace(Throwable t) {
        StringWriter w = new StringWriter();
        t.printStackTrace(new PrintWriter(w));
        return w.toString();
    }

    private static CreatePaymentRequest request() {
        return CreatePaymentRequest.builder()
                .currency("CUP")
                .merchantOpId("123456789012")
                .item(PaymentItem.of("A", 1, BigDecimal.TEN, BigDecimal.ZERO))
                .returnUrl("https://shop.example/ok")
                .cancelUrl("https://shop.example/no")
                .build();
    }

    @Test
    void toStringOfEveryCredentialCarryingTypeIsRedacted() {
        FakeTransport t = FakeTransport.returning(FakeTransport.json(200, TOKEN_JSON));
        EnzonaClient client =
                EnzonaClient.builder().credentials("key", SECRET).transport(t).build();
        client.payments().list();

        assertClean(ClientCredentials.of("key", SECRET).toString(), "ClientCredentials");
        assertClean(client.toString(), "EnzonaClient");
        assertClean(client.settings().toString(), "ClientSettings");
        assertClean(AccessTokenProvider.fixed(TOKEN).toString(), "fixed provider");
        for (ApiRequest r : t.requests()) {
            assertClean(r.toString(), "ApiRequest");
        }
    }

    @Test
    void failuresDoNotCarrySecretsInMessagesCausesOrStackTraces() {
        List<EnzonaException> failures = new ArrayList<>();
        Object[][] scripts = {
            {FakeTransport.json(401, "{\"error\":\"invalid_client\"}")},
            {FakeTransport.json(500, "<html>boom</html>")},
            {FakeTransport.networkError()},
            {
                FakeTransport.json(200, TOKEN_JSON),
                FakeTransport.json(401, "{\"fault\":{\"code\":900901}}")
            },
            {FakeTransport.json(200, TOKEN_JSON), FakeTransport.json(403, "{\"code\":4001}")},
            {FakeTransport.json(200, TOKEN_JSON), FakeTransport.json(404, "{\"code\":4064}")},
            {FakeTransport.json(200, TOKEN_JSON), FakeTransport.networkError()},
            {FakeTransport.json(200, TOKEN_JSON), FakeTransport.json(200, "not json")},
        };
        for (Object[] script : scripts) {
            FakeTransport t = FakeTransport.returning(script);
            EnzonaClient client =
                    EnzonaClient.builder().credentials("key", SECRET).transport(t).build();
            failures.add(
                    assertThrows(EnzonaException.class, () -> client.payments().create(request())));
        }
        failures.add(
                assertThrows(
                        TokenException.class,
                        () ->
                                EnzonaClient.builder()
                                        .credentials("key", SECRET)
                                        .transport(
                                                FakeTransport.returning(
                                                        FakeTransport.json(401, "{}")))
                                        .build()
                                        .payments()
                                        .get("x")));

        for (EnzonaException e : failures) {
            for (Throwable c = e; c != null; c = c.getCause()) {
                assertClean(String.valueOf(c.getMessage()), "message of " + c.getClass());
            }
            assertClean(stackTrace(e), "stack trace of " + e.getClass());
            assertClean(e.toString(), "toString of " + e.getClass());
        }
    }

    @Test
    void theSdkPrintsNothingToTheConsole() {
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out, true));
        System.setErr(new PrintStream(err, true));
        try {
            FakeTransport t =
                    FakeTransport.returning(
                            FakeTransport.json(200, TOKEN_JSON), FakeTransport.json(500, "{}"));
            EnzonaClient client =
                    EnzonaClient.builder().credentials("key", SECRET).transport(t).build();
            assertThrows(EnzonaException.class, () -> client.payments().get("x"));
            client.payments().getAsync("x").handle((p, e) -> null).join();
        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
        assertFalse(out.size() > 0, "stdout: " + out);
        assertFalse(err.size() > 0, "stderr: " + err);
    }

    @Test
    void theSecretIsOnlySentToTheTokenEndpointAsBasicAuth() {
        FakeTransport t = Clients.scripted(FakeTransport.json(200, "{\"id\":1}"));
        EnzonaClient client =
                EnzonaClient.builder().credentials("key", SECRET).transport(t).build();

        client.payments().list();

        ApiRequest apiCall = t.requests().get(1);
        assertFalse(apiCall.headers().toString().contains(SECRET));
        assertFalse(
                apiCall.headers().get("Authorization").contains("Basic"),
                "API calls use Bearer, never the consumer credentials");
        assertFalse(new String(t.requests().get(0).body()).contains(SECRET));
    }
}
