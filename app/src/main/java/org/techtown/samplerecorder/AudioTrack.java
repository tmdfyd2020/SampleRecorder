package org.techtown.samplerecorder;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.util.Log;

import java.nio.ShortBuffer;

public class AudioTrack {

    short[] track_to_short;
    int BufferTrackSize, track_bufferSize;
    int SamplingRate;

    android.media.AudioTrack audioTrack = null;
    boolean isPlaying;
    Thread playThread = null;

    int BufferShortSize;
    ShortBuffer shortBuffer;

    final static MainActivity mainActivity = new MainActivity();

    Queue trackQueue;


    public AudioTrack() {

    }

    public void init() {
        android.util.Log.d("[Main]", "AudioTrack init()");

        BufferShortSize = mainActivity.SamplingRate * 10;
        shortBuffer = ShortBuffer.allocate(BufferShortSize);


        track_bufferSize = android.media.AudioTrack.getMinBufferSize(mainActivity.SamplingRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT) * 2;

        trackQueue = AudioRecord.myQueue;
    }

    public void play() {
        android.util.Log.d("[Main]", "AudioTrack play()");
        android.util.Log.d("[Main]", "[AudioTrack][play()] isPlaying : " + String.valueOf(mainActivity.isPlaying));
        track_to_short = new short[track_bufferSize];
        BufferTrackSize = track_to_short.length;

        audioTrack = new android.media.AudioTrack(AudioManager.STREAM_MUSIC,
                mainActivity.SamplingRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                track_to_short.length,
                android.media.AudioTrack.MODE_STREAM);

        playThread = new Thread(new Runnable() {
            @Override
            public void run() {

                // shortBuffer.position(0);
                // audioTrack.play();
                android.util.Log.d("[Main]", "[AudioTrack][play()][Thread] isPlaying : " + String.valueOf(mainActivity.isPlaying));

                shortBuffer = trackQueue.dequeue();
                shortBuffer.position(0);

                audioTrack.play();
                // TODO
                while (mainActivity.isPlaying) {
                    // shortBuffer = trackQueue.dequeue();
                    shortBuffer.get(track_to_short, 0, BufferTrackSize); // shortBuffer -> track_to_short
                    // error : Attempt to invoke virtual method 'java.nio.ShortBuffer java.nio.ShortBuffer.get(short[], int, int)' on a null object reference
                    audioTrack.write(track_to_short, 0, BufferTrackSize); // track_to_short -> audioTrack
                }
            }
        });
        playThread.start();
    }

    public void stop() {
        android.util.Log.d("[Main]", "Track stop()");
        if (audioTrack != null && audioTrack.getState() != android.media.AudioTrack.STATE_UNINITIALIZED) {
            if (audioTrack.getPlayState() != android.media.AudioTrack.PLAYSTATE_STOPPED) {
                try {
                    audioTrack.stop(); // 오디오 재생 종료

                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }
                audioTrack.release(); // 오디오 트랙이 잡은 모든 리소스를 해제시킨다.
                audioTrack = null;
                playThread = null;
            }
        }
    }

    public void release() {
        Log.d("[Main]", "Track release()");
        audioTrack = null;
        playThread = null;

        trackQueue = AudioRecord.myQueue;
    }

}
