package eu.tango.scamscreener.message;

import eu.tango.scamscreener.ScamScreenerRuntime;
import eu.tango.scamscreener.pipeline.data.PipelineDecision;
import eu.tango.scamscreener.pipeline.data.StageResult;
import eu.tango.scamscreener.training.TrainingCaseMappings;
import net.minecraft.text.MutableText;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Restored v1-style education follow-up messages for supported scam categories.
 */
public final class EducationMessages {
    public static final String EXTERNAL_PLATFORM_REDIRECT_ID = "external_platform_redirect";
    public static final String SUSPICIOUS_LINK_ID = "suspicious_link";
    public static final String UPFRONT_PAYMENT_ID = "upfront_payment";
    public static final String ACCOUNT_DATA_REQUEST_ID = "account_data_request";
    public static final String FAKE_MIDDLEMAN_CLAIM_ID = "fake_middleman_claim";
    public static final String PRESSURE_AND_URGENCY_ID = "pressure_and_urgency";
    public static final String TRUST_MANIPULATION_ID = "trust_manipulation";
    public static final String TOO_GOOD_TO_BE_TRUE_ID = "too_good_to_be_true";
    public static final String DISCORD_HANDLE_ID = "discord_handle";
    public static final String FUNNEL_SEQUENCE_PATTERN_ID = "funnel_sequence_pattern";

    private static final List<String> PRIORITY_MESSAGE_IDS = List.of(
        ACCOUNT_DATA_REQUEST_ID,
        UPFRONT_PAYMENT_ID,
        FAKE_MIDDLEMAN_CLAIM_ID,
        SUSPICIOUS_LINK_ID,
        EXTERNAL_PLATFORM_REDIRECT_ID,
        DISCORD_HANDLE_ID,
        FUNNEL_SEQUENCE_PATTERN_ID,
        TRUST_MANIPULATION_ID,
        TOO_GOOD_TO_BE_TRUE_ID,
        PRESSURE_AND_URGENCY_ID
    );
    private static final Set<String> KNOWN_MESSAGE_IDS = knownMessageIdsInternal();

    private EducationMessages() {
    }

    /**
     * Builds the first matching education follow-up for one final decision.
     *
     * @param decision the final pipeline decision
     * @return the follow-up text, when one applies
     */
    public static MutableText followUpFor(PipelineDecision decision) {
        return followUpFor(decision, ScamScreenerRuntime.getInstance().config().output().disabledEducationMessageIds());
    }

    /**
     * Disables one education message id in the runtime config.
     *
     * @param messageId the message id to disable
     * @return {@code true} when the id is known and stored
     */
    public static boolean disable(String messageId) {
        String normalizedId = normalizeId(messageId);
        if (!KNOWN_MESSAGE_IDS.contains(normalizedId)) {
            return false;
        }

        ScamScreenerRuntime runtime = ScamScreenerRuntime.getInstance();
        runtime.config().output().disabledEducationMessageIds().add(normalizedId);
        runtime.saveConfig();
        return true;
    }

    /**
     * Returns the stable set of supported education message ids.
     *
     * @return the supported ids in suggestion order
     */
    public static Set<String> knownMessageIds() {
        return KNOWN_MESSAGE_IDS;
    }

    static MutableText followUpFor(PipelineDecision decision, Collection<String> disabledMessageIds) {
        String messageId = messageIdFor(decision);
        if (messageId == null || isDisabled(messageId, disabledMessageIds)) {
            return null;
        }

        return followUpComponent(messageId);
    }

    static String messageIdFor(PipelineDecision decision) {
        if (decision == null) {
            return null;
        }

        Set<String> matchedMessageIds = new LinkedHashSet<>();
        for (StageResult stageResult : decision.getStageResults()) {
            if (stageResult == null) {
                continue;
            }

            if (stageResult.hasReasonIds()) {
                for (String reasonId : stageResult.getReasonIds()) {
                    collectMessageId(matchedMessageIds, reasonId);
                }
                continue;
            }

            if (stageResult.hasReason()) {
                for (String reasonId : TrainingCaseMappings.reasonIds(stageResult.getStageId(), stageResult.getReason())) {
                    collectMessageId(matchedMessageIds, reasonId);
                }
            }
        }

        if (matchedMessageIds.isEmpty()) {
            for (String reason : decision.getReasons()) {
                for (String reasonId : TrainingCaseMappings.reasonIds("", reason)) {
                    collectMessageId(matchedMessageIds, reasonId);
                }
            }
        }

        for (String priorityMessageId : PRIORITY_MESSAGE_IDS) {
            if (matchedMessageIds.contains(priorityMessageId)) {
                return priorityMessageId;
            }
        }

        return null;
    }

