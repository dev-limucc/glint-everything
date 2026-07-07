package dev.limucc.glinteverything.client.gui;

import dev.limucc.glinteverything.client.compat.Gfx;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;

/** MC 26.1+ glue: extract-render-state rendering. All logic + input lives in {@link GlintScreenCore}. */
public class GlintEverythingScreen extends GlintScreenCore {

    public GlintEverythingScreen(Screen parent) {
        super(parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        renderCore(new Gfx(g), mouseX, mouseY);
    }
}
