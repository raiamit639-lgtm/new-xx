package net.nexusmod.client.gui.widget;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.nexusmod.client.gui.theme.NexusTheme;

/**
 * Non-interactive text label used for setting-row names and card section
 * headers, matching `.setting-label` / `.card-head h3`. Implemented as a
 * ClickableWidget purely so it participates in the same scroll
 * repositioning system as everything else in TabContent.
 */
public class NexusLabel extends ClickableWidget {

    private final Text tooltip;

    public NexusLabel(int x, int y, int width, int height, Text label) {
        this(x, y, width, height, label, null);
    }

    public NexusLabel(int x, int y, int width, int height, Text label, Text tooltip) {
        super(x, y, width, height, label);
        this.tooltip = tooltip;
        if (tooltip != null) {
            setTooltip(net.minecraft.client.gui.tooltip.Tooltip.of(tooltip));
        }
    }

    @Override
    protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.drawText(MinecraftClient.getInstance().textRenderer, getMessage(), getX(), getY() + (height - 8) / 2,
                NexusTheme.TEXT_PRIMARY, false);
        if (tooltip != null) {
            int dotX = getX() + MinecraftClient.getInstance().textRenderer.getWidth(getMessage()) + 6;
            int dotY = getY() + (height - 8) / 2;
            ctx.fill(dotX, dotY, dotX + 8, dotY + 8, NexusTheme.withAlpha(0xFFFFFFFF, 15));
            ctx.drawText(MinecraftClient.getInstance().textRenderer, Text.literal("i"), dotX + 3, dotY, NexusTheme.TEXT_MUTED, false);
        }
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        // decorative only
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        // no interactive narration needed
    }
}
