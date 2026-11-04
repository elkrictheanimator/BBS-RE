#version 150

#moj_import <light.glsl>
#moj_import <fog.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;

uniform sampler2D Sampler1;
uniform sampler2D Sampler2;

uniform mat4 ModelViewMat;
uniform mat3 NormalMat;
uniform mat4 ProjMat;
uniform int FogShape;

uniform vec3 Light0_Direction;
uniform vec3 Light1_Direction;

// bbsvfx: colour gradient. GradColor = far colour (rgb) + strength (a, 0 = off); t runs along GradAxis
// from GradRange.x with 1 / length GradRange.y, in the vertex space of the model.
uniform vec4 GradColor;
uniform vec3 GradAxis;
uniform vec2 GradRange;

out float vertexDistance;
out vec4 vertexColor;
out vec4 vertexColorB;
out float gradT;
out vec4 lightMapColor;
out vec4 overlayColor;
out vec2 texCoord0;
out vec4 normal;

void main()
{
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    vec3 fogPos = (ModelViewMat * vec4(Position, 1.0)).xyz;
    vertexDistance = (FogShape == 0) ? length(fogPos) : max(length(fogPos.xz), abs(fogPos.y));
    vec3 fixNormal = normalize(NormalMat * Normal);
    vertexColor = minecraft_mix_light(Light0_Direction, Light1_Direction, fixNormal, Color);
    vertexColorB = minecraft_mix_light(Light0_Direction, Light1_Direction, fixNormal, vec4(GradColor.rgb, Color.a));
    // Unclamped on purpose: it is linear, so it interpolates exactly across a face; the fragment shader clamps.
    gradT = (dot(Position, GradAxis) - GradRange.x) * GradRange.y;
    lightMapColor = texelFetch(Sampler2, UV2 / 16, 0);
    overlayColor = texelFetch(Sampler1, UV1, 0);
    texCoord0 = UV0;
    normal = ProjMat * ModelViewMat * vec4(Normal, 0.0);
}
