#version 330 core

precision highp float;

in vec2 v_Local;
in vec4 v_Color;
out vec4 fragColor;

layout (std140) uniform AuroraRectData {
    vec2 u_HalfSize;
    float u_Radius;
    float u_BorderWidth;
    vec4 u_BorderColor;
};

float sdRoundBox(vec2 p, vec2 b, float r) {
    vec2 q = abs(p) - b + r;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
}

void main() {
    float d = sdRoundBox(v_Local, u_HalfSize, u_Radius);

    float fillAlpha = 1.0 - smoothstep(-1.0, 1.0, d);
    vec4 col = vec4(v_Color.rgb, v_Color.a * fillAlpha);

    if (u_BorderWidth > 0.0) {
        float bw = u_BorderWidth * 0.5;
        float borderAlpha = 1.0 - smoothstep(bw - 1.0, bw + 1.0, abs(d));
        vec4 borderCol = vec4(u_BorderColor.rgb, u_BorderColor.a * borderAlpha);
        col.rgb = mix(col.rgb, borderCol.rgb, borderAlpha);
        col.a = max(col.a, borderCol.a);
    }

    fragColor = col;
}
