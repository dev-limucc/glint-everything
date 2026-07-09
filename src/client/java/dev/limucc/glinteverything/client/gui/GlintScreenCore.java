package dev.limucc.glinteverything.client.gui;

import dev.limucc.glinteverything.client.compat.Gfx;
import dev.limucc.glinteverything.client.compat.ScreenNav;
import dev.limucc.glinteverything.client.config.GlintConfig;
import dev.limucc.glinteverything.client.glint.GlintRuntime;
import dev.limucc.glinteverything.client.gui.anim.Easing;
import dev.limucc.glinteverything.client.gui.anim.Tween;
import dev.limucc.glinteverything.client.gui.widget.FlatButton;
import dev.limucc.glinteverything.client.gui.widget.FlatSlider;
import dev.limucc.glinteverything.client.gui.widget.FlatTextField;
import dev.limucc.glinteverything.client.gui.widget.ToggleSwitch;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * The whole settings screen in the Animated Limucc UI style: flat dark panel, sidebar tabs with a
 * gliding accent highlight, content that slides in on tab change. Per-version subclasses only
 * bridge the era's render entry point; every widget here is hand-drawn and hand-hit-tested.
 */
public abstract class GlintScreenCore extends Screen {

    private static final int PANEL = 0xEE15161B, EDGE = 0x22FFFFFF, SIDEBAR = 0x40000000;
    private static final int ACCENT = 0xFF3A6EA5, ACCENT_FAINT = 0x553A6EA5, HOVER_WASH = 0x18FFFFFF;
    private static final int TXT = 0xFFE6E6EA, BRIGHT = 0xFFFFFFFF, DIM = 0xFF74747C;
    private static final int HEAD = 0xFFB0B0B8, HINT = 0xFF606068, VALUE = 0xFFFFFF80;

    private static final String[] TABS = {"General", "Whitelist", "Colors", "About"};

    protected final Screen parent;

    private int panelLeft, panelRight, panelTop, panelBottom, sidebarX, sidebarW, contentLeft, contentRight, tabTop;
    private int tab = 0;
    private long tabSwitchMs = -100000L;
    private final Tween tabHighlight = new Tween();
    private boolean tabHighlightInit;

    // ── General ──
    private final ToggleSwitch tglEnabled = new ToggleSwitch();
    private final ToggleSwitch tglWhitelist = new ToggleSwitch();
    private final FlatSlider slSpeed = new FlatSlider(0.0f, 5.0f, 1.0f);
    private final FlatSlider slScale = new FlatSlider(0.25f, 4.0f, 1.0f);
    private final FlatSlider slAngle = new FlatSlider(0.0f, 90.0f, 10.0f);
    private final FlatButton btnStyle = new FlatButton(0, 0, 90, 16, "");
    private final FlatButton btnResetAnim = new FlatButton(0, 0, 90, 16, "Reset");

    // ── Whitelist ──
    private final FlatTextField fldAdd = new FlatTextField();
    private final FlatButton btnAdd = new FlatButton(0, 0, 62, 16, "Add");
    private final List<String> suggestions = new ArrayList<>();
    private String lastQuery = null;
    private final Tween listScroll = new Tween(0);
    private float listScrollTarget = 0;
    private String selected = null;
    private final ColorEditor perItemEditor = new ColorEditor(
            () -> selectedOverride(),
            v -> { if (selected != null) { GlintRuntime.config().itemColors.put(selected, v); GlintRuntime.rebuild(); } });
    private final FlatButton btnClearOverride = new FlatButton(0, 0, 56, 16, "Default");
    private long addErrorMs = -100000L;

    // ── Colors ──
    private final ToggleSwitch tglTintExtra = new ToggleSwitch();
    private final FlatButton btnNaturalMode = new FlatButton(0, 0, 90, 16, "");
    private final ColorEditor extraEditor = new ColorEditor(
            () -> GlintRuntime.config().extraColor,
            v -> { GlintRuntime.config().extraColor = v; GlintRuntime.rebuild(); });
    private final ColorEditor naturalEditor = new ColorEditor(
            () -> GlintRuntime.config().naturalColor,
            v -> { GlintRuntime.config().naturalColor = v; GlintRuntime.rebuild(); });

    private final FlatButton btnDone = new FlatButton(0, 0, 62, 18, "Done");

    protected GlintScreenCore(Screen parent) {
        super(Component.literal("Glint Everything"));
        this.parent = parent;
    }

