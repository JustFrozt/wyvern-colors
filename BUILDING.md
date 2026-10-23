# Building

Use a Java 21 JDK and Python 3.11 or newer. Run these commands from the repository root. On Windows, use `gradlew.bat` and `python`.

Dependencies are downloaded by Gradle. You don't need to supply local mod JARs.

## Fabric

    bash ./gradlew --no-daemon build
    python3 tools/verify_release.py build/libs/wyvern-colors-2.0.jar

To build without optional integrations in the development runtime, add `-PwithoutWynntils`, `-PwithoutModMenu`, or both to the Gradle command.

## Forge

    bash ./gradlew --no-daemon -p ports/forge build
    python3 tools/verify_loader_release.py ports/forge/build/libs/wyvern-colors-forge-1.21.11-2.0.jar forge

## NeoForge

    bash ./gradlew --no-daemon -p ports/neoforge build
    python3 tools/verify_loader_release.py ports/neoforge/build/libs/wyvern-colors-neoforge-1.21.11-2.0.jar neoforge

Add `-PwithoutWynntils` to build NeoForge without Wynntils in the development runtime.

## Release checks

    python3 tools/verify_release.py

The release checker checks the artwork against its approved hashes. Before releasing a build, also check the inventory and held items on Wynncraft, both reins color settings, resource reloads, and the text controls in game.
