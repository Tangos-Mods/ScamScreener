package eu.tango.scamscreener.ui.messages;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

abstract class StyledMessages {
	protected static final String PREFIX = "[ScamScreener] ";
	protected static final int PREFIX_LIGHT_RED = 0xFF5555;

	protected static MutableComponent prefixed() {
		return Component.literal(PREFIX).withStyle(style -> style.withColor(PREFIX_LIGHT_RED));
	}

	protected static MutableComponent prefixedGray(String value) {
		return prefixed().append(gray(value));
	}

	protected static MutableComponent prefixedList(String header, String... lines) {
		MutableComponent out = prefixed().append(gray(header));
		if (lines == null) {
			return out;
		}
		for (String line : lines) {
			out.append(gray("\n- " + safe(line)));
		}
		return out;
	}

	protected static MutableComponent warningPrefixed(String value) {
		return Component.literal(PREFIX + safe(value))
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.withStyle(ChatFormatting.GRAY);
	}

	protected static MutableComponent text(String value, ChatFormatting... formatting) {
		MutableComponent out = Component.literal(safe(value));
		if (formatting == null || formatting.length == 0) {
			return out;
		}
		return out.withStyle(formatting);
	}

	protected static String safe(String value) {
		return value == null ? "" : value;
	}

	protected static String safe(String value, String fallback) {
		if (value == null || value.isBlank()) {
			return fallback;
		}
		return value;
	}

	protected static MutableComponent gray(String value) {
		return text(value, ChatFormatting.GRAY);
	}

	protected static MutableComponent darkGray(String value) {
		return text(value, ChatFormatting.DARK_GRAY);
	}

	protected static MutableComponent yellow(String value) {
		return text(value, ChatFormatting.YELLOW);
	}

	protected static MutableComponent gold(String value) {
		return text(value, ChatFormatting.GOLD);
	}

	protected static MutableComponent goldBold(String value) {
		return text(value, ChatFormatting.GOLD, ChatFormatting.BOLD);
	}

	protected static MutableComponent darkRed(String value) {
		return text(value, ChatFormatting.DARK_RED);
	}

	protected static MutableComponent darkRedBold(String value) {
		return text(value, ChatFormatting.DARK_RED, ChatFormatting.BOLD);
	}

	protected static MutableComponent aqua(String value) {
		return text(value, ChatFormatting.AQUA);
	}

	protected static MutableComponent green(String value) {
		return text(value, ChatFormatting.GREEN);
	}

	protected static MutableComponent error(String summary, String code, String detail) {
		String safeSummary = summary == null ? "Error." : summary;
		String safeCode = code == null || code.isBlank() ? "ERR-000" : code.trim();
		String safeDetail = detail == null || detail.isBlank() ? "unknown error" : detail;
		String hoverText = safeSummary + " (" + safeCode + ")\n" + safeDetail;

		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal(safeSummary + " ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("[" + safeCode + "]")
				.withStyle(style -> style
					.withColor(ChatFormatting.YELLOW)
					.withHoverEvent(new HoverEvent.ShowText(Component.literal(hoverText).withStyle(ChatFormatting.GRAY)))));
	}
}
