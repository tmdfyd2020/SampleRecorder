package org.techtown.samplerecorder;

import android.Manifest;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.text.Html;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.visualizer.amplitude.AudioRecordView;

import lib.kingja.switchbutton.SwitchMultiButton;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    public static boolean isRecording = false;
    public static boolean isPlaying = false;
    public static boolean fileDrop;
    public static boolean autoStop = false;
    public static AudioRecordView view;

    private org.techtown.samplerecorder.AudioRecord myAudioRecord;
    private org.techtown.samplerecorder.AudioTrack myAudioTrack;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private Button btn_record, btn_play, btn_record_sampleRate, btn_play_sampleRate, btn_exit;
    private ImageView img_recording, img_playing;
    private TextView text_record_timer, text_play_timer;
    private long startTime, totalTime;
    private int record_sampleRate = 16000, play_sampleRate = 16000;
    private int record_tempRate = record_sampleRate, play_tempRate = play_sampleRate, record_sampleRate_index = 1, play_sampleRate_index = 1;
    private SwitchMultiButton switchButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        permissionCheck();
        init();
    }

    public void init() {
//        myLog.d("method activate");

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        img_recording = (ImageView) findViewById(R.id.img_recording);
        img_playing = (ImageView) findViewById(R.id.img_playing);
        view = findViewById(R.id.view_waveForm);
        text_record_timer = (TextView) findViewById(R.id.text_record_timer);
        text_play_timer = (TextView) findViewById(R.id.text_play_timer);

        btn_record = (Button) findViewById(R.id.btn_record);
        btn_record.setOnClickListener(this);
        btn_play = (Button) findViewById(R.id.btn_play);
        btn_play.setOnClickListener(this);
        btn_play.setEnabled(false);
        btn_record_sampleRate = (Button) findViewById(R.id.btn_record_sampleRate);
        btn_record_sampleRate.setOnClickListener(this);
        btn_play_sampleRate = (Button) findViewById(R.id.btn_play_sampleRate);
        btn_play_sampleRate.setOnClickListener(this);
        btn_exit = (Button) findViewById(R.id.btn_record_bufferSize);
        btn_exit.setOnClickListener(this);

        switchButton = (SwitchMultiButton) findViewById(R.id.switchButton);
        switchButton.setOnSwitchListener(new SwitchMultiButton.OnSwitchListener() {
            @Override
            public void onSwitch(int position, String tabText) {

                if (position == 0) {  // for file drop open
                    fileDrop = true;
                    editor.putBoolean("fileState", fileDrop);
                    editor.commit();
                    view.recreate();
                } else if (position == 1) {  // for file drop down
                    fileDrop = false;
                    editor.putBoolean("fileState", fileDrop);
                    editor.commit();
                    view.recreate();
                }
            }
        });

        sharedPreferences = getSharedPreferences("fileDrop", MODE_PRIVATE);
        editor = sharedPreferences.edit();
        fileDrop = sharedPreferences.getBoolean("fileState", true);

        if (fileDrop) {
            switchButton.setSelectedTab(0);
        } else {
            switchButton.setSelectedTab(1);
        }
    }

    public void permissionCheck() {
//        myLog.d("method activate");

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
//        myLog.d("method activate");

        myAudioRecord = new org.techtown.samplerecorder.AudioRecord();
        myAudioRecord.init(record_sampleRate);

        myAudioTrack = new org.techtown.samplerecorder.AudioTrack();
        myAudioTrack.init(play_sampleRate);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_toolbar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_exit:
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar);
                builder.setTitle("Exit")
                        .setMessage("Are you sure you want to quit?")
                        .setIcon(getDrawable(R.drawable.ic_baseline_exit_icon))
                        .setCancelable(false)
                        .setPositiveButton(Html.fromHtml("<font color='#3399FF'>Yes</font>"), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        })
                        .setNegativeButton(Html.fromHtml("<font color='#F06292'>No</font>"), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });

                Dialog dialog = builder.create();
                dialog.getWindow().setGravity(Gravity.CENTER);
                dialog.show();
                break;
        }

        return true;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_record:
                record();
                break;
            case R.id.btn_play:
                play();
                break;
            case R.id.btn_record_sampleRate:
                record_sampleRate();
                break;
            case R.id.btn_play_sampleRate:
                play_sampleRate();
                break;
            case R.id.btn_record_bufferSize:
                exit();
                break;

        }
    }

    public void record() {
//        myLog.d("method activate");

        if (isRecording) {  // if "STOP" button clicked,
            myAudioRecord.stop();
            myAudioRecord.release();
            isRecording = false;  // check : 함수 안으로 집어 넣으면 AudioRecord로 isRecording이 가끔씩 전달되지 않음
            stopRecording();
        } else {  // if "RECORD" button clicked,
            allInit();
            myAudioRecord.start(record_sampleRate);
            isRecording = true;
            startRecording();
        }
    }

    public void stopRecording() {
//        myLog.d("method activate");

        img_recording.clearAnimation();
        img_recording.setVisibility(View.INVISIBLE);
        btn_record.setText("Record");
        btn_record.setEnabled(true);
        btn_record.setBackground(getDrawable(R.drawable.btn_record_active));
        btn_play.setEnabled(true);
        btn_record_sampleRate.setEnabled(true);

        recordHandler.removeMessages(0);
        long stopTime = SystemClock.elapsedRealtime();
        totalTime = stopTime - startTime;
    }

    public void startRecording() {
//        myLog.d("method activate");

        startTime = 0;
        totalTime = 0;

        btn_record.setText("Stop");
        btn_record.setBackground(getDrawable(R.drawable.btn_exit_and_inactive));
        btn_record_sampleRate.setEnabled(false);
        btn_exit.setEnabled(true);
        img_recording.setVisibility(View.VISIBLE);
        text_record_timer.setVisibility(View.VISIBLE);
        text_play_timer.setVisibility(View.INVISIBLE);

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

    public void play() {
//        myLog.d("method activate");

        if (isPlaying) {  // if "STOP" button clicked,
            isPlaying = false;
            myAudioTrack.stop();
            myAudioTrack.release();
            stopPlaying();
        } else {  // if "PLAY" button clicked,
            isPlaying = true;
            myAudioTrack.play(play_sampleRate);
            startPlaying();
        }
    }

    public void stopPlaying() {
//        myLog.d("method activate");

        img_playing.clearAnimation();
        img_playing.setVisibility(View.INVISIBLE);
        btn_record.setEnabled(true);
        btn_play.setText("Play");
        btn_play.setBackground(getDrawable(R.drawable.btn_play_active));
        btn_record_sampleRate.setEnabled(true);

        playHandler.removeMessages(0);
        autoStopHandler.removeMessages(0);

    }

    public void startPlaying() {
//        myLog.d("method activate");

        img_playing.setVisibility(View.VISIBLE);
        btn_record.setEnabled(false);
        btn_play.setText("Stop");
        btn_play.setBackground(getDrawable(R.drawable.btn_exit_and_inactive));
        btn_record_sampleRate.setEnabled(false);
        text_play_timer.setVisibility(View.VISIBLE);

        startTime = SystemClock.elapsedRealtime();
        playHandler.sendEmptyMessage(0);
        autoStopHandler.sendEmptyMessage(0);

        Animation animation = new AlphaAnimation(1, 0);
        animation.setDuration(500);
        animation.setInterpolator(new LinearInterpolator());
        animation.setRepeatCount(Animation.INFINITE);
        animation.setRepeatMode(Animation.REVERSE);
        img_playing.startAnimation(animation);

    }

    public void record_sampleRate() {
//        myLog.d("method activate");

        final String[] freqArray = new String[]{"8,000", "16,000"};
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar);
        builder.setIcon(getDrawable(R.drawable.frequency))
                .setTitle("Sample Rate")
                .setCancelable(false)
                .setSingleChoiceItems(freqArray, record_sampleRate_index, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (freqArray[which].equals("8,000")) {
                            record_tempRate = 8000;
                        } else if (freqArray[which].equals("16,000")) {
                            record_tempRate = 16000;
                        }
                        record_sampleRate_index = which;
                    }
                })
                .setPositiveButton(Html.fromHtml("<font color='#3399FF'>Choice</font>"), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        record_sampleRate = record_tempRate;
                        btn_record_sampleRate.setText("SAMPLE RATE\n" + String.valueOf(record_sampleRate));
                        Toast.makeText(MainActivity.this, Integer.toString(record_sampleRate) + "로 설정 완료", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(Html.fromHtml("<font color='#F06292'>Back</font>"), new DialogInterface.OnClickListener() {
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

    public void play_sampleRate() {
//        myLog.d("method activate");

        final String[] freqArray = new String[]{"8,000", "16,000"};
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar);
        builder.setIcon(getDrawable(R.drawable.frequency))
                .setTitle("Sample Rate")
                .setCancelable(false)
                .setSingleChoiceItems(freqArray, play_sampleRate_index, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        play_sampleRate_index = which;
                        if (freqArray[which].equals("8,000")) {
                            play_tempRate = 8000;
                        } else if (freqArray[which].equals("16,000")) {
                            play_tempRate = 16000;
                        }
                    }
                })
                .setPositiveButton(Html.fromHtml("<font color='#3399FF'>Choice</font>"), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        play_sampleRate = play_tempRate;
                        btn_play_sampleRate.setText("SAMPLE RATE\n" + String.valueOf(play_sampleRate));
                        Toast.makeText(MainActivity.this, Integer.toString(play_sampleRate) + "로 설정 완료", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(Html.fromHtml("<font color='#F06292'>Back</font>"), new DialogInterface.OnClickListener() {
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
//        myLog.d("method activate");

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar);
        builder.setTitle("Exit")
                .setMessage("Are you sure you want to quit?")
                .setIcon(getDrawable(R.drawable.ic_baseline_exit_icon))
                .setCancelable(false)
                .setPositiveButton(Html.fromHtml("<font color='#3399FF'>Yes</font>"), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setNegativeButton(Html.fromHtml("<font color='#F06292'>No</font>"), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
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
            text_record_timer.setText(getRecordTime());

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
            text_play_timer.setText(getPlayTime());

            playHandler.sendEmptyMessage(0);
        }
    };

    public String getPlayTime() {
        long nowTime = SystemClock.elapsedRealtime();
        long overTime = nowTime - startTime;
        long min, sec, msec;
        String timeText;

        if (overTime > totalTime && record_sampleRate == play_sampleRate) {
            min = totalTime / 1000 / 60;
            sec = (totalTime / 1000) % 60;
            msec = ((totalTime - 3) % 1000) / 10;

            timeText = String.format("%02d : %02d : %02d", min, sec, msec);

            return timeText;
        }

        min = overTime / 1000 / 60;
        sec = (overTime / 1000) % 60;
        msec = overTime % 1000 / 10;

        timeText = String.format("%02d : %02d : %02d", min, sec, msec);

        return timeText;
    }

    Handler autoStopHandler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            if (autoStop) {
                myLog.d("autoStop 발생!");
                autoStop = false;
                isPlaying = false;
                myAudioTrack.stop();
                myAudioTrack.release();

                img_playing.clearAnimation();
                img_playing.setVisibility(View.INVISIBLE);
                btn_record.setEnabled(true);
                btn_play.setText("Play");
                btn_play.setBackground(getDrawable(R.drawable.btn_play_active));
                btn_record_sampleRate.setEnabled(true);

                if (record_sampleRate != play_sampleRate) {
                    playHandler.removeMessages(0);
                }
                autoStopHandler.removeMessages(0);
            }
            autoStopHandler.sendEmptyMessage(0);
        }
    };

}