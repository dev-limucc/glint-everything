package dev.limucc.glinteverything.client.gui.widget;

import dev.limucc.glinteverything.client.compat.Gfx;

/**
 * Flat hand-drawn slider in the Animated Limucc UI style: recessed track, accent-blue fill,
 * square knob with the standard 1px top-highlight / bottom-shade depth cues.
 */
public class FlatSlider {
    public int x, y, w;
    public final int h = 12;

    public float min, max, value;
    public boolean dragging;

    private static final int TRACK    = 0xFF1C1C20;
    private static final int FILL     = 0xFF3A6EA5;   // accent blue
    private static final int FILL_DIS = 0xFF2A2A30;
    private static final int KNOB     = 0xFFF1F1F4;
    private static final int KNOB_DIS = 0xFF6A6A70;

    public FlatSlider(float min, float max, float value) {
        this.min = min; this.max = max; this.value = value;
    }

    public void setBounds(int x, int y, int w) { this.x = x; this.y = y; this.w = w; }
    public boolean contains(double mx, double my) { return mx >= x && mx < x + w && my >= y - 2 && my < y + h + 2; }

    /** Start dragging if hit; returns true when the click is consumed. */
    public boolean click(double mx, double my, boolean enabled) {
        if (!enabled || !contains(mx, my)) return false;
        dragging = true;
        drag(mx);
        return true;
    }

    public void drag(double mx) {
        float t = (float) ((mx - (x + 3)) / (double) (w - 6));
        t = t < 0 ? 0 : (t > 1 ? 1 : t);
        value = min + t * (max - min);
    }

    public void release() { dragging = false; }

    public void render(Gfx g, int mouseX, int mouseY, boolean enabled) {
        float t = (value - min) / (max - min);
        t = t < 0 ? 0 : (t > 1 ? 1 : t);

        int ty0 = y + h / 2 - 2, ty1 = y + h / 2 + 2;
        g.fill(x, ty0, x + w, ty1, TRACK);
        g.fill(x, ty0, x + w, ty0 + 1, 0x33000000);                     // recessed top
        int fx = x + Math.round(t * (w - 6)) + 3;
        g.fill(x, ty0, fx, ty1, enabled ? FILL : FILL_DIS);

        int kx = fx - 3;
        int kc = enabled ? KNOB : KNOB_DIS;
        g.fill(kx, y, kx + 6, y + h, kc);
        g.fill(kx, y, kx + 6, y + 1, 0x66FFFFFF);                       // knob top highlight
        g.fill(kx, y + h - 1, kx + 6, y + h, 0x44000000);               // knob bottom shade
    }
}
