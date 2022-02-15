package com.albertocabrera.multipleshaders.utils

import android.content.Context
import android.content.res.Resources
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import android.opengl.GLES20.*
import android.util.Log
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.RuntimeException

private const val OPENGL_ZERO = 0
private const val TAG = SHADER_TAG

fun makeFloatBuffer(arr: FloatArray): FloatBuffer {
    val bb = ByteBuffer.allocateDirect(arr.size * BYTES_PER_FLOAT)
    bb.order(ByteOrder.nativeOrder())
    val fb = bb.asFloatBuffer()
    fb.put(arr)
    fb.position(0)
    return fb
}

fun readTextFileFromResource(context: Context, resourceId: Int): String {

    val body = StringBuilder()

    try {

        val inputStream: InputStream = context.resources.openRawResource(resourceId)
        val inputStreamReader = InputStreamReader(inputStream)
        val bufferedReader = BufferedReader(inputStreamReader)

        var nextLine = bufferedReader.readLine()
        while (nextLine != null) {
            body.append(nextLine)
            body.append("\n")
            nextLine = bufferedReader.readLine()
        }

    } catch (e: IOException) {
        throw RuntimeException("Could not open resource: $resourceId", e)
    } catch (nfe: Resources.NotFoundException) {
        throw RuntimeException("Resource not found: $resourceId", nfe)
    }

    return body.toString()

}

fun createProgram(vertexResource: String, fragmentResource: String): Int {

    // Load both programs and return their respective handles
    val vertexShader = loadShader(GL_VERTEX_SHADER, vertexResource)
    val fragmentShader = loadShader(GL_FRAGMENT_SHADER, fragmentResource)

    // Check if any errors occurred
    if (vertexShader == OPENGL_ZERO || fragmentShader == OPENGL_ZERO) {
        return OPENGL_ZERO
    }

    // Creates an empty program object to host the two shaders
    val program = glCreateProgram()

    if (program != OPENGL_ZERO) {

        // Attach both shaders to the program
        glAttachShader(program, vertexShader)
        glAttachShader(program, fragmentShader)

        // Link the program to OpenGL pipeline
        glLinkProgram(program)

        // Check for any possible errors
        val linkStatus = IntArray(1)
        glGetProgramiv(program, GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] != GL_TRUE) {
            Log.e(TAG, "Could not link program:")
            Log.e(TAG, glGetProgramInfoLog(program))
            glDeleteProgram(program)
            return OPENGL_ZERO
        }

    }

    return program

}

private fun loadShader(shaderType: Int, source: String): Int {

    val shader = glCreateShader(shaderType)
    if (shader != OPENGL_ZERO) {

        // Create and compile shader
        glShaderSource(shader, source)
        glCompileShader(shader)

        // Check for any errors
        val compiled = IntArray(1)
        glGetShaderiv(shader, GL_COMPILE_STATUS, compiled, 0)

        if (compiled[0] == OPENGL_ZERO) {
            Log.e(TAG, "Could not compile shader $shaderType:")
            Log.e(TAG, glGetShaderInfoLog(shader))
            glDeleteShader(shader)
            return OPENGL_ZERO
        }

    }

    return shader
}

fun validateProgram(program: Int): Boolean {

    // Do the actual validation
    glValidateProgram(program)

    // Check the status
    val validateStatus = IntArray(1)
    glGetProgramiv(program, GL_VALIDATE_STATUS, validateStatus, 0)

    val success = validateStatus[0] != OPENGL_ZERO
    Log.v(TAG, "Results of the validating program nr. $program: $success")
    Log.v(TAG, glGetProgramInfoLog(program))

    return success

}