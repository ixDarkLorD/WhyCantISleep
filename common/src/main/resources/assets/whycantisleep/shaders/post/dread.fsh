#version 330

#moj_import <coolcatcanvas:screen_effect.glsl>

// The Lurker's presence (whycantisleep:dread, driven by LurkerPresence). Strength is how near it is: color drains and
// a crawling darkness closes in from the edges. Approach is how fast it is coming: the view tightens and its edges
// split into red and blue. Pulse is a heartbeat: it flushes the edges red for an instant.
uniform sampler2D InSampler;

layout(std140) uniform DreadConfig {
    float Pulse;
    float Approach;
};

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec2 fromCenter = texCoord - 0.5;
    // 0 at the center of the view, 1 at its corners.
    float edge = length(fromCenter) / 0.7071;

    // The split grows towards the edges, so the middle of the view stays sharp.
    vec2 split = fromCenter * edge * 0.012 * Approach * Strength;
    vec3 color = vec3(texture(InSampler, texCoord + split).r, texture(InSampler, texCoord).g, texture(InSampler, texCoord - split).b);

    // Color drains away, and what is left turns cold.
    float grey = ce_luma(color);
    color = mix(color, vec3(grey) * vec3(0.86, 0.9, 1.0), 0.75 * Strength);

    // The darkness has a ragged edge that drifts, like something breathing at the rim of sight.
    float angle = atan(fromCenter.y, fromCenter.x);
    float ragged = ce_noise(vec2(angle * 3.0, Time * 0.6)) * 0.16 + ce_noise(texCoord * 9.0 + vec2(0.0, Time * 0.25)) * 0.08;
    float inner = mix(0.78, 0.3, clamp(Strength * 0.75 + Approach * 0.35, 0.0, 1.0));
    float dark = smoothstep(inner, inner + 0.55, edge + ragged * Strength);
    color *= 1.0 - dark * (0.55 + 0.4 * Strength) * Strength;

    // A beat: red wells up at the edges and is gone.
    color += vec3(0.55, 0.02, 0.0) * Pulse * Strength * smoothstep(0.35, 1.05, edge);

    fragColor = vec4(color, 1.0);
}
