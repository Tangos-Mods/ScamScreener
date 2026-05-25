package eu.tango.scamscreener.gui.screen;

import eu.tango.scamscreener.ScamScreenerRuntime;
import eu.tango.scamscreener.config.store.ConfigPaths;
import eu.tango.scamscreener.gui.base.BaseScreen;
import eu.tango.scamscreener.review.ReviewEntry;
import eu.tango.scamscreener.review.ReviewVerdict;
import eu.tango.scamscreener.training.TrainingCaseExportService;
import eu.tango.scamscreener.training.TrainingUploadReminder;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

/**
 * Minimal in-game Training Hub upload screen.
 */
public final class TrainingHubScreen extends BaseScreen {
    private static final String TRAINING_HUB_URL = "https://scamscreener.creepans.net/";
    private static final int STATUS_INFO_COLOR = 0xB8B8B8;
    private static final int STATUS_SUCCESS_COLOR = 0x99FF99;
    private static final int STATUS_WARNING_COLOR = 0xFFD27F;
    private static final int STATUS_ERROR_COLOR = 0xFF7F7F;
    private static final int COPY_HINT_COLOR = 0xA8D8FF;
    private static final int COPY_HINT_HOVER_COLOR = 0xD8EEFF;
    private static final int FORM_TOP = CONTENT_TOP + 106;

    private Button uploadButton;
    private Button openWebsiteButton;
    private volatile String statusText = "Upload reviewed SAFE/RISK cases anonymously with your local client ID.";
    private volatile int statusColor = STATUS_INFO_COLOR;
    private int clientIdLineX;
    private int clientIdLineY;
    private int clientIdLineWidth;
    private int clientIdLineHeight;
    private boolean clientIdCopied;

    public TrainingHubScreen(Screen parent) {
        super(Component.literal("Training Hub"), parent);
    }

