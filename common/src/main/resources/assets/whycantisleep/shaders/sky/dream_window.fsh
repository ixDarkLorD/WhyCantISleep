#version 330

#moj_import <coolcatlib:sky.glsl>

// A dreamer's way home in the sky of the Dark Between (BetweenClient): a rift, a long tear down the sky that is wide in the
// middle and runs out to fine points at both ends, its sides cut in a few clean, sharp jags; through it their room as
// it is now. Its edge is a thin white-hot line, a huge hue of red and violet glows out from it far into the sky, forks
// of lightning are thrown out of it every moment it is open, and a few fine cracks run off it. It breathes a little. It can be closed: it narrows to a seam and heals in from both ends towards the
// middle, then is gone; and it tears open the same way, from the middle out.
//   SkyParams[0].x  how lit the room is, 0 to 1
//   SkyParams[0].y  a slow breath of light over the whole rift
//   SkyParams[0].z  how open it is, 0 (shut) to 1
//   SkyParams[1]    a lightning strike out of it: x its seed, y how far through it is (0 to 1), z how bright
uniform sampler2D Sampler0;

in vec3 skyLocal;
in vec3 skyWorld;

out vec4 fragColor;

const vec3 RED = vec3(1.00, 0.18, 0.27);
const vec3 CRIMSON = vec3(0.54, 0.04, 0.14);
const vec3 HOT = vec3(1.00, 0.86, 0.84);
const vec3 VOID = vec3(0.015, 0.012, 0.025);
const vec3 VIOLET = vec3(0.32, 0.08, 0.30);
// The sprite is this many times the tear's size (BetweenClient.RIFT_REACH): the tear is drawn in the middle of it, its
// glow and lightning in the room round it.
const float REACH = 2.2;

float hash(float n) {
    return fract(sin(n * 127.1) * 43758.5453);
}

// A zigzag: straight lines between points at even steps, each point pushed to one side by a random amount. It gives the
// sharp, clean jags of a tear rather than the soft wobble of noise.
float zigzag(float x, float seed) {
    float i = floor(x);
    float f = fract(x);
    float a = hash(i + seed) * 2.0 - 1.0;
    float b = hash(i + 1.0 + seed) * 2.0 - 1.0;
    return mix(a, b, f);
}

// The tear's middle line at a height: a gentle lean with a few large jags.
float spine(float y) {
    return y * 0.06 + zigzag(y * 3.2 + 4.0, 1.0) * 0.05 + zigzag(y * 9.0, 7.0) * 0.012;
}

// How wide the tear is at a height (as a share of its length, -1 to 1) when wholly open: a long lens shape, pointed at
// both ends, each side jagged on its own.
float halfWidth(float along, float side, float y) {
    float lens = max(1.0 - along * along, 0.0);
    float jags = 1.0 + zigzag(y * 7.0 + side * 13.0, side * 3.0) * 0.18;
    return 0.27 * pow(lens, 0.85) * jags;
}

// Smooth value noise in one dimension, -1 to 1: the gentle wander of a bolt's path.
float wander(float x, float seed) {
    float i = floor(x);
    float f = fract(x);
    float u = f * f * (3.0 - 2.0 * f);
    return mix(hash(i + seed), hash(i + 1.0 + seed), u) * 2.0 - 1.0;
}

// One stroke of lightning from `from` to `to`: a smooth, wandering path with a bright core and a wide soft glow,
// thinner towards its tip. How bright it is at a point.
float stroke(vec2 at, vec2 from, vec2 to, float seed, float width) {
    vec2 d = to - from;
    float len = max(length(d), 0.0001);
    vec2 dir = d / len;
    vec2 normal = vec2(-dir.y, dir.x);
    float t = dot(at - from, dir) / len;
    if (t < -0.02 || t > 1.02) return 0.0;
    float swing = sin(clamp(t, 0.0, 1.0) * 3.14159);
    float path = (wander(t * 5.0, seed) * 0.07 + wander(t * 13.0, seed + 5.0) * 0.022 + wander(t * 31.0, seed + 9.0) * 0.006) * len * swing;
    float off = abs(dot(at - from, normal) - path);
    float taper = 1.0 - clamp(t, 0.0, 1.0) * 0.75;
    float w = width * taper;
    float core = smoothstep(w, w * 0.25, off);
    float glow = exp(-off / (w * 6.0)) * 0.55 + exp(-off / (w * 22.0)) * 0.18;
    return core * 1.4 + glow;
}

