#version 330

#moj_import <coolcatcanvas:screen_effect.glsl>

// The Lurker's presence (whycantisleep:dread, driven by LurkerPresence).
//   Strength  how near it is: color drains and a crawling darkness closes in from the edges.
//   Approach  how fast it is coming: the view tightens and its edges split into red and blue.
//   Pulse     a heartbeat: it flushes the edges red for an instant.
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

// The Lurker's palette (LurkerPalette.java): cloud lit from below, and the fire itself.
const vec3 CRIMSON = vec3(0.54, 0.04, 0.14);
const vec3 RED = vec3(1.00, 0.18, 0.27);

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec2 fromCenter = texCoord - 0.5;
    // 0 at the center of the view, 1 at its corners.
    float edge = length(fromCenter) / 0.7071;

    // The split grows towards the edges, so the middle of the view stays sharp.
    vec2 split = fromCenter * edge * 0.012 * Approach * Strength;
    vec3 color = vec3(texture(InSampler, texCoord + split).r, texture(InSampler, texCoord).g, texture(InSampler, texCoord - split).b);

    // Color drains away, and what is left takes the navy of the sky after sunset. All but red: the Lurker's own
    // eyes and embers keep their color, and so are the one bright thing left in the view.
    float grey = ce_luma(color);
    float redness = clamp((color.r - max(color.g, color.b)) * 3.0, 0.0, 1.0);
    color = mix(color, vec3(grey) * vec3(0.84, 0.90, 1.0), 0.75 * Strength * (1.0 - redness));

    // The darkness has a ragged edge that drifts, like something breathing at the rim of sight.
    // The raggedness goes round the view by direction, not by angle: an angle jumps from one end of its range to the
    // other straight left of the center, which drew a seam across the darkness there.
    vec2 around = fromCenter / max(length(fromCenter), 0.0001);
    float ragged = ce_noise(around * 2.4 + vec2(Time * 0.6, Time * 0.35)) * 0.16 + ce_noise(texCoord * 9.0 + vec2(0.0, Time * 0.25)) * 0.08;
    float inner = mix(0.78, 0.3, clamp(Strength * 0.75 + Approach * 0.35, 0.0, 1.0));
    float closing = smoothstep(inner, inner + 0.55, edge + ragged * Strength);
    // Watched, the edges are left alone: the dark below is all there is.
    color *= 1.0 - closing * (0.55 + 0.4 * Strength) * Strength * (1.0 - Dark);

    // Watched: everything sinks into the dark except what the one light reaches. The light is a red glow that
    // flickers a little, tight at its heart with a long faint reach.
    vec2 toLight = texCoord - Light;
    float reach = length(toLight);
    float flicker = 0.92 + 0.08 * ce_noise(vec2(Time * 5.0, 0.0));
    float glow = (exp(-reach * reach / 0.02) * 0.9 + exp(-reach * reach / 0.22) * 0.5) * flicker;
    vec3 lit = color * (0.07 + glow * 1.25) * mix(vec3(1.0), RED, 0.4) + CRIMSON * glow * 0.3;
    color = mix(color, lit, Dark);

    // A beat: red wells up at the edges and is gone.
    color += mix(CRIMSON, RED, 0.5) * 0.7 * Pulse * Strength * smoothstep(0.35, 1.05, edge);

    fragColor = vec4(color, 1.0);
}