package net.ixdarklord.whycantisleep.lurker.client;

import net.ixdarklord.coolcatcanvas.api.client.armature.Armature;
import net.ixdarklord.coolcatcanvas.api.client.armature.ArmatureContext;
import net.ixdarklord.coolcatcanvas.api.client.armature.ArmaturePose;
import net.ixdarklord.coolcatcanvas.api.client.armature.render.ArmatureMobRenderer;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.whycantisleep.lurker.LurkerEntity;
import net.ixdarklord.whycantisleep.lurker.LurkerKind;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.EnumMap;
import java.util.Map;

/**
 * Draws the thing under the bed, each kind with an armature of its own ({@code armatures/lurker_<kind>}, from
 * {@code tools/gen_lurker_armatures.py}): its skin, and over it what glows on it, its eyes above all, which shine in the
 * dark and pulse like embers breathed on. A hallucination is drawn see-through and flickering, and a Flicker wavers.
 * <p>
 * All its motion is procedural, worked out here each frame: where its pupils point and when it blinks, and each kind's
 * own way of holding itself and moving (rising out of the ground as it appears, crouched and watching, reared up and
 * hunting, and its strike). The kinds are:
 * <ul>
 *     <li>The Stalker: a ribbed chest and slung belly between four spidery limbs whose knees stand above its back, a
 *     lashing bladed tail, a wide horned head whose jaw drops open as it strikes.</li>
 *     <li>The Flicker: a legless wraith, a narrow torso trailing a tapering spine under a long horned skull, its arms
 *     too long for it; it drifts and bobs, and hunting it leans in and reaches.</li>
 *     <li>The Brute: a hulk on its knuckles, a plated barrel of a torso on short legs, a small tusked head low between
 *     its shoulders; it lumbers, and rears to bring both fists down.</li>
 *     <li>The Leech: a crawler of five shrinking segments dragging a stinger, little legs under the first three, four
 *     mandibles round its mouth that open like a flower.</li>
 * </ul>
 * The joints' rests are the old vanilla models', so the motion below is theirs, in their terms (radians, pixels, Y
 * down), put onto the armature by {@link #turn} and {@link #move}.
 */
public class LurkerRenderer extends ArmatureMobRenderer<LurkerEntity> {
    private static final Map<LurkerKind, Armature> ARMATURES = new EnumMap<>(LurkerKind.class);

    static {
        for (LurkerKind kind : LurkerKind.values()) ARMATURES.put(kind, Armature.of(WhyCantISleep.id("lurker_" + kind.key())));
    }

    private static final float SCALE = 1.15F;
    // How fast its glow rises and falls, in radians a tick: a slow breath, and a racing pulse.
    private static final float PULSE_WATCHING = 0.12F;
    private static final float PULSE_HUNTING = 0.42F;
    // How far off straight ahead, in degrees, puts a pupil at the edge of its eye.
    private static final float LOOK_RANGE = 35.0F;
    // A blink takes this many ticks to close and open again, and comes at two unequal intervals so it never settles
    // into a rhythm.
    private static final float BLINK_TICKS = 7.0F;
    private static final float[] BLINK_EVERY = {83.0F, 131.0F};
    // Ticks it takes to rise out of the ground, and how far under it each kind starts, in model pixels.
    private static final float EMERGE_TICKS = 24.0F;
    private static final float STALKER_DEPTH = 15.0F;
    private static final float FLICKER_DEPTH = 26.0F;
    private static final float BRUTE_DEPTH = 26.0F;
    private static final float LEECH_DEPTH = 9.0F;
    // How far an upper Stalker limb leans out from straight up, and how far a lower one leans out from straight down.
    private static final float UPPER_LEAN = 0.8F;
    private static final float LOWER_LEAN = 0.15F;
    private static final String[] STALKER_LIMBS = {"right_arm", "left_arm", "right_leg", "left_leg"};
    private static final String[] SIDES = {"right", "left"};

    public LurkerRenderer(EntityRendererProvider.Context context) {
        super(context, ARMATURES.get(LurkerKind.STALKER), 0.5F);
        this.armature(entity -> ARMATURES.get(entity.kind()));
        this.scale(SCALE);
        // Its glow rises and falls: slowly while it watches, quick as a pulse while it hunts.
        this.glow((entity, partialTicks) -> {
            float hunt = entity.huntAmount(partialTicks);
            float beat = Mth.sin(age(entity, partialTicks) * (PULSE_WATCHING + (PULSE_HUNTING - PULSE_WATCHING) * hunt));
            return 0.72F + 0.28F * beat;
        });
        // A hallucination is not quite there, and neither is a Flicker.
        this.translucent((entity, partialTicks) -> entity.isHallucination() || entity.kind() == LurkerKind.FLICKER);
        this.tint(LurkerRenderer::tint);
        this.animate(animator -> animator.modify(LurkerRenderer::pose));
    }

