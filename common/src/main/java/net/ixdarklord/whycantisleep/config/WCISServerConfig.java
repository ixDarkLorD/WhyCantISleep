package net.ixdarklord.whycantisleep.config;

import net.ixdarklord.coolcatcore.api.config.Config;
import net.ixdarklord.coolcatcore.api.config.ConfigBuilder;
import net.ixdarklord.coolcatcore.api.config.ConfigScope;
import net.ixdarklord.coolcatcore.api.config.ConfigValue;
import net.ixdarklord.whycantisleep.WhyCantISleep;

/**
 * What the server allows and decides. CoolCatLib syncs it to every client that joins and whenever it changes, so on a
 * server with the mod these values override the client's; a client on a server without the mod reads its own file.
 */
public final class WCISServerConfig {
    private static final ConfigBuilder BUILDER = Config.builder(WhyCantISleep.MOD_ID, ConfigScope.SERVER)
            .comment("Why Can't I Sleep? server settings. On a server these override each player's client settings.");

    static {
        BUILDER.push("highlights", "Highlighting the monsters that blocked a sleep.");
    }
    public static final ConfigValue<Boolean> ALLOW_HIGHLIGHTS = BUILDER.bool("allowHighlights", true)
            .comment("Whether players may see the monsters that blocked their sleep glow.").build();
    public static final ConfigValue<Integer> HIGHLIGHT_DURATION = BUILDER.intValue("highlightDuration", 3)
            .range(1, 10).slider().comment("Seconds the blocking monsters glow.").build();
    public static final ConfigValue<Boolean> ALLOW_ARROWS = BUILDER.bool("allowArrows", true)
            .comment("Whether players may see arrows pointing to blocking monsters that are off-screen or behind walls.").build();
    public static final ConfigValue<Boolean> SHOW_TO_EVERYONE = BUILDER.bool("showToEveryone", false)
            .comment("Shows highlights to players without the mod too, using the vanilla glowing effect.").build();
    public static final ConfigValue<Boolean> ROOM_CLEARING = BUILDER.bool("roomClearing", true)
            .comment("When monsters block a sleep and the player deals with all of them in time, tells them the room is safe.").build();
    public static final ConfigValue<Integer> CLEARING_TIME = BUILDER.intValue("clearingTime", 10)
            .range(3, 60).slider().comment("Seconds the player has to deal with the monsters that blocked their sleep.").build();
    public static final ConfigValue<Boolean> AUTO_SLEEP = BUILDER.bool("autoSleep", true)
            .comment("Whether a player who cleared the room while standing by the bed lies straight down in it.").build();
    static {
        BUILDER.pop();
        BUILDER.push("sleepBoard", "The multiplayer bedtime panel, nudges and votes.");
    }
    public static final ConfigValue<Boolean> SLEEP_BOARD = BUILDER.bool("sleepBoard", true)
            .comment("Whether players in bed are shown who is still awake, and why.").build();
    public static final ConfigValue<Boolean> ALLOW_NUDGES = BUILDER.bool("allowNudges", true)
            .comment("Whether a player in bed may nudge an awake player with a sound and a message.").build();
    public static final ConfigValue<Boolean> SLEEP_VOTE = BUILDER.bool("sleepVote", true)
            .comment("Whether the players in bed can vote from the board to skip the night while a player or two is still up. Those still up are asked first, and can say not yet.").build();
    public static final ConfigValue<Integer> VOTE_MAX_AWAKE = BUILDER.intValue("voteMaxAwake", 1)
            .range(1, 10).comment("The most players that may still be up for the players in bed to vote.").build();
    public static final ConfigValue<Integer> VOTE_WAIT = BUILDER.intValue("voteWait", 30)
            .range(5, 120).comment("Seconds the players still up have to answer; with no answer the night is skipped.").build();
    public static final ConfigValue<Integer> NUDGE_COOLDOWN = BUILDER.intValue("nudgeCooldown", 30)
            .range(0, 600).comment("Seconds a player waits between nudges.").build();
    static {
        BUILDER.pop();
        BUILDER.push("hiding", "Hiding under beds and tables, in chests and closets.");
    }
    public static final ConfigValue<Boolean> ALLOW_HIDING = BUILDER.bool("allowHiding", true)
            .comment("Whether players may hide (sneak and use a bed, table, double chest or closet door with empty hands), where nothing can see them.").build();
    public static final ConfigValue<Boolean> HIDE_UNDER_BEDS = BUILDER.bool("hideUnderBeds", true)
            .comment("Whether players may hide under beds.").enabledWhen(ALLOW_HIDING).build();
    public static final ConfigValue<Boolean> HIDE_UNDER_TABLES = BUILDER.bool("hideUnderTables", true)
            .comment("Whether players may hide under tables (two top slabs or upside-down stairs side by side, with carpet on them).").enabledWhen(ALLOW_HIDING).build();
    public static final ConfigValue<Boolean> HIDE_IN_CHESTS = BUILDER.bool("hideInChests", true)
            .comment("Whether players may hide in double chests.").enabledWhen(ALLOW_HIDING).build();
    public static final ConfigValue<Boolean> HIDE_IN_CLOSETS = BUILDER.bool("hideInClosets", true)
            .comment("Whether players may hide in closets: a door, or a double door, with a nook behind it walled in by blocks or trapdoors.").enabledWhen(ALLOW_HIDING).build();
    public static final ConfigValue<Integer> HIDE_TIME = BUILDER.intValue("hideTime", 15)
            .range(3, 120).slider().comment("Seconds a player must stay hidden before the monsters after them give up and vanish.").build();
    public static final ConfigValue<Boolean> HIDING_CLEARS_MONSTERS = BUILDER.bool("hidingClearsMonsters", true)
            .comment("Whether, once the hide time is up, the monsters after a hiding player and those about their spot (the ones keeping them from "
                    + "sleeping in that bed) vanish. Only when every other player near is hiding too; else they just lose the player.").enabledWhen(ALLOW_HIDING).build();
    public static final ConfigValue<Boolean> COVER = BUILDER.bool("cover", true)
            .comment("Walls close around a hiding spot shorten the hide time: a bed in a corner shakes monsters off sooner.").build();
    public static final ConfigValue<Integer> CRITTER_CHANCE = BUILDER.intValue("critterChance", 5)
            .range(0, 100).slider().comment("Percent chance a silverfish or cave spider is already in a hiding spot, and chases the player out.").build();
    public static final ConfigValue<Boolean> NOISE_GIVES_AWAY = BUILDER.bool("noiseGivesAway", true)
            .comment("Whether a hiding player who makes too much noise with monsters near is found, and pulled out.").build();
    public static final ConfigValue<Boolean> HEARTBEAT_MINIGAME = BUILDER.bool("heartbeatMinigame", true)
            .comment("Whether a player who hides with monsters chasing them must keep their heart slow: clicking on the beat, three misses and they gasp.").enabledWhen(ALLOW_HIDING).build();
    public static final ConfigValue<Boolean> HUNTER_SEARCH = BUILDER.bool("hunterSearch", true)
            .comment("Whether a monster that chased a player into a hiding spot comes to search it, looks about, and then finds them or walks off.").enabledWhen(ALLOW_HIDING).build();
    public static final ConfigValue<Integer> SEARCH_CHANCE = BUILDER.intValue("searchChance", 25)
            .range(0, 100).slider().comment("Percent chance a monster that chased a player into a hiding spot, and came to search it, finds them and drags them out. "
                    + "This is on normal difficulty: half on easy, half as much again on hard, never on peaceful. The noise they make adds to it.")
            .enabledWhen(HUNTER_SEARCH).build();
    static {
        BUILDER.pop();
        BUILDER.push("nights", "How well players sleep, and what happens in the night.");
    }
    public static final ConfigValue<Integer> SHARED_BED_TOLERANCE = BUILDER.intValue("sharedBedTolerance", 2)
            .range(0, 10).comment("How many monsters a bed lets by when another player sleeps in a bed beside it.").build();
    public static final ConfigValue<Boolean> SLEEP_QUALITY = BUILDER.bool("sleepQuality", true)
            .comment("Scores each night's sleep, 0 to 100, from the room, the light, monsters about, music and sleep trouble, and tells the player in the morning.").build();
    public static final ConfigValue<Boolean> NIGHT_TERRORS = BUILDER.bool("nightTerrors", true)
            .comment("Whether restless players can have a night terror when they lie down, and wake up shaken.").build();
    public static final ConfigValue<Integer> TERROR_CHANCE = BUILDER.intValue("terrorChance", 30)
            .range(0, 100).slider().comment("Percent chance of a night terror for a restless player; a sleepless one has twice the chance.").build();
    public static final ConfigValue<Boolean> SLEEP_EFFECTS = BUILDER.bool("sleepEffects", true)
            .comment("Whether waking in the morning can leave a sleep effect: positive after good nights, negative after bad ones, neutral after any. Data packs add more.").build();
    public static final ConfigValue<Integer> SLEEP_EFFECT_CHANCE = BUILDER.intValue("sleepEffectChance", 50)
            .range(0, 100).slider().comment("Percent chance of a sleep effect on waking in the morning.").build();
    public static final ConfigValue<Boolean> WELL_RESTED = BUILDER.bool("wellRested", true)
            .comment("Whether sleeping through the night in a closed, lit room (doors shut, no dark corners) leaves the player well rested.").build();
    static {
        BUILDER.pop();
        BUILDER.push("advancements", "The mod's advancements.");
    }
    public static final ConfigValue<Boolean> ADVANCEMENTS = BUILDER.bool("advancements", true)
            .comment("Whether the mod's advancements can be earned.").build();
    static {
        BUILDER.pop();
    }

    public static final Config CONFIG = BUILDER.build();

    private WCISServerConfig() {
    }
}
