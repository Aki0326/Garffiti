#version 300 es
/*
 * Copyright 2020 Google LLC
 *
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
 * @author a-hongo
 */
//precision highp float;
precision mediump float;

// The virtual scene as rendered to a texture via a framebuffer. This will be
// composed with the background image depending on which modes were set in
// DepthCompositionRenderer.setDepthModes.
//uniform sampler2D u_VirtualSceneColorTexture;
uniform sampler2D u_Texture;

#if USE_OCCLUSION
// The AR camera depth texture.
//uniform sampler2D u_DepthTexture;
uniform sampler2D u_CameraDepthTexture;
uniform mat3 u_DepthUvTransform;
// The near and far clipping planes, used to transform the virtual scene depth
// back into view space to compare with the camera depth texture.
//uniform float u_ZNear;
//uniform float u_ZFar;
// The aspect ratio of the screen. This is used during to create uniform
// blurring for occluded objects.
uniform float u_DepthAspectRatio;
#endif  // USE_OCCLUSION

//varying vec3 v_ViewPosition;
in vec3 v_ViewPosition;
in vec3 v_ViewNormal;
//in vec2 v_VirtualSceneTexCoord;
////varying vec3 v_TexCoordAlpha;
in vec3 v_TexCoordAlpha;
in vec3 v_ScreenSpacePosition;
//uniform vec4 u_ObjColor;

//uniform vec4 u_dotColor;
//uniform vec4 u_lineColor;
//uniform vec4 u_gridControl;  // dotThreshold, lineThreshold, lineFadeShrink, occlusionShrink

//layout(location = 0) out vec4 gl_FragColor;
layout(location = 0) out vec4 o_FragColor;

// Number of mipmap levels in the filtered cubemap.
const int kNumberOfRoughnessLevels = NUMBER_OF_MIPMAP_LEVELS;

// The intensity of the main directional light.
uniform vec3 u_LightIntensity;

// Inverse view matrix. Used for converting normals back into world space for
// environmental radiance calculations.
uniform mat4 u_ViewInverse;

// The direction of the main directional light in view space.
uniform vec4 u_ViewLightDirection;

// The coefficients for the spherical harmonic function which models the diffuse
// irradiance of a distant environmental light for a given surface normal in
// world space. These coefficients must be premultiplied with their
// corresponding spherical harmonics constants. See
// HelloArActivity.updateSphericalHarmonicsCoefficients for more information.
uniform vec3 u_SphericalHarmonicsCoefficients[9];

// The filtered cubemap texture which models the LD term (i.e. radiance (L)
// times distribution function (D)) of the environmental specular calculation as
// a function of direction and roughness.
uniform samplerCube u_Cubemap;

// The DFG lookup texture which models the DFG1 and DFG2 terms of the
// environmental specular calculation as a function of normal dot view and
// perceptual roughness.
uniform sampler2D u_DfgTexture;

// If the current light estimate is valid. Used to short circuit the entire
// shader when the light estimate is not valid.
uniform bool u_LightEstimateIsValid;

struct MaterialParameters {
  vec3 diffuse;
  float perceptualRoughness;  // perceptually linear roughness
  float roughness;            // non-perceptually linear roughness
  float metallic;
  float ambientOcclusion;
  vec3 f0;                  // reflectance
  vec2 dfg;                 // DFG1 and DFG2 terms
  vec3 energyCompensation;  // energy preservation for multiscattering
};

struct ShadingParameters {
// Halfway here refers to halfway between the view and light directions.
  float normalDotView;
  float normalDotHalfway;
  float normalDotLight;
  float viewDotHalfway;
  float oneMinusNormalDotHalfwaySquared;

// These unit vectors are in world space and are used for the environmental
// lighting math.
  vec3 worldNormalDirection;
  vec3 worldReflectDirection;
};

const float kPi = 3.14159265359;

#if USE_OCCLUSION

