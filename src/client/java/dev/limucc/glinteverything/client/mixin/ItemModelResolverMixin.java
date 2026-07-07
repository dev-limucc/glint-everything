package dev.limucc.glinteverything.client.mixin;

import dev.limucc.glinteverything.client.glint.GlintRuntime;
import dev.limucc.glinteverything.client.glint.GlintTinted;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The one spot where the ItemStack and its render state meet: resolve the tint here. */
@Mixin(ItemModelResolver.class)
public abstract class ItemModelResolverMixin {

    @Inject(method = "updateForTopItem", at = @At("RETURN"))
    private void ge$stampGlintColor(ItemStackRenderState output, ItemStack item,
                                    net.minecraft.world.item.ItemDisplayContext displayContext,
                                    net.minecraft.world.level.Level level,
                                    net.minecraft.world.entity.ItemOwner owner,
                                    int seed, CallbackInfo ci) {
        ((GlintTinted) output).ge$glintColor(GlintRuntime.colorFor(item));
    }
}
