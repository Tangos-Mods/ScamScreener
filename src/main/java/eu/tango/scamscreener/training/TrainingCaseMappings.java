package eu.tango.scamscreener.training;

import eu.tango.scamscreener.pipeline.data.StageResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Central stable-id mapping for training-case exports and advanced review annotations.
 */
public final class TrainingCaseMappings {
    private static final String STAGE_UNKNOWN = "stage.unknown";
    private static final Pattern SIMILARITY_REASON_PATTERN = Pattern.compile("^(.+?) matched \".*\" at \\d+\\.\\d{2}$");
    private static final Pattern SPLIT_PATTERN = Pattern.compile(";\\s*");

    private TrainingCaseMappings() {
    }

    /**
     * Returns the stable stage id for one runtime stage name.
     *
     * @param stageName the runtime stage name or legacy display token
     * @return the normalized stable stage id
     */
    public static String stageId(String stageName) {
        if (stageName == null || stageName.isBlank()) {
            return STAGE_UNKNOWN;
        }

        String normalized = stageName.trim();
        if (normalized.startsWith("stage.")) {
            return normalized.toLowerCase(Locale.ROOT);
        }

        return switch (normalized.toLowerCase(Locale.ROOT)) {
            case "mutestage", "mute stage" -> "stage.mute";
            case "playerliststage", "player list", "player list stage" -> "stage.player_list";
            case "rulestage", "rule stage" -> "stage.rule";
            case "levenshteinstage", "similarity stage" -> "stage.similarity";
            case "behaviorstage", "behavior stage" -> "stage.behavior";
            case "trendstage", "trend stage" -> "stage.trend";
            case "funnelstage", "funnel stage" -> "stage.funnel";
            case "contextstage", "context stage" -> "stage.context";
            default -> STAGE_UNKNOWN;
        };
    }

    /**
     * Returns a user-facing stage label for one stable stage id.
     *
     * @param stageId the stable stage id
     * @return the matching display label
     */
    public static String stageLabel(String stageId) {
        return switch (stageId(stageId)) {
            case "stage.mute" -> "Mute Stage";
            case "stage.player_list" -> "Player List";
            case "stage.rule" -> "Rule Stage";
            case "stage.similarity" -> "Similarity Stage";
            case "stage.behavior" -> "Behavior Stage";
            case "stage.trend" -> "Trend Stage";
            case "stage.funnel" -> "Funnel Stage";
            case "stage.context" -> "Context Stage";
            default -> "Unknown Stage";
        };
    }

    /**
     * Builds stable advanced-mapping options from captured stage results.
     *
     * @param stageResults the captured stage results
     * @return the deduplicated mapping options in encounter order
     */
    public static List<MappingOption> optionsForStageResults(List<StageResult> stageResults) {
        if (stageResults == null || stageResults.isEmpty()) {
            return List.of();
        }

        Map<String, MappingOption> options = new LinkedHashMap<>();
        for (StageResult stageResult : stageResults) {
            if (stageResult == null || !stageResult.hasReason()) {
                continue;
            }

            String stageId = stageResult.getStageId();
            for (String reasonPart : splitReasonParts(stageResult.getReason())) {
                MappingOption option = optionFor(stageId, reasonPart);
                if (option != null) {
                    options.putIfAbsent(option.id(), option);
                }
            }
        }

        return List.copyOf(options.values());
    }

    /**
     * Returns the stable reason ids contained in one captured stage result.
     *
     * @param stageResult the stage result to inspect
     * @return the ordered stable reason ids
     */
    public static List<String> reasonIdsFor(StageResult stageResult) {
        if (stageResult == null) {
            return List.of();
        }
        if (stageResult.hasReasonIds()) {
            return List.copyOf(stageResult.getReasonIds());
        }
        if (!stageResult.hasReason()) {
            return List.of();
        }

        return reasonIds(stageResult.getStageId(), stageResult.getReason());
    }

    /**
     * Returns the stable reason ids derived from one stage and raw reason text.
     *
     * @param stageNameOrId the runtime stage name or stable stage id
     * @param reason the raw reason text
     * @return the ordered stable reason ids
     */
    public static List<String> reasonIds(String stageNameOrId, String reason) {
        if (reason == null || reason.isBlank()) {
            return List.of();
        }

        List<String> reasonIds = new ArrayList<>();
        for (String reasonPart : splitReasonParts(reason)) {
            MappingOption option = optionFor(stageNameOrId, reasonPart);
            if (option != null && !option.reasonId().isBlank()) {
                reasonIds.add(option.reasonId());
            }
        }

        return List.copyOf(reasonIds);
    }

