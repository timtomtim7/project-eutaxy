#version 330 core

uniform sampler2D uTexture;
//uniform sampler2D uDevTexture;
uniform vec3 uCameraPosition;
uniform vec2 uAtlasSize;

in vec4 vPosition;
in vec2 vTexCoord;
in vec3 vNormal;

out vec4 fColor;

const vec2[] axes = vec2[](
    vec2(0, 1),
    vec2(1, 0),
    vec2(0, -1),
    vec2(-1, 0),

    vec2(-1, 1),
    vec2(-1, -1),
    vec2(1, -1),
    vec2(1, 1)
);

void main() {
    vec4 texColor = texture2D(uTexture, vTexCoord);
    if(texColor.a <= 0.1)
        discard;

    float dist = 1 - clamp(distance(uCameraPosition, vPosition.xyz) / 8, 0, 1);
    float shading = 1;

    vec2 scaled = vTexCoord * uAtlasSize;
    vec2 center = floor(scaled) + 0.5;
    vec2 subTexture = fract(scaled);

    for(int i = 0; i < 4; i++) {
        vec2 axis = axes[i];
        float adj = texture2D(uTexture, (center + axis) / uAtlasSize).a;
        if(adj <= 0.1) {
            vec2 subTexture2 = subTexture * 2 - 1;
            float dot = (dot(subTexture2, axis) * 0.5 + 0.5);
            dot = pow(dot, 4) * 0.3; // pow was 6
//            texColor *= vec4(vec3(1 - dot), 1);
            shading *= 1 - dot;
        }
    }

    texColor *= 1 - ((1 - shading) * dist);
    texColor.a = 1;

	float brightness = 1;//clamp(dot(vNormal, normalize(vec3(0.3,1.0,-0.4))) * 0.5 + 0.5, 0, 1);

	fColor = texColor * brightness;
	//vec4(vNormal * 0.5 + 0.5, 1.0);
}