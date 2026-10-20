package net.ixdarklord.whycantisleep.between;

import net.ixdarklord.whycantisleep.mind.Mind;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;

/**
 * What the Dark Between's wild creatures leave that is worth keeping ({@link CharmItem}s, registered in WCISItems), each
 * doing for its keeper what the creature did:
 * <ul>
 *     <li>Mare's Ember (Night Mare): breathed in, its swiftness, Speed II for 45 s.</li>
 *     <li>Hollow Fang (Hollow Hound): smells out every hostile creature within 32 blocks, outlined through walls for
 *     15 s. Wears; rests 30 s.</li>
 *     <li>Lullaby Box (Lullaby): its song, the right way round: lucidity and mood restored to whoever winds it, and
 *     some to those within 6 blocks. Wears; rests 2 min.</li>
 *     <li>Gazer's Lens (Gazer): sight in the dark, Night Vision for 2 min.</li>
 *     <li>Stopwatch (Tick-Tock): slips time for every hostile creature within 10 blocks, nearly stopping them for 5 s.
 *     Wears; rests 40 s.</li>
 *     <li>Living Thread (Stitchling): mends a quarter of the wear on whatever is in the other hand.</li>
 *     <li>Gloom Gel (Gloom Jelly): its lightness, Slow Falling for 60 s, for crossing between islands.</li>
 *     <li>Hare's Foot (Dream Hare): the spring of its leaps, Jump Boost II for 60 s, and Luck for 5 min.</li>
 *     <li>Swap Card (Grinning Clown): its trick, changing places with whatever creature its holder looks at within 32
 *     blocks: across a gap between islands, out of a crowd, or to bring something over.</li>
 *     <li>Closet Key (Closet Boogeyman): somewhere to hide, Invisibility for 10 s and every creature after its keeper
 *     within 24 blocks loses them. Wears; rests 1 min.</li>
 *     <li>Huff and Puff (Big Bad Wolf): a breath that blows every creature within 8 blocks away. Wears; rests 15 s.</li>
 *     <li>Dream Sand (Sandman): a moment's real sleep, eyes closed: Regeneration II for 8 s and a lift of mood.</li>
 *     <li>Fairy Dust (Tooth Taker): a lift, a few blocks up and a soft landing, onto a ledge or an island above.</li>
 *     <li>Broodsilk (Broodmother): a line of silk to the block looked at, up to 28 blocks off, that pulls its keeper
 *     there. Wears.</li>
 *     <li>Void Scale (Void Eel): back to where its keeper last stood on solid ground, in this dimension: a way out of
 *     a fall into the void.</li>
 *     <li>Swarm Jar (Hive-Thing): a swarm let loose on every hostile creature within 7 blocks.</li>
 *     <li>Shrinking Skin (Crawlspace): half one's size for a minute, to fit through gaps.</li>
 * </ul>
 */
public final class WildCharms {
    private WildCharms() {
    }

