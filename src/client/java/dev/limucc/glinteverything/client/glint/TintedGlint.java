package dev.limucc.glinteverything.client.glint;

import com.mojang.blaze3d.platform.NativeImage;
import dev.limucc.glinteverything.GlintEverything;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Era-agnostic half of glint tinting: bakes a recolored copy of the vanilla glint texture into a
 * {@link DynamicTexture}, once per color. Turning that texture into a render type is era-specific
 * and lives in {@code compat.GlintTypes} (MC 26.3 fused the glint into the base item/entity shaders).
 */
public final class TintedGlint {

    private static final Identifier VANILLA_ITEM = Identifier.withDefaultNamespace("textures/misc/enchanted_glint_item.png");
    private static final Identifier VANILLA_ARMOR = Identifier.withDefaultNamespace("textures/misc/enchanted_glint_armor.png");

    private static final Map<Long, Identifier> TEXTURES = new HashMap<>();

    private TintedGlint() {}

    /** True for "no tint" (0, or opaque white) - vanilla glint renders untouched. */
    public static boolean isVanilla(int color) {
        return color == 0 || ((color & 0x00FFFFFF) == 0x00FFFFFF && (color >>> 24) == 0xFF);
    }

    /** A copy of the vanilla glint texture multiplied by the ARGB color, uploaded once. */
    public static Identifier texture(boolean armor, int color) {
        long key = ((long) color << 8) | (armor ? 1 : 0);
        if (TEXTURES.containsKey(key)) return TEXTURES.get(key);   // null = known failure, don't retry
        try (InputStream in = Minecraft.getInstance().getResourceManager().open(armor ? VANILLA_ARMOR : VANILLA_ITEM)) {
            NativeImage img = NativeImage.read(in);
            int ca = (color >>> 24) & 0xFF, cr = (color >> 16) & 0xFF, cg = (color >> 8) & 0xFF, cb = color & 0xFF;
            for (int y = 0; y < img.getHeight(); y++) {
                for (int x = 0; x < img.getWidth(); x++) {
                    int p = img.getPixel(x, y);   // ARGB
                    // The vanilla glint texture is almost pure blue/purple, so channel-multiply
                    // yields near-black (invisible under the additive glint blend). Recolor by
                    // intensity instead: brightest channel carries the streak shape.
                    int intensity = Math.max((p >> 16) & 0xFF, Math.max((p >> 8) & 0xFF, p & 0xFF));
                    int a = ((p >>> 24) & 0xFF) * ca / 255;
                    int r = intensity * cr / 255;
                    int g = intensity * cg / 255;
                    int bch = intensity * cb / 255;
                    img.setPixel(x, y, (a << 24) | (r << 16) | (g << 8) | bch);
                }
            }
            Identifier id = Identifier.fromNamespaceAndPath(GlintEverything.MOD_ID,
                    "tinted/" + (armor ? "armor_" : "item_") + Integer.toHexString(color));
            Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(() -> "glinteverything tint", img));
            TEXTURES.put(key, id);
            return id;
        } catch (Exception e) {
            GlintEverything.LOGGER.warn("Could not build tinted glint texture", e);
            TEXTURES.put(key, null);
            return null;
        }
    }
}
