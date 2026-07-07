# Glint Everything

Give **any** item the enchantment glint — and take full control of how every glint in the game
looks and moves.

> Fabric · Minecraft **1.21.11** and **26.1.x** · client-side only · by **dev-limucc**

## Features

| | |
| --- | --- |
| **Whitelist** | Add any item by id (with live suggestions) and it shimmers like an enchanted item — in the inventory, in hand, on the ground, everywhere. |
| **Glint colors** | Recolor the glint of whitelisted items, recolor the *natural* glint (enchanted + other mods' items), or give one specific item its own color. Hex box, RGB sliders and presets. |
| **Animation control** | Speed, size and angle sliders plus motion styles: Classic, Reverse, Vertical, Horizontal, Frozen. Stacks with vanilla's accessibility Glint Speed / Strength. |
| **Tame other glint** | Vanilla + modded glint can be left alone, tinted, or hidden entirely — the whitelist keeps working either way. |
| **Limucc UI** | Flat, dark, fully animated settings screen (Animated Limucc UI style) — open from ModMenu or a keybind. |

Armor worn on your body gets the tinted glint too.

## How it works

- `ItemStack.hasFoil` decides every glint in the game — the whitelist flips it on, "Hidden" flips it off.
- The glint scroll matrix (`TextureTransform.setupGlintTexturing`) is rebuilt from your speed/size/angle/motion settings; untouched settings run the 100% vanilla path.
- The glint render pipeline has no vertex color, so colors are baked: the vanilla glint texture is
  recolored at runtime into a `DynamicTexture` and the glint render types are cloned per color.
  The per-item color rides from `ItemStackRenderState` onto each `ItemSubmit`, so the deferred
  renderer picks the right clone even in a mixed pile of items.

## Building

```powershell
.\build-all.ps1        # both targets -> dist\
# or one target:
.\gradlew.bat build "-Ptarget=26.1.2"
```

Multi-version layout: shared code in `src/client/java`, per-era shims in `src/client/versions/`
(`gfx-*` graphics facade, `screen-*` render glue, `key-*` keybinding, `foil-*` render hooks).

MIT © dev-limucc
