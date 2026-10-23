package example;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A tiny local stand-in for ENZONA, for the examples' tests. It needs no account and moves no
 * money. It enforces the Bearer token and the payment life cycle: pending (1113), confirmed by the
 * buyer (1116), completed (1111).
 */
final class MockEnzona implements AutoCloseable {

    private static final Pattern TOTAL = Pattern.compile("\"total\"\\s*:\\s*\"([0-9.]+)\"");
    private static final Pattern CURRENCY = Pattern.compile("\"currency\"\\s*:\\s*\"([A-Z]{3})\"");
    private static final Pattern OP_ID = Pattern.compile("\"merchant_op_id\"\\s*:\\s*\"([0-9]+)\"");

    private record Stored(String total, String currency, String opId, int status) {}

    private final HttpServer server;
    private final AtomicInteger counter = new AtomicInteger();
    private final Map<String, Stored> payments = new ConcurrentHashMap<>();
    private final Map<String, String> forced = new ConcurrentHashMap<>();
    final List<String> log = new CopyOnWriteArrayList<>();
    final List<String> bodies = new CopyOnWriteArrayList<>();

    MockEnzona() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", this::handle);
        server.start();
    }

    String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    /** The buyer confirms the payment inside the ENZONA app. */
    void buyerConfirms(String uuid) {
        Stored s = payments.get(uuid);
        payments.put(uuid, new Stored(s.total, s.currency, s.opId, 1116));
    }

    /** Forces a reply for a request, e.g. {@code force("POST /payment/v1.0.0/payments/x/refund", 400, "{...}")}. */
    void force(String request, int status, String body) {
        forced.put(request, status + "|" + body);
    }

    int count(String request) {
        return (int) log.stream().filter(request::equals).count();
    }

    private void handle(HttpExchange ex) throws IOException {
        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String path = ex.getRequestURI().getPath();
        String key = ex.getRequestMethod() + " " + path;
        log.add(key);
        bodies.add(body);
        String f = forced.get(key);
        if (f != null) {
            int bar = f.indexOf('|');
            reply(ex, Integer.parseInt(f.substring(0, bar)), f.substring(bar + 1));
            return;
        }
        if (path.equals("/token")) {
            reply(ex, 200, "{\"access_token\":\"mock-token\",\"token_type\":\"Bearer\",\"expires_in\":3600}");
            return;
        }
        if (!"Bearer mock-token".equals(ex.getRequestHeaders().getFirst("Authorization"))) {
            reply(ex, 401, "{\"fault\":{\"code\":900901,\"message\":\"Invalid Credentials\"}}");
            return;
        }
        String base = "/payment/v1.0.0/payments";
        if (key.equals("POST " + base)) {
            String uuid = "mock-" + counter.incrementAndGet();
            payments.put(uuid, new Stored(first(TOTAL, body), first(CURRENCY, body), first(OP_ID, body), 1113));
            reply(ex, 200, payment(uuid));
        } else if (ex.getRequestMethod().equals("GET") && path.startsWith(base + "/") && !path.endsWith("/refunds")) {
            String uuid = path.substring(base.length() + 1);
            reply(ex, payments.containsKey(uuid) ? 200 : 404,
                    payments.containsKey(uuid) ? payment(uuid) : "{\"code\":4064,\"message\":\"Recurso no disponible\"}");
        } else if (key.endsWith("/complete") && ex.getRequestMethod().equals("POST")) {
            String uuid = path.substring(base.length() + 1, path.length() - "/complete".length());
            Stored s = payments.get(uuid);
            if (s == null || s.status != 1116) {
                reply(ex, 400, "{\"code\":4065,\"message\":\"El pago no esta confirmado\"}");
            } else {
                payments.put(uuid, new Stored(s.total, s.currency, s.opId, 1111));
                reply(ex, 200, payment(uuid));
            }
        } else if (key.endsWith("/refund") && ex.getRequestMethod().equals("POST")) {
            String uuid = path.substring(base.length() + 1, path.length() - "/refund".length());
            Stored known = payments.get(uuid);
            String amount = body.contains("\"total\"") ? first(TOTAL, body) : known != null ? known.total : "0.00";
            reply(ex, 200, "{\"transaction_uuid\":\"refund-of-" + uuid + "\",\"parent_payment_uuid\":\"" + uuid
                    + "\",\"status_code\":1111,\"status_denom\":\"Aceptada\",\"amount\":{\"total\":\"" + amount + "\"}}");
        } else if (key.endsWith("/refunds") && ex.getRequestMethod().equals("GET")) {
            String uuid = path.substring(base.length() + 1, path.length() - "/refunds".length());
            reply(ex, 200, "{\"refunds\":[{\"transaction_uuid\":\"refund-of-" + uuid
                    + "\",\"status_denom\":\"Aceptada\"}],\"pagination\":{\"total\":1}}");
        } else {
            reply(ex, 404, "{\"code\":4064}");
        }
    }

    private String payment(String uuid) {
        Stored s = payments.get(uuid);
        return "{\"transaction_uuid\":\"" + uuid + "\",\"status_code\":" + s.status + ",\"currency\":\"" + s.currency
                + "\",\"merchant_op_id\":\"" + s.opId + "\",\"amount\":{\"total\":\"" + s.total
                + "\"},\"links\":[{\"rel\":\"confirm\",\"method\":\"REDIRECT\",\"href\":\"https://enzona.example/confirm/"
                + uuid + "\"}]}";
    }

    private static String first(Pattern p, String text) {
        Matcher m = p.matcher(text);
        return m.find() ? m.group(1) : null;
    }

    private static void reply(HttpExchange ex, int status, String json) throws IOException {
        byte[] out = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(status, out.length);
        ex.getResponseBody().write(out);
        ex.close();
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
