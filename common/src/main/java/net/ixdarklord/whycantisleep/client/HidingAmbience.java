package net.ixdarklord.whycantisleep.client;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientPlayerEvents;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientTickEvents;
import net.ixdarklord.whycantisleep.hiding.HidingView;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * What a hiding player hears of the monsters prowling near: their footfalls, muffled, on whatever they walk on, and
 * when they stand about, a shuffle, a scrape. Only the hiding player hears them, louder the nearer the monster. Lying
 * low under a bed or table, they also see each step land: a puff of shadow at the monster's feet, past the gap.
 */
public final class HidingAmbience {
    // How far the prowling is heard, how far a monster walks between footfalls, and how often one standing about shuffles.
    private static final double HEAR_RANGE = 10.0;
    private static final double STRIDE = 1.1;
    private static final int SHUFFLE_ODDS = 90;
    // How near a footfall must be, lying low, to be seen.
    private static final double SEE_RANGE = 5.0;
    private static final Int2ObjectMap<Prowl> PROWLING = new Int2ObjectOpenHashMap<>();

    private HidingAmbience() {
    }

    static void init() {
        ClientTickEvents.END.register(HidingAmbience::tick);
        ClientPlayerEvents.LEAVE.register(player -> PROWLING.clear());
    }

    private static void tick(Minecraft minecraft) {
        HidingView view = HidingClient.localView();
        if (view == null || minecraft.level == null || minecraft.player == null || minecraft.isPaused()) {
            PROWLING.clear();
            return;
        }
        Vec3 ear = minecraft.player.position();
        RandomSource random = minecraft.level.getRandom();
        Int2ObjectMap<Prowl> seen = new Int2ObjectOpenHashMap<>();
        for (Monster monster : minecraft.level.getEntitiesOfClass(Monster.class, minecraft.player.getBoundingBox().inflate(HEAR_RANGE), Monster::isAlive)) {
            Prowl prowl = PROWLING.get(monster.getId());
            if (prowl == null) prowl = new Prowl(monster.position());
            seen.put(monster.getId(), prowl);
            double distance = monster.position().distanceTo(ear);
            if (distance > HEAR_RANGE) continue;
            // Nearer is louder; all of it is low and dull, heard through the frame.
            float volume = (float) (0.06 + 0.22 * (1.0 - distance / HEAR_RANGE));
            Vec3 at = monster.position();
            double moved = at.subtract(prowl.last).horizontalDistance();
            prowl.last = at;
            prowl.walked += moved;
            if (prowl.walked >= STRIDE && monster.onGround()) {
                prowl.walked = 0.0;
                BlockPos under = monster.getOnPos();
                BlockState ground = minecraft.level.getBlockState(under);
                SoundType sound = ground.isAir() ? SoundType.STONE : ground.getSoundType();
                play(minecraft, at, sound.getStepSound(), volume, 0.55F + random.nextFloat() * 0.1F);
                if (view.lying() && distance < SEE_RANGE) {
                    for (int i = 0; i < 3; i++) {
                        minecraft.level.addParticle(ParticleTypes.SMOKE, at.x + (random.nextDouble() - 0.5) * 0.4, at.y + 0.05,
                                at.z + (random.nextDouble() - 0.5) * 0.4, 0.0, 0.005, 0.0);
                    }
                }
            } else if (moved < 0.01 && random.nextInt(SHUFFLE_ODDS) == 0) {
                // Standing about: a shuffle of feet, a scrape of cloth.
                SoundEvent shuffle = random.nextBoolean() ? SoundEvents.WOOL_STEP : SoundEvents.ARMOR_EQUIP_LEATHER;
                play(minecraft, at, shuffle, volume * 0.8F, 0.5F + random.nextFloat() * 0.15F);
            }
        }
        PROWLING.clear();
        PROWLING.putAll(seen);
    }

    private static void play(Minecraft minecraft, Vec3 at, SoundEvent sound, float volume, float pitch) {
        minecraft.level.playLocalSound(at.x, at.y, at.z, sound, SoundSource.HOSTILE, volume, pitch, false);
    }

    private static final class Prowl {
        private Vec3 last;
        private double walked;

        private Prowl(Vec3 last) {
            this.last = last;
        }
    }
}
