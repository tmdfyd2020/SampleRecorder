package org.techtown.samplerecorder;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.ShortBuffer;
import java.util.LinkedList;
import java.util.Queue;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    short[] record_to_short, track_to_short;
    int record_bufferSize, BufferTrackSize, track_bufferSize;
    int SamplingRate = 16000, tempRate;

    AudioRecord audioRecord = null;
    boolean isRecording = false;
    Thread recordThread = null;
    int retBufferSize;

    AudioTrack audioTrack = null;
    boolean isPlaying = false;
    Thread playThread = null;

    Button btn_record, btn_play, btn_setting, btn_exit;
    ImageView img_recording;
    TextView text_timer;

    int BufferShortSize = SamplingRate * 10;  // 저장될 버퍼의 크기 -> 늘리니까 늘어남
    ShortBuffer shortBuffer = ShortBuffer.allocate(BufferShortSize);

    Queue<ShortBuffer> queue;

    private long baseTime, storeTime;

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
        text_timer = (TextView) findViewById(R.id.text_timer);

        btn_record = (Button) findViewById(R.id.btn_record);
        btn_record.setOnClickListener(this);
        btn_play = (Button) findViewById(R.id.btn_play);
        btn_play.setOnClickListener(this);
        btn_setting = (Button) findViewById(R.id.btn_setting);
        btn_setting.setOnClickListener(this);
        btn_exit = (Button) findViewById(R.id.btn_exit);
        btn_exit.setOnClickListener(this);

        record_bufferSize = AudioRecord.getMinBufferSize(SamplingRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT) * 2;

        track_bufferSize = AudioTrack.getMinBufferSize(SamplingRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT) * 2;

        queue = new LinkedList<ShortBuffer>();

    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.btn_record:
                record();
                break;
            case R.id.btn_play:
                play();
                break;
            case R.id.btn_setting:
                setting();
                break;
            case R.id.btn_exit:
                exit();
                break;
        }
    }

    public void record() {
        if (isRecording == true) {  // 녹화가 진행 중일 떄 버튼이 눌리면,
            isRecording = false;
            btn_record.setText("Record");
            btn_play.setEnabled(true);
            img_recording.setVisibility(View.INVISIBLE);
            btn_record.setBackground(getDrawable(R.drawable.btn_record_active));
            storeTime = SystemClock.elapsedRealtime();
            handler.removeMessages(0);

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
            text_timer.setVisibility(View.VISIBLE);
            btn_record.setBackground(getDrawable(R.drawable.btn_exit_and_inactive));
            baseTime = SystemClock.elapsedRealtime();
            handler.sendEmptyMessage(0);

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
            btn_play.setBackground(getDrawable(R.drawable.btn_play_active));

            destroyAudioTrack();
        } else {
            track_to_short = new short[track_bufferSize];
            BufferTrackSize = track_to_short.length;

            isPlaying = true;
            btn_play.setText("Stop");
            btn_play.setBackground(getDrawable(R.drawable.btn_exit_and_inactive));

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

    public void setting() {
        final String[] frequencyArray = new String[] {"8,000", "16,000"};
        AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
        dialog.setIcon(getDrawable(R.drawable.frequency));
        dialog.setTitle("Sampling Rate");
        dialog.setSingleChoiceItems(frequencyArray, 0, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (frequencyArray[which].equals("8,000")) {
                    Toast.makeText(MainActivity.this, frequencyArray[which] + "을 선택함.", Toast.LENGTH_SHORT).show();
                    tempRate = 8000;
                } else {
                    Toast.makeText(MainActivity.this, frequencyArray[which] + "을 선택함.", Toast.LENGTH_SHORT).show();
                    tempRate = 16000;
                }
            }
        });
        dialog.setPositiveButton("Choice", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SamplingRate = tempRate;
                Toast.makeText(MainActivity.this, Integer.toString(SamplingRate) + "로 설정 완료", Toast.LENGTH_SHORT).show();

            }
        });
        dialog.setNegativeButton("Back", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(MainActivity.this, "취소", Toast.LENGTH_SHORT).show();
                return;
            }
        });
        dialog.show();


    }

    public void exit() {
        audioRecord = null;
        audioTrack = null;
        recordThread = null;
        playThread = null;

        btn_record.setEnabled(true);
        queue = new LinkedList<ShortBuffer>();
        text_timer.setText("00 : 00 : 00");
        baseTime = 0;
        storeTime = 0;
    }

    public String getTime() {  // 스톱워치 실시간 시간
        long nowTime = SystemClock.elapsedRealtime();
        long overTime = nowTime - baseTime;

        long m = overTime / 1000 / 60;
        long s = (overTime / 1000) % 60;
        long ms = overTime % 1000;

        String recTime = String.format("%02d : %02d : %01d", m, s, ms);

        return recTime;
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            text_timer.setText(getTime());

            handler.sendEmptyMessage(0);
        }
    };
}

// TODO : 클래스 전체 분류 및 코드 리팩토링(변수명 등)
// TODO : AudioRecord 기능 측정 -> 10초 가량 녹음을 지속하면 튕긴다.