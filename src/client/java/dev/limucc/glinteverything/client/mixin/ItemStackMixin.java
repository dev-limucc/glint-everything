package dev.limucc.glinteverything.client.mixin;

import dev.limucc.glinteverything.client.glint.GlintRuntime;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Whitelisted items glint; "Hidden" natural glint stops glinting. Every render path asks here. */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    @Inject(method = "hasFoil", at = @At("RETURN"), cancellable = true)
    private void ge$hasFoil(CallbackInfoReturnable<Boolean> cir) {
        ItemStack self = (ItemStack) (Object) this;
        if (cir.getReturnValueZ()) {
            if (GlintRuntime.suppressFoil(self)) cir.setReturnValue(false);
        } else {
            if (GlintRuntime.forceFoil(self)) cir.setReturnValue(true);
        }
    }
}
