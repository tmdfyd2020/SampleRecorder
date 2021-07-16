package org.techtown.samplerecorder;

import android.util.Log;

import java.nio.ShortBuffer;
import java.util.LinkedList;

public class Queue {

    java.util.Queue<ShortBuffer> queue;

    public Queue() {
        myLog.d("");

        queue = new LinkedList<ShortBuffer>();
    }

    public void enqueue(ShortBuffer shortBuffer) {
        myLog.d("");

        queue.add(shortBuffer);
    }

    public ShortBuffer dequeue() {
        myLog.d("");

        return queue.peek();
    }
}
