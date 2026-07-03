package net.nexusmod.mixin;

import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.Entity;
import net.nexusmod.client.config.NexusConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adds an extra distance-based cutoff on top of vanilla's own
 * shouldRender() frustum/visibility check, gated by the entityCulling
 * config toggle. Vanilla already culls entities outside the camera
 * frustum; this mixin additionally skips very distant entities that are
 * technically in-frustum but small enough on screen to be an FPS cost
 * without a visible payoff (matching the intent of the dashboard's
 * "Entity culling" toggle as a *more aggressive* culling option, not a
 * replacement for vanilla's check).
 *
 * Target method name/signature should be verified against the exact
 * Yarn mappings for the Minecraft version this mod is built against —
 * this mixin was written against the commonly-documented 1.21.1
 * `shouldRender(Entity, Frustum, double, double, double)` signature,
 * but Loom will fail fast at build time with a clear "target method not
 * found" error if that's not an exact match, rather than silently
 * miscompiling.
 */
@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {

    private static final double EXTRA_CULL_DISTANCE_SQ = 96.0 * 96.0;

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void nexus$extraDistanceCull(Entity entity, net.minecraft.client.render.Frustum frustum,
                                          double cameraX, double cameraY, double cameraZ,
                                          CallbackInfoReturnable<Boolean> cir) {
        NexusConfig cfg = NexusConfig.get();
        if (!cfg.entityCulling) return;

        double dx = entity.getX() - cameraX;
        double dy = entity.getY() - cameraY;
        double dz = entity.getZ() - cameraZ;
        double distSq = dx * dx + dy * dy + dz * dz;

        // Never cull the entity the camera is riding/spectating, or players,
        // regardless of distance — only cull incidental distant mobs/items.
        if (distSq > EXTRA_CULL_DISTANCE_SQ && !entity.hasVehicle() && !isImportant(entity)) {
            cir.setReturnValue(false);
        }
    }

    private static boolean isImportant(Entity entity) {
        return entity instanceof net.minecraft.entity.player.PlayerEntity;
    }
}
