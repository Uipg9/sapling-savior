# Sapling Savior

Saplings that drop when you chop a tree or its leaves decay plant themselves. A Fabric mod for
**Minecraft 26.3** (Java 25, Mojang names). Server-side logic, so it works in singleplayer and on servers.

## Behavior

- A freshly dropped sapling item (leaf decay, breaking leaves, or chopping a tree) falls to the ground
  and is planted there if the spot is valid (`canSurvive`: dirt/grass/podzol, enough light).
- **Spacing:** nothing is planted if another sapling is within **4 blocks** (horizontally, same area
  vertically). The sapling then stays a normal item, so nothing is lost.
- **Dark oak and jungle are exempt** from spacing against their own kind, so a 2x2 for the big tree
  can be planted. A cluster is capped at four.
- Saplings you throw or drop yourself, saplings from chests, and saplings far from any leaves or logs are left alone.
- **Tree-felling mods** (FallingTree, Tree Harvester, TreeChop and similar) work: whether a drop belongs
  to a tree is decided the moment the item spawns, so removing all the leaves in the same tick is fine.
  If another mod replants first, the spot is simply taken and we skip it.
- A big drop (a whole forest felled at once) is planted at most 32 per tick, so it never causes a lag spike.
- Mangrove propagules are not handled.

## Fast leaf decay

Leaves with no log nearby decay within a few seconds instead of minutes, each after a short random
delay so a canopy thins out instead of vanishing at once. Drops are normal, so the saplings get
planted right away. Player-placed (persistent) leaves are never touched. Implemented as one Mixin
on `LeavesBlock.tick`.

## Development (PowerShell)

| Task | Command |
|---|---|
| Build + unit tests | `.\gradlew.bat build` |
| Launch the dev client (Mod Menu included) | `.\gradlew.bat runClient` |
| In-game test (drops saplings under leaves, asserts planting and spacing) | `.\gradlew.bat runClientGameTest` |
| Everything | `.\scripts\verify.ps1` |

Requires Java 25.

## License

CC0-1.0.

## Credits

Fast leaf decay idea: [Quick Leaf Decay](https://github.com/wb1016/quickleafdecay) and
[Leaves Us In Peace](https://gitlab.com/supersaiyansubtlety/leaves_us_in_peace) (both CC0). This is a
from-scratch implementation.
