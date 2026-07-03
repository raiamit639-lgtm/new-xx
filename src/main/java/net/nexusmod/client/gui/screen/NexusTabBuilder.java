package net.nexusmod.client.gui.screen;

import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.nexusmod.client.config.NexusConfig;
import net.nexusmod.client.gui.widget.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Constructs the widget tree for a given tab. Each tab is a 2-column grid
 * of cards (`.cards-grid`), each card a NexusCardPanel background plus a
 * stack of setting rows (toggle / slider / dropdown) laid out under it.
 *
 * This is intentionally a static builder rather than a class per tab: the
 * row-layout bookkeeping (card height growing with row count, column
 * placement, search filtering) is identical across tabs, so it lives once
 * in {@link Layout} and each tab method just declares its rows.
 */
public final class NexusTabBuilder {

    private NexusTabBuilder() {}

    public static TabContent build(NexusTab tab, int x, int y, int width, String searchQuery, Runnable onChange) {
        Layout layout = new Layout(x, y, width, searchQuery, onChange);
        switch (tab) {
            case HOME -> buildHome(layout);
            case VIDEO -> buildVideo(layout);
            case QUALITY -> buildQuality(layout);
            case PERFORMANCE -> buildPerformance(layout);
            case ADVANCED -> buildAdvanced(layout);
            case COMPATIBILITY -> buildCompatibility(layout);
            case ANIMATIONS -> buildAnimations(layout);
            case EXPERIMENTAL -> buildExperimental(layout);
            case ABOUT -> buildAbout(layout);
        }
        return layout.finish();
    }

    // ---------------------------------------------------------------
    // Tabs
    // ---------------------------------------------------------------

    private static void buildHome(Layout l) {
        l.presetRow();
        l.card(Text.translatable("card.nexus.quick_status"), card -> {
            NexusConfig cfg = NexusConfig.get();
            card.info(Text.translatable("row.nexus.preset_active"), Text.literal(presetLabel(cfg.activePreset)));
            card.info(Text.translatable("row.nexus.render_distance"), Text.literal(cfg.renderDistance + " chunks"));
            card.info(Text.translatable("row.nexus.graphics_quality"), Text.literal(cfg.graphicsQuality.name()));
        });
        l.card(Text.translatable("card.nexus.getting_started"), card -> {
            card.info(Text.translatable("row.nexus.help_keybind"), Text.literal("N"));
            card.info(Text.translatable("row.nexus.help_docs"), Text.translatable("row.nexus.help_docs_value"));
        });
    }

    private static void buildVideo(Layout l) {
        l.card(Text.translatable("card.nexus.display"), card -> {
            NexusConfig cfg = NexusConfig.get();
            card.slider(Text.translatable("row.nexus.max_fps"), 10, 260, cfg.maxFps,
                    v -> Text.literal(v >= 260 ? "\u221E" : String.valueOf(v)),
                    v -> { cfg.maxFps = v; l.changed(); });
            card.toggle(Text.translatable("row.nexus.vsync"), () -> cfg.vsync,
                    v -> { cfg.vsync = v; l.changed(); });
            card.toggle(Text.translatable("row.nexus.smooth_lighting"), () -> cfg.smoothLighting,
                    v -> { cfg.smoothLighting = v; l.changed(); });
        });
        l.card(Text.translatable("card.nexus.render_distance"), card -> {
            NexusConfig cfg = NexusConfig.get();
            card.slider(Text.translatable("row.nexus.render_distance"), 2, 32, cfg.renderDistance,
                    v -> Text.literal(v + "c"), v -> { cfg.renderDistance = v; l.changed(); });
            card.slider(Text.translatable("row.nexus.simulation_distance"), 5, 32, cfg.simulationDistance,
                    v -> Text.literal(v + "c"), v -> { cfg.simulationDistance = v; l.changed(); });
        });
    }

