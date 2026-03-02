package eu.tango.scamscreener.pipeline.rule;

/**
 * Normalized sender-local funnel rule settings.
 *
 * @param externalAfterContactScore score for external platform after contact
 * @param paymentAfterExternalScore score for payment after external platform
 * @param paymentAfterTrustScore score for payment after trust framing
 * @param accountAfterExternalScore score for account request after external platform
 * @param accountAfterTrustScore score for account request after trust framing
 * @param fullChainBonusScore bonus for full trust-to-request chains
 * @param trustBridgeBonusMinimum minimum external-after-trust bridge bonus
 * @param trustBridgeBonusDivisor divisor for bridge bonus scaling
 * @param windowMs sender-local funnel history window in milliseconds
 * @param maxHistory maximum stored funnel steps per sender
 */
public record FunnelRules(
    int externalAfterContactScore,
    int paymentAfterExternalScore,
    int paymentAfterTrustScore,
    int accountAfterExternalScore,
    int accountAfterTrustScore,
    int fullChainBonusScore,
    int trustBridgeBonusMinimum,
    int trustBridgeBonusDivisor,
    long windowMs,
    int maxHistory
) {
    /**
     * Returns the bridge bonus for trust leading into an external-platform step.
     *
     * @return the non-negative bridge bonus
     */
    public int trustBridgeBonus() {
        int safeDivisor = Math.max(1, trustBridgeBonusDivisor);
        return Math.max(trustBridgeBonusMinimum, externalAfterContactScore / safeDivisor);
    }

    /**
     * Returns the external-after-contact reason text.
     *
     * @return the reason text
     */
    public String externalAfterContactReason() {
        return "Funnel step: external platform after prior contact";
    }

    /**
     * Returns the external-after-trust reason text.
     *
     * @return the reason text
     */
    public String externalAfterTrustReason() {
        return "Funnel step: external platform after trust framing";
    }

    /**
     * Returns the payment-after-external reason text.
     *
     * @return the reason text
     */
    public String paymentAfterExternalReason() {
        return "Funnel step: payment request after external platform";
    }

    /**
     * Returns the payment-after-trust reason text.
     *
     * @return the reason text
     */
    public String paymentAfterTrustReason() {
        return "Funnel step: payment request after trust framing";
    }

    /**
     * Returns the account-after-external reason text.
     *
     * @return the reason text
     */
    public String accountAfterExternalReason() {
        return "Funnel step: account request after external platform";
    }

    /**
     * Returns the account-after-trust reason text.
     *
     * @return the reason text
     */
    public String accountAfterTrustReason() {
        return "Funnel step: account request after trust framing";
    }

    /**
     * Returns the full-chain reason text.
     *
     * @return the reason text
     */
    public String fullChainReason() {
        return "Funnel chain: trust -> external platform -> request";
    }
}
