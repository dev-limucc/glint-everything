package dev.limucc.glinteverything.client.compat;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.GpuSampler;
import dev.limucc.glinteverything.GlintEverything;
import dev.limucc.glinteverything.client.glint.TintedGlint;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.oit.OitPipelineSet;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.TextureTransform;
import net.minecraft.resources.Identifier;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * MC 26.3+: the glint is no longer its own render type. The item / entity / armor render types carry
 * the glint inline (shader define GLINT, second sampler {@code GlintSampler}), so a tinted glint is a
 * clone of the vanilla render type whose {@code GlintSampler} points at a recolored glint texture.
 * The vanilla {@link RenderSetup} is read reflectively (its fields are package-private and the
 * {@code TextureBinding} record is not public), so the clone keeps every other property - pipeline,
 * OIT set, outline, layering, lightmap/overlay - exactly as vanilla built it. Cached per (type, color).
 */
public final class GlintTypes {

    private static final String GLINT_SAMPLER = "GlintSampler";

    private static final Map<Integer, Map<RenderType, RenderType>> CACHE = new HashMap<>();

    private static boolean reflectionReady;
    private static boolean reflectionFailed;
    private static Field typeState, setupPipeline, setupOit, setupTextures, setupTextureTransform, setupOutline,
            setupOutlineName, setupLightmap, setupOverlay, setupCrumbling, setupSort, setupLayering, setupSolidPhase;
    private static Method bindingLocation, bindingSampler;

    private GlintTypes() {}

    /** Tinted clone of a vanilla glint-carrying render type, or the vanilla one if it cannot be cloned. */
    public static RenderType tint(RenderType vanilla, int color) {
        if (vanilla == null || TintedGlint.isVanilla(color)) return vanilla;
        Map<RenderType, RenderType> perColor = CACHE.computeIfAbsent(color, c -> new HashMap<>());
        RenderType cached = perColor.get(vanilla);
        if (cached != null) return cached;
        RenderType clone = build(vanilla, color);
        perColor.put(vanilla, clone != null ? clone : vanilla);   // remember failures too
        return clone != null ? clone : vanilla;
    }

    private static RenderType build(RenderType vanilla, int color) {
        try {
            if (!initReflection()) return null;
            Object setup = typeState.get(vanilla);
            RenderPipeline pipeline = (RenderPipeline) setupPipeline.get(setup);
            @SuppressWarnings("unchecked")
            Map<String, Object> textures = (Map<String, Object>) setupTextures.get(setup);

            // Fused types carry "GlintSampler"; the stand-alone GLINT pipeline (trimmed armor, patterned shield) uses Sampler0.
            String glintName = textures.containsKey(GLINT_SAMPLER) ? GLINT_SAMPLER
                    : (pipeline == RenderPipelines.GLINT ? "Sampler0" : null);
            if (glintName == null) return null;
            Identifier vanillaGlint = (Identifier) bindingLocation.invoke(textures.get(glintName));
            Identifier tinted = TintedGlint.texture(ItemFeatureRenderer.ENCHANTED_GLINT_ARMOR.equals(vanillaGlint), color);
            if (tinted == null) return null;

            RenderSetup.RenderSetupBuilder b = RenderSetup.builder(pipeline);
            OitPipelineSet oit = (OitPipelineSet) setupOit.get(setup);
            if (oit != null) b.setOitPipelines(oit);
            for (Map.Entry<String, Object> e : textures.entrySet()) {
                Identifier loc = e.getKey().equals(glintName) ? tinted : (Identifier) bindingLocation.invoke(e.getValue());
                @SuppressWarnings("unchecked")
                Supplier<GpuSampler> sampler = (Supplier<GpuSampler>) bindingSampler.invoke(e.getValue());
                b.withTexture(e.getKey(), loc, sampler);
            }
            b.setTextureTransform((TextureTransform) setupTextureTransform.get(setup));
            b.setLayeringTransform((LayeringTransform) setupLayering.get(setup));
            if ((boolean) setupLightmap.get(setup)) b.useLightmap();
            if ((boolean) setupOverlay.get(setup)) b.useOverlay();
            if ((boolean) setupCrumbling.get(setup)) b.affectsCrumbling();
            if ((boolean) setupSort.get(setup)) b.sortOnUpload();
            if ((boolean) setupSolidPhase.get(setup)) b.withForcedSolidModelPhase();
            RenderSetup.OutlineProperty outline = (RenderSetup.OutlineProperty) setupOutline.get(setup);
            String outlineName = (String) setupOutlineName.get(setup);
            if (outlineName != null) {
                b.setOutline(outline, outlineName);
            } else {
                b.setOutline(outline);
            }
            return RenderType.create("glinteverything:tinted_" + Integer.toHexString(color), b.createRenderSetup());
        } catch (Throwable t) {
            GlintEverything.LOGGER.warn("Could not build tinted glint render type", t);
            return null;
        }
    }

    private static boolean initReflection() {
        if (reflectionReady) return true;
        if (reflectionFailed) return false;
        try {
            typeState = field(RenderType.class, "state");
            Class<?> s = RenderSetup.class;
            setupPipeline = field(s, "pipeline");
            setupOit = field(s, "oitPipelineSet");
            setupTextures = field(s, "textures");
            setupTextureTransform = field(s, "textureTransform");
            setupOutline = field(s, "outlineProperty");
            setupOutlineName = field(s, "outlineTextureName");
            setupLightmap = field(s, "useLightmap");
            setupOverlay = field(s, "useOverlay");
            setupCrumbling = field(s, "affectsCrumbling");
            setupSort = field(s, "sortOnUpload");
            setupLayering = field(s, "layeringTransform");
            setupSolidPhase = field(s, "forceSolidModelPhase");
            Class<?> binding = Class.forName("net.minecraft.client.renderer.rendertype.RenderSetup$TextureBinding");
            bindingLocation = binding.getDeclaredMethod("location");
            bindingLocation.setAccessible(true);
            bindingSampler = binding.getDeclaredMethod("sampler");
            bindingSampler.setAccessible(true);
            reflectionReady = true;
        } catch (Throwable t) {
            reflectionFailed = true;
            GlintEverything.LOGGER.warn("Glint tinting disabled: RenderSetup layout changed", t);
        }
        return reflectionReady;
    }

    private static Field field(Class<?> c, String name) throws NoSuchFieldException {
        Field f = c.getDeclaredField(name);
        f.setAccessible(true);
        return f;
    }
}
