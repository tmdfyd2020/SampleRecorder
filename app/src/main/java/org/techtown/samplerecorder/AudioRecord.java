package org.techtown.samplerecorder;

import android.media.AudioFormat;
import android.media.MediaRecorder;

import java.nio.ShortBuffer;

public class AudioRecord {

    final static MainActivity mainActivity = new MainActivity();
    public static Queue myQueue;

    private final int AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;
    private final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    private android.media.AudioRecord audioRecord = null;
    private Thread recordThread = null;
    private ShortBuffer shortBuffer = null;
    private short[] audioData = null;
    private int capacity_buffer, record_bufferSize, len_audioData;

    public void init() {
        myLog.d("");

        capacity_buffer = mainActivity.SamplingRate * 100;

        record_bufferSize = android.media.AudioRecord.getMinBufferSize(
                mainActivity.SamplingRate,
                CHANNEL_CONFIG,
                AUDIO_FORMAT
        );

        myLog.d("record_bufferSize = " + String.valueOf(record_bufferSize));

        myQueue = new Queue();
    }

    public void start() {
        myLog.d("");

        audioData = new short[record_bufferSize];

        if(audioRecord == null) {
            audioRecord = new android.media.AudioRecord(
                    AUDIO_SOURCE,
                    mainActivity.SamplingRate,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    audioData.length
            );
        }

        audioRecord.startRecording();

        recordThread = new Thread(new Runnable() {
            @Override
            public void run() {

                shortBuffer = ShortBuffer.allocate(capacity_buffer);
                shortBuffer.rewind();

                len_audioData = 0;
                while(mainActivity.isRecording) {
                    len_audioData = audioRecord.read(audioData, 0, record_bufferSize);  // audioRecord -> audioData
                    shortBuffer.put(audioData, 0, len_audioData);  // audioData -> shortBuffer
                    // myQueue.enqueue(shortBuffer);  // shortBuffer -> queue
                }
                myQueue.enqueue(shortBuffer);  // shortBuffer -> queue
            }
        });
        recordThread.start();
    }

    public void stop() {
        myLog.d("");

        if (audioRecord != null && audioRecord.getState() != android.media.AudioTrack.STATE_UNINITIALIZED) {
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
        myLog.d("");

        audioData = null;
        shortBuffer = null;

        myQueue = new Queue();
    }
}