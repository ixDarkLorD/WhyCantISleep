# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

**Why Can't I Sleep?** (`modid = whycantisleep`, package `net.ixdarklord.whycantisleep`, author ixDarkLorD, license **MPL-2.0**: `LICENSE` at the root is bundled into every jar and `mod_license` feeds every loader's metadata).

> Find out what's keeping you awake at night. Highlights the monsters blocking your sleep, explains every failed sleep, and adds a little bedtime fear.

A multiloader mod built on the author's libraries **CoolCatLib: Core** (`coolcatcore`, package `net.ixdarklord.coolcatcore`) and **CoolCatLib: Canvas** (`coolcatcanvas`, package `net.ixdarklord.coolcatcanvas`; required since 2026-10-13, at the author's request, for the visuals). The repository is private until the first release.

### Rules for every session

- **Never use Architectury API.** The Gradle toolchain is Architectury Loom, but the cross-loader runtime layer is CoolCatLib: Core only.
- **Never guess a CoolCatLib API.** Read its source first (see "CoolCatLib" below). If Core lacks something, say so and ask; don't invent it and don't silently work around it.
- **Build every loader before committing** (`gradlew build`), and fix errors before moving on.
- **Keep config names and defaults in sync with the mod page** (the list under "Config" below is the page's). Renaming or re-defaulting an option is a mod page change: ask first.
- Shared logic goes in `common`; loader modules hold only loader glue.
- **The mod is required on both the client and the server** (the author's decision, 2026-10-13, so the creature could be a real entity with its own model). Registry entries are allowed: the `lurker` entity type and the `dread` attachment exist. Sounds are unregistered `SoundEvent`s (`sound/WCISSounds`; the server names one by id, the client resolves it). `assets/whycantisleep/sounds.json` is **generated**: the author drops `.ogg` files into `assets/whycantisleep/sounds/` (see its `README.txt` for the names) and `tools/gen_sounds_json.ps1` rewrites the file, using a sound's own files when present and a vanilla stand-in otherwise. Add a new sound in three places: `WCISSounds`, the script's table, the README's table.
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

Source on this machine, one checkout per Minecraft version: `../CoolCatLib-26.1.2` (branch `26.1.2`), `../CoolCatLib-1.21` (1.21.1), `../CoolCatLib-1.20.1`. Core is `core/{common,fabric,neoforge[,forge]}`; the public API is `core/common/src/main/java/net/ixdarklord/coolcatcore/api`. The API differs between versions (1.20.1 has its own `network.codec` and `CustomPacketPayload`, `ClientGuiEvents.RenderHud` takes `GuiGraphics, float`...), so read the checkout for the version being worked on. Canvas (`coolcatcanvas`, `canvas/` in the same checkouts, API under `canvas/common/.../coolcatcanvas/api`) is required too: `ScreenEffects.tint`, `CameraShake`, `WorldParticles` / `ParticleSpec` / `VfxMaterial` / `Curve` and `Easing`, all used from `client/fx/SleepEffects` and the HUD. Its camera shake and world particles exist on 26.1.2 only so far: check the 1.21.1 and 1.20.1 checkouts before backporting.

APIs this mod uses (`net.ixdarklord.coolcatcore.api.*`):

- `core.ModConstructor` / `ClientModConstructor`: `onConstructMod()` is where configs, payloads and events are registered (NeoForge collects payloads before common setup). `core.neoforge.NeoForgeModEntrypoint`.
- `config.*`: `Config.builder(MOD_ID, ConfigScope.CLIENT | SERVER)`, values as static `ConfigValue` fields, groups with `push`/`pop` in static blocks, `enumValue` (enums implement `EnumType.Displayable`), `color`, `intValue().range().slider()`, `enabledWhen`. A `SERVER` config is synced to clients on join and on change (`Config.isRemote()`, `ConfigEvents.SYNCED`). Names are translated as `config.whycantisleep.<client|server>.<group>.<key>` (`.title` for the config); tooltips fall back to the `comment`. Screens come from Glazed Menu when installed (`config.client.ConfigScreens`); Core has none of its own.
- `network.Network`: `registerClientbound` / `registerServerbound` (record payloads can derive their codec through `PayloadCodecs`), `sendToPlayer`, `sendToServer`, and `canPlayerReceive(player, type)`, which is how the server tells who has the mod. Clientbound handlers must be lambdas into a `client` package class so a dedicated server never loads it.
- `event.v2.common`: `PlayerEvents` (`JOIN`, `LEAVE`, `START_TICK`, `END_TICK`), `ServerTickEvents`, `ServerLifecycleEvents`, `BlockEvents.BREAK`, `EntityEvents.LOAD`. `event.v2.client`: `ClientTickEvents`, `ClientGuiEvents.RENDER_HUD`, `ClientPlayerEvents`. Each is an `EventInvoker` (`X.END.register(...)`).
- `registry.DeferredRegister` / `RegistryEntry` / `EntityAttributes` and `client.registry.EntityRendererRegistry` (the Lurker; Core has no model layer registry, so `LurkerRenderer` bakes its `LayerDefinition` itself), `attachment.AttachmentRegistry` (dread: saved, synced to its own player, kept through death), `event.v2.common.CommandEvents`, `platform.Platform`.

## Sides and sync

The mod is needed on both sides. The server is authoritative: it sends the failure reason and the exact blocking mobs, runs the sleep board and nudges, keeps each player's dread, and its config overrides the client's. The client still recognises the game's own sleep messages by translation key and can redo the monster check itself (`SleepChecks.blockersOnClient`), a leftover from when client-only installs were supported; it is harmless and keeps the HUD working if a report is ever missed.

Config gating: every client option a server setting controls is declared `enabledWhen(<server value>)` in `WCISClientConfig`. Glazed Menu evaluates dependencies across configs, so the entry shows greyed out ("Depends on …") while the server has it off: that is the "locked" state. Code must read such options through `WCISConfigs.isEnabled(option)`, which walks the same dependency chain, never through `option.get()` alone.

## The sleep HUD

Sleep messages are not shown on the game's action bar. `GuiMixin` cancels the game's sleep lines (recognised by translation key in `client/hud/Explanations`) and `client/hud/SleepNotices` shows a card instead: a `SleepNotice` (kind, title, hint), whose kind gives the accent color, the icon (a vanilla item) and the sound and screen effect (`SleepEffects.onNotice`). The sleep board (`client/board/SleepBoard`) and the cards share `client/hud/HudStyle`, which is the game's own UI on purpose (the author asked for a clean HUD on theme with Minecraft's): the tooltip's box and frame sprites, the boss bar sprites for progress, the chat's colors for text, vanilla items for icons and `PlayerFaceExtractor` for players. Don't add custom panel art or gradients to it. The option is still named "Action bar info" on the mod page: it now decides whether a blocked sleep's card names the count and the closest monster.

## The thing under the bed (`lurker/`)

Added at the author's request on 2026-10-13; every number is a first guess (`LurkerServer`'s constants).

