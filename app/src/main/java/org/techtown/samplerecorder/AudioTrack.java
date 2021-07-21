package org.techtown.samplerecorder;

import android.media.AudioFormat;
import android.media.AudioManager;

import java.nio.ShortBuffer;

public class AudioTrack {

    private final int STREAM_TYPE = AudioManager.STREAM_MUSIC;
    private final int CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO;
    private final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private final int MODE = android.media.AudioTrack.MODE_STREAM;

    private Queue queue_fromRecord;

    private android.media.AudioTrack audioTrack = null;
    private Thread playThread = null;
    private ShortBuffer shortBuffer;
    private short[] audioData;
    private int capacity_buffer, track_bufferSize, len_audioData;

    public void init() {
        myLog.d("method activate");

        capacity_buffer = MainActivity.SampleRate * 60;  // stored buffer size (60s)
        shortBuffer = ShortBuffer.allocate(capacity_buffer);

        track_bufferSize = android.media.AudioTrack.getMinBufferSize(  // recorded buffer size
                MainActivity.SampleRate,
                CHANNEL_CONFIG,
                AUDIO_FORMAT
        );

        audioData = new short[track_bufferSize];
        len_audioData = audioData.length;

        queue_fromRecord = AudioRecord.queue;
    }

    public void play() {
        myLog.d("method activate");
        myLog.d("Playing Sample Rate : " + String.valueOf(MainActivity.SampleRate));

        if (audioTrack == null) {
            audioTrack = new android.media.AudioTrack(
                    STREAM_TYPE,
                    MainActivity.SampleRate,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    audioData.length,
                    MODE
            );
        }

        playThread = new Thread(new Runnable() {
            @Override
            public void run() {

                audioTrack.play();

                shortBuffer = queue_fromRecord.dequeue();  // queue -> shortBuffer
                shortBuffer.position(0);

                while (MainActivity.isPlaying) {
                    shortBuffer.get(audioData, 0, len_audioData);  // shortBuffer -> audioData
                    audioTrack.write(audioData, 0, len_audioData);  // audioData -> audioTrack
                }
            }

        });
        playThread.start();
    }

    public void stop() {
        myLog.d("method activate");

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
        myLog.d("method activate");

        audioData = null;
        shortBuffer = null;

        queue_fromRecord = AudioRecord.queue;
    }  // --> Go to MainActivity
}
