package dev.limucc.glinteverything.client.mixin;

import dev.limucc.glinteverything.client.glint.GlintContext;
import dev.limucc.glinteverything.client.glint.GlintTinted;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** MC 26.2+: expose the submit's tint while its foil pass is prepared. */
@Mixin(ItemFeatureRenderer.class)
public abstract class ItemFeatureRendererMixin {

    @Inject(method = "prepareFoilSubmit", at = @At("HEAD"))
    private void ge$beforeFoil(ItemFeatureRenderer.Submit submit, CallbackInfo ci) {
        GlintContext.setRender(((GlintTinted) (Object) submit).ge$glintColor());
    }

    @Inject(method = "prepareFoilSubmit", at = @At("RETURN"))
    private void ge$afterFoil(ItemFeatureRenderer.Submit submit, CallbackInfo ci) {
        GlintContext.setRender(0);
    }
}
