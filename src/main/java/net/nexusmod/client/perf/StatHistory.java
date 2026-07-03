package net.nexusmod.client.perf;

/**
 * Fixed-size rolling buffer of recent samples for one stat (FPS, GPU load
 * estimate, RAM, VRAM). Backs the sparkline graphs on the stat cards.
 * Not thread-safe; only ever touched from the render thread.
 */
public class StatHistory {

    private final float[] samples;
    private int head = 0;
    private int count = 0;
    private float min = Float.MAX_VALUE;
    private float max = -Float.MAX_VALUE;

    public StatHistory(int capacity) {
        this.samples = new float[capacity];
    }

    public void push(float value) {
        samples[head] = value;
        head = (head + 1) % samples.length;
        if (count < samples.length) count++;
        recomputeBounds();
    }

    private void recomputeBounds() {
        min = Float.MAX_VALUE;
        max = -Float.MAX_VALUE;
        for (int i = 0; i < count; i++) {
            float v = samples[(head - 1 - i + samples.length) % samples.length];
            if (v < min) min = v;
            if (v > max) max = v;
        }
        if (min == max) {
            // avoid a divide-by-zero flatline when every sample is identical
            min -= 1f;
            max += 1f;
        }
    }

    public int size() {
        return count;
    }

    public float latest() {
        if (count == 0) return 0f;
        return samples[(head - 1 + samples.length) % samples.length];
    }

    /** Sample at logical index 0 = oldest visible, size()-1 = newest. */
    public float at(int index) {
        int offset = count - 1 - index;
        return samples[(head - 1 - offset + samples.length) % samples.length];
    }

    /** Normalized 0..1 position for the sample at the given logical index, based on the current min/max window. */
    public float normalizedAt(int index) {
        return (at(index) - min) / (max - min);
    }
}
