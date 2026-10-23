package io.github.alejo2075.enzona;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.alejo2075.enzona.exception.PermissionDeniedException;
import io.github.alejo2075.enzona.http.ApiRequest;
import io.github.alejo2075.enzona.model.PaymentPage;
import io.github.alejo2075.enzona.model.PaymentQuery;
import io.github.alejo2075.enzona.model.PaymentStatus;
import io.github.alejo2075.enzona.model.SortOrder;
import io.github.alejo2075.enzona.testsupport.Clients;
import io.github.alejo2075.enzona.testsupport.FakeTransport;
import io.github.alejo2075.enzona.testsupport.Fixtures;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PaymentsApiListTest {

    private static FakeTransport page() {
        return Clients.scripted(
                FakeTransport.json(200, Fixtures.text("payments/list-response.json")));
    }

    @Test
    void listsWithFiltersEncodedInTheQueryString() {
        FakeTransport t = page();

        Clients.client(t)
                .payments()
                .list(
                        PaymentQuery.builder()
                                .merchantUuid("m 1&x")
                                .limit(2)
                                .status(PaymentStatus.ACCEPTED)
                                .order(SortOrder.DESC)
                                .build());

        ApiRequest r = Clients.apiRequests(t).get(0);
        assertEquals("GET", r.method());
        assertEquals("/payment/v1.0.0/payments", r.uri().getRawPath());
        assertEquals(
                "merchant_uuid=m%201%26x&limit=2&status_filter=1111&order_filter=desc",
                r.uri().getRawQuery());
        assertNull(r.body());
    }

    @Test
    void listWithoutFiltersHasNoQueryString() {
        FakeTransport t = page();

        Clients.client(t).payments().list();

        assertNull(Clients.apiRequests(t).get(0).uri().getRawQuery());
    }

    @Test
    void parsesPaymentsAndPaging() {
        PaymentPage page = Clients.client(page()).payments().list();

        assertEquals(2, page.payments().size());
        assertEquals(
                "9f0b2c0c-4f9e-4d6a-9d3c-1a2b3c4d5e6f", page.payments().get(0).transactionUuid());
        assertEquals(PaymentStatus.ACCEPTED, page.payments().get(0).status());
        assertEquals("Aceptada", page.payments().get(0).statusDenom());
        assertEquals("1002", page.payments().get(1).invoiceNumber());
        assertEquals("Pendiente", page.payments().get(1).statusDenom());
        assertEquals(new BigDecimal("25"), page.payments().get(1).amount().total());
        assertEquals(7L, page.pagination().total());
        assertEquals(new BigDecimal("137.00"), page.pagination().totalAmount());
        assertTrue(page.pagination().next().contains("offset=1"));
        assertNull(page.pagination().prev());
    }

    @Test
    void anEmptyOrMissingListIsAnEmptyPage() {
        for (String body : new String[] {"{\"payments\":[]}", "{}", "{\"payments\":null}"}) {
            FakeTransport t = Clients.scripted(FakeTransport.json(200, body));
            PaymentPage page = Clients.client(t).payments().list();
            assertTrue(page.payments().isEmpty(), body);
            assertNull(page.pagination());
        }
    }

    @Test
    void listIsRetriedOnlyWhenAPolicyIsEnabled() {
        FakeTransport t =
                Clients.scripted(
                        FakeTransport.json(502, "{}"),
                        FakeTransport.json(200, Fixtures.text("payments/list-response.json")));

        PaymentPage page = Clients.client(t, Clients.FAST_RETRIES).payments().list();

        assertEquals(2, page.payments().size());
        assertEquals(2, Clients.apiRequests(t).size());
    }

    @Test
    void forbiddenIsAPermissionDeniedException() {
        FakeTransport t =
                Clients.scripted(
                        FakeTransport.json(
                                403, "{\"fault\":{\"code\":900910,\"message\":\"scope\"}}"));

        PermissionDeniedException e =
                assertThrows(
                        PermissionDeniedException.class, () -> Clients.client(t).payments().list());
        assertEquals(900910, e.errorCode().getAsInt());
    }

    @Test
    void listAsyncReturnsThePage() {
        PaymentPage page =
                Clients.client(page())
                        .payments()
                        .listAsync(PaymentQuery.builder().limit(2).build())
                        .join();

        assertEquals(2, page.payments().size());
    }

    @Test
    void aNullQueryIsRejected() {
        assertThrows(
                NullPointerException.class,
                () -> Clients.client(Clients.scripted()).payments().list(null));
    }
}
