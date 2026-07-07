package dev.limucc.glinteverything.client.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.limucc.glinteverything.client.glint.GlintContext;
import dev.limucc.glinteverything.client.glint.GlintTinted;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** MC 1.21.9–1.21.11: expose the submit's tint around the (first, foil-capable) renderItem call. */
@Mixin(ItemFeatureRenderer.class)
public abstract class ItemFeatureRendererMixin {

    private static final String RENDER_ITEM =
            "Lnet/minecraft/client/renderer/entity/ItemRenderer;renderItem(Lnet/minecraft/world/item/ItemDisplayContext;"
            + "Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II[I"
            + "Ljava/util/List;Lnet/minecraft/client/renderer/rendertype/RenderType;"
            + "Lnet/minecraft/client/renderer/item/ItemStackRenderState$FoilType;)V";

    @Inject(method = "render", at = @At(value = "INVOKE", target = RENDER_ITEM, ordinal = 0))
    private void ge$beforeItem(CallbackInfo ci, @Local SubmitNodeStorage.ItemSubmit submit) {
        GlintContext.setRender(((GlintTinted) (Object) submit).ge$glintColor());
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = RENDER_ITEM, ordinal = 0, shift = At.Shift.AFTER))
    private void ge$afterItem(CallbackInfo ci) {
        GlintContext.setRender(0);
    }
}
