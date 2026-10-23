package io.github.alejo2075.enzona.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void normalizesToScaleTwo() {
        assertEquals("10.00", Money.normalize(new BigDecimal("10"), "total").toPlainString());
        assertEquals("0.10", Money.normalize(new BigDecimal("0.1"), "total").toPlainString());
        assertEquals("0.00", Money.normalize(BigDecimal.ZERO, "total").toPlainString());
    }

    @Test
    void rejectsMoreThanTwoDecimalsAndNamesTheField() {
        IllegalArgumentException e =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> Money.normalize(new BigDecimal("1.234"), "shipping"));
        assertEquals("shipping must have at most two decimals but was 1.234", e.getMessage());
    }

    @Test
    void formatsWithoutScientificNotation() {
        assertEquals("1000000.00", Money.format(new BigDecimal("1E+6")));
    }
}