    // As vanilla's mobs: no name over it unless it was given one and is looked at.
    @Override
    protected boolean shouldShowName(LurkerEntity entity, double distanceToCameraSq) {
        return super.shouldShowName(entity, distanceToCameraSq) && (entity.shouldShowName()
                || entity.hasCustomName() && entity == this.entityRenderDispatcher.crosshairPickEntity);
    }

    private static int tint(LurkerEntity entity, float partialTicks) {
        float age = age(entity, partialTicks);
        // A Flicker wavers, without ever quite going.
        if (!entity.isHallucination()) return entity.kind() == LurkerKind.FLICKER ? ARGB.white(0.7F + Mth.sin(age * 0.6F) * 0.12F) : -1;
        // Mostly there, with a tremble, and now and then nearly gone for an instant.
        float flicker = 0.62F + Mth.sin(age * 0.9F) * 0.08F - (Mth.sin(age * 0.23F) > 0.93F ? 0.4F : 0.0F);
        return ARGB.white(Mth.clamp(flicker, 0.1F, 1.0F));
    }

    private static float age(LurkerEntity entity, float partialTicks) {
        return entity.tickCount + partialTicks;
    }

    // ---- Motion ----

    /** What every kind's motion reads this frame. */
    private record Frame(float age, float hunt, float watch, float strike, float emerge, float walk, float speed, float headYaw, float headPitch) {
        /** How far under the ground it still is, in model pixels, starting {@code depth} down and easing up. */
        float sunk(float depth) {
            float left = 1.0F - this.emerge;
            return left * left * depth;
        }
    }

    private static void pose(ArmaturePose pose, ArmatureContext ctx) {
        if (!(ctx.entity() instanceof LurkerEntity entity)) return;
        float partialTicks = ctx.partialTick();
        float age = age(entity, partialTicks);
        float hunt = entity.huntAmount(partialTicks);
        Frame f = new Frame(age, hunt, 1.0F - hunt, Mth.sin(entity.getAttackAnim(partialTicks) * Mth.PI),
                Mth.clamp(age / EMERGE_TICKS, 0.0F, 1.0F), ctx.limbSwing(), ctx.limbSwingAmount(), ctx.headYaw(), ctx.headPitch());
        switch (entity.kind()) {
            case STALKER -> stalker(pose, f);
            case FLICKER -> flicker(pose, f);
            case BRUTE -> brute(pose, f);
            case LEECH -> leech(pose, f);
        }
        eyes(pose, entity, f, partialTicks);
    }

    /**
     * Its pupils slide towards whoever is looking at it, as far as the eye allows, ahead of its head, and dart about
     * while it watches; its lids hang low while it watches, draw right back while it hunts, and come down together
     * when it blinks.
     */
    private static void eyes(ArmaturePose pose, LurkerEntity entity, Frame f, float partialTicks) {
        float lookX = 0.0F;
        float lookY = 0.0F;
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            Vec3 to = player.getEyePosition(partialTicks).subtract(entity.getEyePosition(partialTicks));
            float yaw = (float) (Mth.atan2(to.z, to.x) * Mth.RAD_TO_DEG) - 90.0F;
            float pitch = (float) -(Mth.atan2(to.y, to.horizontalDistance()) * Mth.RAD_TO_DEG);
            lookX = Mth.clamp(Mth.wrapDegrees(yaw - Mth.rotLerp(partialTicks, entity.yHeadRotO, entity.yHeadRot)) / LOOK_RANGE, -1.0F, 1.0F);
            lookY = Mth.clamp((pitch - entity.getXRot(partialTicks)) / LOOK_RANGE, -1.0F, 1.0F);
        }
        // Watching, they also dart: a small jump to somewhere else a few times a second.
        float dart = (float) Math.floor(f.age / 9.0F) + entity.getId();
        lookX = Mth.clamp(lookX + Mth.sin(dart * 12.9898F) * 0.3F * f.watch, -1.0F, 1.0F);
        lookY = Mth.clamp(lookY + Mth.sin(dart * 78.233F) * 0.2F * f.watch, -1.0F, 1.0F);

