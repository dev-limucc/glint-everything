package dev.limucc.glinteverything.client.glint;

import dev.limucc.glinteverything.client.config.GlintConfig;
import dev.limucc.glinteverything.client.config.GlintConfigIO;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The live view of the config: item ids resolved to {@link Item} instances once, then every
 * per-frame question ({@code force? hide? what color?}) is an identity lookup.
 */
public final class GlintRuntime {

    private static GlintConfig config = new GlintConfig();
    private static Set<Item> whitelisted = new HashSet<>();
    private static Map<Item, Integer> itemColors = new HashMap<>();

    private GlintRuntime() {}

    public static GlintConfig config() { return config; }

    public static void load() { apply(GlintConfigIO.load()); }

    public static void saveAndApply() {
        GlintConfigIO.save(config);
        rebuild();
    }

    public static void apply(GlintConfig cfg) {
        config = cfg;
        rebuild();
    }

    /** Recompile the id -> Item lookups (call after any whitelist/color edit). */
    public static void rebuild() {
        Set<Item> wl = new HashSet<>();
        for (String id : config.whitelist) {
            Item item = byId(id);
            if (item != null) wl.add(item);
        }
        Map<Item, Integer> colors = new HashMap<>();
        for (Map.Entry<String, Integer> e : config.itemColors.entrySet()) {
            Item item = byId(e.getKey());
            if (item != null && e.getValue() != null) colors.put(item, e.getValue());
        }
        whitelisted = wl;
        itemColors = colors;
    }

    /** The whitelist wants this non-glinting item to glint. */
    public static boolean forceFoil(ItemStack stack) {
        return config.enabled && config.whitelistGlint && whitelisted.contains(stack.getItem());
    }

    /** Natural glint is set to Hidden and nothing else claims this item. */
    public static boolean suppressFoil(ItemStack stack) {
        if (!config.enabled || config.naturalGlint != GlintConfig.NaturalMode.HIDDEN) return false;
        return !whitelisted.contains(stack.getItem()) && !itemColors.containsKey(stack.getItem());
    }

    /** ARGB tint for this stack's glint; 0 = leave vanilla. */
    public static int colorFor(ItemStack stack) {
        if (!config.enabled || stack.isEmpty()) return 0;
        Integer per = itemColors.get(stack.getItem());
        if (per != null) return per;
        if (whitelisted.contains(stack.getItem())) return config.tintExtra ? config.extraColor : 0;
        return config.naturalGlint == GlintConfig.NaturalMode.TINTED ? config.naturalColor : 0;
    }

    // ── registry helpers (also used by the GUI for suggestions) ──

    private static Map<String, Item> idMap;
    private static List<String> allIds;

    private static void indexRegistry() {
        if (idMap != null) return;
        Map<String, Item> map = new HashMap<>();
        List<String> ids = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            Identifier key = BuiltInRegistries.ITEM.getKey(item);
            map.put(key.toString(), item);
            ids.add(key.toString());
        }
        ids.sort(String::compareTo);
        idMap = map;
        allIds = ids;
    }

    /** Resolve an item id ("stone" or "minecraft:stone"); null if unknown. */
    public static Item byId(String id) {
        indexRegistry();
        if (id == null || id.isBlank()) return null;
        String s = id.trim().toLowerCase();
        Item direct = idMap.get(s.contains(":") ? s : "minecraft:" + s);
        return direct;
    }

    /** Full "namespace:path" form of an id the user typed; null if it isn't a real item. */
    public static String canonicalId(String id) {
        Item item = byId(id);
        return item == null ? null : BuiltInRegistries.ITEM.getKey(item).toString();
    }

    /** All item ids, sorted — for the whitelist suggestion dropdown. */
    public static List<String> allItemIds() {
        indexRegistry();
        return allIds;
    }
}