    static MutableText followUpComponent(String messageId) {
        String disableCommand = "/scamscreener edu disable " + messageId;

        return switch (normalizeId(messageId)) {
            case EXTERNAL_PLATFORM_REDIRECT_ID -> ClientMessages.educationExternalPlatformWarning(disableCommand);
            case SUSPICIOUS_LINK_ID -> ClientMessages.educationSuspiciousLinkWarning(disableCommand);
            case UPFRONT_PAYMENT_ID -> ClientMessages.educationUpfrontPaymentWarning(disableCommand);
            case ACCOUNT_DATA_REQUEST_ID -> ClientMessages.educationAccountDataWarning(disableCommand);
            case FAKE_MIDDLEMAN_CLAIM_ID -> ClientMessages.educationFakeMiddlemanWarning(disableCommand);
            case PRESSURE_AND_URGENCY_ID -> ClientMessages.educationUrgencyWarning(disableCommand);
            case TRUST_MANIPULATION_ID -> ClientMessages.educationTrustManipulationWarning(disableCommand);
            case TOO_GOOD_TO_BE_TRUE_ID -> ClientMessages.educationTooGoodToBeTrueWarning(disableCommand);
            case DISCORD_HANDLE_ID -> ClientMessages.educationDiscordHandleWarning(disableCommand);
            case FUNNEL_SEQUENCE_PATTERN_ID -> ClientMessages.educationFunnelSequenceWarning(disableCommand);
            default -> null;
        };
    }

    private static void collectMessageId(Set<String> messageIds, String reasonId) {
        String messageId = messageIdForReasonId(reasonId);
        if (messageId != null) {
            messageIds.add(messageId);
        }
    }

    private static String messageIdForReasonId(String reasonId) {
        return switch (normalizeId(reasonId)) {
            case "rule.account_data", "rule.urgency_account_combo" -> ACCOUNT_DATA_REQUEST_ID;
            case "rule.upfront_payment", "rule.trust_payment_combo" -> UPFRONT_PAYMENT_ID;
            case "rule.middleman_claim", "rule.middleman_proof_combo" -> FAKE_MIDDLEMAN_CLAIM_ID;
            case "rule.suspicious_link", "rule.link_redirect_combo" -> SUSPICIOUS_LINK_ID;
            case "rule.external_platform" -> EXTERNAL_PLATFORM_REDIRECT_ID;
            case "rule.discord_handle" -> DISCORD_HANDLE_ID;
            case "rule.trust" -> TRUST_MANIPULATION_ID;
            case "rule.too_good" -> TOO_GOOD_TO_BE_TRUE_ID;
            case "rule.urgency", "rule.coercion_threat" -> PRESSURE_AND_URGENCY_ID;
            case "funnel.external_after_contact",
                 "funnel.external_after_trust",
                 "funnel.payment_after_external",
                 "funnel.payment_after_trust",
                 "funnel.account_after_external",
                 "funnel.account_after_trust",
                 "funnel.full_chain" -> FUNNEL_SEQUENCE_PATTERN_ID;
            default -> null;
        };
    }

    private static boolean isDisabled(String messageId, Collection<String> disabledMessageIds) {
        if (disabledMessageIds == null || disabledMessageIds.isEmpty()) {
            return false;
        }

        String normalizedId = normalizeId(messageId);
        for (String disabledMessageId : disabledMessageIds) {
            if (normalizeId(disabledMessageId).equals(normalizedId)) {
                return true;
            }
        }

        return false;
    }

    private static String normalizeId(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static Set<String> knownMessageIdsInternal() {
        LinkedHashSet<String> messageIds = new LinkedHashSet<>();
        messageIds.add(EXTERNAL_PLATFORM_REDIRECT_ID);
        messageIds.add(SUSPICIOUS_LINK_ID);
        messageIds.add(UPFRONT_PAYMENT_ID);
        messageIds.add(ACCOUNT_DATA_REQUEST_ID);
        messageIds.add(FAKE_MIDDLEMAN_CLAIM_ID);
        messageIds.add(PRESSURE_AND_URGENCY_ID);
        messageIds.add(TRUST_MANIPULATION_ID);
        messageIds.add(TOO_GOOD_TO_BE_TRUE_ID);
        messageIds.add(DISCORD_HANDLE_ID);
        messageIds.add(FUNNEL_SEQUENCE_PATTERN_ID);
        return Collections.unmodifiableSet(messageIds);
    }
}
