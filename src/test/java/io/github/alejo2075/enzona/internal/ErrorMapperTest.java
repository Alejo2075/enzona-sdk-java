package io.github.alejo2075.enzona.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.alejo2075.enzona.exception.AuthenticationException;
import io.github.alejo2075.enzona.exception.EnzonaApiException;
import io.github.alejo2075.enzona.exception.EnzonaErrorCode;
import io.github.alejo2075.enzona.exception.NotFoundException;
import io.github.alejo2075.enzona.exception.PermissionDeniedException;
import io.github.alejo2075.enzona.exception.RateLimitException;
import io.github.alejo2075.enzona.exception.ServerException;
import io.github.alejo2075.enzona.exception.ValidationException;
import io.github.alejo2075.enzona.http.ApiResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ErrorMapperTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-10-23T12:00:00Z"), ZoneOffset.UTC);

    private static ApiResponse response(int status, String body) {
        return response(status, body, Map.of());
    }

    private static ApiResponse response(int status, String body, Map<String, List<String>> h) {
        return new ApiResponse(status, h, body.getBytes(StandardCharsets.UTF_8));
    }

    private static EnzonaApiException map(int status, String body) {
        return ErrorMapper.map(response(status, body), CLOCK);
    }

    @Test
    void mapsEachStatusToItsExceptionType() {
        assertInstanceOf(ValidationException.class, map(400, "{}"));
        assertInstanceOf(AuthenticationException.class, map(401, "{}"));
        assertInstanceOf(PermissionDeniedException.class, map(403, "{}"));
        assertInstanceOf(NotFoundException.class, map(404, "{}"));
        assertInstanceOf(RateLimitException.class, map(429, "{}"));
        assertInstanceOf(ServerException.class, map(500, "{}"));
        assertInstanceOf(ServerException.class, map(503, "{}"));
        EnzonaApiException other = map(418, "{}");
        assertEquals(EnzonaApiException.class, other.getClass());
        assertEquals(418, other.statusCode());
    }

    @Test
    void readsTheApiEnvelopeWithNumericCode() {
        EnzonaApiException e =
                map(400, "{\"code\":4083,\"message\":\"Formato incorrecto en la estructura\"}");

        assertEquals(4083, e.errorCode().getAsInt());
        assertEquals(EnzonaErrorCode.ITEM_TAX_MISMATCH, e.knownErrorCode().orElseThrow());
        assertEquals("Formato incorrecto en la estructura", e.apiMessage());
        assertNull(e.description());
    }

    @Test
    void readsTheGatewayFaultEnvelope() {
        EnzonaApiException e =
                map(
                        401,
                        "{\"fault\":{\"code\":900901,\"message\":\"Invalid Credentials\","
                                + "\"description\":\"Invalid Credentials. Make sure you have\"}}");

        assertEquals(900901, e.errorCode().getAsInt());
        assertEquals(EnzonaErrorCode.GATEWAY_INVALID_CREDENTIALS, e.knownErrorCode().orElseThrow());
        assertEquals("Invalid Credentials", e.apiMessage());
        assertEquals("Invalid Credentials. Make sure you have", e.description());
    }

    @Test
    void acceptsTheCodeAsAStringAndFallsBackToTheErrorField() {
        assertEquals(
                4064, map(404, "{\"code\":\"4064\",\"message\":\"x\"}").errorCode().getAsInt());
        assertEquals("boom", map(500, "{\"error\":\"boom\"}").apiMessage());
        assertFalse(map(400, "{\"code\":\"abc\"}").errorCode().isPresent());
        assertFalse(map(400, "{\"code\":{\"a\":1}}").errorCode().isPresent());
    }

    @Test
    void readsGatewayFaultsSentAsXml() {
        EnzonaApiException e =
                map(
                        401,
                        "<ams:fault xmlns:ams=\"http://wso2.org/apimanager/security\">"
                                + "<ams:code>900901</ams:code><ams:message>Invalid Credentials"
                                + "</ams:message><ams:description>x</ams:description></ams:fault>");

        assertEquals(900901, e.errorCode().getAsInt());
        assertEquals("Invalid Credentials", e.apiMessage());
    }

    @Test
    void neverEchoesHtmlOrOtherNonJsonBodies() {
        EnzonaApiException e = map(502, "<html><body>secret internal page</body></html>");

        assertFalse(e.errorCode().isPresent());
        assertNull(e.apiMessage());
        assertFalse(e.getMessage().contains("internal"));
    }

    @Test
    void emptyAndJsonArrayBodiesGiveNoDetails() {
        assertNull(map(500, "").apiMessage());
        assertNull(map(500, "[1,2]").apiMessage());
        assertFalse(map(500, "null").errorCode().isPresent());
    }

    @Test
    void longMessagesAreTruncated() {
        String longText = "x".repeat(2000);
        EnzonaApiException e = map(400, "{\"message\":\"" + longText + "\"}");

        assertEquals(503, e.apiMessage().length());
        assertTrue(e.apiMessage().endsWith("..."));
    }

    @Test
    void keepsOnlyRateLimitHeaders() {
        EnzonaApiException e =
                ErrorMapper.map(
                        response(
                                429,
                                "{}",
                                Map.of(
                                        "Retry-After", List.of("12"),
                                        "X-RateLimit-Remaining", List.of("0"),
                                        "Set-Cookie", List.of("session=abc"),
                                        "Authorization", List.of("Bearer leak"))),
                        CLOCK);

        assertEquals(
                Map.of("retry-after", "12", "x-ratelimit-remaining", "0"), e.rateLimitHeaders());
        RateLimitException rate = assertInstanceOf(RateLimitException.class, e);
        assertEquals(Duration.ofSeconds(12), rate.retryAfter().orElseThrow());
    }

    @Test
    void parsesRetryAfterInSecondsAndAsHttpDate() {
        assertEquals(Duration.ofSeconds(5), ErrorMapper.parseRetryAfter(" 5 ", CLOCK));
        assertEquals(
                Duration.ofSeconds(30),
                ErrorMapper.parseRetryAfter("Fri, 23 Oct 2026 12:00:30 GMT", CLOCK));
        assertEquals(
                Duration.ZERO, ErrorMapper.parseRetryAfter("Fri, 23 Oct 2026 11:00:00 GMT", CLOCK));
        assertNull(ErrorMapper.parseRetryAfter("soon", CLOCK));
        assertNull(ErrorMapper.parseRetryAfter("-3", CLOCK));
        assertNull(ErrorMapper.parseRetryAfter(null, CLOCK));
        assertNull(ErrorMapper.parseRetryAfter(" ", CLOCK));
    }
}
