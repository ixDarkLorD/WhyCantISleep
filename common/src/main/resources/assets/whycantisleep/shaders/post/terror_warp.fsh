#version 330
#extension GL_ARB_separate_shader_objects : require

#include <coolcatlib:screen_effect.glsl>

// The night terror's warped surroundings (NightTerrorClient): the world ripples and slowly twists, more towards the
// edges of the view than in its middle, where the demon is; each heartbeat swells it and pulls the view in a little,
// and the colors split at the edges.
uniform sampler2D InSampler;

layout(std140) uniform TerrorWarpConfig {
    float Amount;
    float Beat;
};

layout(location = 0) in vec2 texCoord;

layout(location = 0) out vec4 fragColor;

void main() {
    vec2 fromCenter = texCoord - 0.5;
    float edge = clamp(length(fromCenter) * 2.0, 0.0, 1.0);
    float warp = Amount * Strength * (0.25 + 0.75 * edge * edge) * (1.0 + Beat);
    vec2 uv = texCoord;
    uv += vec2(sin(uv.y * 9.0 + Time * 1.7), cos(uv.x * 7.0 - Time * 1.3)) * 0.012 * warp;
    float angle = 0.06 * warp * edge * sin(Time * 0.7);
    mat2 turn = mat2(cos(angle), -sin(angle), sin(angle), cos(angle));
    uv = 0.5 + turn * (uv - 0.5);
    uv = 0.5 + (uv - 0.5) * (1.0 - 0.015 * Beat * Amount * Strength);
    vec2 split = fromCenter * 0.008 * warp;
    vec3 color = vec3(texture(InSampler, uv + split).r, texture(InSampler, uv).g, texture(InSampler, uv - split).b);
    fragColor = vec4(color, 1.0);
}
