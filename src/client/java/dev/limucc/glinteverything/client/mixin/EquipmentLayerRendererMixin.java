package dev.limucc.glinteverything.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.limucc.glinteverything.client.glint.GlintContext;
import dev.limucc.glinteverything.client.glint.GlintRuntime;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Worn-armor glint: publish the stack's tint while renderLayers runs; the actual swap happens in
 * {@code RenderTypesMixin} on armorEntityGlint(). Wrapping the call site directly is not an
 * option — fabric-api's armor hooks rewrite it. {@code require = 0}: armor tinting is an extra,
 * it must never break a boot if this method shifts in a future version.
 */
@Mixin(EquipmentLayerRenderer.class)
public abstract class EquipmentLayerRendererMixin {

    private static final String RENDER_LAYERS =
            "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;"
            + "Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;"
            + "Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;"
            + "Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V";

    @Inject(method = RENDER_LAYERS, at = @At("HEAD"), require = 0)
    @SuppressWarnings("rawtypes")
    private void ge$armorHead(EquipmentClientInfo.LayerType layerType, ResourceKey equipmentAssetId, Model model,
                              Object state, ItemStack itemStack, PoseStack poseStack, SubmitNodeCollector collector,
                              int lightCoords, Identifier playerTextureOverride, int outlineColor, int order,
                              CallbackInfo ci) {
        GlintContext.setArmor(GlintRuntime.colorFor(itemStack));
    }

    @Inject(method = RENDER_LAYERS, at = @At("RETURN"), require = 0)
    @SuppressWarnings("rawtypes")
    private void ge$armorTail(EquipmentClientInfo.LayerType layerType, ResourceKey equipmentAssetId, Model model,
                              Object state, ItemStack itemStack, PoseStack poseStack, SubmitNodeCollector collector,
                              int lightCoords, Identifier playerTextureOverride, int outlineColor, int order,
                              CallbackInfo ci) {
        GlintContext.setArmor(0);
    }
}
