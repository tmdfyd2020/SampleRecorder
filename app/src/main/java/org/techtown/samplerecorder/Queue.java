package org.techtown.samplerecorder;

import java.io.File;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.LinkedList;

public class Queue {

    java.util.Queue<ShortBuffer> queue;
    java.util.Queue<File> queue_file;

    public Queue() {
        myLog.d("constructor activate");

        queue = null;
        queue = new LinkedList<ShortBuffer>();
        queue_file = new LinkedList<File>();

    }

    public void enqueue(ShortBuffer shortBuffer) {
        myLog.d("method activate");

        queue.add(shortBuffer);
    }

    public void enqueue_file(File file) {
        myLog.d("method activate");

        queue_file.add(file);
    }

    public ShortBuffer dequeue() {
        myLog.d("method activate");

        return queue.peek();
    }

    public File dequeue_file() {
        myLog.d("method activate");

        return queue_file.peek();
    }
}
