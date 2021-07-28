package org.techtown.samplerecorder;

import android.Manifest;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.text.Html;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.visualizer.amplitude.AudioRecordView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    public static int SampleRate = 16000;
    public static boolean isRecording = false;
    public static boolean isPlaying = false;
    public static boolean fileDrop;
    public static AudioRecordView view;

    private org.techtown.samplerecorder.AudioRecord myAudioRecord;
    private org.techtown.samplerecorder.AudioTrack myAudioTrack;

    private Button btn_record, btn_play, btn_setting, btn_fileDrop, btn_exit;
    private ImageView img_recording;
    private TextView text_timer, text_samplingRate;

    private long startTime, totalTime;
    private int tempRate = SampleRate, dialogIndex = 1;
    private boolean isRecorded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        permissionCheck();
        init();
    }

    public void init() {
        img_recording = (ImageView) findViewById(R.id.img_recording);
        view = findViewById(R.id.view_recordFFT);
        text_timer = (TextView) findViewById(R.id.text_timer);
        text_samplingRate = (TextView) findViewById(R.id.text_samplingRate);

        btn_record = (Button) findViewById(R.id.btn_record);
        btn_record.setOnClickListener(this);
        btn_play = (Button) findViewById(R.id.btn_play);
        btn_play.setOnClickListener(this);
        btn_setting = (Button) findViewById(R.id.btn_setting);
        btn_setting.setOnClickListener(this);
        btn_fileDrop = (Button) findViewById(R.id.btn_fileDrop);
        btn_fileDrop.setOnClickListener(this);
        btn_exit = (Button) findViewById(R.id.btn_exit);
        btn_exit.setOnClickListener(this);

        myAudioRecord = new org.techtown.samplerecorder.AudioRecord();
        myAudioRecord.init();

        myAudioTrack = new org.techtown.samplerecorder.AudioTrack();
        myAudioTrack.init();

        btn_play.setEnabled(false);

        SharedPreferences sharedPreferences = getSharedPreferences("fileDrop", MODE_PRIVATE);
        fileDrop = sharedPreferences.getBoolean("fileState", true);

        if(fileDrop == true) {
            btn_fileDrop.setText("FILE DROP ON");
            btn_fileDrop.setBackground(getDrawable(R.drawable.btn_filedrop_active));
        }
        else {
            btn_fileDrop.setText("FILE DROP OFF");
            btn_fileDrop.setBackground(getDrawable(R.drawable.btn_exit_and_inactive));
        }
    }

    public void permissionCheck() {
        int permission_record = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        int permission_readStore = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        int permission_writeStore = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);

            if (permission_record != PackageManager.PERMISSION_GRANTED) {  // 권한이 있을 때
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        101);
            }

            if (permission_readStore != PackageManager.PERMISSION_GRANTED) {  // 권한이 있을 때
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        102);
            }

            if (permission_writeStore != PackageManager.PERMISSION_GRANTED) {  // 권한이 있을 때
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        103);
            }
    }

    public void allInit() {
        myLog.d("method activate");

        myAudioRecord = new org.techtown.samplerecorder.AudioRecord();
        myAudioRecord.init();

        myAudioTrack = new org.techtown.samplerecorder.AudioTrack();
        myAudioTrack.init();
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
            case R.id.btn_fileDrop:
                fileDrop();
                break;
            case R.id.btn_exit:
                exit();
                break;
        }
    }

    public void record() {
        myLog.d("method activate");

        if (isRecording == true) {  // if "STOP" button clicked,
            myAudioRecord.stop();
            isRecording = false;  // anomaly : 함수 안으로 집어 넣으면 AudioRecord로 isRecording이 가끔씩 전달되지 않음
            stopRecording();
        } else {  // if "RECORD" button clicked,
            myAudioRecord.release();
            allInit();
            myAudioRecord.start(this);
            isRecording = true;
            startRecording();
        }
    }

    public void stopRecording() {
        myLog.d("method activate");

        btn_record.setText("Record");
        btn_record.setEnabled(true);
        btn_record.setBackground(getDrawable(R.drawable.btn_record_active));
        btn_play.setEnabled(true);
        btn_setting.setEnabled(true);
        img_recording.clearAnimation();
        img_recording.setVisibility(View.INVISIBLE);

        long stopTime = SystemClock.elapsedRealtime();
        totalTime = stopTime - startTime;
        recordHandler.removeMessages(0);

        SampleRate = tempRate;
        isRecorded = true;  // using separate dialog settings
    }

    public void startRecording() {
        myLog.d("method activate");

        myAudioTrack.release();

        startTime = 0;
        totalTime = 0;

        btn_record.setText("Stop");
        btn_record.setBackground(getDrawable(R.drawable.btn_exit_and_inactive));
        btn_setting.setEnabled(false);
        btn_exit.setEnabled(true);
        img_recording.setVisibility(View.VISIBLE);
        text_timer.setVisibility(View.VISIBLE);

        startTime = SystemClock.elapsedRealtime();
        recordHandler.sendEmptyMessage(0);

        // animation set at recording image emerge
        Animation animation = new AlphaAnimation(1, 0);
        animation.setDuration(500);
        animation.setInterpolator(new LinearInterpolator());
        animation.setRepeatCount(Animation.INFINITE);
        animation.setRepeatMode(Animation.REVERSE);
        img_recording.startAnimation(animation);

    }

    // TODO AudioTrack release는 언제할 수 있는가?

    public void play() {
        myLog.d("method activate");

        if (isPlaying == true) {  // if "STOP" button clicked,
            isPlaying = false;
            myAudioTrack.stop();
            stopPlaying();
        } else {  // if "PLAY" button clicked,
            isPlaying = true;
            myAudioTrack.play();
            startPlaying();
        }
    }

    public void stopPlaying() {
        myLog.d("method activate");

        btn_record.setEnabled(true);
        btn_play.setText("Play");
        btn_play.setBackground(getDrawable(R.drawable.btn_play_active));
        btn_setting.setEnabled(true);

        playHandler.removeMessages(0);
    }

    public void startPlaying() {
        myLog.d("method activate");

        btn_record.setEnabled(false);
        btn_play.setText("Stop");
        btn_play.setBackground(getDrawable(R.drawable.btn_exit_and_inactive));
        btn_setting.setEnabled(false);

        startTime = SystemClock.elapsedRealtime();
        playHandler.sendEmptyMessage(0);

    }

    public void fileDrop() {
        SharedPreferences sharedPreferences = getSharedPreferences("fileDrop", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        if(fileDrop == true) {
            fileDrop = false;
            editor.putBoolean("fileState", fileDrop);
            editor.commit();
            btn_fileDrop.setText("FILE DROP OFF");
            btn_fileDrop.setBackground(getDrawable(R.drawable.btn_exit_and_inactive));
        }
        else {
            fileDrop = true;
            editor.putBoolean("fileState", fileDrop);
            editor.commit();
            btn_fileDrop.setText("FILE DROP ON");
            btn_fileDrop.setBackground(getDrawable(R.drawable.btn_filedrop_active));
        }
    }

    public void setting() {
        myLog.d("method activate");

        final String[] freqArray = new String[] {"8,000", "16,000"};
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar);
        builder.setIcon(getDrawable(R.drawable.frequency));
        builder.setTitle("Sample Rate");
        builder.setSingleChoiceItems(freqArray, dialogIndex, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialogIndex = which;
                if (freqArray[which].equals("8,000")) {
                    tempRate = 8000;
                } else {
                    tempRate = 16000;
                }
            }
        });
        builder.setPositiveButton(Html.fromHtml("<font color='#3399FF'>Choice</font>"), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
//                if (isRecorded == false) {  // start record with changed sample rate,
//                    SampleRate = tempRate;
//                    text_samplingRate.setText("Sample Rate : " + String.valueOf(SampleRate));
//                    allInit();
//                } else {  // start play with changed sample rate,
//                    SampleRate = tempRate;
//                    text_samplingRate.setText("Sample Rate : " + String.valueOf(SampleRate));
//                }
                SampleRate = tempRate;
                text_samplingRate.setText("Sample Rate : " + String.valueOf(SampleRate));
                Toast.makeText(MainActivity.this, Integer.toString(SampleRate) + "로 설정 완료", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton(Html.fromHtml("<font color='#F06292'>Back</font>"), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(MainActivity.this, "취소", Toast.LENGTH_SHORT).show();
                return;
            }
        });

        Dialog dialog = builder.create();
        dialog.getWindow().setGravity(Gravity.CENTER);
        dialog.show();
    }

    public void exit() {
        myLog.d("method activate");

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar);
        builder.setTitle("Exit");
        builder.setMessage("Are you sure you want to quit?")
                .setCancelable(false)
                .setPositiveButton(Html.fromHtml("<font color='#3399FF'>Yes</font>"), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) {
                        finish();
                    }
                })
                .setNegativeButton(Html.fromHtml("<font color='#F06292'>No</font>"), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) {
                        dialog.cancel();
                    }
                });

        Dialog dialog = builder.create();
        dialog.getWindow().setGravity(Gravity.CENTER);
        dialog.show();
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