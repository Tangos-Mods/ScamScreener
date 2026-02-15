package eu.tango.scamscreener.gui;

import eu.tango.scamscreener.rules.ScamRules;
import eu.tango.scamscreener.ui.AlertReviewRegistry;
import eu.tango.scamscreener.ui.MessageBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AlertInfoScreen extends ScamScreenerGUI {
	private static final int BOX_PADDING = 6;

	private final AlertReviewRegistry.AlertContext alertContext;
	private final List<String> capturedMessages;
	private final List<FormattedCharSequence> wrappedLines = new ArrayList<>();
	private int textAreaX;
	private int textAreaY;
	private int textAreaWidth;
	private int textAreaHeight;
	private int scrollOffset;
	private int maxScroll;

	public AlertInfoScreen(Screen parent, AlertReviewRegistry.AlertContext alertContext) {
		this(parent, alertContext, alertContext == null ? List.of() : alertContext.evaluatedMessages());
	}

	public AlertInfoScreen(Screen parent, AlertReviewRegistry.AlertContext alertContext, List<String> capturedMessages) {
		super(Component.literal("Alert Rule Details"), parent);
		this.alertContext = alertContext;
		this.capturedMessages = normalizeMessages(capturedMessages);
	}

	@Override
	protected void init() {
		int buttonWidth = defaultButtonWidth();
		int buttonsX = centeredX(buttonWidth);
		int buttonsY = this.height - FOOTER_Y_OFFSET - 24;

		this.addRenderableWidget(Button.builder(Component.literal("Close"), button -> this.onClose())
			.bounds(buttonsX, buttonsY, buttonWidth, 20)
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
	public boolean keyPressed(net.minecraft.client.input.KeyEvent keyEvent) {
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
		for (Component line : buildContextLines(alertContext, capturedMessages)) {
			List<FormattedCharSequence> split = this.font.split(line, wrapWidth);
			if (split.isEmpty()) {
				split = this.font.split(Component.literal(" "), wrapWidth);
			}
			wrappedLines.addAll(split);
		}
		updateScrollBounds();
	}

	private static List<Component> buildContextLines(AlertReviewRegistry.AlertContext context, List<String> capturedMessages) {
		List<Component> out = new ArrayList<>();
		if (context == null) {
			out.add(Component.literal("Alert details are unavailable.").withStyle(ChatFormatting.GRAY));
			return out;
		}

		String safePlayer = context.playerName() == null || context.playerName().isBlank() ? "unknown" : context.playerName().trim();
		out.add(Component.literal("Player: ").withStyle(ChatFormatting.GRAY).append(Component.literal(safePlayer).withStyle(ChatFormatting.AQUA)));
		out.add(Component.literal("Risk: ").withStyle(ChatFormatting.GRAY)
			.append(Component.literal(context.riskLevel().name()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
			.append(Component.literal(" | Score: ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(String.valueOf(Math.max(0, context.riskScore()))).withStyle(ChatFormatting.GOLD)));
		out.add(Component.literal(" "));
		out.add(Component.literal("Triggered Rules").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));

		if (context.triggeredRules().isEmpty()) {
			out.add(Component.literal("- none").withStyle(ChatFormatting.DARK_GRAY));
		} else {
			Map<RuleStage, List<ScamRules.ScamRule>> groupedRules = new EnumMap<>(RuleStage.class);
			for (ScamRules.ScamRule rule : context.triggeredRules()) {
				RuleStage stage = RuleStage.of(rule);
				groupedRules.computeIfAbsent(stage, ignored -> new ArrayList<>()).add(rule);
			}
			for (RuleStage stage : RuleStage.values()) {
				List<ScamRules.ScamRule> rules = groupedRules.get(stage);
				if (rules == null || rules.isEmpty()) {
					continue;
				}
				rules.sort(Comparator.naturalOrder());
				out.add(Component.literal(stage.label()).withStyle(stage.color(), ChatFormatting.BOLD));
				for (ScamRules.ScamRule rule : rules) {
					String detail = context.ruleDetails().get(rule);
					if (detail == null || detail.isBlank()) {
						detail = "No detailed trigger context available.";
					}
					out.add(Component.literal("  - " + MessageBuilder.readableRuleName(rule)).withStyle(ChatFormatting.GOLD));
					for (String detailLine : detail.split("\\R")) {
						String normalized = detailLine == null ? "" : detailLine.trim();
						if (normalized.isEmpty()) {
							continue;
						}
						out.add(Component.literal("      " + normalized).withStyle(ChatFormatting.GRAY));
					}
				}
			}
		}

		out.add(Component.literal(" "));
		out.add(Component.literal("Captured Messages").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
		if (capturedMessages == null || capturedMessages.isEmpty()) {
			out.add(Component.literal("- none").withStyle(ChatFormatting.DARK_GRAY));
		} else {
			for (String normalized : normalizeMessages(capturedMessages)) {
				out.add(Component.literal("- " + normalized).withStyle(ChatFormatting.AQUA));
			}
		}
		return out;
	}

	private static List<String> normalizeMessages(List<String> input) {
		if (input == null || input.isEmpty()) {
			return List.of();
		}
		Set<String> unique = new LinkedHashSet<>();
		for (String message : input) {
			String normalized = message == null ? "" : message.replace('\n', ' ').replace('\r', ' ').trim();
			if (!normalized.isBlank()) {
				unique.add(normalized);
			}
		}
		return new ArrayList<>(unique);
	}

	private enum RuleStage {
		RULE("Rule Stage", ChatFormatting.YELLOW),
		BEHAVIOR("Behavior Stage", ChatFormatting.AQUA),
		SIMILARITY("Similarity Stage", ChatFormatting.LIGHT_PURPLE),
		TREND("Trend Stage", ChatFormatting.BLUE),
		FUNNEL("Funnel Stage", ChatFormatting.DARK_AQUA),
		AI("AI Stage", ChatFormatting.GREEN),
		OTHER("Other Stage", ChatFormatting.DARK_GRAY);

		private final String label;
		private final ChatFormatting color;

		RuleStage(String label, ChatFormatting color) {
			this.label = label;
			this.color = color;
		}

		private String label() {
			return label;
		}

		private ChatFormatting color() {
			return color;
		}

		private static RuleStage of(ScamRules.ScamRule rule) {
			if (rule == null) {
				return OTHER;
			}
			return switch (rule) {
				case SUSPICIOUS_LINK,
					PRESSURE_AND_URGENCY,
					UPFRONT_PAYMENT,
					ACCOUNT_DATA_REQUEST,
					DISCORD_HANDLE,
					TOO_GOOD_TO_BE_TRUE,
					TRUST_MANIPULATION -> RULE;
				case EXTERNAL_PLATFORM_PUSH,
					FAKE_MIDDLEMAN_CLAIM,
					SPAMMY_CONTACT_PATTERN -> BEHAVIOR;
				case SIMILARITY_MATCH -> SIMILARITY;
				case MULTI_MESSAGE_PATTERN -> TREND;
				case FUNNEL_SEQUENCE_PATTERN -> FUNNEL;
				case LOCAL_AI_RISK_SIGNAL,
					LOCAL_AI_FUNNEL_SIGNAL -> AI;
			};
		}
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
}
