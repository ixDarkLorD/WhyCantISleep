# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

**Why Can't I Sleep?** (`modid = whycantisleep`, package `net.ixdarklord.whycantisleep`, author ixDarkLorD, license **MPL-2.0**: `LICENSE` at the root is bundled into every jar and `mod_license` feeds every loader's metadata).

> Find out what's keeping you awake at night. Highlights the monsters blocking your sleep, points to the ones you can't see, explains every failed sleep, and shows who is still awake.

> **Split on 2026-10-21, at the author's request.** This repository used to hold everything; the Lurker, the Dark Between, boss combat, and mood and lucidity moved to their own mod, **Behind Closed Eyes** (`behindclosedeyes`, `../BehindClosedEyes-26.1.2`, cloned from this repository's history). This mod kept the name and the sleep features: monster highlights, off-screen arrows, the sleep HUD's cards and explanations, the dimension warning, and the sleep board with nudges. The whispers and the "monster under the bed" scare went with Behind Closed Eyes. The two mods don't depend on each other.

> **CoolCatLib is one mod since 2026-10-14 (version 26.1.2-5):** Core and Canvas merged into **CoolCatLib**, mod id `coolcatlib`. This project now depends on `net.ixdarklord.coolcatlib:coolcatlib-{common,fabric,neoforge}` (`coolcatlib_version`, no `coolcatcanvas_version`), requires `coolcatlib` in `fabric.mod.json` / `neoforge.mods.toml`, and lists its constructors under the `coolcatlib:common` / `coolcatlib:client` entrypoints. The packages are unchanged (`net.ixdarklord.coolcatcore`, `net.ixdarklord.coolcatcanvas`); its sources are `../CoolCatLib-26.1.2/{common,fabric,neoforge}`. Where the notes below say Core, Canvas, `coolcatcore`, `coolcatcanvas` or `coolcatlib-core-*` / `coolcatlib-canvas-*`, read them as this one mod.

> **Folios is a required dependency since 2026-10-22 (the author's pick)**: the mod's guide book is a Folios screen book, `assets/whycantisleep/folios/guide/` (book, five categories, entries; its words are `guide.whycantisleep.guide.*` in the lang file), on the mod's own item, `whycantisleep:guide_book` ("Insomniac's Handbook", `compat/WCISItems`; Folios' cover model, a recipe of a book and any bed), given to each player once on first join (`compat/GuideBookGift`, remembered by a player tag; server config `giveGuideBook`). Folios (`folios_version`, `26.1.2+2.0` since 2026-10-22, `net.ixdarklord.folios:folios-{common,fabric,neoforge}`) comes from Maven Local, published from `../Folios-26.1.2` with `gradlew assemble publishToMavenLocal`, and needs CoolCatLib `26.1.2-6`. The closets the book shows are build files (`folios/guide/builds/*.json`: wooden, trapdoor, double), all in one `build` element on the Hiding Spots entry. Keep the book in step with the features: a Folios tab shows at most five entries. The first upload is a **beta** (`project_release_type=beta`).

A multiloader mod built on the author's libraries **CoolCatLib: Core** (`coolcatcore`, package `net.ixdarklord.coolcatcore`) and **CoolCatLib: Canvas** (`coolcatcanvas`, package `net.ixdarklord.coolcatcanvas`; required since 2026-10-13, at the author's request, for the visuals). The repository is private until the first release.

### Rules for every session

