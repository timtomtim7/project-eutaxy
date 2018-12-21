#version 330 core

uniform sampler2D uTexture;

in vec2 vTexCoord;

out vec4 fColor;

void main() {
//    vec2 v = vTexCoord * 2 - 1;
//    float f = clamp(distance(v, vec2(0, 0)), 0, 1);
//    float vignette = (1 - f*f) * 0.25 + 0.75;
	fColor = texture2D(uTexture, vTexCoord);// * vignette;
}