float DepthGetMillimeters(in sampler2D depth_texture, in vec2 depth_uv) {
  // Depth is packed into the red and green components of its texture.
  // The texture is a normalized format, storing millimeters.
  vec3 packedDepthAndVisibility = texture(depth_texture, depth_uv).xyz;
  return dot(packedDepthAndVisibility.xy, vec2(255.0, 256.0 * 255.0));
}

// Returns linear interpolation position of value between min and max bounds.
// E.g., DepthInverseLerp(1100, 1000, 2000) returns 0.1.
float DepthInverseLerp(in float value, in float min_bound, in float max_bound) {
  return clamp((value - min_bound) / (max_bound - min_bound), 0.0, 1.0);
}

// Returns a value between 0.0 (not visible) and 1.0 (completely visible)
// Which represents how visible or occluded is the pixel in relation to the
// depth map.
float DepthGetVisibility(in sampler2D depth_texture, in vec2 depth_uv,
in float asset_depth_mm) {
  float depth_mm = DepthGetMillimeters(depth_texture, depth_uv);

  // Instead of a hard z-buffer test, allow the asset to fade into the
  // background along a 2 * kDepthTolerancePerMm * asset_depth_mm
  // range centered on the background depth.
  const float kDepthTolerancePerMm = 0.015;
  float visibility_occlusion = clamp(0.5 * (depth_mm - asset_depth_mm) /
  (kDepthTolerancePerMm * asset_depth_mm) + 0.5, 0.0, 1.0);

  // Depth close to zero is most likely invalid, do not use it for occlusions.
  float visibility_depth_near = 1.0 - DepthInverseLerp(
  depth_mm, /*min_depth_mm=*/150.0, /*max_depth_mm=*/200.0);

  // Same for very high depth values.
  float visibility_depth_far = DepthInverseLerp(
  depth_mm, /*min_depth_mm=*/7500.0, /*max_depth_mm=*/8000.0);

  const float kOcclusionAlpha = 0.0;
  float visibility =
      max(max(visibility_occlusion, kOcclusionAlpha),
          max(visibility_depth_near, visibility_depth_far));

  return visibility;
}

