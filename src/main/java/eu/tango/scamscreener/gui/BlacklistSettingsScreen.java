package eu.tango.scamscreener.gui;

import eu.tango.scamscreener.blacklist.BlacklistManager;
import eu.tango.scamscreener.rules.ScamRules;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

final class BlacklistSettingsScreen extends GUI {
	private static final int ENTRIES_PER_PAGE = 7;
	private static final int SCORE_STEP = 10;

	private final BlacklistManager blacklistManager;
	private final List<Button> entryButtons = new ArrayList<>();
	private final List<BlacklistManager.ScamEntry> pageEntries = new ArrayList<>();

	private Button previousPageButton;
	private Button nextPageButton;
	private Button scoreDownButton;
	private Button scoreUpButton;
	private Button setReasonButton;
	private Button removeButton;
	private CycleButton<ScamRules.ScamRule> ruleReasonDropdown;
	private EditBox reasonInput;
	private int page;
	private UUID selectedUuid;
	private int totalPages = 1;

	BlacklistSettingsScreen(Screen parent, BlacklistManager blacklistManager) {
		super(Component.literal("ScamScreener Blacklist"), parent);
		this.blacklistManager = blacklistManager;
	}

	@Override
	protected void init() {
		entryButtons.clear();
		pageEntries.clear();
		int buttonWidth = Math.min(420, Math.max(220, this.width - 30));
		int x = centeredX(buttonWidth);
		int y = 34;

		for (int i = 0; i < ENTRIES_PER_PAGE; i++) {
			final int index = i;
			Button rowButton = this.addRenderableWidget(Button.builder(Component.literal("-"), button -> selectEntry(index))
				.bounds(x, y, buttonWidth, 20)
				.build());
			entryButtons.add(rowButton);
			y += ROW_HEIGHT;
		}

		int half = (buttonWidth - 8) / 2;
		previousPageButton = this.addRenderableWidget(Button.builder(Component.literal("< Previous"), button -> {
			page = Math.max(0, page - 1);
			refreshList();
		}).bounds(x, y, half, 20).build());

		nextPageButton = this.addRenderableWidget(Button.builder(Component.literal("Next >"), button -> {
			page = Math.min(Math.max(0, totalPages - 1), page + 1);
			refreshList();
		}).bounds(x + half + 8, y, half, 20).build());
		y += ROW_HEIGHT;

		int third = (buttonWidth - 16) / 3;
		scoreDownButton = this.addRenderableWidget(Button.builder(Component.literal("Score -" + SCORE_STEP).withStyle(style -> style.withColor(OFF_LIGHT_RED)), button -> {
			adjustScore(-SCORE_STEP);
		}).bounds(x, y, third, 20).build());
		scoreUpButton = this.addRenderableWidget(Button.builder(Component.literal("Score +" + SCORE_STEP).withStyle(style -> style.withColor(ON_LIGHT_GREEN)), button -> {
			adjustScore(SCORE_STEP);
		}).bounds(x + third + 8, y, third, 20).build());
		removeButton = this.addRenderableWidget(Button.builder(Component.literal("Remove"), button -> {
			removeSelected();
		}).bounds(x + (third + 8) * 2, y, third, 20).build());
		y += ROW_HEIGHT;

		int reasonButtonWidth = 52;
		int dropdownWidth = 120;
		int reasonInputWidth = buttonWidth - reasonButtonWidth - dropdownWidth - 16;
		reasonInput = this.addRenderableWidget(new EditBox(this.font, x, y, reasonInputWidth, 20, Component.literal("Reason")));
		reasonInput.setMaxLength(64);
		//? if >=1.21.11 {
		ruleReasonDropdown = this.addRenderableWidget(CycleButton.<ScamRules.ScamRule>builder(
						rule -> Component.literal(rule.name()),
						() -> ScamRules.ScamRule.SUSPICIOUS_LINK
				).withValues(List.of(ScamRules.ScamRule.values()))
				.create(
						x + reasonInputWidth + 8,
						y,
						dropdownWidth,
						20,
						Component.literal("Rules"),
						(button, value) -> applyRuleReasonFromDropdown(value)
				));
		//?} else {
		/*ruleReasonDropdown = this.addRenderableWidget(CycleButton.<ScamRules.ScamRule>builder(rule -> Component.literal(rule.name()))
			.withValues(List.of(ScamRules.ScamRule.values()))
			.withInitialValue(ScamRules.ScamRule.SUSPICIOUS_LINK)
			.create(
				x + reasonInputWidth + 8,
				y,
				dropdownWidth,
				20,
				Component.literal("Rules"),
				(button, value) -> applyRuleReasonFromDropdown(value)
			));
		*///?}
		setReasonButton = this.addRenderableWidget(Button.builder(Component.literal("Save"), button -> applyReasonInput())
			.bounds(x + reasonInputWidth + dropdownWidth + 16, y, reasonButtonWidth, 20)
			.build());

		addBackButton(buttonWidth);
		refreshList();
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		super.render(guiGraphics, mouseX, mouseY, partialTick);
		String pageLabel = "Page " + (totalPages == 0 ? 0 : page + 1) + "/" + Math.max(1, totalPages);
		guiGraphics.drawCenteredString(this.font, pageLabel, this.width / 2, TITLE_Y + 12, 0xAAAAAA);

		BlacklistManager.ScamEntry selected = selectedEntry();
		String selectedText = selected == null
			? "Selected: none"
			: "Selected: " + selected.name() + " | score " + selected.score() + " | " + selected.reason();
		guiGraphics.drawCenteredString(this.font, selectedText, this.width / 2, this.height - 42, 0xCCCCCC);
	}