    /**
     * Normalizes one stored advanced-mapping selection into a stable selection id.
     *
     * @param rawValue the raw persisted selection value
     * @return the stable selection id
     */
    public static String normalizeSelectionId(String rawValue) {
        return parseSelection(rawValue).id();
    }

    /**
     * Parses one stable or legacy advanced-mapping selection.
     *
     * @param rawValue the raw stored selection
     * @return the normalized parsed selection
     */
    public static ParsedSelection parseSelection(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return ParsedSelection.empty();
        }

        String normalized = rawValue.trim();
        int separatorIndex = normalized.indexOf("::");
        if (separatorIndex > 0 && separatorIndex < normalized.length() - 2) {
            String left = stageId(normalized.substring(0, separatorIndex));
            String right = normalizeReasonId(normalized.substring(separatorIndex + 2));
            return new ParsedSelection(composeSelectionId(left, right), left, right, normalized);
        }

        String stageToken = "";
        String reasonToken = normalized;
        int labelSeparator = normalized.indexOf(" - ");
        if (labelSeparator > 0 && labelSeparator < normalized.length() - 3) {
            stageToken = normalized.substring(0, labelSeparator).trim();
            reasonToken = normalized.substring(labelSeparator + 3).trim();
        }

        MappingOption option = optionFor(stageToken, reasonToken);
        if (option != null) {
            return new ParsedSelection(option.id(), option.stageId(), option.reasonId(), option.label());
        }

