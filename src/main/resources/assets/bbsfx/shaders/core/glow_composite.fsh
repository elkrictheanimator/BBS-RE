#version 150

/* Deep Glow composite: the accumulated halo is tinted and pushed through an exponential
 * exposure curve, so the dense core saturates towards white while the rim keeps its
 * colour, and it never clips hard. Meant for additive blending onto the frame. */

uniform sampler2D Sampler0;
uniform vec3 Tint;
uniform float Exposure;

in vec2 texCoord0;

out vec4 fragColor;

float noise(vec2 co)
{
    return fract(sin(dot(co, vec2(12.9898, 78.233))) * 43758.5453);
}

void main()
{
    vec3 energy = texture(Sampler0, texCoord0).rgb * Tint * Exposure;
    vec3 color = vec3(1.0) - exp(-energy);

    /* A bit of dither against banding in the soft tail */
    color += (noise(gl_FragCoord.xy) - 0.5) / 255.0;

    fragColor = vec4(max(color, vec3(0.0)), 1.0);
}
