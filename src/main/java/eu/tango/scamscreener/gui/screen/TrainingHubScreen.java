package eu.tango.scamscreener.gui.screen;

import eu.tango.scamscreener.ScamScreenerRuntime;
import eu.tango.scamscreener.config.store.ConfigPaths;
import eu.tango.scamscreener.gui.base.BaseScreen;
import eu.tango.scamscreener.review.ReviewEntry;
import eu.tango.scamscreener.review.ReviewVerdict;
import eu.tango.scamscreener.training.ScamScreenerClientSession;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmLinkScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.util.concurrent.CompletionException;

/**
 * Minimal in-game Training Hub login and upload screen.
 */
public final class TrainingHubScreen extends BaseScreen {
    private static final String TRAINING_HUB_URL = "https://scamscreener.creepans.net/";
    private static final int STATUS_INFO_COLOR = 0xB8B8B8;
    private static final int STATUS_SUCCESS_COLOR = 0x99FF99;
    private static final int STATUS_WARNING_COLOR = 0xFFD27F;
    private static final int STATUS_ERROR_COLOR = 0xFF7F7F;
    private static final int FORM_TOP = CONTENT_TOP + 106;
    private static final int FIELD_LABEL_TO_INPUT_GAP = 12;
    private static final int FIELD_GROUP_HEIGHT = 36;

    private TextFieldWidget usernameOrEmailField;
    private TextFieldWidget passwordField;
    private ButtonWidget loginButton;
    private ButtonWidget uploadButton;
    private ButtonWidget logoutButton;
    private ButtonWidget openWebsiteButton;
    private volatile boolean busy;
    private volatile String statusText = "Log in with your ScamScreener account to upload reviewed cases.";
    private volatile int statusColor = STATUS_INFO_COLOR;

    public TrainingHubScreen(Screen parent) {
        super(Text.literal("Training Hub"), parent);
    }

