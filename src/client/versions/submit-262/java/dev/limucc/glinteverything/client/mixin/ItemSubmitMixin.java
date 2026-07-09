package dev.limucc.glinteverything.client.mixin;

import dev.limucc.glinteverything.client.glint.GlintContext;
import dev.limucc.glinteverything.client.glint.GlintTinted;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MC 26.2+: item submits became {@link ItemFeatureRenderer.Submit} records built inside
 * {@code SubmitNodeCollection.submitItem}, still synchronously within
 * {@code ItemStackRenderState.submit} — so the extract-side tint hand-off is unchanged.
 */
@Mixin(ItemFeatureRenderer.Submit.class)
public abstract class ItemSubmitMixin implements GlintTinted {

    @Unique
    private int ge$glintColor;

    @Override
    public int ge$glintColor() { return ge$glintColor; }

    @Override
    public void ge$glintColor(int color) { ge$glintColor = color; }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void ge$init(CallbackInfo ci) {
        ge$glintColor = GlintContext.getExtract();
    }
}