// A strike out of the rift: a main bolt with branches off it, and branches off those, every stroke smooth and bright.
float strike(vec2 at, vec2 from, vec2 to, float seed) {
    float light = stroke(at, from, to, seed, 0.009);
    vec2 d = to - from;
    vec2 normal = normalize(vec2(-d.y, d.x));
    for (int i = 0; i < 4; i++) {
        float fi = float(i);
        float along = 0.2 + 0.18 * fi + hash(seed + fi * 3.1) * 0.08;
        float side = hash(seed + fi * 7.7) > 0.5 ? 1.0 : -1.0;
        vec2 root = from + d * along + normal * (wander(along * 5.0, seed) * 0.07 * length(d) * sin(along * 3.14159));
        vec2 tip = root + d * (0.25 + hash(seed + fi * 1.9) * 0.3) * (1.0 - along * 0.5) + normal * side * length(d) * (0.18 + hash(seed + fi * 4.3) * 0.2);
        light = max(light, stroke(at, root, tip, seed + fi * 11.0, 0.0055));
        // A twig off each branch.
        vec2 twigRoot = mix(root, tip, 0.5);
        vec2 twigTip = twigRoot + (tip - root) * 0.45 + normal * side * -0.12 * length(d) * hash(seed + fi * 2.3);
        light = max(light, stroke(at, twigRoot, twigTip, seed + fi * 17.0, 0.0035));
    }
    return light;
}

