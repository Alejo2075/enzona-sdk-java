package io.github.alejo2075.enzona;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.alejo2075.enzona.model.Payment;
import io.github.alejo2075.enzona.testsupport.Clients;
import io.github.alejo2075.enzona.testsupport.FakeTransport;
import io.github.alejo2075.enzona.testsupport.Fixtures;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Applications that use Jackson 2 (Spring Boot, for example) can use the SDK, which uses Jackson 3.
 */
class Jackson2CoexistenceTest {

    @Test
    void bothJacksonVersionsAreOnTheClasspathAndAreDifferentClasses() throws Exception {
        Class<?> jackson2 = Class.forName("com.fasterxml.jackson.databind.ObjectMapper");
        Class<?> jackson3 = Class.forName("tools.jackson.databind.ObjectMapper");

        assertNotSame(jackson2, jackson3);
    }

    @Test
    void theSdkStillParsesAndWritesJsonWhileJackson2IsPresent() throws Exception {
        FakeTransport t =
                Clients.scripted(
                        FakeTransport.json(200, Fixtures.text("payments/payment-details.json")));

        Payment p = Clients.client(t).payments().get("9f0b2c0c-4f9e-4d6a-9d3c-1a2b3c4d5e6f");

        assertEquals(new BigDecimal("112"), p.amount().total());
        assertEquals("Ana", p.name());
        assertTrue(p.link("self").isPresent());
    }

    @Test
    void theApplicationsOwnJackson2MapperIsUnaffectedByTheSdk() throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper mapper =
                new com.fasterxml.jackson.databind.ObjectMapper();

        Map<?, ?> parsed = mapper.readValue("{\"total\":10.50}", Map.class);

        assertEquals(10.5d, parsed.get("total"));
        // The SDK was used before, in the same JVM, and changed nothing global.
        Clients.client(
                        Clients.scripted(
                                FakeTransport.json(
                                        200, Fixtures.text("payments/payment-details.json"))))
                .payments()
                .get("x");
        assertEquals("{\"a\":1}", mapper.writeValueAsString(Map.of("a", 1)));
    }
}
