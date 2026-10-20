package net.ixdarklord.whycantisleep.between.client;

import net.ixdarklord.coolcatcanvas.api.armature.Motion;
import net.ixdarklord.coolcatcanvas.api.client.armature.Armature;
import net.ixdarklord.coolcatcanvas.api.client.armature.render.ArmatureMobRenderer;
import net.ixdarklord.coolcatcore.api.client.registry.EntityRendererRegistry;
import net.ixdarklord.coolcatcore.api.registry.RegistryEntry;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientTickEvents;
import net.ixdarklord.whycantisleep.between.mob.CrawlspaceEntity;
import net.ixdarklord.whycantisleep.between.mob.GazerEntity;
import net.ixdarklord.whycantisleep.between.mob.HiveThingEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.ixdarklord.whycantisleep.between.mob.GrinningClownEntity;
import net.ixdarklord.whycantisleep.between.mob.NightMareEntity;
import net.ixdarklord.whycantisleep.between.mob.TickTockEntity;
import net.ixdarklord.whycantisleep.lurker.WCISEntities;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The Dark Between's wild creatures as they are drawn: CoolCatLib armatures from tools/gen_mobs.py. A base layer walks
 * or idles, or plays what the creature is doing that is its own (the Night Mare pawing and charging, the Gazer fixing
 * its stare, the Tick-Tock about to burst); an action layer plays their blow (the Lullaby's song) as they swing. The
 * Gazer's eye and the Tick-Tock's lit eyes burn brighter as they build.
 */
public final class WildsClient {
    private static final Motion IDLE = Motion.loop("idle").fade(0.25F);
    private static final Motion WALK = Motion.loop("walk").fade(0.2F);
    private static final Motion ATTACK = Motion.once("attack").fade(0.08F);
    private static final Motion WINDUP = Motion.loop("windup").fade(0.15F);
    private static final Motion CHARGE = Motion.loop("charge").fade(0.1F);
    private static final Motion BEAM = Motion.loop("beam").fade(0.15F);
    private static final Motion FUSE = Motion.loop("fuse").fade(0.1F);
    private static final Motion LAUGH = Motion.loop("laugh").fade(0.1F);
    private static final Motion OPEN = Motion.loop("open").fade(0.12F);
    private static final Motion DISGUISED = Motion.loop("disguised").fade(0.3F);
    private static final Motion HOBBLE = Motion.loop("hobble").fade(0.2F);
    private static final Motion REVEAL = Motion.once("reveal").fade(0.05F);
    private static final Motion LUNGE = Motion.loop("lunge").fade(0.15F);
    private static final Motion GRAB = Motion.loop("grab").fade(0.1F);
    private static final Motion BOX = Motion.loop("box").fade(0.1F);
    private static final Motion HIDE = Motion.loop("hide").fade(0.05F);
    private static final Motion POP = Motion.once("pop").fade(0.0F);
    private static final Motion SHUFFLE = Motion.loop("shuffle").fade(0.1F);
    // Little Red with her hood back (the fade between these and her hooded clips lifts and lowers it).
    private static final Motion IDLE_OPEN = Motion.loop("idle_open").fade(0.45F);
    private static final Motion WALK_OPEN = Motion.loop("walk_open").fade(0.45F);
    // Little Red's turning, on its own armature (both of her in one): no fade in, so the swap to it is seamless.
    private static final Motion TURN = Motion.once("turn").fade(0.0F);
    // The Hive-Thing: within this many blocks, specks crawl across the edges of the local player's sight.
    private static final double CRAWL_NEAR = 10.0;
    private static final DustParticleOptions SPECK = new DustParticleOptions(0x140E0A, 0.35F);
    // The Crawlspace: barely there farther than this, fully there this near.
    private static final double UNSEEN_AT = 11.0, SEEN_AT = 4.0;

    private WildsClient() {
    }

    public static void init() {
        mob(WCISEntities.NIGHT_MARE, "night_mare", 0.9F, true,
                e -> e.isCharging() ? CHARGE : e.isWindingUp() ? WINDUP : null, null);
        mob(WCISEntities.HOLLOW_HOUND, "hollow_hound", 0.5F, true, null, null);
        mob(WCISEntities.LULLABY, "lullaby", 0.4F, true, null, null);
        mob(WCISEntities.GAZER, "gazer", 0.5F, false, e -> e.charge() > 0 ? BEAM : null,
                (e, pt) -> 0.65F + 0.35F * e.charge() / (float) GazerEntity.CHARGE_TICKS);
        mob(WCISEntities.TICK_TOCK, "tick_tock", 0.4F, false, e -> e.fuse() > 0 ? FUSE : null,
                (e, pt) -> 0.5F + 0.5F * e.fuse() / (float) TickTockEntity.FUSE_TICKS);
        mob(WCISEntities.STITCHLING, "stitchling", 0.35F, true, null, null);
        mob(WCISEntities.GLOOM_JELLY, "gloom_jelly", 0.0F, false, null,
                (e, pt) -> 0.75F + 0.25F * (float) Math.sin((e.tickCount + pt) * 0.157F));
        mob(WCISEntities.DREAM_HARE, "dream_hare", 0.25F, false, null, null);
        mob(WCISEntities.GRINNING_CLOWN, "grinning_clown", 0.5F, true, e -> e.isLaughing() ? LAUGH : null, null);
        // Children's-story nightmares and phobias.
        mob(WCISEntities.CLOSET_BOOGEYMAN, "closet_boogeyman", 0.7F, true, e -> e.isOpen() ? OPEN : null, (e, pt) -> e.isOpen() ? 1.0F : 0.6F);
        mob(WCISEntities.BIG_BAD_WOLF, "big_bad_wolf", 0.6F, true,
                e -> e.isRevealing() ? REVEAL : e.isDisguised() ? (e.walkAnimation.isMoving() ? HOBBLE : DISGUISED) : null, null);
        mob(WCISEntities.SANDMAN, "sandman", 0.5F, true, null, null);
        mob(WCISEntities.TOOTH_TAKER, "tooth_taker", 0.2F, true, null, null, renderer -> renderer.holdItem("hand", EquipmentSlot.MAINHAND));
        mob(WCISEntities.BROODMOTHER, "broodmother", 1.3F, true, null, null);
        mob(WCISEntities.BROODLING, "broodling", 0.2F, true, null, null);
        mob(WCISEntities.VOID_EEL, "void_eel", 0.0F, true, e -> e.isLunging() ? LUNGE : null, null);
        mob(WCISEntities.HIVE_THING, "hive_thing", 0.6F, true, null, null);
        // The Crawlspace fades in as the local player nears it, from nearly nothing.
        mob(WCISEntities.CRAWLSPACE, "crawlspace", 0.0F, true, e -> e.isHolding() ? GRAB : null, null, renderer -> renderer
                .translucent((e, pt) -> seen(e) < 1.0F)
                .tint((e, pt) -> ARGB.white(seen(e))));
        // The Grinning Clown's kin.
        mob(WCISEntities.MIME_CLOWN, "mime_clown", 0.45F, true, e -> e.isBoxing() ? BOX : null, null);
        mob(WCISEntities.JACK_IN_THE_BOX, "jack_in_the_box", 0.6F, true, e -> e.isHidden() ? HIDE : e.isPopping() ? POP : null, null);
        mob(WCISEntities.BALLOON_CLOWN, "balloon_clown", 0.3F, true, null, null);
        mob(WCISEntities.FUNHOUSE_CLOWN, "funhouse_clown", 0.5F, true, e -> e.isShuffling() ? SHUFFLE : null, null);
        // Little Red, and what she is under the hood. She turns on little_red_turning (her sweet self and her true
        // one in one armature, burning one into the other limb by limb), then is her feral armature from then on.
        Armature turning = Armature.of(WhyCantISleep.id("little_red_turning"));
        Armature feral = Armature.of(WhyCantISleep.id("little_red_feral"));
        mob(WCISEntities.LITTLE_RED, "little_red", 0.35F, true, e -> e.isTurning() ? TURN : e.isFeral() ? null
                : e.isHoodUp() ? (e.walkAnimation.isMoving() ? WALK_OPEN : IDLE_OPEN) : null, null,
                renderer -> renderer.armature(e -> e.isTurning() ? turning : e.isFeral() ? feral : null));
        ClientTickEvents.END.register(WildsClient::crawl);
    }

    /** How much of the Crawlspace shows to the local player, 0.08 to 1, by how near they are. */
    private static float seen(CrawlspaceEntity crawlspace) {
        if (crawlspace.isHolding()) return 1.0F;
        Player player = Minecraft.getInstance().player;
        if (player == null) return 1.0F;
        double d = player.distanceTo(crawlspace);
        return (float) Mth.clamp((UNSEEN_AT - d) / (UNSEEN_AT - SEEN_AT), 0.08, 1.0);
    }

    /** Near a Hive-Thing, specks crawl at the edges of the local player's sight, more the nearer it is. */
    private static void crawl(Minecraft minecraft) {
        Player player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.isPaused()) return;
        HiveThingEntity nearest = null;
        double best = CRAWL_NEAR;
        for (HiveThingEntity hive : minecraft.level.getEntitiesOfClass(HiveThingEntity.class, player.getBoundingBox().inflate(CRAWL_NEAR))) {
            double d = hive.distanceTo(player);
            if (d < best) {
                best = d;
                nearest = hive;
            }
        }
        if (nearest == null) return;
        RandomSource random = player.getRandom();
        Vec3 eye = player.getEyePosition(), look = player.getLookAngle();
        Vec3 side = look.cross(new Vec3(0.0, 1.0, 0.0)).normalize(), up = side.cross(look).normalize();
        int specks = (int) Math.ceil(4.0 * (1.0 - best / CRAWL_NEAR));
        for (int i = 0; i < specks; i++) {
            // Round the rim of the view, a little in front of the eyes.
            double a = random.nextDouble() * Math.PI * 2.0, r = 0.45 + random.nextDouble() * 0.15;
            Vec3 at = eye.add(look.scale(0.6)).add(side.scale(Math.cos(a) * r)).add(up.scale(Math.sin(a) * r * 0.7));
            Vec3 drift = side.scale(-Math.sin(a) * 0.01).add(up.scale(Math.cos(a) * 0.01));
            minecraft.level.addParticle(SPECK, at.x, at.y, at.z, drift.x, drift.y, drift.z);
        }
    }

    private static <T extends Mob> void mob(RegistryEntry<EntityType<T>> type, String name, float shadow, boolean strikes,
                                            @Nullable Function<T, Motion> special, ArmatureMobRenderer.GlowStrength<T> glow) {
        mob(type, name, shadow, strikes, special, glow, renderer -> {
        });
    }

    private static <T extends Mob> void mob(RegistryEntry<EntityType<T>> type, String name, float shadow, boolean strikes,
                                            @Nullable Function<T, Motion> special, ArmatureMobRenderer.GlowStrength<T> glow,
                                            Consumer<ArmatureMobRenderer<T>> extra) {
        Armature armature = Armature.of(WhyCantISleep.id(name));
        EntityRendererRegistry.register(type, context -> {
            ArmatureMobRenderer<T> renderer = new ArmatureMobRenderer<>(context, armature, shadow) {
                // As vanilla's mobs: no name over it unless it was given one and is looked at.
                @Override
                protected boolean shouldShowName(T entity, double distanceToCameraSq) {
                    return super.shouldShowName(entity, distanceToCameraSq) && (entity.shouldShowName()
                            || entity.hasCustomName() && entity == this.entityRenderDispatcher.crosshairPickEntity);
                }
            };
            if (glow != null) renderer.glow(glow);
            extra.accept(renderer);
            renderer.animate(animator -> {
                animator.layer("base").drive(ctx -> {
                    T entity = ctx.owner();
                    Motion own = special != null ? special.apply(entity) : null;
                    if (own != null) return own;
                    return ctx.moving() ? WALK : IDLE;
                });
                if (strikes) {
                    animator.layer("action").drive(ctx -> ctx.entity() instanceof LivingEntity living && living.swinging ? ATTACK : null);
                }
            });
            return renderer;
        });
    }
}
