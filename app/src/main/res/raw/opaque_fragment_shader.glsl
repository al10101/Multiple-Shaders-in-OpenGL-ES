//MIT License
//
//Copyright (c) 2022 Jose Alberto Cabrera Jaime
//
//Permission is hereby granted, free of charge, to any person obtaining a copy
//of this software and associated documentation files (the "Software"), to deal
//in the Software without restriction, including without limitation the rights
//to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
//copies of the Software, and to permit persons to whom the Software is
//furnished to do so, subject to the following conditions:
//
//The above copyright notice and this permission notice shall be included in all
//copies or substantial portions of the Software.
//
//THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
//SOFTWARE.

precision mediump float;

uniform vec3 u_CameraPosition;

uniform vec3 u_LightPosition;
uniform vec3 u_LightColor;
uniform vec3 u_LightSpecular;
uniform float u_LightIntensity;

varying vec3 v_WorldPosition;
varying vec3 v_WorldNormal;
varying vec4 v_Color;

void main() {
    // Initialize variables
    vec3 baseColor = vec3(v_Color);
    vec3 specularColor = vec3(0.0);
    float materialShininess = 0.0;
    float materialSpecular = 0.1;
    // Get the light's direction vector from the light's position and turn
    // the direction vectors into unit vectors so that both the normal and
    // light vectors have a length of 1
    vec3 normalDirection = normalize(v_WorldNormal);
    vec3 lightDirection = normalize(-u_LightPosition);
    // Get the dot product of the two vectors. When the fragment fully points toward
    // the light, the dot product will be -1. Negating the dot product will make
    // easier the next calculations
    float diffuseIntensity = clamp(-dot(lightDirection, normalDirection), 0.0, 1.0);
    // Multiply the color by the diffuse intensity to get diffuse shading
    vec3 diffuseColor = u_LightColor * baseColor * diffuseIntensity;
    // Compute the reflection
    if (diffuseIntensity > 0.0) {
        // Reflect vector
        vec3 reflection = reflect(lightDirection, normalDirection);
        // View vector
        vec3 cameraDirection = normalize(v_WorldPosition - u_CameraPosition);
        // Find the angle between reflection and view
        float specularIntensity = pow(clamp(-dot(reflection, cameraDirection), 0.0, 1.0), materialShininess);
        specularColor += u_LightSpecular * specularIntensity * materialSpecular;
    }
    // Multiply by baseColor intead of the light's color to make it a little bit brighter
    vec3 ambientColor = baseColor * u_LightIntensity;
    // Set the final color
    vec3 res = diffuseColor + ambientColor + specularColor;
    gl_FragColor = vec4(res, v_Color[3]);
}