package eu.tango.scamscreener.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import eu.tango.scamscreener.gui.screen.ScamScreenerMainScreen;
import net.minecraft.client.gui.screens.Screen;

/**
 * ModMenu bridge that opens the ScamScreener root GUI.
 */
public final class ScamScreenerModMenuApi implements ModMenuApi {
    /**
     * Returns the root ScamScreener config screen factory for ModMenu.
     *
     * @return the config screen factory bound to the main screen
     */
    @Override
    public ConfigScreenFactory<Screen> getModConfigScreenFactory() {
        return ScamScreenerMainScreen::new;
    }
}
