package dev.limucc.glinteverything.client.gui.widget;

import dev.limucc.glinteverything.client.compat.Gfx;
import net.minecraft.client.gui.Font;
import net.minecraft.util.Util;

/**
 * Minimal hand-drawn text box in the Animated Limucc UI style (the one place vanilla's EditBox
 * would be acceptable, but drawing it ourselves keeps the flat look identical on every target).
 */
public class FlatTextField {
    public int x, y, w;
    public final int h = 16;

    public String text = "";
    public String hint = "";
    public boolean focused;
    public int maxLength = 64;
    /** Characters this field accepts. */
    public String allowed = "abcdefghijklmnopqrstuvwxyz0123456789_:./-";

    private int caret;

    private static final int BG          = 0xFF1C1C20;
    private static final int BORDER_FOCUS = 0xFF3A6EA5;  // accent blue
    private static final int TEXT_COLOR  = 0xFFE6E6EA;
    private static final int HINT_COLOR  = 0xFF606068;

    public void setBounds(int x, int y, int w) { this.x = x; this.y = y; this.w = w; }
    public boolean contains(double mx, double my) { return mx >= x && mx < x + w && my >= y && my < y + h; }

    public void setText(String s) {
        text = s == null ? "" : s;
        caret = text.length();
    }

    public void render(Gfx g, Font font, int mouseX, int mouseY) {
        g.fill(x, y, x + w, y + h, BG);
        g.fill(x, y, x + w, y + 1, 0x33000000);                     // recessed top
        g.fill(x, y + h - 1, x + w, y + h, 0x22FFFFFF);             // bottom edge
        if (focused) g.fill(x, y + h - 1, x + w, y + h, BORDER_FOCUS);

        if (text.isEmpty() && !focused) {
            g.text(font, hint, x + 4, y + (h - 8) / 2, HINT_COLOR);
        } else {
            String shown = text;
            while (font.width(shown) > w - 10 && !shown.isEmpty()) shown = shown.substring(1);
            g.text(font, shown, x + 4, y + (h - 8) / 2, TEXT_COLOR);
            if (focused && (Util.getMillis() / 500) % 2 == 0) {
                int caretInShown = Math.max(0, Math.min(shown.length(), caret - (text.length() - shown.length())));
                int cx = x + 4 + font.width(shown.substring(0, caretInShown));
                g.fill(cx, y + 3, cx + 1, y + h - 3, 0xFFE6E6EA);
            }
        }
    }

    /** Handle a typed codepoint; true when consumed. */
    public boolean charTyped(char c) {
        if (!focused) return false;
        char lc = Character.toLowerCase(c);
        if (allowed.indexOf(lc) < 0) return true;   // focused field eats the keystroke either way
        if (text.length() >= maxLength) return true;
        text = text.substring(0, caret) + lc + text.substring(caret);
        caret++;
        return true;
    }

    /** Handle a key press (GLFW keycode); true when consumed. */
    public boolean keyPressed(int key) {
        if (!focused) return false;
        switch (key) {
            case 259 -> { if (caret > 0) { text = text.substring(0, caret - 1) + text.substring(caret); caret--; } }   // backspace
            case 261 -> { if (caret < text.length()) text = text.substring(0, caret) + text.substring(caret + 1); }    // delete
            case 263 -> caret = Math.max(0, caret - 1);                                                                // left
            case 262 -> caret = Math.min(text.length(), caret + 1);                                                    // right
            case 268 -> caret = 0;                                                                                     // home
            case 269 -> caret = text.length();                                                                         // end
            case 256 -> focused = false;                                                                               // esc
            default -> { return key != 257; }   // let enter fall through to the screen
        }
        return true;
    }
}
