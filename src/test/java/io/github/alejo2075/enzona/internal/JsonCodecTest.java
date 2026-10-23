package io.github.alejo2075.enzona.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.alejo2075.enzona.exception.SerializationException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JsonCodecTest {

    record Sample(
            BigDecimal total,
            Integer statusCode,
            Long invoiceNumber,
            String merchantOpId,
            @JsonProperty("name") List<String> names) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Out(BigDecimal total, String returnUrl, String description) {}

    private static byte[] b(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    private static String text(byte[] json) {
        return new String(json, StandardCharsets.UTF_8);
    }

    @Test
    void writesMoneyAsTwoDecimalString() {
        assertEquals(
                "{\"total\":\"10.00\",\"return_url\":\"https://x\"}",
                text(JsonCodec.write(new Out(new BigDecimal("10"), "https://x", null))));
        assertEquals(
                "{\"total\":\"0.50\"}",
                text(JsonCodec.write(new Out(new BigDecimal("0.5"), null, null))));
        assertEquals(
                "{\"total\":\"1200.00\"}",
                text(JsonCodec.write(new Out(new BigDecimal("1.2E+3"), null, null))));
    }

    @Test
    void trailingZerosBeyondTwoDecimalsAreAccepted() {
        assertEquals(
                "{\"total\":\"3.10\"}",
                text(JsonCodec.write(new Out(new BigDecimal("3.1000"), null, null))));
    }

    @Test
    void moreThanTwoDecimalsAreRejectedNotRounded() {
        SerializationException e =
                assertThrows(
                        SerializationException.class,
                        () -> JsonCodec.write(new Out(new BigDecimal("10.005"), null, null)));
        assertNotNull(e.getMessage());
    }

    @Test
    void readsMoneyFromStringAndFromNumber() {
        assertEquals(
                new BigDecimal("112.00"),
                JsonCodec.read(b("{\"total\":\"112.00\"}"), Sample.class).total());
        assertEquals(
                new BigDecimal("25.50"),
                JsonCodec.read(b("{\"total\":25.50}"), Sample.class).total());
    }

    @Test
    void readsIntegersSentAsStringsOrNumbers() {
        Sample asStrings =
                JsonCodec.read(
                        b("{\"status_code\":\"1113\",\"invoice_number\":\"1001\"}"), Sample.class);
        assertEquals(1113, asStrings.statusCode());
        assertEquals(1001L, asStrings.invoiceNumber());

        Sample asNumbers =
                JsonCodec.read(b("{\"status_code\":1111,\"invoice_number\":7}"), Sample.class);
        assertEquals(1111, asNumbers.statusCode());
        assertEquals(7L, asNumbers.invoiceNumber());
    }

    @Test
    void readsItemNameSentAsStringOrAsArray() {
        assertEquals(
                List.of("T-shirt"),
                JsonCodec.read(b("{\"name\":\"T-shirt\"}"), Sample.class).names());
        assertEquals(
                List.of("T-shirt", "Cap"),
                JsonCodec.read(b("{\"name\":[\"T-shirt\",\"Cap\"]}"), Sample.class).names());
    }

    @Test
    void keepsLongNumericIdentifiersAsStrings() {
        assertEquals(
                "123456789012",
                JsonCodec.read(b("{\"merchant_op_id\":\"123456789012\"}"), Sample.class)
                        .merchantOpId());
    }

    @Test
    void ignoresUnknownPropertiesAndMapsNulls() {
        Sample s = JsonCodec.read(b("{\"brand_new\":{\"a\":1},\"total\":null}"), Sample.class);
        assertNull(s.total());
    }

    @Test
    void emptyOrInvalidBodiesFailWithSerializationException() {
        assertThrows(SerializationException.class, () -> JsonCodec.read(b(""), Sample.class));
        assertThrows(SerializationException.class, () -> JsonCodec.read(null, Sample.class));
        assertThrows(SerializationException.class, () -> JsonCodec.read(b("<html>"), Sample.class));
    }

    @Test
    void writesMapsWithSnakeCaseKeysUntouched() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("grant_type", "client_credentials");
        assertEquals("{\"grant_type\":\"client_credentials\"}", text(JsonCodec.write(m)));
    }

    @Test
    void readTreeOrNullToleratesGarbage() {
        assertNull(JsonCodec.readTreeOrNull(null));
        assertNull(JsonCodec.readTreeOrNull(b("")));
        assertNull(JsonCodec.readTreeOrNull(b("<html>not json")));
        assertTrue(JsonCodec.readTreeOrNull(b("{\"code\":4064}")).isObject());
    }
}