    private static void buildQuality(Layout l) {
        l.card(Text.translatable("card.nexus.graphics"), card -> {
            NexusConfig cfg = NexusConfig.get();
            card.dropdown(Text.translatable("row.nexus.graphics_quality"),
                    Arrays.asList(NexusConfig.GraphicsQuality.values()), cfg.graphicsQuality,
                    q -> Text.literal(capitalize(q.name())), q -> { cfg.graphicsQuality = q; l.changed(); });
            card.toggle(Text.translatable("row.nexus.animated_textures"), () -> cfg.animatedTextures,
                    v -> { cfg.animatedTextures = v; l.changed(); });
        });
        l.card(Text.translatable("card.nexus.particles"), card -> {
            NexusConfig cfg = NexusConfig.get();
            card.toggle(Text.translatable("row.nexus.particles"), () -> cfg.particles,
                    v -> { cfg.particles = v; l.changed(); });
            card.slider(Text.translatable("row.nexus.particle_density"), 0, 100, cfg.particleDensity,
                    v -> Text.literal(v + "%"), v -> { cfg.particleDensity = v; l.changed(); });
        });
    }

    private static void buildPerformance(Layout l) {
        l.presetRow();
        l.card(Text.translatable("card.nexus.distance_culling"), card -> {
            NexusConfig cfg = NexusConfig.get();
            card.slider(Text.translatable("row.nexus.render_distance"), 2, 32, cfg.renderDistance,
                    v -> Text.literal(v + "c"), v -> { cfg.renderDistance = v; l.changed(); });
            card.slider(Text.translatable("row.nexus.simulation_distance"), 5, 32, cfg.simulationDistance,
                    v -> Text.literal(v + "c"), v -> { cfg.simulationDistance = v; l.changed(); });
            card.dropdown(Text.translatable("row.nexus.chunk_updates"),
                    Arrays.asList(NexusConfig.ChunkUpdateMode.values()), cfg.chunkUpdateMode,
                    m -> Text.literal(capitalize(m.name())), m -> { cfg.chunkUpdateMode = m; l.changed(); });
            card.toggle(Text.translatable("row.nexus.entity_culling"), () -> cfg.entityCulling,
                    v -> { cfg.entityCulling = v; l.changed(); });
            card.toggle(Text.translatable("row.nexus.occlusion_culling"), () -> cfg.occlusionCulling,
                    v -> { cfg.occlusionCulling = v; l.changed(); });
            card.toggle(Text.translatable("row.nexus.frustum_culling"), () -> cfg.frustumCulling,
                    v -> { cfg.frustumCulling = v; l.changed(); });
            card.toggle(Text.translatable("row.nexus.fog_culling"), () -> cfg.fogCulling,
                    v -> { cfg.fogCulling = v; l.changed(); });
        });
        l.card(Text.translatable("card.nexus.threading"), card -> {
            NexusConfig cfg = NexusConfig.get();
            card.slider(Text.translatable("row.nexus.chunk_threads"), 0, 16, cfg.chunkBuilderThreads,
                    v -> Text.literal(v == 0 ? "Auto" : String.valueOf(v)),
                    v -> { cfg.chunkBuilderThreads = v; l.changed(); });
            card.toggle(Text.translatable("row.nexus.async_chunk_rebuild"), () -> cfg.asyncChunkRebuild,
                    v -> { cfg.asyncChunkRebuild = v; l.changed(); });
            card.toggle(Text.translatable("row.nexus.smooth_chunk_loading"), () -> cfg.smoothChunkLoading,
                    v -> { cfg.smoothChunkLoading = v; l.changed(); });
        });
    }

    private static void buildAdvanced(Layout l) {
        l.card(Text.translatable("card.nexus.advanced_rendering"), card -> {
            NexusConfig cfg = NexusConfig.get();
            card.info(Text.translatable("row.nexus.vram_note"), Text.translatable("row.nexus.vram_note_value"));
        });
    }

