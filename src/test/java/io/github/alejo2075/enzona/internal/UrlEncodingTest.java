package io.github.alejo2075.enzona.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class UrlEncodingTest {

    @Test
    void unreservedCharactersAreKept() {
        assertEquals("aZ09-._~", UrlEncoding.encode("aZ09-._~"));
    }

    @Test
    void slashesAndTraversalCannotChangeThePath() {
        assertEquals("..%2F..%2Fadmin", UrlEncoding.encode("../../admin"));
        assertEquals("a%2Fb%3Fc%23d", UrlEncoding.encode("a/b?c#d"));
    }

    @Test
    void nonAsciiAndSpacesAreEncodedAsUtf8() {
        assertEquals("a%20b", UrlEncoding.encode("a b"));
        assertEquals("%C3%A1", UrlEncoding.encode("á"));
    }
}
