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
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.ShortBuffer;
import java.util.LinkedList;
import java.util.Queue;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    public static int SamplingRate = 16000;
    int tempRate;

    public static boolean isRecording = false;
    public static boolean isPlaying = false;

    Button btn_record, btn_play, btn_setting, btn_exit;
    ImageView img_recording;
    TextView text_timer;

    private long baseTime, storeTime;

    org.techtown.samplerecorder.AudioRecord myAudioRecord;
    org.techtown.samplerecorder.AudioTrack myAudioTrack;

    // public static Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        permission();
        // context = this;

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

        myAudioRecord = new org.techtown.samplerecorder.AudioRecord();
        myAudioRecord.init();

        myAudioTrack = new org.techtown.samplerecorder.AudioTrack();
        myAudioTrack.init();
    }

    public void permission() {
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);

        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            // 권한이 있을 때
        } else {
            // 권한이 없을 때
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    101);
        }
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
        if (isRecording == true) {  // 녹화가 진행 중일 때 버튼이 눌리면,
            isRecording = false;
            android.util.Log.d("[Main]", "[MainActivity][record()][else] isRecording " + String.valueOf(isRecording));
            btn_record.setText("Record");
            btn_play.setEnabled(true);
            img_recording.setVisibility(View.INVISIBLE);
            btn_record.setBackground(getDrawable(R.drawable.btn_record_active));
            storeTime = SystemClock.elapsedRealtime();
            handler.removeMessages(0);

            myAudioRecord.stop();

        } else {  // isRecording == false 일 때,

            isRecording = true;  // TODO : 도대체 이게 왜 전달이 안되는거지..?
            android.util.Log.d("[Main]", "[MainActivity][record()][else] isRecording " + String.valueOf(isRecording));  // isRecording = true;

            myAudioRecord.start();

            btn_record.setText("Stop");
            btn_play.setEnabled(false);
            img_recording.setVisibility(View.VISIBLE);
            text_timer.setVisibility(View.VISIBLE);
            btn_record.setBackground(getDrawable(R.drawable.btn_exit_and_inactive));
            baseTime = SystemClock.elapsedRealtime();
            handler.sendEmptyMessage(0);

        }
    }

    public void play() {
        btn_record.setEnabled(false);

        if (isPlaying == true) {  // 플레이가 진행 중인 상태에서 "STOP"을 누르면,
            isPlaying = false;
            android.util.Log.d("[Main]", "[MainActivity][play()][if] isPlaying " + String.valueOf(isPlaying));
            btn_play.setText("Play");
            btn_play.setBackground(getDrawable(R.drawable.btn_play_active));

            myAudioTrack.stop();
        } else {

            isPlaying = true;
            Log.d("[Main]", "[MainActivity][play()][else] isPlaying " + String.valueOf(isPlaying));
            btn_play.setText("Stop");
            btn_play.setBackground(getDrawable(R.drawable.btn_exit_and_inactive));

            myAudioTrack.play();
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
        myAudioRecord.release();
        myAudioTrack.release();

        btn_record.setEnabled(true);
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