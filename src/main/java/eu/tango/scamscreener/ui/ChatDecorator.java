package eu.tango.scamscreener.ui;

import java.util.ArrayList;
import java.util.List;

import eu.tango.scamscreener.chat.parser.ChatLineParser;
import eu.tango.scamscreener.util.TextUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

public final class ChatDecorator {
	private ChatDecorator() {
	}

	public static Component decorateChatMessage(Component message, String rawMessage, boolean blacklisted) {
		String safe = rawMessage == null ? "" : rawMessage.trim();
		String id = MessageFlagging.registerMessage(safe);
		MutableComponent hover = Component.literal("CTRL+Y = legit\nCTRL+N = scam").withStyle(ChatFormatting.YELLOW);
		MutableComponent wrapped = message == null ? Component.empty() : message.copy();
		Style extra = Style.EMPTY
			.withHoverEvent(new HoverEvent.ShowText(hover))
			.withClickEvent(new ClickEvent.CopyToClipboard(MessageFlagging.clickValue(id)));
		applyStyleRecursive(wrapped, extra, blacklisted ? safe : null, blacklisted ? ChatFormatting.RED : null);
		return wrapped;
	}


	public static Component rebuildChatMessage(Component message, ChatLineParser.ParsedPlayerLine parsed) {
		String safe = parsed == null || parsed.message() == null ? "" : parsed.message().trim();
		String id = MessageFlagging.registerMessage(safe);
		MutableComponent hover = Component.literal("CTRL+Y = legit\nCTRL+N = scam").withStyle(ChatFormatting.YELLOW);
		Style clickStyle = Style.EMPTY
			.withHoverEvent(new HoverEvent.ShowText(hover))
			.withClickEvent(new ClickEvent.CopyToClipboard(MessageFlagging.clickValue(id)));

		List<StyledSegment> segments = flattenSegments(message);
		MutableComponent out = Component.empty();
		boolean seenColon = false;
		for (StyledSegment segment : segments) {
			String text = segment.text();
			if (text == null || text.isEmpty()) {
				continue;
			}
			Style base = segment.style()
				.withHoverEvent(clickStyle.getHoverEvent())
				.withClickEvent(clickStyle.getClickEvent());

			if (!seenColon) {
				int colon = text.indexOf(':');
				if (colon < 0) {
					out.append(Component.literal(text).withStyle(base));
					continue;
				}
				String before = text.substring(0, colon + 1);
				out.append(Component.literal(before).withStyle(base));
				String after = text.substring(colon + 1);
				if (!after.isEmpty()) {
					Style red = base.withColor(ChatFormatting.RED);
					out.append(Component.literal(after).withStyle(red));
				}
				seenColon = true;
				continue;
			}

			out.append(Component.literal(text).withStyle(base.withColor(ChatFormatting.RED)));
		}
		return out;
	}

	public static Component decoratePlayerLine(Component message, ChatLineParser.ParsedPlayerLine parsed, boolean blacklisted) {
		if (blacklisted) {
			return rebuildChatMessage(message, parsed);
		}
		String raw = parsed == null ? "" : parsed.message();
		return decorateChatMessage(message, raw, false);
	}

	private static List<StyledSegment> flattenSegments(Component component) {
		if (component == null) {
			return List.of();
		}
		List<StyledSegment> segments = new ArrayList<>();
		component.visit((style, text) -> {
			if (text != null && !text.isEmpty()) {
				segments.add(new StyledSegment(text, style));
			}
			return java.util.Optional.empty();
		}, Style.EMPTY);
		return segments;
	}

	private static void applyStyleRecursive(MutableComponent component, Style extra, String messageText, ChatFormatting defaultColor) {
		if (component == null) {
			return;
		}
		Style merged = component.getStyle()
			.withHoverEvent(extra.getHoverEvent())
			.withClickEvent(extra.getClickEvent());
		if (defaultColor != null) {
			if (shouldColorMessagePart(component, messageText)) {
				merged = merged.withColor(defaultColor);
			} else if (merged.getColor() == null) {
				merged = merged.withColor(defaultColor);
			}
		}
		component.setStyle(merged);
		for (Component sibling : component.getSiblings()) {
			if (sibling instanceof MutableComponent mutable) {
				applyStyleRecursive(mutable, extra, messageText, defaultColor);
			}
		}
	}

	private static boolean shouldColorMessagePart(MutableComponent component, String messageText) {
		if (messageText == null || messageText.isBlank()) {
			return false;
		}
		if (!component.getSiblings().isEmpty()) {
			return false;
		}
		String piece = component.getString();
		if (piece == null || piece.isBlank()) {
			return false;
		}
		String normalizedMessage = TextUtil.normalizeForMatch(messageText);
		String normalizedPiece = TextUtil.normalizeForMatch(piece);
		if (normalizedPiece.isBlank()) {
			return false;
		}
		return normalizedMessage.contains(normalizedPiece);
	}

	private record StyledSegment(String text, Style style) {
	}
}
