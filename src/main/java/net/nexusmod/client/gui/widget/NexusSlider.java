package net.nexusmod.client.gui.widget;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.nexusmod.client.config.NexusConfig;
import net.nexusmod.client.gui.theme.NexusDraw;
import net.nexusmod.client.gui.theme.NexusTheme;

import java.util.function.Function;
import java.util.function.IntConsumer;

/**
 * Slim gradient-fill slider matching the dashboard's `.slider-track` /
 * `.slider-thumb` look (thin 4px track, blue->cyan fill, white glowing
 * thumb) instead of the old thick single-color bar. Value label is drawn
 * to the right as a separate mono-styled readout rather than centered
 * over the bar, matching `.slider-val`.
 */
public class NexusSlider extends SliderWidget {

    private final int min;
    private final int max;
    private final Function<Integer, Text> valueFormatter;
    private final IntConsumer onChange;
    private double displayed;

    public NexusSlider(int x, int y, int width, int height, int min, int max, int initial,
                        Function<Integer, Text> valueFormatter, IntConsumer onChange) {
        super(x, y, width, height, Text.empty(), progress(initial, min, max));
        this.min = min;
        this.max = max;
        this.valueFormatter = valueFormatter;
        this.onChange = onChange;
        this.displayed = this.value;
        updateMessage();
    }

    private static double progress(int v, int min, int max) {
        return MathHelper.clamp((v - min) / (double) (max - min), 0.0, 1.0);
    }

    private int currentValue() {
        return min + (int) Math.round(value * (max - min));
    }

    @Override
    protected void updateMessage() {
        setMessage(valueFormatter.apply(currentValue()));
    }

    @Override
    protected void applyValue() {
        onChange.accept(currentValue());
    }

    @Override
    public void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
        NexusConfig cfg = NexusConfig.get();
        if (cfg.guiAnimations) {
            float speed = 0.28f * Math.max(0.5f, cfg.animationSpeed);
            displayed = MathHelper.lerp(speed, displayed, value);
            if (Math.abs(displayed - value) < 0.001) displayed = value;
        } else {
            displayed = value;
        }

        // reserve space on the right for the value readout, mirroring .slider-val's min-width
        int valueColW = 44;
        int trackW = width - valueColW - 8;
        int trackH = 4;
        int trackY = getY() + (height - trackH) / 2;

        NexusDraw.roundedFill(ctx, getX(), trackY, trackW, trackH, NexusTheme.withAlpha(0xFFFFFFFF, 23), trackH / 2);

        int filled = (int) (trackW * displayed);
        if (filled > 0) {
            NexusDraw.roundedFill(ctx, getX(), trackY, filled, trackH,
                    NexusTheme.lerpArgb(NexusTheme.BLUE, NexusTheme.CYAN, (float) displayed), trackH / 2);
        }

        int thumbSize = 14;
        int thumbX = getX() + filled - thumbSize / 2;
        int thumbY = getY() + (height - thumbSize) / 2;
        NexusDraw.glow(ctx, thumbX, thumbY, thumbSize, thumbSize, thumbSize / 2, NexusTheme.CYAN, 2);
        NexusDraw.roundedFill(ctx, thumbX, thumbY, thumbSize, thumbSize, 0xFFFFFFFF, thumbSize / 2);

        ctx.drawText(net.minecraft.client.MinecraftClient.getInstance().textRenderer,
                getMessage(), getX() + trackW + 8, getY() + (height - 8) / 2,
                NexusTheme.TEXT_SECONDARY, false);
    }
}
