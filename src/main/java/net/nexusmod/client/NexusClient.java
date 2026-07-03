package net.nexusmod.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.nexusmod.client.config.NexusConfig;
import net.nexusmod.client.gui.loader.NexusLoaderScreen;
import net.nexusmod.client.perf.NexusOptionsSync;
import org.lwjgl.glfw.GLFW;

/**
 * Entrypoint for the Nexus client mod. Registers the config, the
 * keybinding used to open the settings dashboard, and hooks into the
 * client tick loop for anything that needs polling (e.g. GUI animation
 * state, live perf sampling).
 */
public class NexusClient implements ClientModInitializer {

    public static final String MOD_ID = "nexus";

    private static KeyBinding openSettingsKey;

    private static boolean initialSyncDone = false;

    @Override
    public void onInitializeClient() {
        // Load config eagerly so defaults are written to disk on first run.
        // NOTE: the initial NexusOptionsSync.apply() call is deferred to the
        // first client tick below (not called here) — on some launch
        // environments (e.g. PojavLauncher/Android) GameOptions isn't
        // constructed yet when client-mod entrypoints run, even though
        // MinecraftClient itself already exists. By the first END_CLIENT_TICK,
        // MinecraftClient has fully finished constructing, so options is safe
        // to touch, while still running before the player can interact with
        // anything (so settings still take effect from the first real frame).
        NexusConfig.get();

        openSettingsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.nexus.open_settings",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_N,
                "category.nexus.general"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!initialSyncDone) {
                initialSyncDone = true;
                NexusOptionsSync.apply();
            }
            if (openSettingsKey.wasPressed() && client.currentScreen == null) {
                client.setScreen(new NexusLoaderScreen(null));
            }
        });

        System.out.println("[Nexus] Initialized. Press N in-game to open the settings dashboard.");
    }

    public static MinecraftClient client() {
        return MinecraftClient.getInstance();
    }
}
