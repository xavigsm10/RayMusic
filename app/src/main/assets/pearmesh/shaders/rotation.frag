#version 300 es

precision highp float;

uniform sampler2D uCurrentArtwork;
uniform sampler2D uPreviousArtwork;
uniform float uTransitionMix;

in vec2 vTexCoord;
layout(location = 0) out vec4 outColor;

void main() {
    // Android Bitmap rows are top-down; the intermediate GL textures are bottom-up.
    vec2 artworkUv = vec2(vTexCoord.x, 1.0 - vTexCoord.y);
    vec3 current = texture(uCurrentArtwork, artworkUv).rgb;
    vec3 previous = texture(uPreviousArtwork, artworkUv).rgb;
    outColor = vec4(mix(previous, current, uTransitionMix), 1.0);
}
