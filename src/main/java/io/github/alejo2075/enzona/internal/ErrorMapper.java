package io.github.alejo2075.enzona.internal;

import io.github.alejo2075.enzona.exception.AuthenticationException;
import io.github.alejo2075.enzona.exception.EnzonaApiException;
import io.github.alejo2075.enzona.exception.NotFoundException;
import io.github.alejo2075.enzona.exception.PermissionDeniedException;
import io.github.alejo2075.enzona.exception.RateLimitException;
import io.github.alejo2075.enzona.exception.ServerException;
import io.github.alejo2075.enzona.exception.ValidationException;
import io.github.alejo2075.enzona.http.ApiResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import tools.jackson.databind.JsonNode;

/**
 * Turns a non-successful {@link ApiResponse} into a typed exception.
 *
 * <p>Two JSON envelopes exist. The API itself answers {@code {"code": 4064, "message": "..."}}. The
 * WSO2 gateway in front of it answers {@code {"fault": {"code": 900901, "message": "...",
 * "description": "..."}}} (or the same fields as XML when JSON was not negotiated). Bodies that are
 * neither are never echoed, since they may be HTML error pages.
 */
public final class ErrorMapper {

    private static final int MAX_TEXT_LENGTH = 500;
    private static final Pattern XML_CODE = Pattern.compile("<(?:\\w+:)?code>\\s*(\\d{1,9})\\s*<");
    private static final Pattern XML_MESSAGE =
            Pattern.compile("<(?:\\w+:)?message>\\s*([^<]{1,200}?)\\s*<");

    private ErrorMapper() {}

    /**
     * Maps a response to an exception.
     *
     * @param response a response whose status is not 2xx
     * @param clock used to interpret an HTTP-date {@code Retry-After}
     * @return the exception to throw
     */
    public static EnzonaApiException map(ApiResponse response, Clock clock) {
        int status = response.statusCode();
        Integer code = null;
        String message = null;
        String description = null;

        JsonNode tree = JsonCodec.readTreeOrNull(response.body());
        if (tree != null && tree.isObject()) {
            JsonNode source = tree.get("fault");
            if (source == null || !source.isObject()) {
                source = tree;
            }
            code = intValue(source, "code");
            message = text(source, "message");
            if (message == null) {
                message = text(source, "error");
            }
            description = text(source, "description");
        } else if (tree == null) {
            String raw = new String(response.body(), StandardCharsets.UTF_8);
            Matcher c = XML_CODE.matcher(raw);
            if (c.find()) {
                code = Integer.valueOf(c.group(1));
                Matcher m = XML_MESSAGE.matcher(raw);
                if (m.find()) {
                    message = m.group(1);
                }
            }
        }
        Map<String, String> rate = rateLimitHeaders(response);

        if (status == 400) {
            return new ValidationException(status, code, message, description, rate);
        } else if (status == 401) {
            return new AuthenticationException(status, code, message, description, rate);
        } else if (status == 403) {
            return new PermissionDeniedException(status, code, message, description, rate);
        } else if (status == 404) {
            return new NotFoundException(status, code, message, description, rate);
        } else if (status == 429) {
            return new RateLimitException(
                    code,
                    message,
                    description,
                    rate,
                    parseRetryAfter(rate.get("retry-after"), clock));
        } else if (status >= 500 && status < 600) {
            return new ServerException(status, code, message, description, rate);
        }
        return new EnzonaApiException(status, code, message, description, rate);
    }

    /**
     * Parses a {@code Retry-After} value (delta seconds or HTTP-date).
     *
     * @param value the header value, may be {@code null}
     * @param clock the clock for HTTP-date values
     * @return the delay, or {@code null} if absent or unparsable
     */
    public static Duration parseRetryAfter(String value, Clock clock) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String v = value.trim();
        try {
            long seconds = Long.parseLong(v);
            return seconds < 0 ? null : Duration.ofSeconds(seconds);
        } catch (NumberFormatException ignored) {
            // fall through to HTTP-date
        }
        try {
            ZonedDateTime when = ZonedDateTime.parse(v, DateTimeFormatter.RFC_1123_DATE_TIME);
            Duration d = Duration.between(clock.instant(), when.toInstant());
            return d.isNegative() ? Duration.ZERO : d;
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static Integer intValue(JsonNode tree, String field) {
        JsonNode n = tree.get(field);
        if (n == null || n.isNull()) {
            return null;
        }
        if (n.isNumber()) {
            return n.canConvertToInt() ? n.intValue() : null;
        }
        if (n.isString()) {
            try {
                return Integer.valueOf(n.asString().trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private static String text(JsonNode tree, String field) {
        JsonNode n = tree.get(field);
        if (n == null || n.isNull()) {
            return null;
        }
        String value = n.isString() ? n.asString() : n.toString();
        return value.length() > MAX_TEXT_LENGTH
                ? value.substring(0, MAX_TEXT_LENGTH) + "..."
                : value;
    }

    private static Map<String, String> rateLimitHeaders(ApiResponse response) {
        Map<String, String> out = new LinkedHashMap<>();
        response.headers()
                .forEach(
                        (name, values) -> {
                            if (!values.isEmpty()
                                    && (name.equals("retry-after")
                                            || name.startsWith("x-ratelimit-"))) {
                                out.put(name, values.get(0));
                            }
                        });
        return out;
    }
}
