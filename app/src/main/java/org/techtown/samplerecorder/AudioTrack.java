package org.techtown.samplerecorder;

import android.media.AudioAttributes;
import android.media.AudioFormat;

import com.visualizer.amplitude.AudioRecordView;

import java.nio.ShortBuffer;

public class AudioTrack {

    private final int CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO;
    private final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private final int MODE = android.media.AudioTrack.MODE_STREAM;

    private android.media.AudioTrack audioTrack = null;
    private Thread playThread = null;
    private ShortBuffer shortBuffer = null;
    private short[] audioData = null;
    private int capacity_buffer, track_bufferSize, len_audioData, dataMax;
    int len_write;

    public void init(int sampleRate, int bufferSize) {
//        myLog.d("method activate");

        capacity_buffer = sampleRate * 60;  // stored buffer size (60s)
        shortBuffer = ShortBuffer.allocate(capacity_buffer);

        track_bufferSize = android.media.AudioTrack.getMinBufferSize(  // recorded buffer size
                sampleRate,
                CHANNEL_CONFIG,
                AUDIO_FORMAT);
        audioData = new short[bufferSize];
        len_audioData = audioData.length;
    }

    public void play(int type, int channel, int sampleRate, AudioRecordView view_play, Queue queue) {
//        myLog.d("method activate");

        view_play.recreate();

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

                len_write = 0;
                while (MainActivity.isPlaying) {
                    shortBuffer.get(audioData, 0, len_audioData);  // shortBuffer -> audioData
                    len_write = audioTrack.write(audioData, 0, len_audioData);  // audioData -> audioTrack

                    for (int i = 0; i < audioData.length; i++) {
                        dataMax = 0;
                        if (Math.abs(audioData[i]) >= dataMax) {
                            dataMax = Math.abs(audioData[i]);
                            //view_play.update(dataMax * 2);
                        }
                    }
                    view_play.update(dataMax * 2);
                    myLog.d("AudioRecord.index = " + String.valueOf(AudioRecord.index) + " || " + "audioData[audioData.length - 1] = " + String.valueOf(audioData[audioData.length - 1]));
                    // 여기가 문제였음 :: 녹음된 내용은 정상적으로 다 나오는데, if 문에 잘못 걸려서 계속 일찍 끝난 것이었음.
                    if (audioData[audioData.length - 1] == AudioRecord.index) {
                        // (audioData[audioData.length - 1] == AudioRecord.index)
                        // 첫 번째 조건은 계속 통과되다가 운 좋게 두 번째 조건 정수 값이 같으면 빠져나오는 것이었음
                        // 그래서 끝까지 간 적도 있고, 첫 부분부터 멈춘 적도 있는 것이었음 조건식을 다시 세워야 함.
                        // 근데 AudioRecord.index가 뭐임? -> 아무것도 설정되어있지 않았는데 왜 돌아갔던 거지??
                        // 녹음된 총 길이 비교를 어떻게 하지? 왜 계속 시작할 때부터 같다고 나오는거지?
                        // audioData가 연속해서 0값을 가지면 끝난 부분이잖아
                        // ## 이상하게 겹치는 부분이 없어 왜지? 분명 같은 short 배열인데 왜 같은 부분이 없지?
                        //       ## record와 track 의 버퍼 사이즈를 같게 하니까 된다!@!! 추가로, play waveform도 된다!!!!!!
                        // len_write는 왜 시작부터 읽은 만큼의 int 값이 아니라 최대 int 값을 출력하는걸까?
                        // 이제 끝부분
                        myLog.d("이건 될걸?");
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
