/**
 *
 * @author a-hongo
 */
precision highp float;
uniform sampler2D u_Texture;
uniform vec4 u_dotColor;
uniform vec4 u_lineColor;
uniform vec4 u_gridControl;  // dotThreshold, lineThreshold, lineFadeShrink, occlusionShrink
varying vec3 v_TexCoordAlpha;

void main() {
    gl_FragColor = texture2D(u_Texture, v_TexCoordAlpha.xy);
}