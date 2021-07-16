package org.techtown.samplerecorder;

import android.media.AudioFormat;
import android.media.AudioManager;

import java.nio.ShortBuffer;

public class AudioTrack {

    final static MainActivity mainActivity = new MainActivity();
    private Queue Queue_fromRecord;

    private final int STREAM_TYPE = AudioManager.STREAM_MUSIC;
    private final int CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO;
    private final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private final int MODE = android.media.AudioTrack.MODE_STREAM;

    private android.media.AudioTrack audioTrack = null;
    private Thread playThread = null;
    private ShortBuffer shortBuffer;
    private short[] audioData;
    private int capacity_buffer, track_bufferSize, len_audioData;

    public void init() {
        myLog.d("");

        capacity_buffer = mainActivity.SamplingRate * 100;
        shortBuffer = ShortBuffer.allocate(capacity_buffer);

        track_bufferSize = android.media.AudioTrack.getMinBufferSize(
                mainActivity.SamplingRate,
                CHANNEL_CONFIG,
                AUDIO_FORMAT
        );

        myLog.d("track_bufferSize = " + String.valueOf(track_bufferSize));

        audioData = new short[track_bufferSize];
        len_audioData = audioData.length;

        Queue_fromRecord = AudioRecord.myQueue;
    }

    public void play() {
        myLog.d("");

        if (audioTrack == null) {
            audioTrack = new android.media.AudioTrack(
                    STREAM_TYPE,
                    mainActivity.SamplingRate,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    audioData.length,
                    MODE
            );
        }

        playThread = new Thread(new Runnable() {
            @Override
            public void run() {

                shortBuffer = Queue_fromRecord.dequeue();  // queue -> shortBuffer
                shortBuffer.position(0);

                audioTrack.play();

                while (mainActivity.isPlaying) {
                    shortBuffer.get(audioData, 0, len_audioData);  // shortBuffer -> audioData
                    audioTrack.write(audioData, 0, len_audioData);  // audioData -> audioTrack
                }
            }
        });
        playThread.start();
    }

    public void stop() {
        myLog.d("");

        if (audioTrack != null && audioTrack.getState() != android.media.AudioTrack.STATE_UNINITIALIZED) {
            if (audioTrack.getPlayState() != android.media.AudioTrack.PLAYSTATE_STOPPED) {
                try {
                    audioTrack.stop();

                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }

                audioTrack.release();
                audioTrack = null;

                playThread = null;
            }
        }
    }

    public void release() {
        myLog.d("");

        mainActivity.isPlaying = false;

        if (audioTrack != null && audioTrack.getState() != android.media.AudioTrack.STATE_UNINITIALIZED) {
            if (audioTrack.getPlayState() == android.media.AudioTrack.PLAYSTATE_PLAYING) {
                try {
                    audioTrack.stop();
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }
                audioTrack.release();
                audioTrack = null;

                playThread = null;
            }
        }

        Queue_fromRecord = AudioRecord.myQueue;
    }
}