float DepthGetBlurredVisibilityAroundUV(in sampler2D depth_texture, in vec2 uv,
                                        in float asset_depth_mm) {
  // Kernel used:
  // 0   4   7   4   0
  // 4   16  26  16  4
  // 7   26  41  26  7
  // 4   16  26  16  4
  // 0   4   7   4   0
  const float kKernelTotalWeights = 269.0;
  float sum = 0.0;

  const float kOcclusionBlurAmount = 0.01;
  vec2 blurriness = vec2(kOcclusionBlurAmount,
                         kOcclusionBlurAmount * u_DepthAspectRatio);

  float current = 0.0;

  current += DepthGetVisibility(depth_texture, uv + vec2(-1.0, -2.0) * blurriness, asset_depth_mm);
  current += DepthGetVisibility(depth_texture, uv + vec2(+1.0, -2.0) * blurriness, asset_depth_mm);
  current += DepthGetVisibility(depth_texture, uv + vec2(-1.0, +2.0) * blurriness, asset_depth_mm);
  current += DepthGetVisibility(depth_texture, uv + vec2(+1.0, +2.0) * blurriness, asset_depth_mm);
  current += DepthGetVisibility(depth_texture, uv + vec2(-2.0, +1.0) * blurriness, asset_depth_mm);
  current += DepthGetVisibility(depth_texture, uv + vec2(+2.0, +1.0) * blurriness, asset_depth_mm);
  current += DepthGetVisibility(depth_texture, uv + vec2(-2.0, -1.0) * blurriness, asset_depth_mm);
  current += DepthGetVisibility(depth_texture, uv + vec2(+2.0, -1.0) * blurriness, asset_depth_mm);
  sum += current * 4.0;

  current = 0.0;
  current += DepthGetVisibility(depth_texture, uv + vec2(-2.0, -0.0) * blurriness, asset_depth_mm);
  current += DepthGetVisibility(depth_texture, uv + vec2(+2.0, +0.0) * blurriness, asset_depth_mm);
  current += DepthGetVisibility(depth_texture, uv + vec2(+0.0, +2.0) * blurriness, asset_depth_mm);
  current += DepthGetVisibility(depth_texture, uv + vec2(-0.0, -2.0) * blurriness, asset_depth_mm);
  sum += current * 7.0;

  current = 0.0;
  current += DepthGetVisibility(depth_texture, uv + vec2(-1.0, -1.0) * blurriness, asset_depth_mm);
  current += DepthGetVisibility(depth_texture, uv + vec2(+1.0, -1.0) * blurriness, asset_depth_mm);
  current += DepthGetVisibility(depth_texture, uv + vec2(-1.0, +1.0) * blurriness, asset_depth_mm);
  current += DepthGetVisibility(depth_texture, uv + vec2(+1.0, +1.0) * blurriness, asset_depth_mm);
  sum += current * 16.0;

  current = 0.0;
  current += DepthGetVisibility(depth_texture, uv + vec2(+0.0, +1.0) * blurriness, asset_depth_mm);
  current += DepthGetVisibility(depth_texture, uv + vec2(-0.0, -1.0) * blurriness, asset_depth_mm);
  current += DepthGetVisibility(depth_texture, uv + vec2(-1.0, -0.0) * blurriness, asset_depth_mm);
  current += DepthGetVisibility(depth_texture, uv + vec2(+1.0, +0.0) * blurriness, asset_depth_mm);
  sum += current * 26.0;

  sum += DepthGetVisibility(depth_texture, uv , asset_depth_mm) * 41.0;

  return sum / kKernelTotalWeights;
}

#endif  // USE_OCCLUSION

vec3 Pbr_CalculateMainLightRadiance(const ShadingParameters shading,
                                    const MaterialParameters material,
                                    const vec3 mainLightIntensity) {
  // Lambertian diffuse
  vec3 diffuseTerm = material.diffuse / kPi;

  // Note that if we were not using the HDR cubemap from ARCore for specular
  // lighting, we would be adding a specular contribution from the main light
  // here. See the top of the file for a more detailed explanation.

  return diffuseTerm * mainLightIntensity * shading.normalDotLight;
}

vec3 Pbr_CalculateDiffuseEnvironmentalRadiance(const vec3 normal,
                                               const vec3 coefficients[9]) {
  // See HelloArActivity.updateSphericalHarmonicsCoefficients() for more
  // information about this calculation.
  vec3 radiance = coefficients[0] + coefficients[1] * (normal.y) +
                  coefficients[2] * (normal.z) + coefficients[3] * (normal.x) +
                  coefficients[4] * (normal.y * normal.x) +
                  coefficients[5] * (normal.y * normal.z) +
                  coefficients[6] * (3.0 * normal.z * normal.z - 1.0) +
                  coefficients[7] * (normal.z * normal.x) +
                  coefficients[8] * (normal.x * normal.x - normal.y * normal.y);
  return max(radiance, 0.0);
}

vec3 Pbr_CalculateSpecularEnvironmentalRadiance(
    const ShadingParameters shading, const MaterialParameters material,
    const samplerCube cubemap) {
  // Lagarde and de Rousiers 2014, "Moving Frostbite to PBR"
  float specularAO =
      clamp(pow(shading.normalDotView + material.ambientOcclusion,
                exp2(-16.0 * material.roughness - 1.0)) -
                1.0 + material.ambientOcclusion,
            0.0, 1.0);
  // Combine DFG and LD terms
  float lod =
      material.perceptualRoughness * float(kNumberOfRoughnessLevels - 1);
  vec3 LD = textureLod(cubemap, shading.worldReflectDirection, lod).rgb;
  vec3 E = mix(material.dfg.xxx, material.dfg.yyy, material.f0);
  return E * LD * specularAO * material.energyCompensation;
}

