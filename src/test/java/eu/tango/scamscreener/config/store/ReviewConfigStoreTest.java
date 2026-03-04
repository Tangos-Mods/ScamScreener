package eu.tango.scamscreener.config.store;

import eu.tango.scamscreener.config.data.ReviewConfig;
import eu.tango.scamscreener.pipeline.data.StageResult;
import eu.tango.scamscreener.review.ReviewCaseMessage;
import eu.tango.scamscreener.review.ReviewCaseRole;
import eu.tango.scamscreener.review.ReviewEntry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ReviewConfigStoreTest {
    @Test
    void toStoredEntryScrubsPersistentIdentifiers() {
        UUID senderUuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        ReviewEntry entry = new ReviewEntry(
            "review-1",
            senderUuid,
            "Alpha",
            "[12] Alpha: trade with Alpha and 123e4567-e89b-12d3-a456-426614174000",
            42,
            "RuleStage",
            1_000L,
            List.of("reported by Alpha", "seen with 123e4567-e89b-12d3-a456-426614174000"),
            List.of(StageResult.score("RuleStage", 42, "External platform push: \"Alpha\"")),
            List.of(new ReviewCaseMessage(
                0,
                "other",
                "player",
                "Alpha can carry 123e4567-e89b-12d3-a456-426614174000",
                true,
                ReviewCaseRole.SIGNAL,
                List.of("trust"),
                List.of("RuleStage - External platform push: \"Alpha\"")
            ))
        );

        ReviewConfig.ReviewConfigEntry storedEntry = ReviewConfigStore.toStoredEntry(entry);

        assertEquals("", storedEntry.getSenderUuid());
        assertEquals("", storedEntry.getSenderName());
        assertEquals("trade with Alpha and <uuid>", storedEntry.getMessage());
        assertEquals("reported by Alpha", storedEntry.getReasons().getFirst());
        assertEquals("seen with <uuid>", storedEntry.getReasons().get(1));
        assertEquals("stage.rule", storedEntry.getStageResults().getFirst().getStageId());
        assertEquals("External platform push: \"Alpha\"", storedEntry.getStageResults().getFirst().getReason());
        assertEquals(List.of("rule.external_platform"), storedEntry.getStageResults().getFirst().getReasonIds());
        assertEquals("Alpha can carry <uuid>", storedEntry.getCaseMessages().getFirst().getCleanText());
        assertEquals("stage.rule::rule.external_platform", storedEntry.getCaseMessages().getFirst().getAdvancedRuleSelections().getFirst());
    }

    @Test
    void toRuntimeEntryDropsLegacySenderFieldsAndLoadsSanitizedData() {
        ReviewConfig.ReviewConfigEntry storedEntry = new ReviewConfig.ReviewConfigEntry();
        storedEntry.setId("review-9");
        storedEntry.setSenderUuid("123e4567-e89b-12d3-a456-426614174000");
        storedEntry.setSenderName("Alpha");
        storedEntry.setMessage("[12] Alpha: trade with Alpha");
        storedEntry.setScore(17);
        storedEntry.setDecidedByStage("RuleStage");
        storedEntry.setCapturedAtMs(2_000L);
        storedEntry.getReasons().add("reported by Alpha");

        ReviewConfig.ReviewStageResult storedStageResult = new ReviewConfig.ReviewStageResult();
        storedStageResult.setStageName("RuleStage");
        storedStageResult.setScoreDelta(17);
        storedStageResult.setReason("External platform push: \"Alpha\"");
        storedEntry.getStageResults().add(storedStageResult);

        ReviewConfig.ReviewCaseMessageConfig storedCaseMessage = new ReviewConfig.ReviewCaseMessageConfig();
        storedCaseMessage.setMessageIndex(0);
        storedCaseMessage.setSpeakerRole("other");
        storedCaseMessage.setMessageSourceType("player");
        storedCaseMessage.setCleanText("Alpha can carry 123e4567-e89b-12d3-a456-426614174000");
        storedCaseMessage.setTriggerMessage(true);
        storedCaseMessage.setCaseRole(ReviewCaseRole.SIGNAL);
        storedCaseMessage.getAdvancedRuleSelections().add("RuleStage - External platform push: \"Alpha\"");
        storedEntry.getCaseMessages().add(storedCaseMessage);

        ReviewEntry runtimeEntry = ReviewConfigStore.toRuntimeEntry(storedEntry);

        assertNull(runtimeEntry.getSenderUuid());
        assertEquals("", runtimeEntry.getSenderName());
        assertEquals("trade with Alpha", runtimeEntry.getMessage());
        assertEquals("reported by Alpha", runtimeEntry.getReasons().getFirst());
        assertEquals("stage.rule", runtimeEntry.getStageResults().getFirst().getStageId());
        assertEquals("External platform push: \"Alpha\"", runtimeEntry.getStageResults().getFirst().getReason());
        assertEquals(List.of("rule.external_platform"), runtimeEntry.getStageResults().getFirst().getReasonIds());
        assertEquals("Alpha can carry <uuid>", runtimeEntry.getCaseMessages().getFirst().getCleanText());
        assertEquals("stage.rule::rule.external_platform", runtimeEntry.getCaseMessages().getFirst().getAdvancedRuleSelections().getFirst());
    }
}
