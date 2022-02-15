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
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class LowPolySphere(context: Context,
                    resolution: Int, radius: Float, rgb: FloatArray,
                    private val pos: FloatArray, lightDir: FloatArray): Model3D() {

    private val stacks = resolution
    private val slices = resolution

    private var vIdx = 0
    private var cIdx = 0
    private var tIdx = 0

    private var colorIncrement = 0f

    private val temp = FloatArray(16)
    private val lightRot = FloatArray(16)

    // slices * 2 takes into account the fact that two triangles are needed for each face bound by
    // the slice and stack borders. The +2 handles the fact that the first two vertices are also the
    // last vertices, so are duplicated
    private val vertices = FloatArray(POSITION_COUNT_3D * (slices * 2 + 2) * stacks)
    private val colors = FloatArray(COLOR_COUNT * (slices * 2 + 2) * stacks)

    // Buffers to contain data
    private val vertexBuffer: FloatBuffer
    private val colorBuffer: FloatBuffer

    // Default shader variables
    private var program = 0
    private var uProjectionMatrixLocation = -1
    private var uViewMatrixLocation = -1
    private var uModelMatrixLocation = -1
    private var aPositionLocation = -1
    private var aColorLocation = -1

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
                colors[cIdx + 0] = rgb[0] * colorIncrement
                colors[cIdx + 1] = rgb[1] * colorIncrement
                colors[cIdx + 2] = rgb[2] * colorIncrement
                colors[cIdx + 3] = 1f
                colors[cIdx + 4] = rgb[0] * colorIncrement
                colors[cIdx + 5] = rgb[1] * colorIncrement
                colors[cIdx + 6] = rgb[2] * colorIncrement
                colors[cIdx + 7] = 1f

                // Increments proportional to the number of components
                vIdx += 2 * POSITION_COUNT_3D
                cIdx += 2 * COLOR_COUNT
                tIdx += POSITION_COUNT_2D

            }

            // Increment in the color
            colorIncrement += 1f / stacks

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

        // Compute the rotation needed to match light's direction
        val (ux, uz, theta) = rotationInfo(lightDir)
        Matrix.setIdentityM(lightRot, 0)
        Matrix.rotateM(lightRot, 0, theta, ux, 0f, uz)

        // Initialize graphic program
        val vertexResource = R.raw.simple_vertex_shader
        val vertexCode = readTextFileFromResource(context, vertexResource)
        val fragmentResource = R.raw.simple_fragment_shader
        val fragmentCode = readTextFileFromResource(context, fragmentResource)
        program = createProgram(vertexCode, fragmentCode)

        Log.v(SHADER_TAG, "Program nr. for LowPolySphere: $program")
        validateProgram(program)

    }

    private fun rotationInfo(dir: FloatArray): FloatArray {

        // Cross product [0, 1, 0] x dir. The Y axis is always zero, so both elements
        // are X and Z coordinates
        val cross = floatArrayOf(dir[2], -dir[0])

        // Normal
        val norm = sqrt(cross[0]*cross[0] + cross[1]*cross[1] )
        val normDir = sqrt(dir[0]*dir[0] + dir[1]*dir[1] + dir[2]*dir[2])

        // Unitary vector to set the rotation
        val unitary = floatArrayOf(cross[0]/norm, cross[1]/norm)

        // Angle of rotation
        val theta = acos(dir[1] / normDir) * RADIANS_TO_DEGREES

        return floatArrayOf(unitary[0], unitary[1], theta)

    }

    override fun setMovement(rotate: FloatArray, modelMatrix: FloatArray) {
        Matrix.setIdentityM(super.modelMatrix, 0)

        // Set initial position
        Matrix.translateM(super.modelMatrix, 0, pos[0], pos[1], pos[2])

        // Pass total movement to the model
        Matrix.multiplyMM(temp, 0, rotate, 0, super.modelMatrix, 0)

        // Set rotation to match light's direction. First, go back to the origin, then rotate
        // and finally move again to the previous position
        val prev = floatArrayOf(temp[12], temp[13], temp[14])
        temp[12] = 0f ; temp[13] = 0f ; temp[14] = 0f
        Matrix.multiplyMM(modelMatrix, 0, lightRot, 0, temp, 0)
        modelMatrix[12] = prev[0] ; modelMatrix[13] = prev[1] ; modelMatrix[14] = prev[2]
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

    }

    override fun bindData(cameraPosition: FloatArray, light: Light) {

        // Get locations
        getLocations()

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

    }

    // Set fragment matrices
    override fun fragmentUniforms(projectionMatrix: FloatArray, viewMatrix:FloatArray, modelMatrix: FloatArray) {
        glUniformMatrix4fv(uProjectionMatrixLocation, 1, false, projectionMatrix, 0)
        glUniformMatrix4fv(uViewMatrixLocation, 1, false, viewMatrix, 0)
        glUniformMatrix4fv(uModelMatrixLocation, 1, false, modelMatrix, 0)
    }

    // Do the actual drawing
    override fun drawPrimitives(modelMatrix: FloatArray) {

        // Do the actual drawing
        glDrawArrays(GL_TRIANGLE_STRIP, 0, (slices + 1) * 2 * (stacks - 1) + 2)


    }

}