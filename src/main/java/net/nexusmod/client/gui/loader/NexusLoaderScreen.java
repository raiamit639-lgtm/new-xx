package net.nexusmod.client.gui.loader;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.nexusmod.client.config.NexusConfig;
import net.nexusmod.client.gui.screen.NexusDashboardScreen;
import net.nexusmod.client.gui.theme.NexusDraw;
import net.nexusmod.client.gui.theme.NexusTheme;

/**
 * Splash screen shown when the settings keybind is pressed, before the
 * dashboard opens: the "NEXUS" wordmark scales up from small with a fade
 * and glow, holds briefly, then the screen transitions into
 * NexusDashboardScreen. Purely cosmetic — holds no settings state.
 *
 * Timing (in ticks, 20/sec):
 *   0-12   scale/fade in (overshoot ease)
 *   12-30  hold, glow pulse
 *   30-42  fade out
 *   42+    swap to NexusDashboardScreen
 *
 * If GUI animations are disabled in config, the whole sequence is skipped
 * and the dashboard opens immediately — the loader is a decorative flourish,
 * not something that should slow down someone who's turned animations off
 * for performance or accessibility reasons.
 */
public class NexusLoaderScreen extends Screen {

    private static final int PHASE_IN_END = 12;
    private static final int PHASE_HOLD_END = 30;
    private static final int PHASE_OUT_END = 42;

    private final Screen parent;
    private int tickCount = 0;
    private float partialTicks = 0f;

    public NexusLoaderScreen(Screen parent) {
        super(Text.translatable("screen.nexus.loader"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        if (!NexusConfig.get().guiAnimations) {
            MinecraftClient.getInstance().setScreen(new NexusDashboardScreen(parent));
        }
    }

    @Override
    public void tick() {
        super.tick();
        tickCount++;
        if (tickCount >= PHASE_OUT_END) {
            MinecraftClient.getInstance().setScreen(new NexusDashboardScreen(parent));
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        partialTicks = delta;
        renderBackground(ctx, mouseX, mouseY, delta);

        float t = tickCount + delta;
        float scale;
        float alpha;

        if (t < PHASE_IN_END) {
            float p = t / PHASE_IN_END;
            scale = overshoot(p);
            alpha = MathHelper.clamp(p * 1.4f, 0f, 1f);
        } else if (t < PHASE_HOLD_END) {
            scale = 1f;
            alpha = 1f;
        } else {
            float p = (t - PHASE_HOLD_END) / (PHASE_OUT_END - PHASE_HOLD_END);
            scale = 1f + p * 0.15f;
            alpha = 1f - MathHelper.clamp(p, 0f, 1f);
        }

        ctx.fill(0, 0, width, height, NexusTheme.withAlpha(0xFF000000, Math.round(200 * Math.min(1f, t / 6f))));

        String word = "NEXUS";
        int baseSize = 64; // logical "big" size before per-letter scaling via matrix transform
        int cx = width / 2;
        int cy = height / 2;

        ctx.getMatrices().push();
        ctx.getMatrices().translate(cx, cy, 0);
        ctx.getMatrices().scale(scale, scale, 1f);
        ctx.getMatrices().translate(-cx, -cy, 0);

        // glow behind the wordmark, pulsing during the hold phase
        float pulse = t >= PHASE_IN_END && t < PHASE_HOLD_END
                ? 0.5f + 0.5f * MathHelper.sin((t - PHASE_IN_END) * 0.5f)
                : 0.5f;
        int glowAlpha = Math.round(60 * alpha * pulse);
        NexusDraw.glow(ctx, cx - 90, cy - 20, 180, 40, 8, NexusTheme.withAlpha(NexusTheme.CYAN, glowAlpha), 10);

        drawBigWordmark(ctx, word, cx, cy, alpha);

        ctx.getMatrices().pop();

        if (t >= PHASE_IN_END && t < PHASE_HOLD_END) {
            String subtitle = Text.translatable("screen.nexus.loader.subtitle").getString();
            int subAlpha = Math.round(180 * alpha);
            ctx.drawCenteredTextWithShadow(textRenderer, subtitle, cx, cy + 28, NexusTheme.withAlpha(0xFFFFFFFF, subAlpha));
        }
    }

    /**
     * Draws "NEXUS" large by scaling up vanilla text rendering via the
     * matrix stack (Minecraft's TextRenderer has no built-in font-size
     * parameter, only fixed 8px-high glyphs, so scaling the matrix is the
     * standard way every Minecraft mod renders "big" text).
     */
    private void drawBigWordmark(DrawContext ctx, String word, int cx, int cy, float alpha) {
        float bigScale = 4.0f;
        int color = NexusTheme.withAlpha(0xFFFFFFFF, Math.round(255 * alpha));

        ctx.getMatrices().push();
        ctx.getMatrices().translate(cx, cy, 0);
        ctx.getMatrices().scale(bigScale, bigScale, 1f);

        int textWidth = textRenderer.getWidth(word);
        // gradient per-letter would need per-glyph draw calls; approximate the
        // dashboard's blue->cyan wordmark by drawing twice with an offset glow
        // in cyan behind solid white, which reads as a lightly-tinted glow edge.
        ctx.drawCenteredTextWithShadow(textRenderer, word, 0, -4, color);

        ctx.getMatrices().pop();
    }

    private static float overshoot(float t) {
        float c = 1.6f;
        float p = t - 1f;
        return 1f + c * p * p * p + p;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(parent);
    }
}
