#version 300 es

precision highp float;

uniform sampler2D uSource;
uniform sampler2D uNormal;
uniform sampler2D uLight;
uniform int uStyle;
uniform float uAspect;
uniform float uNormalScale;
uniform float uIor;
uniform float uSurfaceRatio;
uniform float uDisplacement;
uniform float uThickness;
uniform float uDarkness;
uniform float uLightness;
uniform float uShadowness;

in vec2 vTexCoord;
layout(location = 0) out vec4 outColor;

vec3 sampleMoru(vec2 screenUv, vec2 delta) {
    vec2 u = screenUv + delta;
    vec2 local = vec2(
        fract((u.x - 0.5) * uNormalScale + 0.5),
        fract(u.y * uAspect + 0.5 * uAspect)
    );

    vec3 upperNormal = normalize(texture(uNormal, local).xyz * 2.0 - 1.0);
    vec3 lowerNormal = vec3(0.0, 1.0, 0.0);
    vec4 lightShadow = texture(uLight, local);
    float depth = -lightShadow.r * uDisplacement - uThickness;

    vec3 upperOut = normalize(refract(vec3(0.0, 1.0, 0.0), upperNormal, uIor));
    vec3 lowerOut = normalize(refract(upperOut, lowerNormal, uIor));
    vec3 path = upperOut * depth + lowerOut * uThickness;
    vec2 offset = vec2(path.x * uSurfaceRatio * uIor, 0.0);

    vec3 color = texture(uSource, clamp(screenUv + offset, 0.001, 0.999)).rgb;
    color *= 1.0 - uDarkness;
    color *= mix(vec3(1.0), lightShadow.bbb, uShadowness);
    color = mix(color, vec3(1.0), lightShadow.g * uLightness);
    return color;
}

void main() {
    vec3 original = texture(uSource, vTexCoord).rgb;
    if (uStyle == 0) {
        outColor = vec4(original, 1.0);
        return;
    }

    const float stepUv = 1.736e-4;
    vec3 color = sampleMoru(vTexCoord, vec2(0.0));
    color += sampleMoru(vTexCoord, vec2(-stepUv));
    color += sampleMoru(vTexCoord, vec2(stepUv));
    outColor = vec4(color / 3.0, 1.0);
}
