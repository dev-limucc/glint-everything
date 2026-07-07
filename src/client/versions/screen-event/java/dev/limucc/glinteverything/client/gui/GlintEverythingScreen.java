package dev.limucc.glinteverything.client.gui;

import dev.limucc.glinteverything.client.compat.Gfx;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

/** MC 1.21.9–1.21.11 glue: classic rendering. All logic + input lives in {@link GlintScreenCore}. */
public class GlintEverythingScreen extends GlintScreenCore {

    public GlintEverythingScreen(Screen parent) {
        super(parent);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float a) {
        super.render(g, mouseX, mouseY, a);
        renderCore(new Gfx(g), mouseX, mouseY);
    }
}
