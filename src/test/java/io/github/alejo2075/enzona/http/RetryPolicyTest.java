package io.github.alejo2075.enzona.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class RetryPolicyTest {

    @Test
    void disabledNeverRetries() {
        RetryPolicy p = RetryPolicy.disabled();
        assertFalse(p.enabled());
        assertEquals(1, p.maxAttempts());
        assertNull(p.delayBeforeRetry(1, null));
        assertEquals("RetryPolicy[disabled]", p.toString());
    }

    @Test
    void validatesItsArguments() {
        Duration d = Duration.ofMillis(10);
        assertThrows(IllegalArgumentException.class, () -> RetryPolicy.exponentialBackoff(1, d, d));
        assertThrows(
                IllegalArgumentException.class, () -> RetryPolicy.exponentialBackoff(11, d, d));
        assertThrows(
                IllegalArgumentException.class,
                () -> RetryPolicy.exponentialBackoff(3, Duration.ofMillis(-1), d));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        RetryPolicy.exponentialBackoff(
                                3, Duration.ofSeconds(2), Duration.ofSeconds(1)));
    }

    @Test
    void backoffGrowsIsCappedAndStopsAtTheLastAttempt() {
        RetryPolicy p =
                RetryPolicy.exponentialBackoff(4, Duration.ofMillis(100), Duration.ofMillis(300));
        assertTrue(p.enabled());
        assertEquals(4, p.maxAttempts());

        for (int i = 0; i < 50; i++) {
            long first = p.delayBeforeRetry(1, null).toMillis();
            long second = p.delayBeforeRetry(2, null).toMillis();
            long third = p.delayBeforeRetry(3, null).toMillis();
            assertTrue(first >= 50 && first <= 100, "first=" + first);
            assertTrue(second >= 100 && second <= 200, "second=" + second);
            assertTrue(third >= 150 && third <= 300, "third=" + third);
        }
        assertNull(p.delayBeforeRetry(4, null));
        assertTrue(p.toString().contains("maxAttempts=4"));
    }

    @Test
    void retryAfterIsHonouredUnlessItExceedsTheMaximum() {
        RetryPolicy p =
                RetryPolicy.exponentialBackoff(3, Duration.ofMillis(10), Duration.ofSeconds(2));
        assertEquals(Duration.ofSeconds(1), p.delayBeforeRetry(1, Duration.ofSeconds(1)));
        assertNull(p.delayBeforeRetry(1, Duration.ofSeconds(3)));
    }
}
