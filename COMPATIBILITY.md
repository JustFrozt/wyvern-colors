# Compatibility

These builds are for Minecraft 1.21.11 and Java 21. Use the JAR for your loader.

| Loader | Required version | Settings | Mount potential text |
| --- | --- | --- | --- |
| Fabric | Fabric Loader 0.18.6+ | Mod Menu | Requires compatible Wynntils |
| Forge | Forge 61.2.3+, within 61.x | Mods menu | Built in; Wynntils not required |
| NeoForge | NeoForge 21.11.48+, within 21.11.x | Mods menu | Uses Wynntils when installed; built in otherwise |

Fabric API is not required. Mod Menu and Wynntils are optional dependencies. Other mods are not bundled.

Keep Wynncraft's resource pack enabled. Item colors are taken from the mount's appearance, so renaming it won't affect them. The same colors are used in the inventory and both hands, in first and third person.

Opacity and reins color preferences are saved in `config/wyverncolors.json`. Forge's text size and position, and NeoForge's when Wynntils is absent, are saved in `config/wyverncolors-potential.json`. Integrated text settings are saved by Wynntils. An incompatible Wynntils version may need updating before its text controls can be used.

See [BUILDING.md](BUILDING.md) for build commands.
