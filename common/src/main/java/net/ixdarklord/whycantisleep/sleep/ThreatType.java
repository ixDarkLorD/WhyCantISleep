package net.ixdarklord.whycantisleep.sleep;

import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;

/**
 * The kind of threat a blocking monster is, which picks its highlight color when threat colors are on, and how
 * dangerous it is, which picks the one pointed out above the rest.
 */
public enum ThreatType {
    EXPLOSIVE(0x55FF55, 5),
    UNDEAD(0xFF5555, 3),
    ARTHROPOD(0xAA55FF, 2),
    ENDER(0xFF55FF, 4),
    RAIDER(0x5599FF, 4),
    OTHER(0xFFAA00, 2);

    private final int color;
    private final int danger;

    ThreatType(int color, int danger) {
        this.color = color;
        this.danger = danger;
    }

    /** How dangerous this kind is, 1 to 5: a creeper above all. */
    public int danger() {
        return this.danger;
    }

    /** The highlight color, as {@code 0xRRGGBB}. */
    public int color() {
        return this.color;
    }

    public static ThreatType of(Entity entity) {
        if (entity.is(EntityTypes.CREEPER)) return EXPLOSIVE;
        if (entity.is(EntityTypes.ENDERMAN) || entity.is(EntityTypes.ENDERMITE)) return ENDER;
        if (entity.is(EntityTypeTags.UNDEAD)) return UNDEAD;
        if (entity.is(EntityTypeTags.ARTHROPOD)) return ARTHROPOD;
        if (entity.is(EntityTypeTags.RAIDERS) || entity.is(EntityTypeTags.ILLAGER)) return RAIDER;
        return OTHER;
    }
}
