# Compatibility

These builds are for Minecraft 1.21.11 and Java 21. Use the JAR for your loader.

| Loader | Required version | Settings | Mount potential text |
| --- | --- | --- | --- |
| Fabric | Fabric Loader 0.18.6+ | Mod Menu | Built in, with optional WynnToolZ and Wynntils integration |
| Forge | Forge 61.2.3+, within 61.x | Mods menu | Built in |
| NeoForge | NeoForge 21.11.48+, within 21.11.x | Mods menu | Built in, with the option to use Wynntils |

Fabric API is not required. Mod Menu and Wynntils are optional dependencies. Other mods are not bundled.

Keep Wynncraft's resource pack enabled. Item colors are taken from the mount's appearance, so renaming it won't affect them. The same colors are used in the inventory and both hands, in first and third person.

Opacity, reins colors, and the potential display preference are saved in `config/wyverncolors.json`. The built-in overlay's size and position are saved in `config/wyverncolors-potential.json`. On Fabric, WynnToolZ 2.3.1 can draw the label with Wyvern Colors' tier colors, and WynnExtras 0.19.0 can coexist with Wyvern Colors' reins and flute models.

See [BUILDING.md](BUILDING.md) for build commands.
