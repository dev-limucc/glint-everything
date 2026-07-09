package dev.limucc.glinteverything.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.limucc.glinteverything.client.glint.GlintContext;
import dev.limucc.glinteverything.client.glint.TintedGlint;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * MC 26.2+: the foil render type is chosen inline in {@code getFoilBuffer}
 * (glintTranslucent vs glint) — swap whichever was picked for its tinted clone.
 */
@Mixin(ItemFeatureRenderer.class)
public abstract class ItemRendererMixin {

    @ModifyExpressionValue(method = "getFoilBuffer", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;glintTranslucent()Lnet/minecraft/client/renderer/rendertype/RenderType;"))
    private RenderType ge$tintTranslucentFoil(RenderType original) {
        return ge$tint(original);
    }

    @ModifyExpressionValue(method = "getFoilBuffer", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;glint()Lnet/minecraft/client/renderer/rendertype/RenderType;"))
    private RenderType ge$tintFoil(RenderType original) {
        return ge$tint(original);
    }

    @Unique
    private static RenderType ge$tint(RenderType original) {
        int color = GlintContext.getRender();
        return color == 0 ? original : TintedGlint.swap(original, color);
    }
}
