package eu.tango.scamscreener.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import eu.tango.scamscreener.ScamScreenerClient;
import net.minecraft.client.gui.screens.Screen;

public final class ScamScreenerModMenuApi implements ModMenuApi {
	@Override
	public ConfigScreenFactory<Screen> getModConfigScreenFactory() {
		return parent -> (Screen) ScamScreenerClient.createSettingsScreen(parent);
	}
}
