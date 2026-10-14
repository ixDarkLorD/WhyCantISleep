#version 330

#moj_import <coolcatcanvas:sky.glsl>

// The sky of the Lucid Deep (whycantisleep:lucid_deep): an abyss. Almost nothing shows in it most of the time: black
// overhead, the faintest bruise of navy round the horizon, and far below everything a red light. What is there is
// shown by what lights it:
//   - Lightning: every few seconds a fork of red lightning jumps between two clouds, and for a moment the whole sky
//     lights up, the banks of smoky cloud nearest it most of all.
//   - Surges: now and then the red light below swells, and the clouds take its glow from underneath before it sinks.
//   - Eyes: now and then, a pair of great red eyes opens somewhere in the clouds, looks at you a while, and closes.
// Motes of red light hang in the dark. The colors are the Lurker's palette (LurkerPalette.java).
in vec3 skyLocal;
in vec3 skyWorld;

out vec4 fragColor;

const vec3 ABYSS = vec3(0.004, 0.004, 0.007);
const vec3 NAVY = vec3(0.12, 0.17, 0.29);
const vec3 MAROON = vec3(0.35, 0.14, 0.20);
const vec3 CRIMSON = vec3(0.54, 0.04, 0.14);
const vec3 RED = vec3(1.00, 0.18, 0.27);
const float TAU = 6.2831853;

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

// One bank of cloud: smoke that drifts, thicker in places, with ragged holes.
float cloud(vec2 p, float time, float seed) {
    vec2 drift = vec2(time * 0.012, time * 0.007) + seed;
    float shape = fbm(p * 0.6 + drift);
    float detail = fbm(p * 2.2 - drift * 1.7 + seed * 3.0);
    return smoothstep(0.42, 0.78, shape * 0.75 + detail * 0.35);
}

// The angle between two directions round the sky, from their headings (no seam anywhere).
float apart(vec2 a, vec2 b) {
    return acos(clamp(dot(normalize(a), normalize(b)), -1.0, 1.0));
}

void main() {
    vec3 d = normalize(skyWorld);
    float time = SkyTime.x;
    float up = d.y;
    vec2 heading = normalize(d.xz + vec2(1.0e-4));

    // ---- Lightning: a strike every few seconds, each with its own heading and height, flickering as it dies.
    float period = 4.2;
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

    // ---- The backdrop: near black, the faintest navy at the horizon, the red light far below.
    vec3 color = mix(NAVY * 0.1, ABYSS, smoothstep(0.0, 0.45, up));
    color = mix(color, NAVY * 0.06, smoothstep(0.0, -0.25, up));
    float below = smoothstep(-0.15, -1.0, up);
    color += mix(MAROON * 0.12, CRIMSON * 0.35, below * below) * below * (1.0 + surge * 1.5);
    color += RED * (pow(below, 10.0) * 0.5 + pow(below, 3.0) * 0.05) * (1.0 + surge * 2.0);
    color += NAVY * 0.12 * flash;

    // ---- Clouds: three banks and the haze along the horizon. Dark smoke, all but invisible until something lights it.
    float facing;
    float c1 = cloud(onLayer(d, 60.0, facing) * 0.01, time, 0.0) * facing;
    float c2 = cloud(onLayer(d, -45.0, facing) * 0.012, time, 11.0) * facing;
    float c3 = cloud(onLayer(d, -160.0, facing) * 0.006, time * 0.6, 23.0) * facing;
    float haze = fbm(heading * 3.0 + vec2(time * 0.01, up * 6.0)) * exp(-abs(up) * 7.0);
    color = mix(color, NAVY * 0.07, c1 * 0.8);
    color = mix(color, MAROON * 0.08 + NAVY * 0.03, c2 * 0.85);
    color = mix(color, CRIMSON * 0.12, c3 * 0.6);
    color = mix(color, MAROON * 0.06 + NAVY * 0.03, clamp(haze * 1.1, 0.0, 0.8));
    float clouds = max(max(c1, c2), max(c3, haze));
    // Lit: by the strike (cold at the edges, red at its heart) and, from below, by a surge.
    color += clouds * struck * mix(NAVY * 1.4, RED, clamp(near * 1.4, 0.0, 1.0));
    color += (c2 + c3 + haze * 0.5) * surge * CRIMSON * 0.55 * smoothstep(0.4, -0.3, up);
    color += mix(RED, vec3(1.0, 0.75, 0.75), 0.4) * bolt;

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