	private void selectEntry(int indexOnPage) {
		if (indexOnPage < 0 || indexOnPage >= pageEntries.size()) {
			return;
		}
		BlacklistManager.ScamEntry entry = pageEntries.get(indexOnPage);
		selectedUuid = entry == null ? null : entry.uuid();
		refreshList();
	}

	private void refreshList() {
		List<BlacklistManager.ScamEntry> allEntries = new ArrayList<>(blacklistManager.allEntries());
		totalPages = Math.max(1, (int) Math.ceil(allEntries.size() / (double) ENTRIES_PER_PAGE));
		page = Math.max(0, Math.min(page, totalPages - 1));

		if (selectedUuid != null && blacklistManager.get(selectedUuid) == null) {
			selectedUuid = null;
		}

		pageEntries.clear();
		int start = page * ENTRIES_PER_PAGE;
		for (int i = 0; i < ENTRIES_PER_PAGE; i++) {
			int absolute = start + i;
			Button button = entryButtons.get(i);
			if (absolute >= allEntries.size()) {
				button.active = false;
				button.visible = true;
				button.setMessage(Component.literal("-").withStyle(ChatFormatting.DARK_GRAY));
				continue;
			}
			BlacklistManager.ScamEntry entry = allEntries.get(absolute);
			pageEntries.add(entry);
			button.active = true;
			button.visible = true;
			boolean selected = entry.uuid().equals(selectedUuid);
			button.setMessage(formatEntry(entry, selected));
		}

		if (selectedUuid == null && !allEntries.isEmpty()) {
			selectedUuid = allEntries.get(0).uuid();
			refreshList();
			return;
		}

		previousPageButton.active = page > 0;
		nextPageButton.active = page < totalPages - 1;
		updateActionButtons();
	}

	private void updateActionButtons() {
		BlacklistManager.ScamEntry selected = selectedEntry();
		boolean hasSelection = selected != null;
		scoreDownButton.active = hasSelection;
		scoreUpButton.active = hasSelection;
		setReasonButton.active = hasSelection;
		removeButton.active = hasSelection;
		ruleReasonDropdown.active = hasSelection;
		if (reasonInput != null) {
			reasonInput.setEditable(hasSelection);
			reasonInput.active = hasSelection;
			String value = hasSelection ? safeReason(selected.reason()) : "";
			if (!value.equals(reasonInput.getValue())) {
				reasonInput.setValue(value);
			}
		}
		if (hasSelection) {
			ScamRules.ScamRule parsed = parseRuleReason(selected.reason());
			if (parsed != null && ruleReasonDropdown != null && ruleReasonDropdown.getValue() != parsed) {
				ruleReasonDropdown.setValue(parsed);
			}
		}
	}

	private BlacklistManager.ScamEntry selectedEntry() {
		if (selectedUuid == null) {
			return null;
		}
		return blacklistManager.get(selectedUuid);
	}

	private void adjustScore(int delta) {
		BlacklistManager.ScamEntry selected = selectedEntry();
		if (selected == null) {
			return;
		}
		int updatedScore = Math.max(0, Math.min(100, selected.score() + delta));
		blacklistManager.update(selected.uuid(), selected.name(), updatedScore, selected.reason());
		refreshList();
	}

	private void applyReasonInput() {
		BlacklistManager.ScamEntry selected = selectedEntry();
		if (selected == null || reasonInput == null) {
			return;
		}
		blacklistManager.update(selected.uuid(), selected.name(), selected.score(), reasonInput.getValue());
		refreshList();
	}

	private void applyRuleReasonFromDropdown(ScamRules.ScamRule value) {
		if (reasonInput == null || value == null) {
			return;
		}
		reasonInput.setValue(value.name());
	}

	private void removeSelected() {
		if (selectedUuid == null) {
			return;
		}
		blacklistManager.remove(selectedUuid);
		selectedUuid = null;
		refreshList();
	}

	private static Component formatEntry(BlacklistManager.ScamEntry entry, boolean selected) {
		if (entry == null) {
			return Component.literal("-").withStyle(ChatFormatting.DARK_GRAY);
		}
		String reason = entry.reason() == null ? "-" : entry.reason();
		if (reason.length() > 22) {
			reason = reason.substring(0, 19) + "...";
		}
		MutableComponent row = Component.empty()
			.append(Component.literal(selected ? "> " : "  ").withStyle(ChatFormatting.WHITE))
			.append(Component.literal(entry.name()).withStyle(ChatFormatting.AQUA))
			.append(Component.literal(" | ").withStyle(ChatFormatting.DARK_GRAY))
			.append(Component.literal(String.valueOf(entry.score())).withStyle(ChatFormatting.DARK_RED))
			.append(Component.literal(" | ").withStyle(ChatFormatting.DARK_GRAY))
			.append(Component.literal(reason).withStyle(ChatFormatting.YELLOW));
		return row;
	}

	private static String safeReason(String reason) {
		return reason == null ? "" : reason;
	}

	private static ScamRules.ScamRule parseRuleReason(String reason) {
		if (reason == null || reason.isBlank()) {
			return null;
		}
		try {
			return ScamRules.ScamRule.valueOf(reason.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException ignored) {
			return null;
		}
	}
}
