package net.ixdarklord.whycantisleep.client.terror;

import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxDraw;
import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxFrame;
import net.ixdarklord.coolcatcanvas.api.utils.Easing;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Hands coming for the face from past the screen's edges: each slides in along its own way out from the middle of the
 * view (some ways round the screen, from {@link #lay}), snatching as it comes, until by the stare they hold round a
 * window in the middle, fanned round it and meeting with no gap, and from the stare they creep in over it. They lie
 * flat in the view ({@link ViewPlane}), their forearms running straight out past the edge, so they never turn or
 * twist; their fingers point in, palms to the eyes.
 */
public final class EdgeHands {
    // How far in front of the eyes they are; the window's size, a share of the view's half-height there; how much
    // the window closes and the hands grow from the stare.
    private static final double DISTANCE = 0.4;
    private static final double WINDOW_SHARE = 0.32;
    private static final double CREEP_WINDOW = 0.45;
    private static final double CREEP_GROW = 0.35;
    // Where the wrist is along a hand, fingertips to the forearm's end, and the forearm's thickness, a share of the
    // hand's width (as wide as the forearm painted on the hand).
    private static final double WRIST = 0.72;
    private static final double FOREARM_SHARE = 0.77;
    private final List<Reach> hands = new ArrayList<>();
    private double spacing = Math.PI;

    /**
     * Places this many hands, their ways out spread evenly from one angle to another (degrees, 0 right, 90 up), each
     * coming in from its own time, a share of the terror between {@code from} and {@code to}.
     */
    public void lay(RandomSource random, int count, double fromDegrees, double toDegrees, float from, float to) {
        this.hands.clear();
        double start = Math.toRadians(fromDegrees);
        double span = Math.toRadians(toDegrees - fromDegrees);
        this.spacing = span / Math.max(1, count - 1);
        for (int i = 0; i < count; i++) {
            double angle = start + (count == 1 ? span / 2.0 : this.spacing * i) + (random.nextDouble() - 0.5) * this.spacing * 0.3;
            this.hands.add(new Reach(angle, from + random.nextFloat() * (to - from), random.nextFloat() * 6.28F,
                    random.nextInt(TerrorArt.HAND_VARIANTS.length)));
        }
    }

    /** When each hand starts coming, as shares of the terror: for its sound. */
    public List<Float> starts() {
        List<Float> starts = new ArrayList<>();
        for (Reach hand : this.hands) starts.add(hand.from);
        return starts;
    }

    public void render(VfxFrame frame, TerrorStage stage, float age, float t, int tint) {
        if (this.hands.isEmpty()) return;
        ViewPlane plane = ViewPlane.of(frame.cameraPosition(), DISTANCE);
        float time = frame.time();
        float creep = Easing.SINE_IN_OUT.apply(stage.creep(age));
        double window = plane.halfHeight() * WINDOW_SHARE * (1.0 - CREEP_WINDOW * creep);
        double grow = 1.0 + CREEP_GROW * creep;
        Vec3 middle = plane.middle();
        for (Reach hand : this.hands) {
            float come = Easing.SINE_IN_OUT.apply(Mth.clamp((t - hand.from) / Math.max(0.05F, TerrorStage.STARE - hand.from), 0.0F, 1.0F));
            if (t < hand.from) continue;
            double angle = hand.angle + 0.04 * Math.sin(time * 2.0F + hand.phase);
            Vec3 out = plane.out(angle);
            double edge = plane.edge(angle);
            // Long enough to reach past the screen's edge once in, wide enough to meet the next round the window.
            double length = ((edge - window) * 1.25 + 0.03) * grow;
            double width = Mth.clamp((window + (edge - window) * 0.5) * this.spacing * 1.5, length * 0.45, length * 1.1) * grow;
            // Now and then a snatch: in for a moment, the fingers snapping shut; less as it settles round the window.
            float lunge = (float) Math.pow(Math.max(0.0, Math.sin(time * 2.3F + hand.phase)), 6.0) * Mth.lerp(stage.intensity, 0.6F, 1.0F);
            double offScreen = edge + 0.02;
            double reach = Mth.lerp(come, offScreen, window) - lunge * 0.18 * (edge - window) * (1.0F - come);
            reach = Math.max(window * 0.6, reach + window * 0.08 * Math.sin(time * 5.0F + hand.phase));
            Vec3 tips = middle.add(out.scale(reach));
            Vec3 inward = out.scale(-1.0);
            Vec3 center = tips.add(out.scale(length / 2.0));
            Vec3[] corners = plane.quad(center, inward, length / 2.0, width / 2.0);
            // The forearm, straight out past the edge from under the smoke the hand's forearm ends in.
            Vec3 wrist = tips.add(out.scale(length * (WRIST - 0.08)));
            frame.draw(TerrorArt.ARM_LOWER, TerrorArt.HANDS).beam(wrist, wrist.add(out.scale(length * 1.6)), (float) (width * FOREARM_SHARE), tint);
            // Clawing as it comes, snapping shut with each snatch; held round the window, a slow clench.
            float clench = Mth.clamp(0.15F + 0.85F * lunge + 0.1F * Mth.sin(time * 4.0F + hand.phase), 0.0F, 1.0F);
            clench = Mth.lerp(Mth.clamp((come - 0.85F) / 0.15F, 0.0F, 1.0F), clench, 0.55F + 0.25F * Mth.sin(time * 1.5F + hand.phase));
            int shown = Mth.clamp(Math.round(clench * (TerrorArt.HAND_FRAMES - 1)), 0, TerrorArt.HAND_FRAMES - 1);
            float v0 = shown / (float) TerrorArt.HAND_FRAMES;
            float v1 = (shown + 1) / (float) TerrorArt.HAND_FRAMES;
            // Mirrored on the left of the view, so each side's thumbs face the same way round.
            VfxDraw draw = frame.draw(TerrorArt.HAND_VARIANTS[hand.variant], TerrorArt.HANDS);
            if (Math.cos(angle) < 0.0) draw.quad(corners[3], corners[2], corners[1], corners[0], 0.0F, v0, 1.0F, v1, tint);
            else draw.quad(corners[0], corners[1], corners[2], corners[3], 0.0F, v0, 1.0F, v1, tint);
        }
    }

    // A hand: its way out from the middle of the view, when it starts coming (a share of the terror), its own twitch,
    // and which variant it is.
    private record Reach(double angle, float from, float phase, int variant) {
    }
}