        String fallbackStageId = stageToken.isBlank() ? inferStageId(reasonToken) : stageId(stageToken);
        String fallbackReasonId = fallbackReasonId(reasonToken);
        return new ParsedSelection(composeSelectionId(fallbackStageId, fallbackReasonId), fallbackStageId, fallbackReasonId, normalized);
    }

    private static MappingOption optionFor(String stageNameOrId, String reasonPart) {
        if (reasonPart == null || reasonPart.isBlank()) {
            return null;
        }

        String normalizedStageId = stageId(stageNameOrId);
        MappedReason mappedReason = mapReason(normalizedStageId, reasonPart.trim());
        if (mappedReason == null) {
            return null;
        }

        String optionStageId = mappedReason.stageId().isBlank() ? STAGE_UNKNOWN : mappedReason.stageId();
        String label = mappedReason.reasonText();
        String stageLabel = stageLabel(optionStageId);
        if (!stageLabel.isBlank() && !label.isBlank()) {
            label = stageLabel + " - " + label;
        }
        return new MappingOption(composeSelectionId(optionStageId, mappedReason.reasonId()), optionStageId, mappedReason.reasonId(), label);
    }

    private static MappedReason mapReason(String normalizedStageId, String reasonText) {
        if (reasonText == null || reasonText.isBlank()) {
            return null;
        }

        return switch (normalizedStageId) {
            case "stage.mute" -> mapMuteReason(reasonText);
            case "stage.player_list" -> mapPlayerListReason(reasonText);
            case "stage.rule" -> mapRuleReason(reasonText);
            case "stage.similarity" -> mapSimilarityReason(reasonText);
            case "stage.behavior" -> mapBehaviorReason(reasonText);
            case "stage.trend" -> mapTrendReason(reasonText);
            case "stage.funnel" -> mapFunnelReason(reasonText);
            case "stage.context" -> mapContextReason(reasonText);
            default -> inferMappedReason(reasonText);
        };
    }

    private static MappedReason inferMappedReason(String reasonText) {
        MappedReason mappedReason = mapMuteReason(reasonText);
        if (mappedReason != null) {
            return mappedReason;
        }
        mappedReason = mapPlayerListReason(reasonText);
        if (mappedReason != null) {
            return mappedReason;
        }
        mappedReason = mapRuleReason(reasonText);
        if (mappedReason != null) {
            return mappedReason;
        }
        mappedReason = mapBehaviorReason(reasonText);
        if (mappedReason != null) {
            return mappedReason;
        }
        mappedReason = mapTrendReason(reasonText);
        if (mappedReason != null) {
            return mappedReason;
        }
        mappedReason = mapFunnelReason(reasonText);
        if (mappedReason != null) {
            return mappedReason;
        }
        mappedReason = mapContextReason(reasonText);
        if (mappedReason != null) {
            return mappedReason;
        }
        return mapSimilarityReason(reasonText);
    }

    private static MappedReason mapMuteReason(String reasonText) {
        return switch (reasonText) {
            case "MUTE_SYSTEM_BYPASS" -> new MappedReason("stage.mute", "mute.system_bypass", reasonText);
            case "MUTE_NOISE_BYPASS" -> new MappedReason("stage.mute", "mute.noise_bypass", reasonText);
            case "MUTE_DUPLICATE_BYPASS" -> new MappedReason("stage.mute", "mute.duplicate_bypass", reasonText);
            default -> null;
        };
    }

    private static MappedReason mapPlayerListReason(String reasonText) {
        return switch (reasonText) {
            case "WHITELIST_MATCH" -> new MappedReason("stage.player_list", "player_list.whitelist_match", reasonText);
            case "BLACKLIST_MATCH" -> new MappedReason("stage.player_list", "player_list.blacklist_match", reasonText);
            default -> null;
        };
    }

    private static MappedReason mapRuleReason(String reasonText) {
        return switch (ruleReasonKey(reasonText)) {
            case "suspicious link" -> new MappedReason("stage.rule", "rule.suspicious_link", reasonText);
            case "external platform push" -> new MappedReason("stage.rule", "rule.external_platform", reasonText);
            case "upfront payment wording" -> new MappedReason("stage.rule", "rule.upfront_payment", reasonText);
            case "sensitive account wording" -> new MappedReason("stage.rule", "rule.account_data", reasonText);
            case "too-good-to-be-true wording" -> new MappedReason("stage.rule", "rule.too_good", reasonText);
            case "coercion or extortion wording" -> new MappedReason("stage.rule", "rule.coercion_threat", reasonText);
            case "middleman claim" -> new MappedReason("stage.rule", "rule.middleman_claim", reasonText);
            case "proof or vouch bait" -> new MappedReason("stage.rule", "rule.proof_bait", reasonText);
            case "urgency wording" -> new MappedReason("stage.rule", "rule.urgency", reasonText);
            case "trust manipulation wording" -> new MappedReason("stage.rule", "rule.trust", reasonText);
            case "discord handle with platform mention" -> new MappedReason("stage.rule", "rule.discord_handle", reasonText);
            case "link plus off-platform redirect" -> new MappedReason("stage.rule", "rule.link_redirect_combo", reasonText);
            case "trust framing plus upfront payment" -> new MappedReason("stage.rule", "rule.trust_payment_combo", reasonText);
            case "urgency paired with sensitive account request" -> new MappedReason("stage.rule", "rule.urgency_account_combo", reasonText);
            case "middleman claim plus proof bait" -> new MappedReason("stage.rule", "rule.middleman_proof_combo", reasonText);
            default -> null;
        };
    }

    private static MappedReason mapBehaviorReason(String reasonText) {
        if (reasonText.startsWith("Repeated contact message x")) {
            return new MappedReason("stage.behavior", "behavior.repeated_message", reasonText);
        }
        if (reasonText.startsWith("Burst contact:")) {
            return new MappedReason("stage.behavior", "behavior.burst_contact", reasonText);
        }
        if ("Behavior combo: repeated burst contact".equals(reasonText)) {
            return new MappedReason("stage.behavior", "behavior.combo_repeated_burst", reasonText);
        }

        return null;
    }

    private static MappedReason mapTrendReason(String reasonText) {
        if (reasonText.startsWith("Trend wave:")) {
            return new MappedReason("stage.trend", "trend.multi_sender_wave", reasonText);
        }
        if (reasonText.startsWith("Trend escalation:")) {
            return new MappedReason("stage.trend", "trend.wave_escalation", reasonText);
        }
        if (reasonText.startsWith("Cross-sender repeat:")) {
            return new MappedReason("stage.trend", "trend.single_cross_sender_repeat", reasonText);
        }

        return null;
    }

    private static MappedReason mapFunnelReason(String reasonText) {
        return switch (reasonText) {
            case "Funnel step: external platform after prior contact" ->
                new MappedReason("stage.funnel", "funnel.external_after_contact", reasonText);
            case "Funnel step: external platform after trust framing" ->
                new MappedReason("stage.funnel", "funnel.external_after_trust", reasonText);
            case "Funnel step: payment request after external platform" ->
                new MappedReason("stage.funnel", "funnel.payment_after_external", reasonText);
            case "Funnel step: payment request after trust framing" ->
                new MappedReason("stage.funnel", "funnel.payment_after_trust", reasonText);
            case "Funnel step: account request after external platform" ->
                new MappedReason("stage.funnel", "funnel.account_after_external", reasonText);
            case "Funnel step: account request after trust framing" ->
                new MappedReason("stage.funnel", "funnel.account_after_trust", reasonText);
            case "Funnel chain: trust -> external platform -> request" ->
                new MappedReason("stage.funnel", "funnel.full_chain", reasonText);
            default -> null;
        };
    }

    private static MappedReason mapSimilarityReason(String reasonText) {
        Matcher matcher = SIMILARITY_REASON_PATTERN.matcher(reasonText);
        if (!matcher.matches()) {
            return null;
        }

        String category = normalizeIdSuffix(matcher.group(1));
        if (category.isBlank()) {
            category = "match";
        }
        return new MappedReason("stage.similarity", "similarity." + category, reasonText);
    }

    private static MappedReason mapContextReason(String reasonText) {
        if (reasonText.startsWith("Context signal blend:")) {
            return new MappedReason("stage.context", "context.signal_blend", reasonText);
        }
        if (reasonText.startsWith("Context escalation:")) {
            return new MappedReason("stage.context", "context.escalation", reasonText);
        }

        return null;
    }

    private static String inferStageId(String reasonText) {
        MappedReason mappedReason = inferMappedReason(reasonText);
        return mappedReason == null ? STAGE_UNKNOWN : mappedReason.stageId();
    }

    private static String fallbackReasonId(String reasonText) {
        String normalized = normalizeIdSuffix(reasonText);
        if (normalized.isBlank()) {
            normalized = "unknown";
        }

        return "hint." + normalized;
    }

    private static String ruleReasonKey(String reasonText) {
        int separatorIndex = reasonText.indexOf(':');
        String prefix = separatorIndex < 0 ? reasonText : reasonText.substring(0, separatorIndex);
        return prefix.trim().toLowerCase(Locale.ROOT);
    }

    private static List<String> splitReasonParts(String reason) {
        if (reason == null || reason.isBlank()) {
            return List.of();
        }

        String[] rawParts = SPLIT_PATTERN.split(reason.trim());
        List<String> parts = new ArrayList<>(rawParts.length);
        for (String rawPart : rawParts) {
            if (rawPart != null) {
                String normalizedPart = rawPart.trim();
                if (!normalizedPart.isBlank()) {
                    parts.add(normalizedPart);
                }
            }
        }

        return List.copyOf(parts);
    }

    private static String composeSelectionId(String stageId, String reasonId) {
        return stageId(stageId) + "::" + normalizeReasonId(reasonId);
    }

    private static String normalizeReasonId(String reasonId) {
        if (reasonId == null || reasonId.isBlank()) {
            return "hint.unknown";
        }

        return reasonId.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeIdSuffix(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        StringBuilder builder = new StringBuilder(value.length());
        boolean lastWasSeparator = false;
        for (int index = 0; index < value.length(); index++) {
            char current = Character.toLowerCase(value.charAt(index));
            if ((current >= 'a' && current <= 'z') || (current >= '0' && current <= '9')) {
                builder.append(current);
                lastWasSeparator = false;
                continue;
            }
            if (!lastWasSeparator) {
                builder.append('_');
                lastWasSeparator = true;
            }
        }

        String normalized = builder.toString();
        while (normalized.startsWith("_")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("_")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    /**
     * One selectable stable mapping option exposed in review UI and exports.
     *
     * @param id stable combined selection id
     * @param stageId stable stage id
     * @param reasonId stable reason id
     * @param label user-facing display label
     */
    public record MappingOption(String id, String stageId, String reasonId, String label) {
        public MappingOption {
            id = id == null ? "" : id.trim();
            stageId = TrainingCaseMappings.stageId(stageId);
            reasonId = TrainingCaseMappings.normalizeReasonId(reasonId);
            label = label == null ? "" : label.trim();
        }
    }

    /**
     * One normalized parsed selection.
     *
     * @param id stable combined selection id
     * @param stageId stable stage id
     * @param reasonId stable reason id
     * @param label best-effort label carried with the selection
     */
    public record ParsedSelection(String id, String stageId, String reasonId, String label) {
        public ParsedSelection {
            id = id == null ? "" : id.trim();
            stageId = TrainingCaseMappings.stageId(stageId);
            reasonId = TrainingCaseMappings.normalizeReasonId(reasonId);
            label = label == null ? "" : label.trim();
        }

        private static ParsedSelection empty() {
            return new ParsedSelection("", STAGE_UNKNOWN, "hint.unknown", "");
        }
    }

    private record MappedReason(String stageId, String reasonId, String reasonText) {
    }
}
