package io.github.alejo2075.enzona.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AuthTest {

    @Test
    void credentialsRequireNonBlankValues() {
        assertThrows(IllegalArgumentException.class, () -> ClientCredentials.of(null, "s"));
        assertThrows(IllegalArgumentException.class, () -> ClientCredentials.of("k", " "));
    }

    @Test
    void credentialsToStringRedactsTheSecret() {
        ClientCredentials c = ClientCredentials.of("key-1", "TOPSECRET");
        assertTrue(c.toString().contains("key-1"));
        assertFalse(c.toString().contains("TOPSECRET"));
    }

    @Test
    void credentialsEqualityUsesBothValues() {
        ClientCredentials a = ClientCredentials.of("k", "s");
        assertEquals(a, ClientCredentials.of("k", "s"));
        assertEquals(a.hashCode(), ClientCredentials.of("k", "s").hashCode());
        assertNotEquals(a, ClientCredentials.of("k", "other"));
        assertNotEquals(a, ClientCredentials.of("other", "s"));
        assertNotEquals(a, "k");
    }

    @Test
    void basicAuthorizationEncodesKeyAndSecret() {
        // base64("key:secret")
        assertEquals(
                "Basic a2V5OnNlY3JldA==",
                ClientCredentials.of("key", "secret").basicAuthorization());
    }

    @Test
    void knownScopesHaveTheDocumentedNames() {
        assertEquals("enzona_business_payment", Scope.PAYMENT.value());
        assertEquals("enzona_business_qr", Scope.QR.value());
        assertEquals(Scope.of("enzona_business_qr"), Scope.QR);
        assertEquals("custom", Scope.of("custom").toString());
        assertEquals(Scope.QR.hashCode(), Scope.of("enzona_business_qr").hashCode());
    }

    @Test
    void scopeRejectsBlankOrSpacedNames() {
        assertThrows(IllegalArgumentException.class, () -> Scope.of(null));
        assertThrows(IllegalArgumentException.class, () -> Scope.of(" "));
        assertThrows(IllegalArgumentException.class, () -> Scope.of("a b"));
    }

    @Test
    void fixedProviderReturnsTheTokenAndIgnoresInvalidation() {
        AccessTokenProvider p = AccessTokenProvider.fixed("ABC-123-XYZ");
        assertEquals("ABC-123-XYZ", p.accessToken());
        p.invalidate("ABC-123-XYZ");
        assertEquals("ABC-123-XYZ", p.accessToken());
        assertEquals("ABC-123-XYZ", p.accessTokenAsync().join());
        assertFalse(p.toString().contains("ABC-123-XYZ"));
        assertThrows(IllegalArgumentException.class, () -> AccessTokenProvider.fixed(""));
    }

    @Test
    void defaultAsyncFailsExceptionallyWhenTheTokenCannotBeObtained() {
        AccessTokenProvider broken =
                new AccessTokenProvider() {
                    @Override
                    public String accessToken() {
                        throw new IllegalStateException("no token");
                    }

                    @Override
                    public void invalidate(String rejectedToken) {}
                };
        assertTrue(broken.accessTokenAsync().isCompletedExceptionally());
    }
}
