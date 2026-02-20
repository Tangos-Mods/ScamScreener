package eu.tango.scamscreener.mixin;

import eu.tango.scamscreener.ScamScreenerClient;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {
	@Inject(method = "handleInventoryMouseClick", at = @At("HEAD"), cancellable = true)
	private void scamscreener$onHandleInventoryMouseClick(int containerId, int slotId, int buttonNum, ClickType clickType, Player player, CallbackInfo callbackInfo) {
		if (!ScamScreenerClient.allowMarketInventoryClick(containerId, slotId, buttonNum, clickType)) {
			callbackInfo.cancel();
		}
	}
}

