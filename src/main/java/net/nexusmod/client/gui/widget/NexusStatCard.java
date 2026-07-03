package net.nexusmod.client.gui.widget;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.nexusmod.client.gui.theme.NexusDraw;
import net.nexusmod.client.gui.theme.NexusTheme;
import net.nexusmod.client.perf.StatHistory;

import java.util.function.Function;

/**
 * One of the four top stat cards (FPS / GPU / RAM / VRAM) with a rolling
 * sparkline graph, matching `.graph-card`. Non-interactive (not clickable),
 * but implemented as a widget so it slots into the same layout/tick system
 * as everything else.
 */
public class NexusStatCard extends ClickableWidget {

    private final String label;
    private final StatHistory history;
    private final Function<Float, String> formatter;
    private final int accentColor;
    private final boolean unavailableIsPossible;

    public NexusStatCard(int x, int y, int width, int height, String label, StatHistory history,
                          Function<Float, String> formatter, int accentColor, boolean unavailableIsPossible) {
        super(x, y, width, height, Text.literal(label));
        this.label = label;
        this.history = history;
        this.formatter = formatter;
        this.accentColor = accentColor;
        this.unavailableIsPossible = unavailableIsPossible;
    }

    @Override
    protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
        NexusDraw.glassPanel(ctx, getX(), getY(), width, height, 4);

        var tr = MinecraftClient.getInstance().textRenderer;
        int pad = 8;
        ctx.drawText(tr, Text.literal(label), getX() + pad, getY() + pad, NexusTheme.TEXT_MUTED, false);

        float latest = history.latest();
        boolean unavailable = unavailableIsPossible && latest < 0;
        String valueText = unavailable ? "—" : formatter.apply(latest);
        ctx.drawText(tr, Text.literal(valueText), getX() + pad, getY() + pad + 11, accentColor, false);

        // sparkline
        int graphX = getX() + pad;
        int graphY = getY() + height - 18;
        int graphW = width - pad * 2;
        int graphH = 14;

        if (!unavailable && history.size() > 1) {
            int n = history.size();
            float prevX = graphX;
            float prevY = graphY + graphH - history.normalizedAt(0) * graphH;
            for (int i = 1; i < n; i++) {
                float xf = graphX + (graphW * (float) i / (n - 1));
                float yf = graphY + graphH - history.normalizedAt(i) * graphH;
                drawLine(ctx, prevX, prevY, xf, yf, accentColor);
                prevX = xf;
                prevY = yf;
            }
        }
    }

    /** Thin 1px line via short horizontal fills stepped along x — cheap Bresenham-ish approximation. */
    private static void drawLine(DrawContext ctx, float x0, float y0, float x1, float y1, int color) {
        int steps = Math.max(1, (int) Math.abs(x1 - x0));
        for (int i = 0; i < steps; i++) {
            float t0 = (float) i / steps;
            float t1 = (float) (i + 1) / steps;
            int sx0 = Math.round(x0 + (x1 - x0) * t0);
            int sy0 = Math.round(y0 + (y1 - y0) * t0);
            int sx1 = Math.round(x0 + (x1 - x0) * t1);
            int sy1 = Math.round(y0 + (y1 - y0) * t1);
            int lo = Math.min(sy0, sy1);
            int hi = Math.max(sy0, sy1) + 1;
            ctx.fill(sx0, lo, sx1 + 1, hi, color);
        }
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        // decorative/informational only, nothing to narrate as clickable
    }
}
