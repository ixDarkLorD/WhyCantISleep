#version 330

#moj_import <minecraft:projection.glsl>
#moj_import <coolcatlib:vfx.glsl>

// The storm over the Lord's castle (CastleVortex), as a volume of cloud: each pixel's ray is marched through it. A
// tunnel of heaped, billowing cloud rises over the castle into a blinding light at its top, and a ceiling of storm
// cloud spreads out round the tunnel's mouth. The light lights it: white-hot near the light, through red to darkness
// away from it, each heap of cloud shadowing what is behind it. It turns slowly and climbs.
// CastleVortex draws the faces of the box the cloud fills that face the camera, all carrying the same:
//   UVs: where the castle's roof (the volume's origin) is from the camera, across (x and z)
//   colour: one 32-bit word, its bytes alpha, red, green, blue from the top: bits 0-13 the roof's height from the
//   camera (value / 8 - 1024 blocks); 14-18 how far round the castle the latest lightning strike left the cloud; 19-22
//   where (0-7 up the tunnel's wall, 8-15 out across the ceiling); 23-26 how bright it still is; 27-31 how much of the
//   storm shows (it fades out before the castle is too far off to be drawn)
uniform sampler2D Sampler0;
uniform sampler2D DepthSampler;

in vec2 texCoord;
in vec4 vertexColor;

out vec4 fragColor;

// The tunnel (CastleVortex has the same numbers): its foot and the light at its top, over the roof, and how wide it
// is at each.
const float FOOT = 22.0;
const float TOP = 120.0;
const float NARROW = 16.0;
const float WIDE = 64.0;
// The box the cloud fills, round the roof.
const vec3 BOX_MIN = vec3(-200.0, 14.0, -200.0);
const vec3 BOX_MAX = vec3(200.0, 146.0, 200.0);
// How far a step goes through cloud (growing a little with distance, where a pixel covers more), and through where
// there can be none; and how many steps at most. Steps much longer than this make the cloud swim as the camera
// moves (tried for speed, 2026-10-15, the author found it wobbly); the saving is in the shadows and empty space.
const float STEP = 0.65;
const float SKIP = 3.0;
const int MAX_STEPS = 360;

const vec3 NIGHT = vec3(0.030, 0.022, 0.050);
const vec3 DUSK = vec3(0.16, 0.035, 0.09);
const vec3 MAROON = vec3(0.42, 0.05, 0.12);
const vec3 RED = vec3(0.95, 0.20, 0.27);
const vec3 HOT = vec3(1.00, 0.78, 0.76);
const vec3 WHITE = vec3(1.00, 0.97, 0.95);

float hash(vec3 p) {
    p = fract(p * 0.3183099 + 0.1);
    p *= 17.0;
    return fract(p.x * p.y * p.z * (p.x + p.y + p.z));
}

float noise(vec3 x) {
    vec3 i = floor(x);
    vec3 f = fract(x);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(mix(hash(i), hash(i + vec3(1, 0, 0)), f.x), mix(hash(i + vec3(0, 1, 0)), hash(i + vec3(1, 1, 0)), f.x), f.y),
               mix(mix(hash(i + vec3(0, 0, 1)), hash(i + vec3(1, 0, 1)), f.x), mix(hash(i + vec3(0, 1, 1)), hash(i + vec3(1, 1, 1)), f.x), f.y), f.z);
}

float fbm(vec3 p, int octaves) {
    float sum = 0.0;
    float amp = 0.5;
    float norm = 0.0;
    for (int i = 0; i < octaves; i++) {
        sum += noise(p) * amp;
        norm += amp;
        p = p * 2.03 + vec3(13.7, 5.1, 9.3);
        amp *= 0.5;
    }
    return sum / norm;
}

mat2 turn(float a) {
    float c = cos(a);
    float s = sin(a);
    return mat2(c, -s, s, c);
}

