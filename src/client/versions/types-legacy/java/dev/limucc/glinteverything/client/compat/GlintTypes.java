package dev.limucc.glinteverything.client.compat;

import dev.limucc.glinteverything.client.glint.TintedGlint;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.rendertype.TextureTransform;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * MC 1.21.11-26.2: the glint is its own render type on the GLINT pipeline (POSITION_TEX, no vertex
 * color), so tinting = a clone of the vanilla glint render type bound to a recolored glint texture.
 * Everything is cached per color.
 */
public final class GlintTypes {

    private enum Kind { ITEM, ENTITY, TRANSLUCENT, ARMOR }

    private static final Map<Long, RenderType> TYPES = new HashMap<>();

    private GlintTypes() {}

    /** Swap a vanilla glint render type for its tinted clone; anything else passes through. */
    public static RenderType swap(RenderType vanilla, int color) {
        if (TintedGlint.isVanilla(color)) return vanilla;
        if (vanilla == RenderTypes.glint()) return orElse(get(Kind.ITEM, color), vanilla);
        if (vanilla == RenderTypes.entityGlint()) return orElse(get(Kind.ENTITY, color), vanilla);
        if (vanilla == RenderTypes.glintTranslucent()) return orElse(get(Kind.TRANSLUCENT, color), vanilla);
        if (vanilla == RenderTypes.armorEntityGlint()) return orElse(get(Kind.ARMOR, color), vanilla);
        return vanilla;
    }

    /**
     * Tinted armor glint or null. Called from inside {@code RenderTypes.armorEntityGlint()}'s
     * return-modifier, so nothing here may call any {@code RenderTypes} glint getter.
     */
    public static RenderType armor(int color) {
        if (TintedGlint.isVanilla(color)) return null;
        return get(Kind.ARMOR, color);
    }

    /**
     * Tinted entity glint (held shield / trident special models) or null. Called from inside
     * {@code RenderTypes.entityGlint()}'s return-modifier, so nothing here may call a {@code RenderTypes} glint getter.
     */
    public static RenderType entity(int color) {
        if (TintedGlint.isVanilla(color)) return null;
        return get(Kind.ENTITY, color);
    }

    private static RenderType orElse(RenderType type, RenderType fallback) {
        return type != null ? type : fallback;
    }

    /** Tinted clone or null when the texture could not be built. Never touches vanilla getters. */
    private static RenderType get(Kind kind, int color) {
        long key = ((long) color << 8) | kind.ordinal();
        RenderType cached = TYPES.get(key);
        if (cached != null) return cached;

        Identifier texture = TintedGlint.texture(kind == Kind.ARMOR, color);
        if (texture == null) return null;

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
        GlintBuffers.register(type);
        TYPES.put(key, type);
        return type;
    }
}
