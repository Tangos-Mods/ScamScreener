package eu.tango.scamscreener.pipeline.core;

import eu.tango.scamscreener.api.StageContribution;
import eu.tango.scamscreener.api.StageSlot;
import eu.tango.scamscreener.pipeline.data.ChatEvent;
import eu.tango.scamscreener.pipeline.data.StageResult;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScamScreenerPipelineFactoryTest {
    @Test
    void exposesStableCoreStageOrder() {
        assertEquals(
            List.of(
                StageSlot.MUTE,
                StageSlot.PLAYER_LIST,
                StageSlot.RULE,
                StageSlot.LEVENSHTEIN,
                StageSlot.BEHAVIOR,
                StageSlot.TREND,
                StageSlot.FUNNEL,
                StageSlot.MODEL
            ),
            ScamScreenerPipelineFactory.coreStageOrder()
        );
    }

    @Test
    void insertsExternalStagesBeforeAndAfterTheirTargetSlotInDeclaredOrder() {
        Map<StageSlot, Stage> coreStages = new EnumMap<>(StageSlot.class);
        coreStages.put(StageSlot.MUTE, new NamedStage("MuteStage"));
        coreStages.put(StageSlot.RULE, new NamedStage("RuleStage"));
        coreStages.put(StageSlot.MODEL, new NamedStage("ContextStage"));

        List<Stage> orderedStages = ScamScreenerPipelineFactory.orderedStages(
            coreStages,
            List.of(
                new StageContribution("before-rule-1", StageSlot.RULE, StageContribution.Position.BEFORE, new NamedStage("BeforeRuleOne")),
                new StageContribution("before-rule-2", StageSlot.RULE, StageContribution.Position.BEFORE, new NamedStage("BeforeRuleTwo")),
                new StageContribution("after-rule-1", StageSlot.RULE, StageContribution.Position.AFTER, new NamedStage("AfterRuleOne")),
                new StageContribution("after-model", StageSlot.MODEL, StageContribution.Position.AFTER, new NamedStage("AfterContext"))
            )
        );

        assertEquals(
            List.of(
                "MuteStage",
                "BeforeRuleOne",
                "BeforeRuleTwo",
                "RuleStage",
                "AfterRuleOne",
                "ContextStage",
                "AfterContext"
            ),
            orderedStages.stream().map(Stage::name).toList()
        );
    }

    private static final class NamedStage extends Stage {
        private final String name;

        private NamedStage(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        protected StageResult evaluate(ChatEvent chatEvent) {
            return pass();
        }
    }
}
