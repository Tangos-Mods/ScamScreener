package eu.tango.scamscreener.gui.screen;

import eu.tango.scamscreener.ScamScreenerRuntime;
import eu.tango.scamscreener.debug.DebugKeys;
import eu.tango.scamscreener.gui.base.BaseScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Restored v1-style debug toggle screen.
 */
public final class DebugSettingsScreen extends BaseScreen {
    private ButtonWidget allButton;
    private final Map<String, ButtonWidget> debugButtons = new LinkedHashMap<>();

    /**
     * Creates the debug settings screen.
     *
     * @param parent the parent screen
     */
    public DebugSettingsScreen(Screen parent) {
        super(Text.literal("ScamScreener Debug"), parent);
    }

    @Override
    protected void init() {
        debugButtons.clear();
        ColumnState column = defaultColumnState();
        int buttonWidth = column.buttonWidth();
        int x = column.x();
        int y = column.y();

        allButton = addDrawableChild(
            ButtonWidget.builder(Text.empty(), button -> {
                boolean enabled = !allEnabled(currentStates());
                setAll(enabled);
                refreshButtons();
            }).dimensions(x, y, buttonWidth, DEFAULT_BUTTON_HEIGHT).build()
        );
        y += ROW_HEIGHT;

        for (String key : DebugKeys.keys()) {
            String buttonKey = key;
            ButtonWidget button = addDrawableChild(
                ButtonWidget.builder(Text.empty(), clicked -> {
                    Map<String, Boolean> states = currentStates();
                    boolean enabled = states.getOrDefault(buttonKey, false);
                    setDebugKey(buttonKey, !enabled);
                    refreshButtons();
                }).dimensions(x, y, buttonWidth, DEFAULT_BUTTON_HEIGHT).build()
            );
            debugButtons.put(buttonKey, button);
            y += ROW_HEIGHT;
        }

        addBackButton(buttonWidth);
        refreshButtons();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        super.render(context, mouseX, mouseY, deltaTicks);

        int left = centeredX(defaultButtonWidth());
        drawSectionTitle(context, left, CONTENT_TOP - 18, "Debug");
        drawLine(context, left, CONTENT_TOP - 6, "Legacy v1 debug toggles are persisted again.");
    }

    private void refreshButtons() {
        Map<String, Boolean> states = currentStates();
        if (allButton != null) {
            allButton.setMessage(Text.literal("All Debug: " + onOff(allEnabled(states))));
        }

        for (Map.Entry<String, ButtonWidget> entry : debugButtons.entrySet()) {
            boolean enabled = states.getOrDefault(entry.getKey(), false);
            entry.getValue().setMessage(Text.literal(DebugKeys.label(entry.getKey()) + ": " + onOff(enabled)));
        }
    }

    private Map<String, Boolean> currentStates() {
        return DebugKeys.withDefaults(ScamScreenerRuntime.getInstance().config().debug().flags());
    }

    private void setAll(boolean enabled) {
        var runtime = ScamScreenerRuntime.getInstance();
        runtime.config().debug().flags().clear();
        for (String key : DebugKeys.keys()) {
            runtime.config().debug().flags().put(key, enabled);
        }
        runtime.config().output().setDebugLogging(enabled);
        runtime.saveConfig();
    }

    private void setDebugKey(String key, boolean enabled) {
        var runtime = ScamScreenerRuntime.getInstance();
        runtime.config().debug().flags().put(DebugKeys.normalize(key), enabled);
        runtime.config().output().setDebugLogging(allEnabled(currentStates()));
        runtime.saveConfig();
    }

    private static boolean allEnabled(Map<String, Boolean> states) {
        if (states == null || states.isEmpty()) {
            return false;
        }

        for (String key : DebugKeys.keys()) {
            if (!Boolean.TRUE.equals(states.get(key))) {
                return false;
            }
        }

        return true;
    }
}
