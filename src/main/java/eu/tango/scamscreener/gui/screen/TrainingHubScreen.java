package eu.tango.scamscreener.gui.screen;

import eu.tango.scamscreener.ScamScreenerRuntime;
import eu.tango.scamscreener.config.store.ConfigPaths;
import eu.tango.scamscreener.gui.base.BaseScreen;
import eu.tango.scamscreener.review.ReviewEntry;
import eu.tango.scamscreener.review.ReviewVerdict;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmLinkScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
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
    private static final int FORM_TOP = CONTENT_TOP + 106;

    private ButtonWidget uploadButton;
    private ButtonWidget openWebsiteButton;
    private volatile String statusText = "Upload reviewed SAFE/RISK cases anonymously with your local client ID.";
    private volatile int statusColor = STATUS_INFO_COLOR;

    public TrainingHubScreen(Screen parent) {
        super(Text.literal("Training Hub"), parent);
    }

    @Override
    protected void init() {
        int contentWidth = Math.min(420, Math.max(280, this.width - 40));
        int left = centeredX(contentWidth);
        int y = FORM_TOP + 12;
        int buttonWidth = splitWidth(contentWidth, 2, DEFAULT_SPLIT_GAP);

        uploadButton = addDrawableChild(
            ButtonWidget.builder(Text.literal("Upload"), button -> exportAndUpload())
                .dimensions(left, y, buttonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        openWebsiteButton = addDrawableChild(
            ButtonWidget.builder(Text.literal("Open Training Hub"), button -> openWebsite())
                .dimensions(columnX(left, buttonWidth, DEFAULT_SPLIT_GAP, 1), y, buttonWidth, DEFAULT_BUTTON_HEIGHT)
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
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        super.render(context, mouseX, mouseY, deltaTicks);

        int contentWidth = Math.min(420, Math.max(280, this.width - 40));
        int left = centeredX(contentWidth);
        int y = CONTENT_TOP;

        drawSectionTitle(context, left, y, "Anonymous Upload");
        y += 12;
        drawLine(context, left, y, "Client ID: " + compactMiddle(ScamScreenerRuntime.getInstance().trainingClientId(), 48));
        y += 12;
        drawLine(context, left, y, "Reviewed SAFE/RISK cases: " + exportableReviewCount());
        y += 12;
        drawLine(context, left, y, "Export file: " + compactMiddle(ConfigPaths.trainingCasesV2File().toString(), 54));
        y += 12;
        context.drawTextWithShadow(
            this.textRenderer,
            Text.literal(compact(statusText, 72)),
            left,
            y,
            opaqueColor(statusColor)
        );

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

    private void exportAndUpload() {
        if (ScamScreenerRuntime.getInstance().trainingHubUploadWorker().isRunning()) {
            setStatus("An upload worker is already running.", STATUS_WARNING_COLOR);
            refreshButtons();
            return;
        }
        if (exportableReviewCount() <= 0) {
            setStatus("No reviewed SAFE/RISK cases are available for upload.", STATUS_ERROR_COLOR);
            refreshButtons();
            return;
        }

        ScamScreenerRuntime runtime = ScamScreenerRuntime.getInstance();
        if (!runtime.trainingHubUploadWorker().startUpload()) {
            setStatus("An upload worker is already running.", STATUS_WARNING_COLOR);
            refreshButtons();
            return;
        }

        setStatus("Anonymous upload started in the background. Retry updates appear in chat.", STATUS_SUCCESS_COLOR);
        refreshButtons();
    }

    private void openWebsite() {
        if (this.client == null) {
            setStatus("Client unavailable.", STATUS_ERROR_COLOR);
            refreshButtons();
            return;
        }

        this.client.setScreen(new ConfirmLinkScreen(open -> {
            if (open) {
                try {
                    Util.getOperatingSystem().open(TRAINING_HUB_URL);
                } catch (Exception exception) {
                    setStatus(rootCauseMessage(exception), STATUS_ERROR_COLOR);
                }
            }

            if (this.client != null) {
                this.client.setScreen(this);
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

    private int exportableReviewCount() {
        int count = 0;
        for (ReviewEntry entry : ScamScreenerRuntime.getInstance().reviewStore().entries()) {
            if (entry == null) {
                continue;
            }

            ReviewVerdict verdict = entry.getVerdict();
            if (verdict == ReviewVerdict.RISK || verdict == ReviewVerdict.SAFE) {
                count++;
            }
        }

        return count;
    }

    private void setStatus(String text, int color) {
        statusText = text == null || text.isBlank() ? "" : text.trim();
        statusColor = color;
    }

    private static Throwable rootCause(Throwable throwable) {
        Throwable rootCause = throwable;
        while (rootCause != null && rootCause.getCause() != null) {
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
        return value.length() <= maxLength ? value : value.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    private static String compactMiddle(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "";
        }
        if (value.length() <= maxLength || maxLength < 7) {
            return compact(value, maxLength);
        }

        int sideLength = Math.max(2, (maxLength - 3) / 2);
        int endStart = Math.max(sideLength, value.length() - sideLength);
        return value.substring(0, sideLength) + "..." + value.substring(endStart);
    }
}
