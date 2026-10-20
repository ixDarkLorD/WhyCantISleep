#version 330

#moj_import <coolcatlib:vfx.glsl>

// The Garden's ward, struck (GardenWardClient): the garden's veins wake under the faces round the blow. From where it
// landed a pulse of blood-light runs out through a web of branching veins (the weeping veins' own red), thicker
// trunks and finer branches off them, thorns along the trunks; a bright front races ahead, the veins behind it glow
// and beat once, then sink back into the stone, gone within a few blocks.
// Each face is a quad laid just over the block's face. Its UVs are where on the face's plane it is, in blocks, from
// where the blow landed (projected onto that plane); its colour carries the rest: red how far that plane is from the
// blow (in 40ths of a block), green how far through the field's life it is.
uniform sampler2D Sampler0;
uniform sampler2D DepthSampler;

in vec2 texCoord;
in vec4 vertexColor;

out vec4 fragColor;

const float REACH = 3.5;
const vec3 CRIMSON = vec3(0.54, 0.04, 0.14);
const vec3 RED = vec3(1.00, 0.18, 0.27);
const vec3 PALE = vec3(1.00, 0.78, 0.74);

vec2 hash2(vec2 p) {
    p = vec2(dot(p, vec2(127.1, 311.7)), dot(p, vec2(269.5, 183.3)));
    return fract(sin(p) * 43758.5453);
}

// A web of veins: the edges between cells of scattered points (how near the second-nearest point is to the nearest),
// so they branch and meet like roots, not a grid. 0 on a vein, larger away from it; and which cell (for thorns).
float veins(vec2 p, out vec2 cell) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    float d1 = 8.0, d2 = 8.0;
    for (int y = -1; y <= 1; y++) {
        for (int x = -1; x <= 1; x++) {
            vec2 g = vec2(float(x), float(y));
            vec2 o = hash2(i + g);
            float d = length(g + o - f);
            if (d < d1) {
                d2 = d1;
                d1 = d;
                cell = i + g;
            } else if (d < d2) {
                d2 = d;
            }
        }
    }
    return d2 - d1;
}

void main() {
    float perp = vertexColor.r * 255.0 / 40.0;
    float life = vertexColor.g;
    vec2 q = texCoord;
    float dist = sqrt(dot(q, q) + perp * perp);
    // A little warp, so the veins wander.
    vec2 w = q + 0.12 * vec2(sin(q.y * 3.1), cos(q.x * 2.7));

    vec2 cell;
    float trunk = veins(w * 1.6, cell);
    vec2 twig;
    float branch = veins(w * 4.2 + 7.3, twig);
    // Thick trunks with thorns jutting off them every so often, fine branches between.
    float thick = 0.07 + 0.03 * sin(dist * 9.0);
    float trunks = smoothstep(thick, thick * 0.3, trunk);
    float thorn = smoothstep(0.16, 0.0, trunk) * smoothstep(0.86, 0.96, fract(dot(w, normalize(hash2(cell) - 0.5)) * 2.2));
    float branches = smoothstep(0.05, 0.015, branch) * 0.55;
    float web = max(max(trunks, thorn * 0.8), branches);

    // The pulse: a bright front running out and slowing, the veins behind it lit, beating once as it passes.
    float front = (1.0 - (1.0 - life) * (1.0 - life)) * (REACH + 0.6);
    float band = exp(-pow((dist - front) / 0.28, 2.0));
    float behind = smoothstep(front + 0.1, front - 0.3, dist) * (1.0 - life);
    float beat = 0.75 + 0.25 * sin(VfxTime.x * 11.0 - dist * 5.0);
    float reach = smoothstep(REACH, REACH * 0.3, dist);
    float flash = exp(-dist * 5.0) * (1.0 - life) * (1.0 - life);
    // A faint flush of red under the stone where the veins run, so they read as inside it.
    float flush = behind * 0.08 * (1.0 - smoothstep(0.0, 0.6, trunk));

    float light = (web * (band * 1.3 + behind * 0.75 * beat) + band * 0.12 + flush) * reach + flash * 0.8;
    float alpha = clamp(light, 0.0, 1.0);
    alpha *= vfx_depth_fade(texelFetch(DepthSampler, ivec2(gl_FragCoord.xy), 0).r, gl_FragCoord.z);
    if (alpha < 0.003) discard;
    vec3 color = mix(CRIMSON, RED, clamp(web * 0.7 + band, 0.0, 1.0));
    color = mix(color, PALE, clamp(band * web * 0.8 + flash, 0.0, 1.0));
    fragColor = vec4(color, alpha);
}
