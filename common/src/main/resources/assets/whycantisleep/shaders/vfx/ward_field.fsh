#version 330

#moj_import <coolcatlib:vfx.glsl>

// The Garden's ward, struck (GardenWardClient): a field of force over the faces of the blocks round the blow. It
// flashes where the blow landed and spreads out over the faces near it as a bright front of hexagons, the lattice
// it leaves behind shimmering and fading, gone within a few blocks.
// Each face is a quad laid just over the block's face. Its UVs are where on the face's plane it is, in blocks, from
// where the blow landed (projected onto that plane); its colour carries the rest: red how far that plane is from the
// blow (in 40ths of a block), green how far through the field's life it is.
uniform sampler2D Sampler0;
uniform sampler2D DepthSampler;

in vec2 texCoord;
in vec4 vertexColor;

out vec4 fragColor;

const float REACH = 3.5;
const vec3 RED = vec3(1.00, 0.18, 0.27);
const vec3 PALE = vec3(1.00, 0.80, 0.78);

// How near the edge of its cell of a hexagonal lattice a point is: 0 at a cell's middle, 1 on its edge.
float hexEdge(vec2 p) {
    const vec2 s = vec2(1.0, 1.7320508);
    vec4 centres = floor(vec4(p, p - vec2(0.5, 1.0)) / s.xyxy) + 0.5;
    vec4 h = vec4(p - centres.xy * s, p - (centres.zw + 0.5) * s);
    vec2 g = dot(h.xy, h.xy) < dot(h.zw, h.zw) ? h.xy : h.zw;
    vec2 a = abs(g);
    return max(dot(a, s * 0.5), a.x) * 2.0;
}

void main() {
    float perp = vertexColor.r * 255.0 / 40.0;
    float life = vertexColor.g;
    vec2 q = texCoord;
    float dist = sqrt(dot(q, q) + perp * perp);
    // The front, spreading out from the blow and slowing as it goes.
    float front = (1.0 - (1.0 - life) * (1.0 - life)) * (REACH + 0.6);
    float band = exp(-pow((dist - front) / 0.32, 2.0));
    float behind = smoothstep(front + 0.15, front - 0.25, dist) * (1.0 - life);
    float lattice = smoothstep(0.78, 0.96, hexEdge(q * 3.2));
    float shimmer = 0.65 + 0.35 * sin(VfxTime.x * 9.0 + dist * 6.0);
    float reach = smoothstep(REACH, REACH * 0.35, dist);
    float flash = exp(-dist * 5.0) * (1.0 - life) * (1.0 - life);
    float light = (band * (0.35 + lattice * 0.9) + behind * (lattice * 0.7 + 0.05) * shimmer) * reach + flash;
    float alpha = clamp(light, 0.0, 1.0);
    alpha *= vfx_depth_fade(texelFetch(DepthSampler, ivec2(gl_FragCoord.xy), 0).r, gl_FragCoord.z);
    if (alpha < 0.003) discard;
    fragColor = vec4(mix(RED, PALE, clamp(band * 0.6 + flash, 0.0, 1.0)), alpha);
}
