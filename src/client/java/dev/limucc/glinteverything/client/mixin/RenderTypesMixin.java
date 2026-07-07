package dev.limucc.glinteverything.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.limucc.glinteverything.client.glint.GlintContext;
import dev.limucc.glinteverything.client.glint.TintedGlint;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * While EquipmentLayerRenderer publishes an armor tint, hand out the tinted armor glint clone.
 * {@code TintedGlint.armor} never calls back into RenderTypes, so this cannot recurse.
 */
@Mixin(RenderTypes.class)
public abstract class RenderTypesMixin {

    @ModifyReturnValue(method = "armorEntityGlint", at = @At("RETURN"))
    private static RenderType ge$tintArmorGlint(RenderType original) {
        int color = GlintContext.getArmor();
        if (color == 0) return original;
        RenderType tinted = TintedGlint.armor(color);
        return tinted != null ? tinted : original;
    }
}
