package net.nexusmod.client.gui.theme;

import net.minecraft.client.gui.DrawContext;

/**
 * Low-level drawing helpers shared by every Nexus widget/screen. Minecraft's
 * DrawContext only gives us flat-fill rectangles and text, so gradients,
 * "rounded" corners and glass-panel looks are all built out of that.
 *
 * Rounding is faked with a fixed corner-cut pattern (a handful of 1px fills
 * per corner) since there's no true rounded-rect primitive or clipping in
 * vanilla DrawContext. It reads fine at UI scale and costs nothing extra
 * beyond a few extra fill() calls.
 */
public final class NexusDraw {

    private NexusDraw() {}

    /** Horizontal gradient fill, left color -> right color. */
    public static void hGradient(DrawContext ctx, int x, int y, int w, int h, int colorLeft, int colorRight) {
        if (w <= 0 || h <= 0) return;
        // DrawContext#fillGradient interpolates top-left->top-right / bottom-left->bottom-right
        // when given the same pair of colors on both edges, giving us a clean horizontal gradient.
        ctx.fillGradient(x, y, x + w, y + h, colorLeft, colorRight);
    }

    /** Vertical gradient fill, top color -> bottom color. */
    public static void vGradient(DrawContext ctx, int x, int y, int w, int h, int colorTop, int colorBottom) {
        if (w <= 0 || h <= 0) return;
        for (int i = 0; i < h; i++) {
            float t = h <= 1 ? 0f : (float) i / (h - 1);
            int c = NexusTheme.lerpArgb(colorTop, colorBottom, t);
            ctx.fill(x, y + i, x + w, y + i + 1, c);
        }
    }

    /** Flat fill with faked rounded corners (cuts 1-2px triangular notches off each corner). */
    public static void roundedFill(DrawContext ctx, int x, int y, int w, int h, int color, int radius) {
        if (w <= 0 || h <= 0) return;
        radius = Math.max(0, Math.min(radius, Math.min(w, h) / 2));
        if (radius == 0) {
            ctx.fill(x, y, x + w, y + h, color);
            return;
        }
        // Core cross shape covers everything except the four corner squares.
        ctx.fill(x + radius, y, x + w - radius, y + h, color);
        ctx.fill(x, y + radius, x + radius, y + h - radius, color);
        ctx.fill(x + w - radius, y + radius, x + w, y + h - radius, color);
        // Corners: stepped diagonal to approximate a curve.
        for (int i = 0; i < radius; i++) {
            int inset = (int) (radius - Math.sqrt((float) (2 * radius * i - i * i)));
            inset = Math.max(0, Math.min(inset, radius));
            int rowY1 = y + i;
            int rowY2 = y + h - 1 - i;
            ctx.fill(x + inset, rowY1, x + radius, rowY1 + 1, color);
            ctx.fill(x + w - radius, rowY1, x + w - inset, rowY1 + 1, color);
            ctx.fill(x + inset, rowY2, x + radius, rowY2 + 1, color);
            ctx.fill(x + w - radius, rowY2, x + w - inset, rowY2 + 1, color);
        }
    }

    /** 1px rounded outline. */
    public static void roundedOutline(DrawContext ctx, int x, int y, int w, int h, int color, int radius) {
        roundedFill(ctx, x, y, w, h, color, radius);
        int inset = 1;
        if (w - 2 * inset > 0 && h - 2 * inset > 0) {
            roundedFill(ctx, x + inset, y + inset, w - 2 * inset, h - 2 * inset, 0x00000000, Math.max(0, radius - inset));
        }
    }

    /**
     * A "glass" panel: translucent fill + faint border. Approximates the
     * dashboard's backdrop-filter blur — Minecraft has no cheap GL blur
     * available here, so we fake the effect with layered translucency
     * instead of literally blurring the background.
     */
    public static void glassPanel(DrawContext ctx, int x, int y, int w, int h, int radius) {
        glassPanel(ctx, x, y, w, h, radius, NexusTheme.GLASS, NexusTheme.GLASS_BORDER);
    }

    public static void glassPanel(DrawContext ctx, int x, int y, int w, int h, int radius, int fill, int border) {
        roundedFill(ctx, x, y, w, h, fill, radius);
        outline1px(ctx, x, y, w, h, border, radius);
    }

    /** Cheap 1px border using thin edge fills (works with or without rounding). */
    public static void outline1px(DrawContext ctx, int x, int y, int w, int h, int color, int radius) {
        int r = Math.max(0, Math.min(radius, Math.min(w, h) / 2));
        ctx.fill(x + r, y, x + w - r, y + 1, color);
        ctx.fill(x + r, y + h - 1, x + w - r, y + h, color);
        ctx.fill(x, y + r, x + 1, y + h - r, color);
        ctx.fill(x + w - 1, y + r, x + w, y + h - r, color);
        for (int i = 0; i < r; i++) {
            int inset = (int) (r - Math.sqrt((float) (2 * r * i - i * i)));
            inset = Math.max(0, Math.min(inset, r));
            ctx.fill(x + inset, y + i, x + inset + 1, y + i + 1, color);
            ctx.fill(x + w - inset - 1, y + i, x + w - inset, y + i + 1, color);
            ctx.fill(x + inset, y + h - 1 - i, x + inset + 1, y + h - i, color);
            ctx.fill(x + w - inset - 1, y + h - 1 - i, x + w - inset, y + h - i, color);
        }
    }

    /** Soft outer glow by stacking shrinking, fading rounded outlines. Cheap stand-in for box-shadow. */
    public static void glow(DrawContext ctx, int x, int y, int w, int h, int radius, int color, int spread) {
        for (int i = spread; i > 0; i--) {
            int a = ((color >>> 24) & 0xFF) * i / (spread * 3);
            int c = NexusTheme.withAlpha(color, a);
            roundedOutline(ctx, x - i, y - i, w + i * 2, h + i * 2, c, radius + i);
        }
    }
}
