#version 330 core

layout(location = 0) in vec3 aPosition;
layout(location = 1) in vec2 aTexCoord;
layout(location = 2) in vec3 aNormal;

out vec4 vPosition;
out vec2 vTexCoord;
out vec3 vNormal;

uniform mat4 uModel;
uniform mat4 uViewProj;
uniform vec2 uAtlasSize;

void main() {
	vTexCoord = aTexCoord / uAtlasSize;
	vNormal = normalize((vec4(aNormal, 0.0) * uModel).xyz);
//	gl_Position = vec4(aPosition, 1) * viewProj;
	vec4 position = (vec4(aPosition, 1) * uModel);
	vPosition = position;
	gl_Position = position * uViewProj;
}