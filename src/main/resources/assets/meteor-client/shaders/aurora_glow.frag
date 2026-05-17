#version 330 core

precision highp float;

in vec2 v_Local;
in vec4 v_Color;
out vec4 fragColor;

layout (std140) uniform AuroraGlowData {
    vec2 u_HalfSize;
    float u_Radius;
    float u_GlowRadius;
};

float sdRoundBox(vec2 p, vec2 b, float r) {
    vec2 q = abs(p) - b + r;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
}

void main() {
    float d = sdRoundBox(v_Local, u_HalfSize, u_Radius);
    float dd = max(d, 0.0);
    float falloff = exp(-(dd * dd) / max(u_GlowRadius * u_GlowRadius * 0.4, 1.0));
    fragColor = vec4(v_Color.rgb, v_Color.a * falloff);
}