    private GlintConfig cfg() { return GlintRuntime.config(); }

    private int selectedOverride() {
        Integer v = selected == null ? null : cfg().itemColors.get(selected);
        return v != null ? v : 0xFFFFFFFF;
    }

    // ─────────────────────────────────────────── layout ───────────────────────────────────────────

    @Override
    protected void init() {
        int cx = width / 2, cy = height / 2;
        panelLeft = cx - 260; panelRight = cx + 260;
        panelTop = Math.max(8, cy - 150); panelBottom = Math.min(height - 8, cy + 150);
        sidebarX = panelLeft + 12; sidebarW = 92;
        contentLeft = panelLeft + 118; contentRight = panelRight - 16;
        tabTop = panelTop + 40;

        tglEnabled.snapTo(cfg().enabled);
        tglWhitelist.snapTo(cfg().whitelistGlint);
        tglTintExtra.snapTo(cfg().tintExtra);
        slSpeed.value = cfg().speed;
        slScale.value = cfg().scale;
        slAngle.value = cfg().angle;

        btnDone.setBounds(panelRight - 74, panelBottom - 26, 62, 18);
    }

    // ─────────────────────────────────────────── render ───────────────────────────────────────────

    protected void renderCore(Gfx g, int mouseX, int mouseY) {
        long now = Util.getMillis();

        g.fill(panelLeft, panelTop, panelRight, panelBottom, PANEL);
        g.fill(panelLeft, panelTop, panelRight, panelTop + 1, EDGE);
        g.fill(sidebarX - 4, tabTop - 6, sidebarX + sidebarW + 4, panelBottom - 8, SIDEBAR);

        g.text(font, "Glint Everything", panelLeft + 12, panelTop + 12, BRIGHT);
        g.text(font, "§8by Limucc", panelRight - 12 - font.width("by Limucc"), panelTop + 12, HINT);

        // sidebar: one accent bar gliding to the selected tab
        int selectedY = tabTop + tab * 30;
        if (!tabHighlightInit) { tabHighlight.snap(selectedY); tabHighlightInit = true; }
        tabHighlight.retarget(selectedY, now, 220, Easing.EASE_OUT);
        int hy = Math.round(tabHighlight.update(now));
        g.fill(sidebarX - 4, hy, sidebarX - 2, hy + 24, ACCENT);
        g.fill(sidebarX, hy, sidebarX + sidebarW, hy + 24, ACCENT_FAINT);
        for (int i = 0; i < TABS.length; i++) {
            int ty = tabTop + i * 30;
            boolean hov = mouseX >= sidebarX && mouseX < sidebarX + sidebarW && mouseY >= ty && mouseY < ty + 24;
            if (hov && i != tab) g.fill(sidebarX, ty, sidebarX + sidebarW, ty + 24, HOVER_WASH);
            g.text(font, TABS[i], sidebarX + 8, ty + 8, i == tab ? BRIGHT : (hov ? TXT : DIM));
        }

        // content slides in 22px from the left over 200ms on each tab change
        float t = (now - tabSwitchMs) / 200.0f;
        float slide = (t >= 0 && t < 1) ? (1.0f - Easing.EASE_OUT.clampApply(t)) * 22.0f : 0.0f;
        g.enableScissor(contentLeft - 4, tabTop - 6, panelRight - 8, panelBottom - 8);
        g.pose().pushMatrix();
        g.pose().translate(slide, 0.0f);
        switch (tab) {
            case 0 -> drawGeneral(g, mouseX, mouseY);
            case 1 -> drawWhitelist(g, mouseX, mouseY, now);
            case 2 -> drawColors(g, mouseX, mouseY);
            default -> drawAbout(g);
        }
        g.pose().popMatrix();
        g.disableScissor();

        if (tab == 1) drawSuggestions(g, mouseX, mouseY);
        btnDone.render(g, font, mouseX, mouseY, true);
    }