// Where there may be cloud, 0 to 1: the tunnel's wall, a thin layer thickening a little as it rises, and the
// ceiling round its mouth, a thin sheet.
float shape(vec3 p) {
    float r = length(p.xz);
    float up = clamp((p.y - FOOT) / (TOP - FOOT), 0.0, 1.0);
    float radius = NARROW + (WIDE - NARROW) * pow(up, 1.6);
    float thick = 3.0 + 6.0 * up;
    float wall = 1.0 - smoothstep(0.0, thick, abs(r - radius - thick * 0.35));
    // Its foot fades away gently, thinning over a long way, rather than ending at a line.
    wall *= smoothstep(FOOT - 4.0, FOOT + 34.0, p.y) * (1.0 - smoothstep(TOP, TOP + 10.0, p.y));
    float ceiling = smoothstep(TOP - 7.0, TOP - 2.0, p.y) * (1.0 - smoothstep(TOP + 4.0, TOP + 11.0, p.y))
            * smoothstep(WIDE * 0.75, WIDE * 1.1, r) * (1.0 - smoothstep(110.0, 195.0, r));
    return max(wall, ceiling);
}

// How far a ray can go from a point outside the cloud's shape before it could reach it: the gap to the tunnel's wall
// (its radius changes with height, so a step closes that gap at most 1.8 times as fast) or past its ends, and the gap
// to the ceiling's sheet. Empty space is crossed in a few long jumps, not hundreds of short steps.
float clearance(vec3 p) {
    float r = length(p.xz);
    float up = clamp((p.y - FOOT) / (TOP - FOOT), 0.0, 1.0);
    float radius = NARROW + (WIDE - NARROW) * pow(up, 1.6);
    float thick = 3.0 + 6.0 * up;
    float wall = max((abs(r - radius - thick * 0.35) - thick) / 1.8, max(FOOT - 4.0 - p.y, p.y - TOP - 10.0));
    float ceiling = max(max(TOP - 7.0 - p.y, p.y - TOP - 11.0), max(WIDE * 0.75 - r, r - 195.0));
    return min(wall, ceiling);
}

// How thick the cloud is at a point. It turns as one (a spiral twist, fixed, winds it into arms) and climbs. Big
// heaps from a warped base, eaten into at their edges by finer lumps and finer wisps still, so they billow like
// cauliflower. `detail` (0 to 1) is how much of the fine work to do: none far off, where it would only flicker; and
// far off its edges are softer too, so they don't break up into speckles between pixels.
float density(vec3 p, float t, float detail) {
    float s = shape(p);
    if (s <= 0.02) return 0.0;
    float r = length(p.xz);
    vec2 q = turn(t * 0.05 + log(r + 8.0) * 1.3 + p.y * 0.02) * p.xz;
    vec3 at = vec3(q.x, p.y - t * 0.8, q.y) * 0.055;
    float base = fbm(at + vec3(0.0, 0.0, fbm(at * 0.45, 2) * 1.6), detail > 0.05 ? 3 : 2);
    // Most points in the shape are clear air between heaps: if even the most the swirl (+0.21) and the fine work
    // (+0.16) could add leaves it empty, it is, and the rest needn't be worked out. (Exact: nothing shows different.)
    if (base + s * 0.62 - 0.88 + 0.21 + 0.16 * detail <= 0.0) return 0.0;
    // The swirl: bands of cloud wound round the tunnel in a helix, turning and climbing from its foot to its top.
    // Taken on a circle round the axis (cos and sin of the angle), so it has no seam.
    float phi = atan(p.z, p.x) + p.y * 0.05 - t * 0.35;
    float bands = fbm(vec3(cos(phi) * 1.7, sin(phi) * 1.7, p.y * 0.09 - t * 0.3 + r * 0.04), 3);
    float up = clamp((p.y - FOOT) / (TOP - FOOT), 0.0, 1.0);
    float cloud = base + s * 0.62 - 0.88 + (bands - 0.5) * 0.42 * (1.0 - up * 0.5);
    if (detail > 0.0 && cloud > -0.15) {
        cloud -= ((1.0 - fbm(at * 3.7 + vec3(0.0, t * 0.04, 0.0), 3)) * 0.24 - 0.11) * detail;
        if (detail > 0.5) cloud -= ((1.0 - noise(at * 11.0 + vec3(t * 0.06))) * 0.1 - 0.05) * (detail * 2.0 - 1.0);
    }
    return clamp(cloud * mix(3.0, 5.5, detail), 0.0, 1.0) * smoothstep(FOOT - 2.0, FOOT + 26.0, p.y);
}

