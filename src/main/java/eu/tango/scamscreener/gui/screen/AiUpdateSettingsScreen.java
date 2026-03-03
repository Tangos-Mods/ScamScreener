package eu.tango.scamscreener.gui.screen;

import eu.tango.scamscreener.gui.base.BaseScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * Placeholder v1 AI update screen until the model-stage workflow is restored.
 */
public final class AiUpdateSettingsScreen extends BaseScreen {
    /**
     * Creates the AI update screen.
     *
     * @param parent the parent screen
     */
    public AiUpdateSettingsScreen(Screen parent) {
        super(Text.literal("ScamScreener AI Update"), parent);
    }

    @Override
    protected void init() {
        ColumnState column = defaultColumnState();
        int buttonWidth = column.buttonWidth();
        int x = column.x();
        int y = column.y();

        addDrawableChild(
            ButtonWidget.builder(Text.literal("Model-stage updater not available yet."), button -> {
            }).dimensions(x, y, buttonWidth, DEFAULT_BUTTON_HEIGHT).build()
        ).active = false;
        y += ROW_HEIGHT;

        int halfWidth = splitWidth(buttonWidth, 2, DEFAULT_SPLIT_GAP);
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Check / Download"), button -> {
            }).dimensions(x, y, halfWidth, DEFAULT_BUTTON_HEIGHT).build()
        ).active = false;
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Force Check"), button -> {
            }).dimensions(columnX(x, halfWidth, DEFAULT_SPLIT_GAP, 1), y, halfWidth, DEFAULT_BUTTON_HEIGHT).build()
        ).active = false;
        y += ROW_HEIGHT;

        addDrawableChild(
            ButtonWidget.builder(Text.literal("Join Up-to-Date Message: OFF"), button -> {
            }).dimensions(x, y, buttonWidth, DEFAULT_BUTTON_HEIGHT).build()
        ).active = false;
        y += ROW_HEIGHT;

        int thirdWidth = splitWidth(buttonWidth, 3, DEFAULT_SPLIT_GAP);
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Accept"), button -> {
            }).dimensions(x, y, thirdWidth, DEFAULT_BUTTON_HEIGHT).build()
        ).active = false;
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Merge"), button -> {
            }).dimensions(columnX(x, thirdWidth, DEFAULT_SPLIT_GAP, 1), y, thirdWidth, DEFAULT_BUTTON_HEIGHT).build()
        ).active = false;
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Ignore"), button -> {
            }).dimensions(columnX(x, thirdWidth, DEFAULT_SPLIT_GAP, 2), y, thirdWidth, DEFAULT_BUTTON_HEIGHT).build()
        ).active = false;

        addBackButton(buttonWidth);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        super.render(context, mouseX, mouseY, deltaTicks);

        int left = centeredX(defaultButtonWidth());
        drawSectionTitle(context, left, CONTENT_TOP - 18, "AI Update");
        drawLine(context, left, CONTENT_TOP - 6, "The v1 shell is back; model-update actions stay disabled for now.");
    }
}
