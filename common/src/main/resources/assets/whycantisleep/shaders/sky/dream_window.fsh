#version 330

#moj_import <coolcatcanvas:sky.glsl>

// A dreamer's way home in the sky of the Lucid Deep (DeepClient): a rift, a jagged tear running up the sky, and through
// it their room as it is now. Its edges burn white-hot fading to red, it throws a soft hue of red and violet out into
// the sky round it, cracks run off it into the dark, and the tear breathes, widening and narrowing a little. The room ripples faintly, as if seen through heat.
//   SkyParams[0].x  how lit the room is, 0 to 1
//   SkyParams[0].y  a slow breath of light over the whole rift
uniform sampler2D Sampler0;

in vec3 skyLocal;
in vec3 skyWorld;

out vec4 fragColor;

const vec3 RED = vec3(1.00, 0.18, 0.27);
const vec3 CRIMSON = vec3(0.54, 0.04, 0.14);
const vec3 HOT = vec3(1.00, 0.80, 0.78);
const vec3 VOID = vec3(0.015, 0.012, 0.025);
const vec3 VIOLET = vec3(0.32, 0.08, 0.30);

float hash(float n) {
    return fract(sin(n * 127.1) * 43758.5453);
}

float noise(float x) {
    float i = floor(x);
    float f = fract(x);
    float u = f * f * (3.0 - 2.0 * f);
    return mix(hash(i), hash(i + 1.0), u) * 2.0 - 1.0;
}

// The tear's middle at a height: it zigzags, sharply at small scale and slowly at large.
float spine(float y) {
    return noise(y * 2.2 + 3.0) * 0.16 + noise(y * 7.5 + 11.0) * 0.06 + noise(y * 21.0) * 0.02;
}

// How wide the tear is at a height: widest a little above the middle, closing to points at both ends, ragged.
float width(float y, float time) {
    float body = pow(max(1.0 - pow(abs(y + 0.08) / 0.92, 2.0), 0.0), 0.75);
    float ragged = 1.0 + noise(y * 13.0 + 5.0) * 0.35 + noise(y * 37.0) * 0.12;
    float breath = 1.0 + 0.06 * sin(time * 0.9);
    return 0.34 * body * ragged * breath;
}

void main() {
    vec2 p = skyLocal.xz;
    float time = SkyTime.x;
    float breath = SkyParams[0].y;
    // Up the sky is along the sprite's z.
    float y = p.y;
    float off = p.x - spine(y);
    float w = width(y, time);
    float inside = w - abs(off);

    vec3 color = vec3(0.0);
    float alpha = 0.0;
    if (inside > 0.0) {
        // Through the tear: the room, filling the sprite, shimmering.
        vec2 uv = p * 0.5 + 0.5;
        uv += vec2(sin(uv.y * 18.0 + time * 1.3), cos(uv.x * 15.0 + time * 1.1)) * 0.003;
        vec4 room = texture(Sampler0, uv);
        color = mix(VOID, room.rgb * SkyParams[0].x, room.a);
        // Darker and redder towards the burning edge.
        float edge = smoothstep(0.0, 0.06, inside);
        color = mix(CRIMSON * 0.6, color, edge);
        alpha = 1.0;
    }
    // The edge: a white-hot line, then red light bleeding out into the sky.
    float outside = max(-inside, 0.0);
    float rim = exp(-abs(inside) * 160.0);
    // The bleed is outside the tear only: inside, the room shows clear.
    float bleed = inside > 0.0 ? 0.0 : exp(-outside * 18.0) * step(0.0, w - 0.002);
    color += HOT * rim * 1.2 + RED * bleed * 0.9;
    alpha = max(alpha, clamp(rim * 1.2 + bleed * 0.9, 0.0, 1.0));
    // A hue it gives off all round: a wide, soft haze of light, red near the tear and deepening to crimson and violet
    // further out, pulsing slowly. It fades out before the edges of the sprite, so it has no square border.
    float border = smoothstep(1.0, 0.65, max(abs(p.x), abs(p.y)));
    float pulse = 0.85 + 0.15 * sin(time * 1.4);
    float haze = inside > 0.0 ? 0.0 : (exp(-outside * 4.5) * 0.55 + exp(-outside * 11.0) * 0.35) * border * pulse;
    vec3 hue = mix(VIOLET, mix(CRIMSON, RED, exp(-outside * 6.0)), exp(-outside * 2.5));
    color += hue * haze * (1.0 - alpha);
    alpha += haze * (1.0 - alpha);

    // Cracks running off the tear into the sky: a few thin jagged lines, flickering now and then.
    for (int i = 0; i < 6; i++) {
        float fi = float(i);
        float at = mix(-0.7, 0.75, hash(fi * 3.7 + 1.0));
        float side = hash(fi * 5.1 + 2.0) > 0.5 ? 1.0 : -1.0;
        float reach = 0.25 + 0.35 * hash(fi * 7.3 + 3.0);
        float slope = (hash(fi * 9.9 + 4.0) - 0.5) * 1.2;
        float start = spine(at) + side * width(at, time);
        float along = (p.x - start) * side;
        if (along > 0.0 && along < reach) {
            float line = at + slope * along + noise(along * 25.0 + fi * 13.0) * 0.03;
            float d = abs(y - line);
            float fade = 1.0 - along / reach;
            float flicker = 0.75 + 0.25 * sin(time * (2.0 + fi) + fi * 9.0);
            float crack = exp(-d * 420.0) * fade * flicker;
            color += mix(RED, HOT, crack * 0.5) * crack;
            alpha = max(alpha, clamp(crack, 0.0, 1.0));
        }
    }

    // Everything above was added as light already weighed by how much it covers: weigh it back out for blending.
    if (alpha <= 0.003) discard;
    alpha = min(alpha, 1.0);
    fragColor = sky_finish(vec4(color / alpha * breath, alpha), skyWorld);
}
