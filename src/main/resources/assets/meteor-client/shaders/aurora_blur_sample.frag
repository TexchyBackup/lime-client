#version 330 core

precision highp float;

in vec2 v_Local;
in vec2 v_ScreenUV;
in vec4 v_Color;
out vec4 fragColor;

layout (std140) uniform AuroraBlurSampleData {
    vec2 u_HalfSize;
    float u_Radius;
    float _pad;
};

uniform sampler2D u_Backdrop;

float sdRoundBox(vec2 p, vec2 b, float r) {
    vec2 q = abs(p) - b + r;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
}

void main() {
    float d = sdRoundBox(v_Local, u_HalfSize, u_Radius);
    float mask = 1.0 - smoothstep(-1.0, 1.0, d);

    vec4 blur = texture(u_Backdrop, v_ScreenUV);
    float tint = v_Color.a;
    vec3 tinted = mix(blur.rgb, vec3(0.0), tint);

    fragColor = vec4(tinted, mask);
}
