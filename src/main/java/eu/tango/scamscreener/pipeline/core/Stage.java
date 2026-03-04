package eu.tango.scamscreener.pipeline.core;

import eu.tango.scamscreener.pipeline.data.ChatEvent;
import eu.tango.scamscreener.pipeline.data.StageResult;

import java.util.List;

/**
 * Shared base type for one step in the chat screening pipeline.
 *
 * <p>Concrete stages only implement {@link #evaluate(ChatEvent)}. This base class
 * centralizes the common execution flow and helper methods used to build
 * consistent {@link StageResult} values.
 */
public abstract class Stage {
    /**
     * Evaluates a chat event after applying shared guard logic.
     *
     * @param chatEvent the chat event received from the client
     * @return the result produced by this stage
     */
    public final StageResult apply(ChatEvent chatEvent) {
        if (chatEvent == null || chatEvent.getRawMessage().isBlank()) {
            // Empty input should never force individual stages to duplicate guard code.
            return pass();
        }

        return evaluate(chatEvent);
    }

    /**
     * Evaluates a raw chat message by wrapping it in a minimal chat event.
     *
     * @param chatMessage the raw chat message text received from the client
     * @return the result produced by this stage
     */
    public final StageResult apply(String chatMessage) {
        // Keep a string-only entry point for tests and transitional callers.
        return apply(ChatEvent.messageOnly(chatMessage));
    }

    /**
     * Contains the actual stage-specific logic.
     *
     * @param chatEvent the chat event received from the client
     * @return the result produced by the concrete stage
     */
    protected abstract StageResult evaluate(ChatEvent chatEvent);

    /**
     * Returns a human-readable stage name for logging and debugging.
     *
     * @return the default name of the implementing stage
     */
    public String name() {
        // Use the implementing class name so stages do not need to declare a name manually.
        return getClass().getSimpleName();
    }

    /**
     * Returns a neutral result and continues to the next stage.
     *
     * @return a pass result for this stage
     */
    protected final StageResult pass() {
        // Use the current stage name automatically so callers do not repeat it.
        return StageResult.pass(name());
    }

    /**
     * Returns a mute result and skips the remaining stages.
     *
     * @param reason an optional explanation for the mute decision
     * @return a mute result for this stage
     */
    protected final StageResult mute(String reason) {
        // Mute is a dedicated terminal outcome for suppression-style stages.
        return StageResult.mute(name(), reason);
    }

    /**
     * Returns a mute result with one explicit stable reason id.
     *
     * @param reason an optional explanation for the mute decision
     * @param reasonId the stable reason id
     * @return a mute result for this stage
     */
    protected final StageResult mute(String reason, String reasonId) {
        return StageResult.of(name(), name(), Decision.MUTE, 0, reason, List.of(reasonId));
    }

    /**
     * Returns a whitelist result and skips the remaining stages.
     *
     * @param reason an optional explanation for the whitelist decision
     * @return a whitelist result for this stage
     */
    protected final StageResult whitelist(String reason) {
        // Whitelist is a dedicated terminal outcome for trusted-message bypasses.
        return StageResult.whitelist(name(), reason);
    }

    /**
     * Returns a whitelist result with one explicit stable reason id.
     *
     * @param reason an optional explanation for the whitelist decision
     * @param reasonId the stable reason id
     * @return a whitelist result for this stage
     */
    protected final StageResult whitelist(String reason, String reasonId) {
        return StageResult.of(name(), name(), Decision.WHITELIST, 0, reason, List.of(reasonId));
    }

    /**
     * Returns a blacklist result and skips the remaining stages.
     *
     * @param reason an optional explanation for the blacklist decision
     * @return a blacklist result for this stage
     */
    protected final StageResult blacklist(String reason) {
        // Blacklist is a dedicated terminal outcome for explicit deny-list matches.
        return StageResult.blacklist(name(), reason);
    }

    /**
     * Returns a blacklist result with one explicit stable reason id.
     *
     * @param reason an optional explanation for the blacklist decision
     * @param reasonId the stable reason id
     * @return a blacklist result for this stage
     */
    protected final StageResult blacklist(String reason, String reasonId) {
        return StageResult.of(name(), name(), Decision.BLACKLIST, 0, reason, List.of(reasonId));
    }

