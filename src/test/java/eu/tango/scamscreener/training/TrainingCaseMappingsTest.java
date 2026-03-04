package eu.tango.scamscreener.training;

import eu.tango.scamscreener.pipeline.core.Stage;
import eu.tango.scamscreener.pipeline.data.StageResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrainingCaseMappingsTest {
    @Test
    void buildsMappingOptionsFromStableReasonIdsWithoutReasonTextParsing() {
        List<TrainingCaseMappings.MappingOption> options = TrainingCaseMappings.optionsForStageResults(List.of(
            StageResult.of(
                "RuleStage",
                "stage.rule",
                Stage.Decision.PASS,
                20,
                "External platform push: \"discord\"",
                List.of("rule.external_platform")
            ),
            StageResult.of(
                "TrendStage",
                "stage.trend",
                Stage.Decision.PASS,
                2,
                "",
                List.of("trend.multi_sender_wave")
            )
        ));

        assertEquals(2, options.size());
        assertTrue(options.stream().anyMatch(option -> "stage.rule::rule.external_platform".equals(option.id())));
        assertTrue(options.stream().anyMatch(option -> "stage.trend::trend.multi_sender_wave".equals(option.id())));
        assertTrue(options.stream().anyMatch(option -> option.id().equals("stage.trend::trend.multi_sender_wave")
            && option.label().startsWith("Trend Stage - ")));
    }
}
