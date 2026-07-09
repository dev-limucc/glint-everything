package dev.limucc.glinteverything.client.mixin;

import dev.limucc.glinteverything.client.glint.GlintContext;
import dev.limucc.glinteverything.client.glint.GlintTinted;
import net.minecraft.client.renderer.SubmitNodeStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Stamps the tint (set by the surrounding {@code ItemStackRenderState.submit}) onto each submit. */
@Mixin(SubmitNodeStorage.ItemSubmit.class)
public abstract class ItemSubmitMixin implements GlintTinted {

    @Unique
    private int ge$glintColor;

    @Override
    public int ge$glintColor() { return ge$glintColor; }

    @Override
    public void ge$glintColor(int color) { ge$glintColor = color; }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void ge$init(CallbackInfo ci) {
        ge$glintColor = GlintContext.getExtract();
    }
}
