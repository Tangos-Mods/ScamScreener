package eu.tango.scamscreener.gui;

import eu.tango.scamscreener.gui.screen.ScamScreenerMainScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

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
        Minecraft client = Minecraft.getInstance();
        openRoot(client.screen);
    }

    /**
     * Opens the root ScamScreener GUI.
     *
     * @param parent the parent screen to return to
     */
    public static void openRoot(Screen parent) {
        Minecraft client = Minecraft.getInstance();
        client.setScreen(new ScamScreenerMainScreen(parent));
    }
}
