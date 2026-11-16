package net.ixdarklord.whycantisleep.api;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

/**
 * What a night was like, as its sleeper wakes: what every {@link SleepModifier} scores it from.
 *
 * @param player       the sleeper
 * @param bed          the bed they slept in
 * @param reason       why they woke
 * @param sleptTicks   how long they lay asleep
 * @param enclosed     whether the room was closed: walled, roofed, its doors shut, not under the open sky
 * @param lit          whether the closed room was lit all through, with no dark corner a monster could spawn in
 * @param pillowLight  the block light at the pillow, 0 to 15
 * @param soothing     how many soothing things were placed near the bed ({@code #whycantisleep:soothing})
 * @param unsettling   how many unsettling things were ({@code #whycantisleep:unsettling})
 * @param monsters     the most monsters prowling within 24 blocks at any time of the night
 * @param calmMusic    whether calm music played nearby ({@code #whycantisleep:calm} songs)
 * @param creepyMusic  whether creepy music did ({@code #whycantisleep:creepy})
 * @param loudMusic    whether loud music did ({@code #whycantisleep:loud})
 * @param trouble      how badly they had been sleeping before they lay down
 * @param nightmare    whether they had a night terror
 * @param guards       the most night guards about the bed at any time of the night: iron golems near it, and the
 *                     sleeper's own wolves and cats sitting by it
 * @param nightlight   whether a soft light was near the bed ({@code #whycantisleep:nightlights}, lit, within three
 *                     blocks), short of too bright at the pillow
 * @param napped       whether the sleeper napped in the day since their last night
 */
public record SleepContext(ServerPlayer player, BlockPos bed, WakeReason reason, int sleptTicks, boolean enclosed, boolean lit, int pillowLight,
                           int soothing, int unsettling, int monsters, boolean calmMusic, boolean creepyMusic, boolean loudMusic,
                           SleepTrouble.Level trouble, boolean nightmare, int guards, boolean nightlight, boolean napped) {
    /** A night with no night guards about (as the context was before they counted). */
    public SleepContext(ServerPlayer player, BlockPos bed, WakeReason reason, int sleptTicks, boolean enclosed, boolean lit, int pillowLight,
                        int soothing, int unsettling, int monsters, boolean calmMusic, boolean creepyMusic, boolean loudMusic,
                        SleepTrouble.Level trouble, boolean nightmare) {
        this(player, bed, reason, sleptTicks, enclosed, lit, pillowLight, soothing, unsettling, monsters, calmMusic, creepyMusic, loudMusic,
                trouble, nightmare, 0);
    }

    /** A night with no nightlight and no nap before it (as the context was before they counted). */
    public SleepContext(ServerPlayer player, BlockPos bed, WakeReason reason, int sleptTicks, boolean enclosed, boolean lit, int pillowLight,
                        int soothing, int unsettling, int monsters, boolean calmMusic, boolean creepyMusic, boolean loudMusic,
                        SleepTrouble.Level trouble, boolean nightmare, int guards) {
        this(player, bed, reason, sleptTicks, enclosed, lit, pillowLight, soothing, unsettling, monsters, calmMusic, creepyMusic, loudMusic,
                trouble, nightmare, guards, false, false);
    }
}
