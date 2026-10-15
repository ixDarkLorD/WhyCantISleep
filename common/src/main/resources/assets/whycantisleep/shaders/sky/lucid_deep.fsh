#version 330

#moj_import <coolcatlib:sky.glsl>

// The sky of the Lucid Deep (whycantisleep:lucid_deep): an abyss under a storm. Black overhead, a storm-dark band of
// navy and maroon round the horizon, and far below everything a red light; across it all, cloud. What is there is
// shown most by what lights it:
//   - A storm: banks of heavy cloud in navy and maroon churn past, paler where they thin.
//   - Lightning: every couple of seconds a fork of red lightning jumps between two clouds, and for a moment the whole
//     sky lights up, the clouds nearest it most of all.
//   - Surges: now and then the red light below swells, and the clouds take its glow from underneath before it sinks.
//   - Eyes: now and then, a pair of great red eyes opens somewhere in the clouds, looks at you a while, and closes.
//   - The Dark Sign: an eclipse over the south, once the dreamer nears the end (the Watcher, an eye in a crown of
//     thorns weeping red; or DS3, a black disc in a ring of fire with a stream of light pouring from it).
// Motes of red light hang in the dark. The colors are the Lurker's palette (LurkerPalette.java).
in vec3 skyLocal;
in vec3 skyWorld;

out vec4 fragColor;

const vec3 ABYSS = vec3(0.004, 0.004, 0.007);
const vec3 NAVY = vec3(0.12, 0.17, 0.29);
const vec3 MAROON = vec3(0.35, 0.14, 0.20);
const vec3 CRIMSON = vec3(0.54, 0.04, 0.14);
const vec3 RED = vec3(1.00, 0.18, 0.27);
const vec3 GOLD = vec3(1.00, 0.80, 0.50);
const vec3 EMBER = vec3(1.00, 0.40, 0.14);
const vec3 PALE = vec3(1.00, 0.72, 0.66);
const vec3 VIOLET = vec3(0.35, 0.12, 0.42);
const float TAU = 6.2831853;
// The Dark Sign: where it hangs (over the south, a third of the way up the sky) and how big it is (radians).
const vec3 SIGN = vec3(0.181, 0.513, 0.839);
const float SIGN_RADIUS = 0.12;

float hash(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), u.x), mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), u.x), u.y);
}

float fbm(vec2 p) {
    float sum = 0.0;
    float amp = 0.5;
    for (int i = 0; i < 5; i++) {
        sum += noise(p) * amp;
        p = p * 2.03 + vec2(17.1, 9.2);
        amp *= 0.5;
    }
    return sum;
}

// Where a direction meets a flat layer of cloud at a height (negative below the camera), as a point on that layer, and
// how squarely: 0 when the layer is edge on.
vec2 onLayer(vec3 d, float height, out float facing) {
    float t = height / (abs(d.y) < 1.0e-3 ? 1.0e-3 : d.y);
    facing = t > 0.0 ? clamp(abs(d.y) * 3.0, 0.0, 1.0) : 0.0;
    return d.xz * t;
}

// One bank of storm cloud: heavy smoke driven by the wind, churning in on itself, with ragged gaps.
float cloud(vec2 p, float time, float seed) {
    vec2 drift = vec2(time * 0.035, time * 0.018) + seed;
    // The shape is pushed about by a second, slower noise, so it churns rather than slides.
    vec2 churn = vec2(fbm(p * 0.4 + drift * 0.5 + 7.3), fbm(p * 0.4 - drift * 0.4 + 2.1)) - 0.5;
    float shape = fbm(p * 0.55 + drift + churn * 1.6);
    float detail = fbm(p * 2.4 - drift * 2.2 + seed * 3.0 + churn);
    return smoothstep(0.32, 0.72, shape * 0.75 + detail * 0.35);
}

// The angle between two directions round the sky, from their headings (no seam anywhere).
float apart(vec2 a, vec2 b) {
    return acos(clamp(dot(normalize(a), normalize(b)), -1.0, 1.0));
}

