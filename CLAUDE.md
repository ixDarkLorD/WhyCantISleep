# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

**Why Can't I Sleep?** (`modid = whycantisleep`, package `net.ixdarklord.whycantisleep`, author ixDarkLorD, license **MPL-2.0**: `LICENSE` at the root is bundled into every jar and `mod_license` feeds every loader's metadata).

> Find out what's keeping you awake at night. Highlights the monsters blocking your sleep, explains every failed sleep, and adds a little bedtime fear.

A multiloader mod built on the author's library **CoolCatLib: Core** (`coolcatcore`, package `net.ixdarklord.coolcatcore`). The repository is private until the first release.

### Rules for every session

- **Never use Architectury API.** The Gradle toolchain is Architectury Loom, but the cross-loader runtime layer is CoolCatLib: Core only.
- **Never guess a CoolCatLib API.** Read its source first (see "CoolCatLib" below). If Core lacks something, say so and ask; don't invent it and don't silently work around it.
- **Build every loader before committing** (`gradlew build`), and fix errors before moving on.
- **Keep config names and defaults in sync with the mod page** (the list under "Config" below is the page's). Renaming or re-defaulting an option is a mod page change: ask first.
- Shared logic goes in `common`; loader modules hold only loader glue.
- Commits follow the user's `github-commit` skill: their identity only, no Claude attribution, and push only when asked.

## Versions

One folder per Minecraft version, each on its own branch of the same repo (the author's convention, e.g. `../GlazedMenu-26.1.2`, `../GlazedMenu-1.20`). The targets are the loader/version pairs CoolCatLib: Core actually ships:

| Minecraft | Loaders | Folder / branch | CoolCatLib: Core (Maven Local) | State |
|---|---|---|---|---|
| 26.1 – 26.1.2 | Fabric, NeoForge | this folder, `main` | `26.1.2-4` | in progress (first version) |
| 1.21.1 | Forge, Fabric, NeoForge | `../WhyCantISleep-1.21.1` (not created) | `1.21.1-2100.2.0.1` | backport, after 26.1.2 is feature complete |
| 1.20.1 | Forge, Fabric (no NeoForge) | `../WhyCantISleep-1.20.1` (empty folder) | `1.20.1-2001.2.0.1` | backport |
| 26.2, 26.3 | Fabric, NeoForge | — | **none: CoolCatLib has no build for them** | blocked until CoolCatLib is ported |

The shared catalogs `26.2-v1` and `26.3-v1` exist (Glazed Menu uses them), but this mod requires Core at runtime, so 26.2+ waits on a CoolCatLib port. Finish all features on 26.1.2 before backporting.

## Build setup (copied from `../Starwoven-26.1.2` and `../GlazedMenu-26.1.2`; do not restructure)

- Build logic is **not in this repo**: `settings.gradle`, `build.gradle` and each module's `build.gradle` apply the remote scripts `ixDarkLorD/ModResources/gradle/v4/*.gradle`. Never copy or edit them; project tweaks go in the module's own `build.gradle`.
- Versions come from the catalog `net.ixdarklord:sharedcatalogs:${dependencies_catalog_version}` (`libs.*`): Minecraft 26.1.2, Java 25, loaders, Fabric API. Ask before hardcoding a version the catalog lacks.
- Architectury Loom **no-remap** (26.1 is unobfuscated: no mappings, `implementation`/`compileOnly` rather than `mod*` configurations), Gradle 9.4 wrapper.
- `gradle.properties` keys are read by the shared scripts by name; keep them. `mod_version` is a plain build number and the Minecraft version goes in front everywhere (`26.1.2-1`). `project_upload_debug=true` stays until the author says to publish.
- Each loader module needs its own `gradle.properties` (`loom.platform`, `platform_name`); the shared scripts fail without it. The root `CHANGELOG.md` is required too (the scripts read it at configuration time, and it is bundled into the jars).
- CoolCatLib: Core (`coolcatlib_version`) and MCP Bridge (`mcp_bridge_version`, `localRuntime`, dev runs only) come from **Maven Local**. If Core is missing there, every Gradle task fails: publish it from `../CoolCatLib-26.1.2` with `gradlew assemble publishToMavenLocal`.
- Machine-specific values (`curseforge_api_token`, `modrinth_api_token`, `mod_resources`, `build_jar_output_dir`) live in `~/.gradle/gradle.properties`, never in the repo.
- `build` also copies the loader jars to `build_jar_output_dir` and bumps the untracked `build_numbers.json`.

### Commands (PowerShell: `.\gradlew.bat`, Git Bash: `./gradlew`)

```
gradlew build                    # every loader; jars in <loader>/build/libs
gradlew :common:compileJava      # quickest compile check (or :fabric: / :neoforge:)
gradlew :fabric:runClient
gradlew :neoforge:runClient
gradlew :fabric:runServer        # run/server; Fabric needs run/server/eula.txt (the author accepts it, not Claude)
gradlew :neoforge:runServer
```

There are no tests or linters; verify by building and running the game. Both clients share `run/`. A running game locks `common/build/libs/*-dev.jar`: stop it before rebuilding, and when a server and a client run together start the server first and the client with `-x :common:jar`.

**Testing in the running game**: the `minecraft` MCP tools (MCP Bridge) drive a dev game: `game_start` (`loader`, `task`, `world`), `mc_command`, `mc_logs`, `mc_screenshot`, `mc_summon`, `mc_time`... After checks, `mc_leave_world` then `game_stop`. MCP Bridge exists for 26.1.2 only, so backports are checked by hand.

## Module layout

| Module | What goes there |
|---|---|
| `common` | **Everything.** `WhyCantISleep` (`ModConstructor`: `MOD_ID`, `LOGGER`, `id(path)`), `WhyCantISleepClient` (`ClientModConstructor`), `config/`, the mixins (`mixin/`, client ones in `mixin/client/`, listed in `whycantisleep.common.mixins.json`), assets and data. No `net.fabricmc.*`, `net.neoforged.*` or Forge imports. |
| `fabric` | No Java. `fabric.mod.json` lists the constructors under the `coolcatcore:common` / `coolcatcore:client` entrypoints. |
| `neoforge` | `WhyCantISleepNeoForge extends NeoForgeModEntrypoint`: `common(WhyCantISleep::new)`, `client(() -> WhyCantISleepClient::new)`. |
| `forge` (1.21.1 and 1.20.1 only) | The same with CoolCatLib's `ForgeModEntrypoint`. |

Resources in `common/src/main/resources` are merged into every loader jar, and `${...}` placeholders are expanded in `pack.mcmeta`, `fabric.mod.json` and `neoforge.mods.toml`.

Loader-specific needs, in order: a CoolCatLib: Core API; else a **common** mixin (Core has no sleep, entity-death, glow or camera hooks, so those are mixins); only then loader code.

## CoolCatLib

Source on this machine, one checkout per Minecraft version: `../CoolCatLib-26.1.2` (branch `26.1.2`), `../CoolCatLib-1.21` (1.21.1), `../CoolCatLib-1.20.1`. Core is `core/{common,fabric,neoforge[,forge]}`; the public API is `core/common/src/main/java/net/ixdarklord/coolcatcore/api`. The API differs between versions (1.20.1 has its own `network.codec` and `CustomPacketPayload`, `ClientGuiEvents.RenderHud` takes `GuiGraphics, float`...), so read the checkout for the version being worked on. Canvas (`coolcatcanvas`) is **not** a dependency of this mod.

APIs this mod uses (`net.ixdarklord.coolcatcore.api.*`):

- `core.ModConstructor` / `ClientModConstructor`: `onConstructMod()` is where configs, payloads and events are registered (NeoForge collects payloads before common setup). `core.neoforge.NeoForgeModEntrypoint`.
- `config.*`: `Config.builder(MOD_ID, ConfigScope.CLIENT | SERVER)`, values as static `ConfigValue` fields, groups with `push`/`pop` in static blocks, `enumValue` (enums implement `EnumType.Displayable`), `color`, `intValue().range().slider()`, `enabledWhen`. A `SERVER` config is synced to clients on join and on change (`Config.isRemote()`, `ConfigEvents.SYNCED`). Names are translated as `config.whycantisleep.<client|server>.<group>.<key>` (`.title` for the config); tooltips fall back to the `comment`. Screens come from Glazed Menu when installed (`config.client.ConfigScreens`); Core has none of its own.
- `network.Network`: `registerClientbound` / `registerServerbound` (record payloads can derive their codec through `PayloadCodecs`), `sendToPlayer`, `sendToServer`, and `canPlayerReceive(player, type)`, which is how the server tells who has the mod. Clientbound handlers must be lambdas into a `client` package class so a dedicated server never loads it.
- `event.v2.common`: `PlayerEvents` (`JOIN`, `LEAVE`, `START_TICK`, `END_TICK`), `ServerTickEvents`, `ServerLifecycleEvents`, `BlockEvents.BREAK`, `EntityEvents.LOAD`. `event.v2.client`: `ClientTickEvents`, `ClientGuiEvents.RENDER_HUD`, `ClientPlayerEvents`. Each is an `EventInvoker` (`X.END.register(...)`).
- `attachment.AttachmentRegistry` (persistent per-player counters), `platform.Platform`.

## Sides and sync

The mod must work in three installs, and neither side may require the other:

- **Client only** (singleplayer host is "both"; any server without the mod): the client detects a failed sleep from the vanilla message, redoes the vanilla monster check locally, and runs highlights, arrows, explanations, the dimension warning and whispers. The server-only features (sleep board, nudges, show to everyone, advancements) are off.
- **Server with the mod**: the server is authoritative. It sends the failure reason and the exact blocking mobs to clients that have the mod, and its config overrides theirs.
- **Server only**: players without the mod get the vanilla glowing effect when "Show to everyone" is on, and nudges (a vanilla sound and message).

Config gating: every client option a server setting controls is declared `enabledWhen(<server value>)` in `WCISClientConfig`. Glazed Menu evaluates dependencies across configs, so the entry shows greyed out ("Depends on …") while the server has it off: that is the "locked" state. Code must read such options through `WCISConfigs.isEnabled(option)`, which walks the same dependency chain, never through `option.get()` alone.

## Config (the names and defaults on the mod page)

Client (`WCISClientConfig`, `config/whycantisleep-client.toml`):

| Option | Default | Locked by |
|---|---|---|
| Highlight monsters | on | Allow highlights |
| Threat colors | on | |
| Highlight color | red | |
| Off-screen arrows | on | Allow arrows |
| Arrow size | medium (small / medium / large) | |
| Action bar info | on | |
| Sleep explanations | on | |
| Dimension warning | on | |
| Sleep board | on | Sleep board (server) |
| Whispers | on | |
| Whisper volume | 70% | |
| Under-the-bed scares | on | Scare chance = 0 |
| Screen shake | on | |

Server (`WCISServerConfig`, `config/whycantisleep-server.toml`, synced):

| Option | Default |
|---|---|
| Allow highlights | on |
| Highlight duration | 3 s (1–10) |
| Allow arrows | on |
| Show to everyone | off (vanilla glowing effect for players without the mod) |
| Sleep board | on |
| Allow nudges | on |
| Nudge cooldown | 30 s |
| Scare chance | 2% (0 disables) |
| Advancements | on |

## Features, in build order

1. Monster highlights: exactly the mobs that blocked the sleep (vanilla check range only) glow for the configured time, colored by threat type or one color.
2. Off-screen arrows around the crosshair for blockers that are off-screen or behind walls, and action bar text ("3 monsters nearby - closest: Zombie, 6 blocks below").
3. An explanation and fix hint for every sleep failure (monsters, wrong time, too far, obstructed, occupied, other players awake) and a warning before sleeping in the Nether/End.
4. Multiplayer sleep board (server): who is awake and why, progress toward `playersSleepingPercentage`, and nudges with a cooldown.
5. Spooky touches: whispers panned toward the nearest monster, a rare "monster under the bed" scare, and the advancements "Monsters in the Closet" (blocked by monsters 10 times), "Insomniac" (50 failed sleeps), "Night Shift" (kill a blocker within 5 seconds).

After each feature: build every loader, test in game when possible, add a `CHANGELOG.md` line, commit. Finish with a README (features, config options, supported versions, license).

## Code conventions (from the author's other mods)

- These are **26.1 names**: `net.minecraft.resources.Identifier` (not `ResourceLocation`), `GuiGraphicsExtractor` and `extract*` (not `GuiGraphics` / `render*`), GUI text colors need alpha bits. If unsure of a vanilla API, read the decompiled 26.1.2 source (`gradlew :common:genSourcesWithVineflower`, then the `*-sources.jar` under `.gradle/loom-cache/minecraftMaven/`) rather than guessing from older versions.
- Classes are `final` with a private constructor when they only hold statics; a short Javadoc on each class says what it is for. Comments explain why, in plain sentences.
- Config classes are `WCIS<Scope>Config`; all user-facing text is a translation key in `assets/whycantisleep/lang/en_us.json`, kept sorted by key. Keys: `config.whycantisleep.*`, `gui.whycantisleep.*`, `advancements.whycantisleep.*`, `subtitles.whycantisleep.*`.
- The server is authoritative: clients send intentions (a nudge), and the server checks permission, cooldown and state. Size-cap every serverbound payload.
- Java 25 language features are fine. No Lombok, and no new libraries without asking.
- `CHANGELOG.md`: `## v<mc>-<build> Release | <date>` with emoji subsections (✨ New Features / 🐛 Bug Fixes / 🛠️ Changes).
- Never commit `build/`, `run/`, `.gradle/` or secrets.
