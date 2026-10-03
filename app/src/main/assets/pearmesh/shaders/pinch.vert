#version 300 es

layout(location = 0) in vec2 aFromPosition;
layout(location = 1) in vec2 aToPosition;
layout(location = 2) in vec2 aTexCoord;

uniform float uTime;
uniform vec4 uTextureTransform;

out vec2 vTexCoord;
out vec2 vOrdinaryTexCoord;

void main() {
    const float PI = 3.14159265358979323846;
    float phase = acos(sin(uTime * PI / 5.0)) / PI;
    float mixValue = phase * phase * (3.0 - 2.0 * phase);
    vec2 warpedPosition = mix(aFromPosition, aToPosition, mixValue);

    gl_Position = vec4(warpedPosition, 0.0, 1.0);
    vTexCoord = aTexCoord * uTextureTransform.xy + uTextureTransform.zw;
    vOrdinaryTexCoord = warpedPosition * 0.5 + 0.5;
}
