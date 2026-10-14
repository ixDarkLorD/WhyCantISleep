#version 330

#moj_import <coolcatcanvas:sky.glsl>

// A dreamer's Window in the sky of the Lucid Deep (DeepClient): a square of their room as it is now (the texture, drawn
// by a scene viewport, comes in upside down) in a frame of dark stone whose inner edge glows red. The room ripples
// faintly, as if seen through water, and darkens towards the frame.
//   SkyParams[0].x  how lit the room is, 0 to 1
//   SkyParams[0].y  a slow breath of light over the whole Window
uniform sampler2D Sampler0;

in vec3 skyLocal;
in vec3 skyWorld;

out vec4 fragColor;

const float INNER = 0.86;
const vec3 RED = vec3(1.00, 0.18, 0.27);
const vec3 CRIMSON = vec3(0.54, 0.04, 0.14);
const vec3 STONE = vec3(0.05, 0.05, 0.07);
const vec3 VOID = vec3(0.015, 0.012, 0.025);

float hash(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

void main() {
    vec2 p = skyLocal.xz;
    float edge = max(abs(p.x), abs(p.y));
    float time = SkyTime.x;
    float breath = SkyParams[0].y;
    vec3 color;
    if (edge < INNER) {
        vec2 uv = p / INNER * 0.5 + 0.5;
        uv.y = 1.0 - uv.y;
        uv += vec2(sin(uv.y * 18.0 + time * 1.3), cos(uv.x * 15.0 + time * 1.1)) * 0.0025;
        vec4 room = texture(Sampler0, uv);
        color = mix(VOID, room.rgb * SkyParams[0].x, room.a);
        // Darker towards the frame, and a faint red cast from its glow.
        float rim = smoothstep(0.55, 1.0, edge / INNER);
        color = color * (1.0 - rim * 0.55) + CRIMSON * rim * 0.12;
    } else {
        // The frame: stone with a grain, its inner edge burning red, its outer edge dark.
        float t = (edge - INNER) / (1.0 - INNER);
        vec2 grain = floor((p + 1.0) * 48.0);
        color = STONE * (0.75 + 0.5 * hash(grain));
        color += RED * exp(-t * 7.0) * 1.1 + CRIMSON * exp(-t * 2.5) * 0.25;
        color *= t > 0.9 ? 0.4 : 1.0;
    }
    fragColor = sky_finish(vec4(color * breath, 1.0), skyWorld);
}