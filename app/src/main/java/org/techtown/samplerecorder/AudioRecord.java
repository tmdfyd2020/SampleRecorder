package org.techtown.samplerecorder;

import android.content.Context;
import android.media.AudioFormat;
import android.media.MediaRecorder;

import java.nio.ByteBuffer;

public class AudioRecord {

    private final int AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;
    private final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    public static Queue queue;

    private android.media.AudioRecord audioRecord = null;
    public Thread recordThread = null;
    private ByteBuffer byteBuffer = null;
    private byte[] audioData = null;
    private int capacity_buffer, record_bufferSize, len_audioData;

    public void init() {
        myLog.d("method activate");

        capacity_buffer = MainActivity.SampleRate * 60;  // stored buffer size (60s)
        byteBuffer = ByteBuffer.allocate(capacity_buffer);

        record_bufferSize = android.media.AudioRecord.getMinBufferSize(  // recorded buffer size
                MainActivity.SampleRate,
                CHANNEL_CONFIG,
                AUDIO_FORMAT
        ) * 2;

        audioData = new byte[record_bufferSize];
        queue = new Queue();
    }

    public void start(Context context) {
        myLog.d("method activate");
        myLog.d("Recording Sample Rate : " + String.valueOf(MainActivity.SampleRate));

        if(audioRecord == null) {
            audioRecord = new android.media.AudioRecord(
                    AUDIO_SOURCE,
                    MainActivity.SampleRate,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    audioData.length
            );
        }

        audioRecord.startRecording();

        recordThread = new Thread(new Runnable() {
            @Override
            public void run() {

                byteBuffer.rewind();
                myLog.d("Recording isRecording >> " + String.valueOf(MainActivity.isRecording));

                len_audioData = 0;
                while(MainActivity.isRecording) {
                    len_audioData = audioRecord.read(audioData, 0, record_bufferSize);  // audioRecord -> audioData
                    byteBuffer.put(audioData, 0, len_audioData);  // audioData -> byteBuffer
                    queue.enqueue(byteBuffer);  // byteBuffer -> queue
                }
                myLog.d("len_audioData Size >> " + String.valueOf(len_audioData));
            }
        });
        recordThread.start();
    }

    public void stop() {
        myLog.d("method activate");

        if (audioRecord != null) {
            if (audioRecord.getState() != android.media.AudioRecord.RECORDSTATE_STOPPED) {
                try {
                    audioRecord.stop();
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }

                audioRecord.release();
                audioRecord = null;
                recordThread = null;
            }
        }
    }

    public void release() {
        myLog.d("method activate");

        audioData = null;
        byteBuffer = null;

        queue = new Queue();
    }

    /*
    private byte[] short2byte(short[] sData) {

        int shortArrsize = sData.length;

        byte[] bytes = new byte[shortArrsize * 2];

        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            sData[i] = 0;
        }
        return bytes;
    }
    */

}