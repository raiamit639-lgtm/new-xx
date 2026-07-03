package net.nexusmod.client.perf;

import net.minecraft.client.MinecraftClient;
import org.lwjgl.opengl.GL11;

/**
 * Pulls real performance numbers from the running client for the stat
 * cards. Notes on accuracy, since the dashboard mockup shows GPU% and VRAM
 * which Minecraft doesn't expose cleanly:
 *
 *  - FPS: MinecraftClient's own frame counter (currentFps) - exact.
 *  - RAM: JVM heap usage via Runtime - exact, but this is heap, not the
 *    whole process's resident memory.
 *  - VRAM: there is no cross-vendor, cross-driver way to query this from
 *    LWJGL/OpenGL without vendor-specific extensions (NVX_gpu_memory_info
 *    on NVIDIA, ATI_meminfo on AMD) which aren't guaranteed present. We try
 *    NVX_gpu_memory_info opportunistically and fall back to "not
 *    available" (-1) if the extension isn't there, rather than making up a
 *    number.
 *  - GPU%: OpenGL has no standard load-percentage query at all. Rather
 *    than fabricate a plausible-looking number, we derive a rough load
 *    proxy from frame time headroom (how close frame time is to the
 *    display's refresh budget) and label it as an estimate in the UI, not
 *    a real GPU utilization reading.
 */
public final class PerfSampler {

    private static final int HISTORY_LENGTH = 64;

    public final StatHistory fps = new StatHistory(HISTORY_LENGTH);
    public final StatHistory gpuLoadEstimate = new StatHistory(HISTORY_LENGTH);
    public final StatHistory ramMb = new StatHistory(HISTORY_LENGTH);
    public final StatHistory vramMb = new StatHistory(HISTORY_LENGTH);

    private boolean vramExtensionChecked = false;
    private boolean vramExtensionAvailable = false;
    private int vramTotalKb = -1;

    private static final PerfSampler INSTANCE = new PerfSampler();

    public static PerfSampler get() {
        return INSTANCE;
    }

    private PerfSampler() {}

    /** Call once per client tick or render frame. Cheap - no allocations beyond the fixed ring buffers. */
    public void sample() {
        MinecraftClient client = MinecraftClient.getInstance();

        fps.push(client.getCurrentFps());

        Runtime rt = Runtime.getRuntime();
        long usedBytes = rt.totalMemory() - rt.freeMemory();
        ramMb.push(usedBytes / (1024f * 1024f));

        sampleVram();
        sampleGpuLoadEstimate(client);
    }

    private void sampleVram() {
        if (!vramExtensionChecked) {
            vramExtensionChecked = true;
            try {
                // NVX_gpu_memory_info: GL_GPU_MEMORY_INFO_DEDICATED_VIDMEM_NVX = 0x9047, ..._CURRENT_AVAILABLE_VIDMEM_NVX = 0x9049
                String extensions = GL11.glGetString(GL11.GL_EXTENSIONS);
                vramExtensionAvailable = extensions != null && extensions.contains("GL_NVX_gpu_memory_info");
                if (vramExtensionAvailable) {
                    vramTotalKb = GL11.glGetInteger(0x9047);
                }
            } catch (Exception e) {
                vramExtensionAvailable = false;
            }
        }
        if (vramExtensionAvailable && vramTotalKb > 0) {
            int availableKb = GL11.glGetInteger(0x9049);
            int usedKb = vramTotalKb - availableKb;
            vramMb.push(Math.max(0, usedKb) / 1024f);
        } else {
            vramMb.push(-1f); // signals "unavailable" to the UI layer
        }
    }

    private void sampleGpuLoadEstimate(MinecraftClient client) {
        // Rough proxy only: how close current FPS sits to the configured frame-rate
        // cap suggests how "busy" the render thread is relative to its budget.
        // This is explicitly NOT a hardware utilization reading.
        int cap = client.options.getMaxFps().getValue();
        float target = cap > 0 && cap < 260 ? cap : 240f;
        float ratio = target <= 0 ? 0 : Math.min(1.5f, client.getCurrentFps() / target);
        float loadEstimate = Math.max(0f, Math.min(100f, (1.2f - ratio) * 100f));
        gpuLoadEstimate.push(loadEstimate);
    }

    public boolean isVramAvailable() {
        return vramExtensionAvailable;
    }
}
