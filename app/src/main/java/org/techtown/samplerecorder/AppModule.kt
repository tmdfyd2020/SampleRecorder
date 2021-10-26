package org.techtown.samplerecorder

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

object AppModule {

    fun dataToShort(byte: ByteArray?): Int {
        val buffer = ByteBuffer.wrap(byte!!)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        return abs(buffer.short.toInt()) * 10
    }
}