package eu.tango.scamscreener.review;

/**
 * User-facing verdict state for one review entry.
 */
public enum ReviewVerdict {
    PENDING,
    RISK,
    SAFE,
    IGNORED
}
