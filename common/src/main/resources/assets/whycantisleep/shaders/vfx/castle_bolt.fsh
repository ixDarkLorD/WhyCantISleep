#version 330

#moj_import <coolcatlib:vfx.glsl>

// One segment of a lightning strike out of the storm over the Lord's castle (CastleVortex): a smooth bright line,
// even along its length so segments join up, soft to its sides. v runs across it.
uniform sampler2D Sampler0;
uniform sampler2D DepthSampler;

in vec2 texCoord;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    float across = abs(texCoord.y - 0.5) * 2.0;
    float core = smoothstep(0.5, 0.0, across);
    float glow = exp(-across * 3.5) * 0.6;
    float alpha = clamp(core + glow, 0.0, 1.0) * vertexColor.a;
    alpha *= vfx_depth_fade(texelFetch(DepthSampler, ivec2(gl_FragCoord.xy), 0).r, gl_FragCoord.z);
    if (alpha < 0.003) discard;
    fragColor = vec4(mix(vertexColor.rgb, vec3(1.0), core * 0.5), alpha);
}
