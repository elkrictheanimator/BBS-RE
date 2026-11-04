#version 150

#moj_import <fog.glsl>

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

// bbsvfx: smear-frame dissolve. 0 = intact (normal rendering), 1 = fully dissolved.
uniform float Dissolve;

// bbsvfx: colour gradient. GradColor.a = strength (0 = off); GradMode 0 = tint like Color, 1 = wash like Color overlay.
uniform vec4 GradColor;
uniform float GradMode;

in float vertexDistance;
in vec4 vertexColor;
in vec4 vertexColorB;
in float gradT;
in vec4 lightMapColor;
in vec4 overlayColor;
in vec2 texCoord0;
in vec4 normal;

out vec4 fragColor;

float bbsvfx_hash(vec2 p)
{
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

void main()
{
    vec4 color = texture(Sampler0, texCoord0);

    if (color.a < 0.1)
    {
        discard;
    }

    // bbsvfx: eat away chunks of the surface so trailing smear copies look dissolved, not just faded.
    if (Dissolve > 0.0 && bbsvfx_hash(floor(texCoord0 * 80.0)) < Dissolve)
    {
        discard;
    }

    float gt = GradColor.a > 0.0 ? clamp(gradT, 0.0, 1.0) * GradColor.a : 0.0;

    if (GradMode < 0.5)
    {
        color *= mix(vertexColor, vertexColorB, gt) * ColorModulator;
    }
    else
    {
        color *= vertexColor * ColorModulator;
        color.rgb = mix(color.rgb, GradColor.rgb, gt);
    }

    color.rgb = mix(overlayColor.rgb, color.rgb, overlayColor.a);
    color *= lightMapColor;

    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}
