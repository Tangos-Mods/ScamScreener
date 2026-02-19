package eu.tango.scamscreener.gui;

import eu.tango.scamscreener.lookup.ResolvedTarget;
import eu.tango.scamscreener.whitelist.WhitelistManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

public final class WhitelistSettingsScreen extends ScamScreenerGUI {
	private static final int ENTRIES_PER_PAGE = 8;

	private final WhitelistManager whitelistManager;
	private final Function<String, ResolvedTarget> targetResolver;
	private final List<Button> entryButtons = new ArrayList<>();
	private final List<WhitelistManager.WhitelistEntry> pageEntries = new ArrayList<>();

	private Button previousPageButton;
	private Button nextPageButton;
	private Button removeButton;
	private EditBox addInput;
	private int page;
	private int totalPages = 1;
	private UUID selectedUuid;

	public WhitelistSettingsScreen(
		Screen parent,
		WhitelistManager whitelistManager,
		Function<String, ResolvedTarget> targetResolver
	) {
		super(Component.literal("ScamScreener Whitelist"), parent);
		this.whitelistManager = whitelistManager;
		this.targetResolver = targetResolver;
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

		int half = halfWidth(buttonWidth);
		previousPageButton = this.addRenderableWidget(Button.builder(Component.literal("< Previous"), button -> {
			page = Math.max(0, page - 1);
			refreshList();
		}).bounds(columnX(x, half, 0), y, half, 20).build());

		nextPageButton = this.addRenderableWidget(Button.builder(Component.literal("Next >"), button -> {
			page = Math.min(Math.max(0, totalPages - 1), page + 1);
			refreshList();
		}).bounds(columnX(x, half, 1), y, half, 20).build());
		y += ROW_HEIGHT;

		int addButtonWidth = 72;
		int addInputWidth = buttonWidth - addButtonWidth - 8;
		addInput = this.addRenderableWidget(new EditBox(this.font, x, y, addInputWidth, 20, Component.literal("Player Name")));
		addInput.setMaxLength(32);
		this.addRenderableWidget(Button.builder(Component.literal("Add"), button -> addEntryFromInput())
			.bounds(x + addInputWidth + 8, y, addButtonWidth, 20)
			.build());
		y += ROW_HEIGHT;

		removeButton = this.addRenderableWidget(Button.builder(Component.literal("Remove Selected"), button -> removeSelected())
			.bounds(x, y, buttonWidth, 20)
			.build());

		addBackButton(buttonWidth);
		refreshList();
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		super.render(guiGraphics, mouseX, mouseY, partialTick);
		String pageLabel = "Page " + (totalPages == 0 ? 0 : page + 1) + "/" + Math.max(1, totalPages);
		guiGraphics.drawCenteredString(this.font, pageLabel, this.width / 2, TITLE_Y + 12, 0xAAAAAA);

		WhitelistManager.WhitelistEntry selected = selectedEntry();
		String selectedText = selected == null
			? "Selected: none"
			: "Selected: " + selected.name();
		guiGraphics.drawCenteredString(this.font, selectedText, this.width / 2, this.height - 42, 0xCCCCCC);
	}

	private void selectEntry(int indexOnPage) {
		if (indexOnPage < 0 || indexOnPage >= pageEntries.size()) {
			return;
		}
		WhitelistManager.WhitelistEntry entry = pageEntries.get(indexOnPage);
		selectedUuid = entry == null ? null : entry.uuid();
		refreshList();
	}

	private void refreshList() {
		List<WhitelistManager.WhitelistEntry> allEntries = new ArrayList<>(whitelistManager.allEntries());
		totalPages = Math.max(1, (int) Math.ceil(allEntries.size() / (double) ENTRIES_PER_PAGE));
		page = Math.max(0, Math.min(page, totalPages - 1));

		if (selectedUuid != null && whitelistManager.get(selectedUuid) == null) {
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
			WhitelistManager.WhitelistEntry entry = allEntries.get(absolute);
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
		removeButton.active = selectedUuid != null;
	}

	private void addEntryFromInput() {
		if (addInput == null || targetResolver == null) {
			return;
		}
		String raw = addInput.getValue();
		if (raw == null || raw.isBlank()) {
			return;
		}
		ResolvedTarget target = targetResolver.apply(raw.trim());
		if (target == null) {
			return;
		}

		WhitelistManager.AddOrUpdateResult result = whitelistManager.addOrUpdate(target.uuid(), target.name());
		if (result == WhitelistManager.AddOrUpdateResult.INVALID) {
			return;
		}
		selectedUuid = target.uuid();
		addInput.setValue("");
		refreshList();
	}

	private WhitelistManager.WhitelistEntry selectedEntry() {
		if (selectedUuid == null) {
			return null;
		}
		return whitelistManager.get(selectedUuid);
	}

	private void removeSelected() {
		WhitelistManager.WhitelistEntry selected = selectedEntry();
		if (selected == null) {
			return;
		}
		whitelistManager.remove(selected.uuid());
		selectedUuid = null;
		refreshList();
	}

	private static Component formatEntry(WhitelistManager.WhitelistEntry entry, boolean selected) {
		if (entry == null) {
			return Component.literal("-").withStyle(ChatFormatting.DARK_GRAY);
		}
		MutableComponent row = Component.empty()
			.append(Component.literal(selected ? "> " : "  ").withStyle(ChatFormatting.WHITE))
			.append(Component.literal(entry.name()).withStyle(ChatFormatting.AQUA))
			.append(Component.literal(" | ").withStyle(ChatFormatting.DARK_GRAY))
			.append(Component.literal(entry.uuid().toString()).withStyle(ChatFormatting.GRAY));
		return row;
	}
}
