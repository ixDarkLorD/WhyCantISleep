package net.ixdarklord.whycantisleep.mind;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.ixdarklord.coolcatcore.api.attachment.Attachment;
import net.ixdarklord.coolcatcore.api.attachment.AttachmentRegistry;
import net.ixdarklord.coolcatcore.api.attachment.SyncPolicy;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.function.UnaryOperator;

/**
 * How a player is doing, in two numbers, each 0 to 100: their mood and their lucidity. It is saved with the player,
 * kept through death, and sent to their own client, which shows both as bars and draws what low lucidity brings.
 * <ul>
 *     <li><b>Mood</b> follows the player's routine: {@link #rest}, {@link #food} and {@link #outdoors} are how well
 *     each need is met (0 to 1), {@link #cheer} is what good moments add and {@link #strain} what hard ones take.</li>
 *     <li><b>Lucidity</b> rises while mood is good and falls while it is bad, and takes a blow from a guilty act. Low
 *     lucidity is what the Lurker comes for ({@link LucidityStage}).</li>
 * </ul>
 * Each holds the other back: mood can't rise past {@link #moodCeiling()}, which low lucidity brings down, and lucidity can't
 * recover past {@link #lucidityCeiling()}, which low mood brings down. Neither ceiling ever shuts the way back up.
 * {@link MindServer} holds the rules. The value is immutable: change it with {@link #update}.
 *
 * @param foods the last few different things the player ate, newest last
 */
public record Mind(float mood, float lucidity, float rest, float food, float outdoors, float cheer, float strain, List<String> foods) {
    public static final float MAX = 100.0F;
    /** How many recent foods are remembered. */
    public static final int FOOD_MEMORY = 8;
    public static final Mind DEFAULT = new Mind(70.0F, 100.0F, 1.0F, 0.7F, 0.7F, 0.0F, 0.0F, List.of());

    // Saves from when guilt was a number of its own still carry it; the codec passes over it. Saves from when lucidity
    // was called sanity don't have it under its name, and load with it full.
    public static final Codec<Mind> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.FLOAT.fieldOf("mood").forGetter(Mind::mood),
            Codec.FLOAT.optionalFieldOf("lucidity", MAX).forGetter(Mind::lucidity),
            Codec.FLOAT.fieldOf("rest").forGetter(Mind::rest),
            Codec.FLOAT.fieldOf("food").forGetter(Mind::food),
            Codec.FLOAT.fieldOf("outdoors").forGetter(Mind::outdoors),
            Codec.FLOAT.fieldOf("cheer").forGetter(Mind::cheer),
            Codec.FLOAT.fieldOf("strain").forGetter(Mind::strain),
            Codec.STRING.listOf().fieldOf("foods").forGetter(Mind::foods)
    ).apply(i, Mind::new));

    public static final AttachmentRegistry ATTACHMENTS = AttachmentRegistry.create(WhyCantISleep.MOD_ID);
    public static final Attachment<Mind> MIND = ATTACHMENTS.builder("mind", CODEC, DEFAULT)
            .networkCodec(ByteBufCodecs.fromCodecWithRegistries(CODEC)).sync(SyncPolicy.SELF).copyOnDeath().build();

    public Mind {
        mood = Mth.clamp(mood, 0.0F, MAX);
        lucidity = Mth.clamp(lucidity, 0.0F, MAX);
        foods = List.copyOf(foods);
    }

    public static Mind get(Player player) {
        return MIND.get(player);
    }

    /** Server side: changes a player's mind, which saves it and sends it to them. */
    public static void update(Player player, UnaryOperator<Mind> change) {
        MIND.update(player, change);
    }

    /**
     * The most mood can rise to with lucidity as it is: a mind that isn't steady can't be wholly at ease. No limit while
     * lucidity is steady (75 and up); from there it comes down to 58 with lucidity gone, which is still content, so lucidity
     * can always begin to recover.
     */
    public float moodCeiling() {
        return Mth.clamp(58.0F + this.lucidity * 0.56F, 0.0F, MAX);
    }

    /**
     * The most lucidity can recover to with mood as it is: a mind doesn't mend in misery. No limit while mood is
     * cheerful (75 and up); from there it comes down to 40 with mood gone.
     */
    public float lucidityCeiling() {
        return Mth.clamp(40.0F + this.mood * 0.8F, 0.0F, MAX);
    }

    public LucidityStage stage() {
        return LucidityStage.of(this.lucidity);
    }

    public MoodLevel moodLevel() {
        return MoodLevel.of(this.mood);
    }

    public Mind withMood(float mood) {
        return new Mind(mood, this.lucidity, this.rest, this.food, this.outdoors, this.cheer, this.strain, this.foods);
    }

    public Mind withLucidity(float lucidity) {
        return new Mind(this.mood, lucidity, this.rest, this.food, this.outdoors, this.cheer, this.strain, this.foods);
    }

    public Mind withCheer(float cheer) {
        return new Mind(this.mood, this.lucidity, this.rest, this.food, this.outdoors, Mth.clamp(cheer, 0.0F, 25.0F), this.strain, this.foods);
    }

    public Mind withStrain(float strain) {
        return new Mind(this.mood, this.lucidity, this.rest, this.food, this.outdoors, this.cheer, Mth.clamp(strain, 0.0F, 25.0F), this.foods);
    }

    public Mind withFoods(List<String> foods) {
        return new Mind(this.mood, this.lucidity, this.rest, this.food, this.outdoors, this.cheer, this.strain, foods);
    }
}
