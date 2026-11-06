package dev.limucc.glinteverything.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.limucc.glinteverything.client.compat.GlintTypes;
import dev.limucc.glinteverything.client.glint.GlintContext;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * MC 26.3+: worn armor and special held items (shield, trident) get their glint render type straight from
 * {@code RenderTypes}. While a tint is published (armor: {@code EquipmentLayerRendererMixin}; held item:
 * {@code ItemStackRenderState.submit}), hand out the tinted clone. {@code GlintTypes.tint} never calls
 * back into these getters, so this cannot recurse. Extras, so {@code require = 0}: a shifted getter must
 * not break a boot.
 */
@Mixin(RenderTypes.class)
public abstract class RenderTypesMixin {

    @ModifyReturnValue(method = "armorCutoutNoCullGlint", at = @At("RETURN"), require = 0)
    private static RenderType ge$tintArmorGlint(RenderType original) {
        int color = GlintContext.getArmor();
        return color == 0 ? original : GlintTypes.tint(original, color);
    }

    @ModifyReturnValue(method = "trimmedArmorGlint", at = @At("RETURN"), require = 0)
    private static RenderType ge$tintTrimmedGlint(RenderType original) {
        int color = GlintContext.getArmor();
        return color == 0 ? original : GlintTypes.tint(original, color);
    }

    @ModifyReturnValue(method = "entitySolidGlint", at = @At("RETURN"), require = 0)
    private static RenderType ge$tintEntityGlint(RenderType original) {
        int color = GlintContext.getExtract();
        return color == 0 ? original : GlintTypes.tint(original, color);
    }

    @ModifyReturnValue(method = "patternedShieldGlint", at = @At("RETURN"), require = 0)
    private static RenderType ge$tintShieldGlint(RenderType original) {
        int color = GlintContext.getExtract();
        return color == 0 ? original : GlintTypes.tint(original, color);
    }
}
