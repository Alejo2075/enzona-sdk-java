package io.github.alejo2075.enzona.testsupport;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Loads JSON fixtures from {@code src/test/resources/fixtures}. */
public final class Fixtures {

    private Fixtures() {}

    public static String text(String path) {
        return new String(bytes(path), StandardCharsets.UTF_8);
    }

    public static byte[] bytes(String path) {
        try (InputStream in = Fixtures.class.getResourceAsStream("/fixtures/" + path)) {
            if (in == null) {
                throw new IllegalArgumentException("missing fixture " + path);
            }
            return in.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
