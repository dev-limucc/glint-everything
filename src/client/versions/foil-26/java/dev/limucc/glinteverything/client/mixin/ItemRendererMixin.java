package dev.limucc.glinteverything.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.limucc.glinteverything.client.glint.GlintContext;
import dev.limucc.glinteverything.client.glint.TintedGlint;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** MC 26.1+: swap the chosen foil render type for its tinted clone. */
@Mixin(ItemFeatureRenderer.class)
public abstract class ItemRendererMixin {

    @ModifyReturnValue(method = "getFoilRenderType", at = @At("RETURN"))
    private static RenderType ge$tintFoil(RenderType original) {
        int color = GlintContext.getRender();
        return color == 0 ? original : TintedGlint.swap(original, color);
    }
}
