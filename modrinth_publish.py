"""One-shot Modrinth publish for Glint Everything: project + 2 versions + 3 gallery images.
Token comes from the MODRINTH_TOKEN env var; never written to disk."""
import json
import os
import sys

import requests

API = "https://api.modrinth.com/v2"
TOKEN = os.environ["MODRINTH_TOKEN"]
HDR = {"Authorization": TOKEN, "User-Agent": "dev-limucc/glint-everything/1.0.0"}

BODY = """# Glint Everything

Give **any** item the enchantment glint — and take full control of how every glint in the game looks and moves.

## Features

- **Whitelist** — add any item by id (with live suggestions) and it shimmers like an enchanted item: inventory, hand, ground, everywhere. Worn armor too.
- **Glint colors** — recolor the glint of whitelisted items, recolor the *natural* glint (enchanted + other mods' items), or give one specific item its own color. Hex box, RGB sliders and presets.
- **Animation control** — speed, size and angle sliders plus motion styles: Classic, Reverse, Vertical, Horizontal, Frozen.
- **Tame other glint** — vanilla + modded glint can be left alone, tinted, or hidden entirely; the whitelist keeps working either way.
- **Animated flat settings UI** — open from ModMenu or a keybind. Settings apply live and save on close.

Stacks with vanilla's accessibility **Glint Speed / Glint Strength** options.

Client-side only. Requires Fabric API. ModMenu recommended.
"""

def die(step, r):
    print(f"FAIL {step}: {r.status_code} {r.text[:500]}")
    sys.exit(1)

# 1) create project (icon attached)
data = {
    "slug": "glint-everything",
    "title": "Glint Everything",
    "description": "Whitelist any item to give it the enchantment glint - then recolor it globally or per item and restyle the animation (speed, size, angle, motion).",
    "categories": ["decoration", "utility"],
    "client_side": "required",
    "server_side": "unsupported",
    "body": BODY,
    "license_id": "MIT",
    "project_type": "mod",
    "is_draft": True,
    "initial_versions": [],
    "links": {"source_url": "https://github.com/dev-limucc/glint-everything"},
}
r = requests.post(f"{API}/project", headers=HDR,
                  files={"data": (None, json.dumps(data), "application/json"),
                         "icon": ("icon.png", open("src/main/resources/assets/glinteverything/icon.png", "rb"), "image/png")})
if r.status_code != 200:
    die("create-project", r)
proj = r.json()
pid = proj["id"]
print(f"project created: id={pid} slug={proj['slug']}")

# 2) versions
def upload_version(jar, name, game_versions):
    vdata = {
        "project_id": pid,
        "name": name,
        "version_number": name.split(" ")[0].lstrip("v") + "+mc" + game_versions[-1],
        "changelog": "First release.",
        "dependencies": [],
        "game_versions": game_versions,
        "version_type": "release",
        "loaders": ["fabric"],
        "featured": True,
        "file_parts": ["file"],
        "primary_file": "file",
    }
    rr = requests.post(f"{API}/version", headers=HDR,
                       files={"data": (None, json.dumps(vdata), "application/json"),
                              "file": (os.path.basename(jar), open(jar, "rb"), "application/java-archive")})
    if rr.status_code != 200:
        die(f"version {name} {game_versions}", rr)
    print(f"version uploaded: {rr.json()['id']} {name} {game_versions}")

upload_version("dist/glinteverything-1.0.0+mc1.21.11.jar", "v1.0.0", ["1.21.11"])
upload_version("dist/glinteverything-1.0.0+mc26.1.2.jar", "v1.0.0", ["26.1.2"])

# 3) gallery
def gallery(path, title, description, featured):
    params = {"ext": "png", "featured": str(featured).lower(), "title": title, "description": description}
    rr = requests.post(f"{API}/project/{pid}/gallery", headers={**HDR, "Content-Type": "image/png"},
                       params=params, data=open(path, "rb").read())
    if rr.status_code not in (200, 204):
        die(f"gallery {title}", rr)
    print(f"gallery uploaded: {title}")

gallery("Gallery/totem-glint.png", "Whitelisted Totem of Undying",
        "A Totem of Undying added to the whitelist shimmers with the enchantment glint in hand.", True)
gallery("Gallery/settings-general.png", "General settings",
        "Animated flat settings screen: master switch, whitelist toggle, and glint animation controls (speed, size, angle, motion style).", False)
gallery("Gallery/settings-whitelist.png", "Whitelist editor",
        "Add any item by id with live suggestions; select a row to give that item its own glint color.", False)

# 4) single verify
rv = requests.get(f"{API}/project/{pid}/version", headers=HDR)
print("verify versions:", [(v["name"], v["game_versions"]) for v in rv.json()])
rp = requests.get(f"{API}/project/{pid}", headers=HDR)
print("verify gallery:", [g["title"] for g in rp.json().get("gallery", [])])
print("url: https://modrinth.com/mod/" + rp.json()["slug"])
