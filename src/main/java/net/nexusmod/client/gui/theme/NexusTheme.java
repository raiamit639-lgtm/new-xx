package net.nexusmod.client.gui.theme;

/**
 * Central color/layout constants for the Nexus GUI. Mirrors the CSS
 * custom properties from the dashboard design reference (--bg, --blue,
 * --cyan, etc.) so every screen and widget pulls from one place instead
 * of hard-coding hex values.
 *
 * Colors are ARGB ints (0xAARRGGBB) since that's what DrawContext expects.
 */
public final class NexusTheme {

    private NexusTheme() {}

    // --- Base surfaces ---
    public static final int BG = 0xFF121212;
    public static final int BG_DEEP = 0xFF0A0A0C;
    public static final int GLASS = 0x0BFFFFFF;          // rgba(255,255,255,0.045)
    public static final int GLASS_STRONG = 0x14FFFFFF;   // rgba(255,255,255,0.08)
    public static final int GLASS_BORDER = 0x14FFFFFF;   // rgba(255,255,255,0.08)
    public static final int GLASS_BORDER_HOVER = 0x2EFFFFFF;

    // --- Accent gradient (blue -> cyan) ---
    public static final int BLUE = 0xFF3B82F6;
    public static final int CYAN = 0xFF00E5FF;
    public static final int SUCCESS = 0xFF22D97D;
    public static final int WARNING = 0xFFF5A623;
    public static final int PURPLE = 0xFFB084F5;

    // --- Text ---
    public static final int TEXT_PRIMARY = 0xFFF1F3F5;
    public static final int TEXT_SECONDARY = 0xFF9CA3AF;
    public static final int TEXT_MUTED = 0xFF5E6470;

    // --- Radii (in px, used as a hint for how many corner-rounding steps to draw) ---
    public static final int RADIUS_LG = 6;
    public static final int RADIUS_MD = 5;
    public static final int RADIUS_SM = 3;

    /** Blue -> Cyan, used for active nav items, sliders, primary buttons, presets. */
    public static int accentGradient(float t) {
        return lerpArgb(BLUE, CYAN, t);
    }

    /** Performance color ramp: blue (low) -> teal -> green (high). Matches AnimatedSliderWidget's feel. */
    public static int performanceGradient(float t) {
        int from = 0xFF4C6EF5;
        int mid = 0xFF4CA0C9;
        int to = 0xFF4CD97D;
        return t < 0.5f ? lerpArgb(from, mid, t * 2f) : lerpArgb(mid, to, (t - 0.5f) * 2f);
    }

    public static int withAlpha(int argb, int alpha) {
        return (alpha << 24) | (argb & 0x00FFFFFF);
    }

    /** Scales an existing alpha channel by a 0..1 factor (for fade animations). */
    public static int scaleAlpha(int argb, float factor) {
        int a = (argb >>> 24) & 0xFF;
        int scaled = Math.round(a * clamp01(factor));
        return withAlpha(argb, scaled);
    }

    public static int lerpArgb(int from, int to, float t) {
        t = clamp01(t);
        int fa = (from >>> 24) & 0xFF, fr = (from >> 16) & 0xFF, fg = (from >> 8) & 0xFF, fb = from & 0xFF;
        int ta = (to >>> 24) & 0xFF, tr = (to >> 16) & 0xFF, tg = (to >> 8) & 0xFF, tb = to & 0xFF;
        int a = Math.round(fa + (ta - fa) * t);
        int r = Math.round(fr + (tr - fr) * t);
        int g = Math.round(fg + (tg - fg) * t);
        int b = Math.round(fb + (tb - fb) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static float clamp01(float v) {
        return v < 0f ? 0f : Math.min(v, 1f);
    }
}
