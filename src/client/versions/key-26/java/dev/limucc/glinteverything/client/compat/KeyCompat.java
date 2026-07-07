package dev.limucc.glinteverything.client.compat;

import com.mojang.blaze3d.platform.InputConstants;
import dev.limucc.glinteverything.GlintEverything;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

/** MC 26.1+: keybindings registered with a {@link KeyMapping.Category} and Fabric's KeyMappingHelper. */
public final class KeyCompat {

    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(GlintEverything.MOD_ID, "main"));

    private KeyCompat() {}

    public static KeyMapping register(String translationKey) {
        return KeyMappingHelper.registerKeyMapping(
                new KeyMapping(translationKey, InputConstants.UNKNOWN.getValue(), CATEGORY));
    }
}
