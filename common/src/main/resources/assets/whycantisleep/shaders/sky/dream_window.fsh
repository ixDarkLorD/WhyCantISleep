#version 330

#moj_import <coolcatcanvas:sky.glsl>

// A dreamer's way home in the sky of the Lucid Deep (DeepClient): a rift, a long tear down the sky that is wide in the
// middle and runs out to fine points at both ends, its sides cut in a few clean, sharp jags; through it their room as
// it is now. Its edge is a thin white-hot line, a soft hue of red and violet glows out from it, and a few fine cracks
// run off it. It breathes a little. It can be closed: it narrows to a seam and heals in from both ends towards the
// middle, then is gone; and it tears open the same way, from the middle out.
//   SkyParams[0].x  how lit the room is, 0 to 1
//   SkyParams[0].y  a slow breath of light over the whole rift
//   SkyParams[0].z  how open it is, 0 (shut) to 1
uniform sampler2D Sampler0;

in vec3 skyLocal;
in vec3 skyWorld;

out vec4 fragColor;

const vec3 RED = vec3(1.00, 0.18, 0.27);
const vec3 CRIMSON = vec3(0.54, 0.04, 0.14);
const vec3 HOT = vec3(1.00, 0.86, 0.84);
const vec3 VOID = vec3(0.015, 0.012, 0.025);
const vec3 VIOLET = vec3(0.32, 0.08, 0.30);

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

void main() {
    vec2 p = skyLocal.xz;
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
        vec2 uv = p * 0.5 + 0.5;
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
    float outside = max(-inside, 0.0);
    float border = smoothstep(1.0, 0.7, max(abs(p.x), abs(p.y)));
    float pulse = 0.88 + 0.12 * sin(time * 1.3);
    float haze = inside > 0.0 ? 0.0 : (exp(-outside * 5.0) * 0.5 + exp(-outside * 16.0) * 0.4) * border * pulse * open;
    vec3 hue = mix(VIOLET, mix(CRIMSON, RED, exp(-outside * 7.0)), exp(-outside * 3.0));
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

    if (cover <= 0.003) discard;
    cover = min(cover, 1.0);
    fragColor = sky_finish(vec4(light / cover * breath, cover), skyWorld);
}
