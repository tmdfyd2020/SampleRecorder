package org.techtown.samplerecorder.Main;

import java.util.LinkedList;

public class Queue {

    private final String TAG = this.getClass().getSimpleName();

    java.util.Queue<byte[]> queue;
    java.util.Queue<byte[]> store;

    public Queue() {
//        myLog.d("constructor activate");

        queue = null;
        queue = new LinkedList<byte[]>();
        store = new LinkedList<byte[]>();
    }

    public void enqueue(byte[] data) {
//        myLog.d("method activate");

        queue.offer(data);
        store.offer(data);
    }

    public byte[] dequeue() {
//        myLog.d("method activate");

        return queue.poll();
    }

    public void copy() {
//        myLog.d("method activate");
        
        queue.addAll(store);
    }


    public boolean isEmpty() {
//        myLog.d("method activate");

        return queue.isEmpty();
    }

}
