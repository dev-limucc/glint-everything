package dev.limucc.glinteverything.client.mixin;

import dev.limucc.glinteverything.client.glint.GlintContext;
import dev.limucc.glinteverything.client.glint.GlintTinted;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** MC 26.3+: expose the submit's tint while its quads (which now carry the glint inline) pick their render types. */
@Mixin(ItemFeatureRenderer.class)
public abstract class ItemFeatureRendererMixin {

    @Inject(method = "prepareMainSubmit", at = @At("HEAD"))
    private void ge$beforeMain(ItemFeatureRenderer.Submit submit, CallbackInfo ci) {
        GlintContext.setRender(((GlintTinted) (Object) submit).ge$glintColor());
    }

    @Inject(method = "prepareMainSubmit", at = @At("RETURN"))
    private void ge$afterMain(ItemFeatureRenderer.Submit submit, CallbackInfo ci) {
        GlintContext.setRender(0);
    }
}
