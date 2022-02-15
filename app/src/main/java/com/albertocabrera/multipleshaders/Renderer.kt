package com.albertocabrera.multipleshaders

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

import android.opengl.GLES20.*
import android.content.Context
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.albertocabrera.multipleshaders.models.*
import com.albertocabrera.multipleshaders.utils.BLUE
import com.albertocabrera.multipleshaders.utils.GREEN
import com.albertocabrera.multipleshaders.utils.RED
import com.albertocabrera.multipleshaders.utils.WHITE
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class Renderer(private val context: Context): GLSurfaceView.Renderer {

    // Renderer variables
    private var angle = 0f
    private val bgColor = WHITE
    private lateinit var cameraPosition: FloatArray

    // Uniform matrices
    private val modelMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)

    // Lighting
    private lateinit var sunLight: Light

    // Models to render
    private lateinit var shinny: ShinnySphere
    private lateinit var opaque: OpaqueSphere
    private lateinit var lowPoly: LowPolySphere
    private var models: MutableList<Model3D> = mutableListOf()

    override fun onSurfaceCreated(p0: GL10?, p1: EGLConfig?) {

        // z-Buffering
        glEnable(GL_DEPTH_TEST)
        glDepthMask(true)

        // Enable alpha blending
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        // Set background color
        glClearColor(bgColor[0], bgColor[1], bgColor[2], 1f)

        // Initialize camera. Since it won't be moving, it can be defined here. Else, it should
        // be defined inside onDrawFrame()
        cameraPosition = floatArrayOf(0f, 3f, 8f)
        Matrix.setLookAtM(
            viewMatrix, 0, cameraPosition[0], cameraPosition[1], cameraPosition[2],
            0f, 0f, 0f, 0f, 1f, 0f
        )

        // Initialize lighting
        sunLight = Light(
            floatArrayOf(2f, 4f, 2f),
            WHITE,
            floatArrayOf(0.6f, 0.6f, 0.6f),
            0.1f
        )

        // Define shinny sphere
        val shinnyPos = floatArrayOf(0f, 0f, -1.732f)
        shinny = ShinnySphere(context, 50, 1f, GREEN, 1f, shinnyPos)

        // Define opaque sphere
        val opaquePos = floatArrayOf(2f, 0f, 1.732f)
        opaque = OpaqueSphere(context, 50, 1f, BLUE, 1f, opaquePos)

        // Define low poly sphere
        val lowPolyPos = floatArrayOf(-2f, 0f, 1.732f)
        lowPoly = LowPolySphere(context, 10, 1f, RED, lowPolyPos, sunLight.position)

        // Append models
        models.add(shinny)
        models.add(opaque)
        models.add(lowPoly)

    }

    override fun onSurfaceChanged(p0: GL10?, width: Int, height: Int) {
        glViewport(0, 0, width, height)
        val ratio = width.toFloat() / height.toFloat()
        Matrix.perspectiveM(projectionMatrix, 0, 45f, ratio, 0.1f, 50f)
    }

    override fun onDrawFrame(p0: GL10?) {

        // Clear the rendering surface
        glEnable(GL_DEPTH_TEST)
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

        // Initialize matrices for movement
        val rotate = FloatArray(16)
        Matrix.setIdentityM(rotate, 0)

        // Define movements
        angle += 0.75f
        Matrix.rotateM(rotate, 0, angle, 0f, 1f, 0f)

        // Render all models
        for (model in models) {

            // Define movement
            model.setMovement(rotate, modelMatrix)

            // Draw
            model.useProgram()
            model.fragmentUniforms(projectionMatrix, viewMatrix, modelMatrix)
            model.bindData(cameraPosition, sunLight)
            model.drawPrimitives(modelMatrix)

        }

    }

}