// The cloud's heaps (the warped base, no swirl or fine work), for the shadow it casts close by: each heap shades
// the one behind it.
float heaps(vec3 p, float t) {
    float s = shape(p);
    if (s <= 0.02) return 0.0;
    float r = length(p.xz);
    vec2 q = turn(t * 0.05 + log(r + 8.0) * 1.3 + p.y * 0.02) * p.xz;
    vec3 at = vec3(q.x, p.y - t * 0.8, q.y) * 0.055;
    float base = fbm(at + vec3(0.0, 0.0, fbm(at * 0.45, 2) * 1.6), 3);
    return clamp((base + s * 0.62 - 0.88) * 4.0, 0.0, 1.0) * smoothstep(FOOT - 2.0, FOOT + 26.0, p.y);
}

// The cloud's big heaps only, for the shadows it casts further off: cheap, and shadows are soft anyway.
float coarse(vec3 p, float t) {
    float s = shape(p);
    if (s <= 0.02) return 0.0;
    float r = length(p.xz);
    vec2 q = turn(t * 0.05 + log(r + 8.0) * 1.3 + p.y * 0.02) * p.xz;
    vec3 at = vec3(q.x, p.y - t * 0.8, q.y) * 0.055;
    return clamp((noise(at) * 0.65 + noise(at * 2.03 + vec3(13.7, 5.1, 9.3)) * 0.35 + s * 0.62 - 0.88) * 3.0, 0.0, 1.0)
            * smoothstep(FOOT - 2.0, FOOT + 26.0, p.y);
}

// Cloud lit this much: night in shadow, through crimson and red, to white-hot near the light.
vec3 litCloud(float light) {
    vec3 c = mix(NIGHT, DUSK, smoothstep(0.0, 0.25, light));
    c = mix(c, MAROON, smoothstep(0.2, 0.55, light));
    c = mix(c, RED, smoothstep(0.5, 0.95, light));
    c = mix(c, HOT, smoothstep(0.95, 1.4, light));
    return mix(c, WHITE, smoothstep(1.4, 2.0, light));
}

