package org.techtown.samplerecorder;

import android.media.AudioFormat;
import android.media.AudioManager;

import java.nio.ShortBuffer;

public class AudioTrack {

    private final int CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO;
    private final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private final int MODE = android.media.AudioTrack.MODE_STREAM;

    private Queue queue_fromRecord;

    private android.media.AudioTrack audioTrack = null;
    private Thread playThread = null;
    private ShortBuffer shortBuffer = null;
    private short[] audioData = null;
    private int capacity_buffer, track_bufferSize, len_audioData;

    public void init(int sampleRate) {
//        myLog.d("method activate");

        capacity_buffer = sampleRate * 60;  // stored buffer size (60s)
        shortBuffer = ShortBuffer.allocate(capacity_buffer);

        track_bufferSize = android.media.AudioTrack.getMinBufferSize(  // recorded buffer size
                sampleRate,
                CHANNEL_CONFIG,
                AUDIO_FORMAT ) * 2;
        audioData = new short[track_bufferSize];
        len_audioData = audioData.length;

        queue_fromRecord = AudioRecord.queue;
    }

    public void play(int type, int channel, int sampleRate) {
//        myLog.d("method activate");
//        myLog.d("Playing Sample Rate : " + String.valueOf(sampleRate));

        if (audioTrack == null) {
            audioTrack = new android.media.AudioTrack(
                    type,
                    sampleRate,
                    channel,
                    AUDIO_FORMAT,
                    audioData.length,
                    MODE
            );
        }

        playThread = new Thread(new Runnable() {
            @Override
            public void run() {

                audioTrack.play();

                shortBuffer = queue_fromRecord.dequeue();
                shortBuffer.position(0);

                int len_write;
                while(MainActivity.isPlaying) {
                    shortBuffer.get(audioData, 0, len_audioData);  // shortBuffer -> audioData
                    len_write = audioTrack.write(audioData, 0, len_audioData);  // audioData -> audioTrack


                    if (len_write == len_audioData && audioData[audioData.length - 1] == AudioRecord.index) {
                        MainActivity.autoStop = true;
                        break;
                    }
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
    }
}