    private void drawGeneral(Gfx g, int mouseX, int mouseY) {
        boolean on = cfg().enabled;
        int y = tabTop;

        g.text(font, "Master switch", contentLeft, y + 2, TXT);
        tglEnabled.setPosition(contentRight - 24, y);
        tglEnabled.render(g, cfg().enabled, true);
        y += 26;

        g.text(font, "Whitelist glint", contentLeft, y + 2, on ? TXT : DIM);
        tglWhitelist.setPosition(contentRight - 24, y);
        tglWhitelist.render(g, cfg().whitelistGlint, on);
        y += 26;

        g.text(font, "§7Animation", contentLeft, y + 2, HEAD);
        y += 16;

        y = sliderRow(g, mouseX, mouseY, y, "Speed", slSpeed, String.format("%.2f×", slSpeed.value), on);
        y = sliderRow(g, mouseX, mouseY, y, "Size", slScale, String.format("%.2f×", slScale.value), on);
        y = sliderRow(g, mouseX, mouseY, y, "Angle", slAngle, String.format("%.0f°", slAngle.value), on);

        g.text(font, "Motion", contentLeft, y + 4, on ? TXT : DIM);
        btnStyle.label = cfg().style.label();
        btnStyle.setBounds(contentRight - 90, y, 90, 16);
        btnStyle.render(g, font, mouseX, mouseY, on);
        y += 26;

        g.text(font, "§8Back to vanilla motion", contentLeft, y + 4, HINT);
        btnResetAnim.setBounds(contentRight - 90, y, 90, 16);
        btnResetAnim.render(g, font, mouseX, mouseY, on);
    }

    private int sliderRow(Gfx g, int mouseX, int mouseY, int y, String label, FlatSlider s, String value, boolean enabled) {
        g.text(font, label, contentLeft, y + 2, enabled ? TXT : DIM);
        g.text(font, value, contentRight - 140 - 8 - font.width(value), y + 2, enabled ? VALUE : DIM);
        s.setBounds(contentRight - 140, y, 140);
        s.render(g, mouseX, mouseY, enabled);
        return y + 22;
    }

    private void drawWhitelist(Gfx g, int mouseX, int mouseY, long now) {
        boolean on = cfg().enabled;

        fldAdd.setBounds(contentLeft, tabTop, contentRight - contentLeft - 70);
        fldAdd.hint = "item id, e.g. minecraft:stick";
        fldAdd.render(g, font, mouseX, mouseY);
        if (now - addErrorMs < 900) g.fill(fldAdd.x, fldAdd.y + fldAdd.h - 1, fldAdd.x + fldAdd.w, fldAdd.y + fldAdd.h, 0xFFFF4B57);
        btnAdd.setBounds(contentRight - 62, tabTop, 62, 16);
        btnAdd.render(g, font, mouseX, mouseY, on);

        List<String> list = cfg().whitelist;
        int listTop = tabTop + 24;
        int listBottom = listBottom();
        int viewH = listBottom - listTop;
        float maxScroll = Math.max(0, list.size() * 20 - viewH);
        listScrollTarget = Math.max(0, Math.min(listScrollTarget, maxScroll));
        listScroll.retarget(listScrollTarget, now, 180, Easing.EASE_OUT);
        int scroll = Math.round(listScroll.update(now));

        if (list.isEmpty()) {
            g.text(font, "§8Nothing whitelisted yet — add an item above.", contentLeft, listTop + 8, HINT);
        }

        g.enableScissor(contentLeft - 4, listTop, panelRight - 8, listBottom);
        for (int i = 0; i < list.size(); i++) {
            int ry = listTop + i * 20 - scroll;
            if (ry + 20 < listTop || ry > listBottom) continue;
            String id = list.get(i);
            boolean sel = id.equals(selected);
            boolean hov = mouseX >= contentLeft && mouseX < contentRight && mouseY >= ry && mouseY < ry + 20
                    && mouseY >= listTop && mouseY < listBottom;
            if (sel) {
                g.fill(contentLeft - 4, ry, contentLeft - 2, ry + 20, ACCENT);
                g.fill(contentLeft - 2, ry, contentRight, ry + 20, ACCENT_FAINT);
            } else if (hov) {
                g.fill(contentLeft - 2, ry, contentRight, ry + 20, HOVER_WASH);
            }
            ItemStack icon = iconFor(id);
            if (icon != null) g.item(icon, contentLeft + 2, ry + 2);
            g.text(font, trimmed(id, contentRight - contentLeft - 64), contentLeft + 24, ry + 6, sel ? BRIGHT : TXT);

            Integer override = cfg().itemColors.get(id);
            int swx = contentRight - 36;
            if (override != null) {
                g.fill(swx, ry + 4, swx + 12, ry + 16, override);
                g.fill(swx, ry + 4, swx + 12, ry + 5, 0x22FFFFFF);
            } else {
                g.text(font, "—", swx + 2, ry + 6, HINT);
            }

            boolean hovX = mouseX >= contentRight - 16 && mouseX < contentRight - 4 && mouseY >= ry + 4 && mouseY < ry + 16;
            g.text(font, "✕", contentRight - 14, ry + 6, hovX ? 0xFFFF4B57 : DIM);
        }
        g.disableScissor();

        if (selected != null) {
            int ey = listBottom + 8;
            g.text(font, "§7Tint for §f" + trimmed(selected, contentRight - contentLeft - 70), contentLeft, ey, HEAD);
            btnClearOverride.setBounds(contentRight - 56, ey - 3, 56, 16);
            btnClearOverride.render(g, font, mouseX, mouseY, cfg().itemColors.containsKey(selected));
            perItemEditor.setBounds(contentLeft + 8, ey + 14, contentRight - contentLeft - 16);
            perItemEditor.render(g, font, mouseX, mouseY, on);
        }
    }

