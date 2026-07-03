package net.nexusmod.mixin;

import net.minecraft.client.particle.ParticleManager;
import net.minecraft.particle.ParticleEffect;
import net.nexusmod.client.config.NexusConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Vanilla's particle option is a coarse ALL/DECREASED/MINIMAL toggle.
 * NexusConfig.particleDensity is a 0-100 slider for finer control than
 * that; this mixin probabilistically drops a fraction of particle spawn
 * requests to approximate the slider value, on top of whatever vanilla's
 * own ParticlesMode already filtered out.
 *
 * At density=100 nothing is dropped (fast-path, no RNG call). Below 100,
 * roughly (100 - density)% of spawns are skipped.
 *
 * Targeting by bare method name (no hand-written bytecode descriptor):
 * Mixin resolves a bare name against the @Inject handler's own parameter
 * types below, which survives Yarn intermediary renames across mapping
 * builds that a hand-written descriptor string does not.
 *
 * Return type: this build's mappings resolve addParticle(ParticleEffect,
 * double x3, double velocity x3) to a `boolean` return (whether the
 * particle was actually spawned), confirmed by Mixin's own
 * "CallbackInfoReturnable is required" error against a plain
 * CallbackInfo handler — so the injected method must use
 * CallbackInfoReturnable<Boolean> and cir.setReturnValue(false) to
 * cancel a spawn, not CallbackInfo/ci.cancel() (which only applies to
 * void-returning targets). Returning false here mirrors what vanilla
 * itself returns when a spawn is skipped for its own reasons (e.g.
 * particle-visibility settings), so downstream callers that check the
 * return value see a normal "not spawned" result rather than an
 * exceptional one.
 */
@Mixin(ParticleManager.class)
public class ParticleManagerMixin {

    @Inject(method = "addParticle", at = @At("HEAD"), cancellable = true)
    private void nexus$densityLimit(ParticleEffect parameters, double x, double y, double z,
                                     double velocityX, double velocityY, double velocityZ,
                                     CallbackInfoReturnable<Boolean> cir) {
        NexusConfig cfg = NexusConfig.get();
        if (cfg.particleDensity >= 100) return;
        if (cfg.particleDensity <= 0) {
            cir.setReturnValue(false);
            return;
        }
        if (ThreadLocalRandom.current().nextInt(100) >= cfg.particleDensity) {
            cir.setReturnValue(false);
        }
    }
}
