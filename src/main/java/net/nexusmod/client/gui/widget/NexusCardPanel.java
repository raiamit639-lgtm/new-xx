package net.nexusmod.client.gui.widget;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.nexusmod.client.gui.theme.NexusDraw;
import net.nexusmod.client.gui.theme.NexusTheme;

/**
 * Background + header for a settings card (`.settings-card` / `.card-head`).
 * Purely decorative — rendered first, behind the row widgets that get
 * positioned on top of it by NexusTabBuilder.
 */
public class NexusCardPanel extends ClickableWidget {

    private final int rowCount;

    public NexusCardPanel(int x, int y, int width, int height, Text title, int rowCount) {
        super(x, y, width, height, title);
        this.rowCount = rowCount;
    }

    @Override
    protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
        NexusDraw.glassPanel(ctx, getX(), getY(), width, height, 5);

        var tr = MinecraftClient.getInstance().textRenderer;
        int pad = 12;
        ctx.drawText(tr, getMessage(), getX() + pad, getY() + pad, NexusTheme.TEXT_PRIMARY, false);

        String countText = String.valueOf(rowCount);
        int countW = tr.getWidth(countText) + 12;
        int countX = getX() + width - pad - countW;
        NexusDraw.roundedFill(ctx, countX, getY() + pad - 2, countW, 12, NexusTheme.withAlpha(0xFFFFFFFF, 13), 6);
        ctx.drawText(tr, Text.literal(countText), countX + 6, getY() + pad, NexusTheme.TEXT_MUTED, false);

        ctx.fill(getX() + pad, getY() + pad + 14, getX() + width - pad, getY() + pad + 15, NexusTheme.withAlpha(0xFFFFFFFF, 8));
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