    private void drawSuggestions(Gfx g, int mouseX, int mouseY) {
        if (!fldAdd.focused || suggestions.isEmpty()) return;
        int sy = tabTop + 17;
        int sw = fldAdd.w;
        g.fill(contentLeft, sy, contentLeft + sw, sy + suggestions.size() * 13 + 2, 0xF0121214);
        for (int i = 0; i < suggestions.size(); i++) {
            int ry = sy + 1 + i * 13;
            boolean hov = mouseX >= contentLeft && mouseX < contentLeft + sw && mouseY >= ry && mouseY < ry + 13;
            if (hov) g.fill(contentLeft, ry, contentLeft + sw, ry + 13, ACCENT_FAINT);
            g.text(font, trimmed(suggestions.get(i), sw - 8), contentLeft + 4, ry + 3, hov ? BRIGHT : TXT);
        }
    }

    private void drawColors(Gfx g, int mouseX, int mouseY) {
        boolean on = cfg().enabled;
        int y = tabTop;

        g.text(font, "§7Whitelisted items", contentLeft, y, HEAD);
        y += 14;
        g.text(font, "Tint their glint", contentLeft, y + 2, on ? TXT : DIM);
        tglTintExtra.setPosition(contentRight - 24, y);
        tglTintExtra.render(g, cfg().tintExtra, on);
        y += 20;
        extraEditor.setBounds(contentLeft + 8, y, contentRight - contentLeft - 16);
        extraEditor.render(g, font, mouseX, mouseY, on && cfg().tintExtra);
        y += ColorEditor.HEIGHT + 14;

        g.text(font, "§7Natural glint (enchanted + other mods)", contentLeft, y, HEAD);
        y += 14;
        g.text(font, "Behavior", contentLeft, y + 2, on ? TXT : DIM);
        btnNaturalMode.label = cfg().naturalGlint.label();
        btnNaturalMode.setBounds(contentRight - 90, y - 1, 90, 16);
        btnNaturalMode.render(g, font, mouseX, mouseY, on);
        y += 20;
        naturalEditor.setBounds(contentLeft + 8, y, contentRight - contentLeft - 16);
        naturalEditor.render(g, font, mouseX, mouseY, on && cfg().naturalGlint == GlintConfig.NaturalMode.TINTED);
        y += ColorEditor.HEIGHT + 6;
        g.text(font, "§8Per-item tints live in the Whitelist tab — select a row.", contentLeft, y + 4, HINT);
    }

    private void drawAbout(Gfx g) {
        int y = tabTop;
        g.text(font, "§fGlint Everything", contentLeft, y, BRIGHT); y += 14;
        g.text(font, "§7Give any item the enchantment shimmer.", contentLeft, y, HEAD); y += 20;
        g.text(font, "§8• Whitelist items to make them glint", contentLeft, y, HINT); y += 12;
        g.text(font, "§8• Recolor the glint globally or per item", contentLeft, y, HINT); y += 12;
        g.text(font, "§8• Restyle speed, size, angle and motion", contentLeft, y, HINT); y += 12;
        g.text(font, "§8• Tame or hide vanilla & modded glint", contentLeft, y, HINT); y += 20;
        g.text(font, "§7Settings save on close. Glint strength & vanilla", contentLeft, y, HEAD); y += 12;
        g.text(font, "§7speed also honor Accessibility options.", contentLeft, y, HEAD); y += 20;
        g.text(font, "§8MIT © dev-limucc", contentLeft, y, HINT);
    }

    private final java.util.Map<String, ItemStack> iconCache = new java.util.HashMap<>();

