package dev.limucc.glinteverything.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.limucc.glinteverything.client.glint.GlintContext;
import dev.limucc.glinteverything.client.compat.GlintTypes;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;

/**
 * MC 1.21.9–1.21.11: swap the glint render type for its tinted clone. Items draw through
 * {@code getFoilBuffer} / {@code getSpecialFoilBuffer}, which pick {@code RenderTypes.glint()} /
 * {@code glintTranslucent()} inline (they do NOT go through {@code getFoilRenderTypes}), so both
 * the inline picks and the list getter are hooked.
 */
@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {

    @ModifyReturnValue(method = "getFoilRenderTypes", at = @At("RETURN"))
    private static List<RenderType> ge$tintFoil(List<RenderType> original) {
        int color = GlintContext.getRender();
        if (color == 0) return original;
        List<RenderType> swapped = null;
        for (int i = 0; i < original.size(); i++) {
            RenderType tinted = GlintTypes.swap(original.get(i), color);
            if (tinted != original.get(i)) {
                if (swapped == null) swapped = new ArrayList<>(original);
                swapped.set(i, tinted);
            }
        }
        return swapped != null ? swapped : original;
    }

    private static final String TYPES = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;";
    private static final String RETURNS = "()Lnet/minecraft/client/renderer/rendertype/RenderType;";

    @ModifyExpressionValue(method = {"getFoilBuffer", "getSpecialFoilBuffer"},
            at = @At(value = "INVOKE", target = TYPES + "glint" + RETURNS))
    private static RenderType ge$tintInlineGlint(RenderType original) {
        int color = GlintContext.getRender();
        return color == 0 ? original : GlintTypes.swap(original, color);
    }

    @ModifyExpressionValue(method = {"getFoilBuffer", "getSpecialFoilBuffer"},
            at = @At(value = "INVOKE", target = TYPES + "glintTranslucent" + RETURNS))
    private static RenderType ge$tintInlineGlintTranslucent(RenderType original) {
        int color = GlintContext.getRender();
        return color == 0 ? original : GlintTypes.swap(original, color);
    }
}
