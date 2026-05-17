#version 330 core

precision highp float;

in vec2 v_Local;
in vec4 v_Color;
out vec4 fragColor;

layout (std140) uniform AuroraGradientData {
    vec2 u_HalfSize;
    vec2 u_Direction;
    vec4 u_ColorA;
    vec4 u_ColorB;
    float u_Radius;
    float u_Mode; // 0 = linear, 1 = radial
};

float sdRoundBox(vec2 p, vec2 b, float r) {
    vec2 q = abs(p) - b + r;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
}

void main() {
    float t;
    if (u_Mode < 0.5) {
        // Linear: project local pos onto direction
        float proj = dot(v_Local, u_Direction);
        // Half extent along direction
        float extent = abs(u_Direction.x) * u_HalfSize.x + abs(u_Direction.y) * u_HalfSize.y;
        t = clamp(proj / max(extent * 2.0, 0.0001) + 0.5, 0.0, 1.0);
    } else {
        // Radial: distance from center / max radius
        float maxR = length(u_HalfSize);
        t = clamp(length(v_Local) / max(maxR, 0.0001), 0.0, 1.0);
    }

    vec4 col = mix(u_ColorA, u_ColorB, t);

    // Optional rounded mask
    float d = sdRoundBox(v_Local, u_HalfSize, u_Radius);
    float fillAlpha = 1.0 - smoothstep(-1.0, 1.0, d);

    fragColor = vec4(col.rgb, col.a * fillAlpha);
}
