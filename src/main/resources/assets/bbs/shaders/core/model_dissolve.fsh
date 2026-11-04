#version 150

#moj_import <fog.glsl>

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

// bbsvfx: smear-frame dissolve. 0 = intact (normal rendering), 1 = fully dissolved.
uniform float Dissolve;

// bbsvfx: colour effect. GradColor.rgb = second colour, .a = strength (0 = off). GradMode 0 = tint like Color,
// 1 = wash like Color overlay. GradStyle 0 solid, 1 rainbow, 2 neon, 3 animated image (Sampler3).
// GradAnim = speed, rainbow cycles, image tiling, image fps. GradTime in seconds.
uniform sampler2D Sampler3;
uniform vec4 GradColor;
uniform float GradMode;
uniform float GradStyle;
uniform float GradTime;
uniform float GradFrames;
uniform float GradGlow;
uniform vec4 GradAnim;

in float vertexDistance;
in vec4 vertexColor;
in vec4 vertexLight;
in float gradT;
in vec4 lightMapColor;
in vec4 overlayColor;
in vec2 texCoord0;
in vec4 normal;

out vec4 fragColor;

vec3 bbsvfx_hsv(float h)
{
    vec3 p = abs(fract(vec3(h) + vec3(1.0, 2.0 / 3.0, 1.0 / 3.0)) * 6.0 - 3.0);

    return clamp(p - 1.0, 0.0, 1.0);
}

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

    float ramp = clamp(gradT, 0.0, 1.0);
    float gt = GradColor.a > 0.0 ? ramp * GradColor.a : 0.0;
    // Brightness gradient: 0..1 blends the lit look toward full bright, above 1 it overbrightens.
    float glow = GradGlow > 0.0 ? ramp * GradGlow : 0.0;
    vec3 src = GradColor.rgb;
    float emissive = 0.0;

    if (gt > 0.0)
    {
        if (GradStyle > 2.5)
        {
            // Animated image: frames stacked top to bottom, laid over the model's own texture layout.
            float frame = GradFrames > 1.0 ? floor(mod(GradTime * GradAnim.w, GradFrames)) : 0.0;
            vec2 uv = texCoord0 * GradAnim.z;

            uv.x += GradTime * GradAnim.x;
            uv = fract(uv);
            uv.y = (uv.y + frame) / max(GradFrames, 1.0);

            vec4 img = texture(Sampler3, uv);

            src = img.rgb * GradColor.rgb;
            gt *= img.a;
        }
        else if (GradStyle > 1.5)
        {
            // Neon: a bright pulsing glow that ignores the scene's lighting.
            src = GradColor.rgb * 1.25 * (0.8 + 0.2 * sin(GradTime * GradAnim.x * 6.2831853));
            emissive = 1.0;
        }
        else if (GradStyle > 0.5)
        {
            // Rainbow: the hue runs along the gradient and scrolls with time.
            src = bbsvfx_hsv(fract(gradT * GradAnim.y + GradTime * GradAnim.x)) * GradColor.rgb;
        }
    }

    if (GradMode < 0.5)
    {
        color *= vec4(mix(vertexColor.rgb, vertexLight.rgb * src, gt), vertexColor.a) * ColorModulator;
    }
    else
    {
        color *= vertexColor * ColorModulator;
        color.rgb = mix(color.rgb, src, gt);
    }

    color.rgb = mix(overlayColor.rgb, color.rgb, overlayColor.a);
    color *= mix(lightMapColor, vec4(1.0), clamp(max(gt * emissive, glow), 0.0, 1.0));
    color.rgb *= 1.0 + max(glow - 1.0, 0.0);

    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}
