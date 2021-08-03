package org.techtown.samplerecorder;

import android.Manifest;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.MediaRecorder;
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
import android.view.ViewTreeObserver;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
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
    public static AudioRecordView view_record, view_play;

    private org.techtown.samplerecorder.AudioRecord myAudioRecord;
    private org.techtown.samplerecorder.AudioTrack myAudioTrack;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private Button btn_record, btn_record_source, btn_record_channel, btn_record_sampleRate, btn_record_bufferSize;
    private Button btn_play, btn_play_type, btn_play_channel, btn_play_sampleRate, btn_play_volume;
    private ImageView img_recording, img_playing;
    private TextView text_record_timer, text_play_timer, text_seekbar;
    private SeekBar seekBar_volume;
    private long startTime, totalTime;
    private int record_source, record_tempSource, record_source_index, play_type, play_tempType, play_type_index;
    private int record_channel, record_tempChannel, record_channel_index, play_channel, play_tempChannel, play_channel_index;
    private int record_sampleRate, record_tempRate, record_sampleRate_index, play_sampleRate, play_tempRate, play_sampleRate_index;
    private int record_bufferSize, record_tempBuffer, record_bufferSize_index, play_volume, play_TempVolume, play_volume_index;
    private SwitchMultiButton switchButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        permissionCheck();
        constant_init();
        init();
    }

    public void constant_init() {
        record_source = MediaRecorder.AudioSource.MIC;
        record_tempSource = record_source;
        record_source_index = 0;
        record_channel = AudioFormat.CHANNEL_IN_MONO;
        record_tempChannel = record_channel;
        record_channel_index = 0;
        record_sampleRate = 16000;
        record_tempRate = record_sampleRate;
        record_sampleRate_index = 2;
        record_bufferSize = 1024;
        record_tempBuffer = record_bufferSize;
        record_bufferSize_index = 1;

        play_type = AudioManager.STREAM_MUSIC;
        play_tempType = play_type;
        play_type_index = 0;
        play_channel = AudioFormat.CHANNEL_OUT_MONO;
        play_tempChannel = play_channel;
        play_channel_index = 0;
        play_sampleRate = 16000;
        play_tempRate = play_sampleRate;
        play_sampleRate_index = 2;
        play_volume = 1;
        play_TempVolume = play_volume;
        play_volume_index = 0;
    }

    public void init() {
//        myLog.d("method activate");

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        img_recording = (ImageView) findViewById(R.id.img_recording);
        view_record = findViewById(R.id.view_record_waveForm);
        text_record_timer = (TextView) findViewById(R.id.text_record_timer);
        btn_record_source = (Button) findViewById(R.id.btn_record_source);
        btn_record_source.setOnClickListener(this);
        btn_record_channel = (Button) findViewById(R.id.btn_record_channel);
        btn_record_channel.setOnClickListener(this);
        btn_record_sampleRate = (Button) findViewById(R.id.btn_record_sampleRate);
        btn_record_sampleRate.setOnClickListener(this);
        btn_record_bufferSize = (Button) findViewById(R.id.btn_record_bufferSize);
        btn_record_bufferSize.setOnClickListener(this);
        btn_record = (Button) findViewById(R.id.btn_record);
        btn_record.setOnClickListener(this);

        img_playing = (ImageView) findViewById(R.id.img_playing);
        view_play = findViewById(R.id.view_play_waveForm);
        text_play_timer = (TextView) findViewById(R.id.text_play_timer);
        btn_play_type = (Button) findViewById(R.id.btn_play_type);
        btn_play_type.setOnClickListener(this);
        btn_play_channel = (Button) findViewById(R.id.btn_play_channel);
        btn_play_channel.setOnClickListener(this);
        btn_play_sampleRate = (Button) findViewById(R.id.btn_play_sampleRate);
        btn_play_sampleRate.setOnClickListener(this);
        btn_play_volume = (Button) findViewById(R.id.btn_play_volume);
        btn_play_volume.setOnClickListener(this);
        final AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        int nCurrentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        btn_play_volume.setText("VOLUME\n" + String.valueOf(nCurrentVolume));
        btn_play = (Button) findViewById(R.id.btn_play);
        btn_play.setOnClickListener(this);
        btn_play.setEnabled(false);

        switchButton = (SwitchMultiButton) findViewById(R.id.switchButton);
        switchButton.setOnSwitchListener(new SwitchMultiButton.OnSwitchListener() {
            @Override
            public void onSwitch(int position, String tabText) {

                if (position == 0) {  // for file drop open
                    fileDrop = true;
                    editor.putBoolean("fileState", fileDrop);
                    editor.commit();
                    view_record.recreate();
                } else if (position == 1) {  // for file drop down
                    fileDrop = false;
                    editor.putBoolean("fileState", fileDrop);
                    editor.commit();
                    view_record.recreate();
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

        text_seekbar = (TextView) findViewById(R.id.text_seekbar);
    }

    public void permissionCheck() {
//        myLog.d("method activate");

        int permission_record = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        int permission_writePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int permission_readPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);

        if (permission_record == PackageManager.PERMISSION_GRANTED) {  // 권한이 있을 때
            myLog.d("RECORD_AUDIO 권한 설정 완료");
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[] {Manifest.permission.RECORD_AUDIO},
                    101);
        }

        if (permission_writePermission == PackageManager.PERMISSION_GRANTED) {  // 권한이 있을 때
            myLog.d("WRITE_EXTERNAL_STORAGE 권한 설정 완료");
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    201);
        }

        if (permission_readPermission == PackageManager.PERMISSION_GRANTED) {  // 권한이 있을 때
            myLog.d("READ_EXTERNAL_STORAGE 권한 설정 완료");
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[] {Manifest.permission.READ_EXTERNAL_STORAGE},
                    301);
        }
    }

    public void allInit() {
//        myLog.d("method activate");

        myAudioRecord = new org.techtown.samplerecorder.AudioRecord();
        myAudioRecord.init(record_sampleRate, record_bufferSize);

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
            case R.id.btn_record_source:
                record_source();
                break;
            case R.id.btn_record_channel:
                record_channel();
                break;
            case R.id.btn_record_sampleRate:
                record_sampleRate();
                break;
            case R.id.btn_record_bufferSize:
                record_bufferSize();
                break;
            case R.id.btn_play_type:
                play_type();
                break;
            case R.id.btn_play_channel:
                play_channel();
                break;
            case R.id.btn_play_sampleRate:
                play_sampleRate();
                break;
            case R.id.btn_play_volume:
                play_volume();
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
            myAudioRecord.start(record_source, record_channel, record_sampleRate);
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
        myLog.d(String.valueOf(totalTime));
    }

    public void startRecording() {
//        myLog.d("method activate");

        startTime = 0;
        totalTime = 0;

        btn_record.setText("Stop");
        btn_record.setBackground(getDrawable(R.drawable.btn_exit_and_inactive));
        btn_record_sampleRate.setEnabled(false);
        btn_record_bufferSize.setEnabled(true);
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
            myAudioTrack.play(play_type, play_channel, play_sampleRate);
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

    public void record_source() {
//        myLog.d("method activate");

        final String[] source = new String[] {"MIC", "VOICE"};
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar);
        builder.setIcon(getDrawable(R.drawable.ic_baseline_source_icon))
                .setTitle("Source")
                .setCancelable(false)
                .setSingleChoiceItems(source, record_source_index, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (source[which].equals("MIC")) {
                            record_tempSource = MediaRecorder.AudioSource.MIC;
                        } else {
                            record_tempSource = MediaRecorder.AudioSource.VOICE_RECOGNITION;
                        }
                        record_source_index = which;
                    }
                })
                .setPositiveButton(Html.fromHtml("<font color='#3399FF'>Choice</font>"), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        record_source = record_tempSource;
                        if (record_source == MediaRecorder.AudioSource.MIC) {
                            btn_record_source.setText("SOURCE\nMIC");
                            Toast.makeText(MainActivity.this, "MIC로 설정 완료", Toast.LENGTH_SHORT).show();
                        } else {
                            btn_record_source.setText("SOURCE\nVOICE");
                            Toast.makeText(MainActivity.this, "VOICE로 설정 완료", Toast.LENGTH_SHORT).show();
                        }
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

    public void record_channel() {
//        myLog.d("method activate");

        final String[] channel = new String[] {"MONO", "STEREO"};
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar);
        builder.setIcon(getDrawable(R.drawable.ic_baseline_record_channel))
                .setTitle("Channel")
                .setCancelable(false)
                .setSingleChoiceItems(channel, record_channel_index, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (channel[which].equals("MONO")) {
                            record_tempChannel = AudioFormat.CHANNEL_IN_MONO;
                        } else {
                            record_tempChannel = AudioFormat.CHANNEL_IN_STEREO;
                        }
                        record_channel_index = which;
                    }
                })
                .setPositiveButton(Html.fromHtml("<font color='#3399FF'>Choice</font>"), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        record_channel = record_tempChannel;
                        if (record_channel == AudioFormat.CHANNEL_IN_MONO) {
                            btn_record_channel.setText("CHANNEL\nMONO");
                            Toast.makeText(MainActivity.this, "MONO로 설정 완료", Toast.LENGTH_SHORT).show();
                        } else {
                            btn_record_channel.setText("CHANNEL\nSTEREO");
                            Toast.makeText(MainActivity.this, "STEREO로 설정 완료", Toast.LENGTH_SHORT).show();
                        }
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

    public void record_sampleRate() {
//        myLog.d("method activate");

        final String[] freqArray = new String[] {"8,000", "11,025", "16,000", "22,050", "44,100"};
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar);
        builder.setIcon(getDrawable(R.drawable.ic_baseline_record_samplerate))
                .setTitle("Sample Rate")
                .setCancelable(false)
                .setSingleChoiceItems(freqArray, record_sampleRate_index, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch(freqArray[which]) {
                            case "8,000":
                                record_tempRate = 8000;
                                break;
                            case "11,025":
                                record_tempRate = 11025;
                                break;
                            case "16,000":
                                record_tempRate = 16000;
                                break;
                            case "22,050":
                                record_tempRate = 22050;
                                break;
                            case "44,100":
                                record_tempRate = 44100;
                                break;
                        }
                        record_sampleRate_index = which;
                    }
                })
                .setPositiveButton(Html.fromHtml("<font color='#3399FF'>Choice</font>"), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        record_sampleRate = record_tempRate;
                        playHandler.removeMessages(0);
                        switch(record_sampleRate) {
                            case 8000:
                                btn_record_sampleRate.setText("SAMPLE RATE\n8,000");
                                Toast.makeText(MainActivity.this, "8,000으로 설정 완료", Toast.LENGTH_SHORT).show();
                                break;
                            case 11025:
                                btn_record_sampleRate.setText("SAMPLE RATE\n11,025");
                                Toast.makeText(MainActivity.this, "11,025으로 설정 완료", Toast.LENGTH_SHORT).show();
                                break;
                            case 16000:
                                btn_record_sampleRate.setText("SAMPLE RATE\n16,000");
                                Toast.makeText(MainActivity.this, "16,000으로 설정 완료", Toast.LENGTH_SHORT).show();
                                break;
                            case 22050:
                                btn_record_sampleRate.setText("SAMPLE RATE\n22,050");
                                Toast.makeText(MainActivity.this, "22,050으로 설정 완료", Toast.LENGTH_SHORT).show();
                                break;
                            case 44100:
                                btn_record_sampleRate.setText("SAMPLE RATE\n44,100");
                                Toast.makeText(MainActivity.this, "44,100으로 설정 완료", Toast.LENGTH_SHORT).show();
                                break;
                        }
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

    public void record_bufferSize() {
//        myLog.d("method activate");

        final String[] bufferSize = new String[] {"512", "1,024", "2,048"};
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar);
        builder.setIcon(getDrawable(R.drawable.ic_baseline_buffersize))
                .setTitle("Buffer Size")
                .setCancelable(false)
                .setSingleChoiceItems(bufferSize, record_bufferSize_index, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch(bufferSize[which]) {
                            case "512":
                                record_tempBuffer = 512;
                                break;
                            case "1,024":
                                record_tempBuffer = 1024;
                                break;
                            case "2,048":
                                record_tempBuffer = 2048;
                                break;
                        }
                        record_bufferSize_index = which;
                    }
                })
                .setPositiveButton(Html.fromHtml("<font color='#3399FF'>Choice</font>"), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        record_bufferSize = record_tempBuffer;
                        switch(record_bufferSize) {
                            case 512:
                                btn_record_bufferSize.setText("BUFFER SIZE\n512");
                                Toast.makeText(MainActivity.this, "512로 설정 완료", Toast.LENGTH_SHORT).show();
                                break;
                            case 1024:
                                btn_record_bufferSize.setText("BUFFER SIZE\n1,024");
                                Toast.makeText(MainActivity.this, "1,024로 설정 완료", Toast.LENGTH_SHORT).show();
                                break;
                            case 2048:
                                btn_record_bufferSize.setText("BUFFER SIZE\n2,048");
                                Toast.makeText(MainActivity.this, "2,048로 설정 완료", Toast.LENGTH_SHORT).show();
                                break;
                        }
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

    public void play_type() {
//        myLog.d("method activate");

        final String[] type = new String[] {"MUSIC", "VOICE"};
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar);
        builder.setIcon(getDrawable(R.drawable.ic_baseline_type))
                .setTitle("Play Type")
                .setCancelable(false)
                .setSingleChoiceItems(type, play_type_index, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (type[which].equals("MUSIC")) {
                            play_tempType = AudioManager.STREAM_MUSIC;
                        } else {
                            play_tempType = AudioManager.STREAM_VOICE_CALL;
                        }
                        play_type_index = which;
                    }
                })
                .setPositiveButton(Html.fromHtml("<font color='#3399FF'>Choice</font>"), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        play_type = play_tempType;
                        if (play_type == AudioManager.STREAM_MUSIC) {
                            btn_play_type.setText("SOURCE\nMUSIC");
                            Toast.makeText(MainActivity.this, "MUSIC으로 설정 완료", Toast.LENGTH_SHORT).show();
                        } else {
                            btn_play_type.setText("SOURCE\nVOICE");
                            Toast.makeText(MainActivity.this, "VOICE로 설정 완료", Toast.LENGTH_SHORT).show();
                        }
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

    public void play_channel() {
//        myLog.d("method activate");

        final String[] channel = new String[] {"MONO", "STEREO"};
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar);
        builder.setIcon(getDrawable(R.drawable.ic_baseline_play_channel))
                .setTitle("Sample Rate")
                .setCancelable(false)
                .setSingleChoiceItems(channel, play_channel_index, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (channel[which].equals("MONO")) {
                            play_tempChannel = AudioFormat.CHANNEL_OUT_MONO;
                        } else {
                            play_tempChannel = AudioFormat.CHANNEL_OUT_STEREO;
                        }
                        play_channel_index = which;
                    }
                })
                .setPositiveButton(Html.fromHtml("<font color='#3399FF'>Choice</font>"), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        play_channel = play_tempChannel;
                        if (play_channel == AudioFormat.CHANNEL_OUT_MONO) {
                            btn_play_channel.setText("CHANNEL\nMONO");
                            Toast.makeText(MainActivity.this, "MONO로 설정 완료", Toast.LENGTH_SHORT).show();
                        } else {
                            btn_play_channel.setText("CHANNEL\nSTEREO");
                            Toast.makeText(MainActivity.this, "STEREO로 설정 완료", Toast.LENGTH_SHORT).show();
                        }
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

        final String[] freqArray = new String[] {"8,000", "11,025", "16,000", "22,050", "44,100"};
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar);
        builder.setIcon(getDrawable(R.drawable.ic_baseline_play_samplerate))
                .setTitle("Sample Rate")
                .setCancelable(false)
                .setSingleChoiceItems(freqArray, play_sampleRate_index, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch(freqArray[which]) {
                            case "8,000":
                                play_tempRate = 8000;
                                break;
                            case "11,025":
                                play_tempRate = 11025;
                                break;
                            case "16,000":
                                play_tempRate = 16000;
                                break;
                            case "22,050":
                                play_tempRate = 22050;
                                break;
                            case "44,100":
                                play_tempRate = 44100;
                                break;
                        }
                        play_sampleRate_index = which;
                    }
                })
                .setPositiveButton(Html.fromHtml("<font color='#3399FF'>Choice</font>"), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        play_sampleRate = play_tempRate;
                        playHandler.removeMessages(0);
                        switch(play_sampleRate) {
                            case 8000:
                                btn_play_sampleRate.setText("SAMPLE RATE\n8,000");
                                Toast.makeText(MainActivity.this, "8,000으로 설정 완료", Toast.LENGTH_SHORT).show();
                                break;
                            case 11025:
                                btn_play_sampleRate.setText("SAMPLE RATE\n11,025");
                                Toast.makeText(MainActivity.this, "11,025으로 설정 완료", Toast.LENGTH_SHORT).show();
                                break;
                            case 16000:
                                btn_play_sampleRate.setText("SAMPLE RATE\n16,000");
                                Toast.makeText(MainActivity.this, "16,000으로 설정 완료", Toast.LENGTH_SHORT).show();
                                break;
                            case 22050:
                                btn_play_sampleRate.setText("SAMPLE RATE\n22,050");
                                Toast.makeText(MainActivity.this, "22,050으로 설정 완료", Toast.LENGTH_SHORT).show();
                                break;
                            case 44100:
                                btn_play_sampleRate.setText("SAMPLE RATE\n44,100");
                                Toast.makeText(MainActivity.this, "44,100으로 설정 완료", Toast.LENGTH_SHORT).show();
                                break;
                        }
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

    public void play_volume() {
        View innerView = getLayoutInflater().inflate(R.layout.seekbar, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar);
        builder.setIcon(getDrawable(R.drawable.ic_baseline_type))
                .setTitle("Volume")
                .setView(innerView);

        seekBar_volume = (SeekBar) innerView.findViewById(R.id.seekbar_volume);
        seekBar_action();

        Dialog dialog = builder.create();
        dialog.getWindow().setGravity(Gravity.CENTER);
        dialog.show();
    }

    private void seekBar_action() {

        final AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        int nMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int nCurrentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

        seekBar_volume.setMin(1);
        seekBar_volume.setMax(nMax);
        seekBar_volume.setProgress(nCurrentVolume);
        seekBar_volume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // TODO Auto-generated method stub
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
                btn_play_volume.setText("VOLUME\n" + String.valueOf(progress));
//                myLog.d(String.valueOf(progress));
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }
        });

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

                playHandler.removeMessages(0);
                autoStopHandler.removeMessages(0);
            }
            autoStopHandler.sendEmptyMessage(0);
        }
    };

}