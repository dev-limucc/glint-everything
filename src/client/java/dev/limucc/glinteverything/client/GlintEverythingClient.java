package dev.limucc.glinteverything.client;

import dev.limucc.glinteverything.client.compat.KeyCompat;
import dev.limucc.glinteverything.client.glint.GlintRuntime;
import dev.limucc.glinteverything.client.gui.GlintEverythingScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.KeyMapping;

public class GlintEverythingClient implements ClientModInitializer {

    private static KeyMapping openSettingsKey;

    @Override
    public void onInitializeClient() {
        GlintRuntime.load();

        openSettingsKey = KeyCompat.register("key.glinteverything.open");

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (openSettingsKey.consumeClick()) {
                if (mc.screen == null) mc.setScreen(new GlintEverythingScreen(null));
            }
        });
    }
}
