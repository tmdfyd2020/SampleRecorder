package org.techtown.samplerecorder;

import android.media.AudioFormat;
import android.media.MediaRecorder;

import java.nio.ShortBuffer;

public class AudioRecord {

    private final int AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;
    private final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    public static Queue queue;

    private android.media.AudioRecord audioRecord = null;
    private Thread recordThread = null;
    private ShortBuffer shortBuffer = null;
    private short[] audioData = null;
    private int capacity_buffer, record_bufferSize, len_audioData;

    public void init() {
        myLog.d("method activate");

        capacity_buffer = MainActivity.SampleRate * 60;  // stored buffer size (60s)
        shortBuffer = ShortBuffer.allocate(capacity_buffer);

        record_bufferSize = android.media.AudioRecord.getMinBufferSize(  // recorded buffer size
                MainActivity.SampleRate,
                CHANNEL_CONFIG,
                AUDIO_FORMAT
        );
        audioData = new short[record_bufferSize];

        queue = new Queue();
    }

    public void start() {
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

                shortBuffer.rewind();
                myLog.d("Recording isRecording >> " + String.valueOf(MainActivity.isRecording));

                len_audioData = 0;
                while(MainActivity.isRecording) {
                    len_audioData = audioRecord.read(audioData, 0, record_bufferSize);  // audioRecord -> audioData
                    shortBuffer.put(audioData, 0, len_audioData);  // audioData -> shortBuffer
                    queue.enqueue(shortBuffer);  // shortBuffer -> queue
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
        shortBuffer = null;

        queue = new Queue();
    }
}