package org.techtown.samplerecorder;

import java.io.File;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.LinkedList;

public class Queue {

    java.util.Queue<ShortBuffer> queue;

    public Queue() {
        myLog.d("constructor activate");

        queue = null;
        queue = new LinkedList<ShortBuffer>();
    }

    public void enqueue(ShortBuffer shortBuffer) {
        myLog.d("method activate");

        queue.add(shortBuffer);
    }

    public ShortBuffer dequeue() {
        myLog.d("method activate");

        return queue.peek();
    }

}
