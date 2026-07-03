package net.nexusmod.client.gui.screen;

import net.minecraft.text.Text;

/**
 * The sidebar sections from the dashboard reference. Each has a short
 * subtitle shown in the page header. Content for each tab is built by
 * NexusDashboardScreen#buildTabContent - this enum only carries display
 * metadata and ordering.
 */
public enum NexusTab {
    HOME("tab.nexus.home", "subtitle.nexus.home"),
    VIDEO("tab.nexus.video", "subtitle.nexus.video"),
    QUALITY("tab.nexus.quality", "subtitle.nexus.quality"),
    PERFORMANCE("tab.nexus.performance", "subtitle.nexus.performance"),
    ADVANCED("tab.nexus.advanced", "subtitle.nexus.advanced"),
    COMPATIBILITY("tab.nexus.compatibility", "subtitle.nexus.compatibility"),
    ANIMATIONS("tab.nexus.animations", "subtitle.nexus.animations"),
    EXPERIMENTAL("tab.nexus.experimental", "subtitle.nexus.experimental"),
    ABOUT("tab.nexus.about", "subtitle.nexus.about");

    private final String titleKey;
    private final String subtitleKey;

    NexusTab(String titleKey, String subtitleKey) {
        this.titleKey = titleKey;
        this.subtitleKey = subtitleKey;
    }

    public Text title() {
        return Text.translatable(titleKey);
    }

    public Text subtitle() {
        return Text.translatable(subtitleKey);
    }
}
