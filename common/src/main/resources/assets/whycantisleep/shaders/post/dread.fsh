#version 330

#moj_import <coolcatcanvas:screen_effect.glsl>

// The Lurker's presence (whycantisleep:dread, driven by LurkerPresence).
//   Strength  how near it is: color drains and a crawling darkness closes in from the edges.
//   Approach  how fast it is coming: the view tightens and its edges split into red and blue.
//   Pulse     a heartbeat: it flushes the edges mauve for an instant.
//   Dark      it is watching: the whole view goes dark but for one light.
//   Light     where that light falls, 0 to 1 across and up the screen; beyond that when the Lurker is out of view, so
//             the light spills in from the edge on its side.
uniform sampler2D InSampler;

layout(std140) uniform DreadConfig {
    float Pulse;
    float Approach;
    float Dark;
    vec2 Light;
};

// The Lurker's palette (LurkerPalette.java): the cloud lit from within, its glow, and the bolt's edge.
const vec3 VIOLET = vec3(0.40, 0.23, 0.39);
const vec3 MAUVE = vec3(0.66, 0.39, 0.53);
const vec3 ROSE = vec3(0.80, 0.55, 0.67);

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec2 fromCenter = texCoord - 0.5;
    // 0 at the center of the view, 1 at its corners.
    float edge = length(fromCenter) / 0.7071;

    // The split grows towards the edges, so the middle of the view stays sharp.
    vec2 split = fromCenter * edge * 0.012 * Approach * Strength;
    vec3 color = vec3(texture(InSampler, texCoord + split).r, texture(InSampler, texCoord).g, texture(InSampler, texCoord - split).b);

    // Color drains away, and what is left takes the plum of a storm cloud.
    float grey = ce_luma(color);
    color = mix(color, vec3(grey) * vec3(0.95, 0.86, 0.97), 0.75 * Strength);

    // The darkness has a ragged edge that drifts, like something breathing at the rim of sight.
    float angle = atan(fromCenter.y, fromCenter.x);
    float ragged = ce_noise(vec2(angle * 3.0, Time * 0.6)) * 0.16 + ce_noise(texCoord * 9.0 + vec2(0.0, Time * 0.25)) * 0.08;
    float inner = mix(0.78, 0.3, clamp(Strength * 0.75 + Approach * 0.35, 0.0, 1.0));
    float closing = smoothstep(inner, inner + 0.55, edge + ragged * Strength);
    // Watched, the edges are left alone: the dark below is all there is.
    color *= 1.0 - closing * (0.55 + 0.4 * Strength) * Strength * (1.0 - Dark);

    // Watched: everything sinks into the dark except what the one light reaches. The light is a cold glow that
    // flickers a little, tight at its heart with a long faint reach.
    vec2 toLight = texCoord - Light;
    float reach = length(toLight);
    float flicker = 0.92 + 0.08 * ce_noise(vec2(Time * 5.0, 0.0));
    float glow = (exp(-reach * reach / 0.02) * 0.9 + exp(-reach * reach / 0.22) * 0.5) * flicker;
    vec3 lit = color * (0.07 + glow * 1.25) * mix(vec3(1.0), ROSE, 0.35) + VIOLET * glow * 0.22;
    color = mix(color, lit, Dark);

    // A beat: mauve wells up at the edges and is gone.
    color += MAUVE * 0.75 * Pulse * Strength * smoothstep(0.35, 1.05, edge);

    fragColor = vec4(color, 1.0);
}