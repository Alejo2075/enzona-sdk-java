package io.github.alejo2075.enzona.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PaymentQueryTest {

    @Test
    void anEmptyQueryHasNoParameters() {
        assertTrue(PaymentQuery.builder().build().toQueryParameters().isEmpty());
    }

    @Test
    void everyFilterMapsToItsWireName() {
        Map<String, String> q =
                PaymentQuery.builder()
                        .merchantUuid("m-1")
                        .limit(10)
                        .offset(2)
                        .merchantOpId("123456789012")
                        .enzonaOpId("e-9")
                        .status(PaymentStatus.CONFIRMED)
                        .startDate(LocalDate.of(2026, 10, 1))
                        .endDate(LocalDate.of(2026, 10, 23))
                        .order(SortOrder.DESC)
                        .build()
                        .toQueryParameters();

        assertEquals(
                List.of(
                        "merchant_uuid",
                        "limit",
                        "offset",
                        "merchant_op_filter",
                        "enzona_op_filter",
                        "status_filter",
                        "start_date_filter",
                        "end_date_filter",
                        "order_filter"),
                List.copyOf(q.keySet()));
        assertEquals("m-1", q.get("merchant_uuid"));
        assertEquals("10", q.get("limit"));
        assertEquals("2", q.get("offset"));
        assertEquals("123456789012", q.get("merchant_op_filter"));
        assertEquals("1116", q.get("status_filter"));
        assertEquals("2026-10-01", q.get("start_date_filter"));
        assertEquals("2026-10-23", q.get("end_date_filter"));
        assertEquals("desc", q.get("order_filter"));
    }

    @Test
    void blankFiltersAreOmitted() {
        assertTrue(
                PaymentQuery.builder()
                        .merchantUuid("  ")
                        .merchantOpId("")
                        .build()
                        .toQueryParameters()
                        .isEmpty());
    }

    @Test
    void outOfRangeValuesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> PaymentQuery.builder().limit(0).build());
        assertThrows(
                IllegalArgumentException.class, () -> PaymentQuery.builder().offset(-1).build());
        assertThrows(
                IllegalArgumentException.class,
                () -> PaymentQuery.builder().status(PaymentStatus.UNKNOWN).build());
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        PaymentQuery.builder()
                                .startDate(LocalDate.of(2026, 10, 2))
                                .endDate(LocalDate.of(2026, 10, 1))
                                .build());
    }

    @Test
    void sortOrderWireValues() {
        assertEquals("asc", SortOrder.ASC.wireValue());
        assertEquals("desc", SortOrder.DESC.wireValue());
    }
}
