package dev.limucc.glinteverything.client.mixin;

import dev.limucc.glinteverything.client.glint.GlintContext;
import dev.limucc.glinteverything.client.glint.GlintTinted;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** MC 26.1+: expose the submit's tint while its quads (incl. the foil pass) are drawn. */
@Mixin(ItemFeatureRenderer.class)
public abstract class ItemFeatureRendererMixin {

    @Inject(method = "renderItem", at = @At("HEAD"))
    private void ge$beforeItem(MultiBufferSource.BufferSource bufferSource, OutlineBufferSource outlineBufferSource,
                               SubmitNodeStorage.ItemSubmit submit, CallbackInfo ci) {
        GlintContext.setRender(((GlintTinted) (Object) submit).ge$glintColor());
    }

    @Inject(method = "renderItem", at = @At("RETURN"))
    private void ge$afterItem(MultiBufferSource.BufferSource bufferSource, OutlineBufferSource outlineBufferSource,
                              SubmitNodeStorage.ItemSubmit submit, CallbackInfo ci) {
        GlintContext.setRender(0);
    }
}
