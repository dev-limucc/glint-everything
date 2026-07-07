package dev.limucc.glinteverything.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.limucc.glinteverything.client.glint.GlintRuntime;
import dev.limucc.glinteverything.client.glint.TintedGlint;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Worn-armor glint: the stack is right there, so tint the armor glint render type directly. */
@Mixin(EquipmentLayerRenderer.class)
public abstract class EquipmentLayerRendererMixin {

    @WrapOperation(method = "renderLayers",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;armorEntityGlint()Lnet/minecraft/client/renderer/rendertype/RenderType;"))
    private RenderType ge$tintArmorGlint(Operation<RenderType> original, @Local(argsOnly = true) ItemStack stack) {
        return TintedGlint.swap(original.call(), GlintRuntime.colorFor(stack));
    }
}
