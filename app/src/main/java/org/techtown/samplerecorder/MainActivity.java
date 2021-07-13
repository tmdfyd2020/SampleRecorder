package org.techtown.samplerecorder;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

public class MainActivity extends AppCompatActivity {
    short[] record_to_short, track_to_short;
    int record_bufferSize, BufferTrackSize, track_bufferSize;
    int SamplingRate = 16000;

    AudioRecord audioRecord = null;
    boolean isRecording = false;
    Thread recordThread = null;
    int retBufferSize;

    AudioTrack audioTrack = null;
    boolean isPlaying = false;
    Thread playThread = null;

    Button btn_record, btn_play, btn_exit;
    ImageView img_recording;

    int BufferShortSize = SamplingRate * 10;  // 저장될 버퍼의 크기 -> 늘리니까 늘어남
    ShortBuffer shortBuffer = ShortBuffer.allocate(BufferShortSize);

    Queue<ShortBuffer> queue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. TODO : permission 코드 리팩토링
        // 2. TODO : 안드로이드 TODO 사용법 찾기 -> 다른 필터도 추가하자.
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);

        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            // 권한이 있을 때
        } else {
            // 권한이 없을 때
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    101);
        }

        img_recording = (ImageView) findViewById(R.id.img_recording);

        // TODO 버튼 리스너 하나 설정해놓고, 스위치 문으로 묶기 -> RyongNote 참고(?)
        btn_record = (Button) findViewById(R.id.btn_record);
        btn_record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                record();
            }
        });

        btn_play = (Button) findViewById(R.id.btn_play);
        btn_play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                play();
            }
        });

        btn_exit = (Button) findViewById(R.id.btn_exit);
        btn_exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exit();
            }
        });

        record_bufferSize = AudioRecord.getMinBufferSize(SamplingRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT) * 2;

        track_bufferSize = AudioTrack.getMinBufferSize(SamplingRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT) * 2;

        queue = new LinkedList<ShortBuffer>();

    }

    public void record() {
        if (isRecording == true) {  // 녹화가 진행 중일 떄 버튼이 눌리면,
            isRecording = false;
            btn_record.setText("Record");
            btn_play.setEnabled(true);
            img_recording.setVisibility(View.INVISIBLE);

            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
            recordThread = null;

        } else {  // isRecording == false 일 때,
            record_to_short = null;
            shortBuffer = null;

            // shortBuffer = ShortBuffer.allocate(BufferShortSize);
            record_to_short = new short[record_bufferSize];

            if(audioRecord == null) {
                audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                        SamplingRate,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        record_to_short.length);
            }

            isRecording = true;
            btn_record.setText("Stop");
            btn_play.setEnabled(false);
            img_recording.setVisibility(View.VISIBLE);

            retBufferSize = 0;
            audioRecord.startRecording();

            recordThread = new Thread(new Runnable() {
                @Override
                public void run() {

                    shortBuffer = ShortBuffer.allocate(BufferShortSize);

                    shortBuffer.rewind();

                    while(isRecording) {  // BufferRecord = short[], BufferRecordSize = minSize
                        retBufferSize = audioRecord.read(record_to_short, 0, record_bufferSize);
                        shortBuffer.put(record_to_short, 0, retBufferSize);
                        queue.add(shortBuffer);

                        //audioRecord.read(record_to_short, 0, record_bufferSize);
                        //shortBuffer.put(record_to_short, 0, record_bufferSize);
                        // shortBuffer에 record_to_short에 저장된 음성 녹음 데이터가 저장된다.
                    }

                }
            });

            recordThread.start();
        }
    }

    public void play() {
        btn_record.setEnabled(false);

        if (isPlaying == true) {  // 플레이가 진행 중인 상태에서 "STOP"을 누르면,
            isPlaying = false;
            btn_play.setText("Play");

            destroyAudioTrack();
        } else {
            track_to_short = new short[track_bufferSize];
            BufferTrackSize = track_to_short.length;

            isPlaying = true;
            btn_play.setText("Stop");

            audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                    SamplingRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    track_to_short.length,
                    AudioTrack.MODE_STREAM);

            playThread = new Thread(new Runnable() {
                @Override
                public void run() {

                    shortBuffer.position(0);
                    audioTrack.play();

                    // ToDo : [ shortBuffer.position() <= BufferShortSize - BufferTrackSize ] -> isPlaying으로 무한 반복 재생 성공
                    while (isPlaying) {
                        shortBuffer = queue.peek();
                        shortBuffer.get(track_to_short, 0, BufferTrackSize); // shortBuffer -> track_to_short
                        // error : Attempt to invoke virtual method 'java.nio.ShortBuffer java.nio.ShortBuffer.get(short[], int, int)' on a null object reference
                        audioTrack.write(track_to_short, 0, BufferTrackSize); // track_to_short -> audioTrack
                    }
                }
            });
            playThread.start();
        }
    }

    public void destroyAudioTrack() { // 오디오 끄는 역할

        if (audioTrack != null && audioTrack.getState() != AudioTrack.STATE_UNINITIALIZED) {
            if (audioTrack.getPlayState() != AudioTrack.PLAYSTATE_STOPPED) {
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

    public void exit() {
        audioRecord = null;
        audioTrack = null;
        recordThread = null;
        playThread = null;

        btn_record.setEnabled(true);
        queue = new LinkedList<ShortBuffer>();
    }
}

// TODO : 전체적인 UI 꾸미기
// TODO : Sampling Rate 선택하는 UI 만들기
// TODO : 클래스 전체 분류 및 코드 리팩토링
// TODO : AudioRecord 기능 측정