package dev.limucc.glinteverything.client.compat;

import net.minecraft.client.renderer.rendertype.RenderType;

/**
 * MC 26.2+: nothing to register — feature renderers allocate a vertex builder per render type
 * on demand ({@code RenderTypeFeatureRenderer.getVertexBuilder}), and each foil batch is drawn
 * in-phase after its base geometry, so the fixed-buffer flush-order trick is obsolete.
 */
public final class GlintBuffers {

    private GlintBuffers() {}

    public static void register(RenderType type) {
    }
}
