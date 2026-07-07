package dev.limucc.glinteverything.client.glint;

import com.mojang.blaze3d.platform.NativeImage;
import dev.limucc.glinteverything.GlintEverything;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.rendertype.TextureTransform;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Colored clones of the four vanilla glint render types. The GLINT pipeline has no vertex color
 * (POSITION_TEX), so tinting is done by baking a recolored copy of the vanilla glint texture into
 * a {@link DynamicTexture} and cloning the render type with it. Everything is cached per color.
 */
public final class TintedGlint {

    private static final Identifier VANILLA_ITEM = Identifier.withDefaultNamespace("textures/misc/enchanted_glint_item.png");
    private static final Identifier VANILLA_ARMOR = Identifier.withDefaultNamespace("textures/misc/enchanted_glint_armor.png");

    private enum Kind { ITEM, ENTITY, TRANSLUCENT, ARMOR }

    private static final Map<Long, RenderType> TYPES = new HashMap<>();
    private static final Map<Long, Identifier> TEXTURES = new HashMap<>();

    private TintedGlint() {}

    /** Swap a vanilla glint render type for its tinted clone; anything else passes through. */
    public static RenderType swap(RenderType vanilla, int color) {
        if (color == 0 || (color & 0x00FFFFFF) == 0x00FFFFFF && (color >>> 24) == 0xFF) return vanilla;
        if (vanilla == RenderTypes.glint()) return get(Kind.ITEM, color);
        if (vanilla == RenderTypes.entityGlint()) return get(Kind.ENTITY, color);
        if (vanilla == RenderTypes.glintTranslucent()) return get(Kind.TRANSLUCENT, color);
        if (vanilla == RenderTypes.armorEntityGlint()) return get(Kind.ARMOR, color);
        return vanilla;
    }

    private static RenderType get(Kind kind, int color) {
        long key = ((long) color << 8) | kind.ordinal();
        RenderType cached = TYPES.get(key);
        if (cached != null) return cached;

        Identifier texture = texture(kind == Kind.ARMOR, color);
        if (texture == null) return vanillaFor(kind);   // texture load failed; degrade cleanly

        String name = "glinteverything:" + kind.name().toLowerCase() + "_" + Integer.toHexString(color);
        RenderSetup.RenderSetupBuilder b = RenderSetup.builder(RenderPipelines.GLINT).withTexture("Sampler0", texture);
        RenderType type = switch (kind) {
            case ITEM        -> RenderType.create(name, b.setTextureTransform(TextureTransform.GLINT_TEXTURING).createRenderSetup());
            case ENTITY      -> RenderType.create(name, b.setTextureTransform(TextureTransform.ENTITY_GLINT_TEXTURING).createRenderSetup());
            case TRANSLUCENT -> RenderType.create(name, b.setTextureTransform(TextureTransform.GLINT_TEXTURING)
                                                         .setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET).createRenderSetup());
            case ARMOR       -> RenderType.create(name, b.setTextureTransform(TextureTransform.ARMOR_ENTITY_GLINT_TEXTURING)
                                                         .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING).createRenderSetup());
        };
        TYPES.put(key, type);
        return type;
    }

    private static RenderType vanillaFor(Kind kind) {
        return switch (kind) {
            case ITEM -> RenderTypes.glint();
            case ENTITY -> RenderTypes.entityGlint();
            case TRANSLUCENT -> RenderTypes.glintTranslucent();
            case ARMOR -> RenderTypes.armorEntityGlint();
        };
    }

    /** A copy of the vanilla glint texture multiplied by the ARGB color, uploaded once. */
    private static Identifier texture(boolean armor, int color) {
        long key = ((long) color << 8) | (armor ? 1 : 0);
        if (TEXTURES.containsKey(key)) return TEXTURES.get(key);   // null = known failure, don't retry
        try (InputStream in = Minecraft.getInstance().getResourceManager().open(armor ? VANILLA_ARMOR : VANILLA_ITEM)) {
            NativeImage img = NativeImage.read(in);
            int ca = (color >>> 24) & 0xFF, cr = (color >> 16) & 0xFF, cg = (color >> 8) & 0xFF, cb = color & 0xFF;
            for (int y = 0; y < img.getHeight(); y++) {
                for (int x = 0; x < img.getWidth(); x++) {
                    int p = img.getPixel(x, y);   // ARGB
                    int a = ((p >>> 24) & 0xFF) * ca / 255;
                    int r = ((p >> 16) & 0xFF) * cr / 255;
                    int g = ((p >> 8) & 0xFF) * cg / 255;
                    int bch = (p & 0xFF) * cb / 255;
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
