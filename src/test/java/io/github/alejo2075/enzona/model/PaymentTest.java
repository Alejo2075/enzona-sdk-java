package io.github.alejo2075.enzona.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.alejo2075.enzona.internal.JsonCodec;
import io.github.alejo2075.enzona.testsupport.Fixtures;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class PaymentTest {

    private static Payment read(String json) {
        return JsonCodec.read(json.getBytes(StandardCharsets.UTF_8), Payment.class);
    }

    @Test
    void readsTheCreateResponse() {
        Payment p = read(Fixtures.text("payments/create-response.json"));

        assertEquals("9f0b2c0c-4f9e-4d6a-9d3c-1a2b3c4d5e6f", p.transactionUuid());
        assertEquals(PaymentStatus.PENDING, p.status());
        assertEquals(1113, p.statusCode());
        assertEquals("Pendiente", p.statusDenom());
        assertEquals("CUP", p.currency());
        assertEquals("1001", p.invoiceNumber());
        assertEquals("12", p.terminalId());
        assertEquals("123456789012", p.merchantOpId());
        assertEquals("1500", p.transactionCode());
        assertEquals("2026-10-23T15:00:00.000Z", p.updatedAt());
        assertEquals(new BigDecimal("112.00"), p.amount().total());
        assertEquals(new BigDecimal("10.00"), p.amount().details().shipping());
        assertEquals(1, p.items().size());
        assertEquals("T-shirt", p.items().get(0).name());
        assertEquals(2, p.items().get(0).quantity());
        assertEquals(new BigDecimal("50.00"), p.items().get(0).price());
        assertEquals(3, p.links().size());
    }

    @Test
    void findsLinksByRelNotByPosition() {
        Payment p = read(Fixtures.text("payments/create-response.json"));

        assertEquals("POST", p.link("complete").orElseThrow().method());
        assertEquals("POST", p.link("CANCEL").orElseThrow().method());
        assertTrue(p.link("refund").isEmpty());
        assertEquals(
                URI.create(
                        "https://www.enzona.net/pay/confirm/9f0b2c0c-4f9e-4d6a-9d3c-1a2b3c4d5e6f"),
                p.confirmationUrl().orElseThrow());
    }

    @Test
    void readsNumbersSentAsStringsAndStringsSentAsNumbers() {
        Payment p = read(Fixtures.text("payments/payment-details.json"));

        assertEquals(1116, p.statusCode());
        assertEquals(PaymentStatus.CONFIRMED, p.status());
        assertEquals("Confirmada", p.statusDenom());
        assertEquals("1500", p.transactionCode());
        assertEquals("1001", p.invoiceNumber());
        assertEquals(new BigDecimal("112"), p.amount().total());
        assertEquals(new BigDecimal("2.00"), p.amount().details().tax());
        assertEquals(2, p.items().get(0).quantity());
        assertEquals(new BigDecimal("1.12"), p.commission());
        assertEquals(new BigDecimal("0.00"), p.refundedAmount());
        assertEquals("2026-10-23T15:05:00.000Z", p.updatedAt());
        assertEquals("Ana", p.name());
        assertEquals("https://shop.example/enzona/return", p.returnUrl());
        assertEquals("", p.buyerIdentityCode());
    }

    @Test
    void anItemNameSentAsAnArrayKeepsEveryName() {
        Payment p =
                read(
                        "{\"items\":[{\"name\":[\"T-shirt\",\"Cap\"],\"quantity\":1,"
                                + "\"price\":\"5.00\"}]}");

        assertEquals(List.of("T-shirt", "Cap"), p.items().get(0).names());
        assertEquals("T-shirt", p.items().get(0).name());
    }

    @Test
    void missingFieldsAreNullOrEmptyAndNeverFail() {
        Payment p = read("{}");

        assertNull(p.transactionUuid());
        assertNull(p.amount());
        assertNull(p.statusDenom());
        assertEquals(PaymentStatus.UNKNOWN, p.status());
        assertTrue(p.items().isEmpty());
        assertTrue(p.links().isEmpty());
        assertTrue(p.confirmationUrl().isEmpty());
    }

    @Test
    void anUnusableConfirmLinkGivesNoUrl() {
        assertTrue(
                read("{\"links\":[{\"rel\":\"confirm\",\"method\":\"REDIRECT\",\"href\":\"\"}]}")
                        .confirmationUrl()
                        .isEmpty());
        assertTrue(
                read("{\"links\":[{\"rel\":\"confirm\",\"href\":\"not a uri\"}]}")
                        .confirmationUrl()
                        .isEmpty());
        assertFalse(read("{\"links\":[{\"rel\":null}]}").link("confirm").isPresent());
    }

    @Test
    void statusMappingCoversEveryDocumentedCode() {
        assertEquals(PaymentStatus.ACCEPTED, PaymentStatus.fromCode(1111));
        assertEquals(PaymentStatus.FAILED, PaymentStatus.fromCode(1112));
        assertEquals(PaymentStatus.PENDING, PaymentStatus.fromCode(1113));
        assertEquals(PaymentStatus.REVERSED, PaymentStatus.fromCode(1114));
        assertEquals(PaymentStatus.REFUNDED, PaymentStatus.fromCode(1115));
        assertEquals(PaymentStatus.CONFIRMED, PaymentStatus.fromCode(1116));
        assertEquals(PaymentStatus.CANCELLED, PaymentStatus.fromCode(1117));
        assertEquals(PaymentStatus.ACTIVATED, PaymentStatus.fromCode(1118));
        assertEquals(PaymentStatus.DEACTIVATED, PaymentStatus.fromCode(1119));
        assertEquals(PaymentStatus.UNKNOWN, PaymentStatus.fromCode(9999));
        assertEquals(PaymentStatus.UNKNOWN, PaymentStatus.fromCode(null));
        assertEquals(1113, PaymentStatus.PENDING.code());
        assertEquals(-1, PaymentStatus.UNKNOWN.code());
    }
}
