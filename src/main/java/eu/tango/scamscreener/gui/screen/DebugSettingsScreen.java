package eu.tango.scamscreener.gui.screen;

import eu.tango.scamscreener.ScamScreenerRuntime;
import eu.tango.scamscreener.debug.DebugKeys;
import eu.tango.scamscreener.gui.base.BaseScreen;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Debug toggle screen.
 */
public final class DebugSettingsScreen extends BaseScreen {
    private Button allButton;
    private final Map<String, Button> debugButtons = new LinkedHashMap<>();

    /**
     * Creates the debug settings screen.
     *
     * @param parent the parent screen
     */
    public DebugSettingsScreen(Screen parent) {
        super(Component.literal("ScamScreener Debug"), parent);
    }

    @Override
    protected void init() {
        debugButtons.clear();
        ColumnState column = defaultColumnState();
        int buttonWidth = column.buttonWidth();
        int x = column.x();
        int y = column.y() + 16;

        allButton = addRenderableWidget(
            Button.builder(Component.empty(), button -> {
                boolean enabled = !allEnabled(currentStates());
                setAll(enabled);
                refreshButtons();
            }).bounds(x, y, buttonWidth, DEFAULT_BUTTON_HEIGHT).build()
        );
        y += ROW_HEIGHT;

        for (String key : DebugKeys.keys()) {
            String buttonKey = key;
            Button button = addRenderableWidget(
                Button.builder(Component.empty(), clicked -> {
                    Map<String, Boolean> states = currentStates();
                    boolean enabled = states.getOrDefault(buttonKey, false);
                    setDebugKey(buttonKey, !enabled);
                    refreshButtons();
                }).bounds(x, y, buttonWidth, DEFAULT_BUTTON_HEIGHT).build()
            );
            debugButtons.put(buttonKey, button);
            y += ROW_HEIGHT;
        }

        addBackButton(buttonWidth);
        refreshButtons();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks) {
        super.extractRenderState(context, mouseX, mouseY, deltaTicks);

        int left = centeredX(defaultButtonWidth());
        drawSectionTitle(context, left, CONTENT_TOP - 18, "Debug");
        drawLine(context, left, CONTENT_TOP - 6, "Debug toggles are stored in local runtime config.");
    }

    private void refreshButtons() {
        Map<String, Boolean> states = currentStates();
        if (allButton != null) {
            allButton.setMessage(toggleText("All Debug: ", allEnabled(states)));
        }

        for (Map.Entry<String, Button> entry : debugButtons.entrySet()) {
            boolean enabled = states.getOrDefault(entry.getKey(), false);
            entry.getValue().setMessage(toggleText(DebugKeys.label(entry.getKey()) + ": ", enabled));
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
