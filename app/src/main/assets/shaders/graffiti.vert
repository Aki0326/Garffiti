/**
 *
 * Created by a-hogno.
 * @author n-nitta
 */
uniform mat4 u_ModelViewProjection;
uniform mat2 u_PlaneUvMatrix;

attribute vec3 a_XZPositionAlpha; // (x, z, alpha)

varying vec3 v_TexCoordAlpha;

void main() {
    vec4 local_pos = vec4(a_XZPositionAlpha.x, 0.0, a_XZPositionAlpha.y, 1.0);
    vec2 texture_pos = u_PlaneUvMatrix * vec2(local_pos.x, -local_pos.z);
    v_TexCoordAlpha = vec3(texture_pos.x + 0.5, texture_pos.y + 0.5, a_XZPositionAlpha.z);
    gl_Position = u_ModelViewProjection * local_pos;
}