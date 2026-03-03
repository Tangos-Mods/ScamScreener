package eu.tango.scamscreener.review;

/**
 * Compact user-facing signal tags attached to reviewed case messages.
 */
public enum ReviewSignalTag {
    TRUST("trust", "Trust"),
    EXTERNAL_PLATFORM("external_platform", "External Platform"),
    PAYMENT("payment", "Payment"),
    URGENCY("urgency", "Urgency"),
    IMPERSONATION("impersonation", "Impersonation"),
    THREAT("threat", "Threat"),
    REPETITION("repetition", "Repetition"),
    CONTACT_BAIT("contact_bait", "Contact Bait");

    private final String id;
    private final String label;

    ReviewSignalTag(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public String id() {
        return id;
    }

    public String label() {
        return label;
    }
}
