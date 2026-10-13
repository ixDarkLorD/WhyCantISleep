package net.ixdarklord.whycantisleep.lurker;

import com.mojang.serialization.Codec;
import net.ixdarklord.coolcatcore.api.attachment.Attachment;
import net.ixdarklord.coolcatcore.api.attachment.AttachmentRegistry;
import net.ixdarklord.coolcatcore.api.attachment.SyncPolicy;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * A player's dread: how far the thing under the bed has come for them, 0 to {@link #MAX}. It grows with nights slept
 * in the dark and with long stretches without sleep, and falls with nights slept in the light. It is saved with the
 * player and sent to their own client, which draws what each {@link Stage} brings.
 */
public final class Dread {
    public static final int MAX = 100;
    public static final AttachmentRegistry ATTACHMENTS = AttachmentRegistry.create(WhyCantISleep.MOD_ID);
    public static final Attachment<Integer> DREAD = ATTACHMENTS.builder("dread", Codec.intRange(0, MAX), 0)
            .networkCodec(ByteBufCodecs.VAR_INT).sync(SyncPolicy.SELF).copyOnDeath().build();

    private Dread() {
    }

    public static int get(Player player) {
        return DREAD.get(player);
    }

    /** Server side. */
    public static void set(Player player, int dread) {
        DREAD.set(player, Mth.clamp(dread, 0, MAX));
    }

    /** Server side. */
    public static void add(Player player, int amount) {
        set(player, get(player) + amount);
    }

    public static Stage stage(Player player) {
        return Stage.of(get(player));
    }

    /** What a level of dread brings. */
    public enum Stage {
        /** Nothing. */
        CALM(0),
        /** Eyes under beds in the dark, and whispers. Only the player sees and hears them. */
        WATCHED(25),
        /** It comes out: a figure only the player sees, gone when looked at. */
        HAUNTED(50),
        /** It is real, and it hunts. */
        HUNTED(85);

        private final int from;

        Stage(int from) {
            this.from = from;
        }

        /** The dread this stage starts at. */
        public int from() {
            return this.from;
        }

        public boolean reached(Stage other) {
            return this.ordinal() >= other.ordinal();
        }

        public static Stage of(int dread) {
            Stage stage = CALM;
            for (Stage candidate : values()) {
                if (dread >= candidate.from) stage = candidate;
            }
            return stage;
        }
    }
}