    @Override
    protected void init() {
        int contentWidth = Math.min(420, Math.max(280, this.width - 40));
        int left = centeredX(contentWidth);
        int y = FORM_TOP + FIELD_LABEL_TO_INPUT_GAP;

        String existingUsername = usernameOrEmailField == null ? "" : usernameOrEmailField.getText();
        ScamScreenerClientSession session = currentSession();

        usernameOrEmailField = addDrawableChild(
            new TextFieldWidget(
                this.textRenderer,
                left,
                y,
                contentWidth,
                DEFAULT_BUTTON_HEIGHT,
                Text.literal("ScamScreener Username or Email")
            )
        );
        usernameOrEmailField.setMaxLength(96);
        usernameOrEmailField.setChangedListener(value -> refreshButtons());
        if (!existingUsername.isBlank()) {
            usernameOrEmailField.setText(existingUsername);
        } else if (session != null && !session.username().isBlank()) {
            usernameOrEmailField.setText(session.username());
        }

        y += FIELD_GROUP_HEIGHT;
        passwordField = addDrawableChild(
            new TextFieldWidget(
                this.textRenderer,
                left,
                y,
                contentWidth,
                DEFAULT_BUTTON_HEIGHT,
                Text.literal("ScamScreener Password")
            )
        );
        passwordField.setMaxLength(128);
        passwordField.addFormatter((value, firstCharacterIndex) ->
            OrderedText.styledForwardsVisitedString(maskedPassword(value), Style.EMPTY)
        );
        passwordField.setChangedListener(value -> refreshButtons());

        y += FIELD_GROUP_HEIGHT;
        int buttonWidth = splitWidth(contentWidth, 2, DEFAULT_SPLIT_GAP);
        loginButton = addDrawableChild(
            ButtonWidget.builder(Text.literal("Log In"), button -> logIn())
                .dimensions(left, y, buttonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        uploadButton = addDrawableChild(
            ButtonWidget.builder(Text.literal("Export + Upload"), button -> exportAndUpload())
                .dimensions(columnX(left, buttonWidth, DEFAULT_SPLIT_GAP, 1), y, buttonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );

        y += ROW_HEIGHT;
        logoutButton = addDrawableChild(
            ButtonWidget.builder(Text.literal("Log Out"), button -> logOut())
                .dimensions(left, y, buttonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        openWebsiteButton = addDrawableChild(
            ButtonWidget.builder(Text.literal("Open Website"), button -> openWebsite())
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
    public void removed() {
        clearPasswordField();
        super.removed();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        super.render(context, mouseX, mouseY, deltaTicks);

        int contentWidth = Math.min(420, Math.max(280, this.width - 40));
        int left = centeredX(contentWidth);
        int y = CONTENT_TOP;

        drawSectionTitle(context, left, y, "ScamScreener Login");
        y += 12;
        drawLine(context, left, y, "Use your ScamScreener account only.");
        y += 12;
        context.drawTextWithShadow(
            this.textRenderer,
            Text.literal("Do NOT enter your Minecraft credentials."),
            left,
            y,
            opaqueColor(STATUS_ERROR_COLOR)
        );
        y += 12;
        drawLine(context, left, y, "Admin accounts with required web MFA may be blocked.");
        y += 18;
        drawLine(context, left, y, sessionSummary());
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

        int usernameLabelY = FORM_TOP;
        int passwordLabelY = FORM_TOP + FIELD_GROUP_HEIGHT;
        int dataInfoY = FORM_TOP + FIELD_LABEL_TO_INPUT_GAP + (FIELD_GROUP_HEIGHT * 2) + ROW_HEIGHT + DEFAULT_BUTTON_HEIGHT + 18;

        context.drawTextWithShadow(
            this.textRenderer,
            Text.literal("Username or Email"),
            left,
            usernameLabelY,
            opaqueColor(STATUS_INFO_COLOR)
        );
        context.drawTextWithShadow(
            this.textRenderer,
            Text.literal("Password"),
            left,
            passwordLabelY,
            opaqueColor(STATUS_INFO_COLOR)
        );

        drawSectionTitle(context, left, dataInfoY, "Inside Minecraft");
        dataInfoY += 12;
        drawLine(context, left, dataInfoY, "Your password is only used for ScamScreener login.");
        dataInfoY += 12;
        drawLine(context, left, dataInfoY, "The mod does not save the password on disk.");
        dataInfoY += 12;
        drawLine(context, left, dataInfoY, "The login session stays in memory until logout or restart.");
        dataInfoY += 12;
        drawLine(context, left, dataInfoY, "Reviewed SAFE/RISK cases stay local until you upload them.");
    }

    private void logIn() {
        if (busy || currentSession() != null) {
            return;
        }

        String usernameOrEmail = currentUsernameOrEmail();
        String password = currentPassword();
        if (usernameOrEmail.isBlank() || password.isBlank()) {
            setStatus("Enter your ScamScreener username/email and password.", STATUS_ERROR_COLOR);
            refreshButtons();
            return;
        }

        busy = true;
        setStatus("Logging in...", STATUS_INFO_COLOR);
        refreshButtons();

        ScamScreenerClientSession.loginAsync(usernameOrEmail, password)
            .whenComplete((session, throwable) -> runOnClient(() -> {
                busy = false;
                clearPasswordField();

                if (throwable != null) {
                    setStatus(rootCauseMessage(throwable), STATUS_ERROR_COLOR);
                    refreshButtons();
                    return;
                }

                ScamScreenerRuntime.getInstance().setTrainingHubSession(session);
                if (usernameOrEmailField != null && !session.username().isBlank()) {
                    usernameOrEmailField.setText(session.username());
                }
                setStatus("Logged in as " + displayUsername(session) + ".", STATUS_SUCCESS_COLOR);
                refreshButtons();
            }));
    }

    private void exportAndUpload() {
        if (busy) {
            return;
        }

        if (currentSession() == null) {
            setStatus("Log in first.", STATUS_ERROR_COLOR);
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

        setStatus("Upload worker started in the background. Retry updates appear in chat.", STATUS_INFO_COLOR);
        refreshButtons();
    }

    private void logOut() {
        if (busy) {
            return;
        }

        ScamScreenerClientSession session = currentSession();
        if (session == null) {
            ScamScreenerRuntime.getInstance().clearTrainingHubSession();
            setStatus("No active session.", STATUS_INFO_COLOR);
            refreshButtons();
            return;
        }

        busy = true;
        setStatus("Logging out...", STATUS_INFO_COLOR);
        refreshButtons();

        session.logoutAsync().whenComplete((ignored, throwable) -> runOnClient(() -> {
            busy = false;
            ScamScreenerRuntime.getInstance().clearTrainingHubSession();

            if (throwable != null) {
                setStatus("Session cleared locally. " + rootCauseMessage(throwable), STATUS_WARNING_COLOR);
                refreshButtons();
                return;
            }

            setStatus("Logged out.", STATUS_INFO_COLOR);
            refreshButtons();
        }));
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
        ScamScreenerClientSession session = currentSession();
        boolean hasSession = session != null;
        boolean hasCredentials = !currentUsernameOrEmail().isBlank() && !currentPassword().isBlank();
        boolean hasExportableCases = exportableReviewCount() > 0;
        boolean uploadWorkerRunning = ScamScreenerRuntime.getInstance().trainingHubUploadWorker().isRunning();

        if (loginButton != null) {
            loginButton.active = !busy && !hasSession && hasCredentials;
        }
        if (uploadButton != null) {
            uploadButton.active = !busy && !uploadWorkerRunning && hasSession && hasExportableCases;
        }
        if (logoutButton != null) {
            logoutButton.active = !busy && !uploadWorkerRunning && hasSession;
        }
        if (openWebsiteButton != null) {
            openWebsiteButton.active = !busy;
        }
    }

    private ScamScreenerClientSession currentSession() {
        return ScamScreenerRuntime.getInstance().trainingHubSession();
    }

    private String sessionSummary() {
        ScamScreenerClientSession session = currentSession();
        if (session == null) {
            return "Session: not logged in.";
        }

        return "Session: " + displayUsername(session) + " | expires " + session.expiresAt();
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

    private String currentUsernameOrEmail() {
        return usernameOrEmailField == null ? "" : usernameOrEmailField.getText().trim();
    }

    private String currentPassword() {
        return passwordField == null ? "" : passwordField.getText();
    }

    private void clearPasswordField() {
        if (passwordField != null) {
            passwordField.setText("");
        }
    }

    private void setStatus(String text, int color) {
        statusText = text == null || text.isBlank() ? "" : text.trim();
        statusColor = color;
    }

    private void runOnClient(Runnable action) {
        if (action == null) {
            return;
        }

        if (this.client != null) {
            this.client.execute(action);
            return;
        }

        action.run();
    }

    private static String displayUsername(ScamScreenerClientSession session) {
        if (session == null || session.username() == null || session.username().isBlank()) {
            return "<unknown>";
        }

        return session.username().trim();
    }

    private static Throwable rootCause(Throwable throwable) {
        Throwable rootCause = throwable;
        while (rootCause instanceof CompletionException && rootCause.getCause() != null) {
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

    private static String maskedPassword(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }

        return "*".repeat(value.length());
    }
}
