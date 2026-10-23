package io.github.alejo2075.enzona.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.alejo2075.enzona.internal.JsonCodec;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class CreatePaymentRequestTest {

    private static CreatePaymentRequest.Builder valid() {
        return CreatePaymentRequest.builder()
                .currency("CUP")
                .merchantOpId("123456789012")
                .returnUrl("https://shop.example/enzona/return")
                .cancelUrl("https://shop.example/enzona/cancel");
    }

    private static PaymentItem shirt() {
        return PaymentItem.of("T-shirt", "Blue", 2, new BigDecimal("50"), new BigDecimal("2"));
    }

    private static String json(CreatePaymentRequest r) {
        return new String(JsonCodec.write(r), StandardCharsets.UTF_8);
    }

    @Test
    void computesTotalAndTaxFromTheItemsAndSerializesTheWireFormat() {
        CreatePaymentRequest r =
                valid().description("Order #123")
                        .item(shirt())
                        .shipping(new BigDecimal("10"))
                        .invoiceNumber("1001")
                        .terminalId("12")
                        .build();

        assertEquals(new BigDecimal("112.00"), r.amount().total());
        assertEquals(new BigDecimal("2.00"), r.amount().details().tax());
        assertEquals(
                "{\"description\":\"Order #123\",\"currency\":\"CUP\","
                        + "\"amount\":{\"total\":\"112.00\",\"details\":{\"shipping\":\"10.00\","
                        + "\"tax\":\"2.00\",\"discount\":\"0.00\",\"tip\":\"0.00\"}},"
                        + "\"items\":[{\"name\":\"T-shirt\",\"description\":\"Blue\","
                        + "\"quantity\":2,\"price\":\"50.00\",\"tax\":\"2.00\"}],"
                        + "\"merchant_op_id\":\"123456789012\",\"invoice_number\":\"1001\","
                        + "\"return_url\":\"https://shop.example/enzona/return\","
                        + "\"cancel_url\":\"https://shop.example/enzona/cancel\","
                        + "\"terminal_id\":\"12\"}",
                json(r));
    }

    @Test
    void appliesTipAndDiscountAndSumsSeveralItems() {
        CreatePaymentRequest r =
                valid().item(PaymentItem.of("A", 3, new BigDecimal("1.10"), new BigDecimal("0.30")))
                        .item(
                                PaymentItem.of(
                                        "B", 1, new BigDecimal("4.05"), new BigDecimal("0.20")))
                        .tip(new BigDecimal("1"))
                        .discount(new BigDecimal("0.50"))
                        .build();

        // 3*1.10 + 4.05 = 7.35 ; tax 0.50 ; + tip 1.00 - discount 0.50
        assertEquals(new BigDecimal("8.35"), r.amount().total());
        assertEquals(new BigDecimal("0.50"), r.amount().details().tax());
        assertEquals(new BigDecimal("0.50"), r.amount().details().discount());
        assertEquals(new BigDecimal("1.00"), r.amount().details().tip());
    }

    @Test
    void anExplicitTotalMustMatchTheComputedOne() {
        valid().item(shirt()).total(new BigDecimal("102")).build();

        IllegalArgumentException e =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> valid().item(shirt()).total(new BigDecimal("99.99")).build());
        assertTrue(e.getMessage().contains("102.00"), e.getMessage());
    }

    @Test
    void withoutItemsTheTotalIsRequiredAndUsedAsIs() {
        CreatePaymentRequest r = valid().total(new BigDecimal("25")).build();

        assertEquals(new BigDecimal("25.00"), r.amount().total());
        assertTrue(r.items().isEmpty());
        assertTrue(!json(r).contains("\"items\""), json(r));
        assertThrows(IllegalArgumentException.class, () -> valid().build());
    }

    @Test
    void amountsAreNeverRounded() {
        assertThrows(
                IllegalArgumentException.class,
                () -> PaymentItem.of("A", 1, new BigDecimal("1.005"), BigDecimal.ZERO));
        assertThrows(
                IllegalArgumentException.class,
                () -> valid().item(shirt()).shipping(new BigDecimal("0.001")).build());
        assertThrows(
                IllegalArgumentException.class,
                () -> valid().total(new BigDecimal("10.123")).build());
    }

    @Test
    void negativeAndZeroValuesAreRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> PaymentItem.of("A", 0, BigDecimal.ONE, BigDecimal.ZERO));
        assertThrows(
                IllegalArgumentException.class,
                () -> PaymentItem.of("A", 1, new BigDecimal("-1"), BigDecimal.ZERO));
        assertThrows(
                IllegalArgumentException.class,
                () -> PaymentItem.of("A", 1, BigDecimal.ONE, new BigDecimal("-0.01")));
        assertThrows(
                IllegalArgumentException.class,
                () -> valid().item(shirt()).tip(new BigDecimal("-1")).build());
        assertThrows(IllegalArgumentException.class, () -> valid().total(BigDecimal.ZERO).build());
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        valid().item(PaymentItem.of("A", 1, BigDecimal.TEN, BigDecimal.ZERO))
                                .discount(new BigDecimal("20"))
                                .build());
    }

    @Test
    void requiredFieldsAreChecked() {
        assertThrows(
                IllegalArgumentException.class,
                () -> valid().currency(null).total(BigDecimal.TEN).build());
        assertThrows(
                IllegalArgumentException.class,
                () -> valid().currency("CU").total(BigDecimal.TEN).build());
        assertThrows(
                IllegalArgumentException.class,
                () -> valid().merchantOpId(" ").total(BigDecimal.TEN).build());
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        CreatePaymentRequest.builder()
                                .currency("CUP")
                                .merchantOpId("1")
                                .cancelUrl("https://x.example")
                                .total(BigDecimal.TEN)
                                .build());
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        CreatePaymentRequest.builder()
                                .currency("CUP")
                                .merchantOpId("1")
                                .returnUrl("https://x.example")
                                .total(BigDecimal.TEN)
                                .build());
    }

    @Test
    void currencyIsNormalizedToUpperCase() {
        assertEquals("CUP", valid().currency("cup").total(BigDecimal.TEN).build().currency());
    }

    @Test
    void urlsMustBeAbsoluteHttpOrHttps() {
        assertThrows(IllegalArgumentException.class, () -> valid().returnUrl("/relative"));
        assertThrows(IllegalArgumentException.class, () -> valid().returnUrl("ftp://x.example"));
        assertThrows(IllegalArgumentException.class, () -> valid().cancelUrl("not a url"));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        valid().returnUrl(URI.create("javascript:alert(1)"))
                                .total(BigDecimal.TEN)
                                .build());
        assertEquals(
                URI.create("http://localhost:8080/ok"),
                valid().returnUrl(URI.create("http://localhost:8080/ok"))
                        .total(BigDecimal.TEN)
                        .build()
                        .returnUrl());
    }

    @Test
    void optionalFieldsAreOmittedWhenBlank() {
        CreatePaymentRequest r =
                valid().total(BigDecimal.TEN)
                        .merchantUuid(" ")
                        .description("")
                        .invoiceNumber(null)
                        .build();

        assertNull(r.merchantUuid());
        assertNull(r.description());
        assertNull(r.invoiceNumber());
        String body = json(r);
        assertTrue(!body.contains("merchant_uuid") && !body.contains("description"), body);
    }

    @Test
    void merchantUuidAndBuyerIdentityCodeAreSentWhenSet() {
        String body =
                json(
                        valid().total(BigDecimal.TEN)
                                .merchantUuid("11111111-2222-3333-4444-555555555555")
                                .buyerIdentityCode("B-1")
                                .build());
        assertTrue(body.contains("\"merchant_uuid\":\"11111111-2222-3333-4444-555555555555\""));
        assertTrue(body.contains("\"buyer_identity_code\":\"B-1\""));
    }

    @Test
    void generatedMerchantOpIdsAreTwelveDigits() {
        for (int i = 0; i < 200; i++) {
            assertTrue(
                    CreatePaymentRequest.newMerchantOpId().matches("\\d{12}"),
                    CreatePaymentRequest.newMerchantOpId());
        }
        CreatePaymentRequest r =
                CreatePaymentRequest.builder()
                        .currency("CUP")
                        .generateMerchantOpId()
                        .returnUrl("https://a.example")
                        .cancelUrl("https://b.example")
                        .total(BigDecimal.TEN)
                        .build();
        assertTrue(r.merchantOpId().matches("\\d{12}"));
    }

    @Test
    void itemsAreCopiedSoLaterChangesDoNotAffectTheRequest() {
        List<PaymentItem> source = new java.util.ArrayList<>(List.of(shirt()));
        CreatePaymentRequest r = valid().items(source).build();
        source.clear();
        assertEquals(1, r.items().size());
        assertThrows(UnsupportedOperationException.class, () -> r.items().clear());
    }
}
