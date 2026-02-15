package eu.tango.scamscreener.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UploadTosScreen extends GUI {
	private static final Pattern ORDERED_LIST_PATTERN = Pattern.compile("^(\\d+)\\.\\s+(.+)$");
	private static final int BOX_PADDING = 6;

	private final String markdownText;
	private final Runnable onAccept;
	private final List<FormattedCharSequence> wrappedLines = new ArrayList<>();
	private int textAreaX;
	private int textAreaY;
	private int textAreaWidth;
	private int textAreaHeight;
	private int scrollOffset;
	private int maxScroll;

	public UploadTosScreen(Screen parent, String markdownText, Runnable onAccept) {
		super(Component.literal("Training Upload Terms of Service"), parent);
		this.markdownText = markdownText == null ? "" : markdownText;
		this.onAccept = onAccept;
	}

	@Override
	protected void init() {
		int buttonWidth = defaultButtonWidth();
		int buttonsX = centeredX(buttonWidth);
		int buttonsY = this.height - FOOTER_Y_OFFSET - 24;

		int half = halfWidth(buttonWidth);
		this.addRenderableWidget(Button.builder(Component.literal("Accept"), button -> {
			if (onAccept != null) {
				onAccept.run();
			}
			this.onClose();
		}).bounds(columnX(buttonsX, half, 0), buttonsY, half, 20).build());
		this.addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> this.onClose())
			.bounds(columnX(buttonsX, half, 1), buttonsY, half, 20)
			.build());

		textAreaWidth = buttonWidth;
		textAreaX = buttonsX;
		textAreaY = CONTENT_START_Y;
		textAreaHeight = Math.max(80, buttonsY - textAreaY - 10);

		rebuildWrappedLines();
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		super.render(guiGraphics, mouseX, mouseY, partialTick);
		renderTextBox(guiGraphics);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (isInsideTextArea(mouseX, mouseY) && maxScroll > 0) {
			scrollBy(scrollY > 0 ? -3 : 3);
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public boolean keyPressed(KeyEvent keyEvent) {
		if (keyEvent == null) {
			return false;
		}
		int keyCode = keyEvent.key();
		if (keyCode == GLFW.GLFW_KEY_UP) {
			scrollBy(-1);
			return true;
		}
		if (keyCode == GLFW.GLFW_KEY_DOWN) {
			scrollBy(1);
			return true;
		}
		if (keyCode == GLFW.GLFW_KEY_PAGE_UP) {
			scrollBy(-visibleLineCount());
			return true;
		}
		if (keyCode == GLFW.GLFW_KEY_PAGE_DOWN) {
			scrollBy(visibleLineCount());
			return true;
		}
		if (keyCode == GLFW.GLFW_KEY_HOME) {
			setScrollOffset(0);
			return true;
		}
		if (keyCode == GLFW.GLFW_KEY_END) {
			setScrollOffset(maxScroll);
			return true;
		}
		return super.keyPressed(keyEvent);
	}

	private void renderTextBox(GuiGraphics guiGraphics) {
		guiGraphics.fill(textAreaX, textAreaY, textAreaX + textAreaWidth, textAreaY + textAreaHeight, 0xA0101010);
		guiGraphics.fill(textAreaX, textAreaY, textAreaX + textAreaWidth, textAreaY + 1, 0xFF5A5A5A);
		guiGraphics.fill(textAreaX, textAreaY + textAreaHeight - 1, textAreaX + textAreaWidth, textAreaY + textAreaHeight, 0xFF5A5A5A);
		guiGraphics.fill(textAreaX, textAreaY, textAreaX + 1, textAreaY + textAreaHeight, 0xFF5A5A5A);
		guiGraphics.fill(textAreaX + textAreaWidth - 1, textAreaY, textAreaX + textAreaWidth, textAreaY + textAreaHeight, 0xFF5A5A5A);

		int lineHeight = this.font.lineHeight + 1;
		int startX = textAreaX + BOX_PADDING;
		int startY = textAreaY + BOX_PADDING;
		int visible = visibleLineCount();
		for (int line = 0; line < visible; line++) {
			int lineIndex = scrollOffset + line;
			if (lineIndex >= wrappedLines.size()) {
				break;
			}
			guiGraphics.drawString(this.font, wrappedLines.get(lineIndex), startX, startY + line * lineHeight, 0xFFE6E6E6, false);
		}

		if (maxScroll > 0) {
			int trackLeft = textAreaX + textAreaWidth - 4;
			int trackTop = textAreaY + 2;
			int trackBottom = textAreaY + textAreaHeight - 2;
			int trackHeight = Math.max(1, trackBottom - trackTop);
			guiGraphics.fill(trackLeft, trackTop, trackLeft + 2, trackBottom, 0xFF3A3A3A);

			int thumbHeight = Math.max(10, (int) (trackHeight * (visibleLineCount() / (double) wrappedLines.size())));
			int thumbRange = Math.max(1, trackHeight - thumbHeight);
			int thumbTop = trackTop + (int) Math.round((scrollOffset / (double) maxScroll) * thumbRange);
			guiGraphics.fill(trackLeft, thumbTop, trackLeft + 2, thumbTop + thumbHeight, 0xFFC8C8C8);
		}
	}

	private void rebuildWrappedLines() {
		wrappedLines.clear();
		int wrapWidth = Math.max(80, textAreaWidth - (BOX_PADDING * 2) - 6);
		List<Component> markdownLines = parseMarkdownLines(markdownText);
		for (Component line : markdownLines) {
			List<FormattedCharSequence> split = this.font.split(line, wrapWidth);
			if (split.isEmpty()) {
				split = this.font.split(Component.literal(" "), wrapWidth);
			}
			wrappedLines.addAll(split);
		}
		updateScrollBounds();
	}

	private int visibleLineCount() {
		int lineHeight = this.font.lineHeight + 1;
		return Math.max(1, (textAreaHeight - (BOX_PADDING * 2)) / lineHeight);
	}

	private void updateScrollBounds() {
		maxScroll = Math.max(0, wrappedLines.size() - visibleLineCount());
		scrollOffset = clamp(scrollOffset, 0, maxScroll);
	}

	private void scrollBy(int delta) {
		setScrollOffset(scrollOffset + delta);
	}

	private void setScrollOffset(int value) {
		scrollOffset = clamp(value, 0, maxScroll);
	}

	private static int clamp(int value, int min, int max) {
		if (value < min) {
			return min;
		}
		if (value > max) {
			return max;
		}
		return value;
	}

	private boolean isInsideTextArea(double mouseX, double mouseY) {
		return mouseX >= textAreaX
			&& mouseX <= textAreaX + textAreaWidth
			&& mouseY >= textAreaY
			&& mouseY <= textAreaY + textAreaHeight;
	}

	private static List<Component> parseMarkdownLines(String markdown) {
		String normalized = markdown == null ? "" : markdown.replace("\r\n", "\n").replace('\r', '\n');
		String[] lines = normalized.split("\n", -1);
		List<Component> result = new ArrayList<>(lines.length);
		for (String rawLine : lines) {
			String line = rawLine == null ? "" : rawLine.stripTrailing();
			String trimmed = line.trim();
			if (trimmed.isEmpty()) {
				result.add(Component.literal(" "));
				continue;
			}
			if (isHorizontalRule(trimmed)) {
				result.add(Component.literal("------------------------------------------------------------").withStyle(ChatFormatting.DARK_GRAY));
				continue;
			}
			if (trimmed.startsWith("### ")) {
				result.add(applyHeadingStyle(parseInlineMarkdown(trimmed.substring(4)), ChatFormatting.YELLOW));
				continue;
			}
			if (trimmed.startsWith("## ")) {
				result.add(applyHeadingStyle(parseInlineMarkdown(trimmed.substring(3)), ChatFormatting.GOLD));
				continue;
			}
			if (trimmed.startsWith("# ")) {
				result.add(applyHeadingStyle(parseInlineMarkdown(trimmed.substring(2)), ChatFormatting.RED));
				continue;
			}

			Matcher ordered = ORDERED_LIST_PATTERN.matcher(trimmed);
			if (ordered.matches()) {
				MutableComponent listLine = Component.literal(ordered.group(1) + ". ").withStyle(ChatFormatting.GRAY);
				listLine.append(parseInlineMarkdown(ordered.group(2)));
				result.add(listLine);
				continue;
			}
			if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
				MutableComponent listLine = Component.literal("- ").withStyle(ChatFormatting.GRAY);
				listLine.append(parseInlineMarkdown(trimmed.substring(2).trim()));
				result.add(listLine);
				continue;
			}

			result.add(parseInlineMarkdown(trimmed));
		}
		return result;
	}

	private static boolean isHorizontalRule(String line) {
		if (line.length() < 3) {
			return false;
		}
		char first = line.charAt(0);
		if (first != '-' && first != '_' && first != '*') {
			return false;
		}
		for (int i = 1; i < line.length(); i++) {
			if (line.charAt(i) != first) {
				return false;
			}
		}
		return true;
	}

	private static MutableComponent applyHeadingStyle(MutableComponent text, ChatFormatting color) {
		return Component.empty().append(text).withStyle(color, ChatFormatting.BOLD);
	}

	private static MutableComponent parseInlineMarkdown(String text) {
		MutableComponent out = Component.empty();
		if (text == null || text.isEmpty()) {
			return out;
		}
		int index = 0;
		while (index < text.length()) {
			if (text.startsWith("**", index)) {
				int end = text.indexOf("**", index + 2);
				if (end > index + 2) {
					out.append(Component.empty()
						.append(parseInlineMarkdown(text.substring(index + 2, end)))
						.withStyle(ChatFormatting.BOLD));
					index = end + 2;
					continue;
				}
			}
			if (text.charAt(index) == '*') {
				int end = text.indexOf('*', index + 1);
				if (end > index + 1) {
					out.append(Component.empty()
						.append(parseInlineMarkdown(text.substring(index + 1, end)))
						.withStyle(ChatFormatting.ITALIC));
					index = end + 1;
					continue;
				}
			}
			if (text.charAt(index) == '`') {
				int end = text.indexOf('`', index + 1);
				if (end > index + 1) {
					out.append(Component.literal(text.substring(index + 1, end)).withStyle(ChatFormatting.GOLD));
					index = end + 1;
					continue;
				}
			}
			if (text.charAt(index) == '[') {
				int closeLabel = text.indexOf(']', index + 1);
				if (closeLabel > index + 1 && closeLabel + 1 < text.length() && text.charAt(closeLabel + 1) == '(') {
					int closeUrl = text.indexOf(')', closeLabel + 2);
					if (closeUrl > closeLabel + 2) {
						String label = text.substring(index + 1, closeLabel);
						String url = text.substring(closeLabel + 2, closeUrl).trim();
						if (!url.isEmpty()) {
							Style linkStyle = Style.EMPTY
								.withColor(ChatFormatting.AQUA)
								.withUnderlined(true)
								.withHoverEvent(new HoverEvent.ShowText(Component.literal(url)));
							try {
								linkStyle = linkStyle.withClickEvent(new ClickEvent.OpenUrl(URI.create(url)));
							} catch (IllegalArgumentException ignored) {
							}
							out.append(Component.literal(label).setStyle(linkStyle));
							index = closeUrl + 1;
							continue;
						}
					}
				}
			}

			int next = findNextMarker(text, index);
			if (next <= index) {
				out.append(Component.literal(String.valueOf(text.charAt(index))));
				index++;
			} else {
				out.append(Component.literal(text.substring(index, next)));
				index = next;
			}
		}
		return out;
	}

	private static int findNextMarker(String text, int fromIndex) {
		int next = text.length();
		int star = text.indexOf('*', fromIndex);
		if (star >= 0 && star < next) {
			next = star;
		}
		int backtick = text.indexOf('`', fromIndex);
		if (backtick >= 0 && backtick < next) {
			next = backtick;
		}
		int bracket = text.indexOf('[', fromIndex);
		if (bracket >= 0 && bracket < next) {
			next = bracket;
		}
		return next;
	}
}
