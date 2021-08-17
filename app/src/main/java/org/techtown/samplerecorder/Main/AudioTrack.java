package org.techtown.samplerecorder.Main;

import android.media.AudioAttributes;
import android.media.AudioFormat;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class AudioTrack {

    public static int dataMax;

    private android.media.AudioTrack audioTrack = null;
    private Thread playThread = null;
    private byte[] audioData = null;
    private int track_bufferSize;

    public void init(int bufferSize) {
//        myLog.d("method activate");

        track_bufferSize = bufferSize;
        audioData = new byte[track_bufferSize];
    }

    public void play(int type, int channel, int sampleRate, Queue queue) {
        myLog.d("method activate");

        if (audioTrack == null) {
            audioTrack = new android.media.AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(type)
                            .build())
                    .setAudioFormat(new AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(channel)
                            .build())
                    .setBufferSizeInBytes(track_bufferSize * 2)
                    .build();
        }

        playThread = new Thread(new Runnable() {
            @Override
            public void run() {

                audioTrack.play();

                while (MainActivity.isPlaying) {

                    if (queue.isEmpty()) {
                        queue.copy();
                        MainActivity.autoStop = true;
                        break;
                    }

                    audioData = queue.dequeue();
                    audioTrack.write(audioData, 0, track_bufferSize);

                    // using draw waveform in MainActivity
                    dataMax = 0;
                    for (int i = 0; i < audioData.length; i++) {
                        ByteBuffer buffer = ByteBuffer.wrap(audioData);
                        buffer.order(ByteOrder.LITTLE_ENDIAN);
                        dataMax = 10 * Math.abs(buffer.getShort());
                    }
                }

            }

        });

        playThread.start();
    }

    public void stop() {
//        myLog.d("method activate");

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
//        myLog.d("method activate");
    }
}
