package eu.tango.scamscreener.market;

import net.minecraft.ChatFormatting;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class MarketItemKey {
	private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9]+");
	private static final Pattern PET_LEVEL_PREFIX = Pattern.compile("^\\[\\s*lvl\\s*\\d+\\s*]\\s*", Pattern.CASE_INSENSITIVE);

	private MarketItemKey() {
	}

	public static String fromItemStack(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return "";
		}
		return fromDisplayName(stack.getHoverName().getString());
	}

	public static String fromDisplayName(String displayName) {
		return normalize(stripPetLevelPrefix(displayName));
	}

	public static String fromDisplayNameAndLore(String displayName, List<String> loreLines) {
		String base = normalize(stripPetLevelPrefix(displayName));
		if (base.isBlank()) {
			return "";
		}
		if (loreLines == null || loreLines.isEmpty()) {
			return base;
		}
		StringBuilder suffix = new StringBuilder();
		for (String line : loreLines) {
			String normalized = normalize(line);
			if (normalized.isBlank()) {
				continue;
			}
			if (normalized.contains("star") || normalized.contains("dungeon")
				|| normalized.contains("recombobulated") || normalized.contains("hot potato")) {
				suffix.append('-').append(normalized);
			}
		}
		return suffix.length() == 0 ? base : base + suffix;
	}

	public static String normalize(String text) {
		if (text == null || text.isBlank()) {
			return "";
		}
		String stripped = ChatFormatting.stripFormatting(text);
		String lower = (stripped == null ? text : stripped).toLowerCase(Locale.ROOT);
		return NON_ALNUM.matcher(lower).replaceAll("_").replaceAll("^_+|_+$", "");
	}

	private static String stripPetLevelPrefix(String text) {
		if (text == null || text.isBlank()) {
			return "";
		}
		return PET_LEVEL_PREFIX.matcher(text).replaceFirst("").trim();
	}
}
