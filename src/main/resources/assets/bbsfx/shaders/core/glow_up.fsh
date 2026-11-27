#version 150

/* Dual filter upsample: an 8 tap tent around the coarser level. It is added on top of
 * the finer level, so every level contributes - tight edges from the fine ones, a wide
 * soft halo from the coarse ones, which together fall off like a real glow. */

uniform sampler2D Sampler0;
uniform vec2 Texel;
uniform float Intensity;

in vec2 texCoord0;

out vec4 fragColor;

void main()
{
    vec2 uv = texCoord0;
    vec4 sum = texture(Sampler0, uv + vec2(-2.0 * Texel.x, 0.0));

    sum += texture(Sampler0, uv + vec2(2.0 * Texel.x, 0.0));
    sum += texture(Sampler0, uv + vec2(0.0, -2.0 * Texel.y));
    sum += texture(Sampler0, uv + vec2(0.0, 2.0 * Texel.y));
    sum += texture(Sampler0, uv + vec2(-Texel.x, Texel.y)) * 2.0;
    sum += texture(Sampler0, uv + vec2(Texel.x, Texel.y)) * 2.0;
    sum += texture(Sampler0, uv + vec2(Texel.x, -Texel.y)) * 2.0;
    sum += texture(Sampler0, uv + vec2(-Texel.x, -Texel.y)) * 2.0;

    fragColor = sum / 12.0 * Intensity;
}
