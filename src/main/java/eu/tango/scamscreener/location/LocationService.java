package eu.tango.scamscreener.location;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class LocationService {
	private static final int UPDATE_INTERVAL_TICKS = 5;

	private int ticks;
	private LocationSnapshot current = LocationSnapshot.unknown();

	public void onClientTick(Minecraft client) {
		ticks++;
		if (client == null || client.level == null || client.player == null || client.getConnection() == null) {
			reset();
			return;
		}
		if (ticks % UPDATE_INTERVAL_TICKS != 0) {
			return;
		}
		current = readFromSidebar(client).orElse(LocationSnapshot.unknown());
	}

	public void reset() {
		ticks = 0;
		current = LocationSnapshot.unknown();
	}

	public LocationSnapshot current() {
		return current;
	}

	private Optional<LocationSnapshot> readFromSidebar(Minecraft client) {
		Scoreboard scoreboard = client.level.getScoreboard();
		Objective objective = resolveSidebarObjective(scoreboard, client.player.getScoreboardName());
		if (objective == null) {
			return Optional.empty();
		}

		for (String line : sidebarLines(scoreboard, objective)) {
			Optional<SkyblockIsland> island = SkyblockIsland.fromScoreboardLine(line);
			if (island.isPresent()) {
				return Optional.of(new LocationSnapshot(island.get(), line));
			}
		}
		return Optional.empty();
	}

	private static Objective resolveSidebarObjective(Scoreboard scoreboard, String playerName) {
		if (scoreboard == null) {
			return null;
		}

		if (playerName != null && !playerName.isBlank()) {
			PlayerTeam team = scoreboard.getPlayersTeam(playerName);
			if (team != null) {
				DisplaySlot teamSlot = DisplaySlot.teamColorToSlot(team.getColor());
				if (teamSlot != null) {
					Objective objective = scoreboard.getDisplayObjective(teamSlot);
					if (objective != null) {
						return objective;
					}
				}
			}
		}

		return scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
	}

	private static List<String> sidebarLines(Scoreboard scoreboard, Objective objective) {
		return scoreboard.listPlayerScores(objective).stream()
			.filter(entry -> entry != null && !entry.isHidden())
			.sorted(Comparator.comparingInt(PlayerScoreEntry::value).reversed()
				.thenComparing(entry -> entry.owner().toLowerCase(Locale.ROOT)))
			.limit(15)
			.map(entry -> formatLine(scoreboard, entry))
			.filter(line -> line != null && !line.isBlank())
			.toList();
	}

	private static String formatLine(Scoreboard scoreboard, PlayerScoreEntry entry) {
		PlayerTeam team = scoreboard.getPlayersTeam(entry.owner());
		Component rendered = PlayerTeam.formatNameForTeam(team, entry.ownerName());
		String raw = rendered == null ? "" : rendered.getString();
		String stripped = ChatFormatting.stripFormatting(raw);
		String value = stripped == null ? raw : stripped;
		return value == null ? "" : value.trim();
	}

	public record LocationSnapshot(SkyblockIsland island, String rawLine) {
		private static LocationSnapshot unknown() {
			return new LocationSnapshot(SkyblockIsland.UNKNOWN, "");
		}
	}
}