vec3 Pbr_CalculateEnvironmentalRadiance(
const ShadingParameters shading, const MaterialParameters material,
const vec3 sphericalHarmonicsCoefficients[9], const samplerCube cubemap) {
  // The lambertian diffuse BRDF term (1/pi) is baked into
  // HelloArActivity.sphericalHarmonicsFactors.
  vec3 diffuseTerm =
  Pbr_CalculateDiffuseEnvironmentalRadiance(
          shading.worldNormalDirection, sphericalHarmonicsCoefficients) *
      material.diffuse * material.ambientOcclusion;

  vec3 specularTerm =
      Pbr_CalculateSpecularEnvironmentalRadiance(shading, material, cubemap);

  return diffuseTerm + specularTerm;
}

void Pbr_CreateShadingParameters(const in vec3 viewNormal,
                                 const in vec3 viewPosition,
                                 const in vec4 viewLightDirection,
                                 const in mat4 viewInverse,
                                 out ShadingParameters shading) {
  vec3 normalDirection = normalize(viewNormal);
  vec3 viewDirection = -normalize(viewPosition);
  vec3 lightDirection = normalize(viewLightDirection.xyz);
  vec3 halfwayDirection = normalize(viewDirection + lightDirection);

  // Clamping the minimum bound yields better results with values less than or
  // equal to 0, which would otherwise cause discontinuity in the geometry
  // factor. Neubelt and Pettineo 2013, "Crafting a Next-gen Material Pipeline
  // for The Order: 1886"
  shading.normalDotView = max(dot(normalDirection, viewDirection), 1e-4);
  shading.normalDotHalfway =
  clamp(dot(normalDirection, halfwayDirection), 0.0, 1.0);
  shading.normalDotLight =
  clamp(dot(normalDirection, lightDirection), 0.0, 1.0);
  shading.viewDotHalfway =
  clamp(dot(viewDirection, halfwayDirection), 0.0, 1.0);

  // The following calculation can be proven as being equivalent to 1-(N.H)^2 by
  // using Lagrange's identity.
  //
  // ||a x b||^2 = ||a||^2 ||b||^2 - (a . b)^2
  //
  // Since we're using unit vectors: ||N x H||^2 = 1 - (N . H)^2
  //
  // We are calculating it in this way to preserve floating point precision.
  vec3 NxH = cross(normalDirection, halfwayDirection);
  shading.oneMinusNormalDotHalfwaySquared = dot(NxH, NxH);

  shading.worldNormalDirection = (viewInverse * vec4(normalDirection, 0.0)).xyz;
  vec3 reflectDirection = reflect(-viewDirection, normalDirection);
  shading.worldReflectDirection =
  (viewInverse * vec4(reflectDirection, 0.0)).xyz;
}