    /**
     * Returns an allow result and skips the remaining stages.
     *
     * @param reason an optional explanation for the allow decision
     * @return an allow result for this stage
     */
    protected final StageResult allow(String reason) {
        // Allow is the common "safe to stop here" outcome.
        return StageResult.allow(name(), reason);
    }

    /**
     * Returns an allow result with one explicit stable reason id.
     *
     * @param reason an optional explanation for the allow decision
     * @param reasonId the stable reason id
     * @return an allow result for this stage
     */
    protected final StageResult allow(String reason, String reasonId) {
        return StageResult.of(name(), name(), Decision.ALLOW, 0, reason, List.of(reasonId));
    }

    /**
     * Returns a block result and stops the pipeline immediately.
     *
     * @param scoreDelta the score contribution of this stage
     * @param reason an optional explanation for the block decision
     * @return a block result for this stage
     */
    protected final StageResult block(int scoreDelta, String reason) {
        // Blocking stays explicit while still recording the stage score.
        return StageResult.block(name(), scoreDelta, reason);
    }

    /**
     * Returns a block result with explicit stable reason ids.
     *
     * @param scoreDelta the score contribution of this stage
     * @param reason an optional explanation for the block decision
     * @param reasonIds the stable reason ids
     * @return a block result for this stage
     */
    protected final StageResult block(int scoreDelta, String reason, List<String> reasonIds) {
        return StageResult.of(name(), name(), Decision.BLOCK, scoreDelta, reason, reasonIds);
    }

    /**
     * Returns a score-only result and continues to the next stage.
     *
     * @param scoreDelta the score contribution of this stage
     * @param reason an optional explanation for the score change
     * @return a scoring result for this stage
     */
    protected final StageResult score(int scoreDelta, String reason) {
        // This is the standard path for suspicious-but-not-final findings.
        return StageResult.score(name(), scoreDelta, reason);
    }

    /**
     * Returns a scoring result with explicit stable reason ids.
     *
     * @param scoreDelta the score contribution of this stage
     * @param reasonIds the stable reason ids
     * @param reason an optional explanation for the score change
     * @return a scoring result for this stage
     */
    protected final StageResult score(int scoreDelta, List<String> reasonIds, String reason) {
        return StageResult.of(name(), name(), Decision.PASS, scoreDelta, reason, reasonIds);
    }

    /**
     * Returns a custom result when a stage needs full control over its outcome.
     *
     * @param decision the pipeline decision for this stage
     * @param scoreDelta the score contribution of this stage
     * @param reason an optional reason for the outcome
     * @return a fully customized result for this stage
     */
    protected final StageResult result(Decision decision, int scoreDelta, String reason) {
        // Keep custom result creation centralized so the stage API stays consistent.
        return StageResult.of(name(), decision, scoreDelta, reason);
    }

    /**
     * Returns a custom result with explicit stable reason ids.
     *
     * @param decision the pipeline decision for this stage
     * @param scoreDelta the score contribution of this stage
     * @param reason an optional reason for the outcome
     * @param reasonIds the stable reason ids
     * @return a fully customized result for this stage
     */
    protected final StageResult result(Decision decision, int scoreDelta, String reason, List<String> reasonIds) {
        return StageResult.of(name(), name(), decision, scoreDelta, reason, reasonIds);
    }

    /**
     * The minimal outcomes a stage can produce.
     */
    public enum Decision {
        /**
         * No final verdict was reached, so the pipeline should continue.
         */
        PASS,
        /**
         * The message should be suppressed immediately by the mute logic.
         */
        MUTE,
        /**
         * The message is trusted and should bypass the remaining stages.
         */
        WHITELIST,
        /**
         * The message matched an explicit blacklist entry.
         */
        BLACKLIST,
        /**
         * The message is considered safe and later stages can be skipped.
         */
        ALLOW,
        /**
         * The message is considered unsafe and should be rejected immediately.
         */
        BLOCK
    }
}
