package net.ixdarklord.whycantisleep.lurker;

import net.minecraft.util.RandomSource;

import java.util.Locale;

/**
 * The kinds of Lurker. They are one creature underneath (one entity, the same watching and hunting) and four to look
 * at, each with a body, a skin and a way of moving of its own, and one thing each that only it does:
 * <ul>
 *     <li>the <b>Stalker</b>, on four spider's legs: nothing but patience and teeth;</li>
 *     <li>the <b>Flicker</b>, a legless wraith with arms too long for it, half there: hunting, it steps out of sight and reappears behind its player
 *     whenever they look away from it;</li>
 *     <li>the <b>Brute</b>, a hulk on its knuckles, slow and hard to kill: it roars, and its roar slows its player, blinds them and
 *     shakes their lucidity;</li>
 *     <li>the <b>Leech</b>, a segmented crawler quicker than a sprint: its bite does little harm but leaves its player starving,
 *     weak and miserable, and feeds it.</li>
 * </ul>
 * Every number here is a first guess, to be tuned by play.
 */
public enum LurkerKind {
    STALKER(30.0, 0.27, 5.0, 1.0, 0.0, 40),
    FLICKER(18.0, 0.25, 4.0, 1.0, 0.0, 25),
    BRUTE(60.0, 0.2, 9.0, 1.2, 0.8, 20),
    LEECH(14.0, 0.32, 2.0, 0.85, 0.0, 15);

    private static final LurkerKind[] VALUES = values();

    public final double health;
    /** A player sprints at about 0.28 on this scale. */
    public final double speed;
    public final double damage;
    /** Its size beside the Stalker's; its reach and the box it is hit in grow with it. */
    public final double scale;
    public final double knockbackResistance;
    // How often this kind comes, beside the others.
    private final int weight;

    LurkerKind(double health, double speed, double damage, double scale, double knockbackResistance, int weight) {
        this.health = health;
        this.speed = speed;
        this.damage = damage;
        this.scale = scale;
        this.knockbackResistance = knockbackResistance;
        this.weight = weight;
    }

    /** Its name in commands, saves, translation keys and texture names. */
    public String key() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    public static LurkerKind byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : STALKER;
    }

    public static LurkerKind byKey(String key) {
        for (LurkerKind kind : VALUES) if (kind.key().equals(key)) return kind;
        return STALKER;
    }

    /** One of the kinds, the commoner ones more often. */
    public static LurkerKind pick(RandomSource random) {
        int total = 0;
        for (LurkerKind kind : VALUES) total += kind.weight;
        int roll = random.nextInt(total);
        for (LurkerKind kind : VALUES) {
            roll -= kind.weight;
            if (roll < 0) return kind;
        }
        return STALKER;
    }
}