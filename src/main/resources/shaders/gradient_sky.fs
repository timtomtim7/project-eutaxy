#version 330 core

in vec3 vPosition;

out vec4 fColor;

void main() {
	vec3 dir = normalize(vPosition);
//	float d = dot(dir, normalize(vec3(0.3,1.0,-0.4)));
	float d = dot(dir, vec3(0, 1, 0));

	fColor = vec4(vec3(d * 0.25 + 0.7) * vec3(0.85, 0.9, 1.0), 1);
}