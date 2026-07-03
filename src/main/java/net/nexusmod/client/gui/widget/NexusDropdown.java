package net.nexusmod.client.gui.widget;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.nexusmod.client.gui.theme.NexusDraw;
import net.nexusmod.client.gui.theme.NexusTheme;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Click-to-cycle dropdown matching `.dropdown`. A full popup-list dropdown
 * isn't worth the complexity here since every use case in this mod is a
 * small fixed enum (3-4 options) — cycling on click is one interaction
 * instead of open/select/close, and still reads as a dropdown visually
 * with its chevron icon.
 */
public class NexusDropdown<T> extends ClickableWidget {

    private final List<T> options;
    private final Function<T, Text> labelFor;
    private final Consumer<T> onChange;
    private int index;

    public NexusDropdown(int x, int y, int width, int height, List<T> options, T initial,
                          Function<T, Text> labelFor, Consumer<T> onChange) {
        super(x, y, width, height, Text.empty());
        this.options = options;
        this.labelFor = labelFor;
        this.onChange = onChange;
        this.index = Math.max(0, options.indexOf(initial));
        setMessage(labelFor.apply(options.get(index)));
    }

    @Override
    protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
        boolean hovered = isHovered();
        NexusDraw.roundedFill(ctx, getX(), getY(), width, height, NexusTheme.withAlpha(0xFFFFFFFF, 13), 3);
        NexusDraw.outline1px(ctx, getX(), getY(), width, height,
                hovered ? NexusTheme.GLASS_BORDER_HOVER : NexusTheme.GLASS_BORDER, 3);

        var tr = MinecraftClient.getInstance().textRenderer;
        ctx.drawText(tr, getMessage(), getX() + 10, getY() + (height - 8) / 2, NexusTheme.TEXT_PRIMARY, false);

        // chevron
        int cx = getX() + width - 14;
        int cy = getY() + height / 2;
        ctx.fill(cx - 3, cy - 1, cx + 4, cy, NexusTheme.TEXT_MUTED);
        ctx.fill(cx - 1, cy, cx + 2, cy + 2, NexusTheme.TEXT_MUTED);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        index = (index + 1) % options.size();
        T selected = options.get(index);
        setMessage(labelFor.apply(selected));
        onChange.accept(selected);
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        builder.put(NarrationPart.TITLE, getMessage());
    }
}
