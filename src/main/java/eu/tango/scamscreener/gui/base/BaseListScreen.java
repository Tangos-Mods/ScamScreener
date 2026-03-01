package eu.tango.scamscreener.gui.base;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * Shared base for screens that host a custom list widget.
 *
 * <p>This centralizes the version-specific mouse input bridge so list screens
 * only implement plain coordinate-based handlers.
 */
public abstract class BaseListScreen extends BaseScreen {
    /**
     * Creates a list-oriented ScamScreener screen.
     *
     * @param title the visible screen title
     * @param parent the parent screen to return to
     */
    protected BaseListScreen(Text title, Screen parent) {
        super(title, parent);
    }

    /**
     * Handles a list click using normalized coordinates.
     *
     * @param mouseX the click x position
     * @param mouseY the click y position
     * @param button the mouse button index
     * @return {@code true} when the click was consumed
     */
    protected boolean handleListClick(double mouseX, double mouseY, int button) {
        return false;
    }

    /**
     * Handles list scrolling using normalized coordinates.
     *
     * @param mouseX the scroll x position
     * @param mouseY the scroll y position
     * @param verticalAmount the vertical scroll amount
     * @return {@code true} when the scroll was consumed
     */
    protected boolean handleListScroll(double mouseX, double mouseY, double verticalAmount) {
        return false;
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.gui.Click event, boolean doubleClick) {
        if (event != null && handleListClick(event.x(), event.y(), event.button())) {
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    /**
     * Handles list scrolling once for all list-based screens.
     *
     * @param mouseX the scroll x position
     * @param mouseY the scroll y position
     * @param horizontalAmount the horizontal scroll amount
     * @param verticalAmount the vertical scroll amount
     * @return {@code true} when the scroll was consumed
     */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (handleListScroll(mouseX, mouseY, verticalAmount)) {
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
}
