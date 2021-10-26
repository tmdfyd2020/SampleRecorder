package org.techtown.samplerecorder.Main;

import java.util.LinkedList;

public class Queue {

    private final String TAG = this.getClass().getSimpleName();

    java.util.Queue<byte[]> queue;
    java.util.Queue<byte[]> store;

    public Queue() {
        queue = null;
        queue = new LinkedList<byte[]>();
        store = new LinkedList<byte[]>();
    }

    public void enqueue(byte[] data) {
        queue.offer(data);
        store.offer(data);
    }

    public byte[] dequeue() {
        return queue.poll();
    }

    public void copy() {
        queue.addAll(store);
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

}
