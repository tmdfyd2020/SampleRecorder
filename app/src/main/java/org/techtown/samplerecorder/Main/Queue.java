package org.techtown.samplerecorder.Main;

import java.util.LinkedList;

public class Queue {

    java.util.Queue<short[]> queue;
    java.util.Queue<short[]> store;

    public Queue() {
//        myLog.d("constructor activate");

        queue = null;
        queue = new LinkedList<short[]>();
        store = new LinkedList<short[]>();
    }

    public void enqueue(short[] data) {
//        myLog.d("method activate");

        queue.offer(data);
        store.offer(data);
    }

    public short[] dequeue() {
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
