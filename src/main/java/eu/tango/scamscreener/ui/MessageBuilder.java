package eu.tango.scamscreener.ui;

import eu.tango.scamscreener.rules.ScamRules;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.nio.file.Path;
import java.net.URI;
import java.util.stream.Collectors;

/**
 * Shared message presets for ScamScreener chat output.
 * <p>
 * Intended to be extended by message utility classes such as {@link Messages},
 * {@link DebugMessages} so they only need to fill in
 * content-specific values.
 */
public abstract class MessageBuilder {
	protected static final String DEFAULT_PREFIX = "[ScamScreener] ";
	protected static final int DEFAULT_PREFIX_COLOR = 0xFF5555;
	protected static final String WARNING_BORDER = "====================================";
	protected static final int WARNING_WIDTH = WARNING_BORDER.length();
	protected static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
	protected static final int MAX_HOVER_MESSAGES = 3;

	protected MessageBuilder() {
	}

	protected static MutableComponent prefixedMessage(String prefix, int prefixColor) {
		return Component.literal(safePrefix(prefix))
			.withStyle(style -> style.withColor(prefixColor));
	}

	protected static MutableComponent labeledMessage(
		String prefix,
		int prefixColor,
		ChatFormatting labelColor,
		ChatFormatting messageColor,
		String label,
		String message
	) {
		return prefixedMessage(prefix, prefixColor)
			.append(Component.literal(safeText(label) + ": ").withStyle(labelColor))
			.append(Component.literal(safeText(message)).withStyle(messageColor));
	}

