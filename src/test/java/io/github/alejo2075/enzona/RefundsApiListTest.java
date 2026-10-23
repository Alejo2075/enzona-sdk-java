package io.github.alejo2075.enzona;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.alejo2075.enzona.http.ApiRequest;
import io.github.alejo2075.enzona.model.PaymentStatus;
import io.github.alejo2075.enzona.model.RefundPage;
import io.github.alejo2075.enzona.model.RefundQuery;
import io.github.alejo2075.enzona.model.SortOrder;
import io.github.alejo2075.enzona.testsupport.Clients;
import io.github.alejo2075.enzona.testsupport.FakeTransport;
import io.github.alejo2075.enzona.testsupport.Fixtures;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class RefundsApiListTest {

    private static FakeTransport page() {
        return Clients.scripted(
                FakeTransport.json(200, Fixtures.text("refunds/refunds-list.json")));
    }

    @Test
    void listsWithEveryFilterInTheQueryString() {
        FakeTransport t = page();

        Clients.client(t)
                .refunds()
                .list(
                        RefundQuery.builder()
                                .merchantUuid("m-1")
                                .transactionUuid("p-1")
                                .commerceRefundId("R 1")
                                .limit(5)
                                .offset(1)
                                .status(PaymentStatus.ACCEPTED)
                                .startDate(LocalDate.of(2026, 10, 1))
                                .endDate(LocalDate.of(2026, 10, 23))
                                .order(SortOrder.ASC)
                                .build());

        ApiRequest r = Clients.apiRequests(t).get(0);
        assertEquals("GET", r.method());
        assertEquals("/payment/v1.0.0/payments/refunds", r.uri().getRawPath());
        assertEquals(
                "merchant_uuid=m-1&transaction_uuid=p-1&commerce_refund_id=R%201&limit=5&offset=1"
                        + "&status_filter=1111&start_date_filter=2026-10-01"
                        + "&end_date_filter=2026-10-23&order_filter=asc",
                r.uri().getRawQuery());
    }

    @Test
    void parsesRefundsAndPaging() {
        RefundPage page = Clients.client(page()).refunds().list();

        assertEquals(1, page.refunds().size());
        assertEquals("Devolucion parcial", page.refunds().get(0).transactionDenom());
        assertEquals("Aceptada", page.refunds().get(0).statusDenom());
        assertEquals(
                new BigDecimal("50.00"), page.refunds().get(0).amount().details().totalRefunded());
        assertEquals(1L, page.pagination().total());
    }

    @Test
    void anEmptyOrMissingListIsAnEmptyPage() {
        for (String body : new String[] {"{\"refunds\":[]}", "{}"}) {
            FakeTransport t = Clients.scripted(FakeTransport.json(200, body));
            assertTrue(Clients.client(t).refunds().list().refunds().isEmpty(), body);
        }
    }

    @Test
    void listIsRetriedOnlyWhenAPolicyIsEnabled() {
        FakeTransport t =
                Clients.scripted(
                        FakeTransport.networkError(),
                        FakeTransport.json(200, Fixtures.text("refunds/refunds-list.json")));

        assertEquals(1, Clients.client(t, Clients.FAST_RETRIES).refunds().list().refunds().size());
        assertEquals(2, Clients.apiRequests(t).size());
    }

    @Test
    void invalidQueriesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> RefundQuery.builder().limit(0).build());
        assertThrows(
                IllegalArgumentException.class, () -> RefundQuery.builder().offset(-1).build());
        assertThrows(
                IllegalArgumentException.class,
                () -> RefundQuery.builder().status(PaymentStatus.UNKNOWN).build());
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        RefundQuery.builder()
                                .startDate(LocalDate.of(2026, 10, 2))
                                .endDate(LocalDate.of(2026, 10, 1))
                                .build());
        assertThrows(
                NullPointerException.class,
                () -> Clients.client(Clients.scripted()).refunds().list(null));
    }

    @Test
    void listAsyncReturnsThePage() {
        assertEquals(
                1,
                Clients.client(page())
                        .refunds()
                        .listAsync(RefundQuery.builder().limit(5).build())
                        .join()
                        .refunds()
                        .size());
    }
}
