package net.ixdarklord.whycantisleep.mind;

import net.ixdarklord.coolcatcore.api.event.v2.common.PlayerEvents;
import net.ixdarklord.coolcatcore.api.event.v2.common.ServerLifecycleEvents;
import net.ixdarklord.whycantisleep.config.WCISServerConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The rules of {@link Mind}: what moves a player's mood, guilt and sanity.
 * <p>
 * <b>Mood</b> is not a checklist. Every ten seconds it steps towards a target made of three needs, which ordinary
 * play keeps met: rest (sleeping now and then), food (not going hungry, with some variety), and the open sky (not
 * living underground). Small good things lift it for a while ({@link Mind#cheer}): a food not eaten lately, feeding
 * an animal, company, a night's sleep. Getting hurt and dying weigh on it for a while ({@link Mind#strain}).
 * <p>
 * <b>Guilt</b> comes from killing what trusted the player (villagers, pets, young animals, golems) and from attacking
 * another player who isn't fighting back. It fades with time and kindness.
 * <p>
 * <b>Sanity</b> recovers while mood is content or better and wears away while it is gloomy or worse, and can't
 * recover past what guilt allows. Sleeping in the dark costs some at once; sleeping in the light gives some back.
 * <p>
 * Every number here is a first guess, to be tuned by play. An in-game day is 24000 ticks, 120 of these updates.
 */
public final class MindServer {
    private static final int UPDATE_TICKS = 200;
    // Without sleep: fine for a day, then the need for rest goes unmet over the two after.
    private static final float RESTED_TICKS = 24_000.0F;
    private static final float EXHAUSTED_TICKS = 72_000.0F;
    // This many different foods among the recent ones is as varied as a diet needs to be.
    private static final int VARIED_DIET = 4;
    // How fast the feel for open sky follows where the player is: most of a day to change over.
    private static final float OUTDOORS_PACE = 0.008F;
    private static final double COMPANY_RANGE = 12.0;
    private static final float COMPANY_BONUS = 5.0F;
    // Mood's target: this much whatever happens, this much more with every need met.
    private static final float MOOD_FLOOR = 25.0F;
    private static final float MOOD_FROM_NEEDS = 55.0F;
    private static final float MOOD_STEP = 1.0F;
    // What cheer and strain keep of themselves each update: about half gone in eight minutes.
    private static final float FADE = 0.985F;

    private static final float NEW_FOOD_CHEER = 4.0F;
    private static final float MEAL_CHEER = 0.5F;
    private static final float FED_ANIMAL_CHEER = 2.0F;
    private static final float FED_ANIMAL_GUILT = -1.0F;
    private static final float LIT_SLEEP_CHEER = 3.0F;
    private static final float RESTED_CHEER = 6.0F;
    private static final float RESTED_GUILT = -4.0F;
    private static final float HURT_STRAIN = 0.4F;
    private static final float DEATH_STRAIN = 15.0F;

    // Sanity each update, by mood: a day of good mood gives back 24, a day of misery takes 30.
    private static final float SANITY_RECOVERY = 0.2F;
    private static final float SANITY_WEAR_GLOOMY = -0.12F;
    private static final float SANITY_WEAR_MISERABLE = -0.25F;
    private static final float DARK_SLEEP_SANITY = -8.0F;
    private static final float LIT_SLEEP_SANITY = 6.0F;

    // Guilt fades by 12 a day.
    private static final float GUILT_FADE = 0.1F;
    private static final float GUILT_VILLAGER = 25.0F;
    private static final float GUILT_OWN_PET = 30.0F;
    private static final float GUILT_PET = 20.0F;
    private static final float GUILT_GOLEM = 10.0F;
    private static final float GUILT_YOUNG = 8.0F;
    private static final float GUILT_PLAYER_KILL = 35.0F;
    private static final float GUILT_PLAYER_HIT = 2.0F;
    // A player who struck back within this long is fighting, not being attacked.
    private static final int PROVOKED_TICKS = 20 * 30;
    private static final int HIT_GUILT_INTERVAL = 20;

    // Who last struck whom, and when (server ticks): "attacker>victim".
    private static final Map<String, Integer> LAST_STRIKE = new HashMap<>();

    private MindServer() {
    }

    public static void init() {
        PlayerEvents.END_TICK.register(player -> {
            if (player instanceof ServerPlayer serverPlayer && serverPlayer.tickCount % UPDATE_TICKS == 0) update(serverPlayer);
        });
        PlayerEvents.LEAVE.register(player -> {
            String id = player.getUUID().toString();
            LAST_STRIKE.keySet().removeIf(key -> key.contains(id));
        });
        ServerLifecycleEvents.STOPPED.register(server -> LAST_STRIKE.clear());
    }

    /** Whether the mood and sanity system runs for this player: never in creative or spectator, or with it turned off. */
    public static boolean applies(Player player) {
        return WCISServerConfig.MIND_SYSTEM.get() && !player.isCreative() && !player.isSpectator();
    }

    // Every ten seconds: the needs are measured, mood steps towards what they make, and sanity and guilt follow.
    private static void update(ServerPlayer player) {
        if (!applies(player) || !player.isAlive()) return;
        ServerLevel level = player.level();
        Mind mind = Mind.get(player);

        float awake = player.getStats().getValue(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));
        float rest = 1.0F - Mth.clamp((awake - RESTED_TICKS) / (EXHAUSTED_TICKS - RESTED_TICKS), 0.0F, 1.0F);
        float variety = mind.foods().isEmpty() ? 0.5F : Math.min(1.0F, mind.foods().stream().distinct().count() / (float) VARIED_DIET);
        float food = player.getFoodData().getFoodLevel() / 20.0F * 0.6F + variety * 0.4F;
        float sky = level.canSeeSky(player.blockPosition().above()) ? 1.0F : 0.0F;
        float outdoors = mind.outdoors() + (sky - mind.outdoors()) * OUTDOORS_PACE;

        float target = MOOD_FLOOR + MOOD_FROM_NEEDS * (rest * 0.4F + food * 0.3F + outdoors * 0.3F)
                + mind.cheer() - mind.strain() + (hasCompany(player) ? COMPANY_BONUS : 0.0F);
        float mood = mind.mood() + Mth.clamp(Mth.clamp(target, 0.0F, Mind.MAX) - mind.mood(), -MOOD_STEP, MOOD_STEP);
        float guilt = Math.max(0.0F, mind.guilt() - GUILT_FADE);

        MoodLevel feeling = MoodLevel.of(mood);
        float drift = feeling.ordinal() >= MoodLevel.CONTENT.ordinal() ? SANITY_RECOVERY
                : feeling == MoodLevel.GLOOMY ? SANITY_WEAR_GLOOMY : feeling == MoodLevel.MISERABLE ? SANITY_WEAR_MISERABLE : 0.0F;
        float cap = Mind.MAX - guilt * 0.6F;
        float sanity = mind.sanity() + drift;
        // Recovery stops at what guilt allows; sanity already above that settles down to it.
        if (sanity > cap) sanity = Math.max(cap, mind.sanity() - 0.3F);

        Mind updated = new Mind(mood, sanity, guilt, rest, food, outdoors, mind.cheer() * FADE, mind.strain() * FADE, mind.foods());
        Mind.update(player, old -> updated);
    }

    // Another player, a villager or one's own pet close by.
    private static boolean hasCompany(ServerPlayer player) {
        return !player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(COMPANY_RANGE), entity ->
                entity != player && entity.isAlive() && (entity instanceof Player other && !other.isSpectator() || entity instanceof AbstractVillager
                        || entity instanceof TamableAnimal pet && pet.isOwnedBy(player))).isEmpty();
    }

    /** The player ate something ({@code FoodPropertiesMixin}): a food not eaten lately lifts the mood more. */
    public static void onAte(ServerPlayer player, ItemStack stack) {
        if (!applies(player)) return;
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        Mind.update(player, mind -> {
            boolean fresh = !mind.foods().contains(id);
            List<String> foods = new ArrayList<>(mind.foods());
            foods.add(id);
            while (foods.size() > Mind.FOOD_MEMORY) foods.removeFirst();
            return mind.withFoods(foods).withCheer(mind.cheer() + (fresh ? NEW_FOOD_CHEER : MEAL_CHEER));
        });
    }

    /** The player fed an animal ({@code AnimalMixin}): a small kindness, good for the mood and for a guilty conscience. */
    public static void onFedAnimal(ServerPlayer player) {
        if (!applies(player)) return;
        Mind.update(player, mind -> mind.withCheer(mind.cheer() + FED_ANIMAL_CHEER).withGuilt(mind.guilt() + FED_ANIMAL_GUILT));
    }

    /** The player lay down to sleep: in the dark it costs sanity at once, in the light it gives some back. */
    public static void onLayDown(ServerPlayer player, boolean dark) {
        if (!applies(player)) return;
        Mind.update(player, mind -> dark ? mind.withSanity(mind.sanity() + DARK_SLEEP_SANITY)
                : mind.withSanity(Math.min(Math.max(mind.sanity(), mind.sanityCap()), mind.sanity() + LIT_SLEEP_SANITY)).withCheer(mind.cheer() + LIT_SLEEP_CHEER));
    }

    /** The player woke after sleeping the night through ({@code ServerPlayerMixin}). */
    public static void onRested(ServerPlayer player) {
        if (!applies(player)) return;
        Mind.update(player, mind -> mind.withCheer(mind.cheer() + RESTED_CHEER).withGuilt(mind.guilt() + RESTED_GUILT));
    }

    /** The player was hurt ({@code ServerPlayerMixin}): it weighs on the mood, and between players it may be guilt. */
    public static void onHurt(ServerPlayer player, DamageSource source, float damage) {
        if (applies(player)) Mind.update(player, mind -> mind.withStrain(mind.strain() + damage * HURT_STRAIN));
        if (source.getEntity() instanceof ServerPlayer attacker && attacker != player) onStruck(attacker, player);
    }

    // One player struck another. Striking someone who hasn't struck back lately is an attack, and a little guilt each time.
    private static void onStruck(ServerPlayer attacker, ServerPlayer victim) {
        int now = attacker.level().getServer().getTickCount();
        Integer last = LAST_STRIKE.put(key(attacker, victim), now);
        if (!WCISServerConfig.PVP_GUILT.get() || !applies(attacker) || isProvoked(attacker, victim, now)) return;
        if (last == null || now - last >= HIT_GUILT_INTERVAL) addGuilt(attacker, GUILT_PLAYER_HIT);
    }

    // Whether the victim has been fighting the attacker: they struck them within the last half minute.
    private static boolean isProvoked(ServerPlayer attacker, ServerPlayer victim, int now) {
        Integer struckBack = LAST_STRIKE.get(key(victim, attacker));
        return struckBack != null && now - struckBack <= PROVOKED_TICKS;
    }

    private static String key(Player attacker, Player victim) {
        return attacker.getUUID() + ">" + victim.getUUID();
    }

    /** A player died ({@code ServerPlayerMixin}): it weighs on their mood, and on the conscience of whoever murdered them. */
    public static void onPlayerDied(ServerPlayer player, DamageSource source) {
        if (applies(player)) Mind.update(player, mind -> mind.withStrain(mind.strain() + DEATH_STRAIN));
        if (source.getEntity() instanceof ServerPlayer killer && killer != player && WCISServerConfig.PVP_GUILT.get() && applies(killer)
                && !isProvoked(killer, player, killer.level().getServer().getTickCount())) {
            addGuilt(killer, GUILT_PLAYER_KILL);
        }
    }

    /** Something other than a player died ({@code LivingEntityMixin}): killing what trusted the player is guilt. */
    public static void onKilled(LivingEntity victim, DamageSource source) {
        if (!(source.getEntity() instanceof ServerPlayer killer) || !applies(killer)) return;
        float guilt = victim instanceof AbstractVillager ? GUILT_VILLAGER
                : victim instanceof TamableAnimal pet && pet.isTame() ? (pet.isOwnedBy(killer) ? GUILT_OWN_PET : GUILT_PET)
                : victim instanceof IronGolem ? GUILT_GOLEM
                : victim instanceof Animal && victim.isBaby() ? GUILT_YOUNG : 0.0F;
        if (guilt > 0.0F) addGuilt(killer, guilt);
    }

    private static void addGuilt(ServerPlayer player, float amount) {
        Mind.update(player, mind -> mind.withGuilt(mind.guilt() + amount));
    }

    /** Server side: moves a player's sanity, for what the Lurker does to it. */
    public static void addSanity(Player player, float amount) {
        Mind.update(player, mind -> mind.withSanity(mind.sanity() + amount));
    }

    /** Server side: lifts a player's sanity to at least this much. */
    public static void restoreSanity(Player player, float atLeast) {
        Mind.update(player, mind -> mind.withSanity(Math.max(mind.sanity(), atLeast)));
    }
}
