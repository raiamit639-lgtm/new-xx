package net.nexusmod.client.perf;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.option.ParticlesMode;
import net.nexusmod.client.config.NexusConfig;

/**
 * Pushes NexusConfig values that already have a vanilla equivalent
 * (render distance, simulation distance, max FPS, vsync, particle
 * on/off) directly into the client's real GameOptions, instead of
 * reimplementing what vanilla's own renderer and chunk manager already
 * do correctly. This is deliberately the *first* line of "FPS boosting"
 * for this mod: ride on Minecraft's tested settings pipeline rather than
 * duplicating it with custom mixins, which is both safer and more
 * correct than hand-rolling equivalents.
 *
 * Settings with no vanilla equivalent (entity/occlusion/fog culling,
 * chunk update aggressiveness, chunk builder thread count, particle
 * density scaling below vanilla's on/off toggle) are handled by the
 * mixins in net.nexusmod.mixin instead.
 *
 * Call {@link #apply()} once after config load and again any time a
 * setting changes from the dashboard, so the two stay in lockstep.
 */
public final class NexusOptionsSync {

    private NexusOptionsSync() {}

    public static void apply() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        GameOptions options = client.options;
        // On some launch environments (notably PojavLauncher/Android), GameOptions
        // isn't constructed yet at the point client-mod entrypoints run, even
        // though MinecraftClient itself already exists — null-check separately
        // rather than assuming client != null implies options != null.
        if (options == null) return;
        NexusConfig cfg = NexusConfig.get();

        options.getViewDistance().setValue(cfg.renderDistance);
        options.getSimulationDistance().setValue(cfg.simulationDistance);
        options.getMaxFps().setValue(cfg.maxFps);
        options.getEnableVsync().setValue(cfg.vsync);
        options.getParticles().setValue(cfg.particles ? ParticlesMode.ALL : ParticlesMode.MINIMAL);
        // GraphicsQuality and GraphicsMode share the same FAST/FANCY/FABULOUS ordering,
        // so ordinal mapping holds — verify against the exact Yarn mapping for this MC
        // version if GraphicsMode's enum order ever changes upstream.
        options.getGraphicsMode().setValue(GraphicsMode.byId(cfg.graphicsQuality.ordinal()));

        // Persist through vanilla's own options.txt writer too, so external
        // tools/other mods reading GameOptions directly see a consistent value
        // rather than only-in-memory changes that vanish on next launch.
        options.write();
    }
}
