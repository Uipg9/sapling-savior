# Sapling Savior: working notes for Claude

- Minecraft 26.3 Fabric mod. Java 25. Unobfuscated: use Mojang names, never add mappings/yarn.
- TDD: write a failing JUnit test in `src/test` first, then implement. In-game checks go in
  `src/client/.../*ClientGameTest.java`.
- Shell is Windows PowerShell: use `.\gradlew.bat`. Filter Gradle output to errors only.
- Verify with `.\scripts\verify.ps1` (build + unit tests + client gametest). Read the gametest
  screenshots in `build\run\clientGameTest\screenshots\` to check what actually rendered.
- Never kill processes to stop the client: `runClientGameTest` exits by itself.
- Fast leaf decay is the one Mixin (`mixin/LeavesBlockMixin`, tail of `LeavesBlock.tick`); pure rules in
  `LeafDecayRules`. `defaultRequire: 1` makes a broken injection fail the boot. The gametest sets
  `random_tick_speed 0` so only the scheduled-tick decay can remove leaves.
- The tree check (leaves/logs within 6) must stay at ENTITY_LOAD time: tree-felling mods clear leaves in
  the same tick. Plantings are queued in `PlantQueue` (32 per tick, 100-tick expiry, cleared on server stop).
- Planting needs no Mixin: `ServerEntityEvents.ENTITY_LOAD` queues fresh sapling `ItemEntity`s, planting happens at
  `END_SERVER_TICK` in `SaplingSavior`. Pure spacing rules live in `SaplingRules` (unit-testable without
  Minecraft bootstrap).
- `fabric.mod.json` environment must stay `*` (the integrated server needs the mod, and the gametest
  entrypoint lives in the client source set).
- Mod Menu is dev-only (`localRuntime`); do not make it a hard dependency.