    @Override
    protected void init() {
        int contentWidth = Math.min(420, Math.max(280, this.width - 40));
        int left = centeredX(contentWidth);
        int y = FORM_TOP + 12;
        int buttonWidth = splitWidth(contentWidth, 2, DEFAULT_SPLIT_GAP);

        uploadButton = addRenderableWidget(
            Button.builder(Component.literal("Upload"), button -> exportAndUpload())
                .bounds(left, y, buttonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        openWebsiteButton = addRenderableWidget(
            Button.builder(Component.literal("Open Training Hub"), button -> openWebsite())
                .bounds(columnX(left, buttonWidth, DEFAULT_SPLIT_GAP, 1), y, buttonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );

        addBackButton(Math.min(220, contentWidth));
        refreshButtons();
    }

    @Override
    public void tick() {
        refreshButtons();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks) {
        super.extractRenderState(context, mouseX, mouseY, deltaTicks);

        int contentWidth = Math.min(420, Math.max(280, this.width - 40));
        int left = centeredX(contentWidth);
        int y = CONTENT_TOP;
        String clientId = ScamScreenerRuntime.getInstance().trainingClientId();
        String clientIdText = "Client ID: " + clientId + clientIdSuffix();
        boolean clientIdHovered = isHoveringClientId(mouseX, mouseY);

        drawSectionTitle(context, left, y, "Anonymous Upload");
        y += 12;
        clientIdLineX = left;
        clientIdLineY = y;
        clientIdLineWidth = this.font.width(clientIdText);
        clientIdLineHeight = this.font.lineHeight;
        context.text(
            this.font,
            Component.literal(clientIdText),
            left,
            y,
            opaqueColor(clientIdHovered ? COPY_HINT_HOVER_COLOR : COPY_HINT_COLOR)
        );
        y += 12;
        drawLine(context, left, y, "Reviewed SAFE/RISK cases: " + exportableReviewCount());
        y += 12;
        drawLine(context, left, y, "Export file: " + compactMiddle(ConfigPaths.trainingCasesV2File().toString(), 54));
        y += 12;
        context.text(this.font, Component.literal(compact(statusText, 72)), left, y, opaqueColor(statusColor));

        int dataInfoY = FORM_TOP + ROW_HEIGHT + DEFAULT_BUTTON_HEIGHT + 18;
        drawSectionTitle(context, left, dataInfoY, "Inside Minecraft");
        dataInfoY += 12;
        drawLine(context, left, dataInfoY, "The mod uploads with your local training client ID.");
        dataInfoY += 12;
        drawLine(context, left, dataInfoY, "No ScamScreener account login is required in the mod.");
        dataInfoY += 12;
        drawLine(context, left, dataInfoY, "Link this client ID to your account later in the Training Hub.");
        dataInfoY += 12;
        drawLine(context, left, dataInfoY, "Reviewed SAFE/RISK cases stay local until you upload them.");
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        if (event != null && event.button() == 0 && isHoveringClientId(event.x(), event.y())) {
            copyClientId();
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    private void exportAndUpload() {
        ScamScreenerRuntime runtime = ScamScreenerRuntime.getInstance();
        if (runtime.trainingHubUploadWorker().isRunning()) {
            setStatus("An upload worker is already running.", STATUS_WARNING_COLOR);
            refreshButtons();
            return;
        }
        if (exportableReviewCount() <= 0) {
            setStatus("No reviewed SAFE/RISK cases are available for upload.", STATUS_ERROR_COLOR);
            refreshButtons();
            return;
        }
        if (!runtime.trainingHubUploadWorker().startUpload()) {
            setStatus("An upload worker is already running.", STATUS_WARNING_COLOR);
            refreshButtons();
            return;
        }

        TrainingUploadReminder.postpone();
        setStatus("Anonymous upload started in the background. Retry updates appear in chat.", STATUS_SUCCESS_COLOR);
        refreshButtons();
    }

    private void openWebsite() {
        if (this.minecraft == null) {
            setStatus("Client unavailable.", STATUS_ERROR_COLOR);
            refreshButtons();
            return;
        }

        this.minecraft.setScreen(new ConfirmLinkScreen(open -> {
            if (open) {
                try {
                    Util.getPlatform().openUri(TRAINING_HUB_URL);
                } catch (Exception exception) {
                    setStatus(rootCauseMessage(exception), STATUS_ERROR_COLOR);
                }
            }

            if (this.minecraft != null) {
                this.minecraft.setScreen(this);
            }
        }, TRAINING_HUB_URL, true));
    }

    private void refreshButtons() {
        boolean hasExportableCases = exportableReviewCount() > 0;
        boolean uploadWorkerRunning = ScamScreenerRuntime.getInstance().trainingHubUploadWorker().isRunning();

        if (uploadButton != null) {
            uploadButton.active = !uploadWorkerRunning && hasExportableCases;
        }
        if (openWebsiteButton != null) {
            openWebsiteButton.active = true;
        }
    }

    private void copyClientId() {
        if (this.minecraft == null) {
            setStatus("Client unavailable.", STATUS_ERROR_COLOR);
            return;
        }

        this.minecraft.keyboardHandler.setClipboard(ScamScreenerRuntime.getInstance().trainingClientId());
        clientIdCopied = true;
    }

    private String clientIdSuffix() {
        return clientIdCopied ? " (copied)" : " (click to copy)";
    }

    private boolean isHoveringClientId(double mouseX, double mouseY) {
        return mouseX >= clientIdLineX
            && mouseX < clientIdLineX + clientIdLineWidth
            && mouseY >= clientIdLineY
            && mouseY < clientIdLineY + clientIdLineHeight;
    }

    private int exportableReviewCount() {
        return TrainingCaseExportService.countExportableReviewedCases(ScamScreenerRuntime.getInstance().reviewStore().entries());
    }

    private void setStatus(String text, int color) {
        statusText = text == null || text.isBlank() ? "" : text.trim();
        statusColor = color;
    }

    private static Throwable rootCause(Throwable throwable) {
        Throwable rootCause = throwable;
        while (rootCause.getCause() != null) {
            rootCause = rootCause.getCause();
        }

        return rootCause;
    }

    private static String rootCauseMessage(Throwable throwable) {
        Throwable rootCause = rootCause(throwable);
        String message = rootCause == null ? null : rootCause.getMessage();
        return message == null || message.isBlank() ? "unknown error" : message;
    }

    private static String compact(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "";
        }
        if (value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    private static String compactMiddle(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "";
        }
        if (value.length() <= maxLength || maxLength < 8) {
            return value;
        }

        int remaining = maxLength - 3;
        int prefixLength = remaining / 2;
        int suffixLength = remaining - prefixLength;
        return value.substring(0, prefixLength) + "..." + value.substring(value.length() - suffixLength);
    }
}
