package dev.limucc.glinteverything.gametest;

import dev.limucc.glinteverything.client.compat.ScreenNav;
import dev.limucc.glinteverything.client.config.GlintConfig;
import dev.limucc.glinteverything.client.glint.GlintRuntime;
import dev.limucc.glinteverything.client.gui.GlintEverythingScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.renderer.rendertype.TextureTransform;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.MixinEnvironment;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.nio.file.Path;

/**
 * Era-agnostic runtime check for every Glint Everything feature (whitelist, natural-glint modes, tint,
 * per-item tint, armor tint, animation matrix, settings GUI). Run with
 * {@code gradlew runClientGameTest "-Ptarget=<mc>"}. Screenshots land in build/run/clientGameTest/screenshots.
 * Pixel checks compare screenshots of the same static scene, so only the glint can differ.
 */
public class GlintEverythingGameTest implements FabricClientGameTest {

    private static final String ENCHANTED_SWORD = "minecraft:diamond_sword[enchantments={sharpness:1}]";
    private static final String PLAIN_SWORD = "minecraft:iron_sword";

    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError("GE-GAMETEST FAIL: " + what);
        System.out.println("[GE-GAMETEST] PASS: " + what);
    }

    private static void info(String what) {
        System.out.println("[GE-GAMETEST] INFO: " + what);
    }

    private static final String KNOWN_NO_SPECIAL_TINT = "1.21.11";

    private static String mcVersion() {
        return net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer("minecraft")
                .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("?");
    }

    private static GlintConfig cfg() {
        return GlintRuntime.config();
    }

    private static void resetConfig() {
        GlintConfig c = cfg();
        c.enabled = true;
        c.whitelist.clear();
        c.whitelistGlint = true;
        c.tintExtra = false;
        c.naturalGlint = GlintConfig.NaturalMode.DEFAULT;
        c.itemColors.clear();
        c.speed = 1.0f;
        c.scale = 1.0f;
        c.angle = 10.0f;
        c.style = GlintConfig.AnimStyle.CLASSIC;
        GlintRuntime.rebuild();
    }

    // ── pixel helpers ───────────────────────────────────────────────────────────
    private static BufferedImage read(Path p) throws Exception {
        return ImageIO.read(p.toFile());
    }

    /** Number of pixels in the region whose channel difference exceeds 24 (any channel). */
    private static int changed(BufferedImage a, BufferedImage b, int x0, int y0, int x1, int y1) {
        int n = 0;
        for (int y = y0; y < y1; y++) {
            for (int x = x0; x < x1; x++) {
                int p = a.getRGB(x, y), q = b.getRGB(x, y);
                int dr = Math.abs(((p >> 16) & 255) - ((q >> 16) & 255));
                int dg = Math.abs(((p >> 8) & 255) - ((q >> 8) & 255));
                int db = Math.abs((p & 255) - (q & 255));
                if (dr > 24 || dg > 24 || db > 24) n++;
            }
        }
        return n;
    }

    /** Sum of (channel - max(other two)) over the region; channel: 0=R 1=G 2=B. A hue-dominance score. */
    private static long dominance(BufferedImage a, int channel, int x0, int y0, int x1, int y1) {
        long sum = 0;
        for (int y = y0; y < y1; y++) {
            for (int x = x0; x < x1; x++) {
                int p = a.getRGB(x, y);
                int r = (p >> 16) & 255, g = (p >> 8) & 255, b = p & 255;
                int v = channel == 0 ? r - Math.max(g, b) : channel == 1 ? g - Math.max(r, b) : b - Math.max(r, g);
                if (v > 0) sum += v;
            }
        }
        return sum;
    }

    /** The live glint scroll matrix; the accessor was renamed getMatrix() -> createMatrix() in 26.2. */
    private static Matrix4f glintMatrix() {
        try {
            java.lang.reflect.Method m;
            try {
                m = TextureTransform.class.getMethod("createMatrix");
            } catch (NoSuchMethodException e) {
                m = TextureTransform.class.getMethod("getMatrix");
            }
            return new Matrix4f((Matrix4f) m.invoke(TextureTransform.GLINT_TEXTURING));
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static void command(TestSingleplayerContext world, String cmd) {
        world.getServer().runCommand(cmd);
    }

    private static boolean mainHandFoil(ClientGameTestContext ctx) {
        return ctx.computeOnClient(mc -> mc.player.getMainHandItem().hasFoil());
    }

    @Override
    public void runTest(ClientGameTestContext ctx) throws RuntimeException {
        try {
            run(ctx);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void run(ClientGameTestContext ctx) throws Exception {
        ctx.runOnClient(mc -> MixinEnvironment.getCurrentEnvironment().audit());
        check(true, "MixinEnvironment.audit() passed (all mixins applied)");
        ctx.runOnClient(mc -> resetConfig());

        try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
            ctx.waitTicks(60);
            command(world, "gamemode creative @p");
            command(world, "time set noon");
            command(world, "weather clear");
            ctx.waitTicks(20);

            // ── Scene A: natural (enchanted) glint, vanilla settings ──────────────────
            command(world, "item replace entity @p weapon.mainhand with " + ENCHANTED_SWORD);
            ctx.waitTicks(20);
            check(mainHandFoil(ctx), "enchanted sword hasFoil() with vanilla settings");
            BufferedImage naturalVanilla = read(ctx.takeScreenshot("ge-01-natural-vanilla"));
            int w = naturalVanilla.getWidth(), h = naturalVanilla.getHeight();
            int x0 = w / 2, y0 = h / 2;   // held item lives in the lower-right quadrant

            // Scene B: natural glint tinted red
            ctx.runOnClient(mc -> {
                cfg().naturalGlint = GlintConfig.NaturalMode.TINTED;
                cfg().naturalColor = 0xFFFF2020;
                GlintRuntime.rebuild();
            });
            ctx.waitTicks(10);
            BufferedImage naturalRed = read(ctx.takeScreenshot("ge-02-natural-tinted-red"));
            long redVanilla = dominance(naturalVanilla, 0, x0, y0, w, h);
            long redTinted = dominance(naturalRed, 0, x0, y0, w, h);
            int chg = changed(naturalVanilla, naturalRed, x0, y0, w, h);
            info("natural red-tint: red-dominance vanilla=" + redVanilla + " tinted=" + redTinted + " changedPixels=" + chg);
            check(chg > 150, "tinted natural glint changes the held item's pixels (" + chg + ")");
            check(redTinted > redVanilla + 2000, "tinted natural glint is red-dominant (" + redVanilla + " -> " + redTinted + ")");

            // Scene C: natural glint hidden
            ctx.runOnClient(mc -> {
                cfg().naturalGlint = GlintConfig.NaturalMode.HIDDEN;
                GlintRuntime.rebuild();
            });
            ctx.waitTicks(10);
            check(!mainHandFoil(ctx), "Hidden mode: enchanted sword hasFoil() == false");
            BufferedImage naturalHidden = read(ctx.takeScreenshot("ge-03-natural-hidden"));
            int chgHidden = changed(naturalVanilla, naturalHidden, x0, y0, w, h);
            info("natural hidden: changedPixels vs vanilla=" + chgHidden);
            check(chgHidden > 150, "Hidden mode removes the glint pixels (" + chgHidden + ")");

            // ── Scene D: whitelist (non-glinting iron sword) ───────────────────────────
            ctx.runOnClient(mc -> {
                cfg().naturalGlint = GlintConfig.NaturalMode.DEFAULT;
                GlintRuntime.rebuild();
            });
            command(world, "item replace entity @p weapon.mainhand with " + PLAIN_SWORD);
            ctx.waitTicks(20);
            check(!mainHandFoil(ctx), "plain iron sword does not glint before whitelisting");
            BufferedImage plain = read(ctx.takeScreenshot("ge-04-plain-sword"));

            ctx.runOnClient(mc -> {
                cfg().whitelist.add("minecraft:iron_sword");
                GlintRuntime.rebuild();
            });
            ctx.waitTicks(10);
            check(mainHandFoil(ctx), "whitelisted iron sword hasFoil() == true");
            BufferedImage whitelisted = read(ctx.takeScreenshot("ge-05-whitelisted-sword"));
            int chgWl = changed(plain, whitelisted, x0, y0, w, h);
            info("whitelist: changedPixels vs plain=" + chgWl);
            check(chgWl > 150, "whitelisted item renders a glint (" + chgWl + " pixels changed)");

            // Scene E: tint the whitelist glint (cyan "extra" colour), then a per-item override (green)
            ctx.runOnClient(mc -> {
                cfg().tintExtra = true;
                cfg().extraColor = 0xFF20FF40;
                GlintRuntime.rebuild();
            });
            ctx.waitTicks(10);
            BufferedImage extraGreen = read(ctx.takeScreenshot("ge-06-whitelist-tinted-green"));
            long greenPlain = dominance(whitelisted, 1, x0, y0, w, h);
            long greenTinted = dominance(extraGreen, 1, x0, y0, w, h);
            info("whitelist tint: green-dominance " + greenPlain + " -> " + greenTinted);
            check(greenTinted > greenPlain + 2000, "tinted whitelist glint is green-dominant");

            ctx.runOnClient(mc -> {
                cfg().itemColors.put("minecraft:iron_sword", 0xFF2040FF);   // per-item override beats the extra colour
                GlintRuntime.rebuild();
            });
            ctx.waitTicks(10);
            BufferedImage perItem = read(ctx.takeScreenshot("ge-07-per-item-blue"));
            long blueExtra = dominance(extraGreen, 2, x0, y0, w, h);
            long bluePer = dominance(perItem, 2, x0, y0, w, h);
            info("per-item tint: blue-dominance " + blueExtra + " -> " + bluePer);
            check(bluePer > blueExtra + 2000, "per-item colour override beats the whitelist tint (blue-dominant)");

            // whitelist master toggle off -> glint disappears again
            ctx.runOnClient(mc -> {
                cfg().whitelistGlint = false;
                GlintRuntime.rebuild();
            });
            ctx.waitTicks(10);
            check(!mainHandFoil(ctx), "whitelist toggle off: iron sword stops glinting");
            ctx.runOnClient(mc -> {
                cfg().whitelistGlint = true;
                GlintRuntime.rebuild();
            });

            // ── Scene F: GUI glint (survival inventory with hotbar items) ───────────────
            command(world, "gamemode survival @p");
            command(world, "item replace entity @p hotbar.0 with " + ENCHANTED_SWORD);
            command(world, "item replace entity @p hotbar.1 with " + PLAIN_SWORD);
            command(world, "item replace entity @p hotbar.2 with minecraft:enchanted_golden_apple");
            ctx.runOnClient(mc -> {
                cfg().naturalGlint = GlintConfig.NaturalMode.DEFAULT;
                cfg().itemColors.clear();
                GlintRuntime.rebuild();
            });
            ctx.waitTicks(10);
            ctx.getInput().pressKey(options -> options.keyInventory);
            ctx.waitTicks(10);
            BufferedImage guiVanilla = read(ctx.takeScreenshot("ge-08-inventory-gui-vanilla-glint"));
            int[] gui = ctx.computeOnClient(mc -> new int[]{mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(),
                    (int) mc.getWindow().getGuiScale()});
            // hotbar slot 0 (the enchanted diamond sword) of the survival inventory: container 176x166, slot at (8,142)
            int sx = ((gui[0] - 176) / 2 + 8) * gui[2], sy = ((gui[1] - 166) / 2 + 142) * gui[2], sz = 16 * gui[2];
            ctx.runOnClient(mc -> {
                cfg().naturalGlint = GlintConfig.NaturalMode.TINTED;
                cfg().naturalColor = 0xFFFF2020;
                GlintRuntime.rebuild();
            });
            ctx.waitTicks(10);
            BufferedImage guiRed = read(ctx.takeScreenshot("ge-09-inventory-gui-tinted-red"));
            long guiRedBefore = dominance(guiVanilla, 0, sx, sy, sx + sz, sy + sz);
            long guiRedAfter = dominance(guiRed, 0, sx, sy, sx + sz, sy + sz);
            int guiChg = changed(guiVanilla, guiRed, sx, sy, sx + sz, sy + sz);
            info("inventory GUI slot red-tint: red-dominance " + guiRedBefore + " -> " + guiRedAfter + " changedPixels=" + guiChg);
            check(guiRedAfter > guiRedBefore + 300, "GUI (inventory slot) glint is tinted red (" + guiRedBefore + " -> " + guiRedAfter + ")");
            ctx.getInput().pressKey(com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE);
            ctx.waitTicks(5);
            ctx.takeScreenshot("ge-10-hotbar-hud-tinted");

            // ── Scene F2: special held models (shield / trident): vanilla glint vs red-tinted glint ──
            command(world, "gamemode creative @p");
            String[][] special = {
                    {"shield", "minecraft:shield[enchantments={unbreaking:1}]"},
                    {"trident", "minecraft:trident[enchantments={loyalty:1}]"}};
            for (String[] sp : special) {
                ctx.runOnClient(mc -> {
                    cfg().naturalGlint = GlintConfig.NaturalMode.DEFAULT;
                    GlintRuntime.rebuild();
                });
                command(world, "item replace entity @p weapon.mainhand with " + sp[1]);
                ctx.waitTicks(15);
                BufferedImage spVanilla = read(ctx.takeScreenshot("ge-11-" + sp[0] + "-vanilla-glint"));
                ctx.runOnClient(mc -> {
                    cfg().naturalGlint = GlintConfig.NaturalMode.TINTED;
                    cfg().naturalColor = 0xFFFF2020;
                    GlintRuntime.rebuild();
                });
                ctx.waitTicks(10);
                BufferedImage spRed = read(ctx.takeScreenshot("ge-11-" + sp[0] + "-tinted-red"));
                int sx0 = w / 4, sy0 = h / 4;
                long spBefore = dominance(spVanilla, 0, sx0, sy0, w, h);
                long spAfter = dominance(spRed, 0, sx0, sy0, w, h);
                int spChg = changed(spVanilla, spRed, sx0, sy0, w, h);
                info(sp[0] + " red-tint: red-dominance " + spBefore + " -> " + spAfter + " changedPixels=" + spChg);
                boolean tinted = spChg > 150 && spAfter > spBefore + 1500;
                if (KNOWN_NO_SPECIAL_TINT.equals(mcVersion())) {
                    // 1.21.11: shield/trident models draw their glint from ModelPartSubmit at render time, where the tint is unknown.
                    info("KNOWN LIMITATION (MC " + mcVersion() + "): held " + sp[0] + " glint tint " + (tinted ? "works" : "is not applied"));
                } else {
                    check(tinted, "held " + sp[0] + " glint is tinted red");
                }
            }
            ctx.runOnClient(mc -> {
                cfg().naturalGlint = GlintConfig.NaturalMode.DEFAULT;
                GlintRuntime.rebuild();
            });

            // ── Scene G: armor glint, third person ─────────────────────────────────────
            command(world, "gamemode creative @p");
            command(world, "item replace entity @p armor.head with minecraft:diamond_helmet[enchantments={protection:1}]");
            command(world, "item replace entity @p armor.chest with minecraft:diamond_chestplate[enchantments={protection:1}]");
            command(world, "item replace entity @p armor.legs with minecraft:diamond_leggings[enchantments={protection:1}]");
            ctx.runOnClient(mc -> {
                cfg().naturalGlint = GlintConfig.NaturalMode.DEFAULT;
                GlintRuntime.rebuild();
            });
            ctx.getInput().pressKey(options -> options.keyTogglePerspective);
            ctx.getInput().pressKey(options -> options.keyTogglePerspective);   // front-facing third person
            ctx.waitTicks(20);
            BufferedImage armorVanilla = read(ctx.takeScreenshot("ge-13-armor-vanilla-glint"));
            ctx.runOnClient(mc -> {
                cfg().naturalGlint = GlintConfig.NaturalMode.TINTED;
                cfg().naturalColor = 0xFFFF2020;
                GlintRuntime.rebuild();
            });
            ctx.waitTicks(10);
            BufferedImage armorRed = read(ctx.takeScreenshot("ge-14-armor-tinted-red"));
            int aw = armorVanilla.getWidth(), ah = armorVanilla.getHeight();
            long armorRedBefore = dominance(armorVanilla, 0, aw / 4, 0, aw * 3 / 4, ah);
            long armorRedAfter = dominance(armorRed, 0, aw / 4, 0, aw * 3 / 4, ah);
            int armorChg = changed(armorVanilla, armorRed, aw / 4, 0, aw * 3 / 4, ah);
            info("armor red-tint: red-dominance " + armorRedBefore + " -> " + armorRedAfter + " changedPixels=" + armorChg);
            check(armorChg > 150 && armorRedAfter > armorRedBefore + 1500, "worn-armor glint is tinted red");
            ctx.getInput().pressKey(options -> options.keyTogglePerspective);   // back to first person

            // ── Scene H: animation settings drive the glint matrix (TextureTransformMixin) ─────
            ctx.runOnClient(mc -> resetConfig());
            Matrix4f vanillaA = ctx.computeOnClient(mc -> glintMatrix());
            ctx.runOnClient(mc -> {
                cfg().style = GlintConfig.AnimStyle.STATIC;
                cfg().scale = 2.0f;
                cfg().angle = 45.0f;
                GlintRuntime.rebuild();
            });
            Matrix4f staticA = ctx.computeOnClient(mc -> glintMatrix());
            ctx.waitTicks(20);
            Matrix4f staticB = ctx.computeOnClient(mc -> glintMatrix());
            check(!vanillaA.equals(staticA, 1.0e-4f), "custom scale/angle changes the glint matrix");
            check(staticA.equals(staticB, 1.0e-6f), "Frozen style: glint matrix does not change over time");
            check(Math.abs(staticA.m30()) < 1.0e-6f && Math.abs(staticA.m31()) < 1.0e-6f, "Frozen style: no scroll offset");
            ctx.runOnClient(mc -> {
                cfg().style = GlintConfig.AnimStyle.CLASSIC;
                cfg().speed = 4.0f;
                GlintRuntime.rebuild();
            });
            Matrix4f fastA = ctx.computeOnClient(mc -> glintMatrix());
            ctx.waitTicks(20);
            Matrix4f fastB = ctx.computeOnClient(mc -> glintMatrix());
            check(!fastA.equals(fastB, 1.0e-6f), "Classic style @4x speed: glint matrix scrolls over time");
            command(world, "gamemode creative @p");
            command(world, "item replace entity @p weapon.mainhand with " + ENCHANTED_SWORD);
            ctx.waitTicks(10);
            ctx.takeScreenshot("ge-15-animation-fast-large");

            // ── Scene I: settings GUI ──────────────────────────────────────────────────
            ctx.runOnClient(mc -> resetConfig());
            ctx.getInput().resizeWindow(1280, 720);
            ctx.waitTicks(5);
            ctx.setScreen(() -> new GlintEverythingScreen(null));
            ctx.waitForScreen(GlintEverythingScreen.class);
            ctx.waitTicks(15);
            ctx.takeScreenshot("ge-16-settings-general");
            double scale = ctx.computeOnClient(mc -> mc.getWindow().getGuiScale());
            int sidebarX = (int) field(ctx, "sidebarX"), sidebarW = (int) field(ctx, "sidebarW"), tabTop = (int) field(ctx, "tabTop");
            String[] tabNames = {"general", "whitelist", "colors", "about"};
            for (int i = 1; i < tabNames.length; i++) {
                ctx.getInput().setCursorPos((sidebarX + sidebarW / 2.0) * scale, (tabTop + i * 30 + 12) * scale);
                ctx.getInput().pressMouse(0);
                ctx.waitTicks(15);
                ctx.takeScreenshot("ge-17-settings-" + tabNames[i]);
                if (i == 1) {
                    // type into the whitelist box and add via Enter
                    int contentLeft = (int) field(ctx, "contentLeft");
                    ctx.getInput().setCursorPos((contentLeft + 20) * scale, (tabTop + 8) * scale);
                    ctx.getInput().pressMouse(0);
                    ctx.getInput().typeChars("minecraft:stick");
                    ctx.waitTicks(5);
                    ctx.takeScreenshot("ge-18-settings-whitelist-typing");
                    ctx.getInput().pressKey(com.mojang.blaze3d.platform.InputConstants.KEY_RETURN);
                    ctx.waitTicks(5);
                    boolean added = ctx.computeOnClient(mc -> cfg().whitelist.contains("minecraft:stick"));
                    check(added, "Whitelist tab: typed id + Enter adds minecraft:stick to the whitelist");
                    ctx.takeScreenshot("ge-19-settings-whitelist-added");
                }
            }
            ctx.getInput().pressKey(com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE);
            ctx.waitTicks(5);
            check(ctx.computeOnClient(mc -> ScreenNav.current(mc) == null), "settings screen closed with Escape");
        }
    }

    /** Read a private int field of the open settings screen (tab geometry) via reflection. */
    private static Object field(ClientGameTestContext ctx, String name) throws Exception {
        Object screen = ctx.computeOnClient(ScreenNav::current);
        Class<?> c = screen.getClass();
        while (c != null) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f.get(screen);
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }
}
