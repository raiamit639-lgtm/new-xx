package net.nexusmod.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Central configuration object for Nexus. Holds every value the dashboard
 * GUI reads from and writes to, and handles loading/saving a JSON config
 * file in the Minecraft config directory.
 *
 * Robustness notes (vs. the original version):
 *  - `configVersion` lets future releases migrate old configs instead of
 *    silently misreading them or forcing a reset.
 *  - `sanitize()` clamps every numeric field and null-checks every enum
 *    after load, so a hand-edited or corrupted JSON file can't hand the
 *    game an out-of-range render distance or a null enum that NPEs deep
 *    in a mixin.
 *  - `save()` writes to a temp file and atomically moves it into place,
 *    so a crash mid-write can't leave a truncated, unreadable config
 *    (previously: writing directly to nexus.json could corrupt it if the
 *    game crashed mid-save).
 */
public class NexusConfig {

    public static final int CURRENT_VERSION = 2;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static NexusConfig instance;

    public int configVersion = CURRENT_VERSION;

    // --- Rendering distance & culling ---
    public int renderDistance = 12;            // chunks, 2-32
    public int simulationDistance = 10;         // chunks, 5-32
    public boolean frustumCulling = true;
    public boolean occlusionCulling = true;
    public boolean entityCulling = true;
    public boolean fogCulling = true;

    // --- Chunk build / threading ---
    public ChunkUpdateMode chunkUpdateMode = ChunkUpdateMode.AGGRESSIVE;
    public int chunkBuilderThreads = 0;         // 0 = auto-detect from core count
    public boolean asyncChunkRebuild = true;
    public boolean smoothChunkLoading = true;

    // --- Visual quality ---
    public GraphicsQuality graphicsQuality = GraphicsQuality.FANCY;
    public boolean smoothLighting = true;
    public boolean animatedTextures = true;
    public boolean particles = true;
    public int particleDensity = 100;           // percent, 0-100

    // --- Frame pacing ---
    public int maxFps = 240;                     // 10-260 (260+ treated as unlimited by vanilla options)
    public boolean vsync = false;

    // --- GUI behavior ---
    public boolean guiAnimations = true;
    public float animationSpeed = 1.0f;          // 0.5-2.0

    // --- Presets ---
    public Preset activePreset = Preset.BALANCED;

    public enum GraphicsQuality {
        FAST, FANCY, FABULOUS
    }

    public enum ChunkUpdateMode {
        CONSERVATIVE, BALANCED, AGGRESSIVE
    }

    public enum Preset {
        ULTRA_PERFORMANCE, BALANCED, QUALITY, ULTRA_QUALITY, CUSTOM
    }

    public static NexusConfig get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    private static Path configPath() {
        return Path.of("config", "nexus.json");
    }

    private static NexusConfig load() {
        Path path = configPath();
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                NexusConfig loaded = GSON.fromJson(reader, NexusConfig.class);
                if (loaded != null) {
                    loaded.migrateIfNeeded();
                    loaded.sanitize();
                    return loaded;
                }
            } catch (IOException | JsonSyntaxException e) {
                System.err.println("[Nexus] Failed to read config, using defaults: " + e.getMessage());
            }
        }
        NexusConfig fresh = new NexusConfig();
        fresh.sanitize();
        return fresh;
    }

    /** Placeholder for future version bumps; each case falls through to the next so a v1 config
     *  picks up every migration step on the way to CURRENT_VERSION. */
    private void migrateIfNeeded() {
        if (configVersion < 1) {
            configVersion = 1;
        }
        if (configVersion < 2) {
            // v1 -> v2: introduced simulationDistance, chunkUpdateMode, maxFps, vsync, activePreset.
            // Fields already carry their class-default values from Gson's no-such-key handling,
            // so nothing to backfill beyond bumping the version marker.
            configVersion = 2;
        }
    }

    /** Clamps every numeric field and replaces null enums with safe defaults. Called after every load. */
    private void sanitize() {
        renderDistance = clamp(renderDistance, 2, 32);
        simulationDistance = clamp(simulationDistance, 5, 32);
        chunkBuilderThreads = clamp(chunkBuilderThreads, 0, 32);
        particleDensity = clamp(particleDensity, 0, 100);
        maxFps = clamp(maxFps, 10, 260);
        animationSpeed = clampf(animationSpeed, 0.5f, 2.0f);

        if (chunkUpdateMode == null) chunkUpdateMode = ChunkUpdateMode.BALANCED;
        if (graphicsQuality == null) graphicsQuality = GraphicsQuality.FANCY;
        if (activePreset == null) activePreset = Preset.CUSTOM;
        if (configVersion <= 0) configVersion = CURRENT_VERSION;
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private static float clampf(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    /**
     * Applies one of the built-in presets by overwriting the relevant fields,
     * then marks activePreset. Any manual tweak afterward should set
     * activePreset back to CUSTOM from the calling screen code.
     */
    public void applyPreset(Preset preset) {
        switch (preset) {
            case ULTRA_PERFORMANCE -> {
                renderDistance = 6; simulationDistance = 5;
                graphicsQuality = GraphicsQuality.FAST;
                smoothLighting = false; animatedTextures = false;
                particles = true; particleDensity = 20;
                entityCulling = true; occlusionCulling = true; fogCulling = true; frustumCulling = true;
                chunkUpdateMode = ChunkUpdateMode.AGGRESSIVE;
            }
            case BALANCED -> {
                renderDistance = 12; simulationDistance = 10;
                graphicsQuality = GraphicsQuality.FANCY;
                smoothLighting = true; animatedTextures = true;
                particles = true; particleDensity = 100;
                entityCulling = true; occlusionCulling = true; fogCulling = true; frustumCulling = true;
                chunkUpdateMode = ChunkUpdateMode.AGGRESSIVE;
            }
            case QUALITY -> {
                renderDistance = 20; simulationDistance = 12;
                graphicsQuality = GraphicsQuality.FANCY;
                smoothLighting = true; animatedTextures = true;
                particles = true; particleDensity = 100;
                entityCulling = false; occlusionCulling = true; fogCulling = false; frustumCulling = true;
                chunkUpdateMode = ChunkUpdateMode.BALANCED;
            }
            case ULTRA_QUALITY -> {
                renderDistance = 32; simulationDistance = 16;
                graphicsQuality = GraphicsQuality.FABULOUS;
                smoothLighting = true; animatedTextures = true;
                particles = true; particleDensity = 100;
                entityCulling = false; occlusionCulling = false; fogCulling = false; frustumCulling = true;
                chunkUpdateMode = ChunkUpdateMode.CONSERVATIVE;
            }
            case CUSTOM -> { /* no-op: user is hand-tuning, leave fields as-is */ }
        }
        activePreset = preset;
        sanitize();
        save();
    }

    public void save() {
        Path path = configPath();
        try {
            Files.createDirectories(path.getParent());
            Path tmp = path.resolveSibling(path.getFileName().toString() + ".tmp");
            try (Writer writer = Files.newBufferedWriter(tmp)) {
                GSON.toJson(this, writer);
            }
            Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            System.err.println("[Nexus] Failed to save config: " + e.getMessage());
        }
    }
}
