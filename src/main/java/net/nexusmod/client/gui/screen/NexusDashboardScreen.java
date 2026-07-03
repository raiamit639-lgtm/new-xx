package net.nexusmod.client.gui.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.nexusmod.client.config.NexusConfig;
import net.nexusmod.client.gui.theme.NexusDraw;
import net.nexusmod.client.gui.theme.NexusTheme;
import net.nexusmod.client.gui.widget.NexusNavItem;
import net.nexusmod.client.gui.widget.NexusStatCard;
import net.nexusmod.client.perf.NexusOptionsSync;
import net.nexusmod.client.perf.PerfSampler;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The full Nexus settings dashboard: topbar (logo, search, FPS chip),
 * sidebar nav across nine tabs, a scrollable content area with per-tab
 * cards, and a bottom status bar with Reset/Done.
 *
 * Layout constants below intentionally mirror the CSS reference's pixel
 * proportions (topbar height, sidebar width, card gaps) scaled down to fit
 * Minecraft's GUI space, rather than being arbitrary guesses.
 */
public class NexusDashboardScreen extends Screen {

    private static final int TOPBAR_H = 40;
    private static final int BOTTOMBAR_H = 34;
    private static final int SIDEBAR_W = 130;
    private static final int PANEL_MARGIN = 20;

    private final Screen parent;
    private NexusTab activeTab = NexusTab.PERFORMANCE;

    private final List<NexusNavItem> navItems = new ArrayList<>();
    private final Map<NexusTab, TabContent> tabContents = new LinkedHashMap<>();
    private TextFieldWidget searchField;

    private final List<NexusStatCard> statCards = new ArrayList<>();
    private final PerfSampler perf = PerfSampler.get();

    private int panelX, panelY, panelW, panelH;
    private int scrollOffset = 0;
    private int maxScroll = 0;

    // fade/slide-in when the screen first opens, matching .app's appIn keyframes
    private float openAnim = 0f;

