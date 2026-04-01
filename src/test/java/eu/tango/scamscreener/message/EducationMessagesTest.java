package eu.tango.scamscreener.message;

import eu.tango.scamscreener.pipeline.core.Stage;
import eu.tango.scamscreener.pipeline.data.PipelineDecision;
import eu.tango.scamscreener.pipeline.data.StageResult;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EducationMessagesTest {
    @Test
    void messageIdForUsesLegacyPriorityOrder() {
        PipelineDecision decision = new PipelineDecision(
            PipelineDecision.Outcome.REVIEW,
            42,
            "RuleStage",
            List.of(StageResult.of(
                "RuleStage",
                "stage.rule",
                Stage.Decision.PASS,
                42,
                "Trust manipulation wording: trusted; Upfront payment wording: pay first",
                List.of("rule.trust", "rule.upfront_payment")
            )),
            List.of("Trust manipulation wording: trusted", "Upfront payment wording: pay first")
        );

        assertEquals(EducationMessages.UPFRONT_PAYMENT_ID, EducationMessages.messageIdFor(decision));
    }

    @Test
    void followUpForSkipsDisabledEducationMessageIds() {
        PipelineDecision decision = new PipelineDecision(
            PipelineDecision.Outcome.REVIEW,
            35,
            "RuleStage",
            List.of(StageResult.of(
                "RuleStage",
                "stage.rule",
                Stage.Decision.PASS,
                35,
                "External platform push: discord",
                List.of("rule.external_platform")
            )),
            List.of("External platform push: discord")
        );

        assertNull(EducationMessages.followUpFor(decision, Set.of(EducationMessages.EXTERNAL_PLATFORM_REDIRECT_ID)));
    }

    @Test
    void educationWarningsExposeDisableCommandAndHelpLink() {
        String disableCommand = "/scamscreener edu disable " + EducationMessages.EXTERNAL_PLATFORM_REDIRECT_ID;
        MutableComponent warning = ClientMessages.educationExternalPlatformWarning(disableCommand);

        assertTrue(warning.getString().contains("The user is trying to move you over to an external platform."));
        assertTrue(hasClickValue(warning, disableCommand));
        assertTrue(hasClickValue(warning, "https://hypixel-skyblock.fandom.com/wiki/Scams"));
    }

    private static boolean hasClickValue(Component root, String expected) {
        for (Component component : flatten(root)) {
            ClickEvent clickEvent = component.getStyle().getClickEvent();
            if (clickEvent instanceof ClickEvent.RunCommand runCommand && expected.equals(runCommand.command())) {
                return true;
            }
            if (clickEvent instanceof ClickEvent.OpenUrl openUrl && expected.equals(openUrl.uri().toString())) {
                return true;
            }
        }

        return false;
    }

    private static List<Component> flatten(Component root) {
        List<Component> flattened = new ArrayList<>();
        collect(root, flattened);
        return flattened;
    }

    private static void collect(Component component, List<Component> flattened) {
        if (component == null) {
            return;
        }

        flattened.add(component);
        for (Component sibling : component.getSiblings()) {
            collect(sibling, flattened);
        }
    }
}