void main() {
    float t = VfxTime.x;
    // Where the camera is from the roof, and which way this pixel looks.
    uvec4 bytes = uvec4(floor(vertexColor * 255.0 + 0.5));
    uint word = bytes.a << 24u | bytes.r << 16u | bytes.g << 8u | bytes.b;
    vec3 origin = -vec3(texCoord.x, float(word & 16383u) / 8.0 - 1024.0, texCoord.y);
    float shown = float(word >> 27u) / 31.0;
    vec2 screen = vec2(textureSize(DepthSampler, 0));
    vec2 ndc = gl_FragCoord.xy / screen * 2.0 - 1.0;
    vec4 far = inverse(ProjMat) * vec4(ndc, 1.0, 1.0);
    vec3 viewDir = normalize(far.xyz / far.w);
    vec3 dir = transpose(mat3(VfxViewMat)) * viewDir;
    // Through the box, and no further than whatever solid is in the way.
    vec3 inv = 1.0 / dir;
    vec3 t0 = (BOX_MIN - origin) * inv;
    vec3 t1 = (BOX_MAX - origin) * inv;
    vec3 lo = min(t0, t1);
    vec3 hi = max(t0, t1);
    float near = max(max(max(lo.x, lo.y), lo.z), 0.0);
    float farT = min(min(hi.x, hi.y), hi.z);
    float scene = vfx_linear_depth(texelFetch(DepthSampler, ivec2(gl_FragCoord.xy), 0).r) / max(-viewDir.z, 0.0001);
    farT = min(farT, scene);
    if (farT <= near) discard;

    // The latest strike: where it left the cloud, and how bright it still is.
    float strikeAround = float((word >> 14u) & 31u) / 32.0 * 6.2831853;
    int place = int((word >> 19u) & 15u);
    float lightning = float((word >> 23u) & 15u) / 15.0;
    vec3 strike;
    if (place < 8) {
        float up = float(place) / 7.0;
        float y = FOOT + (TOP - FOOT) * up;
        float radius = NARROW + (WIDE - NARROW) * pow(up, 1.6);
        strike = vec3(cos(strikeAround) * radius, y, sin(strikeAround) * radius);
    } else {
        float outward = 60.0 + float(place - 8) / 7.0 * 140.0;
        strike = vec3(cos(strikeAround) * outward, TOP - 1.0, sin(strikeAround) * outward);
    }
    vec3 core = vec3(0.0, TOP + 4.0, 0.0);
    // Small steps through cloud, long ones through where none can be. The start is offset a little per pixel so the
    // steps don't show as bands.
    float jitter = fract(52.9829189 * fract(dot(gl_FragCoord.xy, vec2(0.06711056, 0.00583715))));
    // A small per-pixel offset (half a block at most) hides the steps without showing as grain.
    float at = near + jitter * STEP;
    float through = 1.0;
    // The shadow at the last cloud looked at: light changes slowly along a ray, so it is worked out at every other
    // sample of cloud and kept for the one between.
    float shade = 0.0;
    int lit = 0;
    vec3 color = vec3(0.0);
    for (int i = 0; i < MAX_STEPS && at < farT; i++) {
        vec3 p = origin + dir * at;
        if (shape(p) <= 0.02) {
            at += max(SKIP, clearance(p));
            continue;
        }
        float stride = STEP * (1.0 + at / 400.0);
        float step = min(stride, farT - at);
        at += stride;
        // The fine work fades out with distance, so far off the cloud is smooth rather than speckled.
        float detail = clamp(1.0 - (at - 80.0) / 260.0, 0.0, 1.0);
        float d = density(p, t, detail);
        if (d <= 0.0) continue;
        // Lit by the light at the top, shadowed by the cloud between: three looks towards the light, the nearest at the
        // cloud's heaps (so each heap shades the one behind it), the two further only at the biggest.
        vec3 toCore = core - p;
        float dist = length(toCore);
        vec3 towards = toCore / dist;
        if ((lit++ & 1) == 0) shade = heaps(p + towards * 1.5, t) * 0.8 + coarse(p + towards * 4.5, t) + coarse(p + towards * 11.0, t);
        float direct = exp(-shade * 1.6);
        // Highlights and shadows: the thick heart of a heap is darker than its thin edge (powder), and cloud seen
        // against the light glows at its rims where the light comes through it.
        float powder = 1.0 - exp(-d * 2.5);
        float rim = pow(max(dot(dir, towards), 0.0), 5.0) * (1.0 - d * 0.6);
        float energy = exp(-dist / 38.0) * 2.4 + exp(-dist / 110.0) * 0.45;
        float light = energy * direct * mix(1.0, powder, 0.55) * (1.0 + 1.8 * rim);
        // The beam up the tunnel's middle lights its inside a little; the shadowed side keeps a faint red glow, from
        // the light bouncing round inside the storm.
        light += exp(-length(p.xz) / 14.0) * 0.35 * direct + (0.05 + 0.08 * (1.0 - d)) * exp(-shade * 0.4);
        // The strike lights the cloud round it, brightest close in, falling away over some forty blocks.
        float fromStrike = length(p - strike);
        float flash = lightning * (exp(-fromStrike / 15.0) * 2.0 + exp(-fromStrike / 42.0) * 0.75) * (0.4 + 0.6 * exp(-shade));
        vec3 c = litCloud(light + flash * 0.6) + mix(RED, HOT, clamp(flash - 0.6, 0.0, 1.0)) * flash * 0.5;
        float a = 1.0 - exp(-d * step * 0.75);
        color += through * a * c;
        through *= 1.0 - a;
        if (through < 0.03) break;
    }
    float covered = 1.0 - through;
    float alpha = covered * shown;
    if (alpha < 0.003) discard;
    fragColor = vec4(min(color / covered, vec3(1.0)), alpha);
}
