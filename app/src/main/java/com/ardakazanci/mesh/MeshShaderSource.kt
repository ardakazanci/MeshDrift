package com.ardakazanci.mesh

internal const val MESH_SHADER_SRC = """
uniform float2 resolution;
uniform float time;
uniform float2 drag;
uniform float2 inertia;
uniform float2 focus;
uniform float focusGlow;
uniform float revealProgress;
uniform float energy;

const int NUM_OCTAVES = 3;
const float PARTICLE_COUNT = 28.0;

float rand(float2 n) {
    return fract(sin(dot(n, float2(12.9898, 4.1414))) * 43758.5453);
}

float noise(float2 p) {
    float2 ip = floor(p);
    float2 u = fract(p);
    u = u * u * (3.0 - 2.0 * u);

    float res = mix(
        mix(rand(ip), rand(ip + float2(1.0, 0.0)), u.x),
        mix(rand(ip + float2(0.0, 1.0)), rand(ip + float2(1.0, 1.0)), u.x),
        u.y
    );

    return res * res;
}

float2 rotate05(float2 p) {
    float c = cos(0.5);
    float s = sin(0.5);
    return float2(
        c * p.x - s * p.y,
        s * p.x + c * p.y
    );
}

float fbm(float2 x) {
    float v = 0.0;
    float a = 0.5;
    float2 shift = float2(100.0, 100.0);

    for (int octave = 0; octave < NUM_OCTAVES; ++octave) {
        v += a * noise(x);
        x = rotate05(x) * 2.0 + shift;
        a *= 0.5;
    }

    return v;
}

float4 tanhApprox(float4 x) {
    float4 e = exp(2.0 * x);
    return (e - 1.0) / (e + 1.0);
}

float meteorTrail(float2 p, float2 head, float2 dir, float width, float decay) {
    float2 rel = p - head;
    float along = dot(rel, dir);
    float2 offAxis = rel - dir * along;
    float across = length(offAxis);
    float headGlow = exp(-dot(rel, rel) * 18.0);
    float tail = exp(-across * width) * exp(along * decay) * step(0.0, -along);
    return headGlow + tail;
}

half4 main(float2 fragCoord) {
    float2 motion = drag * 0.72 + inertia * 0.18;
    float reveal = smoothstep(0.0, 1.0, revealProgress);
    float pulse = 0.5 + 0.5 * sin(time * (1.4 + energy * 0.8));
    float2 uv = fragCoord / resolution;
    float aspect = resolution.x / resolution.y;
    float2 focusAspect = float2((uv.x - focus.x) * aspect, uv.y - focus.y);
    float focusDistance = length(focusAspect);
    float focusMask = smoothstep(0.22, 0.0, focusDistance);

    float2 shake = float2(
        sin(time * (1.2 + motion.x * 0.14)) * (0.006 + energy * 0.008) * (1.0 - reveal * 0.35),
        cos(time * (2.1 + motion.y * 0.16)) * (0.006 + energy * 0.008) * (1.0 - reveal * 0.35)
    );

    float2 centered = ((fragCoord + (shake + motion * 0.05) * resolution) - resolution * 0.5) / resolution.y;
    float radial = length(centered);
    float swirl = atan(centered.y, centered.x) + radial * (1.2 + energy * 1.8);
    centered += float2(cos(swirl), sin(swirl)) * (0.04 + energy * 0.05) * (0.65 + pulse * 0.35);

    float2 p = float2(
        centered.x * (8.0 + motion.x * 0.9) - centered.y * (6.0 + motion.y * 0.7),
        centered.x * (6.0 + motion.x * 0.7) + centered.y * (8.0 - motion.y * 0.8)
    );
    p += float2(
        sin(radial * 14.0 - time * (1.6 + energy * 1.2)),
        cos(radial * 12.0 - time * (1.35 + energy * 1.0))
    ) * (0.06 + energy * 0.09);

    float4 o = float4(0.0);
    float f = 3.0 + fbm(p + float2(time * (5.2 + energy * 1.4), motion.y * 1.1));

    for (int idx = 1; idx <= 28; ++idx) {
        float i = float(idx);

        float2 phase = float2(i * i + (time + p.x * (0.08 + motion.x * 0.025)) * 0.022)
            + i * float2(11.0 + motion.x * 0.8, 9.0 + motion.y * 0.8);

        float2 v = p
            + cos(phase) * (4.2 + energy * 0.6)
            + float2(
                sin(time * 3.0 + i + motion.x * 0.9) * 0.0035,
                cos(time * 3.4 - i + motion.y * 0.9) * 0.0035
            );

        float arc = sin(length(v) * 2.0 - time * 1.2 + i * 0.15);
        v += normalize(v + float2(0.0001, 0.0001)) * arc * (0.035 + energy * 0.025);

        float tailNoise = fbm(v + float2(time * (0.72 + energy * 0.12), i + motion.x * 1.2))
            * (1.0 - (i / PARTICLE_COUNT));

        float denom = length(max(v, float2(v.x * f * 0.02, v.y)));

        float4 currentContribution =
            (cos(sin(i) * float4(1.0, 2.0, 3.0, 1.0)) + 1.0)
            * exp(sin(i * i + time))
            / max(denom, 0.001);

        float thinnessFactor = smoothstep(0.0, 1.0, i / PARTICLE_COUNT);
        o += currentContribution * (1.0 + tailNoise * (2.0 + energy)) * thinnessFactor;
    }

    o = tanhApprox(pow(o / 100.0, float4(1.5)));

    float3 tintA = float3(0.32, 0.68, 1.0);
    float3 tintB = float3(1.0, 0.52, 0.24);
    float3 tintC = float3(0.56, 1.0, 0.82);
    float paletteFlow = 0.5 + 0.5 * sin(time * 0.9 + motion.x * 2.4 - motion.y * 1.8);
    float3 tint = mix(mix(tintA, tintB, paletteFlow), tintC, pulse * (0.18 + energy * 0.22));
    float vignette = smoothstep(1.28, 0.18, radial);
    float3 color = o.rgb * tint * (0.62 + energy * 0.34);
    color += pow(o.rgb, float3(2.0)) * (0.14 + pulse * 0.08);
    float3 specularTint = float3(1.0, 0.985, 0.96);
    color += specularTint * focusMask * (0.02 + focusGlow * 0.04) * (1.0 - reveal * 0.86);
    color *= vignette + 0.22;
    color *= mix(1.0, 0.9, reveal);
    color = mix(color, color * float3(0.94, 0.92, 0.9), reveal * 0.12);

    return half4(color, 1.0);
}
"""