// The DS3 Dark Sign (the first design, after Dark Souls 3's eclipse; chosen with /whycantisleep sky sign style
// ds3): a black disc in a blazing rim of gold, a corona of ember and red flaring off it, a wide warm halo; molten
// light hangs in drips from its lower rim, and from the bottom of it a long stream of light pours down the sky to the
// horizon, flowing. The clouds that cross it veil it and are lit at their rims by it.
vec3 signDS3(vec3 color, vec3 d, float up, float time, float c1, float c2, float rim1, float rim2) {
    float signDisc = 0.0;
    float signToward = dot(d, SIGN);
    float signAway = acos(clamp(signToward, -1.0, 1.0));
    color += mix(CRIMSON, EMBER, 0.3) * exp(-signAway * 3.2) * 0.3;  // the sky round it, warmed
    if (signToward > 0.25) {
        vec3 signRight = normalize(cross(vec3(0.0, 1.0, 0.0), SIGN));
        vec3 signUp = cross(SIGN, signRight);
        // This direction on the sky round the Sign, in the disc's radii (its middle at 0, its rim at 1).
        vec2 p = vec2(dot(d, signRight), dot(d, signUp)) / signToward / SIGN_RADIUS;
        float r = length(p);
        float angle = atan(p.y, p.x);
        float edge = r - 1.0;
        // The corona: tongues of flame round the rim, reaching out further here than there, slowly turning.
        float tongues = fbm(vec2(angle * 2.4 + time * 0.04, edge * 1.2 - time * 0.12));
        float flicker = 0.88 + 0.12 * sin(time * 1.7 + angle * 5.0);
        float rim = exp(-abs(edge) * 46.0);
        float corona = exp(-max(edge, 0.0) * mix(5.5, 1.8, tongues)) * smoothstep(-0.02, 0.02, edge);
        float halo = exp(-max(edge, 0.0) * 0.8) * smoothstep(-0.02, 0.05, edge);
        vec3 light = GOLD * rim * 2.4 + mix(GOLD, EMBER, smoothstep(0.0, 0.25, edge)) * corona * 1.3 * flicker
                + mix(EMBER, CRIMSON, smoothstep(0.2, 2.0, edge)) * halo * 0.45;
        // The drips: molten light hanging from the lower rim, each its own length, glowing softly into the dark round
        // it, thinning smoothly from the rim to a swelling bead at its tip; now and then a drop stretches off the bead,
        // falls and fades. Each place looks at its own drip and its neighbours', so their glows run into each other.
        if (p.y < 0.15 && abs(p.x) < 1.05) {
            float home = floor((p.x + 1.0) * 6.5);
            for (int k = -1; k <= 1; k++) {
                float column = home + float(k);
                if (column < 0.0 || column > 12.0 || hash(vec2(column, 8.8)) <= 0.3) continue;
                float x0 = (column + 0.5) / 6.5 - 1.0 + (hash(vec2(column, 4.2)) - 0.5) * 0.05;
                float bottom = -sqrt(max(0.0, 1.0 - x0 * x0));
                float beat = time * (0.08 + hash(vec2(column, 6.3)) * 0.07) + hash(vec2(column, 1.7));
                float swell = fract(beat);  // how far its bead has gathered toward falling
                // Full just as the drop lets go, and back to as it was by the time the next one gathers.
                float full = smoothstep(0.0, 0.85, swell) - smoothstep(0.85, 1.0, swell);
                float reach = (0.12 + hash(vec2(column, 2.6)) * 0.38) * (1.0 + 0.12 * full);
                float down = (bottom - p.y) / reach;  // 0 at the rim, 1 at its tip
                float dx = p.x - x0;
                // The drip: its width easing from the rim to the tip, a bright core and a soft wide glow round it.
                float along = smoothstep(-0.12, 0.05, down) * (1.0 - smoothstep(0.85, 1.05, down));
                float width = mix(0.04, 0.012, smoothstep(0.0, 1.0, down));
                float core = exp(-(dx * dx) / (width * width));
                float glow = exp(-(dx * dx) / (width * width * 9.0)) * 0.35;
                light += mix(GOLD, EMBER, clamp(down, 0.0, 1.0)) * (core + glow) * along * (1.15 - clamp(down, 0.0, 1.0) * 0.45);
                // The bead at its tip, swelling as it gathers.
                float tip = bottom - reach;
                float size = 0.022 + 0.016 * full;
                float bead = length(vec2(dx, (p.y - tip) * 0.85)) / size;
                light += mix(GOLD, EMBER, 0.55) * (exp(-bead * bead) * 0.85 + exp(-bead * bead * 0.15) * 0.18);
                // A drop falling away from it once it is full, stretching as it goes, fading.
                float fall = smoothstep(0.85, 1.0, swell);
                if (fall > 0.0) {
                    float at = tip - fall * fall * 0.45;
                    float drop = length(vec2(dx, (p.y - at) / (1.0 + fall * 1.5))) / 0.02;
                    light += EMBER * exp(-drop * drop) * (1.0 - fall) * 0.9;
                }
            }
        }
        // The stream of light from its bottom down to the horizon: narrowing as it falls, flowing.
        float fall = -1.0 - p.y;  // how far below the disc, in radii
        if (fall > -0.15) {
            float width = 0.11 * exp(-max(fall, 0.0) * 0.28) + 0.016;
            float flow = 0.72 + 0.28 * noise(vec2(p.x * 26.0, fall * 2.5 - time * 1.4));
            float stream = exp(-(p.x * p.x) / (width * width)) * flow * smoothstep(-0.15, 0.25, fall);
            stream *= smoothstep(-0.2, 0.06, up);  // into the haze along the horizon
            light += mix(GOLD, EMBER, smoothstep(0.0, 3.0, fall)) * stream * 1.35;
            light += EMBER * exp(-(p.x * p.x) / (width * width * 9.0)) * 0.12 * smoothstep(-0.2, 0.06, up) * smoothstep(-0.15, 0.25, fall);
        }
        signDisc = smoothstep(1.0, 0.985, r);
        // Clouds crossing it: the high bank veils it most.
        float veil = 1.0 - c1 * 0.6 - c2 * 0.3;
        color = mix(color, vec3(0.012, 0.004, 0.004) + CRIMSON * 0.06 * smoothstep(0.7, 1.0, r), signDisc * (0.75 + 0.25 * veil));
        color += light * (1.0 - signDisc) * veil;
        // And lit by it: the rims of the clouds near it catch its light.
        color += (rim1 + rim2) * EMBER * exp(-max(edge, 0.0) * 0.6) * 0.18 * (1.0 - signDisc);
    }
    return color;
}

