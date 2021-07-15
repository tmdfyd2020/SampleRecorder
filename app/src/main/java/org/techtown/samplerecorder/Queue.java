package org.techtown.samplerecorder;

import android.util.Log;

import java.nio.ShortBuffer;
import java.util.LinkedList;

public class Queue {

    java.util.Queue<ShortBuffer> queue;
    // public static Queue qcontext;

    public Queue() {
        android.util.Log.d("[Main]", "Queue creator()");
        queue = new LinkedList<ShortBuffer>();
        // queue = new ConcurrentLinkedQueue<ShortBuffer>();
        // qcontext = this;
    }

    public void enqueue(ShortBuffer shortBuffer) {
        android.util.Log.d("[Main]", "Queue enqueue()");
        queue.add(shortBuffer);
    }

    public ShortBuffer dequeue() {
        Log.d("[Main]", "Queue dequeue()");
        return queue.peek();
    }
}
