package eu.tango.scamscreener.message;

import eu.tango.scamscreener.pipeline.core.Stage;
import eu.tango.scamscreener.pipeline.data.PipelineDecision;
import eu.tango.scamscreener.pipeline.data.StageResult;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
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
        MutableText warning = ClientMessages.educationExternalPlatformWarning(disableCommand);

        assertTrue(warning.getString().contains("The user is trying to move you over to an external platform."));
        assertTrue(hasClickValue(warning, disableCommand));
        assertTrue(hasClickValue(warning, "https://hypixelskyblock.minecraft.wiki/w/Scams"));
    }

    private static boolean hasClickValue(Text root, String expected) {
        for (Text text : flatten(root)) {
            ClickEvent clickEvent = text.getStyle().getClickEvent();
            if (clickEvent == null) {
                continue;
            }

            String clickValue = extractClickValue(clickEvent);
            if (expected.equals(clickValue)) {
                return true;
            }
        }

        return false;
    }

    private static List<Text> flatten(Text root) {
        List<Text> flattened = new ArrayList<>();
        collect(root, flattened);
        return flattened;
    }

    private static void collect(Text text, List<Text> flattened) {
        if (text == null) {
            return;
        }

        flattened.add(text);
        for (Text sibling : text.getSiblings()) {
            collect(sibling, flattened);
        }
    }

    private static String extractClickValue(ClickEvent clickEvent) {
        for (String accessor : List.of("command", "value", "getValue", "uri", "url", "file", "path")) {
            try {
                Method method = clickEvent.getClass().getMethod(accessor);
                Object value = method.invoke(clickEvent);
                if (value != null) {
                    return value.toString();
                }
            } catch (Exception ignored) {
            }
        }

        return null;
    }
}