// The Dark Sign as it is (the Watcher): inspired by that eclipse, but the Deep's own. The black disc is an eye: a slit
// pupil in a dark iris of crimson threads, looking about a little. Round it a thin hot rim, and standing out from that
// a crown of thorns of red light, each its own length, the crown turning slowly; past them a broken ring of arcs,
// notched, turning the other way, and a fainter dotted one beyond. From its lower rim three thin red tears run down
// the sky, drops sliding down them. Its light is the Deep's, crimson and violet, not gold.
vec3 signWatcher(vec3 color, vec3 d, float up, float time, float c1, float c2, float rim1, float rim2) {
    float toward = dot(d, SIGN);
    float away = acos(clamp(toward, -1.0, 1.0));
    color += mix(CRIMSON, MAROON, 0.4) * exp(-away * 3.0) * 0.3 + VIOLET * exp(-away * 1.4) * 0.05;  // the sky round it
    if (toward <= 0.25) return color;
    vec3 right = normalize(cross(vec3(0.0, 1.0, 0.0), SIGN));
    vec3 upward = cross(SIGN, right);
    vec2 p = vec2(dot(d, right), dot(d, upward)) / toward / SIGN_RADIUS;  // in the disc's radii
    float r = length(p);
    float angle = atan(p.y, p.x);
    float edge = r - 1.0;
    float outside = smoothstep(-0.02, 0.02, edge);

    // The rim, a soft corona and a wide halo.
    vec3 light = PALE * exp(-abs(edge) * 60.0) * 2.0
            + mix(RED, CRIMSON, smoothstep(0.0, 0.3, edge)) * exp(-max(edge, 0.0) * 4.5) * outside * 0.9
            + mix(CRIMSON, VIOLET, smoothstep(0.3, 2.5, edge)) * exp(-max(edge, 0.0) * 0.9) * outside * 0.35;

    // The crown of thorns: a spike in each of 28 places round it, narrowing to its point, slowly turning.
    float turn = angle / TAU + time * 0.004;
    float place = floor(turn * 28.0);
    float across = fract(turn * 28.0) - 0.5;
    float reach = 0.1 + pow(hash(vec2(place, 3.3)), 2.0) * 0.8;
    float along = clamp(edge / reach, 0.0, 1.0);
    float halfWidth = 0.34 * (1.0 - along);
    float thorn = smoothstep(halfWidth, halfWidth * 0.35, abs(across)) * step(0.0, edge) * (1.0 - smoothstep(0.8, 1.0, along));
    float throb = 0.8 + 0.2 * sin(time * 1.1 + place * 2.7);
    light += mix(PALE, CRIMSON, smoothstep(0.0, 0.6, along)) * thorn * throb * 0.9;

    // The broken ring, notched, turning the other way; and a dotted one beyond, fainter.
    float arcs = fract(angle / TAU * 9.0 - time * 0.006);
    float ring = exp(-pow((r - 1.62) / 0.018, 2.0)) * smoothstep(0.0, 0.05, arcs) * smoothstep(1.0, 0.84, arcs);
    float notch = exp(-pow((r - 1.69) / 0.03, 2.0)) * (1.0 - smoothstep(0.006, 0.014, abs(arcs - 0.42)));
    float dots = fract(angle / TAU * 60.0 + time * 0.003);
    float dotted = exp(-pow((r - 1.9) / 0.012, 2.0)) * smoothstep(0.35, 0.2, abs(dots - 0.5));
    light += mix(RED, PALE, 0.25) * (ring * 0.8 + notch * 0.9) + CRIMSON * dotted * 0.5;

    // Three tears down the sky from its lower rim, each with a drop sliding down it.
    for (int i = 0; i < 3; i++) {
        float x0 = i == 0 ? -0.38 : (i == 1 ? 0.06 : 0.44);
        float length0 = i == 0 ? 1.5 : (i == 1 ? 2.6 : 1.9);
        float top = -sqrt(max(0.0, 1.0 - x0 * x0));
        float fall = top - p.y;
        if (fall < -0.05 || fall > length0) continue;
        float sway = 0.035 * sin(fall * 2.6 + float(i) * 1.9);
        float dx = p.x - x0 - sway;
        float width = 0.02 * (1.0 - 0.55 * fall / length0) + 0.006;
        float fade = 1.0 - smoothstep(length0 * 0.55, length0, fall);
        float tear = (exp(-(dx * dx) / (width * width)) + exp(-(dx * dx) / (width * width * 10.0)) * 0.3) * fade * smoothstep(-0.05, 0.05, fall);
        float slide = fract(time * 0.09 + float(i) * 0.37) * length0;
        float drop = length(vec2(dx, (fall - slide) * 0.6)) / 0.025;
        light += mix(RED, CRIMSON, fall / length0) * tear * 0.9 + PALE * exp(-drop * drop) * fade * 0.8;
    }

    // The eye: a dark iris of crimson threads, a slit pupil looking about a little, and a dim glow inside the rim.
    vec2 look = vec2(sin(time * 0.11), sin(time * 0.077 + 1.3)) * 0.07;
    vec2 q = p - look;
    float threads = fbm(vec2(atan(q.y, q.x) * 5.0, length(q) * 2.5));
    float lit = smoothstep(0.2, 0.95, r) * (0.35 + threads * 0.9);
    vec3 iris = mix(vec3(0.02, 0.004, 0.008), CRIMSON * 0.75 + MAROON * 0.2, lit);
    float slitShape = length(vec2(q.x / 0.16, q.y / 0.62));
    float slit = smoothstep(1.0, 0.85, slitShape);
    float slitEdge = exp(-pow((slitShape - 1.0) / 0.12, 2.0));
    vec3 eye = mix(iris, vec3(0.004, 0.0, 0.002), slit) + RED * 0.35 * slitEdge + RED * 0.25 * smoothstep(0.82, 1.0, r);
    float disc = smoothstep(1.0, 0.985, r);
    float veil = 1.0 - c1 * 0.6 - c2 * 0.3;
    color = mix(color, eye, disc * (0.75 + 0.25 * veil));
    color += light * (1.0 - disc) * veil;
    color += (rim1 + rim2) * CRIMSON * exp(-max(edge, 0.0) * 0.6) * 0.2 * (1.0 - disc);
    return color;
}

