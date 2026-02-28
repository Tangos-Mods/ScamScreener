package eu.tango.scamscreener.gui;

import eu.tango.scamscreener.ui.AlertReviewRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class AlertManageScreen extends ScamScreenerGUI {
	private static final int AUTO_REFRESH_INTERVAL_TICKS = 20;
	private static final int LIST_ROW_HEIGHT = 20;
	private static final int CHECKBOX_HEIGHT = 20;
	private static final int LIST_TOP = 48;
	private static final int LIST_BOTTOM_PADDING = 8;
	private static final int CHECKBOX_GAP = 4;
	private static final int ACTION_BUTTON_GAP = 12;

	private final AlertReviewRegistry.AlertContext alertContext;
	private final List<ReviewRow> sourceRows;
	private final boolean showPlayerActions;
	private final List<SelectionState> states = new ArrayList<>();
	private final SubmitHandler submitHandler;
	private final Runnable openFileHandler;
	private final Supplier<List<ReviewRow>> refreshRowsSupplier;

	private int listX;
	private int listY;
	private int listWidth;
	private int listHeight;
	private int maxVisibleRows;
	private int scrollOffsetRows;

	private SimpleCheckbox blacklistCheckbox;
	private SimpleCheckbox blockCheckbox;

	public AlertManageScreen(
		Screen parent,
		AlertReviewRegistry.AlertContext alertContext,
		List<ReviewRow> sourceRows,
		SubmitHandler submitHandler
	) {
		this(
			parent,
			Component.literal("Manage Alert"),
			alertContext,
			sourceRows,
			true,
			submitHandler,
			null,
			null
		);
	}

	public AlertManageScreen(
		Screen parent,
		Component title,
		List<ReviewRow> sourceRows,
		SubmitHandler submitHandler
	) {
		this(parent, title, sourceRows, submitHandler, null, null);
	}

	public AlertManageScreen(
		Screen parent,
		Component title,
		List<ReviewRow> sourceRows,
		SubmitHandler submitHandler,
		Supplier<List<ReviewRow>> refreshRowsSupplier
	) {
		this(parent, title, sourceRows, submitHandler, null, refreshRowsSupplier);
	}

	public AlertManageScreen(
		Screen parent,
		Component title,
		List<ReviewRow> sourceRows,
		SubmitHandler submitHandler,
		Runnable openFileHandler
	) {
		this(parent, title, sourceRows, submitHandler, openFileHandler, null);
	}

	public AlertManageScreen(
		Screen parent,
		Component title,
		List<ReviewRow> sourceRows,
		SubmitHandler submitHandler,
		Runnable openFileHandler,
		Supplier<List<ReviewRow>> refreshRowsSupplier
	) {
		this(
			parent,
			title == null ? Component.literal("Review Training CSV") : title,
			null,
			sourceRows,
			false,
			submitHandler,
			openFileHandler,
			refreshRowsSupplier
		);
	}

	private AlertManageScreen(
		Screen parent,
		Component title,
		AlertReviewRegistry.AlertContext alertContext,
		List<ReviewRow> sourceRows,
		boolean showPlayerActions,
		SubmitHandler submitHandler,
		Runnable openFileHandler,
		Supplier<List<ReviewRow>> refreshRowsSupplier
	) {
		super(title == null ? Component.literal("Manage Alert") : title, parent);
		this.alertContext = alertContext;
		this.sourceRows = new ArrayList<>(sanitizeRows(sourceRows));
		this.showPlayerActions = showPlayerActions;
		this.submitHandler = submitHandler;
		this.openFileHandler = openFileHandler;
		this.refreshRowsSupplier = refreshRowsSupplier;
		for (int i = 0; i < this.sourceRows.size(); i++) {
			states.add(SelectionState.fromReviewRow(this.sourceRows.get(i)));
		}
	}

	@Override
	public void tick() {
		super.tick();
		if (refreshRowsSupplier == null || !isPeriodicTick(AUTO_REFRESH_INTERVAL_TICKS)) {
			return;
		}
		try {
			refreshRows(refreshRowsSupplier.get());
		} catch (Exception ignored) {
		}
	}

	@Override
	protected void init() {
		int contentWidth = Math.min(620, Math.max(260, this.width - 30));
		int x = centeredX(contentWidth);
		int actionY = this.height - FOOTER_Y_OFFSET - 24;

		listX = x;
		listY = LIST_TOP;
		listWidth = contentWidth;

		int listBottom = actionY;
		if (showPlayerActions) {
			int checkboxY = actionY - CHECKBOX_HEIGHT * 2 - CHECKBOX_GAP;
			listBottom = checkboxY;
			blacklistCheckbox = new SimpleCheckbox(
				x,
				checkboxY,
				contentWidth,
				CHECKBOX_HEIGHT,
				Component.literal("Add player to ScamScreener blacklist"),
				false
			);
			blockCheckbox = new SimpleCheckbox(
				x,
				checkboxY + CHECKBOX_HEIGHT + CHECKBOX_GAP,
				contentWidth,
				CHECKBOX_HEIGHT,
				Component.literal("Add player to Hypixel /block list"),
				false
			);
		} else {
			blacklistCheckbox = null;
			blockCheckbox = null;
		}

		listHeight = Math.max(80, listBottom - listY - LIST_BOTTOM_PADDING);
		maxVisibleRows = Math.max(1, listHeight / LIST_ROW_HEIGHT);

		if (openFileHandler != null) {
			int quarter = localSplitWidth(contentWidth, 4, ACTION_BUTTON_GAP);
			this.addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> this.onClose())
				.bounds(localColumnX(x, quarter, 0), actionY, quarter, 20)
				.build());
			this.addRenderableWidget(Button.builder(Component.literal("Open File"), button -> openFileHandler.run())
				.bounds(localColumnX(x, quarter, 1), actionY, quarter, 20)
				.build());
			this.addRenderableWidget(Button.builder(Component.literal("Save"), button -> submit(false))
				.bounds(localColumnX(x, quarter, 2), actionY, quarter, 20)
				.build());
			this.addRenderableWidget(Button.builder(Component.literal("Save & Upload"), button -> submit(true))
				.bounds(localColumnX(x, quarter, 3), actionY, quarter, 20)
				.build());
		} else {
			int third = localSplitWidth(contentWidth, 3, ACTION_BUTTON_GAP);
			this.addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> this.onClose())
				.bounds(localColumnX(x, third, 0), actionY, third, 20)
				.build());
			this.addRenderableWidget(Button.builder(Component.literal("Save"), button -> submit(false))
				.bounds(localColumnX(x, third, 1), actionY, third, 20)
				.build());
			this.addRenderableWidget(Button.builder(Component.literal("Save & Upload"), button -> submit(true))
				.bounds(localColumnX(x, third, 2), actionY, third, 20)
				.build());
		}
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		super.render(guiGraphics, mouseX, mouseY, partialTick);
		int headerY = TITLE_Y + 12;
		if (alertContext != null) {
			String header = safePlayerName(alertContext.playerName()) + " | score " + Math.max(0, alertContext.riskScore());
			guiGraphics.drawCenteredString(this.font, header, this.width / 2, headerY, opaqueColor(0xCCCCCC));
		}
		renderSelectedSummary(guiGraphics, headerY + this.font.lineHeight + 2);

		renderList(guiGraphics, mouseX, mouseY);
		if (showPlayerActions && blacklistCheckbox != null && blockCheckbox != null) {
			blacklistCheckbox.render(guiGraphics, this.font, mouseX, mouseY);
			blockCheckbox.render(guiGraphics, this.font, mouseX, mouseY);
		}
	}

	//? if <1.21.11 {
	@Override
	public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
		if (event != null && handleMouseClicked(event.x(), event.y(), event.button())) {
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}
	//?} else {
	/*@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (handleMouseClicked(mouseX, mouseY, button)) {
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}
	*///?}

	private boolean handleMouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0 && isInsideList(mouseX, mouseY)) {
			int relativeY = (int) (mouseY - listY);
			int row = relativeY / LIST_ROW_HEIGHT;
			int absolute = scrollOffsetRows + row;
			if (absolute >= 0 && absolute < states.size()) {
				states.set(absolute, states.get(absolute).next());
				return true;
			}
		}
		if (showPlayerActions && blacklistCheckbox != null && blacklistCheckbox.mouseClicked(mouseX, mouseY, button)) {
			return true;
		}
		if (showPlayerActions && blockCheckbox != null && blockCheckbox.mouseClicked(mouseX, mouseY, button)) {
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (isInsideList(mouseX, mouseY) && sourceRows.size() > maxVisibleRows) {
			int delta = scrollY > 0 ? -1 : 1;
			int maxOffset = Math.max(0, sourceRows.size() - maxVisibleRows);
			scrollOffsetRows = Mth.clamp(scrollOffsetRows + delta, 0, maxOffset);
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	private void submit(boolean upload) {
		if (submitHandler == null) {
			return;
		}
		List<ReviewedSelection> selections = new ArrayList<>();
		for (int i = 0; i < sourceRows.size(); i++) {
			SelectionState state = states.get(i);
			ReviewRow row = sourceRows.get(i);
			if (showPlayerActions && !state.hasLabel()) {
				continue;
			}
			if (!showPlayerActions && state.label() == row.currentLabel()) {
				continue;
			}
			selections.add(new ReviewedSelection(row.rowId(), row.message(), state.label()));
		}
		boolean addToBlacklist = showPlayerActions && blacklistCheckbox != null && blacklistCheckbox.checked();
		boolean addToBlock = showPlayerActions && blockCheckbox != null && blockCheckbox.checked();
		int result = submitHandler.submit(new SaveRequest(
			selections,
			upload,
			addToBlacklist,
			addToBlock
		));
		if (result > 0) {
			this.onClose();
		}
	}

	private void refreshRows(List<ReviewRow> updatedRows) {
		List<ReviewRow> sanitized = sanitizeRows(updatedRows);
		Map<String, SelectionState> byRowId = new HashMap<>();
		Map<String, SelectionState> byMessage = new HashMap<>();
		for (int i = 0; i < sourceRows.size() && i < states.size(); i++) {
			ReviewRow row = sourceRows.get(i);
			SelectionState state = states.get(i);
			if (row == null || state == null) {
				continue;
			}
			if (row.rowId() != null && !row.rowId().isBlank()) {
				byRowId.put(row.rowId(), state);
			}
			if (row.message() != null && !row.message().isBlank()) {
				byMessage.putIfAbsent(row.message(), state);
			}
		}

		sourceRows.clear();
		sourceRows.addAll(sanitized);
		states.clear();
		for (ReviewRow row : sourceRows) {
			SelectionState existing = null;
			if (row != null && row.rowId() != null && !row.rowId().isBlank()) {
				existing = byRowId.get(row.rowId());
			}
			if (existing == null && row != null && row.message() != null && !row.message().isBlank()) {
				existing = byMessage.get(row.message());
			}
			states.add(existing == null ? SelectionState.fromReviewRow(row) : existing);
		}

		int maxOffset = Math.max(0, sourceRows.size() - maxVisibleRows);
		scrollOffsetRows = Mth.clamp(scrollOffsetRows, 0, maxOffset);
	}

	private void renderList(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		guiGraphics.fill(listX, listY, listX + listWidth, listY + listHeight, 0xA0101010);
		guiGraphics.fill(listX, listY, listX + listWidth, listY + 1, 0xFF5A5A5A);
		guiGraphics.fill(listX, listY + listHeight - 1, listX + listWidth, listY + listHeight, 0xFF5A5A5A);
		guiGraphics.fill(listX, listY, listX + 1, listY + listHeight, 0xFF5A5A5A);
		guiGraphics.fill(listX + listWidth - 1, listY, listX + listWidth, listY + listHeight, 0xFF5A5A5A);

		if (sourceRows.isEmpty()) {
			guiGraphics.drawCenteredString(this.font, Component.literal("No messages available"), this.width / 2, listY + listHeight / 2 - 4, opaqueColor(0xAAAAAA));
			return;
		}

		int maxOffset = Math.max(0, sourceRows.size() - maxVisibleRows);
		scrollOffsetRows = Mth.clamp(scrollOffsetRows, 0, maxOffset);
		int visible = Math.min(maxVisibleRows, sourceRows.size() - scrollOffsetRows);
		int textX = listX + 8;

		for (int i = 0; i < visible; i++) {
			int absolute = scrollOffsetRows + i;
			int rowY = listY + i * LIST_ROW_HEIGHT;
			boolean hovered = mouseX >= listX && mouseX <= listX + listWidth && mouseY >= rowY && mouseY < rowY + LIST_ROW_HEIGHT;

			int rowBackground = (i % 2 == 0) ? 0x402A2A2A : 0x40333333;
			if (hovered) {
				rowBackground = 0x60606060;
			}
			guiGraphics.fill(listX + 1, rowY + 1, listX + listWidth - 1, rowY + LIST_ROW_HEIGHT - 1, rowBackground);

			SelectionState state = states.get(absolute);
			ReviewRow row = sourceRows.get(absolute);
			String prefix = state.marker() + " (" + clampScore(row.modScore()) + ") ";
			int color = opaqueColor(state.color());

			guiGraphics.drawString(this.font, prefix + compactMessage(row.message(), 160), textX, rowY + 6, color, false);
		}

		if (sourceRows.size() > maxVisibleRows) {
			renderScrollBar(guiGraphics);
		}
	}

	private void renderScrollBar(GuiGraphics guiGraphics) {
		int trackLeft = listX + listWidth - 4;
		int trackTop = listY + 2;
		int trackBottom = listY + listHeight - 2;
		int trackHeight = Math.max(1, trackBottom - trackTop);
		guiGraphics.fill(trackLeft, trackTop, trackLeft + 2, trackBottom, 0xFF3A3A3A);

		int totalRows = Math.max(1, sourceRows.size());
		int thumbHeight = Math.max(12, (int) (trackHeight * (maxVisibleRows / (double) totalRows)));
		int maxOffset = Math.max(1, totalRows - maxVisibleRows);
		int thumbRange = Math.max(1, trackHeight - thumbHeight);
		int thumbTop = trackTop + (int) Math.round((scrollOffsetRows / (double) maxOffset) * thumbRange);
		guiGraphics.fill(trackLeft, thumbTop, trackLeft + 2, thumbTop + thumbHeight, 0xFFC8C8C8);
	}

	private boolean isInsideList(double mouseX, double mouseY) {
		return mouseX >= listX
			&& mouseX <= listX + listWidth
			&& mouseY >= listY
			&& mouseY <= listY + listHeight;
	}

	private int countSelections(SelectionState state) {
		int count = 0;
		for (SelectionState current : states) {
			if (current == state) {
				count++;
			}
		}
		return count;
	}

	private static List<ReviewRow> sanitizeRows(List<ReviewRow> input) {
		if (input == null || input.isEmpty()) {
			return List.of();
		}
		List<ReviewRow> out = new ArrayList<>();
		for (int i = 0; i < input.size(); i++) {
			ReviewRow row = input.get(i);
			if (row == null) {
				continue;
			}
			String message = normalizeReviewMessage(row.message());
			if (message.isBlank()) {
				continue;
			}
			String rowId = row.rowId() == null || row.rowId().isBlank() ? "row-" + i : row.rowId().trim();
			out.add(new ReviewRow(rowId, message, row.currentLabel(), row.modScore()));
		}
		return out;
	}

	private static String normalizeReviewMessage(String raw) {
		if (raw == null || raw.isBlank()) {
			return "";
		}
		return raw.replace('\n', ' ').replace('\r', ' ').trim();
	}

	private static String compactMessage(String raw, int maxLen) {
		if (raw == null || raw.isBlank()) {
			return "";
		}
		String normalized = normalizeReviewMessage(raw);
		if (normalized.length() <= maxLen) {
			return normalized;
		}
		return normalized.substring(0, Math.max(0, maxLen - 3)) + "...";
	}

	private static String safePlayerName(String playerName) {
		return playerName == null || playerName.isBlank() ? "unknown" : playerName.trim();
	}

	private static int clampScore(int score) {
		return Math.max(0, Math.min(100, score));
	}

	private void renderSelectedSummary(GuiGraphics guiGraphics, int y) {
		int scamCount = countSelections(SelectionState.SCAM);
		int legitCount = countSelections(SelectionState.LEGIT);
		int ignoredCount = Math.max(0, sourceRows.size() - scamCount - legitCount);

		String prefix = "Selected: ";
		String scamPart = "scam " + scamCount;
		String legitPart = "legit " + legitCount;
		String ignoredPart = "ignored " + ignoredCount;
		String separator = " | ";

		int totalWidth = this.font.width(prefix)
			+ this.font.width(scamPart)
			+ this.font.width(separator)
			+ this.font.width(legitPart)
			+ this.font.width(separator)
			+ this.font.width(ignoredPart);
		int x = (this.width - totalWidth) / 2;

		int neutralColor = opaqueColor(0xCFCFCF);
		int scamColor = opaqueColor(SelectionState.SCAM.color());
		int legitColor = opaqueColor(SelectionState.LEGIT.color());

		guiGraphics.drawString(this.font, prefix, x, y, neutralColor, false);
		x += this.font.width(prefix);
		guiGraphics.drawString(this.font, scamPart, x, y, scamColor, false);
		x += this.font.width(scamPart);
		guiGraphics.drawString(this.font, separator, x, y, neutralColor, false);
		x += this.font.width(separator);
		guiGraphics.drawString(this.font, legitPart, x, y, legitColor, false);
		x += this.font.width(legitPart);
		guiGraphics.drawString(this.font, separator, x, y, neutralColor, false);
		x += this.font.width(separator);
		guiGraphics.drawString(this.font, ignoredPart, x, y, neutralColor, false);
	}

	private static int localSplitWidth(int totalWidth, int columns, int spacing) {
		int safeColumns = Math.max(1, columns);
		int safeSpacing = Math.max(0, spacing);
		int gaps = Math.max(0, safeColumns - 1);
		int available = Math.max(0, totalWidth - gaps * safeSpacing);
		return available / safeColumns;
	}

	private static int localColumnX(int startX, int cellWidth, int columnIndex) {
		int safeIndex = Math.max(0, columnIndex);
		return startX + (cellWidth + ACTION_BUTTON_GAP) * safeIndex;
	}

	private enum SelectionState {
		IGNORE(-1, 0xFFFFFF, "I"),
		SCAM(1, 0xFFB3B3, "S"),
		LEGIT(0, 0xB3FFB3, "L");

		private final int label;
		private final int color;
		private final String marker;

		SelectionState(int label, int color, String marker) {
			this.label = label;
			this.color = color;
			this.marker = marker;
		}

		private SelectionState next() {
			return switch (this) {
				case IGNORE -> SCAM;
				case SCAM -> LEGIT;
				case LEGIT -> IGNORE;
			};
		}

		private boolean hasLabel() {
			return label >= 0;
		}

		private int label() {
			return label;
		}

		private int color() {
			return color;
		}

		private String marker() {
			return "[" + marker + "]";
		}

		private static SelectionState fromCurrentLabel(int currentLabel) {
			return switch (currentLabel) {
				case 1 -> SCAM;
				case 0 -> LEGIT;
				default -> IGNORE;
			};
		}

		private static SelectionState fromReviewRow(ReviewRow row) {
			if (row == null) {
				return IGNORE;
			}
			SelectionState byLabel = fromCurrentLabel(row.currentLabel());
			if (byLabel != IGNORE) {
				return byLabel;
			}
			return row.modScore() > 0 ? SCAM : IGNORE;
		}
	}

	private static final class SimpleCheckbox {
		private final int x;
		private final int y;
		private final int width;
		private final int height;
		private final Component label;
		private boolean checked;

		private SimpleCheckbox(int x, int y, int width, int height, Component label, boolean checked) {
			this.x = x;
			this.y = y;
			this.width = width;
			this.height = height;
			this.label = label == null ? Component.literal("") : label;
			this.checked = checked;
		}

		private void render(GuiGraphics guiGraphics, Font font, int mouseX, int mouseY) {
			boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
			int rowColor = hovered ? 0x40444444 : 0x302A2A2A;
			guiGraphics.fill(x, y, x + width, y + height, rowColor);
			guiGraphics.fill(x, y, x + width, y + 1, 0xFF5A5A5A);
			guiGraphics.fill(x, y + height - 1, x + width, y + height, 0xFF5A5A5A);
			guiGraphics.fill(x, y, x + 1, y + height, 0xFF5A5A5A);
			guiGraphics.fill(x + width - 1, y, x + width, y + height, 0xFF5A5A5A);

			int boxSize = 12;
			int boxX = x + 6;
			int boxY = y + (height - boxSize) / 2;
			guiGraphics.fill(boxX, boxY, boxX + boxSize, boxY + boxSize, 0xFF111111);
			guiGraphics.fill(boxX, boxY, boxX + boxSize, boxY + 1, 0xFF9A9A9A);
			guiGraphics.fill(boxX, boxY + boxSize - 1, boxX + boxSize, boxY + boxSize, 0xFF9A9A9A);
			guiGraphics.fill(boxX, boxY, boxX + 1, boxY + boxSize, 0xFF9A9A9A);
			guiGraphics.fill(boxX + boxSize - 1, boxY, boxX + boxSize, boxY + boxSize, 0xFF9A9A9A);
			if (checked) {
				guiGraphics.drawString(font, Component.literal("x").withStyle(ChatFormatting.GREEN), boxX + 3, boxY + 2, opaqueColor(ON_LIGHT_GREEN), false);
			}

			int textColor = checked ? opaqueColor(ON_LIGHT_GREEN) : opaqueColor(0xE0E0E0);
			guiGraphics.drawString(font, label, boxX + boxSize + 8, y + (height - font.lineHeight) / 2, textColor, false);
		}

		private boolean mouseClicked(double mouseX, double mouseY, int button) {
			if (button != 0) {
				return false;
			}
			if (mouseX < x || mouseX > x + width || mouseY < y || mouseY > y + height) {
				return false;
			}
			checked = !checked;
			return true;
		}

		private boolean checked() {
			return checked;
		}
	}

	@FunctionalInterface
	public interface SubmitHandler {
		int submit(SaveRequest request);
	}

	public record ReviewRow(String rowId, String message, int currentLabel, int modScore) {
		public ReviewRow(String rowId, String message, int currentLabel) {
			this(rowId, message, currentLabel, 0);
		}

		public ReviewRow {
			rowId = rowId == null ? "" : rowId.trim();
			message = normalizeReviewMessage(message);
			currentLabel = (currentLabel == 0 || currentLabel == 1) ? currentLabel : -1;
			modScore = clampScore(modScore);
		}
	}

	public record ReviewedSelection(String rowId, String message, int label) {
	}

	public record SaveRequest(
		List<ReviewedSelection> selections,
		boolean upload,
		boolean addToBlacklist,
		boolean addToBlock
	) {
	}
}
