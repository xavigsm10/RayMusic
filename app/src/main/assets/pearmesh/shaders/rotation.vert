#version 300 es

layout(location = 0) in vec2 aPosition;
layout(location = 1) in vec2 aTexCoord;

uniform float uTime;
uniform vec2 uViewScale;
uniform vec3 uImageScales;
uniform int uInstance;
uniform bool uArtworkFill;

out vec2 vTexCoord;

vec2 rotateCounterClockwise(vec2 value, float angle) {
    float sine = sin(angle);
    float cosine = cos(angle);
    return vec2(
        cosine * value.x - sine * value.y,
        sine * value.x + cosine * value.y
    );
}

vec2 modelTranslation(int instance) {
    if (instance == 1) return vec2(-0.25, 0.15);
    if (instance == 2) return vec2(0.7, 0.7);
    return vec2(0.0);
}

float modelScale(int instance) {
    return instance == 0 ? 1.4 : 0.7;
}

float rotationTimeScale(int instance) {
    if (instance == 1) return 70.0;
    if (instance == 2) return 90.0;
    return 120.0;
}

void main() {
    if (uArtworkFill) {
        gl_Position = vec4(aPosition, 0.0, 1.0);
        vTexCoord = (aTexCoord - 0.5) / uViewScale + 0.5;
        return;
    }

    float twoPi = 6.2831853071795864769;
    float angle = uTime * twoPi / rotationTimeScale(uInstance);
    vec2 position = rotateCounterClockwise(aPosition, angle);
    position *= modelScale(uInstance);
    position += modelTranslation(uInstance);
    position *= uViewScale;
    // Lyricify applies one shared scale to the whole artwork group.
    position *= uImageScales.x;

    // Instance 2 is parented to instance 0 and receives the parent rotation once.
    if (uInstance == 2) {
        float parentAngle = uTime * twoPi / rotationTimeScale(0);
        position = rotateCounterClockwise(position, parentAngle);
    }

    gl_Position = vec4(position, 0.0, 1.0);
    vTexCoord = aTexCoord;
}
