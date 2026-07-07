package dev.limucc.glinteverything.client.gui;

import dev.limucc.glinteverything.client.compat.Gfx;
import dev.limucc.glinteverything.client.gui.widget.FlatSlider;
import dev.limucc.glinteverything.client.gui.widget.FlatTextField;
import net.minecraft.client.gui.Font;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * Compact ARGB color editor: R/G/B sliders side by side + live swatch on one row, a hex box and
 * preset swatches on the next. Reads/writes through the supplied getter/setter so several
 * editors can drive different config colors.
 */
public class ColorEditor {

    public static final int[] PRESETS = {
            0xFFFFFFFF, 0xFF57C7FF, 0xFFB24BF3, 0xFFFF4B57,
            0xFFFFA53A, 0xFFFFE14B, 0xFF4BE38A, 0xFFFF6BD6,
    };

    public static final int HEIGHT = 36;

    private final IntSupplier getter;
    private final IntConsumer setter;

    private final FlatSlider r = new FlatSlider(0, 255, 255);
    private final FlatSlider g = new FlatSlider(0, 255, 255);
    private final FlatSlider b = new FlatSlider(0, 255, 255);
    private final FlatTextField hex = new FlatTextField();

    private int x, y, w;

    public ColorEditor(IntSupplier getter, IntConsumer setter) {
        this.getter = getter;
        this.setter = setter;
        hex.allowed = "0123456789abcdef";
        hex.maxLength = 6;
    }

    public void setBounds(int x, int y, int w) {
        this.x = x; this.y = y; this.w = w;
        int sw = (w - 24 - 3 * 6) / 3;
        r.setBounds(x, y, sw);
        g.setBounds(x + sw + 6, y, sw);
        b.setBounds(x + 2 * (sw + 6), y, sw);
        hex.setBounds(x, y + 18, 56);
    }

    public boolean hexFocused() { return hex.focused; }

    public void render(Gfx gfx, Font font, int mouseX, int mouseY, boolean enabled) {
        int color = getter.getAsInt();
        if (!r.dragging) r.value = (color >> 16) & 0xFF;
        if (!g.dragging) g.value = (color >> 8) & 0xFF;
        if (!b.dragging) b.value = color & 0xFF;
        if (!hex.focused) hex.setText(String.format("%06x", color & 0xFFFFFF));

        r.render(gfx, mouseX, mouseY, enabled);
        g.render(gfx, mouseX, mouseY, enabled);
        b.render(gfx, mouseX, mouseY, enabled);

        // live swatch
        int sx = x + w - 18;
        gfx.fill(sx, y, sx + 18, y + 12, enabled ? color : 0xFF28282E);
        gfx.fill(sx, y, sx + 18, y + 1, 0x22FFFFFF);
        gfx.fill(sx, y + 11, sx + 18, y + 12, 0x44000000);

        hex.render(gfx, font, mouseX, mouseY);
        gfx.text(font, "#", x - 7, y + 22, 0xFF74747C);

        // presets
        int px = x + 64;
        for (int c : PRESETS) {
            boolean hov = enabled && mouseX >= px && mouseX < px + 12 && mouseY >= y + 20 && mouseY < y + 32;
            gfx.fill(px, y + 20, px + 12, y + 32, enabled ? c : 0xFF28282E);
            if (hov) gfx.fill(px, y + 20, px + 12, y + 32, 0x40FFFFFF);
            px += 16;
        }
    }

    /** True when the click landed on any part of the editor. */
    public boolean click(double mx, double my, boolean enabled) {
        hex.focused = enabled && hex.contains(mx, my);
        if (!enabled) return false;
        if (hex.focused) return true;
        if (r.click(mx, my, true) || g.click(mx, my, true) || b.click(mx, my, true)) {
            push();
            return true;
        }
        int px = x + 64;
        for (int c : PRESETS) {
            if (mx >= px && mx < px + 12 && my >= y + 20 && my < y + 32) {
                setter.accept(c);
                return true;
            }
            px += 16;
        }
        return false;
    }

    public void drag(double mx) {
        if (r.dragging) { r.drag(mx); push(); }
        if (g.dragging) { g.drag(mx); push(); }
        if (b.dragging) { b.drag(mx); push(); }
    }

    public boolean dragging() { return r.dragging || g.dragging || b.dragging; }

    public void release() { r.release(); g.release(); b.release(); }

    public boolean charTyped(char c) {
        if (!hex.charTyped(c)) return false;
        applyHex();
        return true;
    }

    public boolean keyPressed(int key) {
        if (!hex.keyPressed(key)) return false;
        applyHex();
        return true;
    }

    private void applyHex() {
        if (hex.text.length() == 6) {
            try {
                setter.accept(0xFF000000 | Integer.parseInt(hex.text, 16));
            } catch (NumberFormatException ignored) {}
        }
    }

    private void push() {
        setter.accept(0xFF000000 | (Math.round(r.value) << 16) | (Math.round(g.value) << 8) | Math.round(b.value));
    }
}
