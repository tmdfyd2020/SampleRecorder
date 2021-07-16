package org.techtown.samplerecorder;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    public static int SamplingRate = 16000;
    public static boolean isRecording = false;
    public static boolean isPlaying = false;

    private org.techtown.samplerecorder.AudioRecord myAudioRecord;
    private org.techtown.samplerecorder.AudioTrack myAudioTrack;

    private Button btn_record, btn_play, btn_setting, btn_exit;
    private ImageView img_recording;
    private TextView text_timer;

    private long startTime, totalTime;
    private int tempRate = 16000, dialogIndex = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        permission();

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

        btn_play.setEnabled(false);
        btn_exit.setEnabled(false);
    }

    public void permission() {
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {  // 권한이 있을 때
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
        myLog.d("");
        if (isRecording == true) {  // 녹화가 진행 중일 때 버튼이 눌리면,
            myAudioRecord.stop();
            stopRecording();
        } else {  // isRecording == false 일 때,
            myAudioRecord.start();
            startRecording();
        }
    }

    public void stopRecording() {
        myLog.d("");
        isRecording = false;

        btn_record.setText("Record");
        btn_record.setEnabled(false);
        btn_record.setBackground(getDrawable(R.drawable.btn_record_active));
        btn_play.setEnabled(true);
        btn_setting.setEnabled(true);
        img_recording.setVisibility(View.INVISIBLE);

        long stopTime = SystemClock.elapsedRealtime();
        totalTime = stopTime - startTime;
        recordHandler.removeMessages(0);
        android.util.Log.d("[Main]", String.valueOf(stopTime));
    }

    public void startRecording() {
        myLog.d("");
        myLog.d("Recording Sampling Rate = " + String.valueOf(SamplingRate));

        isRecording = true;

        btn_record.setText("Stop");
        btn_record.setBackground(getDrawable(R.drawable.btn_exit_and_inactive));
        btn_setting.setEnabled(false);
        btn_exit.setEnabled(true);
        img_recording.setVisibility(View.VISIBLE);
        text_timer.setVisibility(View.VISIBLE);

        startTime = SystemClock.elapsedRealtime();
        recordHandler.sendEmptyMessage(0);
    }

    public void play() {
        myLog.d("");
        if (isPlaying == true) {  // 플레이가 진행 중인 상태에서 "STOP"을 누르면,
            stopPlaying();
            myAudioTrack.stop();
        } else {
            startPlaying();
            myAudioTrack.play();
        }
    }

    public void stopPlaying() {
        myLog.d("");
        isPlaying = false;

        btn_play.setText("Play");
        btn_play.setBackground(getDrawable(R.drawable.btn_play_active));
        btn_setting.setEnabled(true);

        playHandler.removeMessages(0);
    }

    public void startPlaying() {
        myLog.d("");
        myLog.d("Playing Sampling Rate = " + String.valueOf(SamplingRate));
        isPlaying = true;

        btn_play.setText("Stop");
        btn_play.setBackground(getDrawable(R.drawable.btn_exit_and_inactive));
        btn_setting.setEnabled(false);

        startTime = SystemClock.elapsedRealtime();
        playHandler.sendEmptyMessage(0);
    }

    public void setting() {
        myLog.d("");
        final String[] frequencyArray = new String[] {"8,000", "16,000"};

        AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
        dialog.setIcon(getDrawable(R.drawable.frequency));
        dialog.setTitle("Sampling Rate");
        dialog.setSingleChoiceItems(frequencyArray, dialogIndex, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialogIndex = which;
                if (frequencyArray[which].equals("8,000")) {
                    tempRate = 8000;
                } else {
                    tempRate = 16000;
                }
            }
        });
        dialog.setPositiveButton("Choice", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SamplingRate = tempRate;
                myAudioRecord.init();
                myAudioTrack.init();

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
        myLog.d("");
        myAudioRecord.release();
        myAudioTrack.release();

        btn_record.setEnabled(true);
        btn_play.setEnabled(false);
        btn_play.setText("Play");
        btn_play.setBackground(getDrawable(R.drawable.btn_play_active));
        btn_setting.setEnabled(true);
        text_timer.setVisibility(View.INVISIBLE);

        startTime = 0;
        totalTime = 0;
    }

    Handler recordHandler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            text_timer.setText(getRecordTime());
            recordHandler.sendEmptyMessage(0);
        }
    };

    public String getRecordTime() {
        long nowTime = SystemClock.elapsedRealtime();
        long overTime = nowTime - startTime;

        long m = overTime / 1000 / 60;
        long s = (overTime / 1000) % 60;
        long ms = overTime % 1000 / 10;

        String timeText = String.format("%02d : %02d : %02d", m, s, ms);

        return timeText;
    }

    Handler playHandler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            text_timer.setText(getPlayTime());
            playHandler.sendEmptyMessage(0);
        }
    };

    public String getPlayTime() {
        long nowTime = SystemClock.elapsedRealtime();
        long overTime = nowTime - startTime;
        long min, sec, msec;
        String timeText;

        if (overTime > totalTime) {
            min = totalTime / 1000 / 60;
            sec = (totalTime / 1000) % 60;
            msec = totalTime % 1000 / 10;

            timeText = String.format("%02d : %02d : %02d", min, sec, msec);

            return timeText;
        }

        min = overTime / 1000 / 60;
        sec = (overTime / 1000) % 60;
        msec = overTime % 1000 / 10;

        timeText = String.format("%02d : %02d : %02d", min, sec, msec);

        return timeText;
    }


}