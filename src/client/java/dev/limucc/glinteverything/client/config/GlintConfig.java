package dev.limucc.glinteverything.client.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Plain serializable settings. Runtime lookups are compiled from this in {@code GlintRuntime}. */
public class GlintConfig {

    /** How items that already glint (vanilla enchanted + other mods' foil items) are treated. */
    public enum NaturalMode {
        DEFAULT("Vanilla"), TINTED("Tinted"), HIDDEN("Hidden");

        private final String label;
        NaturalMode(String label) { this.label = label; }
        public String label() { return label; }
        public NaturalMode next() { NaturalMode[] v = values(); return v[(ordinal() + 1) % v.length]; }
    }

    /** Direction/motion style of the glint scroll animation. */
    public enum AnimStyle {
        CLASSIC("Classic"), REVERSE("Reverse"), VERTICAL("Vertical"), HORIZONTAL("Horizontal"), STATIC("Frozen");

        private final String label;
        AnimStyle(String label) { this.label = label; }
        public String label() { return label; }
        public AnimStyle next() { AnimStyle[] v = values(); return v[(ordinal() + 1) % v.length]; }
    }

    public boolean enabled = true;

    // ── whitelist ───────────────────────────────────────────────
    /** Items that get the glint even though they normally don't. */
    public List<String> whitelist = new ArrayList<>();
    /** Master toggle for the whitelist glow. */
    public boolean whitelistGlint = true;

    // ── colors ──────────────────────────────────────────────────
    /** Tint the glint of whitelisted "extra" items. */
    public boolean tintExtra = false;
    /** ARGB tint for whitelisted items (used when {@link #tintExtra}). */
    public int extraColor = 0xFF57C7FF;
    /** What happens to items that glint on their own. */
    public NaturalMode naturalGlint = NaturalMode.DEFAULT;
    /** ARGB tint for natural glint (used when {@link #naturalGlint} == TINTED). */
    public int naturalColor = 0xFFB24BF3;
    /** Per-item ARGB overrides (item id -> color); beats every other color rule. */
    public Map<String, Integer> itemColors = new LinkedHashMap<>();

    // ── animation ───────────────────────────────────────────────
    /** Speed multiplier on top of vanilla's glint speed (1 = vanilla). */
    public float speed = 1.0f;
    /** Glint texture scale multiplier (1 = vanilla). */
    public float scale = 1.0f;
    /** Scroll rotation in degrees (vanilla = 10). */
    public float angle = 10.0f;
    public AnimStyle style = AnimStyle.CLASSIC;

    /** True when the animation section is untouched, so vanilla code runs unmodified. */
    public boolean isDefaultAnim() {
        return Math.abs(speed - 1.0f) < 1.0e-3f
            && Math.abs(scale - 1.0f) < 1.0e-3f
            && Math.abs(angle - 10.0f) < 1.0e-3f
            && style == AnimStyle.CLASSIC;
    }
}
