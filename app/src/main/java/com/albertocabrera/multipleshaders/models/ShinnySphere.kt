package com.albertocabrera.multipleshaders.models

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

import android.content.Context
import android.opengl.GLES20.*
import android.opengl.Matrix
import android.util.Log
import com.albertocabrera.multipleshaders.R
import com.albertocabrera.multipleshaders.utils.*
import java.nio.FloatBuffer
import kotlin.math.cos
import kotlin.math.sin

class ShinnySphere(context: Context,
                   resolution: Int, radius: Float,
                   rgb: FloatArray, alpha: Float,
                   private val pos: FloatArray): Model3D() {

    private val stacks = resolution
    private val slices = resolution

    private var vIdx = 0
    private var cIdx = 0
    private var tIdx = 0

    // slices * 2 takes into account the fact that two triangles are needed for each face bound by
    // the slice and stack borders. The +2 handles the fact that the first two vertices are also the
    // last vertices, so are duplicated
    private val vertices = FloatArray(POSITION_COUNT_3D * (slices * 2 + 2) * stacks)
    private val colors = FloatArray(COLOR_COUNT * (slices * 2 + 2) * stacks)
    private val normals = FloatArray(POSITION_COUNT_3D * (slices * 2 + 2) * stacks)

    // Buffers to contain data
    private val vertexBuffer: FloatBuffer
    private val colorBuffer: FloatBuffer
    private val normalBuffer: FloatBuffer

    // Default shader variables
    private var program = 0
    private var uProjectionMatrixLocation = -1
    private var uViewMatrixLocation = -1
    private var uModelMatrixLocation = -1
    private var uNormalMatrixLocation = -1
    private var aPositionLocation = -1
    private var aColorLocation = -1
    private var aNormalLocation = -1

    // Extra variables to compute lighting
    private val normalMatrix = FloatArray(9)
    private var uCameraPositionLocation = -1
    private var uLightPositionLocation = -1
    private var uLightColorLocation = -1
    private var uLightIntensityLocation = -1
    private var uLightSpecularLocation = -1

    init {

        // Precompute all circumference positions
        val circle = FloatArray(slices * POSITION_COUNT_2D)
        generateCircumference(circle, slices)

        // The outer loop, going from south to north
        for (phiIdx in 0 until stacks) {

            // The first circle
            val phi0 = PI_F * ( (phiIdx + 0) * (1f / stacks) - 0.5f )

            // The second circle
            val phi1 = PI_F * ( (phiIdx + 1) * (1f / stacks) - 0.5f )

            // Pre-calculated values
            val c0 = cos(phi0)
            val s0 = sin(phi0)
            val c1 = cos(phi1)
            val s1 = sin(phi1)

            // The inner loop, going through the whole circumference
            tIdx = 0
            for (thetaIdx in 0 until slices) {

                // First point, xyz coordinates
                vertices[vIdx + 0] = radius * c0 * circle[tIdx + 0]
                vertices[vIdx + 1] = radius * s0
                vertices[vIdx + 2] = radius * c0 * circle[tIdx + 1]

                // Second point, xyz coordinates
                vertices[vIdx + 3] = radius * c1 * circle[tIdx + 0]
                vertices[vIdx + 4] = radius * s1
                vertices[vIdx + 5] = radius * c1 * circle[tIdx + 1]

                // Bot points are the same color
                colors[cIdx + 0] = rgb[0]
                colors[cIdx + 1] = rgb[1]
                colors[cIdx + 2] = rgb[2]
                colors[cIdx + 3] = alpha
                colors[cIdx + 4] = rgb[0]
                colors[cIdx + 5] = rgb[1]
                colors[cIdx + 6] = rgb[2]
                colors[cIdx + 7] = alpha

                // Normalize every point for the lighting effect
                normals[vIdx + 0] = c0 * circle[tIdx + 0]
                normals[vIdx + 1] = s0
                normals[vIdx + 2] = c0 * circle[tIdx + 1]
                normals[vIdx + 3] = c1 * circle[tIdx + 0]
                normals[vIdx + 4] = s1
                normals[vIdx + 5] = c1 * circle[tIdx + 1]

                // Increments proportional to the number of components
                vIdx += 2 * POSITION_COUNT_3D
                cIdx += 2 * COLOR_COUNT
                tIdx += POSITION_COUNT_2D

            }

            // Create a degenerate triangle to connect stacks
            vertices[vIdx + 3] = vertices[vIdx - 3]
            vertices[vIdx + 0] = vertices[vIdx + 3]
            vertices[vIdx + 4] = vertices[vIdx - 2]
            vertices[vIdx + 1] = vertices[vIdx + 4]
            vertices[vIdx + 5] = vertices[vIdx - 1]
            vertices[vIdx + 2] = vertices[vIdx + 5]

        }

        // The vertex, color and normal data are converted to byte arrays that OpenGL can use
        vertexBuffer = makeFloatBuffer(vertices)
        colorBuffer = makeFloatBuffer(colors)
        normalBuffer = makeFloatBuffer(normals)

        // Initialize graphic program
        val vertexResource = R.raw.shinny_vertex_shader
        val vertexCode = readTextFileFromResource(context, vertexResource)
        val fragmentResource = R.raw.shinny_fragment_shader
        val fragmentCode = readTextFileFromResource(context, fragmentResource)
        program = createProgram(vertexCode, fragmentCode)
        Log.v(SHADER_TAG, "Program nr. for ShinnySphere: $program")
        validateProgram(program)

    }

    override fun setMovement(rotate: FloatArray, modelMatrix: FloatArray) {

        // Set initial position
        Matrix.setIdentityM(super.modelMatrix, 0)
        Matrix.translateM(super.modelMatrix, 0, pos[0], pos[1], pos[2])

        // Pass total movement to the model
        Matrix.multiplyMM(modelMatrix, 0, rotate, 0, super.modelMatrix, 0)

    }

    override fun useProgram() {
        glUseProgram(program)
    }

    private fun getLocations() {

        // Get location of the uniforms
        uProjectionMatrixLocation = glGetUniformLocation(program, U_PROJECTION_MATRIX)
        uViewMatrixLocation = glGetUniformLocation(program, U_VIEW_MATRIX)
        uModelMatrixLocation = glGetUniformLocation(program, U_MODEL_MATRIX)

        // Get location of the attributes
        aPositionLocation = glGetAttribLocation(program, A_POSITION)
        aColorLocation = glGetAttribLocation(program, A_COLOR)
        aNormalLocation = glGetAttribLocation(program, A_NORMAL)

    }

    override fun bindData(cameraPosition: FloatArray, light: Light) {

        // Get locations
        getLocations()

        // Define lighting
        setLightBuffers(cameraPosition, light)

        // Positions
        glVertexAttribPointer(
            aPositionLocation, POSITION_COUNT_3D,
            GL_FLOAT, false, POSITION_STRIDE_3D, vertexBuffer
        )
        glEnableVertexAttribArray(aPositionLocation)

        // Colors
        glVertexAttribPointer(
            aColorLocation, COLOR_COUNT,
            GL_FLOAT, false, COLOR_STRIDE, colorBuffer
        )
        glEnableVertexAttribArray(aColorLocation)

        // Normal vectors
        glVertexAttribPointer(
            aNormalLocation, POSITION_COUNT_3D,
            GL_FLOAT, false, POSITION_STRIDE_3D, normalBuffer
        )
        glEnableVertexAttribArray(aNormalLocation)

    }

    private fun setLightBuffers(cameraPosition: FloatArray, light: Light) {
        // Set location of the normal uniform
        uNormalMatrixLocation = glGetUniformLocation(program, U_NORMAL_MATRIX)

        // Set up the camera
        uCameraPositionLocation = glGetUniformLocation(program, U_CAMERA_POSITION)
        glUniform3fv(uCameraPositionLocation, 1, cameraPosition, 0)

        // Set location of the lighting uniforms
        uLightPositionLocation = glGetUniformLocation(program, U_LIGHT_POSITION)
        uLightColorLocation = glGetUniformLocation(program, U_LIGHT_COLOR)
        uLightSpecularLocation = glGetUniformLocation(program, U_LIGHT_SPECULAR)
        uLightIntensityLocation = glGetUniformLocation(program, U_LIGHT_INTENSITY)

        // Send the lights to the shader
        glUniform3fv(uLightPositionLocation, 1, light.position, 0)
        glUniform3fv(uLightColorLocation, 1, light.color, 0)
        glUniform3fv(uLightSpecularLocation, 1, light.specular, 0)
        glUniform1f(uLightIntensityLocation, light.intensity)
    }

    // Set fragment matrices
    override fun fragmentUniforms(projectionMatrix: FloatArray, viewMatrix:FloatArray, modelMatrix: FloatArray) {
        glUniformMatrix4fv(uProjectionMatrixLocation, 1, false, projectionMatrix, 0)
        glUniformMatrix4fv(uViewMatrixLocation, 1, false, viewMatrix, 0)
        glUniformMatrix4fv(uModelMatrixLocation, 1, false, modelMatrix, 0)
    }

    // Do the actual drawing
    override fun drawPrimitives(modelMatrix: FloatArray) {

        // Get normal matrix
        upperLeft(modelMatrix, normalMatrix)
        glUniformMatrix3fv(uNormalMatrixLocation, 1, false, normalMatrix, 0)

        // Do the actual drawing
        glDrawArrays(GL_TRIANGLE_STRIP, 0, (slices + 1) * 2 * (stacks - 1) + 2)

    }

}