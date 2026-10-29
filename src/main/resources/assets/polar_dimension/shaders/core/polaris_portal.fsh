#version 150

#moj_import <matrix.glsl>

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;

uniform float GameTime;
uniform int EndPortalLayers;

in vec4 texProj0;

const vec3[] COLORS = vec3[](
    vec3(0.131119, 0.024600, 0.141836),
    vec3(0.108348, 0.023981, 0.113640),
    vec3(0.120596, 0.025422, 0.129553),
    vec3(0.137062, 0.027471, 0.150532),
    vec3(0.119703, 0.029424, 0.131221),
    vec3(0.142863, 0.021724, 0.164122),
    vec3(0.191498, 0.027998, 0.220698),
    vec3(0.118735, 0.038530, 0.128453),
    vec3(0.224622, 0.032786, 0.259912),
    vec3(0.213119, 0.027547, 0.248694),
    vec3(0.176753, 0.034570, 0.205755),
    vec3(0.284081, 0.060833, 0.305241),
    vec3(0.246866, 0.035725, 0.297885),
    vec3(0.385369, 0.078835, 0.409555),
    vec3(0.375671, 0.097503, 0.408284),
    vec3(0.741789, 0.078705, 0.839007)
);

const mat4 SCALE_TRANSLATE = mat4(
    0.5, 0.0, 0.0, 0.25,
    0.0, 0.5, 0.0, 0.25,
    0.0, 0.0, 1.0, 0.0,
    0.0, 0.0, 0.0, 1.0
);

mat4 end_portal_layer(float layer) {
    mat4 translate = mat4(
        1.0, 0.0, 0.0, 17.0 / layer,
        0.0, 1.0, 0.0, (2.0 + layer / 1.5) * (GameTime * 1.5),
        0.0, 0.0, 1.0, 0.0,
        0.0, 0.0, 0.0, 1.0
    );

    mat2 rotate = mat2_rotate_z(radians((layer * layer * 4321.0 + layer * 9.0) * 2.0));

    mat2 scale = mat2((4.5 - layer / 4.0) * 2.0);

    return mat4(scale * rotate) * translate * SCALE_TRANSLATE;
}

out vec4 fragColor;

void main() {
    vec3 color = textureProj(Sampler0, texProj0).rgb * COLORS[0];
    for (int i = 0; i < EndPortalLayers; i++) {
        color += textureProj(Sampler1, texProj0 * end_portal_layer(float(i + 1))).rgb * COLORS[i];
    }
    fragColor = vec4(color, 1.0);
}

