package dev.limucc.glinteverything.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.limucc.glinteverything.client.glint.GlintContext;
import dev.limucc.glinteverything.client.compat.GlintTypes;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * MC 1.21.11-26.2. While EquipmentLayerRenderer publishes an armor tint, hand out the tinted armor glint clone;
 * while {@code ItemStackRenderState.submit} publishes an item tint, hand out the tinted entity glint clone (the
 * held shield / trident special models fetch {@code RenderTypes.entityGlint()} directly, bypassing the foil buffer
 * hooks). {@code GlintTypes.armor/entity} never call back into RenderTypes, so this cannot recurse.
 */
@Mixin(RenderTypes.class)
public abstract class RenderTypesMixin {

    @ModifyReturnValue(method = "armorEntityGlint", at = @At("RETURN"))
    private static RenderType ge$tintArmorGlint(RenderType original) {
        int color = GlintContext.getArmor();
        if (color == 0) return original;
        RenderType tinted = GlintTypes.armor(color);
        return tinted != null ? tinted : original;
    }

    @ModifyReturnValue(method = "entityGlint", at = @At("RETURN"), require = 0)
    private static RenderType ge$tintEntityGlint(RenderType original) {
        int color = GlintContext.getExtract();
        if (color == 0) return original;
        RenderType tinted = GlintTypes.entity(color);
        return tinted != null ? tinted : original;
    }
}
