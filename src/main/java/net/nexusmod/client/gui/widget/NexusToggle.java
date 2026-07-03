package net.nexusmod.client.gui.widget;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.nexusmod.client.NexusClient;
import net.nexusmod.client.config.NexusConfig;
import net.nexusmod.client.gui.theme.NexusDraw;
import net.nexusmod.client.gui.theme.NexusTheme;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Pill-shaped animated toggle matching the dashboard's `.toggle` component:
 * a 38x22 rounded track that goes from flat gray to a blue->cyan gradient
 * with a soft glow when on, with a knob that overshoots slightly (spring
 * ease) when it lands. This is the successor to AnimatedToggleWidget,
 * restyled to the new visual language; kept as a standalone widget (not a
 * row) so screens can lay it out freely next to a label built separately.
 */
public class NexusToggle extends ClickableWidget {

    private static final int TRACK_W = 38;
    private static final int TRACK_H = 22;

    private boolean state;
    private float anim; // 0 = off, 1 = on
    private final Consumer<Boolean> onChange;

    public NexusToggle(int x, int y, BooleanSupplier initial, Consumer<Boolean> onChange) {
        super(x, y, TRACK_W, TRACK_H, Text.empty());
        this.onChange = onChange;
        this.state = initial.getAsBoolean();
        this.anim = state ? 1f : 0f;
    }

    public boolean isOn() {
        return state;
    }

    public void setOn(boolean value) {
        this.state = value;
    }

    @Override
    protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
        NexusConfig cfg = NexusConfig.get();
        float target = state ? 1f : 0f;
        if (cfg.guiAnimations) {
            float speed = 0.22f * Math.max(0.5f, cfg.animationSpeed);
            anim = MathHelper.lerp(speed, anim, target);
            if (Math.abs(anim - target) < 0.002f) anim = target;
        } else {
            anim = target;
        }

        int radius = TRACK_H / 2;
        int trackOff = NexusTheme.withAlpha(0xFFFFFFFF, 26); // rgba(255,255,255,0.10)
        int trackColor = anim <= 0f ? trackOff : NexusTheme.lerpArgb(trackOff, NexusTheme.accentGradient(0.4f), anim);

        NexusDraw.roundedFill(ctx, getX(), getY(), TRACK_W, TRACK_H, trackColor, radius);
        if (anim > 0.01f) {
            NexusDraw.hGradient(ctx, getX(), getY(), TRACK_W, TRACK_H,
                    NexusTheme.scaleAlpha(NexusTheme.BLUE, anim), NexusTheme.scaleAlpha(NexusTheme.CYAN, anim));
            if (anim > 0.5f) {
                NexusDraw.glow(ctx, getX(), getY(), TRACK_W, TRACK_H, radius, NexusTheme.CYAN, 3);
            }
        } else {
            NexusDraw.outline1px(ctx, getX(), getY(), TRACK_W, TRACK_H, NexusTheme.GLASS_BORDER, radius);
        }

        int knobSize = TRACK_H - 6;
        int travel = TRACK_W - knobSize - 6;
        // slight spring overshoot near the end of travel for a "snappy" feel
        float eased = anim < 1f && anim > 0f ? overshoot(anim) : anim;
        int knobX = getX() + 3 + Math.round(travel * eased);
        int knobY = getY() + 3;
        int knobColor = state ? 0xFFFFFFFF : 0xFFC9CCD1;
        NexusDraw.roundedFill(ctx, knobX, knobY, knobSize, knobSize, knobColor, knobSize / 2);
    }

    private static float overshoot(float t) {
        // cubic-bezier(0.34,1.56,0.64,1) approximation
        float c = 1.4f;
        return 1 + c * (t - 1) * (t - 1) * (t - 1) + (t - 1);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        state = !state;
        onChange.accept(state);
        MinecraftClient client = NexusClient.client();
        if (client.getSoundManager() != null) {
            client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f));
        }
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        builder.put(NarrationPart.TITLE, Text.literal(state ? "On" : "Off"));
    }
}