    public NexusDashboardScreen(Screen parent) {
        super(Text.translatable("screen.nexus.dashboard"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        panelW = Math.min(1200, width - PANEL_MARGIN * 2);
        panelH = Math.min(760, height - PANEL_MARGIN * 2);
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;

        buildTopbar();
        buildSidebar();
        buildBottombar();
        rebuildTabContent();
    }

    private void buildTopbar() {
        int searchX = panelX + 190;
        int searchW = 220;
        searchField = new TextFieldWidget(textRenderer, searchX + 10, panelY + 11, searchW - 20, 18,
                Text.translatable("gui.nexus.search_placeholder"));
        searchField.setPlaceholder(Text.translatable("gui.nexus.search_placeholder"));
        searchField.setMaxLength(64);
        searchField.setDrawsBackground(false);
        searchField.setChangedListener(this::onSearchChanged);
        addSelectableChild(searchField);
    }

    private void buildSidebar() {
        navItems.clear();
        int navY = panelY + TOPBAR_H + 12;
        int itemH = 26;
        NexusTab[] mainTabs = {
                NexusTab.HOME, NexusTab.VIDEO, NexusTab.QUALITY, NexusTab.PERFORMANCE,
                NexusTab.ADVANCED, NexusTab.COMPATIBILITY, NexusTab.ANIMATIONS, NexusTab.EXPERIMENTAL
        };
        for (NexusTab tab : mainTabs) {
            NexusNavItem item = new NexusNavItem(panelX + 10, navY, SIDEBAR_W - 20, itemH,
                    tab.title(), tab == activeTab, this::onNavClicked);
            navItems.add(item);
            addDrawableChild(item);
            navY += itemH + 2;
        }
        navY += 12; // divider gap
        NexusNavItem about = new NexusNavItem(panelX + 10, navY, SIDEBAR_W - 20, itemH,
                NexusTab.ABOUT.title(), activeTab == NexusTab.ABOUT, this::onNavClicked);
        navItems.add(about);
        addDrawableChild(about);
    }

    private void onNavClicked(NexusNavItem clicked) {
        NexusTab[] mainTabs = {
                NexusTab.HOME, NexusTab.VIDEO, NexusTab.QUALITY, NexusTab.PERFORMANCE,
                NexusTab.ADVANCED, NexusTab.COMPATIBILITY, NexusTab.ANIMATIONS, NexusTab.EXPERIMENTAL
        };
        int idx = navItems.indexOf(clicked);
        NexusTab target = (idx == navItems.size() - 1) ? NexusTab.ABOUT : mainTabs[idx];
        setActiveTab(target);
    }

    private void setActiveTab(NexusTab tab) {
        if (tab == activeTab) return;
        activeTab = tab;
        for (int i = 0; i < navItems.size(); i++) {
            NexusNavItem item = navItems.get(i);
            boolean isLast = i == navItems.size() - 1;
            item.setActive(isLast ? tab == NexusTab.ABOUT : item.getMessage().equals(tab.title()));
        }
        scrollOffset = 0;
        rebuildTabContent();
    }

    private void buildBottombar() {
        int btnY = panelY + panelH - BOTTOMBAR_H + 6;
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.nexus.reset"), b -> resetCurrentTab())
                .dimensions(panelX + panelW - 220, btnY, 90, 22).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.nexus.done"), b -> close())
                .dimensions(panelX + panelW - 120, btnY, 100, 22).build());
    }

    private void resetCurrentTab() {
        // Reset applies the Balanced preset as a sane baseline, matching the
        // dashboard's default-selected preset chip.
        NexusConfig.get().applyPreset(NexusConfig.Preset.BALANCED);
        NexusOptionsSync.apply();
        rebuildTabContent();
    }

    private void onSearchChanged(String query) {
        rebuildTabContent(); // filtering handled inside TabContent using current query
    }

    private void rebuildTabContent() {
        for (TabContent content : tabContents.values()) {
            content.widgets().forEach(this::remove);
        }
        tabContents.clear();
        statCards.forEach(this::remove);
        statCards.clear();

        int contentX = panelX + SIDEBAR_W + 20;
        int contentY = panelY + TOPBAR_H + 20;
        int contentW = panelX + panelW - 20 - contentX;

        // stat strip shows on every tab except About, matching the reference's
        // "always-visible performance context" role for the graph row.
        if (activeTab != NexusTab.ABOUT) {
            buildStatCards(contentX, contentY, contentW);
            contentY += 70;
        }

        String query = searchField != null ? searchField.getText() : "";
        TabContent content = NexusTabBuilder.build(activeTab, contentX, contentY, contentW, query, this::onSettingChanged);
        tabContents.put(activeTab, content);
        content.widgets().forEach(this::addDrawableChild);

        maxScroll = Math.max(0, content.contentHeight() - (panelY + panelH - BOTTOMBAR_H - contentY));
    }

    private void buildStatCards(int x, int y, int totalW) {
        int gap = 10;
        int cardW = (totalW - gap * 3) / 4;
        int cardH = 58;

        statCards.add(new NexusStatCard(x, y, cardW, cardH, "FPS", perf.fps,
                v -> String.valueOf(Math.round(v)), NexusTheme.SUCCESS, false));
        statCards.add(new NexusStatCard(x + (cardW + gap), y, cardW, cardH, "GPU (est.)", perf.gpuLoadEstimate,
                v -> Math.round(v) + "%", NexusTheme.CYAN, false));
        statCards.add(new NexusStatCard(x + (cardW + gap) * 2, y, cardW, cardH, "RAM", perf.ramMb,
                v -> String.format("%.1fG", v / 1024f), NexusTheme.BLUE, false));
        statCards.add(new NexusStatCard(x + (cardW + gap) * 3, y, cardW, cardH, "VRAM", perf.vramMb,
                v -> String.format("%.1fG", v / 1024f), NexusTheme.PURPLE, true));

        statCards.forEach(this::addDrawableChild);
    }

    private void onSettingChanged() {
        NexusConfig cfg = NexusConfig.get();
        cfg.activePreset = NexusConfig.Preset.CUSTOM;
        cfg.save();
        NexusOptionsSync.apply();
    }

    @Override
    public void tick() {
        super.tick();
        perf.sample();
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        NexusConfig cfg = NexusConfig.get();
        openAnim = cfg.guiAnimations ? MathHelper.lerp(0.18f, openAnim, 1f) : 1f;

        renderBackground(ctx, mouseX, mouseY, delta);
        ctx.fill(0, 0, width, height, NexusTheme.withAlpha(0xFF000000, Math.round(140 * openAnim)));

        renderPanel(ctx);
        renderTopbar(ctx, mouseX, mouseY);
        renderSidebarDivider(ctx);
        renderPageHeader(ctx);
        renderBottombar(ctx);

        super.render(ctx, mouseX, mouseY, delta);

        if (searchField != null) searchField.render(ctx, mouseX, mouseY, delta);
    }

    private void renderPanel(DrawContext ctx) {
        NexusDraw.roundedFill(ctx, panelX, panelY, panelW, panelH, NexusTheme.BG_DEEP, NexusTheme.RADIUS_LG);
        NexusDraw.outline1px(ctx, panelX, panelY, panelW, panelH, NexusTheme.withAlpha(0xFFFFFFFF, 15), NexusTheme.RADIUS_LG);
    }

    private void renderTopbar(DrawContext ctx, int mouseX, int mouseY) {
        ctx.fill(panelX + 1, panelY + 1, panelX + panelW - 1, panelY + TOPBAR_H, NexusTheme.withAlpha(0xFFFFFFFF, 8));
        ctx.fill(panelX + 1, panelY + TOPBAR_H, panelX + panelW - 1, panelY + TOPBAR_H + 1, NexusTheme.GLASS_BORDER);

        NexusDraw.hGradient(ctx, panelX + 12, panelY + 8, 24, 24, NexusTheme.BLUE, NexusTheme.CYAN);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("N"), panelX + 24, panelY + 15, 0xFF06121A);
        ctx.drawText(textRenderer, Text.translatable("gui.nexus.title"), panelX + 44, panelY + 15, NexusTheme.TEXT_PRIMARY, false);

        NexusDraw.glassPanel(ctx, panelX + 190, panelY + 8, 220, 24, 3);

        int fps = Math.round(perf.fps.latest());
        int chipW = 70;
        int chipX = panelX + panelW - 12 - chipW;
        NexusDraw.roundedFill(ctx, chipX, panelY + 7, chipW, 26, NexusTheme.withAlpha(NexusTheme.SUCCESS, 26), 4);
        NexusDraw.outline1px(ctx, chipX, panelY + 7, chipW, 26, NexusTheme.withAlpha(NexusTheme.SUCCESS, 64), 4);
        ctx.fill(chipX + 8, panelY + 17, chipX + 11, panelY + 20, NexusTheme.SUCCESS);
        ctx.drawText(textRenderer, Text.literal(String.valueOf(fps)), chipX + 16, panelY + 14, NexusTheme.SUCCESS, false);
    }

