package io.github.alejo2075.enzona.internal;

import io.github.alejo2075.enzona.exception.SerializationException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

/**
 * The only place that talks to Jackson. The mapper is immutable and thread-safe.
 *
 * <p>Configuration:
 *
 * <ul>
 *   <li>snake_case property names and unknown properties ignored;
 *   <li>{@link BigDecimal} <b>written</b> as a JSON string with exactly two decimals ({@code
 *       "10.00"}), which is what ENZONA requires;
 *   <li>lenient <b>reading</b>: numbers may arrive as JSON numbers or strings (Jackson's scalar
 *       coercion), and a single value is accepted where a list is expected, because ENZONA sends
 *       {@code items[].name} as a string in payments and as an array in refunds.
 * </ul>
 */
public final class JsonCodec {

    private static final JsonMapper MAPPER = createMapper();

    private JsonCodec() {}

    private static JsonMapper createMapper() {
        SimpleModule money = new SimpleModule("enzona-money");
        money.addSerializer(BigDecimal.class, new MoneySerializer());
        return JsonMapper.builder()
                .addModule(money)
                .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
                .build();
    }

    /**
     * Serializes a value to UTF-8 JSON bytes.
     *
     * @param value a map, record or other bean
     * @return the JSON bytes
     * @throws SerializationException if the value cannot be serialized, for example a {@link
     *     BigDecimal} with more than two decimals
     */
    public static byte[] write(Object value) {
        try {
            return MAPPER.writeValueAsBytes(value);
        } catch (JacksonException e) {
            throw new SerializationException("Could not serialize request body", e);
        }
    }

    /**
     * Parses a JSON document into a type.
     *
     * @param json the raw bytes
     * @param type the target type
     * @param <T> the target type
     * @return the parsed value
     * @throws SerializationException if the body is empty or does not match the type
     */
    public static <T> T read(byte[] json, Class<T> type) {
        if (json == null || json.length == 0) {
            throw new SerializationException(
                    "Empty response body, expected " + type.getSimpleName());
        }
        try {
            return MAPPER.readValue(json, type);
        } catch (JacksonException e) {
            throw new SerializationException(
                    "Could not parse response body as " + type.getSimpleName(), e);
        }
    }

    /**
     * Parses a JSON document into a tree.
     *
     * @param json the raw bytes
     * @return the tree, or {@code null} if the input is empty or not valid JSON
     */
    public static JsonNode readTreeOrNull(byte[] json) {
        if (json == null || json.length == 0) {
            return null;
        }
        try {
            return MAPPER.readTree(new String(json, StandardCharsets.UTF_8));
        } catch (JacksonException e) {
            return null;
        }
    }

    /** Writes every {@link BigDecimal} as a two-decimal string. */
    private static final class MoneySerializer extends ValueSerializer<BigDecimal> {
        @Override
        public void serialize(BigDecimal value, JsonGenerator g, SerializationContext ctxt)
                throws JacksonException {
            try {
                g.writeString(Money.format(value));
            } catch (IllegalArgumentException e) {
                throw new SerializationException(e.getMessage());
            }
        }
    }
}