        float blink = 0.0F;
        for (float every : BLINK_EVERY) {
            float into = (f.age + entity.getId() * 17) % every;
            if (into < BLINK_TICKS) blink = Math.max(blink, Mth.sin(into / BLINK_TICKS * Mth.PI));
        }
        float lid = Mth.clamp(f.watch * 0.34F + f.hunt * 0.08F + blink, 0.08F, 1.0F);
        for (String eye : new String[]{"right_eye", "left_eye"}) {
            // A positive turn of the head swings its face towards -x, and a positive tilt towards +y: so do these.
            move(pose, eye + "_pupil", -lookX * 0.65F, lookY * 0.3F + lid * 0.25F, 0.0F);
            // Narrow slits while it hunts.
            pose.scale(eye + "_pupil", 1.0F - f.hunt * 0.35F, 1.0F, 1.0F);
            // The lid is as tall as the eye; how much of it shows is its height.
            pose.scale(eye + "_lid", 1.0F, lid, 1.0F);
        }
    }

    /** Crouched low and nearly still while it watches, reared up and scuttling while it hunts. */
    private static void stalker(ArmaturePose pose, Frame f) {
        float sunk = f.sunk(STALKER_DEPTH);
        float breath = Mth.sin(f.age * (0.1F + f.hunt * 0.12F));
        // Watching it crouches, belly near the ground; hunting it rears up on its limbs.
        float crouch = f.watch * 3.2F - f.hunt * 0.8F;
        float walk = f.walk * 0.9F;
        float speed = f.speed;

        // Watching, its head tips slowly to one side and back, like something listening; hunting, it shudders. It darts
        // forward as it strikes.
        turn(pose, "head", f.headPitch * Mth.DEG_TO_RAD - f.strike * 0.35F - f.hunt * 0.12F, f.headYaw * Mth.DEG_TO_RAD,
                f.watch * (Mth.sin(f.age * 0.045F) * 0.38F + Mth.sin(f.age * 0.35F) * 0.03F) + f.hunt * Mth.sin(f.age * 1.3F) * 0.07F);
        move(pose, "head", 0.0F, crouch + sunk + breath * 0.3F, -(f.strike * 3.0F + f.hunt));
        // Its jaw hangs a little open, works as it breathes, gapes while it hunts and drops wide as it strikes.
        turn(pose, "jaw", 0.1F + Mth.sin(f.age * 0.2F) * 0.06F + f.hunt * (0.35F + Mth.sin(f.age * 0.9F) * 0.12F) + f.strike * 0.75F, 0.0F, 0.0F);

        move(pose, "chest", 0.0F, crouch + sunk + breath * 0.4F, 0.0F);
        // Hunting, its shoulders roll with each stride.
        turn(pose, "chest", -f.hunt * 0.1F + f.watch * 0.06F, 0.0F, Mth.sin(walk) * 0.09F * speed * f.hunt);
        turn(pose, "belly", -0.14F + breath * 0.04F - f.watch * 0.1F, Mth.cos(walk) * 0.25F * speed, 0.0F);

        // The tail: a slow curl while it watches, a lash while it hunts, each length a little behind the one before.
        float lash = 0.18F + f.hunt * 0.5F;
        for (int i = 0; i < 4; i++) {
            turn(pose, "tail_" + i, 0.16F + f.hunt * 0.14F + Mth.sin(f.age * 0.09F - i * 0.6F) * 0.12F,
                    Mth.sin(f.age * lash - i * 0.8F) * (0.22F + f.hunt * 0.25F), 0.0F);
        }

        for (int i = 0; i < 4; i++) {
            boolean right = i % 2 == 0;
            boolean front = i < 2;
            float side = right ? -1.0F : 1.0F;
            // A scuttle: the limbs of each diagonal move together, each knee bending as its foot lifts.
            float phase = right == front ? 0.0F : Mth.PI;
            float swing = Mth.cos(walk + phase) * speed;
            float lift = Math.max(0.0F, Mth.sin(walk + phase)) * speed;
            // Standing still, one foot at a time lifts and taps, now and then.
            float tap = Math.max(0.0F, Mth.sin(f.age * 0.23F + i * 1.9F) - 0.86F) * 7.0F * f.watch * (1.0F - Math.min(1.0F, speed * 4.0F));
            // Coming up out of the ground its limbs reach high and wide before they take its weight.
            float reach = (1.0F - f.emerge) * 0.9F;
            // Crouched, the knees fold out further so the body can sink between them.
            float upperZ = side * (UPPER_LEAN + lift * 0.25F + f.watch * 0.28F + tap * 0.12F - reach * 0.5F);
            float upperX = swing * (0.6F + f.hunt * 0.25F) + (front ? -0.15F - f.hunt * 0.2F : 0.2F) - reach * (front ? 0.6F : -0.3F);
            String upper = STALKER_LIMBS[i];
            turn(pose, upper, upperX, side * (front ? 0.25F : -0.35F), upperZ);
            // Undo the upper part's lean so the lower one hangs down, a little outward.
            turn(pose, upper + "_lower", -upperX * 0.8F - tap * 0.25F, 0.0F, -upperZ + side * (LOWER_LEAN + tap * 0.2F));
            move(pose, upper, 0.0F, crouch * 0.85F + sunk + breath * 0.4F, 0.0F);
        }
    }

    /** Hanging in the air and bobbing; hunting, it leans in and reaches for its player with both arms. */
    private static void flicker(ArmaturePose pose, Frame f) {
        float speed = f.speed;
        // Hunting, it leans in towards its player and comes down a little.
        float torsoX = f.hunt * 0.35F + speed * 0.2F + Mth.sin(f.age * 0.07F) * 0.04F;
        move(pose, "torso", 0.0F, Mth.sin(f.age * 0.09F) * 1.2F + f.hunt * 2.0F + f.sunk(FLICKER_DEPTH), 0.0F);
        turn(pose, "torso", torsoX, 0.0F, Mth.sin(f.age * 0.05F) * 0.05F);

        // The head keeps to where it is looking whatever the body does; watching, it tips to one side.
        turn(pose, "head", f.headPitch * Mth.DEG_TO_RAD - torsoX - f.strike * 0.3F, f.headYaw * Mth.DEG_TO_RAD,
                f.watch * Mth.sin(f.age * 0.04F) * 0.3F + f.hunt * Mth.sin(f.age * 1.5F) * 0.06F);
        turn(pose, "jaw", 0.08F + f.hunt * (0.4F + Mth.sin(f.age * 0.8F) * 0.1F) + f.strike * 0.7F, 0.0F, 0.0F);

        // The spine trails behind and sways, each length after the one above it.
        for (int i = 0; i < 4; i++) {
            turn(pose, "spine_" + i, 0.12F + speed * 0.3F + f.hunt * 0.1F + Mth.sin(f.age * 0.11F - i * 0.7F) * 0.14F, 0.0F,
                    Mth.sin(f.age * 0.08F - i * 0.9F) * 0.16F);
        }

        for (int i = 0; i < 2; i++) {
            float side = i == 0 ? -1.0F : 1.0F;
            // Watching, its arms hang, the hands turning slowly; hunting, they come up and reach, and rake as it strikes.
            float sway = Mth.sin(f.age * 0.06F + i * 2.1F);
            turn(pose, SIDES[i] + "_arm", f.watch * (0.08F + sway * 0.06F) + f.hunt * (-1.25F + Mth.sin(f.age * 0.5F + i * Mth.PI) * 0.18F) - f.strike * 0.6F,
                    0.0F, side * (0.12F + f.watch * sway * 0.05F - f.hunt * 0.1F));
            turn(pose, SIDES[i] + "_forearm", -0.25F - f.hunt * 0.35F + f.strike * 0.5F, 0.0F, 0.0F);
        }
    }

    /** Lumbering on its knuckles, each arm swinging with the opposite leg; striking, it rears and brings both fists down. */
    private static void brute(ArmaturePose pose, Frame f) {
        float sunk = f.sunk(BRUTE_DEPTH);
        float breath = Mth.sin(f.age * (0.07F + f.hunt * 0.08F));
        float walk = f.walk * 0.6F;
        float speed = f.speed;

        move(pose, "hips", 0.0F, sunk, 0.0F);
        turn(pose, "hips", 0.0F, 0.0F, Mth.sin(walk) * 0.06F * speed);
        // It carries itself bent forward over its fists; rearing to strike, it straightens.
        float torsoX = 0.32F + f.watch * 0.08F + breath * 0.02F - f.strike * 0.45F;
        turn(pose, "torso", torsoX, Mth.sin(walk) * 0.12F * speed, 0.0F);

        // The head is sunk too deep in its shoulders to turn far.
        turn(pose, "head", f.headPitch * Mth.DEG_TO_RAD - torsoX + 0.1F, Mth.clamp(f.headYaw, -40.0F, 40.0F) * Mth.DEG_TO_RAD,
                f.watch * Mth.sin(f.age * 0.035F) * 0.12F);
        turn(pose, "jaw", 0.06F + Mth.sin(f.age * 0.15F) * 0.04F + f.hunt * 0.25F + f.strike * 0.6F, 0.0F, 0.0F);

        for (int i = 0; i < 2; i++) {
            float side = i == 0 ? -1.0F : 1.0F;
            // Each arm swings with the leg on the other side.
            float swing = Mth.cos(walk + (i == 0 ? 0.0F : Mth.PI)) * speed;
            move(pose, SIDES[i] + "_leg", 0.0F, sunk, 0.0F);
            turn(pose, SIDES[i] + "_leg", -swing * 0.7F, 0.0F, 0.0F);
            // Undo the torso's lean so the arms hang to the ground, then swing, and throw them up to strike.
            turn(pose, SIDES[i] + "_arm", -torsoX + swing * 0.55F - f.strike * 2.2F, 0.0F, side * -(0.1F + f.hunt * 0.12F + breath * 0.02F));
            turn(pose, SIDES[i] + "_forearm", -0.12F - Math.max(0.0F, swing) * 0.3F - f.strike * 0.5F, 0.0F, 0.0F);
        }
    }

    /** A slither along the ground, its legs scrabbling; hunting, it lifts its head, and striking it lunges. */
    private static void leech(ArmaturePose pose, Frame f) {
        float sunk = f.sunk(LEECH_DEPTH);
        float speed = f.speed;
        // A wave runs down its length: slow and slight while it lies in wait, wide while it moves.
        float wave = f.walk * 0.9F + f.age * 0.07F;
        float bend = 0.1F + speed * 0.3F;
        for (int i = 0; i < 5; i++) {
            // Its tail end lifts a little, the stinger with it.
            turn(pose, "segment_" + i, i >= 3 ? -0.12F - f.hunt * 0.15F + Mth.sin(f.age * 0.1F - i) * 0.05F : 0.0F,
                    Mth.sin(wave - i * 0.9F) * bend * (i == 0 ? 0.5F : 1.0F), 0.0F);
        }
        move(pose, "segment_0", 0.0F, sunk, 0.0F);

        move(pose, "head", 0.0F, sunk - f.hunt, -f.strike * 3.0F);
        turn(pose, "head", f.headPitch * Mth.DEG_TO_RAD - f.hunt * 0.2F,
                Mth.clamp(f.headYaw, -50.0F, 50.0F) * Mth.DEG_TO_RAD + Mth.sin(wave + 0.9F) * bend * 0.5F,
                f.watch * Mth.sin(f.age * 0.05F) * 0.12F);

        // The mandibles work open and shut, faster while it hunts, and gape wide as it strikes.
        float open = 0.2F + (0.5F + 0.5F * Mth.sin(f.age * (0.25F + f.hunt * 0.5F))) * 0.4F + f.strike * 0.6F;
        turn(pose, "right_mandible", 0.0F, open, 0.0F);
        turn(pose, "left_mandible", 0.0F, -open, 0.0F);
        turn(pose, "top_mandible", -open, 0.0F, 0.0F);
        turn(pose, "bottom_mandible", open, 0.0F, 0.0F);

        // Its legs scrabble, each pair a step behind the one in front, the two sides in turn.
        for (int i = 0; i < 6; i++) {
            float side = i % 2 == 0 ? 1.0F : -1.0F;
            float step = f.walk * 1.6F + (i / 2) * 1.3F + (i % 2) * Mth.PI;
            turn(pose, SIDES[i % 2] + "_leg_" + i / 2, Mth.cos(step) * 0.7F * speed, 0.0F, side * Math.max(0.0F, Mth.sin(step)) * 0.3F * speed);
        }
    }

    /**
     * Turns a joint further by a vanilla model part's rotation: radians, about the model's own axes. Vanilla's Y is down
     * and the armature's up, which reverses turns about x and z; in Blockbench's units (which reverse x and y again)
     * that is (x, -y, -z).
     */
    private static void turn(ArmaturePose pose, String joint, float x, float y, float z) {
        pose.rotate(joint, x * Mth.RAD_TO_DEG, -y * Mth.RAD_TO_DEG, -z * Mth.RAD_TO_DEG);
    }

    /** Moves a joint by a vanilla model part's offset: pixels, Y down. */
    private static void move(ArmaturePose pose, String joint, float x, float y, float z) {
        pose.translate(joint, -x, -y, z);
    }
}
