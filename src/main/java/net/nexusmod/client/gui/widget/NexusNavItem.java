package net.nexusmod.client.gui.widget;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.nexusmod.client.gui.theme.NexusDraw;
import net.nexusmod.client.gui.theme.NexusTheme;

import java.util.function.Consumer;

/**
 * A single sidebar entry (Home / Video / Quality / Performance / ...).
 * Active state gets the left accent bar + gradient background glow from
 * `.nav-item.active`; inactive items get a subtle hover highlight.
 */
public class NexusNavItem extends ClickableWidget {

    private boolean active;
    private final Consumer<NexusNavItem> onSelect;

    public NexusNavItem(int x, int y, int width, int height, Text label, boolean active, Consumer<NexusNavItem> onSelect) {
        super(x, y, width, height, label);
        this.active = active;
        this.onSelect = onSelect;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int radius = 3;
        if (active) {
            NexusDraw.hGradient(ctx, getX(), getY(), width, height,
                    NexusTheme.withAlpha(NexusTheme.BLUE, 41), NexusTheme.withAlpha(NexusTheme.CYAN, 15));
            NexusDraw.outline1px(ctx, getX(), getY(), width, height, NexusTheme.withAlpha(NexusTheme.CYAN, 71), radius);
            // left accent bar
            int barH = 18;
            int barY = getY() + (height - barH) / 2;
            NexusDraw.vGradient(ctx, getX(), barY, 3, barH, NexusTheme.BLUE, NexusTheme.CYAN);
        } else if (isHovered()) {
            NexusDraw.roundedFill(ctx, getX(), getY(), width, height, NexusTheme.withAlpha(0xFFFFFFFF, 11), radius);
        }

        int textColor = active ? 0xFFFFFFFF : (isHovered() ? NexusTheme.TEXT_PRIMARY : NexusTheme.TEXT_SECONDARY);
        ctx.drawText(MinecraftClient.getInstance().textRenderer, getMessage(), getX() + 14, getY() + (height - 8) / 2, textColor, false);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        onSelect.accept(this);
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        builder.put(NarrationPart.TITLE, getMessage());
    }
}
