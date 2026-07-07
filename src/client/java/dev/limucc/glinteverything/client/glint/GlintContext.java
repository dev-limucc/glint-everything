package dev.limucc.glinteverything.client.glint;

/**
 * Hands the per-stack glint color across the two seams where vanilla loses the ItemStack:
 * extraction ({@code ItemStackRenderState.submit} -> {@code ItemSubmit} construction) and
 * drawing ({@code ItemSubmit} -> foil render type selection). Both hand-offs happen within
 * one call stack, so a ThreadLocal is exact even with parallel extraction.
 */
public final class GlintContext {

    private static final ThreadLocal<int[]> EXTRACT = ThreadLocal.withInitial(() -> new int[1]);
    private static final ThreadLocal<int[]> RENDER = ThreadLocal.withInitial(() -> new int[1]);

    private GlintContext() {}

    public static void setExtract(int color) { EXTRACT.get()[0] = color; }
    public static int getExtract() { return EXTRACT.get()[0]; }

    public static void setRender(int color) { RENDER.get()[0] = color; }
    public static int getRender() { return RENDER.get()[0]; }
}
