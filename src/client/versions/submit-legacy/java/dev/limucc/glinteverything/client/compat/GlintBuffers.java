package dev.limucc.glinteverything.client.compat;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import dev.limucc.glinteverything.GlintEverything;
import dev.limucc.glinteverything.client.mixin.BufferSourceAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;

/**
 * MC 1.21.x–26.1: custom glint render types must live in the fixed-buffer map so their batch
 * flushes AFTER the base item geometry (map order). Left in the shared buffer they'd flush
 * first, fail the GLINT pipeline's depth-EQUAL test, and never show a pixel.
 */
public final class GlintBuffers {

    private GlintBuffers() {}

    public static void register(RenderType type) {
        try {
            var fixed = ((BufferSourceAccessor)
                    Minecraft.getInstance().renderBuffers().bufferSource()).ge$fixedBuffers();
            fixed.putIfAbsent(type, new ByteBufferBuilder(type.bufferSize()));
        } catch (Exception e) {
            GlintEverything.LOGGER.warn("Could not register tinted glint buffer", e);
        }
    }
}
