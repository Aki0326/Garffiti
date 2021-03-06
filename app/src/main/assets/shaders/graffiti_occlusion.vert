#version 300 es
/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 *
 * @author n-nitta, a-hongo
 */
uniform mat4 u_ModelView;
uniform mat4 u_ModelViewProjection;
uniform mat2 u_PlaneUvMatrix;

//attribute vec4 a_Position;
//attribute vec3 a_XZPositionAlpha; // (x, z, alpha)
layout(location = 0) in vec3 a_XZPositionAlpha; // (x, z, alpha)
layout(location = 1) in vec3 a_Normal;

out vec3 v_ViewPosition;
out vec3 v_ViewNormal;
//attribute vec2 a_TexCoord;
//varying vec2 v_TexCoord;
//varying vec3 v_TexCoordAlpha;
out vec3 v_TexCoordAlpha;
//varying vec3 v_ScreenSpacePosition;
out vec3 v_ScreenSpacePosition;

void main() {
   vec4 local_pos = vec4(a_XZPositionAlpha.x, 0.0, a_XZPositionAlpha.y, 1.0);
   v_ViewPosition = (u_ModelView * local_pos).xyz;
   v_ViewNormal = normalize((u_ModelView * vec4(a_Normal, 0.0)).xyz);
   vec2 texture_pos = u_PlaneUvMatrix * vec2(local_pos.x, -local_pos.z);
   v_TexCoordAlpha = vec3(texture_pos.x + 0.5, texture_pos.y + 0.5, a_XZPositionAlpha.z);
   gl_Position = u_ModelViewProjection * local_pos;
   v_ScreenSpacePosition = gl_Position.xyz / gl_Position.w;
}