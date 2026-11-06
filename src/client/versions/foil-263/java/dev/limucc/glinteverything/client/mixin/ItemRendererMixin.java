package dev.limucc.glinteverything.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.limucc.glinteverything.client.compat.GlintTypes;
import dev.limucc.glinteverything.client.glint.GlintContext;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * MC 26.3+: {@code prepareMainSubmit} picks each quad's render type from its material
 * ({@code itemGlintRenderType()} / {@code itemGlintSpecialRenderType()} for foil items) - swap whichever
 * was picked for its tinted clone.
 */
@Mixin(ItemFeatureRenderer.class)
public abstract class ItemRendererMixin {

    @ModifyExpressionValue(method = "prepareMainSubmit", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/resources/model/geometry/BakedQuad$MaterialInfo;itemGlintRenderType()Lnet/minecraft/client/renderer/rendertype/RenderType;"))
    private RenderType ge$tintGlint(RenderType original) {
        return ge$tint(original);
    }

    @ModifyExpressionValue(method = "prepareMainSubmit", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/resources/model/geometry/BakedQuad$MaterialInfo;itemGlintSpecialRenderType()Lnet/minecraft/client/renderer/rendertype/RenderType;"))
    private RenderType ge$tintGlintSpecial(RenderType original) {
        return ge$tint(original);
    }

    @Unique
    private static RenderType ge$tint(RenderType original) {
        int color = GlintContext.getRender();
        return color == 0 ? original : GlintTypes.tint(original, color);
    }
}
