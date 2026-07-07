package dev.limucc.glinteverything.client.mixin;

import dev.limucc.glinteverything.client.config.GlintConfig;
import dev.limucc.glinteverything.client.glint.GlintRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.TextureTransform;
import net.minecraft.util.Util;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Rebuilds the glint scroll matrix with the configured speed / scale / angle / direction.
 * Bails instantly when the animation settings are vanilla so the stock path runs untouched.
 */
@Mixin(TextureTransform.class)
public abstract class TextureTransformMixin {

    @Inject(method = "setupGlintTexturing", at = @At("HEAD"), cancellable = true)
    private static void ge$setupGlintTexturing(float scale, CallbackInfoReturnable<Matrix4f> cir) {
        GlintConfig c = GlintRuntime.config();
        if (!c.enabled || c.isDefaultAnim()) return;

        double vanillaSpeed = Minecraft.getInstance().options.glintSpeed().get();
        long millis = (long) (Util.getMillis() * vanillaSpeed * 8.0 * c.speed);
        float o0 = (millis % 110000L) / 110000.0F;
        float o1 = (millis % 30000L) / 30000.0F;

        float tx;
        float ty;
        switch (c.style) {
            case REVERSE    -> { tx = o0;  ty = -o1; }
            case VERTICAL   -> { tx = 0;   ty = o1;  }
            case HORIZONTAL -> { tx = -o1; ty = 0;   }
            case STATIC     -> { tx = 0;   ty = 0;   }
            default         -> { tx = -o0; ty = o1;  }   // CLASSIC = vanilla drift
        }

        Matrix4f matrix = new Matrix4f().translation(tx, ty, 0.0F);
        matrix.rotateZ((float) Math.toRadians(c.angle)).scale(Math.max(0.05F, scale * c.scale));
        cir.setReturnValue(matrix);
    }
}
