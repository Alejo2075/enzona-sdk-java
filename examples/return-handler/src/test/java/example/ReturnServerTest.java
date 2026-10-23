package example;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import example.ReturnServer.Order;
import io.github.alejo2075.enzona.EnzonaClient;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** The handler against a minimal ENZONA stand-in whose payment status the test controls. */
class ReturnServerTest {

    private HttpServer enzona;
    private ReturnServer shop;
    private int shopPort;
    private volatile int paymentStatus = 1113;
    private volatile String amount = "250.00";
    private final AtomicInteger completes = new AtomicInteger();
    private final AtomicInteger cancels = new AtomicInteger();
    private final HttpClient http = HttpClient.newHttpClient();

    @BeforeEach
    void start() throws IOException {
        enzona = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        enzona.createContext("/token", ex -> reply(ex, 200, "{\"access_token\":\"t\",\"expires_in\":3600}"));
        enzona.createContext("/payment/v1.0.0/payments/u-1/complete", ex -> {
            completes.incrementAndGet();
            paymentStatus = 1111;
            reply(ex, 200, payment());
        });
        enzona.createContext("/payment/v1.0.0/payments/u-1/cancel", ex -> {
            cancels.incrementAndGet();
            paymentStatus = 1117;
            reply(ex, 200, payment());
        });
        enzona.createContext("/payment/v1.0.0/payments/u-1", ex -> reply(ex, 200, payment()));
        enzona.createContext("/payment/v1.0.0/payments/boom", ex -> reply(ex, 500, "{}"));
        enzona.start();

        EnzonaClient client = EnzonaClient.builder()
                .credentials("test-key", "test-secret-not-real")
                .baseUrl("http://127.0.0.1:" + enzona.getAddress().getPort())
                .allowInsecureBaseUrlForTesting()
                .build();
        shop = new ReturnServer(client);
        shop.register(new Order("A-1", new BigDecimal("250.00"), "CUP", "123456789012", "u-1"));
        shop.register(new Order("A-2", new BigDecimal("250.00"), "CUP", "123456789012", "boom"));
        shopPort = shop.start(0);
    }

    @AfterEach
    void stop() {
        shop.close();
        enzona.stop(0);
    }

    private String payment() {
        return "{\"transaction_uuid\":\"u-1\",\"status_code\":" + paymentStatus
                + ",\"currency\":\"CUP\",\"merchant_op_id\":\"123456789012\",\"amount\":{\"total\":\"" + amount + "\"}}";
    }

    private HttpResponse<String> get(String path) throws Exception {
        return http.send(
                HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + shopPort + path)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void waitsWhileTheBuyerHasNotConfirmed() throws Exception {
        HttpResponse<String> r = get("/enzona/return?order=A-1");

        assertEquals(202, r.statusCode());
        assertEquals(0, completes.get());
    }

    @Test
    void completesAConfirmedPaymentExactlyOnceEvenIfTheBuyerReloads() throws Exception {
        paymentStatus = 1116;

        assertEquals(200, get("/enzona/return?order=A-1").statusCode());
        assertEquals(200, get("/enzona/return?order=A-1").statusCode());
        assertEquals(200, get("/enzona/return?order=A-1").statusCode());

        assertEquals(1, completes.get());
    }

    @Test
    void refusesToCompleteAPaymentThatDoesNotMatchTheOrder() throws Exception {
        paymentStatus = 1116;
        amount = "1.00";

        assertEquals(409, get("/enzona/return?order=A-1").statusCode());
        assertEquals(0, completes.get());
    }

    @Test
    void anUnknownOrAbsentOrderIsNotFound() throws Exception {
        assertEquals(404, get("/enzona/return?order=nope").statusCode());
        assertEquals(404, get("/enzona/return").statusCode());
        assertEquals(404, get("/enzona/return?other=A-1").statusCode());
    }

    @Test
    void anEnzonaFailureShowsNoDetailsToTheBuyer() throws Exception {
        HttpResponse<String> r = get("/enzona/return?order=A-2");

        assertEquals(502, r.statusCode());
        assertEquals("could not verify the payment, please retry", r.body());
    }

    @Test
    void cancellingCancelsThePayment() throws Exception {
        assertEquals(200, get("/enzona/cancel?order=A-1").statusCode());
        assertEquals(1, cancels.get());
        assertEquals(404, get("/enzona/cancel?order=nope").statusCode());
    }

    private static void reply(HttpExchange ex, int status, String json) throws IOException {
        byte[] out = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(status, out.length);
        ex.getResponseBody().write(out);
        ex.close();
    }
}
