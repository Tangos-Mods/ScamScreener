package eu.tango.scamscreener.message;

import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.MutableComponent;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientMessagesTest {
    @Test
    void updateAvailableMatchesExpectedText() {
        MutableComponent message = ClientMessages.updateAvailable(
            "2.0.1+26.1",
            "2.0.2+26.1",
            "https://modrinth.com/project/XTB0bgAW",
            "line 1"
        );

        assertEquals(
            "[ScamScreener] Update Available 2.0.1 -> 2.0.2. [click] to open on Modrinth",
            message.getString()
        );
    }

    @Test
    void changelogHoverTextTruncatesAfterTenLines() {
        String changelog = String.join("\n",
            "line 1",
            "line 2",
            "line 3",
            "line 4",
            "line 5",
            "line 6",
            "line 7",
            "line 8",
            "line 9",
            "line 10",
            "line 11",
            "line 12"
        );

        String hover = ClientMessages.changelogHoverText(changelog).getString();

        assertTrue(hover.contains("line 1"));
        assertTrue(hover.contains("line 10"));
        assertFalse(hover.contains("line 11"));
        assertFalse(hover.contains("line 12"));
        assertTrue(hover.endsWith("and many more..."));
    }

    @Test
    void commandHelpListsEducationDisableCommand() {
        assertTrue(ClientMessages.commandHelp().getString().contains("edu disable"));
    }

    @Test
    void trainingUploadRetryMessageIncludesRemainingRetriesAndDelay() {
        String message = ClientMessages.trainingUploadRetryScheduled("network timeout", 3, 30).getString();

        assertTrue(message.contains("Training upload failed: network timeout."));
        assertTrue(message.contains("Retrying automatically 3 more times"));
        assertTrue(message.contains("30s between attempts"));
    }

    @Test
    void trainingUploadReminderMatchesExpectedTextAndActions() {
        MutableComponent message = ClientMessages.trainingUploadReminder(7);

        assertEquals(
            "[ScamScreener] You have 7 saved cases. Upload them anonymously to help making this mod better [upload] [don't show again]",
            message.getString()
        );

        Style uploadStyle = message.getSiblings().get(3).getStyle();
        Style disableStyle = message.getSiblings().get(5).getStyle();

        assertNotNull(uploadStyle.getClickEvent());
        assertEquals("/ss training upload", extractClickValue(uploadStyle));
        assertNotNull(disableStyle.getClickEvent());
        assertEquals("/ss training reminder off", extractClickValue(disableStyle));
    }

    private static String extractClickValue(Style style) {
        if (style == null || style.getClickEvent() == null) {
            return null;
        }

        for (String accessor : new String[]{"command", "value", "getValue", "uri", "url", "file", "path"}) {
            try {
                Method method = style.getClickEvent().getClass().getMethod(accessor);
                Object value = method.invoke(style.getClickEvent());
                if (value != null) {
                    return value.toString();
                }
            } catch (Exception ignored) {
            }
        }

        return null;
    }
}
