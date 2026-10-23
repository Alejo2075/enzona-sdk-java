package io.github.alejo2075.enzona.model;

/** Sort order of a list, sent as {@code order_filter}. */
public enum SortOrder {
    /** Oldest first. */
    ASC("asc"),
    /** Newest first. */
    DESC("desc");

    private final String wireValue;

    SortOrder(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * Returns the value used on the wire.
     *
     * @return {@code asc} or {@code desc}
     */
    public String wireValue() {
        return wireValue;
    }
}
