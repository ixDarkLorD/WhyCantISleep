#version 330

#moj_import <coolcatlib:vfx.glsl>

// The beam from the Lord's castle into the storm (CastleVortex): bright down its middle, soft to its sides, with
// light flowing up it in surges. u runs along it (0 at the castle), v across it.
uniform sampler2D Sampler0;
uniform sampler2D DepthSampler;

in vec2 texCoord;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    float t = VfxTime.x;
    float across = abs(texCoord.y - 0.5) * 2.0;
    float along = texCoord.x;
    float core = exp(-across * across * 18.0);
    float soft = exp(-across * 3.0) * 0.35;
    float surge = 0.7 + 0.3 * sin(along * 38.0 - t * 9.0) + 0.15 * sin(along * 91.0 - t * 17.0);
    // A little ripple sideways, so it isn't a ruled line.
    float ripple = 1.0 + 0.12 * sin(along * 60.0 + t * 5.0) * across;
    float light = (core * surge + soft) * ripple;
    // It fades in off the roof and out into the eye.
    light *= smoothstep(0.0, 0.02, along) * (1.0 - smoothstep(0.9, 1.0, along) * 0.6);
    float alpha = clamp(light, 0.0, 1.0) * vertexColor.a;
    alpha *= vfx_depth_fade(texelFetch(DepthSampler, ivec2(gl_FragCoord.xy), 0).r, gl_FragCoord.z);
    if (alpha < 0.003) discard;
    fragColor = vec4(mix(vertexColor.rgb, vec3(1.0), core * 0.4), alpha);
}