    private static void buildCompatibility(Layout l) {
        l.card(Text.translatable("card.nexus.compat"), card -> {
            card.info(Text.translatable("row.nexus.compat_note"), Text.translatable("row.nexus.compat_note_value"));
        });
    }

    private static void buildAnimations(Layout l) {
        l.card(Text.translatable("card.nexus.gui_animations"), card -> {
            NexusConfig cfg = NexusConfig.get();
            card.toggle(Text.translatable("row.nexus.gui_animations"), () -> cfg.guiAnimations,
                    v -> { cfg.guiAnimations = v; l.changed(); });
            card.slider(Text.translatable("row.nexus.animation_speed"), 50, 200, Math.round(cfg.animationSpeed * 100),
                    v -> Text.literal(String.format("%.1fx", v / 100f)),
                    v -> { cfg.animationSpeed = v / 100f; l.changed(); });
        });
    }

    private static void buildExperimental(Layout l) {
        l.card(Text.translatable("card.nexus.experimental"), card -> {
            card.info(Text.translatable("row.nexus.experimental_note"), Text.translatable("row.nexus.experimental_note_value"));
        });
    }

    private static void buildAbout(Layout l) {
        l.card(Text.translatable("card.nexus.about"), card -> {
            card.info(Text.literal("Nexus"), Text.literal("v1.0.0"));
            card.info(Text.translatable("row.nexus.about_keybind"), Text.literal("N"));
        });
    }

    private static String presetLabel(NexusConfig.Preset preset) {
        return capitalize(preset.name().replace('_', ' '));
    }

