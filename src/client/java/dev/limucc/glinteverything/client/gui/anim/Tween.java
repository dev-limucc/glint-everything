package dev.limucc.glinteverything.client.gui.anim;

/**
 * One retargetable time-based interpolation — Animated Limucc UI style library.
 * When the target changes mid-flight, {@link #retarget} re-anchors the curve at the current
 * value, so there is never a jump.
 */
public final class Tween {
    private float start, end, current;
    private long startMs;
    private int durationMs = 1;
    private Easing easing = Easing.LINEAR;
    private boolean active;

    public Tween() {}
    public Tween(float initial) { snap(initial); }

    /** Jump straight to value with no animation (adopt an external change). */
    public void snap(float value) { this.start = this.end = this.current = value; this.active = false; }

    public float current() { return current; }
    public float start()   { return start; }
    public boolean isActive() { return active; }

    /** Aim at target; if it differs from where we're heading, start a fresh eased segment. */
    public void retarget(float target, long now, int durationMs, Easing easing) {
        if (active && Math.abs(target - end) < 1.0e-4f) return;
        if (!active && Math.abs(target - current) < 1.0e-4f) { this.end = target; return; }
        this.start = current; this.end = target; this.startMs = now;
        this.durationMs = Math.max(1, durationMs); this.easing = easing; this.active = true;
    }

    /** Advance to now and return the current value. */
    public float update(long now) {
        if (!active) return current;
        float t = (now - startMs) / (float) durationMs;
        if (t >= 1.0f)      { current = end; active = false; }
        else if (t > 0.0f)  { current = start + (end - start) * easing.apply(t); }
        return current;
    }
}