void Pbr_CreateMaterialParameters(const in vec2 texCoord,
                                  const in sampler2D texture,
                                  const in sampler2D dfgTexture,
                                  const in ShadingParameters shading,
                                  out MaterialParameters material) {
  // Read the material parameters from the textures
  vec3 color = texture(texture, texCoord).rgb;
  // Roughness inputs are perceptually linear; convert them to regular roughness
  // values. Roughness levels approaching 0 will make specular reflections
  // completely invisible, so cap the lower bound. This value was chosen such
  // that (kMinPerceptualRoughness^4) > 0 in fp16 (i.e. 2^(-14/4), rounded up).
  // https://github.com/google/filament/blob/main/shaders/src/common_material.fs#L2
  const float kMinPerceptualRoughness = 0.089;
  material.perceptualRoughness = kMinPerceptualRoughness;
  material.roughness =
      material.perceptualRoughness * material.perceptualRoughness;
  material.metallic = 0.0;
  material.ambientOcclusion = 0.0;

  material.diffuse = color * (1.0 - material.metallic);
  // F0 is defined as "Fresnel reflectance at 0 degrees", i.e. specular
  // reflectance when light is grazing a surface perfectly perpendicularly. This
  // value is derived from the index of refraction for a material. Most
  // dielectric materials have an F0 value of 0.00-0.08, which leaves 0.04 as a
  // reasonable constant for a simple roughness/metallic material workflow as
  // implemented by this shader.
  material.f0 = mix(vec3(0.04), color, material.metallic);

  // The DFG texture is a simple lookup table indexed by [normal dot view,
  // perceptualRoughness].
  material.dfg =
      textureLod(dfgTexture,
                 vec2(shading.normalDotView, material.perceptualRoughness), 0.0)
          .xy;

  // Energy preservation for multiscattering (see
  // https://google.github.io/filament/Filament.md.html#materialsystem/improvingthebrdfs)
  material.energyCompensation =
      1.0 + material.f0 * (1.0 / material.dfg.y - 1.0);
}

vec3 LinearToSrgb(const vec3 color) {
  vec3 kGamma = vec3(1.0 / 2.2);
  return clamp(pow(color, kGamma), 0.0, 1.0);
}

void main() {
//    gl_FragColor = texture2D(u_Texture, v_TexCoordAlpha.xy);
  o_FragColor = texture(u_Texture, v_TexCoordAlpha.xy);

// Perform lighting calculations if the estimation is valid.
if (u_LightEstimateIsValid) {
  ShadingParameters shading;

  Pbr_CreateShadingParameters(v_ViewNormal, v_ViewPosition,
                              u_ViewLightDirection, u_ViewInverse, shading);
  MaterialParameters material;
  Pbr_CreateMaterialParameters(v_TexCoordAlpha.xy, u_Texture,
                               u_DfgTexture, shading, material);

  // Combine the radiance contributions of both the main light and environment
  vec3 mainLightRadiance =
      Pbr_CalculateMainLightRadiance(shading, material, u_LightIntensity);

  vec3 environmentalRadiance = Pbr_CalculateEnvironmentalRadiance(
      shading, material, u_SphericalHarmonicsCoefficients, u_Cubemap);

  vec3 radiance = mainLightRadiance + environmentalRadiance;

  // Convert final color to sRGB color space
//  o_FragColor = vec4(LinearToSrgb(radiance), 1.0);
}

  #if USE_OCCLUSION
  const float kMetersToMillimeters = 1000.0;
//  if (o_FragColor.a == 0.0) {
    // There's no sense in calculating occlusion for a fully transparent pixel.
//    return;
//  }
  //  float assetDepthMm = Depth_GetVirtualSceneDepthMillimeters(
  //  u_VirtualSceneDepthTexture, v_VirtualSceneTexCoord, u_ZNear, u_ZFar);
//  float assetDepthMm = Depth_GetVirtualSceneDepthMillimeters(
//  u_VirtualSceneDepthTexture, v_TexCoordAlpha.xy, u_ZNear, u_ZFar);
    float asset_depth_mm = v_ViewPosition.z * kMetersToMillimeters * -1.;

  // Computes the texture coordinates to sample from the depth image.
  vec2 depth_uvs = (u_DepthUvTransform * vec3(v_ScreenSpacePosition.xy, 1)).xy;

  // The following step is very costly. Replace the last line with the
  // commented line if it's too expensive.
  o_FragColor *= DepthGetVisibility(u_CameraDepthTexture, depth_uvs, asset_depth_mm);
//  o_FragColor *= round(DepthGetVisibility(u_CameraDepthTexture, depth_uvs, asset_depth_mm));

//  o_FragColor *= DepthGetBlurredVisibilityAroundUV(u_CameraDepthTexture, depth_uvs, asset_depth_mm);

#endif  // USE_OCCLUSION
}
