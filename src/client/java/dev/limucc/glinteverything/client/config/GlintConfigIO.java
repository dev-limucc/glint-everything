package dev.limucc.glinteverything.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.limucc.glinteverything.GlintEverything;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Loads/saves {@link GlintConfig} as JSON in the standard config folder. */
public final class GlintConfigIO {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private GlintConfigIO() {}

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve(GlintEverything.MOD_ID + ".json");
    }

    public static GlintConfig load() {
        Path p = path();
        if (Files.exists(p)) {
            try {
                GlintConfig cfg = GSON.fromJson(Files.readString(p), GlintConfig.class);
                if (cfg != null) return sanitize(cfg);
            } catch (Exception e) {
                GlintEverything.LOGGER.warn("Could not read {}, using defaults", p, e);
            }
        }
        return new GlintConfig();
    }

    public static void save(GlintConfig cfg) {
        try {
            Files.createDirectories(path().getParent());
            Files.writeString(path(), GSON.toJson(sanitize(cfg)));
        } catch (IOException e) {
            GlintEverything.LOGGER.warn("Could not save config", e);
        }
    }

    /** Clamp values a hand-edited file might have broken. */
    private static GlintConfig sanitize(GlintConfig c) {
        if (c.whitelist == null) c.whitelist = new java.util.ArrayList<>();
        if (c.itemColors == null) c.itemColors = new java.util.LinkedHashMap<>();
        if (c.naturalGlint == null) c.naturalGlint = GlintConfig.NaturalMode.DEFAULT;
        if (c.style == null) c.style = GlintConfig.AnimStyle.CLASSIC;
        c.speed = clamp(c.speed, 0.0f, 10.0f);
        c.scale = clamp(c.scale, 0.1f, 8.0f);
        c.angle = clamp(c.angle, 0.0f, 90.0f);
        return c;
    }

    private static float clamp(float v, float lo, float hi) {
        return Float.isFinite(v) ? Math.max(lo, Math.min(hi, v)) : 1.0f;
    }
}