    private void renderSidebarDivider(DrawContext ctx) {
        int x = panelX + SIDEBAR_W;
        ctx.fill(x, panelY + TOPBAR_H + 1, x + 1, panelY + panelH - BOTTOMBAR_H, NexusTheme.GLASS_BORDER);
    }

    private void renderPageHeader(DrawContext ctx) {
        int x = panelX + SIDEBAR_W + 20;
        int y = panelY + TOPBAR_H + 8;
        ctx.drawText(textRenderer, activeTab.title(), x, y, NexusTheme.TEXT_PRIMARY, false);
        ctx.drawText(textRenderer, activeTab.subtitle(), x, y + 11, NexusTheme.TEXT_SECONDARY, false);
    }

    private void renderBottombar(DrawContext ctx) {
        int y = panelY + panelH - BOTTOMBAR_H;
        ctx.fill(panelX + 1, y, panelX + panelW - 1, y + 1, NexusTheme.GLASS_BORDER);
        ctx.fill(panelX + 1, y + 1, panelX + panelW - 1, panelY + panelH - 1, NexusTheme.withAlpha(0xFFFFFFFF, 8));

        int dotY = y + 16;
        ctx.fill(panelX + 16, dotY - 2, panelX + 19, dotY + 1, NexusTheme.SUCCESS);
        ctx.drawText(textRenderer, Text.translatable("gui.nexus.config_saved"), panelX + 26, dotY - 4, NexusTheme.TEXT_MUTED, false);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scrollOffset = MathHelper.clamp(scrollOffset - (int) (verticalAmount * 14), 0, maxScroll);
        TabContent content = tabContents.get(activeTab);
        if (content != null) content.setScroll(scrollOffset);
        return true;
    }

    @Override
    public void close() {
        NexusConfig.get().save();
        MinecraftClient.getInstance().setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
