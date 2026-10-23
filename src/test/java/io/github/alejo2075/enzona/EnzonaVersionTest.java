package io.github.alejo2075.enzona;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class EnzonaVersionTest {

    @Test
    void versionIsResolvedFromFilteredResource() {
        assertNotEquals("unknown", EnzonaVersion.get());
        assertFalse(EnzonaVersion.get().contains("${"));
    }
}
