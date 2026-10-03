#version 300 es

precision highp float;

uniform sampler2D uLyricsBackdrop;
uniform sampler2D uOrdinaryBackdrop;
uniform float uBlackScrimAlpha;
uniform float uLyricsModeMix;
uniform float uDitherStrength;
uniform int uMaterialMode;
in vec2 vTexCoord;
in vec2 vOrdinaryTexCoord;
layout(location = 0) out vec4 outColor;

vec3 applySaturation(vec3 color, float saturation) {
    vec3 redColumn = vec3(
        0.2126 + 0.7873 * saturation,
        0.2126 - 0.2126 * saturation,
        0.2126 - 0.2126 * saturation
    );
    vec3 greenColumn = vec3(
        0.7152 - 0.7152 * saturation,
        0.7152 + 0.2848 * saturation,
        0.7152 - 0.7152 * saturation
    );
    vec3 blueColumn = vec3(
        0.0722 - 0.0722 * saturation,
        0.0722 - 0.0722 * saturation,
        0.0722 + 0.9278 * saturation
    );
    return redColumn * color.r + greenColumn * color.g + blueColumn * color.b;
}

vec3 treated(sampler2D source, vec2 coordinate) {
    vec3 color = texture(source, coordinate).rgb;
    color = applySaturation(color, 1.4);
    color = clamp(color, vec3(-0.752941), vec3(1.25098));
    // Updated Lyricify material reduces saturation again before composition. Original is 0.70.
    color = applySaturation(color, 0.70);
    // Apply a non-linear plus darker black scrim. Makes it more like Apple Music.
    float luminance = dot(color, vec3(0.2126, 0.7152, 0.0722));
    float brightnessCurve = pow(max(luminance, 0.0), 1.25);
    float subtraction = uBlackScrimAlpha * brightnessCurve;
    color = max(color - vec3(subtraction), vec3(0.0));
    return color;
}

/* Moru is applied in a separate fullscreen pass after the mesh material. */
/*
vec3 moruSample(vec2 screenCoordinate, vec2 localCoordinate, vec2 sampleOffset) {
    // Moru keeps local texture coordinates separate from the transformed
    // wallpaper coordinate used for the refracted lookup.
    vec2 u = localCoordinate + sampleOffset;
    vec2 local = vec2(
        fract((u.x - 0.5) * uMoruNormalScale + 0.5),
        fract(u.y * uMoruAspect + 0.5 * uMoruAspect)
    );

    vec3 upperNormal = normalize(texture(uMoruNormal, local).xyz * 2.0 - 1.0);
    vec3 lowerNormal = vec3(0.0, 1.0, 0.0);
    vec4 lightShadow = texture(uMoruLight, local);
    float depth = -lightShadow.r * uMoruDisplacement - uMoruThickness;

    vec3 upperOut = normalize(refract(vec3(0.0, 1.0, 0.0), upperNormal, uMoruIor));
    vec3 upperPath = upperOut * depth;
    vec3 lowerOut = normalize(refract(upperOut, lowerNormal, uMoruIor));
    vec3 path = upperPath + lowerOut * uMoruThickness;
    vec2 refractOffset = vec2(path.x * uMoruSurfaceRatio * uMoruIor, 0.0);

    vec3 result = texture(
        uOrdinaryBackdrop,
        clamp(screenCoordinate + refractOffset, 0.001, 0.999)
    ).rgb;
    result *= 1.0 - uMoruDarkness;
    result *= mix(vec3(1.0), lightShadow.bbb, uMoruShadowness);
    result = mix(result, vec3(1.0), lightShadow.g * uMoruLightness);
    return result;
}

vec3 applyMoru(vec3 color, vec2 screenCoordinate, vec2 localCoordinate) {
    if (uMoruStyle == 0) return color;
    const float sampleStep = 1.736e-4;
    vec3 refracted = moruSample(screenCoordinate, localCoordinate, vec2(0.0));
    refracted += moruSample(screenCoordinate, localCoordinate, vec2(-sampleStep));
    refracted += moruSample(screenCoordinate, localCoordinate, vec2(sampleStep));
    return mix(color, refracted / 3.0, 0.82);
}
*/

void main() {
    vec3 color;
    if (uMaterialMode == 0) {
        color = treated(uOrdinaryBackdrop, vOrdinaryTexCoord);
    } else if (uMaterialMode == 1) {
        color = treated(uLyricsBackdrop, vTexCoord);
    } else if (uMaterialMode == 2) {
        vec3 ordinary = treated(uOrdinaryBackdrop, vOrdinaryTexCoord);
        vec3 lyrics = treated(uLyricsBackdrop, vTexCoord);
        color = mix(ordinary, lyrics, uLyricsModeMix);
    } else {
        vec3 ordinary = treated(uOrdinaryBackdrop, vOrdinaryTexCoord);
        color = mix(ordinary, vec3(0.0), uLyricsModeMix);
    }

    float dither = fract(
        52.9829189 * fract(dot(gl_FragCoord.xy, vec2(0.06711056, 0.00583715)))
    ) - 0.5;
    float ditherStrength = uDitherStrength;
    if (uMaterialMode == 3) {
        ditherStrength *= 1.0 - uLyricsModeMix;
    }
    color += dither * (ditherStrength / 255.0);
    outColor = vec4(clamp(color, 0.07, 0.97), 1.0);
}
