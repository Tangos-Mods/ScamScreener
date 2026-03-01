package eu.tango.scamscreener.gui;

import eu.tango.scamscreener.gui.screen.ScamScreenerMainScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

/**
 * Small launcher helpers for ScamScreener screens.
 */
public final class ScamScreenerScreens {
    private ScamScreenerScreens() {
    }

    /**
     * Opens the root ScamScreener GUI.
     */
    public static void openRoot() {
        MinecraftClient client = MinecraftClient.getInstance();
        openRoot(client.currentScreen);
    }

    /**
     * Opens the root ScamScreener GUI.
     *
     * @param parent the parent screen to return to
     */
    public static void openRoot(Screen parent) {
        MinecraftClient client = MinecraftClient.getInstance();
        client.setScreen(new ScamScreenerMainScreen(parent));
    }
}
