#version 330

#moj_import <coolcatlib:screen_effect.glsl>

// The heart portal's pull (whycantisleep:heart_pull, driven by HeartPortalClient). Strength is how near an awake
// portal the player is, 0 far off to 1 at its mouth, and runs up to 1 and fades again as it throws them out:
//   - the picture is drawn in toward the middle and turns a little, as if the rift were breathing it in;
//   - its colours split outward from the middle;
//   - a crimson vignette closes in from the edges, veined, pulsing with a slow heartbeat;
//   - near the full strength, a red-white glow swells at the middle.
uniform sampler2D InSampler;

in vec2 texCoord;

out vec4 fragColor;

float fbm(vec2 p) {
    float sum = 0.0;
    float amp = 0.5;
    for (int i = 0; i < 4; i++) {
        sum += ce_noise(p) * amp;
        p = p * 2.03 + vec2(5.3, 11.7);
        amp *= 0.5;
    }
    return sum;
}

void main() {
    float s = clamp(Strength, 0.0, 1.0);
    vec2 size = vec2(textureSize(InSampler, 0));
    float aspect = size.x / size.y;
    vec2 c = (texCoord - 0.5) * vec2(aspect, 1.0) * 2.0;
    float r = length(c);
    // A heartbeat: two quick beats and a rest, about once every two seconds.
    float t = fract(Time * 0.5);
    float beat = exp(-pow((t - 0.08) * 22.0, 2.0)) + 0.7 * exp(-pow((t - 0.24) * 22.0, 2.0));

    // Drawn in: the picture is pulled toward the middle, more at the edges, breathing with the beat, and turns.
    float pull = s * (0.10 + 0.05 * beat) * smoothstep(0.0, 1.5, r);
    float turn = s * s * 0.18 * sin(Time * 0.9) * smoothstep(0.2, 1.4, r);
    vec2 w = c * (1.0 - pull);
    w = vec2(w.x * cos(turn) - w.y * sin(turn), w.x * sin(turn) + w.y * cos(turn));
    vec2 uv = w / (vec2(aspect, 1.0) * 2.0) + 0.5;
    // Colours split outward from the middle.
    vec2 split = normalize(c + 1e-5) * 0.008 * s * (0.5 + r);
    vec3 color = vec3(texture(InSampler, uv + split).r, texture(InSampler, uv).g, texture(InSampler, uv - split).b);

    // The vignette: crimson, ragged, veined, its edge breathing in with the beat.
    float v = fbm(c * 2.2 + vec2(Time * 0.1, -Time * 0.14));
    float reach = 1.55 - s * (0.55 + 0.12 * beat);
    float vignette = smoothstep(reach - 0.35, reach + 0.25, r + (v - 0.5) * 0.35);
    float veins = smoothstep(0.035, 0.0, abs(fbm(c * 5.0 + vec2(0.0, Time * 0.05)) - 0.5)) * vignette;
    vec3 blood = mix(vec3(0.16, 0.0, 0.02), vec3(0.55, 0.02, 0.08), v);
    color = mix(color, blood, vignette * (0.55 + 0.25 * s));
    color += vec3(0.9, 0.08, 0.15) * veins * s * (0.6 + 0.4 * beat);

    // At the height of it, a red-white glow at the middle.
    float glow = smoothstep(0.7, 1.0, s) * exp(-r * r * 2.5);
    color = mix(color, vec3(1.0, 0.75, 0.75), glow * 0.65);
    fragColor = vec4(color, 1.0);
}