    public static boolean maresEmber(ServerLevel level, Player player, InteractionHand hand) {
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, 45 * 20, 1));
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1.0, player.getZ(), 20, 0.3, 0.5, 0.3, 0.05);
        return true;
    }

    public static boolean hollowFang(ServerLevel level, Player player, InteractionHand hand) {
        for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(32.0), mob -> mob instanceof Enemy)) {
            mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 15 * 20, 0), player);
        }
        level.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 1.2, player.getZ(), 12, 0.3, 0.3, 0.3, 0.02);
        return true;
    }

    public static boolean lullabyBox(ServerLevel level, Player player, InteractionHand hand) {
        for (Player near : level.getEntitiesOfClass(Player.class, player.getBoundingBox().inflate(6.0))) {
            boolean self = near == player;
            float lucidity = self ? 20.0F : 10.0F, mood = self ? 15.0F : 8.0F;
            Mind.update(near, mind -> mind.withLucidity(Math.min(Mind.MAX, mind.lucidity() + lucidity))
                    .withMood(Math.min(Mind.MAX, mind.mood() + mood)));
            level.sendParticles(ParticleTypes.NOTE, near.getX(), near.getY() + 2.1, near.getZ(), 4, 0.4, 0.2, 0.4, 0.5);
        }
        return true;
    }

    public static boolean gazersLens(ServerLevel level, Player player, InteractionHand hand) {
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 120 * 20, 0));
        return true;
    }

    public static boolean stopwatch(ServerLevel level, Player player, InteractionHand hand) {
        boolean any = false;
        for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(10.0), mob -> mob instanceof Enemy)) {
            mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 5 * 20, 4), player);
            mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 5 * 20, 1), player);
            level.sendParticles(ParticleTypes.ENCHANT, mob.getX(), mob.getY() + mob.getBbHeight(), mob.getZ(), 10, 0.3, 0.3, 0.3, 0.2);
            any = true;
        }
        return any;
    }

    public static boolean livingThread(ServerLevel level, Player player, InteractionHand hand) {
        ItemStack mend = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (!mend.isDamageableItem() || mend.getDamageValue() == 0) return false;
        int quarter = Math.max(1, mend.getMaxDamage() / 4);
        mend.set(DataComponents.DAMAGE, Math.max(0, mend.getDamageValue() - quarter));
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1.0, player.getZ(), 8, 0.4, 0.3, 0.4, 0.0);
        return true;
    }

    public static boolean gloomGel(ServerLevel level, Player player, InteractionHand hand) {
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60 * 20, 0));
        level.sendParticles(ParticleTypes.GLOW, player.getX(), player.getY() + 1.0, player.getZ(), 12, 0.3, 0.5, 0.3, 0.02);
        return true;
    }

    public static boolean swapCard(ServerLevel level, Player player, InteractionHand hand) {
        net.minecraft.world.phys.Vec3 eye = player.getEyePosition(), reach = eye.add(player.getLookAngle().scale(32.0));
        net.minecraft.world.phys.EntityHitResult hit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(level, player, eye, reach,
                player.getBoundingBox().expandTowards(player.getLookAngle().scale(32.0)).inflate(1.0),
                e -> e instanceof net.minecraft.world.entity.LivingEntity && e.isAlive() && !e.isSpectator(), 0.3F);
        if (hit == null || !player.hasLineOfSight(hit.getEntity())) return false;
        net.minecraft.world.entity.Entity other = hit.getEntity();
        net.minecraft.world.phys.Vec3 mine = player.position(), theirs = other.position();
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, mine.x, mine.y + 1.0, mine.z, 20, 0.3, 0.5, 0.3, 0.2);
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, theirs.x, theirs.y + 1.0, theirs.z, 20, 0.3, 0.5, 0.3, 0.2);
        other.teleportTo(mine.x, mine.y, mine.z);
        player.teleportTo(theirs.x, theirs.y, theirs.z);
        player.resetFallDistance();
        return true;
    }

    public static boolean closetKey(ServerLevel level, Player player, InteractionHand hand) {
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 200, 0));
        for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(24.0), mob -> mob.getTarget() == player)) {
            mob.setTarget(null);
        }
        level.sendParticles(ParticleTypes.LARGE_SMOKE, player.getX(), player.getY() + 1.0, player.getZ(), 16, 0.3, 0.6, 0.3, 0.02);
        return true;
    }

    public static boolean huffAndPuff(ServerLevel level, Player player, InteractionHand hand) {
        for (net.minecraft.world.entity.LivingEntity blown : level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
                player.getBoundingBox().inflate(8.0), e -> e != player && !e.isSpectator())) {
            net.minecraft.world.phys.Vec3 away = blown.position().subtract(player.position()).multiply(1.0, 0.0, 1.0);
            if (away.lengthSqr() < 1.0E-4) continue;
            away = away.normalize().scale(2.0 * (1.0 - blown.distanceTo(player) / 10.0) + 0.6);
            blown.push(away.x, 0.5, away.z);
            blown.hurtMarked = true;
        }
        net.minecraft.world.phys.Vec3 look = player.getLookAngle();
        level.sendParticles(ParticleTypes.CLOUD, player.getX() + look.x, player.getEyeY() - 0.2, player.getZ() + look.z, 40, 1.5, 0.4, 1.5, 0.15);
        return true;
    }

    public static boolean dreamSand(ServerLevel level, Player player, InteractionHand hand) {
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 160, 1));
        Mind.update(player, mind -> mind.withMood(Math.min(Mind.MAX, mind.mood() + 10.0F)));
        level.sendParticles(ParticleTypes.WAX_ON, player.getX(), player.getEyeY(), player.getZ(), 12, 0.3, 0.2, 0.3, 0.02);
        return true;
    }

    public static boolean fairyDust(ServerLevel level, Player player, InteractionHand hand) {
        player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 40, 2));
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 140, 0));
        level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 0.5, player.getZ(), 16, 0.4, 0.4, 0.4, 0.02);
        return true;
    }

    public static boolean broodsilk(ServerLevel level, Player player, InteractionHand hand) {
        net.minecraft.world.phys.Vec3 eye = player.getEyePosition(), reach = eye.add(player.getLookAngle().scale(28.0));
        net.minecraft.world.phys.BlockHitResult hit = level.clip(new net.minecraft.world.level.ClipContext(eye, reach,
                net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, player));
        if (hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS) return false;
        net.minecraft.world.phys.Vec3 to = hit.getLocation().subtract(eye);
        double far = to.length();
        player.setDeltaMovement(to.normalize().scale(Math.min(2.4, 0.9 + far * 0.08)).add(0.0, 0.25, 0.0));
        player.hurtMarked = true;
        player.resetFallDistance();
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 30, 0));
        for (int i = 0; i < 16; i++) {
            net.minecraft.world.phys.Vec3 at = eye.lerp(hit.getLocation(), i / 16.0);
            level.sendParticles(ParticleTypes.WHITE_ASH, at.x, at.y, at.z, 2, 0.02, 0.02, 0.02, 0.0);
        }
        return true;
    }

    public static boolean voidScale(ServerLevel level, Player player, InteractionHand hand) {
        WildsServer.Ground ground = WildsServer.lastGround(player);
        if (ground == null || !ground.dimension().equals(level.dimension())) return false;
        level.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1.0, player.getZ(), 30, 0.4, 0.8, 0.4, 0.3);
        player.teleportTo(ground.at().x, ground.at().y, ground.at().z);
        player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        player.hurtMarked = true;
        player.resetFallDistance();
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0));
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1.0, player.getZ(), 30, 0.4, 0.8, 0.4, 0.1);
        return true;
    }

    public static boolean swarmJar(ServerLevel level, Player player, InteractionHand hand) {
        boolean any = false;
        for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(7.0), mob -> mob instanceof Enemy)) {
            mob.hurtServer(level, player.damageSources().playerAttack(player), 4.0F);
            mob.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 0), player);
            level.sendParticles(ParticleTypes.SMOKE, mob.getX(), mob.getY() + mob.getBbHeight() * 0.6, mob.getZ(), 12, 0.4, 0.4, 0.4, 0.02);
            any = true;
        }
        return any;
    }

    public static boolean shrinkingSkin(ServerLevel level, Player player, InteractionHand hand) {
        if (!(player instanceof net.minecraft.server.level.ServerPlayer server)) return false;
        WildsServer.shrink(server, 1200);
        level.sendParticles(ParticleTypes.POOF, player.getX(), player.getY() + 0.5, player.getZ(), 12, 0.3, 0.3, 0.3, 0.02);
        return true;
    }

    public static boolean haresFoot(ServerLevel level, Player player, InteractionHand hand) {
        player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 60 * 20, 1));
        player.addEffect(new MobEffectInstance(MobEffects.LUCK, 300 * 20, 0));
        return true;
    }
}
