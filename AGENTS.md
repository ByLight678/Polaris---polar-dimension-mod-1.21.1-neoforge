# Repository Guidelines

## Project Structure & Module Organization

Polar Dimension targets Minecraft 1.21.1, NeoForge 21.1.264, and Java 21 using ModDevGradle.

- `src/main/java/com/josep/polardimension/`: mod initialization and Java features; `worldgen/` contains terrain generation, `portal/` handles frames and travel, `village/` owns the fixed North Pole layout, and `entity/` handles mobs and residency; `client/` contains rendering.
- `src/main/resources/assets/polar_dimension/`: models, textures, translations, and the purple portal shader.
- `src/main/resources/data/polar_dimension/`: dimension, biome, density-function, noise-setting JSON, and imported village templates.
- `src/main/templates/META-INF/neoforge.mods.toml`: metadata expanded by Gradle.
- `src/generated/resources/`: data generator output; update generators rather than hand-editing generated files.
- `src/gameTest/`: opt-in world-generation integration tests and structure templates, excluded from the release JAR.

## Build, Test, and Development Commands

Use the Windows PowerShell helper to select the local Java 21 JDK and Gradle cache:

```powershell
.\dev.ps1 build                 # Compile, check, package into build/libs/
.\dev.ps1 runClient             # Launch the development client
.\dev.ps1 runServer             # Launch the dedicated development server
.\dev.ps1 runData               # Generate registered data resources
.\dev.ps1 -PpolarisTests runGameTestServer  # Validate generated Polaris terrain
```

With JDK 21 configured, use `gradlew.bat` on Windows or `./gradlew` on Linux/macOS. CI runs `./gradlew build`.

## Coding Style & Naming Conventions

Use four-space Java indentation, UTF-8, and braces on the declaration line. Use `UpperCamelCase` classes, `lowerCamelCase` methods and fields, and `UPPER_SNAKE_CASE` constants. Resource paths and registry IDs use lowercase snake_case under `polar_dimension`. Keep client-only classes out of common initialization. No formatter or linter is configured.

## Testing Guidelines

Run `build` before submitting changes. Run the opt-in NeoForge GameTests for worldgen, village, entity, or portal changes; extend the relevant `*Test` class. Tests check generated chunks, not just JSON syntax. Use fresh worlds when generation changes; validate village templates, rotations, and population. No unit-test framework or coverage threshold is configured. Manually exercise visible changes and dedicated-server compatibility.

## Commit & Pull Request Guidelines

No Git history is available in this checkout. Use concise, imperative subjects such as `Add polar biome registration`. Keep changes focused. PRs should explain behavior, link relevant issues, list validation performed, and include screenshots for visible changes.

## Configuration & Generated Files

Reimport assets with `tools/import_village.py`; regenerate `structure/village/layout.json` with `tools/plan_village.py`. Import wilderness templates with `tools/import_wilderness.py`. Preserve external source templates. Regenerate models from `art/` with `tools/export_elf_models.cjs`, `tools/export_reindeer_model.cjs`, `tools/export_penguin_model.cjs`, `tools/export_evil_snowman_model.cjs`, `tools/export_santa_gecko.cjs`, or `tools/export_evil_elf_model.cjs`. Santa uses GeckoLib; keep its required dependency and bundled distribution in sync.

Keep dependency versions and mod identity in `gradle.properties`. Keep the mod ID consistent with the entry point and resource namespaces. Never commit `.tools/`, `.gradle-user-home/`, `build/`, `run/`, credentials, or local IDE files.











