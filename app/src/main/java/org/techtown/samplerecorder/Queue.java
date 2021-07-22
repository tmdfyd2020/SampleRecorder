package org.techtown.samplerecorder;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.LinkedList;

public class Queue {

    java.util.Queue<ByteBuffer> queue;

    public Queue() {
        myLog.d("constructor activate");

        queue = null;
        queue = new LinkedList<ByteBuffer>();
    }

    public void enqueue(ByteBuffer byteBuffer) {
        myLog.d("method activate");

        queue.add(byteBuffer);
    }

    public ByteBuffer dequeue() {
        myLog.d("method activate");

        return queue.peek();
    }
}
