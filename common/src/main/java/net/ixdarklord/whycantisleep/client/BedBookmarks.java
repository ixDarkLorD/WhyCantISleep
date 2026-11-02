package net.ixdarklord.whycantisleep.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientPlayerEvents;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientTickEvents;
import net.ixdarklord.coolcatcore.api.platform.Platform;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.whycantisleep.api.WakeReason;
import net.ixdarklord.whycantisleep.config.WCISClientConfig;
import net.ixdarklord.whycantisleep.config.WCISConfigs;
import net.ixdarklord.whycantisleep.network.WokePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The beds the player has slept in, kept by the client for each world (a file in the config folder, so the server
 * keeps nothing): where each is, how the last full night in it went, how many nights it has seen, and which is the
 * player's respawn point (the server tells, with the sleep trouble). The sleep status popup lists the latest, and
 * Jade and WTHIT show a bed's line when the player looks at it.
 */
public final class BedBookmarks {
    // The most beds kept for one world: the oldest goes past it.
    private static final int MOST = 8;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type FILE_TYPE = new TypeToken<Map<String, List<Bed>>>() {
    }.getType();

    private static @Nullable Map<String, List<Bed>> loaded;
    // The bed slept in now, by its head, while the player sleeps; kept until the morning says how it went.
    private static @Nullable GlobalPos sleepingIn;
    private static @Nullable GlobalPos respawn;

    private BedBookmarks() {
    }

    public static void init() {
        ClientTickEvents.END.register(minecraft -> {
            if (minecraft.player != null && minecraft.level != null && minecraft.player.isSleeping()) {
                minecraft.player.getSleepingPos().ifPresent(pos -> sleepingIn = GlobalPos.of(minecraft.level.dimension(), pos.immutable()));
            }
        });
        ClientPlayerEvents.LEAVE.register(player -> {
            sleepingIn = null;
            respawn = null;
        });
    }

    /**
     * One bed: where it is, its last full night's score, how many nights were slept in it (0 for one only named), when
     * it was last slept in or named, and the name a name tag gave it (null for none).
     */
    public record Bed(String dimension, int x, int y, int z, int score, int nights, long last, @Nullable String name) {
        public GlobalPos pos() {
            return GlobalPos.of(ResourceKey.create(Registries.DIMENSION, new ResourceLocation(this.dimension)), new BlockPos(this.x, this.y, this.z));
        }
    }

    /** The player woke: a full night (to morning, or shaken from a terror) is the bed's last word on how it sleeps. */
    public static void onWoke(WokePayload payload) {
        GlobalPos bed = sleepingIn;
        sleepingIn = null;
        if (bed == null || !(payload.reason() == WakeReason.MORNING || payload.shaken())) return;
        List<Bed> beds = beds(true);
        if (beds == null) return;
        Bed before = find(beds, bed);
        beds.remove(before);
        beds.add(0, new Bed(bed.dimension().location().toString(), bed.pos().getX(), bed.pos().getY(), bed.pos().getZ(), payload.score(),
                before == null ? 1 : before.nights() + 1, System.currentTimeMillis(), before == null ? null : before.name()));
        while (beds.size() > MOST) beds.remove(beds.size() - 1);
        save();
    }

    /** Names a bed (its head), kept even before a night is slept in it. */
    public static void name(GlobalPos bed, String name) {
        List<Bed> beds = beds(true);
        if (beds == null) return;
        Bed before = find(beds, bed);
        beds.remove(before);
        beds.add(0, new Bed(bed.dimension().location().toString(), bed.pos().getX(), bed.pos().getY(), bed.pos().getZ(),
                before == null ? 0 : before.score(), before == null ? 0 : before.nights(), System.currentTimeMillis(), name));
        while (beds.size() > MOST) beds.remove(beds.size() - 1);
        save();
    }

    /** Forgets a bed (its head). */
    public static void forget(GlobalPos bed) {
        List<Bed> beds = beds(false);
        if (beds != null && beds.remove(find(beds, bed))) save();
    }

    /** Where the player respawns, as the server last said (null when not set). */
    public static void onRespawn(@Nullable GlobalPos pos) {
        respawn = pos;
    }

    /** Whether this bed (its head) is where the player respawns. */
    public static boolean isRespawn(GlobalPos bed) {
        return bed.equals(respawn);
    }

    /** The beds kept for this world, the latest slept in first. */
    public static List<Bed> list() {
        if (!WCISConfigs.isEnabled(WCISClientConfig.BED_BOOKMARKS)) return List.of();
        List<Bed> beds = beds(false);
        return beds == null ? List.of() : List.copyOf(beds);
    }

    /** The bed kept at this head, or null. */
    public static @Nullable Bed get(GlobalPos head) {
        List<Bed> beds = beds(false);
        return beds == null ? null : find(beds, head);
    }

    private static @Nullable Bed find(List<Bed> beds, GlobalPos head) {
        for (Bed bed : beds) {
            if (bed.pos().equals(head)) return bed;
        }
        return null;
    }

    // This world's beds, or null outside a world; made when asked to.
    private static @Nullable List<Bed> beds(boolean create) {
        String world = worldKey();
        if (world == null) return null;
        Map<String, List<Bed>> all = load();
        if (create) return all.computeIfAbsent(world, key -> new ArrayList<>());
        List<Bed> beds = all.get(world);
        if (beds != null) beds.sort(Comparator.comparingLong(Bed::last).reversed());
        return beds;
    }

    // A world by its folder in singleplayer, a server by its address.
    private static @Nullable String worldKey() {
        Minecraft minecraft = Minecraft.getInstance();
        IntegratedServer local = minecraft.getSingleplayerServer();
        if (local != null) return "local:" + local.getWorldPath(LevelResource.ROOT).normalize().getFileName();
        ServerData server = minecraft.getCurrentServer();
        return server != null ? "server:" + server.ip : null;
    }

    private static Path file() {
        return Platform.getConfigFolder().resolve(WhyCantISleep.MOD_ID).resolve("beds.json");
    }

    private static Map<String, List<Bed>> load() {
        if (loaded != null) return loaded;
        loaded = new HashMap<>();
        Path file = file();
        if (!Files.exists(file)) return loaded;
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Map<String, List<Bed>> read = GSON.fromJson(reader, FILE_TYPE);
            if (read != null) read.forEach((world, beds) -> loaded.put(world, new ArrayList<>(beds.stream().filter(Objects::nonNull).toList())));
        } catch (IOException | JsonParseException exception) {
            WhyCantISleep.LOGGER.warn("Couldn't read the bed bookmarks in {}", file, exception);
        }
        return loaded;
    }

    private static void save() {
        Path file = file();
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(load(), FILE_TYPE, writer);
            }
        } catch (IOException exception) {
            WhyCantISleep.LOGGER.warn("Couldn't save the bed bookmarks to {}", file, exception);
        }
    }
}
