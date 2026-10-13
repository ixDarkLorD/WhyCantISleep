package net.ixdarklord.whycantisleep.sleep;

import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

/** The kind of threat a blocking monster is, which picks its highlight color when threat colors are on. */
public enum ThreatType {
    EXPLOSIVE(0x55FF55),
    UNDEAD(0xFF5555),
    ARTHROPOD(0xAA55FF),
    ENDER(0xFF55FF),
    RAIDER(0x5599FF),
    OTHER(0xFFAA00);

    private final int color;

    ThreatType(int color) {
        this.color = color;
    }

    /** The highlight color, as {@code 0xRRGGBB}. */
    public int color() {
        return this.color;
    }

    public static ThreatType of(Entity entity) {
        if (entity.is(EntityType.CREEPER)) return EXPLOSIVE;
        if (entity.is(EntityType.ENDERMAN) || entity.is(EntityType.ENDERMITE)) return ENDER;
        if (entity.is(EntityTypeTags.UNDEAD)) return UNDEAD;
        if (entity.is(EntityTypeTags.ARTHROPOD)) return ARTHROPOD;
        if (entity.is(EntityTypeTags.RAIDERS) || entity.is(EntityTypeTags.ILLAGER)) return RAIDER;
        return OTHER;
    }
}
