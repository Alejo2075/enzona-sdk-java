package example;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.alejo2075.enzona.EnzonaClient;
import io.github.alejo2075.enzona.exception.EnzonaException;
import io.github.alejo2075.enzona.model.Payment;
import io.github.alejo2075.enzona.model.PaymentStatus;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The endpoints a shop needs for ENZONA: where the buyer returns after paying ({@code returnUrl})
 * and where they return if they cancel ({@code cancelUrl}).
 *
 * <p>The redirect proves nothing, so the handler looks the payment up with the identifier it stored
 * when it created the payment, checks it against the order and only then completes it. Handling the
 * same return twice completes the payment once. The orders live in memory here; use a database.
 */
public final class ReturnServer implements AutoCloseable {

    /** What the shop remembers about an order. */
    public record Order(
            String id,
            BigDecimal total,
            String currency,
            String merchantOpId,
            String transactionUuid) {}

    private final EnzonaClient client;
    private final ConcurrentHashMap<String, Order> orders = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object> locks = new ConcurrentHashMap<>();
    private HttpServer server;

    public ReturnServer(EnzonaClient client) {
        this.client = client;
    }

    /** Remembers an order right after its payment was created. */
    public void register(Order order) {
        orders.put(order.id(), order);
    }

    /** Starts listening on a loopback port (0 picks a free one) and returns the port. */
    public int start(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 0);
        server.createContext("/enzona/return", this::onReturn);
        server.createContext("/enzona/cancel", this::onCancel);
        server.start();
        return server.getAddress().getPort();
    }

    private void onReturn(HttpExchange ex) throws IOException {
        Order order = orderFrom(ex);
        if (order == null) {
            reply(ex, 404, "unknown order");
            return;
        }
        synchronized (locks.computeIfAbsent(order.id(), k -> new Object())) {
            try {
                handleReturn(ex, order);
            } catch (EnzonaException e) {
                // Never show ENZONA details to the buyer. The buyer can reload the page.
                reply(ex, 502, "could not verify the payment, please retry");
            }
        }
    }

    private void handleReturn(HttpExchange ex, Order order) throws IOException {
        // snippet:start
        Payment payment = client.payments().get(order.transactionUuid());

        if (payment.status() == PaymentStatus.ACCEPTED) {
            reply(ex, 200, "paid"); // already completed: reloading the page is harmless
        } else if (payment.status() != PaymentStatus.CONFIRMED) {
            reply(ex, 202, "waiting for the buyer to confirm");
        } else if (!matches(order, payment)) {
            reply(ex, 409, "the payment does not match the order");
        } else {
            Payment completed = client.payments().complete(order.transactionUuid());
            boolean paid = completed.status() == PaymentStatus.ACCEPTED;
            reply(ex, paid ? 200 : 202, paid ? "paid" : "waiting for the buyer to confirm");
        }
        // snippet:end
    }

    private static boolean matches(Order order, Payment payment) {
        return order.currency().equals(payment.currency())
                && order.merchantOpId().equals(payment.merchantOpId())
                && payment.amount() != null
                && order.total().compareTo(payment.amount().total()) == 0;
    }

    private void onCancel(HttpExchange ex) throws IOException {
        Order order = orderFrom(ex);
        if (order == null) {
            reply(ex, 404, "unknown order");
            return;
        }
        synchronized (locks.computeIfAbsent(order.id(), k -> new Object())) {
            try {
                client.payments().cancel(order.transactionUuid());
                reply(ex, 200, "cancelled");
            } catch (EnzonaException e) {
                reply(ex, 502, "could not cancel the payment, please retry");
            }
        }
    }

    private Order orderFrom(HttpExchange ex) {
        String query = ex.getRequestURI().getRawQuery();
        if (query == null) {
            return null;
        }
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0 && pair.substring(0, eq).equals("order")) {
                return orders.get(URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8));
            }
        }
        return null;
    }

    private static void reply(HttpExchange ex, int status, String text) throws IOException {
        byte[] out = text.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
        ex.sendResponseHeaders(status, out.length);
        ex.getResponseBody().write(out);
        ex.close();
    }

    @Override
    public void close() {
        if (server != null) {
            server.stop(0);
        }
    }

    public static void main(String[] args) throws IOException {
        EnzonaClient client = EnzonaClient.builder().credentialsFromEnvironment().build();
        ReturnServer s = new ReturnServer(client);
        System.out.println("Listening on http://127.0.0.1:" + s.start(8080));
    }
}
