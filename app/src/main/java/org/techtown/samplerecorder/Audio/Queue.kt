package org.techtown.samplerecorder.Audio

import java.util.*
import java.util.Queue

class Queue {
    var queue: Queue<ByteArray>? = null
    var store: Queue<ByteArray>

    fun enqueue(data: ByteArray) {
        queue!!.offer(data)
        store.offer(data)
    }

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    fun dequeue(): ByteArray {
        return queue!!.poll()
    }

    fun copy() {
        queue!!.addAll(store)
    }

    val isEmpty: Boolean
        get() = queue!!.isEmpty()

    init {
        queue = LinkedList()
        store = LinkedList()
    }

    companion object {
        private const val TAG = "Queue"
    }
}