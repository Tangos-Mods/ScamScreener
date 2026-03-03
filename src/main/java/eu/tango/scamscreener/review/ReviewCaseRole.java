package eu.tango.scamscreener.review;

/**
 * Case-level role assigned to one message during review.
 */
public enum ReviewCaseRole {
    EXCLUDED("Excluded", "[ ]", 0xB8B8B8),
    CONTEXT("Context", "[C]", 0x55FFFF),
    SIGNAL("Signal", "[S]", 0xFFB366);

    private final String label;
    private final String marker;
    private final int color;

    ReviewCaseRole(String label, String marker, int color) {
        this.label = label;
        this.marker = marker;
        this.color = color;
    }

    public String label() {
        return label;
    }

    public String marker() {
        return marker;
    }

    public int color() {
        return color;
    }
}
