package dev.limucc.glinteverything.client.mixin;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.SequencedMap;

/**
 * Custom glint render types must live in the fixed-buffer map: shared-buffer batches flush
 * BEFORE the base item geometry, and the GLINT pipeline's depth EQUAL test then fails on an
 * empty depth buffer — the glint silently disappears. Fixed buffers flush after, in map order.
 */
@Mixin(MultiBufferSource.BufferSource.class)
public interface BufferSourceAccessor {

    @Accessor("fixedBuffers")
    SequencedMap<RenderType, ByteBufferBuilder> ge$fixedBuffers();
}
