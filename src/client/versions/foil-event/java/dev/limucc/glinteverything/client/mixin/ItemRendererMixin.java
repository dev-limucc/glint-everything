package dev.limucc.glinteverything.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.limucc.glinteverything.client.glint.GlintContext;
import dev.limucc.glinteverything.client.glint.TintedGlint;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;

/** MC 1.21.9–1.21.11: swap every glint render type in the foil list for its tinted clone. */
@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {

    @ModifyReturnValue(method = "getFoilRenderTypes", at = @At("RETURN"))
    private static List<RenderType> ge$tintFoil(List<RenderType> original) {
        int color = GlintContext.getRender();
        if (color == 0) return original;
        List<RenderType> swapped = null;
        for (int i = 0; i < original.size(); i++) {
            RenderType tinted = TintedGlint.swap(original.get(i), color);
            if (tinted != original.get(i)) {
                if (swapped == null) swapped = new ArrayList<>(original);
                swapped.set(i, tinted);
            }
        }
        return swapped != null ? swapped : original;
    }
}