- **Never use Architectury API.** The Gradle toolchain is Architectury Loom, but the cross-loader runtime layer is CoolCatLib: Core only.
- **Never guess a CoolCatLib API.** Read its source first (see "CoolCatLib" below). If Core lacks something, say so and ask; don't invent it and don't silently work around it.
- **Build every loader before committing** (`gradlew build`), and fix errors before moving on.
- **Keep config names and defaults in sync with the mod page** (the list under "Config" below is the page's). Renaming or re-defaulting an option is a mod page change: ask first.
- Shared logic goes in `common`; loader modules hold only loader glue.
- **The mod registers one item and nothing else** (the guide book, `whycantisleep:guide_book`, "Insomniac's Handbook", in `compat/WCISItems`: a Folios `GuideBookItem` with its own name, crafted from a book and any bed; the author's request, 2026-10-22). No blocks, entities or attachments: the server only sends payloads and runs the sleep board, and sounds are unregistered `SoundEvent`s (`sound/WCISSounds`). `assets/whycantisleep/sounds.json` is **generated**: the author drops `.ogg` files into `assets/whycantisleep/sounds/` (see its `readme.txt` for the names) and `tools/gen_sounds_json.ps1` rewrites the file, using a sound's own files when present and a vanilla stand-in otherwise. Add a new sound in three places: `WCISSounds`, the script's table, the readme's table.
- Commits follow the user's `github-commit` skill: their identity only, no Claude attribution, and push only when asked.

## This branch: 1.20.1 (ported 2026-10-23)

This folder is the **1.20.1** port (Forge and Fabric) of `main` at `5692d22`. The rest of this file describes the 26.1.2 version; where it says otherwise, this section wins. Behaviour and design are the same; only what 1.20.1 forces changed. **Nothing here has been run in game yet** (MCP Bridge has no 1.20.1 build): every item below is built, not tested.

- **Build**: catalog `1.20.1-v9`, `project_loaders=fabric,forge`, `project_java_version=17`, **remapping** Architectury Loom (Mojang mappings + Parchment, `mod*` configurations), copied from `../WhereDidIPutIt-1.20.1`. CoolCatLib is `coolcatlib-<loader>:1.20.1-${coolcatlib_version}` (`2001.4.0.1`), Folios `folios-<loader>:${folios_version}` (`1.20.1+2.1`, published from `../Folios-1.20.1` on 2026-10-23), both from Maven Local. No MCP Bridge or Glazed Menu build exists for 1.20.1. `-Ptest_world="<world folder>"` starts a dev run in a world of `run/saves`. `pack.mcmeta` has `pack_format`; the access widener header is `named`.
- **Modules**: `common`, `fabric`, `forge` (`WhyCantISleepForge extends ForgeModEntrypoint`, `META-INF/mods.toml`; its Minecraft range uses the `[${mc_version},) , (,${max_mc_version}]` form). No `neoforge`. Commands are `:forge:` where the text says `:neoforge:`.
- **Names**: `ResourceLocation`, `GuiGraphics` (`drawString` through `HudStyle.text`, which skips text faded under alpha 5 because 1.20.1 draws alpha-less text opaque), `PoseStack` (`pushPose`, `translate(x, y, 0)`, `mulPose(Axis.ZP...)`), `net.minecraft.Util`, CoolCatLib's `api.utils.ARGB` for 26.1's `ARGB`, `player.serverLevel()`, `entity.getType().is(tag)`, `GameRules.getInt/getBoolean(RULE_...)`, `ClickEvent(Action, value)`, key categories as strings, `Mth.clamp` for `Math.clamp`. `ClientGuiEvents.RENDER_HUD` takes `(GuiGraphics, float partialTick)`.
- **HUD sprites** (`client/hud/GuiSprites`): 1.20.1 has no GUI sprite atlas, so the sprites stay under `textures/gui/sprites/hud/` as plain textures and `GuiSprites.blit` draws them, stretched or nine-sliced by their `.mcmeta` (`gui.scaling`, the later format), tinted through `GuiGraphics.setColor`; read once, again after a resource reload (`MinecraftMixin`). The vanilla hearts and toast mouse of the breath minigame map to `icons.png` / `toasts.png` there. **Item fade** (`ItemFade`) is the shader color around `renderFakeItem` (no `GuiItemRenderStateMixin`/`GuiRendererMixin`): flat items fade, solid block models only darken. Faces are `PlayerFaceRenderer`. The **tooltip** icon and pips are glyphs (▲ ▼ ■) in their colors, as 1.20.1 can't set sprites in text; the `pip_*` sprites are unused here.
- **Rendering mixins** (no render states on 1.20.1): `PlayerRendererMixin` (render HEAD: the chest peek, `ChestPeek.render` with the player itself, drawn in place of the player, so no name tag; `getRenderOffset` RETURN: the floor drop and the tremble) replaces `AvatarRendererMixin`, `AvatarRenderStateMixin`, `LivingEntityRendererMixin` and `HidingPose`; `HumanoidModelMixin` poses from the entity in `setupAnim(LivingEntity, ...)` and clamps the head there. `CameraMixin` hooks `Camera.setup`. The near plane under a bed is `GameRendererMixin`'s `@ModifyConstant(0.05F)` in `getProjectionMatrix` (`HidingClient.nearPlane`). The **squint** is drawn from `GameRendererMixin` at `popPush("gui")` in `GameRenderer.render`, before the HUD and with F1 too (1.20.1 skips `Gui.render` when the HUD is hidden). The off-screen arrows take the FOV from `GameRendererAccessor.getFov`. `LocalPlayerMixin` hooks `serverAiStep`; the inventory's clicks are `InventoryScreenMixin.mouseClicked` (no `AbstractRecipeBookScreen`); `ChatScreenMixin.mouseClicked(double, double, int)`.
- **Events CoolCatLib 1.20.1 lacks**: no `LivingEvents` (damage: `LivingEntityMixin` at `LivingEntity.hurt` HEAD calls `SleepNights.onHurt` and `HidingServer.onHurt`), no `PlayerEvents.CHANGE_DIMENSION` (`ServerPlayerMixin` at `triggerDimensionChangeTriggers` TAIL calls `HidingServer.onChangedDimension`). `ItemTooltipEvents.APPEND` has no tooltip context. A closet or chest hider is turned with `connection.teleport(..., yaw, 0)`.
- **Data**: `recipes/`, `tags/items/`, `tags/entity_types/`. 1.20.1 has no jukebox songs: `#whycantisleep:creepy`, `calm` and `loud` are **item tags of music discs** (`Bedroom` reads the jukebox's record). No undead, arthropod or illager tags: `ThreatType` uses `MobType`. Sleep effects use `BuiltInRegistries.MOB_EFFECT` ids (the datagen builder takes a `MobEffect`); the data loaders are the non-generic `SimpleJsonResourceReloadListener(Gson, dir)`. Effect names: `CONFUSION`, `MOVEMENT_SLOWDOWN`; `level.isDay()`; `BedBlock.canSetSpawn` for beds that explode.
- **Guide book**: no item model definitions on 1.20.1: `models/item/guide_book.json` parents `folios:item/guide_book` (builtin/entity) and the client registers Folios' own renderer for it (`ItemRendererRegistry.register(WCISItems.GUIDE_BOOK, GuideBookItemModel::render)`, **a Folios internal class**: Folios 1.20.1 has no API to draw a mod's own book item). A recipe result can't carry NBT, so the item names its book in its NBT (`folios_book`) when crafted and while in an inventory, and `getDefaultInstance` is the named stack. No `Item.Properties.setId`.

## Versions

One folder per Minecraft version, each on its own branch of the same repo (the author's convention, e.g. `../GlazedMenu-26.1.2`, `../GlazedMenu-1.20`). The targets are the loader/version pairs CoolCatLib: Core actually ships:

| Minecraft | Loaders | Folder / branch | CoolCatLib: Core (Maven Local) | State |
|---|---|---|---|---|
| 26.1 – 26.1.2 | Fabric, NeoForge | `../WhyCantISleep-26.1.2`, `main` | `26.1.2-6` | in progress (first version) |
| 1.21.1 | Forge, Fabric, NeoForge | `../WhyCantISleep-1.21.1` (not created) | `1.21.1-2100.2.0.1` | backport, after 26.1.2 is feature complete |
| 1.20.1 | Forge, Fabric (no NeoForge) | this folder, branch `1.20.1` | `1.20.1-2001.4.0.1` | ported 2026-10-23, see "This branch: 1.20.1"; untested in game |
| 26.2, 26.3 | Fabric, NeoForge | — | **none: CoolCatLib has no build for them** | blocked until CoolCatLib is ported |

The shared catalogs `26.2-v1` and `26.3-v1` exist (Glazed Menu uses them), but this mod requires Core at runtime, so 26.2+ waits on a CoolCatLib port. Finish all features on 26.1.2 before backporting.

## Build setup (copied from `../Starwoven-26.1.2` and `../GlazedMenu-26.1.2`; do not restructure)

- Build logic is **not in this repo**: `settings.gradle`, `build.gradle` and each module's `build.gradle` apply the remote scripts `ixDarkLorD/ModResources/gradle/v4/*.gradle`. Never copy or edit them; project tweaks go in the module's own `build.gradle`.
- Versions come from the catalog `net.ixdarklord:sharedcatalogs:${dependencies_catalog_version}` (`libs.*`): Minecraft 26.1.2, Java 25, loaders, Fabric API. Ask before hardcoding a version the catalog lacks.
- Architectury Loom **no-remap** (26.1 is unobfuscated: no mappings, `implementation`/`compileOnly` rather than `mod*` configurations), Gradle 9.4 wrapper.
- `gradle.properties` keys are read by the shared scripts by name; keep them. `mod_version` is a plain build number and the Minecraft version goes in front everywhere (`26.1.2-1`). `project_upload_debug=true` stays until the author says to publish.
- Each loader module needs its own `gradle.properties` (`loom.platform`, `platform_name`); the shared scripts fail without it. The root `CHANGELOG.md` is required too (the scripts read it at configuration time, and it is bundled into the jars).
- The CoolCatLib in Maven Local is published from the working tree of `../CoolCatLib-26.1.2` (republished 2026-10-14 for its Armature system; that build moved Canvas's asset namespace to `coolcatlib`, so shaders import `coolcatlib:sky.glsl`, `coolcatlib:vfx.glsl`, `coolcatlib:screen_effect.glsl`). Don't run Gradle with `--refresh-dependencies --offline`: it drops Loom's cached Minecraft metadata and the next offline build fails until one runs online.
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

Source on this machine, one checkout per Minecraft version: `../CoolCatLib-26.1.2` (branch `26.1.2`), `../CoolCatLib-1.21` (1.21.1), `../CoolCatLib-1.20.1`. Core is `core/{common,fabric,neoforge[,forge]}`; the public API is `core/common/src/main/java/net/ixdarklord/coolcatcore/api`. The API differs between versions (1.20.1 has its own `network.codec` and `CustomPacketPayload`, `ClientGuiEvents.RenderHud` takes `GuiGraphics, float`...), so read the checkout for the version being worked on. Canvas (`coolcatcanvas`, `canvas/` in the same checkouts, API under `canvas/common/.../coolcatcanvas/api`) is required too: `ScreenEffects.tint`, `CameraShake`, `WorldParticles` / `ParticleSpec` / `VfxMaterial` / `Curve` and `Easing`, used from `client/fx/SleepEffects` and the HUD. Its camera shake and world particles exist on 26.1.2 only so far: check the 1.21.1 and 1.20.1 checkouts before backporting.

APIs this mod uses (`net.ixdarklord.coolcatcore.api.*`):

- `core.ModConstructor` / `ClientModConstructor`: `onConstructMod()` is where configs, payloads and events are registered (NeoForge collects payloads before common setup). `core.neoforge.NeoForgeModEntrypoint`.
- `config.*`: `Config.builder(MOD_ID, ConfigScope.CLIENT | SERVER)`, values as static `ConfigValue` fields, groups with `push`/`pop` in static blocks, `enumValue` (enums implement `EnumType.Displayable`), `color`, `intValue().range().slider()`, `enabledWhen`. A `SERVER` config is synced to clients on join and on change (`Config.isRemote()`, `ConfigEvents.SYNCED`). Names are translated as `config.whycantisleep.<client|server>.<group>.<key>` (`.title` for the config); tooltips fall back to the `comment`. Screens come from Glazed Menu when installed (`config.client.ConfigScreens`); Core has none of its own.
- `network.Network`: `registerClientbound` / `registerServerbound` (record payloads can derive their codec through `PayloadCodecs`), `sendToPlayer`, `sendToServer`, and `canPlayerReceive(player, type)`, which is how the server tells who has the mod. Clientbound handlers must be lambdas into a `client` package class so a dedicated server never loads it.
- `event.v2.common`: `PlayerEvents` (`JOIN`, `LEAVE`, `START_TICK`, `END_TICK`), `ServerTickEvents`, `ServerLifecycleEvents`, `BlockEvents.BREAK`, `EntityEvents.LOAD`. `event.v2.client`: `ClientTickEvents`, `ClientGuiEvents.RENDER_HUD`, `ClientPlayerEvents`. Each is an `EventInvoker` (`X.END.register(...)`).
- `platform.Platform`.

> **The sleep board is called the "Bedtime" panel since 2026-10-21 (the author's pick).** Players see "Bedtime" on the panel and "Bedtime panel" in the config; the code (`SleepBoard`, `SleepBoardServer`, `SleepBoardPayload`), the lang keys (`gui.whycantisleep.board.*`) and the config keys (`sleepBoard`) keep the old name, so existing config files still load. Where the notes say "sleep board", it is this panel. (**A mod page change: the page still says "Sleep board".**)

## Sides and sync

With the mod on the server, the server is authoritative: it sends the exact blocking mobs, runs the sleep board and nudges, and its config overrides the client's. The client recognises the game's own sleep messages by translation key and can redo the monster check itself (`SleepChecks.blockersOnClient`) when no report came, so the HUD still works if a report is missed (and was written to work on a server without the mod; whether a client-only install connects cleanly on each loader is untested since the split).

Config gating: every client option a server setting controls is declared `enabledWhen(<server value>)` in `WCISClientConfig`. Glazed Menu evaluates dependencies across configs, so the entry shows greyed out ("Depends on …") while the server has it off: that is the "locked" state. Code must read such options through `WCISConfigs.isEnabled(option)`, which walks the same dependency chain, never through `option.get()` alone.

## The sleep HUD

Sleep messages are not shown on the game's action bar. `GuiMixin` cancels the game's sleep lines (recognised by translation key in `client/hud/Explanations`) and `client/hud/SleepNotices` shows a card instead: a `SleepNotice` (kind, title, hint), whose kind gives the accent color, the icon (a vanilla item) and the sound and screen effect (`SleepEffects.onNotice`). The sleep board (`client/board/SleepBoard`) and the cards share `client/hud/HudStyle`. The author first asked for the game's own tooltip look, then (2026-10-14) for beveled pixel-art textures of the mod's own, and on 2026-10-21 for a **simple, clean, dark** look, which is the current one: flat charcoal panels (`card`, a little see-through) with a hairline border and corners rounded by a pixel, no bevels and no shadows, split by thin dividers (`HudStyle.divider`); text drawn without shadows. A message card has its icon in a square tinted its kind's color (`iconSquare`), its title in that color over a gray hint, and a thin line along its foot (`footLine`) that runs down as the card's time runs out (the hiding card's fills over the hide time instead: `SleepNotice.progress`). The sleep board: the title and "2 of 3 asleep", a divider, a bar in segments, one per sleeper the night needs (`HudStyle.segments`, one bar past what fits), a "Still awake" label, rows of face, name, a colored dot and the reason (`HudStyle.dot`), and a footer under a divider: how to nudge, and "Ready" or the cooldown. This was the second of two sketches the author was shown (2026-10-21) and chose. Its colors are the charcoal ones: a near-black, slightly blue panel (`#111317`, a little see-through) in a `#2E3138` hairline, `#22252B` dividers, `#26292F` tracks, green `#4ADE80` sleep segments, neutral gray text a step back (the author tried a charcoal `#2C2C2C` / plum / wine / rose palette on 2026-10-21 and asked to go back to this one). Icons fade with their card: the game draws GUI items from an atlas at full opacity, so `ItemFade.item` records the opacity on the item's render state (`GuiItemRenderStateMixin`) and `GuiRendererMixin` blits it with that color, premultiplied (all four channels), as the atlas is. The sprites are under `textures/gui/sprites/hud/`, drawn by `tools/gen_hud_textures.ps1`. Keep to it: add a new panel there rather than drawing gradients in code; small flat fills (dividers, dots, icon squares, the foot line, a hover highlight) are fine. Vanilla items stay the cards' icons, `PlayerFaceExtractor` the players' faces. `tools/gen_arrow_texture.ps1` draws the HUD arrows.

## Hiding under a bed (`hiding/`, the author's request, 2026-10-21)

Sneaking and using a bed with both hands empty crawls under it instead of sleeping, if there is a floor under it: a solid top on the block under each half (`Hiding.hasFloor`; a bed over air answers "There's no floor under this bed to hide on.", and breaking the floor brings a hider out). The server's `allowHiding` turned off brings out anyone hiding. `HidingServer` listens to CoolCatLib's `InteractionEvents.USE_BLOCK` on both sides (the client answers too, so it predicts neither a sleep nor the dimension warning; `SleepClient.onUseBlock` also steps aside). The player is held flat on the floor halfway along the bed (the crawling pose, `PlayerMixin.updatePlayerPose`, on every client too: `HidingPayload` tells everyone in the level) and can look but not move (`LocalPlayerMixin`), nor use or start breaking any block (`USE_BLOCK` answers `FAIL` and `ATTACK_BLOCK` interrupts, on both sides, so a click on the bed overhead never becomes a try to sleep); the first-person camera sits beneath the frame, midway between the floor and the bed's underside, with the near plane drawn in to 0.02 so looking up or down never sees through the bed or the floor (`CameraMixin`); in third person the view orbits a point a block over the bed instead of the player's eyes beneath it (also `CameraMixin`). The hiding body is held still: its yaw along the bed, head to the pillow end, on both sides after each tick (`Hiding.holdBody`), and drawn flat on the floor (`AvatarRendererMixin.getRenderOffset` lowers the crawling pose 0.18, which otherwise hangs against the bed's underside), arms and legs straight in line with the body, the head following the look (90 degrees either way, a small nod; not frozen, and not round like an owl) (`AvatarRendererMixin` marks the render state through `HidingPose`, `HumanoidModelMixin` poses it), since the crawling pose lifts the head and paddles the arms out through the bed. Under the bed with monsters near, trying to walk or jump, or swinging the view about, makes noise (`HidingClient.listen`, 0 to 1, falling while still): past 0.12 the card turns to a red "Hold still! They'll hear you. Don't move until they're gone" with the noise as its foot line, back under 0.04 to the warning. Noise has no other effect yet. With a monster within 12 blocks (what the client can see) a hiding player trembles (a quick uneven 0.012 offset) and their card turns from "Hiding under the bed / All quiet out there" to a red "Something's out there / Stay still and don't make a sound" (`SleepNotice.Kind.HIDING_DANGER`, swapped in place by `SleepNotices.replace`); when the hide time passes with nothing to shake off, the line is "All quiet. Nothing came looking for you." (`gui.whycantisleep.hide.nothing`). The hiding card shows the item of the very bed hidden under (`HidingPayload` carries the bed's head) and the hand is hidden (`GameRendererMixin`). Nothing can take them as a target, or keep them as one, except what is in `#whycantisleep:ignores_hiding` (the Wither, the Ender Dragon, the Warden, Elder Guardians; Behind Closed Eyes adds its Gardener from its own data): `TargetingConditionsMixin` (every target goal and sensor) and `LivingEntityMixin.canAttack` (keeping a target) ask `Hiding.hiddenFrom(looker, target)`, which knows who is looking (the author, 2026-10-21: hiding doesn't work on bosses). After the server's hide time (15 s) the hunters still within 48 blocks and the monsters within 12 of the bed vanish in smoke (`HidingEvents.SHAKEN_OFF` first), except persistent mobs (named, tamed, kept) and the `#whycantisleep:ignores_hiding` tag (the Wither, the Ender Dragon, the Warden, Elder Guardians). Sneaking again (`LeaveHidingPayload`), any damage (CoolCatLib `LivingEvents.DAMAGE`), the bed breaking, dying, sleeping or leaving brings them out where they went in (else the bed's stand-up spot). The card while hiding and the "it's quiet now" line are the sleep HUD's (`SleepNotice.Kind.HIDING`, `gui.whycantisleep.hide.*` messages).

## Hiding spots, and what goes with hiding (2026-10-21)

The section above was written for beds alone; hiding now works for any **hiding spot** (`api/HidingSpot`, `HidingSpotType`, `HidingSpots`, `HidingPosture`). `HidingServer` asks `HidingSpots.typeFor(state)` on both sides (registered types first, then data pack ones, server only) and the type's `find` on the server: a spot, a refusal message, or nothing (the block's own use goes ahead). A spot carries its anchor block, where the player is held, the way they face, a posture (`LYING`, `STANDING`, `TUCKED`), the view's height, a squint, a hide-time scale, the card's icon and "where" text. `HidingPayload` sends all of it to every client, kept as a `HidingView` per player (`Hiding.view`), which the pose, camera and render mixins read. Crouched in front of a table, a sneak-use that lands on the floor under (or just past) a spot counts as using the spot.

- **Built-in spots** (`hiding/BuiltinHidingSpots`): under a **bed** (lying, needs a floor); under a **table** (two top slabs or upside-down stairs side by side, carpet on both, floors under both; the carpet can be clicked too); inside a **double chest** (tucked; a single one refuses "too small"); in a **closet** (a door with a 1x2 nook behind it, walled on three sides, roofed and floored; standing, facing the door, which is shut behind them).
- **Data pack spots** (`hiding/DataHidingSpots`, a reload listener over `data/<ns>/whycantisleep/hiding_spot/*.json`; the format is in `HidingSpots`' Javadoc).
- **Squint**: the view narrows between dark eyelids (one stretched sprite, `hud/eyelids/<style>`, from `tools/gen_eyelids.py`, in the client config's `eyelidStyle` (`config/EyelidStyle`; the author, 2026-10-24): `smooth` (512x256, a smooth edge), `stepped`, `dithered`, and the default `pixel` (128x64 pixel art, its edge in four stepped tones dithered into each other); `<style>_glow`, a band along that edge, is drawn over it in red only while the heartbeat minigame shows, `HidingMinigame.shown`; the author, 2026-10-24), eased in and out over 14 ticks (sliding out from where the lids are, and widening as they go until the pinched corners are off the screen: `EYELIDS_CLEAR`), with a slow sway, a tighter squint while monsters are near, and a blink every 3.5 to 10 s (`HidingClient.renderEyelids`). Drawn first in the HUD (`GuiMixin` at the head of `Gui.extractRenderState`), so the HUD stays over it and it stays with F1 (the author, 2026-10-21).
- **Chest**: the lid is raised a crack while someone is in (`ChestBlockEntityMixin` raises `getOpenNess`, `HidingClient.lidAjar`), and the player is drawn as only the band of their head with the eyes, cut from their skin and drawn in the gap (`ChestPeek`, from `LivingEntityRendererMixin` in place of the model): it rises as the lid lifts, turns a little with the look, bobs, and shivers near monsters. The first-person view sits in the gap, 0.12 ahead of the player (`CameraMixin`).
- **Breath** (`HOLD_BREATH`, V): no noise while held, 8 s of breath, a gasp (loud) when it runs out, back three times as slowly. **Noise** goes to the server in tenths (`NoisePayload`, `HidingEvents.NOISE_CHANGED`); at 1 with a monster within 12 of the spot, the player is found (`Reason.HEARD`) and the nearest comes for them.
- **Cover**: each full block on a side of the spot (not the spot's own blocks) takes 10% off the hide time, at most three. **Critters**: a 5% chance a silverfish or cave spider is already in the spot (`Reason.STARTLED`). **Prowling** (`HidingAmbience`): monsters within 10 blocks are heard while hiding, muffled footsteps on what they walk on and shuffling when they stand, and lying low a puff of shadow where each step lands.
- The hiding card comes back by itself after another card (the "it's quiet now" line) took its place.

## Nights (`sleep/SleepNights`, `Bedroom`, 2026-10-21)

Every sleep is followed from lying down (`ServerPlayerMixin.startSleepInBed`; the trouble is read at its head, before the game wipes the time since rest) to waking (`ServerPlayerMixin.stopSleepInBed`, which every way of waking passes through; a night skip is known from `ServerLevelMixin.wakeUpAllPlayers`). On waking it is scored (`api/SleepQuality`, from 75): a closed room (+10) or the open (-5), dark corners (-10), a bright light at the pillow (-5), monsters within 24 (-6 each, -30 at most), creepy (-20) or loud (-15) music from a jukebox within 16, both counting when both played, or else calm music (+10) (`#whycantisleep:creepy`, `#whycantisleep:loud`, `#whycantisleep:calm` jukebox song tags; the author, 2026-10-21: loud, noisy music ruins sleep too), restless (-10) or sleepless (-20), a nightmare (-15), waking early (-25), the bedtime routine (+10). The room is a flood fill of the air round the bed through open doors (`Bedroom`): closed if it stays under 600 blocks, within 12, and never sees the sky; lit if no block in it has block light 0. `WokePayload` tells the client, which shows a card (`SleepMornings`).

- **Night terrors**: restless players lying down have a 30% chance (twice that sleepless) of one (`TerrorPayload`, `NightTerrorClient`: red dark, a quickening heartbeat, a whisper, eyes opening, a stare, a shriek and a jolt) and wake shaken (nausea, slowness). `/whycantisleep nightmare [players]` (operators, level 2; `WhyCantISleepApi.nightmare`) plays one on demand: for a sleeper it counts as the night's terror, for someone awake it is only seen.
- **Well rested**: a closed, lit room slept in till morning gives Regeneration for 30 s.
- **Wake-up reasons** (`api/WakeReason`): morning, got up (no card), hurt, bed gone, other.
- **Sleep debt moon** (`SleepDebtMoon`, `TroublePayload` every 5 s): top left, shown from one day without sleep, full at three (pulsing), with the days beside it.

## Sleep effects, modifiers, status and tooltips (2026-10-21)

- **Sleep quality modifiers** (`api/SleepModifier`, `SleepModifiers`, `SleepContext`): every night is scored by every modifier from what it was like; those with a say become its factors. The mod's own 15 are **data** (`data/whycantisleep/whycantisleep/sleep_modifier/*.json`, loaded by `sleep/DataSleepModifiers`; the format, conditions and all, is in `SleepModifiers`' Javadoc); mods may also register in code. A data file with an existing id replaces it; `"disabled": true` takes it out. The routine modifier counting (and `wellRested` on) is what gives Regeneration. Labels are Title Case names (the author, 2026-10-21: well written, capitalized).
- **Sleep effects** (`api/SleepEffect`, `SleepEffects`): 22 data files (`.../sleep_effect/*.json`, `sleep/DataSleepEffects`): 9 positive, 8 negative, 5 neutral. On waking in the morning, at `sleepEffectChance`, the kind is drawn (positive weight = score - 40, negative = 60 - score, neutral 20), then one of it by weight (the rarity's: common 60, uncommon 25, rare 10, epic 4, legendary 1, unless given) among those whose quality range fits. `SleepEvents.SLEEP_EFFECT` can change or cancel it. Shown on the card and in the status popup with `hud/buff`, `hud/debuff` or `hud/neutral` (`tools/gen_buffs.py`), named in the rarity's color.
- **Data generation** (`api/datagen`): `SleepEffectBuilder`, `SleepModifierBuilder` and the abstract `SleepDataProvider` write those files from Java; their keys must stay in step with the two loaders' codecs.
- **Things by the bed** (`sleep/SleepDecor`): blocks within 6 (3 up and down) whose item is in `#whycantisleep:soothing` (+2 each, +10 at most) or `#whycantisleep:unsettling` (-3 each, -15 at most); a potted plant counts as its plant. Their tooltips close with a section of their own (`SleepTooltip`, on CoolCatLib's `ItemTooltipEvents.APPEND`, after every other mod's lines; the author, 2026-10-21): the buff or debuff icon and the meter's pips are GUI-atlas sprites set in the text (`Component.object` with an `AtlasSprite`; `hud/pip_*` from `tools/gen_buffs.py`), with the points and the cap.
- **Sleep status** (`client/SleepStatus`): a moon button in the top right of the survival inventory's player preview (`InventoryScreenMixin` draws, `AbstractRecipeBookScreenMixin` takes clicks, `AbstractContainerScreenAccessor` for the panel's corner); its popup shows the trouble (from `TroublePayload`) and the last night (the last `WokePayload`, kept for the session).
- `/whycantisleep nightmare` and the loud music tag are above, in Nights.

## Highlights, room clearing and multiplayer (2026-10-21)

- **Threat ranking**: of the blockers, the most dangerous kind (`ThreatType.danger`: creeper 5, ender and raider 4, undead 3, others 2), then the nearest, has a brighter, throbbing outline and a larger pointer (`Blockers.isTop`).
- **Rings**: a blocker in view but behind blocks gets a pulsing ring where it is (`hud/blocker_ring`, `tools/gen_ring.py`) instead of the hollow arrow (the hollow frame of `blocker_arrow.png` is no longer drawn); arrows are only for blockers outside the view.
- **Trails** (`BlockerTrails`): each step of a highlighted blocker leaves a print in its color, shown as dust for 15 s.
- **Room clearing** (`RoomClearing`): every blocker of a refused sleep dealt with within the clearing time, and nothing else near, is "The room is safe"; standing within 3.5 of the bed the player lies straight down. The dying still count for the game's check, so it waits for them to be gone.
- **Sleep rules** (`SleepChecks.applyRules`, applied to the game's own check by `ServerPlayerMixin`'s `@ModifyVariable` on its monster list, and to the highlights): `#whycantisleep:blocks_sleep` (any mob), `#whycantisleep:does_not_block_sleep`, and a bed within 3 of another player's who is asleep lets `sharedBedTolerance` blockers by.
- **Sleep vote** (`SleepVotes`): while no more than `voteMaxAwake` players are up, the board has a vote row; once everyone in bed has voted those up are asked in chat ("Go ahead" / "Not yet", clickable, `/whycantisleep vote yes|no`); a "Not yet" ends it for a minute, no answer in `voteWait` seconds skips the night the game's way (`ServerLevelAccessor.wakeUpAllPlayers`). Not tested with two players yet.

## The API (`api/`, for other mods; Behind Closed Eyes uses it)

Published to Maven Local as `net.ixdarklord.whycantisleep:whycantisleep-common` (`gradlew assemble publishToMavenLocal`); other mods compile against it `compileOnly` and call it only behind `Platform.isModLoaded("whycantisleep")`, from a class of their own. Keep it stable: rename or remove nothing in `api/` without asking.

- `WhyCantISleepApi`: `sleepTrouble(player)`, `sleepBlockers(player, bed)` (the game's own check, with the mod's rules), `lastSleepQuality(player)`, `isHiding(player)` (both sides), `hidingSpot`, `hiddenTicks`, `hideTicks(player)`, `baseHideTicks()`, `hidingNoise`, `startHiding` (by block or by spot), `stopHiding`.
- Hiding spots: `HidingSpots.register(HidingSpotType)` (on both sides), `HidingSpot` (with `withSquint`), `HidingPosture`; data pack spots need no code.
- `SleepTrouble` (failed attempts in a row, from `SleepTroubles`, kept for the server's session; ticks since rest, the game's `TIME_SINCE_REST` statistic) and its `Level`: `RESTED`, `RESTLESS` (2 refusals or 2 days), `SLEEPLESS` (5 or 3 days, when phantoms come).
- `SleepEvents` (CoolCatLib `event.v1.Event`): `SLEEP_FAILED` (the problem, the blockers, the trouble), `LAY_DOWN`, `TROUBLE_CHANGED`, `SCORE_QUALITY` (add or remove factors), `WOKE` (reason, quality, shaken, well rested), `NIGHT_SKIPPED`, `NIGHT_VOTED`, `NIGHT_TERROR` (cancellable), `ROOM_SAFE`.
- `HidingEvents`: `START` (cancellable, `setSpot` to change the spot), `STOP` (with a `Reason`: left, hurt, interrupted, heard, startled, forced), `SHAKEN_OFF` (a mutable list of hunters: `keep`, `add`), `NOISE_CHANGED`.

## Config (the names and defaults on the mod page)

Client (`WCISClientConfig`, `config/whycantisleep-client.toml`):

| Option | Default | Locked by |
|---|---|---|
| Highlight monsters | on | Allow highlights |
| Threat colors | on | |
| Highlight color | red | |
| Off-screen arrows | on | Allow arrows |
| Arrow size | medium (small / medium / large) | |
| Eyelid style (`eyelidStyle`) | pixel art (smooth / stepped / dithered / pixel art) | |
| Action bar info | on | |
| Sleep explanations | on | |
| Dimension warning | on | |
| Bedtime panel (`sleepBoard`) | on | Bedtime panel (server) |
| Blocker trails | on (**new, not on the mod page yet**) | Highlight monsters |
| Sleep debt moon | on (**new, not on the mod page yet**) | |
| Morning card | on (**new, not on the mod page yet**) | Sleep quality |
| Sleep status button (`statusButton`) | on (**new, not on the mod page yet**) | |

Server (`WCISServerConfig`, `config/whycantisleep-server.toml`, synced):

| Option | Default |
|---|---|
| Allow highlights | on |
| Highlight duration | 3 s (1–10) |
| Allow arrows | on |
| Show to everyone | off (vanilla glowing effect for players without the mod) |
| Room clearing | on (**new, not on the mod page yet**) |
| Clearing time | 10 s (3–60) (**new, not on the mod page yet**) |
| Auto sleep | on (**new, not on the mod page yet**) |
| Bedtime panel (`sleepBoard`) | on |
| Allow nudges | on |
| Sleep vote | on (**new, not on the mod page yet**) |
| Most players up for a vote (`voteMaxAwake`) | 1 (1–10) (**new, not on the mod page yet**) |
| Vote answer time (`voteWait`) | 30 s (5–120) (**new, not on the mod page yet**) |
| Nudge cooldown | 30 s |
| Allow hiding | on (**new, not on the mod page yet**) |
| Hide under beds / tables, in chests / closets (`hideUnderBeds`, `hideUnderTables`, `hideInChests`, `hideInClosets`) | on (**new, not on the mod page yet**) |
| Hide time | 15 s (3–120) (**new, not on the mod page yet**) |
| Hiding clears monsters (`hidingClearsMonsters`) | on; only when every other player within 32 blocks is hiding too (**new, not on the mod page yet**) |
| Cover | on (**new, not on the mod page yet**) |
| Critter chance | 5% (0–100) (**new, not on the mod page yet**) |
| Noise gives away | on (**new, not on the mod page yet**) |
| Heartbeat minigame (`heartbeatMinigame`) | on (**new, not on the mod page yet**) |
| Monsters search (`hunterSearch`) | on (**new, not on the mod page yet**) |
| Search chance (`searchChance`) | 25% on normal (half on easy, x1.5 on hard, none on peaceful; noise adds) (0–100) (**new, not on the mod page yet**) |
| Sleep quality | on (**new, not on the mod page yet**) |
| Night terrors | on (**new, not on the mod page yet**) |
| Terror chance | 30% (0–100) (**new, not on the mod page yet**) |
| Well rested | on (**new, not on the mod page yet**) |
| Sleep effects | on (**new, not on the mod page yet**) |
| Sleep effect chance | 50% (0–100) (**new, not on the mod page yet**) |
| Shared bed tolerance | 2 (0–10) (**new, not on the mod page yet**) |
| Advancements | on (its own `advancements` group since the split; none exist yet) |
| Give the guide book (`giveGuideBook`) | on (**new, not on the mod page yet**) |

Moved to Behind Closed Eyes with the split (a mod page change: the author updates the page): Scary content, Whispers, Whisper volume, Under-the-bed scares, Screen shake, Status indicators, Inventory status; Scare chance, Bed creature, Mood and lucidity, PvP guilt.

## Features, in build order

1. Monster highlights: exactly the mobs that blocked the sleep (vanilla check range only) glow for the configured time, colored by threat type or one color.
2. Off-screen arrows around the crosshair for blockers that are off-screen or behind walls, and action bar text ("3 monsters nearby - closest: Zombie, 6 blocks below").
3. An explanation and fix hint for every sleep failure (monsters, wrong time, too far, obstructed, occupied, other players awake) and a warning before sleeping in the Nether/End.
4. Multiplayer bedtime panel (the sleep board in the code; server): who is awake and why, progress toward `playersSleepingPercentage`, and nudges with a cooldown.
5. Advancements (the whispers and the "monster under the bed" scare moved to Behind Closed Eyes): "Monsters in the Closet" (blocked by monsters 10 times), "Insomniac" (50 failed sleeps), "Night Shift" (kill a blocker within 5 seconds).

After each feature: build every loader, test in game when possible, add a `CHANGELOG.md` line, commit. Finish with a README (features, config options, supported versions, license).

## Code conventions (from the author's other mods)

- These are **26.1 names**: `net.minecraft.resources.Identifier` (not `ResourceLocation`), `GuiGraphicsExtractor` and `extract*` (not `GuiGraphics` / `render*`), GUI text colors need alpha bits. If unsure of a vanilla API, read the decompiled 26.1.2 source (`gradlew :common:genSourcesWithVineflower`, then the `*-sources.jar` under `.gradle/loom-cache/minecraftMaven/`) rather than guessing from older versions. If that jar is empty, the Fabric one at `~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged-deobf/26.1.2/*-sources.jar` has the sources.
- Classes are `final` with a private constructor when they only hold statics; a short Javadoc on each class says what it is for. Comments explain why, in plain sentences.
- Config classes are `WCIS<Scope>Config`; all user-facing text is a translation key in `assets/whycantisleep/lang/en_us.json`, kept sorted by key. Keys: `config.whycantisleep.*`, `gui.whycantisleep.*`, `advancements.whycantisleep.*`, `subtitles.whycantisleep.*`.
- The server is authoritative: clients send intentions (a nudge), and the server checks permission, cooldown and state. Size-cap every serverbound payload.
- Java 25 language features are fine. No Lombok, and no new libraries without asking.
- `CHANGELOG.md`: `## v<mc>-<build> Release | <date>` with emoji subsections (✨ New Features / 🐛 Bug Fixes / 🛠️ Changes).
- Never commit `build/`, `run/`, `.gradle/` or secrets.