    private static String capitalize(String s) {
        String lower = s.toLowerCase().replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    // ---------------------------------------------------------------
    // Layout helper
    // ---------------------------------------------------------------

    /**
     * Tracks running Y position per column and accumulates widgets/home
     * positions for the resulting TabContent. Two columns, matching
     * `.cards-grid { grid-template-columns: 1fr 1fr; }`.
     */
    private static final class Layout {
        private final int x, width;
        private final String searchQuery;
        private final Runnable onChange;
        private final List<ClickableWidget> widgets = new ArrayList<>();
        private final List<Integer> homeY = new ArrayList<>();
        private final int colGap = 14;
        private final int colWidth;
        private int[] colY;
        private int topY;

        Layout(int x, int y, int width, String searchQuery, Runnable onChange) {
            this.x = x;
            this.width = width;
            this.searchQuery = searchQuery == null ? "" : searchQuery.toLowerCase();
            this.onChange = onChange;
            this.colWidth = (width - colGap) / 2;
            this.colY = new int[]{y, y};
            this.topY = y;
        }

        void changed() {
            onChange.run();
        }

        void presetRow() {
            NexusConfig cfg = NexusConfig.get();
            NexusConfig.Preset[] presets = NexusConfig.Preset.values();
            int chipW = 96;
            int gap = 6;
            List<NexusChip> chips = new ArrayList<>();
            for (int i = 0; i < presets.length; i++) {
                NexusConfig.Preset preset = presets[i];
                NexusChip chip = new NexusChip(x + i * (chipW + gap), topY, chipW, 22,
                        Text.literal(presetLabel(preset)), cfg.activePreset == preset,
                        clicked -> {
                            cfg.applyPreset(preset);
                            net.nexusmod.client.perf.NexusOptionsSync.apply();
                            for (NexusChip c : chips) c.setSelected(false);
                            clicked.setSelected(true);
                            changed();
                        });
                chips.add(chip);
                place(chip);
            }
            // preset row occupies its own strip above the two columns
            colY[0] = Math.max(colY[0], topY + 30);
            colY[1] = Math.max(colY[1], topY + 30);
        }

        void card(Text title, java.util.function.Consumer<CardBuilder> rows) {
            int col = colY[0] <= colY[1] ? 0 : 1;
            int cardX = x + col * (colWidth + colGap);
            int cardTop = colY[col];

            CardBuilder builder = new CardBuilder(cardX, cardTop);
            rows.accept(builder);

            if (builder.matchedAnyRow || searchQuery.isEmpty()) {
                int cardHeight = 34 + builder.rowCount * 24;
                NexusCardPanel panel = new NexusCardPanel(cardX, cardTop, colWidth, cardHeight, title, builder.rowCount);
                // Panel must render behind the rows CardBuilder already appended, since Screen
                // renders children in add order — insert it at the index where those rows begin.
                int insertIdx = widgets.size() - builder.addedWidgets.size();
                widgets.add(insertIdx, panel);
                homeY.add(insertIdx, cardTop);
                colY[col] = cardTop + cardHeight + 14;
            } else {
                // No row matched the search query: discard this card's widgets entirely and
                // rebuild homeY to stay in lockstep with the trimmed widget list.
                widgets.removeAll(builder.addedWidgets);
                homeY.clear();
                for (ClickableWidget w : widgets) homeY.add(w.getY());
            }
        }

        private void place(ClickableWidget w) {
            widgets.add(w);
            homeY.add(w.getY());
        }

        TabContent finish() {
            int maxY = Math.max(colY[0], colY[1]);
            return new TabContent(widgets, homeY, maxY - topY);
        }

        /** Collects rows for one card, tracking row count and search-match state. */
        final class CardBuilder {
            private final int cardX;
            private int rowY;
            private int rowCount = 0;
            private boolean matchedAnyRow = false;
            private final List<ClickableWidget> addedWidgets = new ArrayList<>();

            CardBuilder(int cardX, int cardTop) {
                this.cardX = cardX;
                this.rowY = cardTop + 30;
            }

            private boolean matches(Text label) {
                if (searchQuery.isEmpty()) return true;
                boolean m = label.getString().toLowerCase().contains(searchQuery);
                if (m) matchedAnyRow = true;
                return m;
            }

            void toggle(Text label, java.util.function.BooleanSupplier initial, java.util.function.Consumer<Boolean> onChange) {
                if (!matches(label)) return;
                NexusLabel lbl = new NexusLabel(cardX + 12, rowY, colWidth - 60, 18, label);
                NexusToggle tgl = new NexusToggle(cardX + colWidth - 12 - 38, rowY - 2, initial, onChange);
                addRow(lbl, tgl);
            }

            void slider(Text label, int min, int max, int initial,
                        java.util.function.Function<Integer, Text> valueFormatter,
                        java.util.function.IntConsumer onChange) {
                if (!matches(label)) return;
                NexusLabel lbl = new NexusLabel(cardX + 12, rowY, colWidth - 138, 18, label);
                NexusSlider sld = new NexusSlider(cardX + 126, rowY - 2, colWidth - 138, 18, min, max, initial, valueFormatter, onChange);
                addRow(lbl, sld);
            }

            <T> void dropdown(Text label, List<T> options, T initial, java.util.function.Function<T, Text> fmt,
                               java.util.function.Consumer<T> onChange) {
                if (!matches(label)) return;
                NexusLabel lbl = new NexusLabel(cardX + 12, rowY, 110, 18, label);
                NexusDropdown<T> dd = new NexusDropdown<>(cardX + 126, rowY - 2, colWidth - 138, 18, options, initial, fmt, onChange);
                addRow(lbl, dd);
            }

            void info(Text label, Text value) {
                if (!matches(label)) return;
                NexusLabel lbl = new NexusLabel(cardX + 12, rowY, 140, 18, label);
                NexusLabel val = new NexusLabel(cardX + colWidth - 140 - 12, rowY, 140, 18, value);
                addRow(lbl, val);
            }

            private void addRow(ClickableWidget a, ClickableWidget b) {
                widgets.add(a); homeY.add(a.getY()); addedWidgets.add(a);
                widgets.add(b); homeY.add(b.getY()); addedWidgets.add(b);
                rowY += 24;
                rowCount++;
                matchedAnyRow = true;
            }
        }
    }
}
