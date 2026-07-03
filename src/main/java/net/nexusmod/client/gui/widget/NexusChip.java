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
 * Pill-shaped selectable chip, used for the preset row (Ultra Performance /
 * Balanced / Quality / Ultra Quality / Custom) and for filter-style tags.
 * Matches `.preset-chip` / `.preset-chip.selected`.
 */
public class NexusChip extends ClickableWidget {

    private boolean selected;
    private final Consumer<NexusChip> onClick;

    public NexusChip(int x, int y, int width, int height, Text label, boolean selected, Consumer<NexusChip> onClick) {
        super(x, y, width, height, label);
        this.selected = selected;
        this.onClick = onClick;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int radius = height / 2;
        boolean hovered = isHovered();
        int textColor;

        if (selected) {
            NexusDraw.hGradient(ctx, getX(), getY(), width, height, NexusTheme.BLUE, NexusTheme.CYAN);
            NexusDraw.glow(ctx, getX(), getY(), width, height, radius, NexusTheme.CYAN, 3);
            textColor = 0xFF06121A;
        } else {
            int fill = NexusTheme.withAlpha(0xFFFFFFFF, hovered ? 10 : 8);
            NexusDraw.roundedFill(ctx, getX(), getY(), width, height, fill, radius);
            NexusDraw.outline1px(ctx, getX(), getY(), width, height,
                    hovered ? NexusTheme.GLASS_BORDER_HOVER : NexusTheme.GLASS_BORDER, radius);
            textColor = hovered ? NexusTheme.TEXT_PRIMARY : NexusTheme.TEXT_SECONDARY;
        }

        ctx.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer,
                getMessage(), getX() + width / 2, getY() + (height - 8) / 2, textColor);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        onClick.accept(this);
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        builder.put(NarrationPart.TITLE, getMessage());
    }
}
