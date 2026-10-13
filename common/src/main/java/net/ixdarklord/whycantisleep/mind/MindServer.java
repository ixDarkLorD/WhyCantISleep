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
 * The rules of {@link Mind}: what moves a player's mood and lucidity. There are only those two.
 * <p>
 * <b>Mood</b> is not a checklist. Every ten seconds it steps towards a target made of three needs, which ordinary
 * play keeps met: rest (sleeping now and then), food (not going hungry, with some variety), and the open sky (not
 * living underground). Small good things lift it for a while ({@link Mind#cheer}): a food not eaten lately, feeding
 * an animal, company, a night's sleep. Hard things weigh on it for a while ({@link Mind#strain}): getting hurt, dying,
 * and a bad conscience.
 * <p>
 * <b>Lucidity</b> recovers while mood is content or better and wears away while it is gloomy or worse. Sleeping in the
 * dark costs some at once; sleeping in the light gives some back. And a <b>guilty act</b> costs it at once: killing
 * what trusted the player (villagers, pets, young animals, golems), or attacking another player who isn't fighting
 * back. Guilt is not a number of its own: the act takes its toll on lucidity there and then, weighs on the mood for a
 * while, and is over.
 * <p>
 * <b>Each limits the other</b> ({@link Mind#moodCeiling()}, {@link Mind#lucidityCeiling()}): mood can't climb past what
 * lucidity allows, and lucidity can't recover past what mood allows. The ceilings are set so there is always a way back
 * through ordinary play: even with lucidity gone mood can reach content (58), where lucidity recovers. (With a lower
 * ceiling a player at the bottom of both stayed there for good, which a test showed on 2026-10-14.)
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
    private static final float FED_ANIMAL_LUCIDITY = 0.5F;
    private static final float LIT_SLEEP_CHEER = 3.0F;
    private static final float RESTED_CHEER = 6.0F;
    private static final float RESTED_LUCIDITY = 3.0F;
    private static final float HURT_STRAIN = 0.4F;
    private static final float DEATH_STRAIN = 15.0F;

    // Lucidity each update, by mood: a day of good mood gives back 24, a day of misery takes 30.
    private static final float LUCIDITY_RECOVERY = 0.2F;
    private static final float LUCIDITY_WEAR_GLOOMY = -0.12F;
    private static final float LUCIDITY_WEAR_MISERABLE = -0.25F;
    // Lucidity above what mood allows comes down by this much an update.
    private static final float LUCIDITY_SETTLE = 0.3F;
    private static final float DARK_SLEEP_LUCIDITY = -8.0F;
    private static final float LIT_SLEEP_LUCIDITY = 6.0F;

    // What a guilty act costs in lucidity at once; it also weighs on the mood, by this share of the same amount.
    private static final float VILLAGER_COST = 15.0F;
    private static final float OWN_PET_COST = 18.0F;
    private static final float PET_COST = 12.0F;
    private static final float GOLEM_COST = 6.0F;
    private static final float YOUNG_COST = 5.0F;
    private static final float PLAYER_KILL_COST = 20.0F;
    private static final float PLAYER_HIT_COST = 1.5F;
    private static final float CONSCIENCE_STRAIN = 0.6F;
    // A player who struck back within this long is fighting, not being attacked.
    private static final int PROVOKED_TICKS = 20 * 30;
    private static final int HIT_COST_INTERVAL = 20;

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

    /** Whether the mood and lucidity system runs for this player: never in creative or spectator, or with it turned off. */
    public static boolean applies(Player player) {
        return WCISServerConfig.MIND_SYSTEM.get() && !player.isCreative() && !player.isSpectator();
    }

    // Every ten seconds: the needs are measured, mood steps towards what they make, and lucidity follows mood.
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
        // Lucidity holds mood back: it climbs no higher than its ceiling, and comes down to it if it is above.
        float mood = mind.mood() + Mth.clamp(Mth.clamp(target, 0.0F, mind.moodCeiling()) - mind.mood(), -MOOD_STEP, MOOD_STEP);

        MoodLevel feeling = MoodLevel.of(mood);
        float drift = feeling.ordinal() >= MoodLevel.CONTENT.ordinal() ? LUCIDITY_RECOVERY
                : feeling == MoodLevel.GLOOMY ? LUCIDITY_WEAR_GLOOMY : feeling == MoodLevel.MISERABLE ? LUCIDITY_WEAR_MISERABLE : 0.0F;

        // Mood holds lucidity back in turn: recovery stops at its ceiling, and lucidity above that settles down to it.
        float ceiling = mind.withMood(mood).lucidityCeiling();
        float lucidity = mind.lucidity() + drift;
        if (lucidity > ceiling) lucidity = Math.max(ceiling, mind.lucidity() - LUCIDITY_SETTLE);

        Mind updated = new Mind(mood, lucidity, rest, food, outdoors, mind.cheer() * FADE, mind.strain() * FADE, mind.foods());
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

    /** The player fed an animal ({@code AnimalMixin}): a small kindness, good for the mood and a little for the mind. */
    public static void onFedAnimal(ServerPlayer player) {
        if (!applies(player)) return;
        Mind.update(player, mind -> mind.withCheer(mind.cheer() + FED_ANIMAL_CHEER).withLucidity(mind.lucidity() + FED_ANIMAL_LUCIDITY));
    }

    /** The player lay down to sleep: in the dark it costs lucidity at once, in the light it gives some back. */
    public static void onLayDown(ServerPlayer player, boolean dark) {
        if (!applies(player)) return;
        Mind.update(player, mind -> dark ? mind.withLucidity(mind.lucidity() + DARK_SLEEP_LUCIDITY)
                : mind.withLucidity(mind.lucidity() + LIT_SLEEP_LUCIDITY).withCheer(mind.cheer() + LIT_SLEEP_CHEER));
    }

    /** The player woke after sleeping the night through ({@code ServerPlayerMixin}). */
    public static void onRested(ServerPlayer player) {
        if (!applies(player)) return;
        Mind.update(player, mind -> mind.withCheer(mind.cheer() + RESTED_CHEER).withLucidity(mind.lucidity() + RESTED_LUCIDITY));
    }

    /** The player was hurt ({@code ServerPlayerMixin}): it weighs on the mood, and between players it may be an attack. */
    public static void onHurt(ServerPlayer player, DamageSource source, float damage) {
        if (applies(player)) Mind.update(player, mind -> mind.withStrain(mind.strain() + damage * HURT_STRAIN));
        if (source.getEntity() instanceof ServerPlayer attacker && attacker != player) onStruck(attacker, player);
    }

    // One player struck another. Striking someone who hasn't struck back lately is an attack, and costs a little each time.
    private static void onStruck(ServerPlayer attacker, ServerPlayer victim) {
        int now = attacker.level().getServer().getTickCount();
        Integer last = LAST_STRIKE.put(key(attacker, victim), now);
        if (!WCISServerConfig.PVP_GUILT.get() || !applies(attacker) || isProvoked(attacker, victim, now)) return;
        if (last == null || now - last >= HIT_COST_INTERVAL) onGuiltyAct(attacker, PLAYER_HIT_COST);
    }

    // Whether the victim has been fighting the attacker: they struck them within the last half minute.
    private static boolean isProvoked(ServerPlayer attacker, ServerPlayer victim, int now) {
        Integer struckBack = LAST_STRIKE.get(key(victim, attacker));
        return struckBack != null && now - struckBack <= PROVOKED_TICKS;
    }

    private static String key(Player attacker, Player victim) {
        return attacker.getUUID() + ">" + victim.getUUID();
    }

    /** A player died ({@code ServerPlayerMixin}): it weighs on their mood, and on the mind of whoever murdered them. */
    public static void onPlayerDied(ServerPlayer player, DamageSource source) {
        if (applies(player)) Mind.update(player, mind -> mind.withStrain(mind.strain() + DEATH_STRAIN));
        if (source.getEntity() instanceof ServerPlayer killer && killer != player && WCISServerConfig.PVP_GUILT.get() && applies(killer)
                && !isProvoked(killer, player, killer.level().getServer().getTickCount())) {
            onGuiltyAct(killer, PLAYER_KILL_COST);
        }
    }

    /** Something other than a player died ({@code LivingEntityMixin}): killing what trusted the player costs them. */
    public static void onKilled(LivingEntity victim, DamageSource source) {
        if (!(source.getEntity() instanceof ServerPlayer killer) || !applies(killer)) return;
        float cost = victim instanceof AbstractVillager ? VILLAGER_COST
                : victim instanceof TamableAnimal pet && pet.isTame() ? (pet.isOwnedBy(killer) ? OWN_PET_COST : PET_COST)
                : victim instanceof IronGolem ? GOLEM_COST
                : victim instanceof Animal && victim.isBaby() ? YOUNG_COST : 0.0F;
        if (cost > 0.0F) onGuiltyAct(killer, cost);
    }

    // A guilty act: its cost comes off lucidity at once, and a share of it weighs on the mood for a while.
    private static void onGuiltyAct(ServerPlayer player, float cost) {
        Mind.update(player, mind -> mind.withLucidity(mind.lucidity() - cost).withStrain(mind.strain() + cost * CONSCIENCE_STRAIN));
    }

    /** Server side: moves a player's lucidity, for what the Lurker does to it. */
    public static void addLucidity(Player player, float amount) {
        Mind.update(player, mind -> mind.withLucidity(mind.lucidity() + amount));
    }

    /** Server side: lifts a player's lucidity to at least this much. */
    public static void restoreLucidity(Player player, float atLeast) {
        Mind.update(player, mind -> mind.withLucidity(Math.max(mind.lucidity(), atLeast)));
    }
}
