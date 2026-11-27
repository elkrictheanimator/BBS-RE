#version 150

/* Dual filter downsample (Bjorge, "Bandwidth-efficient rendering", 2015): the centre
 * and the four diagonal neighbours of the source, which blurs while it halves. */

uniform sampler2D Sampler0;
uniform vec2 Texel;

in vec2 texCoord0;

out vec4 fragColor;

void main()
{
    vec2 uv = texCoord0;
    vec4 sum = texture(Sampler0, uv) * 4.0;

    sum += texture(Sampler0, uv - Texel);
    sum += texture(Sampler0, uv + Texel);
    sum += texture(Sampler0, uv + vec2(Texel.x, -Texel.y));
    sum += texture(Sampler0, uv + vec2(-Texel.x, Texel.y));

    fragColor = sum / 8.0;
}
