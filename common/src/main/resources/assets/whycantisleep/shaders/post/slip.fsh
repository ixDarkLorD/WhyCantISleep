#version 330

#moj_import <coolcatlib:screen_effect.glsl>

// Slipping under into the Dark Between (whycantisleep:slip, driven by DreamTransition), as the view rises away from the
// sleeper. Strength is how far under they are, 0 to 1:
//   - the picture warps, slowly turning and rippling, its colors draining and splitting;
//   - black smoke rolls in from every edge;
//   - black tentacles reach in from the edges towards the middle, writhing, each with a faint red rim;
// and at 1 the screen is wholly black, where the black of the crossing takes over.
uniform sampler2D InSampler;

in vec2 texCoord;

out vec4 fragColor;

const float TAU = 6.2831853;
const int TENTACLES = 11;

float fbm(vec2 p) {
    float sum = 0.0;
    float amp = 0.5;
    for (int i = 0; i < 4; i++) {
        sum += ce_noise(p) * amp;
        p = p * 2.07 + vec2(13.1, 7.7);
        amp *= 0.5;
    }
    return sum;
}

void main() {
    float under = clamp(Strength, 0.0, 1.0);
    vec2 size = vec2(textureSize(InSampler, 0));
    float aspect = size.x / size.y;
    // From the middle of the screen, in a square measure: 0 at the middle, about 1 at the edges.
    vec2 c = (texCoord - 0.5) * vec2(aspect, 1.0) * 2.0;
    float r = length(c);

    // The warp: the picture turns about the middle, more towards the edges, and ripples.
    float turn = under * under * 0.55 * smoothstep(0.0, 1.4, r) * sin(Time * 0.7 + 1.0);
    float s = sin(turn);
    float co = cos(turn);
    vec2 warped = vec2(c.x * co - c.y * s, c.x * s + c.y * co);
    warped += vec2(sin(c.y * 9.0 + Time * 3.0), cos(c.x * 8.0 + Time * 2.6)) * 0.012 * under;
    vec2 uv = warped / (vec2(aspect, 1.0) * 2.0) + 0.5;
    float split = 0.006 * under;
    vec3 color = vec3(texture(InSampler, uv + vec2(split, 0.0)).r, texture(InSampler, uv).g, texture(InSampler, uv - vec2(split, 0.0)).b);
    color = mix(color, vec3(ce_luma(color)) * vec3(0.9, 0.85, 0.95), under * 0.7);
    color *= 1.0 - under * 0.45;

    // The smoke: a front that rolls in from the edges, ragged and drifting, swallowing the screen as `under` grows.
    float edge = min(min(texCoord.x, 1.0 - texCoord.x), min(texCoord.y, 1.0 - texCoord.y));
    float puff = fbm(texCoord * vec2(aspect, 1.0) * 3.5 + vec2(Time * 0.15, -Time * 0.22));
    float front = under * 0.62;
    float smoke = smoothstep(front + 0.07, front - 0.05, edge + (puff - 0.5) * 0.22);
    vec3 smokeColor = mix(vec3(0.0), vec3(0.06, 0.045, 0.07), puff * (1.0 - smoke * 0.6));

    // The tentacles: each reaches in from the edge along its own heading, writhing, thick at the root and tapering
    // to a point, the tip creeping inwards as `under` grows.
    float tentacle = 0.0;
    float rim = 0.0;
    for (int i = 0; i < TENTACLES; i++) {
        float fi = float(i);
        float heading = fi / float(TENTACLES) * TAU + ce_hash(vec2(fi, 3.0)) * 0.4;
        float reach = mix(0.55, 1.0, ce_hash(vec2(fi, 9.0)));
        float tip = 1.6 - under * 1.55 * reach;
        if (r < tip) continue;
        // How far along it this point is, from the tip (0) back to the edge (1).
        float along = clamp((r - tip) / max(1.7 - tip, 0.01), 0.0, 1.0);
        float wriggle = sin(r * 7.0 - Time * 2.4 + fi * 2.3) * 0.16 * (1.0 - along * 0.6);
        vec2 dir = vec2(cos(heading + wriggle), sin(heading + wriggle));
        // Distance from its middle line, across.
        float across = abs(c.x * dir.y - c.y * dir.x);
        float facing = dot(c, dir);
        float thick = mix(0.012, 0.16, pow(along, 0.8));
        if (facing > 0.0) {
            float body = smoothstep(thick, thick * 0.6, across);
            tentacle = max(tentacle, body);
            rim = max(rim, smoothstep(thick * 1.25, thick, across) * (1.0 - body));
        }
    }

    float black = max(smoke, tentacle);
    color = mix(color, smokeColor, smoke);
    color = mix(color, vec3(0.0), tentacle);
    color += vec3(0.5, 0.03, 0.08) * rim * under * (1.0 - smoke);
    // The last of it goes all at once to black, for the crossing's own black to take over.
    color *= 1.0 - smoothstep(0.88, 1.0, under);
    fragColor = vec4(color, 1.0);
}
