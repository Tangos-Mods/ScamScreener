package eu.tango.scamscreener.update;

import eu.tango.scamscreener.ScamScreenerMod;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModrinthUpdateCheckerTest {
    @Test
    void buildVersionsUriUsesV2ProjectEndpoint() throws Exception {
        Method method = ModrinthUpdateChecker.class.getDeclaredMethod("buildVersionsUri");
        method.setAccessible(true);

        URI uri = (URI) method.invoke(null);

        assertEquals(
            "https://api.modrinth.com/v2/project/XTB0bgAW/version?loaders="
                + URLEncoder.encode("[\"fabric\"]", StandardCharsets.UTF_8)
                + "&game_versions="
                + URLEncoder.encode("[\"" + ScamScreenerMod.MINECRAFT + "\"]", StandardCharsets.UTF_8)
                + "&include_changelog=true",
            uri.toString()
        );
    }
}
