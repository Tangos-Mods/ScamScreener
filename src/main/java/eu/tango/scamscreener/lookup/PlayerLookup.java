package eu.tango.scamscreener.lookup;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;

import java.util.Collection;
import java.util.UUID;

public final class PlayerLookup {
	public Collection<PlayerInfo> onlinePlayers() {
		Minecraft client = Minecraft.getInstance();
		ClientPacketListener connection = client.getConnection();
		if (connection == null) {
			return java.util.List.of();
		}
		return connection.getOnlinePlayers();
	}

	public UUID findUuidByName(String playerName) {
		if (playerName == null || playerName.isBlank()) {
			return null;
		}

		for (PlayerInfo entry : onlinePlayers()) {
			if (entry.getProfile().name().equalsIgnoreCase(playerName.trim())) {
				return entry.getProfile().id();
			}
		}
		return null;
	}

	public String findNameByUuid(UUID uuid) {
		if (uuid == null) {
			return "unknown";
		}

		for (PlayerInfo entry : onlinePlayers()) {
			if (uuid.equals(entry.getProfile().id())) {
				return entry.getProfile().name();
			}
		}
		return "unknown";
	}
}
