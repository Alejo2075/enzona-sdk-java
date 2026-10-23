package io.github.alejo2075.enzona;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/** Exposes the version of this SDK, used for the {@code User-Agent} header. */
public final class EnzonaVersion {

    private static final String VERSION = load();

    private EnzonaVersion() {}

    /**
     * Returns the SDK version as defined in the published artifact.
     *
     * @return the version string, or {@code "unknown"} if it cannot be determined
     */
    public static String get() {
        return VERSION;
    }

    private static String load() {
        try (InputStream in = EnzonaVersion.class.getResourceAsStream("/enzona-sdk.properties")) {
            if (in == null) {
                return "unknown";
            }
            Properties props = new Properties();
            props.load(in);
            return props.getProperty("version", "unknown");
        } catch (IOException e) {
            return "unknown";
        }
    }
}
