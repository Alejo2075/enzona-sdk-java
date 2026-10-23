package io.github.alejo2075.enzona;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.alejo2075.enzona.exception.NotFoundException;
import io.github.alejo2075.enzona.http.ApiRequest;
import io.github.alejo2075.enzona.model.PaymentStatus;
import io.github.alejo2075.enzona.model.RefundPage;
import io.github.alejo2075.enzona.model.RefundQuery;
import io.github.alejo2075.enzona.model.SortOrder;
import io.github.alejo2075.enzona.testsupport.Clients;
import io.github.alejo2075.enzona.testsupport.FakeTransport;
import io.github.alejo2075.enzona.testsupport.Fixtures;
import org.junit.jupiter.api.Test;

class RefundsApiListForPaymentTest {

    private static final String UUID = "9f0b2c0c-4f9e-4d6a-9d3c-1a2b3c4d5e6f";

    private static FakeTransport page() {
        return Clients.scripted(
                FakeTransport.json(200, Fixtures.text("refunds/refunds-list.json")));
    }

    @Test
    void listsTheRefundsOfThePaymentFromItsPath() {
        FakeTransport t = page();

        RefundPage result = Clients.client(t).refunds().listForPayment(UUID);

        ApiRequest r = Clients.apiRequests(t).get(0);
        assertEquals("GET", r.method());
        assertEquals("/payment/v1.0.0/payments/" + UUID + "/refunds", r.uri().getRawPath());
        assertNull(r.uri().getRawQuery());
        assertEquals(1, result.refunds().size());
    }

    @Test
    void onlyPagingStatusDateAndOrderFiltersAreSent() {
        FakeTransport t = page();

        Clients.client(t)
                .refunds()
                .listForPayment(
                        UUID,
                        RefundQuery.builder()
                                .merchantUuid("ignored")
                                .transactionUuid("ignored")
                                .commerceRefundId("ignored")
                                .limit(3)
                                .status(PaymentStatus.ACCEPTED)
                                .order(SortOrder.DESC)
                                .build());

        assertEquals(
                "limit=3&status_filter=1111&order_filter=desc",
                Clients.apiRequests(t).get(0).uri().getRawQuery());
    }

    @Test
    void theIdentifierIsEncodedAndValidated() {
        FakeTransport t = page();
        EnzonaClient client = Clients.client(t);

        client.refunds().listForPayment("a/b");
        assertEquals(
                "/payment/v1.0.0/payments/a%2Fb/refunds",
                Clients.apiRequests(t).get(0).uri().getRawPath());
        assertThrows(IllegalArgumentException.class, () -> client.refunds().listForPayment(" "));
        assertThrows(NullPointerException.class, () -> client.refunds().listForPayment(UUID, null));
    }

    @Test
    void anUnknownPaymentIsNotFound() {
        FakeTransport t = Clients.scripted(FakeTransport.json(404, "{\"code\":4066}"));
        assertThrows(
                NotFoundException.class, () -> Clients.client(t).refunds().listForPayment(UUID));
    }

    @Test
    void isRetriedOnlyWhenAPolicyIsEnabled() {
        FakeTransport t =
                Clients.scripted(
                        FakeTransport.json(500, "{}"),
                        FakeTransport.json(200, Fixtures.text("refunds/refunds-list.json")));

        Clients.client(t, Clients.FAST_RETRIES).refunds().listForPayment(UUID);

        assertEquals(2, Clients.apiRequests(t).size());
    }

    @Test
    void listForPaymentAsyncReturnsThePage() {
        assertEquals(
                1,
                Clients.client(page())
                        .refunds()
                        .listForPaymentAsync(UUID, RefundQuery.builder().build())
                        .join()
                        .refunds()
                        .size());
    }
}
