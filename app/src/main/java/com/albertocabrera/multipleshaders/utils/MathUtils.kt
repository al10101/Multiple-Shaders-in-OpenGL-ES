package com.albertocabrera.multipleshaders.utils

import kotlin.math.cos
import kotlin.math.sin

fun generateCircumference(arr: FloatArray, slices: Int) {

    // The 2D loop, going from 0 to 360, and defines the circle
    var tIdx = 0
    for (thetaIdx in 0 until slices) {

        // Increment along the longitude circle each "slice"
        val theta = -2f * PI_F * thetaIdx / (slices - 1)
        val c = cos(theta)
        val s = sin(theta)

        // Save circumference
        arr[tIdx + 0] = c
        arr[tIdx + 1] = s

        tIdx += POSITION_COUNT_2D

    }

}

fun upperLeft(inMatrix: FloatArray, outMatrix: FloatArray) {
    outMatrix[0] = inMatrix[0]
    outMatrix[1] = inMatrix[1]
    outMatrix[2] = inMatrix[2]

    outMatrix[3] = inMatrix[4]
    outMatrix[4] = inMatrix[5]
    outMatrix[5] = inMatrix[6]

    outMatrix[6] = inMatrix[8]
    outMatrix[7] = inMatrix[9]
    outMatrix[8] = inMatrix[10]
}