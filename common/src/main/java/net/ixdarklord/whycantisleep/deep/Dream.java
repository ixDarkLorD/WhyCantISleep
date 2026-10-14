package net.ixdarklord.whycantisleep.deep;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.ixdarklord.coolcatcore.api.attachment.Attachment;
import net.ixdarklord.whycantisleep.mind.Mind;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * A player's dream: whether they are in the Lucid Deep, and where they fell asleep, which is where they wake. Kept on
 * the player (it outlives a death and a log-out, so a player who leaves mid-dream still wakes where they slept).
 *
 * @param from where they were asleep: the dimension and the spot, empty while they are awake
 */
public record Dream(Optional<Place> from) {
    public static final Dream AWAKE = new Dream(Optional.empty());
    public static final Codec<Dream> CODEC = RecordCodecBuilder.create(i -> i.group(
            Place.CODEC.optionalFieldOf("from").forGetter(Dream::from)
    ).apply(i, Dream::new));

    public static final Attachment<Dream> DREAM = Mind.ATTACHMENTS.builder("dream", CODEC, AWAKE).copyOnDeath().build();

    /** Where a dreamer wakes: the dimension they slept in, and the spot. */
    public record Place(ResourceKey<Level> dimension, Vec3 pos, float yaw) {
        public static final Codec<Place> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(Place::dimension),
                Vec3.CODEC.fieldOf("pos").forGetter(Place::pos),
                Codec.FLOAT.optionalFieldOf("yaw", 0.0F).forGetter(Place::yaw)
        ).apply(i, Place::new));
    }

    public boolean dreaming() {
        return this.from.isPresent();
    }
}
