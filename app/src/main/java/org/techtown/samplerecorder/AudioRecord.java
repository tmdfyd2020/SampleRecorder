package org.techtown.samplerecorder;

import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.util.Log;

import java.nio.ShortBuffer;

public class AudioRecord {

    final static MainActivity mainActivity = new MainActivity();
    public static Queue myQueue;

    private android.media.AudioRecord audioRecord = null;
    private Thread recordThread = null;
    int retBufferSize;
    short[] record_to_short;
    int record_bufferSize;

    int BufferShortSize; // 저장될 버퍼의 크기 -> 늘리니까 늘어남
    ShortBuffer shortBuffer;

    public void init() {
        android.util.Log.d("[Main]", "AudioRecord init()");

        //isRecording = mainActivity.isRecording;
        //SamplingRate = mainActivity.SamplingRate;
        // SamplingRate = ((MainActivity)MainActivity.context).SamplingRate;

        BufferShortSize =  mainActivity.SamplingRate * 10;
        shortBuffer = ShortBuffer.allocate(BufferShortSize);

        record_bufferSize = android.media.AudioRecord.getMinBufferSize(mainActivity.SamplingRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT) * 2;

        myQueue = new Queue();  // 뭔가 새로운 큐가 아니라 서로 같은 큐여야 될 것 같다.

    }

    public void start() {
        android.util.Log.d("[Main]", "AudioRecord start()");
        android.util.Log.d("[Main]", "[AudioRecord][start()] isRecording : " + String.valueOf(mainActivity.isRecording));

        record_to_short = null;
        shortBuffer = null;

        // shortBuffer = ShortBuffer.allocate(BufferShortSize);
        record_to_short = new short[record_bufferSize];  // error : NegativeArraySizeException: -4

        if(audioRecord == null) {
            audioRecord = new android.media.AudioRecord(MediaRecorder.AudioSource.MIC,
                    mainActivity.SamplingRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    record_to_short.length);
        }

        retBufferSize = 0;
        audioRecord.startRecording();

        recordThread = new Thread(new Runnable() {
            @Override
            public void run() {

                shortBuffer = ShortBuffer.allocate(BufferShortSize);
                shortBuffer.rewind();

                android.util.Log.d("[Main]", "[AudioRecord][start()][Thread] isRecording : " + String.valueOf(mainActivity.isRecording));

                // TODO : 첫 번째 에러 : Record 누르고 정지를 누르면 튕기면서 read에 null이 뜸 -> while 조건문에서 걸린다?!
                while(mainActivity.isRecording) {  // BufferRecord = short[], BufferRecordSize = minSize
                    retBufferSize = audioRecord.read(record_to_short, 0, record_bufferSize);
                    shortBuffer.put(record_to_short, 0, retBufferSize);
                    // myQueue.enqueue(shortBuffer);
                    android.util.Log.d("[Main]", "Recorder in while");
                    // myQueue.enqueue(shortBuffer);
                    //audioRecord.read(record_to_short, 0, record_bufferSize);
                    //shortBuffer.put(record_to_short, 0, record_bufferSize);
                    // shortBuffer에 record_to_short에 저장된 음성 녹음 데이터가 저장된다.
                }
                myQueue.enqueue(shortBuffer);
            }
        });

        recordThread.start();
    }

    public void stop() {
        android.util.Log.d("[Main]", "AudioRecord stop()");
        audioRecord.stop();
        audioRecord.release();
        audioRecord = null;
        recordThread = null;
    }

    public void release() {
        Log.d("[Main]", "AudioRecord release()");
        audioRecord = null;
        recordThread = null;

        myQueue = new Queue();
    }
}