void main() {
    vec3 d = normalize(skyWorld);
    float time = SkyTime.x;
    float up = d.y;
    vec2 heading = normalize(d.xz + vec2(1.0e-4));

    // ---- Lightning: a strike every few seconds, each with its own heading and height, flickering as it dies.
    float period = 2.6;
    float strike = floor(time / period);
    float into = fract(time / period) * period;
    float flash = 0.0;
    float strikeAngle = hash(vec2(strike, 1.3)) * TAU;
    vec2 strikeHeading = vec2(cos(strikeAngle), sin(strikeAngle));
    float strikeHeight = mix(-0.15, 0.35, hash(vec2(strike, 7.9)));
    if (into < 0.5 && hash(vec2(strike, 3.1)) > 0.15) {
        float flicker = step(0.45, fract(into * 13.0 + hash(vec2(strike, 5.0)))) * 0.65 + 0.35;
        flash = (1.0 - into / 0.5) * flicker;
    }
    float bolt = 0.0;
    if (flash > 0.0) {
        // The fork: a jagged line across the sky between two clouds, near level, at its heading and height.
        float side = dot(vec2(-strikeHeading.y, strikeHeading.x), heading);
        float off = atan(side, dot(strikeHeading, heading));
        float span = 0.45;
        if (abs(off) < span) {
            float along = off / span;
            float path = strikeHeight + (noise(vec2(along * 9.0, strike * 3.7)) - 0.5) * 0.12 + (noise(vec2(along * 31.0, strike)) - 0.5) * 0.03;
            float distance = abs(up - path);
            float ends = smoothstep(1.0, 0.75, abs(along));
            bolt = (exp(-distance * 900.0) + exp(-distance * 90.0) * 0.35) * ends * flash;
            float branch = abs(up - (path - (along - 0.2) * 0.25));
            bolt += exp(-branch * 700.0) * flash * smoothstep(0.2, 0.3, along) * smoothstep(0.55, 0.4, along) * 0.7;
        }
    }
    // How lit this part of the sky is by the strike: all of it a little, near the bolt a great deal.
    float near = exp(-apart(heading, strikeHeading) * 2.2) * exp(-abs(up - strikeHeight) * 3.5);
    float struck = flash * (0.18 + near * 1.6);

    // ---- Surges: every so often the light below swells for a few seconds, lighting the clouds from beneath.
    float surgePeriod = 13.0;
    float surgeInto = fract(time / surgePeriod) * surgePeriod;
    float surge = hash(vec2(floor(time / surgePeriod), 2.2)) > 0.35 ? sin(clamp(surgeInto / 4.0, 0.0, 1.0) * 3.14159) : 0.0;
    surge = surge * surge;

    // ---- The backdrop: near black overhead, a storm-dark band of navy and maroon round the horizon, the red light
    // far below.
    vec3 color = mix(NAVY * 0.22 + MAROON * 0.08, ABYSS, smoothstep(0.0, 0.5, up));
    color = mix(color, NAVY * 0.08 + MAROON * 0.04, smoothstep(0.0, -0.25, up));
    float below = smoothstep(-0.15, -1.0, up);
    color += mix(MAROON * 0.12, CRIMSON * 0.35, below * below) * below * (1.0 + surge * 1.5);
    color += RED * (pow(below, 10.0) * 0.5 + pow(below, 3.0) * 0.05) * (1.0 + surge * 2.0);
    color += NAVY * 0.12 * flash;

    // ---- Clouds: three banks of storm and the haze along the horizon. Dark, heavy and moving, in the palette's navy
    // and maroon, lit from below by the red light, and brightly by the lightning.
    float facing;
    float c1 = cloud(onLayer(d, 60.0, facing) * 0.01, time, 0.0) * facing;
    float c2 = cloud(onLayer(d, -45.0, facing) * 0.012, time, 11.0) * facing;
    float c3 = cloud(onLayer(d, -160.0, facing) * 0.006, time * 0.6, 23.0) * facing;
    float haze = fbm(heading * 3.0 + vec2(time * 0.01, up * 6.0)) * exp(-abs(up) * 7.0);
    // Each bank is darker on top and paler at its rim, where it thins: that is what shows its shape in the dark.
    float rim1 = c1 * (1.0 - c1) * 4.0;
    float rim2 = c2 * (1.0 - c2) * 4.0;
    color = mix(color, NAVY * 0.36 + MAROON * 0.06, c1 * 0.9);
    color += NAVY * 0.2 * rim1;
    color = mix(color, MAROON * 0.42 + NAVY * 0.12, c2 * 0.9);
    color += (MAROON * 0.25 + CRIMSON * 0.12) * rim2;
    color = mix(color, CRIMSON * 0.3 + MAROON * 0.15, c3 * 0.75);
    color = mix(color, MAROON * 0.26 + NAVY * 0.12, clamp(haze * 1.2, 0.0, 0.85));
    float clouds = max(max(c1, c2), max(c3, haze));
    // Lit: by the strike (cold at the edges, red at its heart) and, from below, by a surge.
    color += clouds * struck * mix(NAVY * 1.4, RED, clamp(near * 1.4, 0.0, 1.0));
    color += (c2 + c3 + haze * 0.5) * surge * CRIMSON * 0.55 * smoothstep(0.4, -0.3, up);
    color += mix(RED, vec3(1.0, 0.75, 0.75), 0.4) * bolt;

    // ---- The Dark Sign over the south, once the dreamer is near the end (SkyParams[0]: x how far it has come out,
    // y which design: 0 the Watcher, 1 DS3; DeepSkyClient).
    float signShown = SkyParams[0].x;
    if (signShown > 0.001) {
        vec3 withSign = SkyParams[0].y > 0.5 ? signDS3(color, d, up, time, c1, c2, rim1, rim2)
                : signWatcher(color, d, up, time, c1, c2, rim1, rim2);
        color = mix(color, withSign, signShown);
    }

    // ---- Eyes: every half minute or so, a pair opens somewhere in the clouds, looks, and closes.
    float eyePeriod = 26.0;
    float eyeSlot = floor(time / eyePeriod);
    float eyeInto = fract(time / eyePeriod) * eyePeriod;
    if (hash(vec2(eyeSlot, 9.4)) > 0.25 && eyeInto < 7.0) {
        float eyeAngle = hash(vec2(eyeSlot, 4.7)) * TAU;
        vec2 eyeHeading = vec2(cos(eyeAngle), sin(eyeAngle));
        float eyeHeight = mix(0.08, 0.42, hash(vec2(eyeSlot, 6.1)));
        // Where this direction is on the face that holds the eyes: across (its own heading) and up.
        float across = atan(dot(vec2(-eyeHeading.y, eyeHeading.x), heading), dot(eyeHeading, heading));
        vec2 face = vec2(across, up - eyeHeight) / 0.11;
        // They open slowly, hold, blink once, and close.
        float open = smoothstep(0.0, 1.6, eyeInto) * smoothstep(7.0, 5.6, eyeInto);
        open *= 1.0 - smoothstep(3.3, 3.45, eyeInto) * smoothstep(3.75, 3.6, eyeInto);
        for (int side = -1; side <= 1; side += 2) {
            vec2 e = face - vec2(float(side) * 0.75, 0.0);
            // An almond: as wide as it is, as tall as it is open.
            float height = 0.38 * open + 0.001;
            float shape = length(vec2(e.x / 0.62, e.y / height));
            float lid = smoothstep(1.02, 0.92, shape);
            float glow = exp(-max(shape - 1.0, 0.0) * 2.5) * open;
            float pupil = smoothstep(0.16, 0.07, abs(e.x)) * lid;
            vec3 eye = mix(RED * 1.4, vec3(1.0, 0.55, 0.45), smoothstep(0.7, 0.0, shape) * 0.5);
            color = mix(color, eye, lid * 0.95);
            color = mix(color, vec3(0.02, 0.0, 0.01), pupil * 0.92);
            color += CRIMSON * glow * 0.35 * (1.0 - lid);
        }
    }

    // ---- Motes of red light hanging in the dark, each pulsing slowly on its own beat: on a grid laid over the
    // directions themselves, so there is no seam.
    vec3 grid = d * 36.0;
    vec3 cell = floor(grid);
    vec3 local = fract(grid) - 0.5;
    float seed = hash(cell.xy + cell.z * 17.31);
    if (seed > 0.9) {
        vec3 at = vec3(hash(cell.xy + 3.1), hash(cell.yz + 7.7), hash(cell.zx + 1.3)) - 0.5;
        float pulse = 0.5 + 0.5 * sin(time * (0.6 + seed) + seed * 40.0);
        color += RED * exp(-length(local - at * 0.6) * 18.0) * pulse * (0.55 + flash * 0.6);
    }

    fragColor = sky_finish(vec4(color, 1.0), skyWorld);
}
