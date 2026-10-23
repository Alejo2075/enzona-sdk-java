package io.github.alejo2075.enzona.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.alejo2075.enzona.internal.JsonCodec;
import io.github.alejo2075.enzona.testsupport.Fixtures;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class RefundTest {

    private static Refund read(String json) {
        return JsonCodec.read(json.getBytes(StandardCharsets.UTF_8), Refund.class);
    }

    private static String json(Object o) {
        return new String(JsonCodec.write(o), StandardCharsets.UTF_8);
    }

    @Test
    void readsARefundWithArrayItemNamesAndRefundedAmounts() {
        Refund r = read(Fixtures.text("refunds/refund-response.json"));

        assertEquals("7a6b5c4d-3e2f-4a1b-8c9d-0e1f2a3b4c5d", r.transactionUuid());
        assertEquals("9f0b2c0c-4f9e-4d6a-9d3c-1a2b3c4d5e6f", r.parentPaymentUuid());
        assertEquals("2002", r.transactionCode());
        assertEquals("12", r.terminalId());
        assertEquals("Aceptada", r.statusDenom());
        assertEquals(new BigDecimal("50.00"), r.amount().total());
        assertEquals(new BigDecimal("50.00"), r.amount().details().refunded());
        assertEquals(new BigDecimal("50.00"), r.amount().details().totalRefunded());
        assertEquals(List.of("T-shirt"), r.items().get(0).names());
        assertEquals("Ana", r.refundName());
        assertEquals("https://cdn.example/a.png", r.refundAvatar());
    }

    @Test
    void statusFallsBackToTheTransactionStatusCode() {
        assertEquals(
                PaymentStatus.ACCEPTED,
                read(Fixtures.text("refunds/refund-response.json")).status());
        assertEquals(PaymentStatus.REFUNDED, read("{\"status_code\":1115}").status());
        assertEquals(PaymentStatus.UNKNOWN, read("{}").status());
    }

    @Test
    void missingFieldsAreNullOrEmpty() {
        Refund r = read("{}");

        assertNull(r.transactionUuid());
        assertNull(r.statusDenom());
        assertTrue(r.items().isEmpty());
        assertTrue(r.links().isEmpty());
        assertTrue(r.statusDenoms().isEmpty());
    }

    @Test
    void aFullRefundSendsAnEmptyObject() {
        assertEquals("{}", json(RefundRequest.full()));
        assertEquals("{}", json(RefundRequest.builder().build()));
    }

    @Test
    void aPartialRefundSendsTheAmountAsATwoDecimalString() {
        String body =
                json(
                        RefundRequest.builder()
                                .amount(new BigDecimal("5"))
                                .description("Damaged item")
                                .commerceRefundId("R-1")
                                .build());

        assertEquals(
                "{\"amount\":{\"total\":\"5.00\"},\"description\":\"Damaged item\","
                        + "\"commerce_refund_id\":\"R-1\"}",
                body);
    }

    @Test
    void invalidRefundAmountsAreRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> RefundRequest.builder().amount(BigDecimal.ZERO).build());
        assertThrows(
                IllegalArgumentException.class,
                () -> RefundRequest.builder().amount(new BigDecimal("-1")).build());
        assertThrows(
                IllegalArgumentException.class,
                () -> RefundRequest.builder().amount(new BigDecimal("1.234")).build());
    }

    @Test
    void blankOptionalFieldsAreOmitted() {
        assertEquals(
                "{}", json(RefundRequest.builder().description(" ").commerceRefundId("").build()));
    }
}
