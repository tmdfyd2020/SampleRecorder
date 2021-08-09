package org.techtown.samplerecorder;

import android.media.AudioAttributes;
import android.media.AudioFormat;

import java.nio.ShortBuffer;

public class AudioTrack {

    public static int dataMax;

    private android.media.AudioTrack audioTrack = null;
    private Thread playThread = null;
    private ShortBuffer shortBuffer = null;
    private short[] audioData = null;
    private int capacity_buffer, len_audioData;

    public void init(int sampleRate, int bufferSize) {
//        myLog.d("method activate");

        capacity_buffer = sampleRate * 240;  // stored buffer size (60s)
        shortBuffer = ShortBuffer.allocate(capacity_buffer);

        audioData = new short[bufferSize];
        len_audioData = audioData.length;
    }

    public void play(int type, int channel, int sampleRate, Queue queue) {
//        myLog.d("method activate");

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
                    .setBufferSizeInBytes(audioData.length)
                    .build();
        }

        playThread = new Thread(new Runnable() {
            @Override
            public void run() {

                audioTrack.play();

                shortBuffer = queue.dequeue();
                shortBuffer.position(0);

                while (MainActivity.isPlaying) {
                    shortBuffer.get(audioData, 0, len_audioData);  // shortBuffer -> audioData
                    audioTrack.write(audioData, 0, len_audioData);  // audioData -> audioTrack

                    dataMax = 0;
                    for (int i = 0; i < audioData.length; i++) {
                        if (Math.abs(audioData[i]) >= dataMax) {
                            dataMax = Math.abs(audioData[i]);
                        }
                    }

                    if ((audioData[audioData.length - 1] == AudioRecord.lastData_1) && (audioData[audioData.length - 2] == AudioRecord.lastData_2)) {
                        MainActivity.autoStop = true;
                        break;
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
