#version 300 es

precision highp float;

uniform sampler2D uSource;
uniform vec2 uTexelSize;
uniform float uOffset;
uniform int uUpsample;

in vec2 vTexCoord;
layout(location = 0) out vec4 outColor;

void main() {
    vec2 offset = uTexelSize * uOffset;
    vec3 color;

    if (uUpsample == 0) {
        color = texture(uSource, vTexCoord).rgb * 4.0;
        color += texture(uSource, vTexCoord + vec2(-offset.x, -offset.y)).rgb;
        color += texture(uSource, vTexCoord + vec2( offset.x, -offset.y)).rgb;
        color += texture(uSource, vTexCoord + vec2(-offset.x,  offset.y)).rgb;
        color += texture(uSource, vTexCoord + vec2( offset.x,  offset.y)).rgb;
        color *= 0.125;
    } else {
        color = texture(uSource, vTexCoord + vec2(-2.0 * offset.x, 0.0)).rgb;
        color += texture(uSource, vTexCoord + vec2(-offset.x,  offset.y)).rgb * 2.0;
        color += texture(uSource, vTexCoord + vec2(0.0, 2.0 * offset.y)).rgb;
        color += texture(uSource, vTexCoord + vec2( offset.x,  offset.y)).rgb * 2.0;
        color += texture(uSource, vTexCoord + vec2(2.0 * offset.x, 0.0)).rgb;
        color += texture(uSource, vTexCoord + vec2( offset.x, -offset.y)).rgb * 2.0;
        color += texture(uSource, vTexCoord + vec2(0.0, -2.0 * offset.y)).rgb;
        color += texture(uSource, vTexCoord + vec2(-offset.x, -offset.y)).rgb * 2.0;
        color /= 12.0;
    }

    outColor = vec4(color, 1.0);
}
