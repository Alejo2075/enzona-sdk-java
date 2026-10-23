package example;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.sun.net.httpserver.HttpServer;
import io.github.alejo2075.enzona.EnzonaClient;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class PaymentDemoTest {

    @Test
    void createsAPaymentAgainstAMockServer() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/token", ex -> reply(ex, "{\"access_token\":\"t\",\"expires_in\":3600}"));
        server.createContext("/payment/v1.0.0/payments", ex -> reply(ex,
                "{\"transaction_uuid\":\"u-1\",\"status_code\":1113,\"links\":[{\"rel\":\"confirm\","
                        + "\"method\":\"REDIRECT\",\"href\":\"https://enzona.example/confirm/u-1\"}]}"));
        server.start();
        try {
            EnzonaClient client = EnzonaClient.builder()
                    .credentials("test-key", "test-secret-not-real")
                    .baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                    .allowInsecureBaseUrlForTesting()
                    .build();
            assertEquals("https://enzona.example/confirm/u-1", PaymentDemo.createPaymentUrl(client).toString());
        } finally {
            server.stop(0);
        }
    }

    private static void reply(com.sun.net.httpserver.HttpExchange ex, String json) throws java.io.IOException {
        byte[] out = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(200, out.length);
        ex.getResponseBody().write(out);
        ex.close();
    }
}
