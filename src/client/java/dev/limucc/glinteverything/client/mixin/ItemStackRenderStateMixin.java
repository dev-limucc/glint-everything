package dev.limucc.glinteverything.client.mixin;

import dev.limucc.glinteverything.client.glint.GlintContext;
import dev.limucc.glinteverything.client.glint.GlintTinted;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Carries the resolved glint color on the render state (stamped by {@code ItemModelResolverMixin})
 * and exposes it to {@code ItemSubmit} construction for the duration of {@code submit(...)}.
 */
@Mixin(ItemStackRenderState.class)
public abstract class ItemStackRenderStateMixin implements GlintTinted {

    @Unique
    private int ge$glintColor;

    @Override
    public int ge$glintColor() { return ge$glintColor; }

    @Override
    public void ge$glintColor(int color) { ge$glintColor = color; }

    @Inject(method = "clear", at = @At("RETURN"))
    private void ge$clear(CallbackInfo ci) {
        ge$glintColor = 0;
    }

    @Inject(method = "submit", at = @At("HEAD"))
    private void ge$submitHead(CallbackInfo ci) {
        GlintContext.setExtract(ge$glintColor);
    }

    @Inject(method = "submit", at = @At("RETURN"))
    private void ge$submitTail(CallbackInfo ci) {
        GlintContext.setExtract(0);
    }
}