	protected static MutableComponent buildError(String prefix, int prefixColor, String summary, String code, String detail) {
		String safeSummary = summary == null ? "Error." : summary;
		String safeCode = code == null || code.isBlank() ? "ERR-000" : code.trim();
		String safeDetail = detail == null || detail.isBlank() ? "unknown error" : detail;
		String hoverText = safeSummary + " (" + safeCode + ")\n" + safeDetail;

		return prefixedMessage(prefix, prefixColor)
			.append(Component.literal(safeSummary + " ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("[" + safeCode + "]")
				.withStyle(style -> style
					.withColor(ChatFormatting.YELLOW)
					.withHoverEvent(new HoverEvent.ShowText(Component.literal(hoverText).withStyle(ChatFormatting.GRAY)))));
	}

	protected static MutableComponent clickableFilePath(String path) {
		String safePath = path == null ? "" : path;
		return Component.literal(safePath).setStyle(
			Style.EMPTY
				.withColor(ChatFormatting.YELLOW)
				.withUnderlined(true)
				.withClickEvent(new ClickEvent.OpenFile(safePath))
				.withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to open this file in Explorer")))
		);
	}

	protected static MutableComponent clickableContainingFolderPath(String filePath) {
		String safePath = filePath == null ? "" : filePath;
		String folderPath = safePath;
		try {
			Path parsed = Path.of(safePath);
			Path parent = parsed.getParent();
			if (parent != null) {
				folderPath = parent.toString();
			}
		} catch (Exception ignored) {
		}
		return Component.literal(safePath).setStyle(
			Style.EMPTY
				.withColor(ChatFormatting.YELLOW)
				.withUnderlined(true)
				.withClickEvent(new ClickEvent.OpenFile(folderPath))
				.withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to open containing folder in Explorer")))
		);
	}

	protected static MutableComponent clickableUrl(String label, String url, String hoverText) {
		String safeLabel = safeText(label);
		String safeUrl = safeText(url);
		String safeHover = hoverText == null || hoverText.isBlank() ? safeUrl : hoverText;
		Style style = Style.EMPTY
			.withColor(ChatFormatting.AQUA)
			.withUnderlined(true)
			.withHoverEvent(new HoverEvent.ShowText(Component.literal(safeHover).withStyle(ChatFormatting.YELLOW)));
		if (!safeUrl.isBlank()) {
			try {
				style = style.withClickEvent(new ClickEvent.OpenUrl(URI.create(safeUrl)));
			} catch (Exception ignored) {
			}
		}
		return Component.literal(safeLabel).setStyle(
			style
		);
	}

	protected static MutableComponent actionTag(String label, ChatFormatting color, String hover, String command) {
		Style style = Style.EMPTY.withColor(color);
		if (hover != null && !hover.isBlank()) {
			style = style.withHoverEvent(new HoverEvent.ShowText(Component.literal(hover).withStyle(ChatFormatting.YELLOW)));
		}
		if (command != null && !command.isBlank()) {
			style = style.withClickEvent(new ClickEvent.RunCommand(command));
		} else {
			style = style.withStrikethrough(true);
		}
		return Component.literal("[" + safeText(label) + "]").setStyle(style);
	}

	protected static String centered(String text) {
		if (text == null) {
			return "";
		}
		String padding = leadingPadding(text);
		if (padding.isEmpty()) {
			return text;
		}
		return padding + text;
	}

	protected static String centeredBold(String text) {
		if (text == null) {
			return "";
		}
		String padding = leadingPadding(text, true);
		if (padding.isEmpty()) {
			return text;
		}
		return padding + text;
	}

	protected static String leftCenterPadding(String text) {
		return leadingPadding(text);
	}

	protected static String leadingPadding(String text) {
		return leadingPadding(text, false);
	}

	protected static String leadingPadding(String text, boolean bold) {
		if (text == null || text.isEmpty()) {
			return "";
		}

		Minecraft client = Minecraft.getInstance();
		if (client != null && client.font != null) {
			int targetWidth = client.font.width(WARNING_BORDER);
			int textWidth = client.font.width(text);
			if (bold) {
				textWidth += estimateBoldExtraPixels(text);
			}
			if (textWidth >= targetWidth) {
				return "";
			}
			int spaceWidth = Math.max(1, client.font.width(" "));
			int leftPixels = (targetWidth - textWidth) / 2;
			int spaceCount = Math.max(0, (leftPixels + (spaceWidth / 2)) / spaceWidth);
			return " ".repeat(spaceCount);
		}

		if (text.length() >= WARNING_WIDTH) {
			return "";
		}
		int leftPadding = (WARNING_WIDTH - text.length()) / 2;
		return " ".repeat(Math.max(0, leftPadding));
	}

	private static int estimateBoldExtraPixels(String text) {
		int extra = 0;
		for (int i = 0; i < text.length(); i++) {
			if (!Character.isWhitespace(text.charAt(i))) {
				extra++;
			}
		}
		return extra;
	}

	protected static int scoreGradientColor(int score) {
		int clamped = Math.max(0, Math.min(100, score));
		int fade = 255 - (int) Math.round((clamped / 100.0) * 255.0);
		return (255 << 16) | (fade << 8) | fade;
	}

	protected static int levelColor(ScamRules.ScamRiskLevel level) {
		return switch (level) {
			case LOW -> 0xFFB3B3;
			case MEDIUM -> 0xFF8080;
			case HIGH -> 0xFF4D4D;
			case CRITICAL -> 0xB30000;
		};
	}

	public static String readableRuleName(ScamRules.ScamRule rule) {
		if (rule == null) {
			return "Unknown Rule";
		}
		return switch (rule) {
			case SUSPICIOUS_LINK -> "Suspicious Link";
			case PRESSURE_AND_URGENCY -> "Pressure/Urgency";
			case UPFRONT_PAYMENT -> "Upfront Payment";
			case ACCOUNT_DATA_REQUEST -> "Account Data Request";
			case EXTERNAL_PLATFORM_PUSH -> "External Platform Push";
			case DISCORD_HANDLE -> "Discord Handle";
			case FAKE_MIDDLEMAN_CLAIM -> "Fake Middleman Claim";
			case TOO_GOOD_TO_BE_TRUE -> "Too Good To Be True";
			case TRUST_MANIPULATION -> "Trust Manipulation";
			case SPAMMY_CONTACT_PATTERN -> "Spammy Contact Pattern";
			case MULTI_MESSAGE_PATTERN -> "Multi-Message Pattern";
			case FUNNEL_SEQUENCE_PATTERN -> "Funnel Sequence Pattern";
			case SIMILARITY_MATCH -> "Similarity Match";
			case LOCAL_AI_RISK_SIGNAL -> "Local AI Risk Signal";
			case LOCAL_AI_FUNNEL_SIGNAL -> "Local AI Funnel Signal";
		};
	}

	protected static MutableComponent readableRule(ScamRules.ScamRule rule, String exactDetail, List<String> evaluatedMessages) {
		String name = readableRuleName(rule);

		String detail = exactDetail == null || exactDetail.isBlank()
			? "No detailed trigger context available."
			: exactDetail;

		MutableComponent hover = Component.literal("ScamScreener Rule").withStyle(style -> style.withColor(ChatFormatting.DARK_RED).withBold(true))
			.append(Component.literal("\nRule: ").withStyle(style -> style.withColor(ChatFormatting.GRAY).withBold(false)))
			.append(Component.literal(name).withStyle(style -> style.withColor(ChatFormatting.GOLD).withBold(false)))
			.append(Component.literal("\nWhy triggered:").withStyle(style -> style.withColor(ChatFormatting.GRAY).withBold(false)))
			.append(highlightQuoted(detail))
			.append(Component.literal("\nEvaluated message(s):").withStyle(style -> style.withColor(ChatFormatting.GRAY).withBold(false)))
			.append(Component.literal("\n" + compactHoverMessages(evaluatedMessages)).withStyle(style -> style.withColor(ChatFormatting.AQUA).withBold(false)));

		return Component.literal(name).setStyle(
			Style.EMPTY
				.withColor(ChatFormatting.YELLOW)
				.withHoverEvent(new HoverEvent.ShowText(hover))
		);
	}

	protected static String formatTimestamp(String input) {
		if (input == null || input.isBlank()) {
			return "n/a";
		}
		try {
			return TIMESTAMP_FORMATTER.format(Instant.parse(input));
		} catch (Exception ignored) {
			return input;
		}
	}

	protected static String compactHoverMessages(List<String> inputs) {
		if (inputs == null || inputs.isEmpty()) {
			return "n/a";
		}
		StringBuilder out = new StringBuilder();
		int limit = Math.min(4, inputs.size());
		for (int i = 0; i < limit; i++) {
			String input = inputs.get(i);
			if (input == null || input.isBlank()) {
				continue;
			}
			String singleLine = input.replace('\n', ' ').replace('\r', ' ').trim();
			if (singleLine.length() > 140) {
				singleLine = singleLine.substring(0, 137) + "...";
			}
			if (out.length() > 0) {
				out.append("\n");
			}
			out.append("- ").append(singleLine);
		}
		if (inputs.size() > limit) {
			out.append("\n- ... and ").append(inputs.size() - limit).append(" more");
		}
		return out.length() == 0 ? "n/a" : out.toString();
	}

	protected static MutableComponent highlightQuoted(String detail) {
		if (detail == null || detail.isBlank()) {
			return Component.literal("\nNo detailed trigger context available.").withStyle(style -> style.withColor(ChatFormatting.YELLOW).withBold(false));
		}
		String[] parts = detail.split("\"", -1);
		MutableComponent out = Component.literal("\n").withStyle(style -> style.withColor(ChatFormatting.YELLOW).withBold(false));
		for (int i = 0; i < parts.length; i++) {
			String part = parts[i];
			if (part.isEmpty()) {
				continue;
			}
			boolean quoted = (i % 2 == 1);
			out.append(Component.literal(part).withStyle(style -> style.withColor(quoted ? ChatFormatting.GOLD : ChatFormatting.YELLOW).withBold(quoted)));
		}
		return out;
	}

	protected static MutableComponent hoverableMessage(String raw, List<String> highlightTerms) {
		String normalized = raw.replace('\n', ' ').replace('\r', ' ').trim();
		String display = normalized.length() > 140 ? normalized.substring(0, 137) + "..." : normalized;
		String id = MessageFlagging.registerMessage(normalized);
		MutableComponent hover = Component.literal("CTRL+Y = legit\nCTRL+N = scam").withStyle(ChatFormatting.YELLOW);

		MutableComponent content = highlightText(display, highlightTerms);
		return content.setStyle(
			Style.EMPTY
				.withColor(ChatFormatting.AQUA)
				.withUnderlined(true)
				.withHoverEvent(new HoverEvent.ShowText(hover))
				.withClickEvent(new ClickEvent.CopyToClipboard(MessageFlagging.clickValue(id)))
		);
	}

	protected static MutableComponent highlightText(String text, List<String> terms) {
		if (terms == null || terms.isEmpty() || text == null || text.isBlank()) {
			return Component.literal(text == null ? "" : text);
		}

		String lower = text.toLowerCase(Locale.ROOT);
		int index = 0;
		MutableComponent out = Component.empty();

		while (index < text.length()) {
			Match next = findNextMatch(lower, terms, index);
			if (next == null) {
				out.append(Component.literal(text.substring(index)));
				break;
			}
			if (next.start() > index) {
				out.append(Component.literal(text.substring(index, next.start())));
			}
			out.append(Component.literal(text.substring(next.start(), next.end()))
				.withStyle(style -> style.withColor(ChatFormatting.GOLD).withBold(true)));
			index = next.end();
		}

		return out;
	}

	private static Match findNextMatch(String lowerText, List<String> terms, int fromIndex) {
		int bestStart = -1;
		int bestEnd = -1;
		for (String term : terms) {
			if (term == null || term.isBlank()) {
				continue;
			}
			String lowerTerm = term.toLowerCase(Locale.ROOT);
			int found = lowerText.indexOf(lowerTerm, fromIndex);
			if (found < 0) {
				continue;
			}
			if (bestStart == -1 || found < bestStart) {
				bestStart = found;
				bestEnd = found + lowerTerm.length();
			}
		}
		if (bestStart < 0) {
			return null;
		}
		return new Match(bestStart, bestEnd);
	}

	protected static List<String> extractQuotedMatches(ScamRules.ScamAssessment assessment) {
		if (assessment == null || assessment.ruleDetails() == null || assessment.ruleDetails().isEmpty()) {
			return List.of();
		}
		List<String> matches = new ArrayList<>();
		for (String detail : assessment.ruleDetails().values()) {
			if (detail == null || detail.isBlank()) {
				continue;
			}
			String[] parts = detail.split("\"", -1);
			for (int i = 1; i < parts.length; i += 2) {
				String match = parts[i].trim();
				if (!match.isBlank()) {
					matches.add(match);
				}
			}
		}
		return matches;
	}

	private record Match(int start, int end) {
	}

	protected static MutableComponent actionLine(String alertContextId) {
		boolean hasContext = alertContextId != null && !alertContextId.isBlank();
		MutableComponent line = Component.literal(leadingPadding("[manage] [info]"));

		line.append(actionTag(
			"manage",
			ChatFormatting.GOLD,
			"Open review window for training-label selection and upload options.",
			hasContext ? "/scamscreener review manage " + alertContextId : null
		));
		line.append(Component.literal(" "));
		line.append(actionTag(
			"info",
			ChatFormatting.YELLOW,
			"Open rule detail window for this alert.",
			hasContext ? "/scamscreener review info " + alertContextId : null
		));

		return line;
	}

	private static String safePrefix(String prefix) {
		return prefix == null ? "" : prefix;
	}

	protected static String safeText(String value) {
		return value == null ? "" : value;
	}
}
