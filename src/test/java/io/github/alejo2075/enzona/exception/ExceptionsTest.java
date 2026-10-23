package io.github.alejo2075.enzona.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ExceptionsTest {

    @Test
    void apiExceptionKeepsStatusCodeAndMessages() {
        EnzonaApiException e =
                new EnzonaApiException(
                        400, 4083, "Formato incorrecto", "details", Map.of("retry-after", "3"));

        assertEquals(400, e.statusCode());
        assertEquals(4083, e.errorCode().getAsInt());
        assertEquals(EnzonaErrorCode.ITEM_TAX_MISMATCH, e.knownErrorCode().orElseThrow());
        assertEquals("Formato incorrecto", e.apiMessage());
        assertEquals("details", e.description());
        assertEquals("3", e.rateLimitHeaders().get("retry-after"));
        assertEquals("ENZONA API error (HTTP 400, code=4083): Formato incorrecto", e.getMessage());
    }

    @Test
    void apiExceptionWithoutCodeHasEmptyCode() {
        EnzonaApiException e = new EnzonaApiException(502, null, null, null, null);

        assertFalse(e.errorCode().isPresent());
        assertFalse(e.knownErrorCode().isPresent());
        assertTrue(e.rateLimitHeaders().isEmpty());
        assertEquals("ENZONA API error (HTTP 502)", e.getMessage());
    }

    @Test
    void rateLimitHeadersAreNotModifiable() {
        EnzonaApiException e = new EnzonaApiException(429, null, null, null, Map.of());

        assertThrows(UnsupportedOperationException.class, () -> e.rateLimitHeaders().put("a", "b"));
    }

    @Test
    void unknownCodeMapsToUnknown() {
        assertEquals(EnzonaErrorCode.UNKNOWN, EnzonaErrorCode.of(123456));
        assertEquals(EnzonaErrorCode.TRANSACTION_NOT_FOUND, EnzonaErrorCode.of(4064));
        assertEquals(4064, EnzonaErrorCode.TRANSACTION_NOT_FOUND.code());
    }

    @Test
    void errorCodesAreUnique() {
        long distinct =
                java.util.Arrays.stream(EnzonaErrorCode.values())
                        .mapToInt(EnzonaErrorCode::code)
                        .distinct()
                        .count();
        assertEquals(EnzonaErrorCode.values().length, distinct);
    }

    @Test
    void rateLimitExposesRetryAfter() {
        RateLimitException e =
                new RateLimitException(900800, "slow down", null, Map.of(), Duration.ofSeconds(7));

        assertEquals(429, e.statusCode());
        assertEquals(Duration.ofSeconds(7), e.retryAfter().orElseThrow());
        assertFalse(
                new RateLimitException(null, null, null, Map.of(), null).retryAfter().isPresent());
    }

    @Test
    void everyApiSubclassIsAnEnzonaException() {
        Map<String, String> none = Map.of();
        assertInstanceOf(
                EnzonaException.class, new ValidationException(400, null, null, null, none));
        assertInstanceOf(
                EnzonaException.class, new AuthenticationException(401, null, null, null, none));
        assertInstanceOf(
                EnzonaException.class, new PermissionDeniedException(403, null, null, null, none));
        assertInstanceOf(EnzonaException.class, new NotFoundException(404, null, null, null, none));
        assertInstanceOf(EnzonaException.class, new ServerException(500, null, null, null, none));
    }

    @Test
    void tokenExceptionCarriesStatus() {
        assertEquals(401, new TokenException("rejected", 401).statusCode());
        assertEquals(0, new TokenException("io", new RuntimeException()).statusCode());
    }

    @Test
    void transportAndSerializationKeepTheirCause() {
        RuntimeException cause = new RuntimeException("boom");
        assertEquals(cause, new TransportException("x", cause).getCause());
        assertEquals(cause, new SerializationException("x", cause).getCause());
        assertEquals("x", new TransportException("x").getMessage());
        assertEquals("y", new SerializationException("y").getMessage());
    }
}
