package eu.tango.scamscreener.message;

import net.minecraft.network.chat.MutableComponent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
}
