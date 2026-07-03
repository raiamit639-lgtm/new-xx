package net.nexusmod.client.gui.screen;

import net.minecraft.client.gui.widget.ClickableWidget;

import java.util.List;

/**
 * Holds the widgets built for one tab plus the total logical content
 * height (for scroll clamping) and each widget's "home" Y position, so
 * scrolling can reposition them without rebuilding on every scroll tick.
 */
public class TabContent {

    private final List<ClickableWidget> widgetList;
    private final List<Integer> homeY;
    private final int contentHeight;

    public TabContent(List<ClickableWidget> widgetList, List<Integer> homeY, int contentHeight) {
        this.widgetList = widgetList;
        this.homeY = homeY;
        this.contentHeight = contentHeight;
    }

    public List<ClickableWidget> widgets() {
        return widgetList;
    }

    public int contentHeight() {
        return contentHeight;
    }

    public void setScroll(int offset) {
        for (int i = 0; i < widgetList.size(); i++) {
            widgetList.get(i).setY(homeY.get(i) - offset);
        }
    }
}
