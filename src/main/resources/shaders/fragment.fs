#version 330 core

uniform sampler2D uTexture;
uniform vec2 uAtlasSize;

in vec4 vPosition;
in vec2 vTexCoord;
in vec3 vNormal;

out vec4 fColor;

void main() {
    vec4 texColor = texture2D(uTexture, vTexCoord);
    if(texColor.a <= 0.1)
        discard;

    texColor.a = 1;


	float brightness = dot(vNormal, normalize(vec3(0.5,0.8,0.4))) * 0.5 + 0.5;

	fColor = texColor * brightness;
	//vec4(vNormal * 0.5 + 0.5, 1.0);
}