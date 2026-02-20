package eu.tango.scamscreener.market;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.ItemStack;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class RareItemClassifier {
	private static final String RESOURCE_PATH = "/assets/scam-screener/data/market-rare-rules.json";

	private final Rules rules;

	public RareItemClassifier() {
		this(loadRules(RESOURCE_PATH));
	}

	RareItemClassifier(Rules rules) {
		this.rules = rules == null ? Rules.empty() : rules;
	}

	public Match classify(ItemStack stack, String itemKey, List<String> tooltipLines) {
		String normalizedKey = MarketItemKey.normalize(itemKey);
		String itemName = stack == null || stack.isEmpty() ? "" : safeLower(stack.getHoverName().getString());
		return classify(normalizedKey, itemName, tooltipLines);
	}

	public Match classify(String itemKey, String itemName, List<String> tooltipLines) {
		String normalizedKey = MarketItemKey.normalize(itemKey);
		String normalizedName = safeLower(itemName);

		if (!normalizedKey.isBlank() && rules.exactItemKeys().contains(normalizedKey)) {
			return new Match(true, "item_key");
		}
		for (String fragment : rules.nameContains()) {
			if (!fragment.isBlank() && normalizedName.contains(fragment)) {
				return new Match(true, "name_contains");
			}
		}
		if (tooltipLines != null && !tooltipLines.isEmpty()) {
			for (String line : tooltipLines) {
				String normalizedLine = safeLower(line);
				if (normalizedLine.isBlank()) {
					continue;
				}
				for (String fragment : rules.loreContains()) {
					if (!fragment.isBlank() && normalizedLine.contains(fragment)) {
						return new Match(true, "lore_contains");
					}
				}
			}
		}
		return new Match(false, "");
	}

	private static Rules loadRules(String resourcePath) {
		InputStream stream = RareItemClassifier.class.getResourceAsStream(resourcePath);
		if (stream == null) {
			return Rules.empty();
		}
		try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
			JsonElement parsed = JsonParser.parseReader(reader);
			if (!parsed.isJsonObject()) {
				return Rules.empty();
			}
			JsonObject root = parsed.getAsJsonObject();
			Set<String> exact = normalizeSet(asStringArray(root, "item_keys"));
			List<String> nameContains = normalizeList(asStringArray(root, "name_contains"));
			List<String> loreContains = normalizeList(asStringArray(root, "lore_contains"));
			return new Rules(exact, nameContains, loreContains);
		} catch (Exception ignored) {
			return Rules.empty();
		}
	}

	private static List<String> asStringArray(JsonObject object, String key) {
		JsonArray array = array(object, key);
		if (array == null || array.isEmpty()) {
			return List.of();
		}
		List<String> out = new ArrayList<>();
		for (JsonElement element : array) {
			if (element == null || !element.isJsonPrimitive()) {
				continue;
			}
			String value = element.getAsString();
			if (value != null && !value.isBlank()) {
				out.add(value);
			}
		}
		return out;
	}

	private static Set<String> normalizeSet(List<String> values) {
		Set<String> out = new LinkedHashSet<>();
		if (values == null) {
			return out;
		}
		for (String value : values) {
			String normalized = MarketItemKey.normalize(value);
			if (!normalized.isBlank()) {
				out.add(normalized);
			}
		}
		return out;
	}

	private static List<String> normalizeList(List<String> values) {
		if (values == null || values.isEmpty()) {
			return List.of();
		}
		List<String> out = new ArrayList<>();
		for (String value : values) {
			String normalized = safeLower(value);
			if (!normalized.isBlank()) {
				out.add(normalized);
			}
		}
		return List.copyOf(out);
	}

	private static String safeLower(String value) {
		if (value == null || value.isBlank()) {
			return "";
		}
		String stripped = ChatFormatting.stripFormatting(value);
		String safe = stripped == null ? value : stripped;
		return safe.toLowerCase(Locale.ROOT).trim();
	}

	private static JsonArray array(JsonObject object, String key) {
		if (object == null || key == null || key.isBlank()) {
			return null;
		}
		JsonElement element = object.get(key);
		return element != null && element.isJsonArray() ? element.getAsJsonArray() : null;
	}

	record Rules(Set<String> exactItemKeys, List<String> nameContains, List<String> loreContains) {
		Rules {
			exactItemKeys = exactItemKeys == null ? Set.of() : Set.copyOf(exactItemKeys);
			nameContains = nameContains == null ? List.of() : List.copyOf(nameContains);
			loreContains = loreContains == null ? List.of() : List.copyOf(loreContains);
		}

		private static Rules empty() {
			return new Rules(Set.of(), List.of(), List.of());
		}
	}

	public record Match(boolean rare, String reason) {
		public Match {
			reason = reason == null ? "" : reason;
		}
	}
}

