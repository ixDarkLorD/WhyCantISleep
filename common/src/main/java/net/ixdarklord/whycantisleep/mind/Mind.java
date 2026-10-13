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
 * How a player is doing: their mood, their sanity and their guilt, each 0 to 100, with what feeds them. It is saved
 * with the player, kept through death, and sent to their own client, which shows it and draws what low sanity brings.
 * <ul>
 *     <li><b>Mood</b> follows the player's routine: {@link #rest}, {@link #food} and {@link #outdoors} are how well
 *     each need is met (0 to 1), {@link #cheer} is what good moments add and {@link #strain} what hard ones take.</li>
 *     <li><b>Guilt</b> is earned by what the player does, and fades.</li>
 *     <li><b>Sanity</b> rises while mood is good and falls while it is bad, and can't recover past what guilt allows
 *     ({@link #sanityCap()}). Low sanity is what the Lurker comes for ({@link SanityStage}).</li>
 * </ul>
 * {@link MindServer} holds the rules. The value is immutable: change it with {@link #update}.
 *
 * @param foods the last few different things the player ate, newest last
 */
public record Mind(float mood, float sanity, float guilt, float rest, float food, float outdoors, float cheer, float strain, List<String> foods) {
    public static final float MAX = 100.0F;
    /** How many recent foods are remembered. */
    public static final int FOOD_MEMORY = 8;
    public static final Mind DEFAULT = new Mind(70.0F, 100.0F, 0.0F, 1.0F, 0.7F, 0.7F, 0.0F, 0.0F, List.of());

    public static final Codec<Mind> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.FLOAT.fieldOf("mood").forGetter(Mind::mood),
            Codec.FLOAT.fieldOf("sanity").forGetter(Mind::sanity),
            Codec.FLOAT.fieldOf("guilt").forGetter(Mind::guilt),
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
        sanity = Mth.clamp(sanity, 0.0F, MAX);
        guilt = Mth.clamp(guilt, 0.0F, MAX);
        foods = List.copyOf(foods);
    }

    public static Mind get(Player player) {
        return MIND.get(player);
    }

    /** Server side: changes a player's mind, which saves it and sends it to them. */
    public static void update(Player player, UnaryOperator<Mind> change) {
        MIND.update(player, change);
    }

    /** The most sanity can recover to under the guilt carried. */
    public float sanityCap() {
        return MAX - this.guilt * 0.6F;
    }

    public SanityStage stage() {
        return SanityStage.of(this.sanity);
    }

    public MoodLevel moodLevel() {
        return MoodLevel.of(this.mood);
    }

    public Mind withMood(float mood) {
        return new Mind(mood, this.sanity, this.guilt, this.rest, this.food, this.outdoors, this.cheer, this.strain, this.foods);
    }

    public Mind withSanity(float sanity) {
        return new Mind(this.mood, sanity, this.guilt, this.rest, this.food, this.outdoors, this.cheer, this.strain, this.foods);
    }

    public Mind withGuilt(float guilt) {
        return new Mind(this.mood, this.sanity, guilt, this.rest, this.food, this.outdoors, this.cheer, this.strain, this.foods);
    }

    public Mind withNeeds(float rest, float food, float outdoors) {
        return new Mind(this.mood, this.sanity, this.guilt, rest, food, outdoors, this.cheer, this.strain, this.foods);
    }

    public Mind withCheer(float cheer) {
        return new Mind(this.mood, this.sanity, this.guilt, this.rest, this.food, this.outdoors, Mth.clamp(cheer, 0.0F, 25.0F), this.strain, this.foods);
    }

    public Mind withStrain(float strain) {
        return new Mind(this.mood, this.sanity, this.guilt, this.rest, this.food, this.outdoors, this.cheer, Mth.clamp(strain, 0.0F, 25.0F), this.foods);
    }

    public Mind withFoods(List<String> foods) {
        return new Mind(this.mood, this.sanity, this.guilt, this.rest, this.food, this.outdoors, this.cheer, this.strain, foods);
    }
}
