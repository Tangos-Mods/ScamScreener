package eu.tango.scamscreener.location;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

public enum SkyblockIsland {
	UNKNOWN("Unknown"),
	PRIVATE_ISLAND("Private Island", "your island"),
	HUB("Hub", "village", "community center", "mountain"),
	THE_PARK("The Park", "park", "birch park", "spruce woods", "savanna woodland", "jungle island", "dark thicket"),
	GOLD_MINE("Gold Mine"),
	DEEP_CAVERNS("Deep Caverns", "gunpowder mines", "lapis quarry", "pigmen s den", "slimehill", "diamond reserve", "obsidian sanctuary"),
	THE_FARMING_ISLANDS("The Farming Islands", "farming islands", "the barn", "barn", "mushroom desert"),
	SPIDERS_DEN("Spider's Den", "spiders den", "arachne s sanctuary", "gravel mines"),
	THE_END("The End", "end island", "dragon s nest", "void sepulture", "bruiser hideout"),
	DUNGEON_HUB("Dungeon Hub", "catacombs entrance"),
	THE_CATACOMBS("The Catacombs", "catacombs", "dungeon"),
	JERRYS_WORKSHOP("Jerry's Workshop", "jerry workshop"),
	CRIMSON_ISLE("Crimson Isle", "stronghold", "scarlet fields", "smoldering tomb", "blazing volcano", "magma chamber"),
	DWARVEN_MINES("Dwarven Mines"),
	CRYSTAL_HOLLOWS("Crystal Hollows", "jungle", "goblin holdout", "mithril deposits", "precursor remnants", "magma fields"),
	THE_GARDEN("The Garden", "garden"),
	THE_RIFT("The Rift", "rift"),
	KUUDRA("Kuudra", "kuudra hollow"),
	DARK_AUCTION("Dark Auction");

	private static final Pattern SCOREBOARD_PREFIX = Pattern.compile("^(?:area|zone|location)\\s*:?\\s*", Pattern.CASE_INSENSITIVE);
	private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9]+");
	private static final Map<String, SkyblockIsland> EXACT_LOOKUP = new LinkedHashMap<>();
	private static final List<AliasMatch> PARTIAL_LOOKUP = new ArrayList<>();

	static {
		for (SkyblockIsland island : values()) {
			if (island == UNKNOWN) {
				continue;
			}
			for (String alias : island.aliases) {
				String normalized = normalize(alias);
				if (normalized.isBlank()) {
					continue;
				}
				EXACT_LOOKUP.putIfAbsent(normalized, island);
				PARTIAL_LOOKUP.add(new AliasMatch(normalized, island));
			}
			String display = normalize(island.displayName);
			if (!display.isBlank()) {
				EXACT_LOOKUP.putIfAbsent(display, island);
				PARTIAL_LOOKUP.add(new AliasMatch(display, island));
			}
		}
		PARTIAL_LOOKUP.sort(Comparator.comparingInt((AliasMatch match) -> match.normalizedAlias().length()).reversed());
	}

	private final String displayName;
	private final List<String> aliases;

	SkyblockIsland(String displayName, String... aliases) {
		this.displayName = displayName;
		this.aliases = aliases == null ? List.of() : Arrays.asList(aliases);
	}

	public String displayName() {
		return displayName;
	}

	public static Optional<SkyblockIsland> fromScoreboardLine(String line) {
		if (line == null || line.isBlank()) {
			return Optional.empty();
		}
		String normalized = normalize(cleanLine(line));
		if (normalized.isBlank()) {
			return Optional.empty();
		}

		SkyblockIsland exact = EXACT_LOOKUP.get(normalized);
		if (exact != null) {
			return Optional.of(exact);
		}

		for (AliasMatch match : PARTIAL_LOOKUP) {
			if (normalized.contains(match.normalizedAlias())) {
				return Optional.of(match.island());
			}
		}
		return Optional.empty();
	}

	private static String cleanLine(String line) {
		String cleaned = line == null ? "" : line.trim();
		if (!cleaned.isEmpty() && cleaned.charAt(0) == '\u23E3') {
			cleaned = cleaned.substring(1).trim();
		}
		cleaned = SCOREBOARD_PREFIX.matcher(cleaned).replaceFirst("");
		return cleaned.trim();
	}

	private static String normalize(String value) {
		if (value == null) {
			return "";
		}
		String lower = value.toLowerCase(Locale.ROOT);
		return NON_ALNUM.matcher(lower).replaceAll(" ").trim();
	}

	private record AliasMatch(String normalizedAlias, SkyblockIsland island) {
	}
}