void main() {
    vec2 full = skyLocal.xz;
    vec2 p = full * REACH;
    float time = SkyTime.x;
    float breath = SkyParams[0].y;
    float open = clamp(SkyParams[0].z, 0.0, 1.0);
    if (open <= 0.001) discard;
    // Opening runs out from the middle along its length, then widens; closing does the same backwards.
    float reach = smoothstep(0.0, 0.55, open) * 0.9;
    float widen = smoothstep(0.25, 1.0, open);
    float y = p.y;
    float along = y / max(reach, 0.0001);
    float side = p.x - spine(y) > 0.0 ? 1.0 : -1.0;
    float w = abs(along) < 1.0 ? halfWidth(along, side, y) * widen * (1.0 + 0.04 * sin(time * 0.9)) : 0.0;
    // A seam: shut but for a sliver, a fine bright line still shows where it is.
    w = max(w, abs(along) < 1.0 ? 0.004 * (1.0 - along * along) : 0.0);
    float inside = w - abs(p.x - spine(y));

    vec3 light = vec3(0.0);
    float cover = 0.0;
    if (inside > 0.0) {
        // Through the tear: the room, filling the sprite, with a faint shimmer.
        vec2 uv = clamp(p * 0.5 + 0.5, 0.0, 1.0);
        uv += vec2(sin(uv.y * 16.0 + time * 1.2), cos(uv.x * 14.0 + time)) * 0.0025;
        vec4 room = texture(Sampler0, uv);
        vec3 color = mix(VOID, room.rgb * SkyParams[0].x, room.a);
        // A narrow band of deep red just inside the edge, so the opening has a lip.
        light = mix(CRIMSON * 0.7, color, smoothstep(0.0, 0.022, inside));
        cover = 1.0;
    }
    // The edge: one thin, clean white-hot line.
    float rim = smoothstep(0.012, 0.0, abs(inside)) * step(0.0005, w);
    light += HOT * rim * (1.0 - cover);
    cover = max(cover, rim);
    // The hue round it: red close in, deepening to crimson and violet, pulsing slowly, faded out before the sprite's own
    // edges so it has no square border. It shows as much as the rift is open.
    // How far outside the tear: across from its side, and beyond its ends, so the glow rounds off past its points.
    float outside = length(vec2(max(-inside, 0.0), max(abs(y) - reach, 0.0)));
    // It spreads far out into the sky, across the whole sprite, fading out before its edges.
    float border = smoothstep(1.0, 0.55, length(full));
    float pulse = 0.85 + 0.15 * sin(time * 1.3) + 0.05 * sin(time * 3.7);
    float haze = inside > 0.0 ? 0.0 : (exp(-outside * 0.75) * 0.45 + exp(-outside * 3.0) * 0.38 + exp(-outside * 14.0) * 0.38) * border * pulse * open;
    vec3 hue = mix(VIOLET * 0.8, mix(CRIMSON, RED, exp(-outside * 5.0)), exp(-outside * 1.2));
    light += hue * haze * (1.0 - cover);
    cover += haze * (1.0 - cover);

    // A few fine cracks from the widest part of the tear, straight-edged like the jags, fading out.
    for (int i = 0; i < 4; i++) {
        float fi = float(i);
        float at = mix(-0.4, 0.4, hash(fi * 3.7 + 1.0));
        float crackSide = mod(fi, 2.0) < 1.0 ? 1.0 : -1.0;
        float span = (0.18 + 0.2 * hash(fi * 7.3 + 3.0)) * widen;
        float slope = (hash(fi * 9.9 + 4.0) - 0.5) * 0.9;
        float start = spine(at) + crackSide * halfWidth(at / 0.9, crackSide, at) * widen;
        float outward = (p.x - start) * crackSide;
        if (outward > 0.0 && outward < span) {
            float line = at + slope * outward + zigzag(outward * 18.0, fi * 5.0) * 0.012;
            float crack = smoothstep(0.006, 0.0, abs(y - line)) * (1.0 - outward / span);
            light += mix(RED, HOT, 0.3) * crack * (1.0 - cover);
            cover = max(cover, crack);
        }
    }

    // Lightning: a strike out of the rift's edge, when DreamTransition calls one (SkyParams[1]: x its seed, y how far
    // through it is, 0 to 1, z how bright). Three forks, flickering, fading as it ends.
    float strikes = 0.0;
    float life = SkyParams[1].y;
    if (SkyParams[1].z > 0.0 && life < 1.0) {
        float seed = SkyParams[1].x;
        float flicker = (0.55 + 0.45 * step(0.35, fract(life * 9.0 + hash(seed)))) * pow(1.0 - life, 1.4);
        for (int i = 0; i < 3; i++) {
            float fs = seed + float(i) * 13.7;
            float at = mix(-0.6, 0.6, hash(fs + 2.0)) * reach / 0.9;
            float side = hash(fs + 3.0) > 0.5 ? 1.0 : -1.0;
            vec2 from = vec2(spine(at) + side * halfWidth(at / 0.9, side, at) * widen, at);
            float angle = (hash(fs + 4.0) - 0.5) * 1.5;
            vec2 to = from + vec2(side * cos(angle), sin(angle)) * (1.0 + hash(fs + 5.0) * 0.9);
            strikes = max(strikes, strike(p, from, to, fs));
        }
        strikes *= flicker * SkyParams[1].z;
    }
    vec3 boltColor = mix(RED, HOT, clamp(strikes - 0.8, 0.0, 1.0));
    light += boltColor * strikes * (1.0 - cover * 0.5);
    cover = max(cover, clamp(strikes, 0.0, 1.0));

    if (cover <= 0.003) discard;
    cover = min(cover, 1.0);
    fragColor = sky_finish(vec4(light / cover * breath, cover), skyWorld);
}