- **Dread** (`Dread`, 0 to 100, a synced player attachment): +15 for lying down to sleep with block light under 4 at the bed, -20 for sleeping in the light, +2 a minute after a day and a half without sleep and +4 after three days (the game's `time_since_rest` statistic). Stages: Watched from 25, Haunted from 50, Hunted from 85.
- **Watched and Haunted are client side** (`lurker/client/LurkerHaunt`), seen only by the player: eyes under dark beds and whispers; then the creature itself as a hallucination, a `LurkerEntity` added to the client's level only (never known to the server), standing in the dark or crawling out beside a bed the player uses, gone when looked at, approached or after 8 seconds.
- **Hunted is real** (`LurkerServer.summon`): a `LurkerEntity` comes from beside the player's last bed if it is near, else from the dark around them. It targets only its victim, vanishes in daylight, when its player is out of reach for 20 seconds or after 3 minutes. Killing it sets dread to 0; if it kills its player, dread drops to 30.
- **Opting out**: the client option "Scary content" off stops every frightening effect on that client and tells the server (`ScaryPreferencePayload`) to build no dread for, and send no creature after, that player. The server option "Bed creature" turns it off for everyone. Creative and spectator players are left alone.
- **Model and textures**: `LurkerModel` (boxes in code: chest, belly, tail, horned head with a hinged jaw, four two-part clawed limbs) and `tools/gen_lurker_textures.ps1`, which share the parts' places on a 64x64 layout drawn at 128x128; change both together. The eyes are a second texture drawn with `RenderTypes.eyes`: a ring of violet light around an unlit core. `LurkerRenderState` carries the strike animation and whether it is a hallucination (drawn translucent and flickering).
- **Presence** (`lurker/client/LurkerPresence`): any Lurker within 24 blocks drives the `whycantisleep:dread` Canvas screen effect (`post_effect/dread.json`, `shaders/post/dread.fsh`; uniforms `Pulse` and `Approach`): color drains and a ragged darkness closes in with nearness; while it closes the distance the view tightens and splits at the edges, and a quickening heartbeat flushes the edges red and jolts the camera. `tools/gen_arrow_texture.ps1` draws the HUD arrows.
- **Testing**: `/whycantisleep dread [0-100]` and `/whycantisleep summon` (operators).

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
| Scary content | on | **new, not on the mod page yet** |
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
| Bed creature | on (**new, not on the mod page yet**) |
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