    /** Icon stack or null. At the title screen item components aren't bound yet and ItemStack construction throws. */
    private ItemStack iconFor(String id) {
        if (iconCache.containsKey(id)) return iconCache.get(id);
        ItemStack stack = null;
        try {
            Item item = GlintRuntime.byId(id);
            if (item != null) stack = new ItemStack(item);
        } catch (Exception ignored) {}
        iconCache.put(id, stack);
        return stack;
    }

    private int listBottom() {
        return panelBottom - 12 - (selected != null ? 62 : 0);
    }

    private String trimmed(String s, int width) {
        if (font.width(s) <= width) return s;
        String out = s;
        while (!out.isEmpty() && font.width(out + "…") > width) out = out.substring(0, out.length() - 1);
        return out + "…";
    }

    // ─────────────────────────────────────────── input ───────────────────────────────────────────

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) return true;
        if (event.button() != 0) return false;
        return clickCore(event.x(), event.y());
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (super.mouseDragged(event, dx, dy)) return true;
        return dragCore(event.x(), event.y());
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        slSpeed.release(); slScale.release(); slAngle.release();
        perItemEditor.release(); extraEditor.release(); naturalEditor.release();
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (tab == 1 && mx >= contentLeft - 4 && mx < panelRight && my >= tabTop + 24 && my < listBottom()) {
            listScrollTarget -= (float) sy * 40;
            return true;
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (fldAdd.focused) {
            if (key == 257 || key == 335) { addCurrent(); return true; }   // enter
            if (fldAdd.keyPressed(key)) return true;
        }
        if (perItemEditor.hexFocused() && perItemEditor.keyPressed(key)) return true;
        if (extraEditor.hexFocused() && extraEditor.keyPressed(key)) return true;
        if (naturalEditor.hexFocused() && naturalEditor.keyPressed(key)) return true;
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        char c = (char) event.codepoint();
        if (fldAdd.focused) { fldAdd.charTyped(c); refreshSuggestions(); return true; }
        if (perItemEditor.hexFocused() && perItemEditor.charTyped(c)) return true;
        if (extraEditor.hexFocused() && extraEditor.charTyped(c)) return true;
        if (naturalEditor.hexFocused() && naturalEditor.charTyped(c)) return true;
        return super.charTyped(event);
    }

    private boolean clickCore(double mx, double my) {
        if (btnDone.contains(mx, my)) { onClose(); return true; }

        // sidebar tabs
        for (int i = 0; i < TABS.length; i++) {
            int ty = tabTop + i * 30;
            if (mx >= sidebarX && mx < sidebarX + sidebarW && my >= ty && my < ty + 24) {
                selectTab(i);
                return true;
            }
        }

        boolean on = cfg().enabled;
        switch (tab) {
            case 0 -> { return clickGeneral(mx, my, on); }
            case 1 -> { return clickWhitelist(mx, my, on); }
            case 2 -> { return clickColors(mx, my, on); }
        }
        return false;
    }

    private boolean clickGeneral(double mx, double my, boolean on) {
        if (tglEnabled.contains(mx, my)) { cfg().enabled = !cfg().enabled; return true; }
        if (!on) return false;
        if (tglWhitelist.contains(mx, my)) { cfg().whitelistGlint = !cfg().whitelistGlint; return true; }
        if (btnStyle.contains(mx, my)) { cfg().style = cfg().style.next(); return true; }
        if (btnResetAnim.contains(mx, my)) {
            cfg().speed = 1.0f; cfg().scale = 1.0f; cfg().angle = 10.0f; cfg().style = GlintConfig.AnimStyle.CLASSIC;
            slSpeed.value = 1.0f; slScale.value = 1.0f; slAngle.value = 10.0f;
            return true;
        }
        if (slSpeed.click(mx, my, true)) { cfg().speed = slSpeed.value; return true; }
        if (slScale.click(mx, my, true)) { cfg().scale = slScale.value; return true; }
        if (slAngle.click(mx, my, true)) { cfg().angle = slAngle.value; return true; }
        return false;
    }

    private boolean clickWhitelist(double mx, double my, boolean on) {
        // suggestion popup first — it floats above the list
        if (fldAdd.focused && !suggestions.isEmpty()) {
            int sy = tabTop + 17;
            for (int i = 0; i < suggestions.size(); i++) {
                int ry = sy + 1 + i * 13;
                if (mx >= contentLeft && mx < contentLeft + fldAdd.w && my >= ry && my < ry + 13) {
                    fldAdd.setText(suggestions.get(i));
                    refreshSuggestions();
                    return true;
                }
            }
        }

        fldAdd.focused = fldAdd.contains(mx, my);
        if (fldAdd.focused) { refreshSuggestions(); return true; }
        if (btnAdd.contains(mx, my) && on) { addCurrent(); return true; }

        if (selected != null) {
            if (btnClearOverride.contains(mx, my)) {
                cfg().itemColors.remove(selected);
                GlintRuntime.rebuild();
                return true;
            }
            if (perItemEditor.click(mx, my, on)) return true;
        }

        int listTop = tabTop + 24;
        int listBottom = listBottom();
        if (mx >= contentLeft - 4 && mx < contentRight && my >= listTop && my < listBottom) {
            int scroll = Math.round(listScroll.current());
            int idx = (int) ((my - listTop + scroll) / 20);
            if (idx >= 0 && idx < cfg().whitelist.size()) {
                String id = cfg().whitelist.get(idx);
                if (mx >= contentRight - 16 && mx < contentRight - 4) {
                    cfg().whitelist.remove(idx);
                    cfg().itemColors.remove(id);
                    if (id.equals(selected)) selected = null;
                    GlintRuntime.rebuild();
                } else {
                    selected = id.equals(selected) ? null : id;
                }
                return true;
            }
        }
        return false;
    }

    private boolean clickColors(double mx, double my, boolean on) {
        extraEditor.click(-1, -1, false);   // drop hex focus unless re-hit below
        naturalEditor.click(-1, -1, false);
        if (!on) return false;
        if (tglTintExtra.contains(mx, my)) { cfg().tintExtra = !cfg().tintExtra; GlintRuntime.rebuild(); return true; }
        if (btnNaturalMode.contains(mx, my)) { cfg().naturalGlint = cfg().naturalGlint.next(); GlintRuntime.rebuild(); return true; }
        if (cfg().tintExtra && extraEditor.click(mx, my, true)) return true;
        if (cfg().naturalGlint == GlintConfig.NaturalMode.TINTED && naturalEditor.click(mx, my, true)) return true;
        return false;
    }

    private boolean dragCore(double mx, double my) {
        boolean any = false;
        if (slSpeed.dragging) { slSpeed.drag(mx); cfg().speed = slSpeed.value; any = true; }
        if (slScale.dragging) { slScale.drag(mx); cfg().scale = slScale.value; any = true; }
        if (slAngle.dragging) { slAngle.drag(mx); cfg().angle = slAngle.value; any = true; }
        if (perItemEditor.dragging()) { perItemEditor.drag(mx); any = true; }
        if (extraEditor.dragging()) { extraEditor.drag(mx); any = true; }
        if (naturalEditor.dragging()) { naturalEditor.drag(mx); any = true; }
        return any;
    }

    private void selectTab(int t) {
        if (t == tab) return;
        tab = t;
        tabSwitchMs = Util.getMillis();
        fldAdd.focused = false;
    }

    private void addCurrent() {
        String canonical = GlintRuntime.canonicalId(fldAdd.text);
        if (canonical == null) {
            addErrorMs = Util.getMillis();
            return;
        }
        if (!cfg().whitelist.contains(canonical)) {
            cfg().whitelist.add(canonical);
            GlintRuntime.rebuild();
        }
        fldAdd.setText("");
        refreshSuggestions();
    }

    private void refreshSuggestions() {
        String q = fldAdd.text.trim().toLowerCase();
        if (q.equals(lastQuery)) return;
        lastQuery = q;
        suggestions.clear();
        if (q.isEmpty()) return;
        List<String> starts = new ArrayList<>();
        List<String> contains = new ArrayList<>();
        for (String id : GlintRuntime.allItemIds()) {
            if (cfg().whitelist.contains(id)) continue;
            String path = id.substring(id.indexOf(':') + 1);
            if (path.startsWith(q) || id.startsWith(q)) starts.add(id);
            else if (path.contains(q)) contains.add(id);
            if (starts.size() >= 5) break;
        }
        suggestions.addAll(starts);
        for (String id : contains) {
            if (suggestions.size() >= 5) break;
            suggestions.add(id);
        }
    }

    @Override
    public void onClose() {
        GlintRuntime.saveAndApply();
        ScreenNav.open(this.minecraft, parent);
    }
}
