package dev.limucc.glinteverything;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GlintEverything implements ModInitializer {

    public static final String MOD_ID = "glinteverything";
    public static final Logger LOGGER = LoggerFactory.getLogger("GlintEverything");

    @Override
    public void onInitialize() {
        // Client-side mod; everything happens in the client entrypoint.
    }
}
