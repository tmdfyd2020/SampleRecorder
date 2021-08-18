package org.techtown.samplerecorder.Main;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.media.AudioAttributes;
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

import org.techtown.samplerecorder.List;
import org.techtown.samplerecorder.R;

import lib.kingja.switchbutton.SwitchMultiButton;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    final int SAMPLE_RATE_8000 = 8000;
    final int SAMPLE_RATE_11025 = 11025;
    final int SAMPLE_RATE_16000 = 16000;
    final int SAMPLE_RATE_22050 = 22050;
    final int SAMPLE_RATE_44100 = 44100;
    final int BUFFER_SIZE_512 = 512;
    final int BUFFER_SIZE_1024 = 1024;
    final int BUFFER_SIZE_2048 = 2048;
    final int MESSAGE_RECORD = 1;
    final int MESSAGE_PLAY = 2;

    public static boolean isRecording = false;
    public static boolean isPlaying = false;
    public static boolean fileDrop;
    public static boolean autoStop = false;

    private AudioRecord myAudioRecord;
    private AudioTrack myAudioTrack;
    private AudioRecordView view_waveform;
    private SwitchMultiButton switchButton;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private Context context;
    private Queue queue;
    private Button btn_record, btn_record_source, btn_record_channel, btn_record_sampleRate, btn_record_bufferSize;
    private Button btn_play, btn_play_type, btn_play_channel, btn_play_sampleRate, btn_play_volume;
    private ImageView img_recording, img_playing, img_seekbar;
    private TextView text_timer, text_seekbar;
    private SeekBar seekBar_volume;
    private SettingsContentObserver observer;
    private long startTime;
    private int record_source, record_source_index, record_channel, record_channel_index, record_sampleRate, record_sampleRate_index,
            record_bufferSize, record_bufferSize_index, play_type, volume_type, play_type_index,
            play_channel, play_channel_index, play_sampleRate, play_sampleRate_index;

    Message message;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        permissionCheck();
        init_constant();
        init();
    }

    public void init_constant() {
        record_source = MediaRecorder.AudioSource.MIC;
        record_source_index = 1;
        record_channel = AudioFormat.CHANNEL_IN_MONO;
        record_channel_index = 0;
        record_sampleRate = 16000;
        record_sampleRate_index = 2;
        record_bufferSize = 1024;
        record_bufferSize_index = 1;

        play_type = AudioAttributes.USAGE_MEDIA;
        play_type_index = 1;
        volume_type = AudioManager.STREAM_MUSIC;
        setVolumeControlStream(volume_type);
        play_channel = AudioFormat.CHANNEL_OUT_MONO;
        play_channel_index = 0;
        play_sampleRate = 16000;
        play_sampleRate_index = 2;
    }

    public void init() {
//        myLog.d("method activate");

        myAudioTrack = new AudioTrack();

        context = getApplicationContext();

        Toolbar toolbar_main = findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar_main);
        getSupportActionBar().setTitle("음성 녹음");

        img_recording = findViewById(R.id.img_recording);
        view_waveform = findViewById(R.id.view_waveForm);
        text_timer = findViewById(R.id.text__timer);
        btn_record_source = findViewById(R.id.btn_record_source);
        btn_record_source.setOnClickListener(this);
        btn_record_channel = findViewById(R.id.btn_record_channel);
        btn_record_channel.setOnClickListener(this);
        btn_record_sampleRate = findViewById(R.id.btn_record_sampleRate);
        btn_record_sampleRate.setOnClickListener(this);
        btn_record_bufferSize = findViewById(R.id.btn_record_bufferSize);
        btn_record_bufferSize.setOnClickListener(this);
        btn_record = findViewById(R.id.btn_record);
        btn_record.setOnClickListener(this);

        img_playing = findViewById(R.id.img_playing);
        btn_play_type = findViewById(R.id.btn_play_type);
        btn_play_type.setOnClickListener(this);
        btn_play_channel = findViewById(R.id.btn_play_channel);
        btn_play_channel.setOnClickListener(this);
        btn_play_sampleRate = findViewById(R.id.btn_play_sampleRate);
        btn_play_sampleRate.setOnClickListener(this);
        btn_play_volume = findViewById(R.id.btn_play_volume);
        btn_play_volume.setOnClickListener(this);
        final AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        int nCurrentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        btn_play_volume.setText("VOLUME\n" + String.valueOf(nCurrentVolume));
        btn_play = findViewById(R.id.btn_play);
        btn_play.setOnClickListener(this);
        btn_play.setEnabled(false);

        switchButton = findViewById(R.id.switchButton);
        switchButton.setOnSwitchListener(new SwitchMultiButton.OnSwitchListener() {
            @Override
            public void onSwitch(int position, String tabText) {

                if (position == 0) {  // for file drop on
                    int permission_writePermission = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
                    if (permission_writePermission == PackageManager.PERMISSION_GRANTED) {  // 권한이 있을 때
                        myLog.d("WRITE_EXTERNAL_STORAGE 권한 설정 완료");
                    } else {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                201);
                    }

                    fileDrop = true;
                    editor.putBoolean("fileState", fileDrop);
                    editor.commit();
                } else if (position == 1) {  // for file drop off
                    fileDrop = false;
                    editor.putBoolean("fileState", fileDrop);
                    editor.commit();
                }
            }
        });

        sharedPreferences = getSharedPreferences("fileDrop", MODE_PRIVATE);
        editor = sharedPreferences.edit();
        fileDrop = sharedPreferences.getBoolean("fileState", false);

        if (fileDrop) {
            switchButton.setSelectedTab(0);
        } else {
            switchButton.setSelectedTab(1);
        }

        // real time volume change listener
        observer = new SettingsContentObserver(new Handler());
        this.getApplicationContext().getContentResolver().registerContentObserver(
                android.provider.Settings.System.CONTENT_URI,
                true,
                observer
        );
    }

    public class SettingsContentObserver extends ContentObserver {
        AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        public SettingsContentObserver(Handler handler) {
            super(handler);
        }

        @Override
        public boolean deliverSelfNotifications() {
            return super.deliverSelfNotifications();
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            btn_play_volume.setText("VOLUME\n" + String.valueOf(audioManager.getStreamVolume(volume_type)));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getApplicationContext().getContentResolver().unregisterContentObserver(observer);
    }

    public void permissionCheck() {
//        myLog.d("method activate");

        int permission_record = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);

        if (permission_record == PackageManager.PERMISSION_GRANTED) {  // 권한이 있을 때
            myLog.d("RECORD_AUDIO 권한 설정 완료");
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    101);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_main_toolbar, menu);
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

            case R.id.list_play:
                int permission_readPermission = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE);
                if (permission_readPermission == PackageManager.PERMISSION_GRANTED) {  // 권한이 있을 때
                    myLog.d("READ_EXTERNAL_STORAGE 권한 설정 완료");
                } else {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            301);
                }

                Intent intent = new Intent(this, List.class);
                intent.putExtra("sampleRate", play_sampleRate);
                intent.putExtra("bufferSize", record_bufferSize);
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
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
            myAudioRecord.release(context);
            isRecording = false;  // check : 함수 안으로 집어 넣으면 AudioRecord로 isRecording이 가끔씩 전달되지 않음
            stopRecording();
        } else {  // if "RECORD" button clicked,
            myAudioRecord = new AudioRecord();
            queue = new Queue();
            myAudioRecord.init(record_bufferSize);
            myAudioRecord.start(record_source, record_channel, record_sampleRate, queue);
            myLog.d("MainActivity record sample rate : " + String.valueOf(record_sampleRate));
            isRecording = true;
            startRecording();
        }
    }

    public void stopRecording() {
//        myLog.d("method activate");

        recordHandler.removeMessages(0);

        img_recording.clearAnimation();
        img_recording.setVisibility(View.INVISIBLE);
        btn_record.clearAnimation();
        btn_record.setText("Record");
        btn_record.setEnabled(true);
        btn_record.setBackground(getDrawable(R.drawable.btn_record_active));
        btn_play.setEnabled(true);
    }

    public void startRecording() {
//        myLog.d("method activate");

        view_waveform.recreate();
        view_waveform.setChunkColor(getResources().getColor(R.color.record_red));

        startTime = SystemClock.elapsedRealtime();
//        Message recordMsg = recordHandler.obtainMessage();
////        Message recordMsg = new Message();
//        recordMsg.what = MESSAGE_RECORD;
        message = recordHandler.obtainMessage();
        message.what = MESSAGE_RECORD;
        recordHandler.sendMessage(message);

        btn_record.setText("Stop");
        btn_record_bufferSize.setEnabled(true);
        img_recording.setVisibility(View.VISIBLE);
        text_timer.setVisibility(View.VISIBLE);

        // animation set at recording image emerge
        Animation animation = new AlphaAnimation(1, 0);
        animation.setDuration(500);
        animation.setInterpolator(new LinearInterpolator());
        animation.setRepeatCount(Animation.INFINITE);
        animation.setRepeatMode(Animation.REVERSE);
        img_recording.startAnimation(animation);
        btn_record.startAnimation(animation);
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
            myAudioTrack.init(record_bufferSize);
            myAudioTrack.play(play_type, play_channel, play_sampleRate, queue);
            startPlaying();
        }
    }

    public void stopPlaying() {
//        myLog.d("method activate");

        playHandler.removeMessages(0);

        img_playing.clearAnimation();
        img_playing.setVisibility(View.INVISIBLE);
        btn_record.setEnabled(true);
        btn_play.clearAnimation();
        btn_play.setText("Play");
    }

    public void startPlaying() {
//        myLog.d("method activate");

        view_waveform.recreate();
        view_waveform.setChunkColor(getResources().getColor(R.color.play_blue));

        startTime = SystemClock.elapsedRealtime();
        Message playMsg = playHandler.obtainMessage();
        playMsg.what = MESSAGE_PLAY;
        playHandler.sendMessage(playMsg);

        img_playing.setVisibility(View.VISIBLE);
        btn_record.setEnabled(false);
        btn_play.setText("Stop");

        Animation animation = new AlphaAnimation(1, 0);
        animation.setDuration(500);
        animation.setInterpolator(new LinearInterpolator());
        animation.setRepeatCount(Animation.INFINITE);
        animation.setRepeatMode(Animation.REVERSE);
        img_playing.startAnimation(animation);
        btn_play.startAnimation(animation);
    }

    public void record_source() {
//        myLog.d("method activate");

        final String[] source = new String[]{"DEFAULT", "MIC", "VOICE COMMUNICATION", "VOICE PERFORMANCE", "VOICE RECOGNITION", "CAMCORDER", "UNPROCESSED"};
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar);
        builder.setIcon(getDrawable(R.drawable.ic_baseline_source_icon))
                .setTitle("Source")
                .setSingleChoiceItems(source, record_source_index, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (source[which]) {
                            case "DEFAULT":  // Default audio source *
                                record_source = MediaRecorder.AudioSource.DEFAULT;
                                btn_record_source.setText("SOURCE\nDEFAULT");
                                break;
                            case "MIC":  // Microphone audio source
                                record_source = MediaRecorder.AudioSource.MIC;
                                btn_record_source.setText("SOURCE\nMIC");
                                break;
                            case "VOICE COMMUNICATION":  // Microphone audio source tuned for voice communications such as VoIP.
                                record_source = MediaRecorder.AudioSource.VOICE_COMMUNICATION;
                                btn_record_source.setText("SOURCE\nVOICE COMMUNICATION");
                                break;
                            case "VOICE PERFORMANCE":  // Source for capturing audio meant to be processed in real time and played back for live performance (e.g karaoke).
                                record_source = MediaRecorder.AudioSource.VOICE_PERFORMANCE;
                                btn_record_source.setText("SOURCE\nVOICE PERFORMANCE");
                                break;
                            case "VOICE RECOGNITION":  // Microphone audio source tuned for voice recognition.
                                record_source = MediaRecorder.AudioSource.VOICE_RECOGNITION;
                                btn_record_source.setText("SOURCE\nVOICE RECOGNITION");
                                break;
                            case "CAMCORDER":  // Microphone audio source tuned for voice recognition.
                                record_source = MediaRecorder.AudioSource.CAMCORDER;
                                btn_record_source.setText("SOURCE\nCAMCORDER");
                                break;
                            case "UNPROCESSED":  // Microphone audio source tuned for voice recognition.
                                record_source = MediaRecorder.AudioSource.UNPROCESSED;
                                btn_record_source.setText("SOURCE\nUNPROCESSED");
                                break;
                        }
                        record_source_index = which;
                    }
                })
                .setPositiveButton(Html.fromHtml("<font color='#3399FF'>Choice</font>"), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (record_source) {
                            case MediaRecorder.AudioSource.DEFAULT:
                                btn_record_source.setText("SOURCE\nDEFAULT");
                                Toast.makeText(MainActivity.this, "DEFAULT로 설정 완료", Toast.LENGTH_SHORT).show();
                                break;
                            case MediaRecorder.AudioSource.MIC:
                                btn_record_source.setText("SOURCE\nMIC");
                                Toast.makeText(MainActivity.this, "MIC로 설정 완료", Toast.LENGTH_SHORT).show();
                                break;
                            case MediaRecorder.AudioSource.VOICE_COMMUNICATION:
                                btn_record_source.setText("SOURCE\nVOICE COMMUNICATION");
                                Toast.makeText(MainActivity.this, "VOICE COMMUNICATION으로 설정 완료", Toast.LENGTH_SHORT).show();
                                break;
                            case MediaRecorder.AudioSource.VOICE_PERFORMANCE:
                                btn_record_source.setText("SOURCE\nVOICE PERFORMANCE");
                                Toast.makeText(MainActivity.this, "VOICE PERFORMANCE으로 설정 완료", Toast.LENGTH_SHORT).show();
                                break;
                            case MediaRecorder.AudioSource.VOICE_RECOGNITION:
                                btn_record_source.setText("SOURCE\nVOICE RECOGNITION");
                                Toast.makeText(MainActivity.this, "VOICE RECOGNITION으로 설정 완료", Toast.LENGTH_SHORT).show();
                                break;
                            case MediaRecorder.AudioSource.CAMCORDER:
                                btn_record_source.setText("SOURCE\nCAMCORDER");
                                Toast.makeText(MainActivity.this, "CAMCORDER로 설정 완료", Toast.LENGTH_SHORT).show();
                                break;
                            case MediaRecorder.AudioSource.UNPROCESSED:
                                btn_record_source.setText("SOURCE\nUNPROCESSED");
                                Toast.makeText(MainActivity.this, "UNPROCESSED로 설정 완료", Toast.LENGTH_SHORT).show();
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

    public void record_channel() {
//        myLog.d("method activate");

        final String[] channel = new String[]{"MONO", "STEREO"};
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar);
        builder.setIcon(getDrawable(R.drawable.ic_baseline_record_channel))
                .setTitle("Channel")
                .setSingleChoiceItems(channel, record_channel_index, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (channel[which].equals("MONO")) {
                            record_channel = AudioFormat.CHANNEL_IN_MONO;
                            btn_record_channel.setText("CHANNEL\nMONO");
                        } else {
                            record_channel = AudioFormat.CHANNEL_IN_STEREO;
                            btn_record_channel.setText("CHANNEL\nSTEREO");
                        }
                        record_channel_index = which;
                    }
                })
                .setPositiveButton(Html.fromHtml("<font color='#3399FF'>Choice</font>"), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
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

        final String[] sampleRate = new String[]{"8,000", "11,025", "16,000", "22,050", "44,100"};
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar);
        builder.setIcon(getDrawable(R.drawable.ic_baseline_record_samplerate))
                .setTitle("Sample Rate")
                .setSingleChoiceItems(sampleRate, record_sampleRate_index, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (sampleRate[which]) {
                            case "8,000":
                                record_sampleRate = SAMPLE_RATE_8000;
                                btn_record_sampleRate.setText("SAMPLE RATE\n8,000");
                                break;
                            case "11,025":
                                record_sampleRate = SAMPLE_RATE_11025;
                                btn_record_sampleRate.setText("SAMPLE RATE\n11,025");
                                break;
                            case "16,000":
                                record_sampleRate = SAMPLE_RATE_16000;
                                btn_record_sampleRate.setText("SAMPLE RATE\n16,000");
                                break;
                            case "22,050":
                                record_sampleRate = SAMPLE_RATE_22050;
                                btn_record_sampleRate.setText("SAMPLE RATE\n22,050");
                                break;
                            case "44,100":
                                record_sampleRate = SAMPLE_RATE_44100;
                                btn_record_sampleRate.setText("SAMPLE RATE\n44,100");
                                break;
                        }
                        record_sampleRate_index = which;
                    }
                })
                .setPositiveButton(Html.fromHtml("<font color='#3399FF'>Choice</font>"), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (record_sampleRate) {
                            case SAMPLE_RATE_8000:
                                btn_record_sampleRate.setText("SAMPLE RATE\n8,000");
                                Toast.makeText(MainActivity.this, "8,000으로 설정 완료", Toast.LENGTH_SHORT).show();
                                break;
                            case SAMPLE_RATE_11025:
                                btn_record_sampleRate.setText("SAMPLE RATE\n11,025");
                                Toast.makeText(MainActivity.this, "11,025으로 설정 완료", Toast.LENGTH_SHORT).show();
                                break;
                            case SAMPLE_RATE_16000:
                                btn_record_sampleRate.setText("SAMPLE RATE\n16,000");
                                Toast.makeText(MainActivity.this, "16,000으로 설정 완료", Toast.LENGTH_SHORT).show();
                                break;
                            case SAMPLE_RATE_22050:
                                btn_record_sampleRate.setText("SAMPLE RATE\n22,050");
                                Toast.makeText(MainActivity.this, "22,050으로 설정 완료", Toast.LENGTH_SHORT).show();
                                break;
                            case SAMPLE_RATE_44100:
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

        final String[] bufferSize = new String[]{"512", "1,024", "2,048"};
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar);
        builder.setIcon(getDrawable(R.drawable.ic_baseline_buffersize))
                .setTitle("Buffer Size")
                .setSingleChoiceItems(bufferSize, record_bufferSize_index, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (bufferSize[which]) {
                            case "512":
                                record_bufferSize = BUFFER_SIZE_512;
                                btn_record_bufferSize.setText("BUFFER SIZE\n512");
                                break;
                            case "1,024":
                                record_bufferSize = BUFFER_SIZE_1024;
                                btn_record_bufferSize.setText("BUFFER SIZE\n1,024");
                                break;
                            case "2,048":
                                record_bufferSize = BUFFER_SIZE_2048;
                                btn_record_bufferSize.setText("BUFFER SIZE\n2,048");
                                break;
                        }
                        record_bufferSize_index = which;
                    }
                })
                .setPositiveButton(Html.fromHtml("<font color='#3399FF'>Choice</font>"), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (record_bufferSize) {
                            case BUFFER_SIZE_512:
                                btn_record_bufferSize.setText("BUFFER SIZE\n512");
                                Toast.makeText(MainActivity.this, "512로 설정 완료", Toast.LENGTH_SHORT).show();
                                break;
                            case BUFFER_SIZE_1024:
                                btn_record_bufferSize.setText("BUFFER SIZE\n1,024");
                                Toast.makeText(MainActivity.this, "1,024로 설정 완료", Toast.LENGTH_SHORT).show();
                                break;
                            case BUFFER_SIZE_2048:
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

        final String[] type = new String[]{"RING", "MEDIA", "ALARM", "NOTIFICATION", "SYSTEM"};
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar);
        builder.setIcon(getDrawable(R.drawable.ic_baseline_type))
                .setTitle("Play Type")
                .setSingleChoiceItems(type, play_type_index, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
                        switch (type[which]) {
                            case "RING":
                                play_type = AudioAttributes.USAGE_NOTIFICATION_RINGTONE;
                                btn_play_type.setText("TYPE\nRING");
                                volume_type = AudioManager.STREAM_RING;
                                setVolumeControlStream(volume_type);
                                btn_play_volume.setText("VOLUME\n" + String.valueOf(audioManager.getStreamVolume(volume_type)));
                                // audioManager.getStreamVolume(volume_type);
                                break;
                            case "MEDIA":
                                play_type = AudioAttributes.USAGE_MEDIA;
                                btn_play_type.setText("TYPE\nMEDIA");
                                volume_type = AudioManager.STREAM_MUSIC;
                                setVolumeControlStream(volume_type);
                                btn_play_volume.setText("VOLUME\n" + String.valueOf(audioManager.getStreamVolume(volume_type)));
                                break;
                            case "ALARM":
                                play_type = AudioAttributes.USAGE_ALARM;
                                btn_play_type.setText("TYPE\nALARM");
                                volume_type = AudioManager.STREAM_ALARM;
                                setVolumeControlStream(volume_type);
                                btn_play_volume.setText("VOLUME\n" + String.valueOf(audioManager.getStreamVolume(volume_type)));
                                break;
                            case "NOTIFICATION":
                                play_type = AudioAttributes.USAGE_NOTIFICATION;
                                btn_play_type.setText("TYPE\nNOTIFICATION");
                                volume_type = AudioManager.STREAM_NOTIFICATION;
                                setVolumeControlStream(volume_type);
                                btn_play_volume.setText("VOLUME\n" + String.valueOf(audioManager.getStreamVolume(volume_type)));
                                break;
                            case "SYSTEM":
                                play_type = AudioAttributes.USAGE_ASSISTANCE_SONIFICATION;
                                btn_play_type.setText("TYPE\nSYSTEM");
                                volume_type = AudioManager.STREAM_SYSTEM;
                                setVolumeControlStream(volume_type);
                                btn_play_volume.setText("VOLUME\n" + String.valueOf(audioManager.getStreamVolume(volume_type)));
                                break;
                        }
                        play_type_index = which;
                    }
                })
                .setPositiveButton(Html.fromHtml("<font color='#3399FF'>Choice</font>"), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (play_type) {
                            case AudioAttributes.USAGE_NOTIFICATION_RINGTONE:
                                btn_play_type.setText("TYPE\nRING");
                                Toast.makeText(MainActivity.this, "RING으로 설정 완료", Toast.LENGTH_SHORT).show();
                                break;
                            case AudioAttributes.USAGE_MEDIA:
                                btn_play_type.setText("TYPE\nMEDIA");
                                Toast.makeText(MainActivity.this, "MEDIA로 설정 완료", Toast.LENGTH_SHORT).show();
                                break;
                            case AudioAttributes.USAGE_ALARM:
                                btn_play_type.setText("TYPE\nALARM");
                                Toast.makeText(MainActivity.this, "ALARM으로 설정 완료", Toast.LENGTH_SHORT).show();
                                break;
                            case AudioAttributes.USAGE_NOTIFICATION:
                                btn_play_type.setText("TYPE\nNOTIFICATION");
                                Toast.makeText(MainActivity.this, "NOTIFICATION으로 설정 완료", Toast.LENGTH_SHORT).show();
                                break;
                            case AudioAttributes.USAGE_ASSISTANCE_SONIFICATION:
                                btn_play_type.setText("TYPE\nSYSTEM");
                                Toast.makeText(MainActivity.this, "SYSTEM으로 설정 완료", Toast.LENGTH_SHORT).show();
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

    public void play_channel() {
//        myLog.d("method activate");

        final String[] channel = new String[]{"MONO", "STEREO"};
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar);
        builder.setIcon(getDrawable(R.drawable.ic_baseline_play_channel))
                .setTitle("Sample Rate")
                .setSingleChoiceItems(channel, play_channel_index, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (channel[which].equals("MONO")) {
                            play_channel = AudioFormat.CHANNEL_OUT_MONO;
                            btn_play_channel.setText("CHANNEL\nMONO");
                        } else {
                            play_channel = AudioFormat.CHANNEL_OUT_STEREO;
                            btn_play_channel.setText("CHANNEL\nSTEREO");
                        }
                        play_channel_index = which;
                    }
                })
                .setPositiveButton(Html.fromHtml("<font color='#3399FF'>Choice</font>"), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
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

        final String[] freqArray = new String[]{"8,000", "11,025", "16,000", "22,050", "44,100"};
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar);
        builder.setIcon(getDrawable(R.drawable.ic_baseline_play_samplerate))
                .setTitle("Sample Rate")
                .setSingleChoiceItems(freqArray, play_sampleRate_index, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (freqArray[which]) {
                            case "8,000":
                                play_sampleRate = SAMPLE_RATE_8000;
                                btn_play_sampleRate.setText("SAMPLE RATE\n8,000");
                                break;
                            case "11,025":
                                play_sampleRate = SAMPLE_RATE_11025;
                                btn_play_sampleRate.setText("SAMPLE RATE\n11,025");
                                break;
                            case "16,000":
                                play_sampleRate = SAMPLE_RATE_16000;
                                btn_play_sampleRate.setText("SAMPLE RATE\n16,000");
                                break;
                            case "22,050":
                                play_sampleRate = SAMPLE_RATE_22050;
                                btn_play_sampleRate.setText("SAMPLE RATE\n22,050");
                                break;
                            case "44,100":
                                play_sampleRate = SAMPLE_RATE_44100;
                                btn_play_sampleRate.setText("SAMPLE RATE\n44,100");
                                break;
                        }
                        play_sampleRate_index = which;
                    }
                })
                .setPositiveButton(Html.fromHtml("<font color='#3399FF'>Choice</font>"), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (play_sampleRate) {
                            case SAMPLE_RATE_8000:
                                btn_play_sampleRate.setText("SAMPLE RATE\n8,000");
                                Toast.makeText(MainActivity.this, "8,000으로 설정 완료", Toast.LENGTH_SHORT).show();
                                break;
                            case SAMPLE_RATE_11025:
                                btn_play_sampleRate.setText("SAMPLE RATE\n11,025");
                                Toast.makeText(MainActivity.this, "11,025으로 설정 완료", Toast.LENGTH_SHORT).show();
                                break;
                            case SAMPLE_RATE_16000:
                                btn_play_sampleRate.setText("SAMPLE RATE\n16,000");
                                Toast.makeText(MainActivity.this, "16,000으로 설정 완료", Toast.LENGTH_SHORT).show();
                                break;
                            case SAMPLE_RATE_22050:
                                btn_play_sampleRate.setText("SAMPLE RATE\n22,050");
                                Toast.makeText(MainActivity.this, "22,050으로 설정 완료", Toast.LENGTH_SHORT).show();
                                break;
                            case SAMPLE_RATE_44100:
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
        builder.setTitle("Volume")
                .setView(innerView);

        seekBar_volume = (SeekBar) innerView.findViewById(R.id.seekbar_volume);
        text_seekbar = (TextView) innerView.findViewById(R.id.text_seekbar);
        img_seekbar = (ImageView) innerView.findViewById(R.id.img_seekbar);
        seekBar_action();

        Dialog dialog = builder.create();
        dialog.getWindow().setGravity(Gravity.CENTER);
        dialog.show();
    }

    private void seekBar_action() {

        final AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        int nMin = 1;
        int nMax = audioManager.getStreamMaxVolume(volume_type);
        int nCurrentVolume = audioManager.getStreamVolume(volume_type);

        seekBar_volume.setMin(nMin);
        seekBar_volume.setMax(nMax);
        seekBar_volume.setProgress(nCurrentVolume);

        if (nCurrentVolume >= 13) {
            text_seekbar.setTextColor(getResources().getColor(R.color.record_red));
            img_seekbar.setImageResource(R.drawable.png_volume_loud);
        } else if (nCurrentVolume >= 10 && nCurrentVolume < 13) {
            text_seekbar.setTextColor(getResources().getColor(R.color.play_blue));
            img_seekbar.setImageResource(R.drawable.png_volume_loud);
        } else {
            text_seekbar.setTextColor(getResources().getColor(R.color.play_blue));
            img_seekbar.setImageResource(R.drawable.png_volume_small);
        }
        text_seekbar.setText(String.valueOf(nCurrentVolume));

        seekBar_volume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // TODO Auto-generated method stub
                audioManager.setStreamVolume(volume_type, progress, 0);
                if (progress >= 13) {
                    text_seekbar.setTextColor(getResources().getColor(R.color.record_red));
                    img_seekbar.setImageResource(R.drawable.png_volume_loud);
                } else if (progress >= 10 && progress < 13) {
                    text_seekbar.setTextColor(getResources().getColor(R.color.play_blue));
                    img_seekbar.setImageResource(R.drawable.png_volume_loud);
                } else {
                    text_seekbar.setTextColor(getResources().getColor(R.color.play_blue));
                    img_seekbar.setImageResource(R.drawable.png_volume_small);
                }
                text_seekbar.setText(String.valueOf(progress));
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

    public String getTime() {
        long nowTime = SystemClock.elapsedRealtime();
        long overTime = nowTime - startTime;

        long min = overTime / 1000 / 60;
        long sec = (overTime / 1000) % 60;
        long msec = overTime % 1000 / 10;

        String timeText = String.format("%02d : %02d : %02d", min, sec, msec);

        return timeText;
    }

    Handler recordHandler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            text_timer.setText(getTime());
            view_waveform.update(AudioRecord.dataMax);
            recordHandler.sendEmptyMessage(0);
        }
    };

    Handler playHandler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            if (autoStop) {
//                myLog.d("autoStop 발생!");

                autoStop = false;
                isPlaying = false;
                myAudioTrack.stop();
                myAudioTrack.release();

                img_playing.clearAnimation();
                img_playing.setVisibility(View.INVISIBLE);
                btn_record.setEnabled(true);
                btn_play.clearAnimation();
                btn_play.setText("Play");

                playHandler.removeMessages(0);
            } else {
                text_timer.setText(getTime());
                view_waveform.update(AudioTrack.dataMax);

                playHandler.sendEmptyMessage(0);
            }
        }
    };


}