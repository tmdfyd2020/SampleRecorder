package org.techtown.samplerecorder;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.visualizer.amplitude.AudioRecordView;

import org.techtown.samplerecorder.List.ListActivity;
import org.techtown.samplerecorder.Main.AudioRecord;
import org.techtown.samplerecorder.Main.AudioTrack;
import org.techtown.samplerecorder.Main.Queue;

import lib.kingja.switchbutton.SwitchMultiButton;

public class Old extends AppCompatActivity {

    private final String TAG = this.getClass().getSimpleName();

    final int MESSAGE_RECORD = 1;
    final int MESSAGE_PLAY = 2;

    public static boolean isRecording = false;
    public static boolean isPlaying = false;
    public static boolean autoStop = false;

    public static String filePath = "";

    private AudioRecord mAudioRecord;
    private AudioTrack mAudioTrack;
    private AudioRecordView view_waveform;
    private SwitchMultiButton switchButton;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private Queue queue;
    public Button btn_record, btnSource, btnRecordChannel, btnRecordRate, btnBufferSize;
    public Button btn_play, btnType, btnPlayChannel, btnPlayRate, btnVolume;
    private ImageView img_recording, img_playing;
    private TextView text_timer;
    private VolumeContentObserver volumeObserver;
    private long startTime;
    public static int source, recordChannel, recordRate, bufferSize, type, playChannel, playRate, volumeType;
    private boolean fileDrop;

    private DialogService dialogService;
    private LogUtil.Companion mLog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        permissionCheck();
        initUi();
        dialogService = new DialogService(this);
        mLog = LogUtil.Companion;
        filePath = getFilesDir().getAbsolutePath();
    }

    @SuppressLint("SetTextI18n")
    public void initUi() {
        mAudioTrack = new AudioTrack();

        Toolbar toolbar_main = findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar_main);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        img_recording = findViewById(R.id.img_recording);
        img_playing = findViewById(R.id.img_playing);
        view_waveform = findViewById(R.id.view_waveForm);
        text_timer = findViewById(R.id.text_timer);

        btnSource = findViewById(R.id.btn_record_source);
        btnType = findViewById(R.id.btn_play_type);
        btnRecordChannel = findViewById(R.id.btn_record_channel);
        btnPlayChannel = findViewById(R.id.btn_play_channel);
        btnRecordRate = findViewById(R.id.btn_record_sampleRate);
        btnPlayRate = findViewById(R.id.btn_play_sampleRate);
        btnBufferSize = findViewById(R.id.btn_record_bufferSize);

        btnSource.setText(getString(R.string.source) + "\n" + getString(R.string.mic));
        btnType.setText(getString(R.string.type) + "\n" + getString(R.string.media));
        btnRecordChannel.setText(getString(R.string.channel) + "\n" + getString(R.string.mono));
        btnPlayChannel.setText(getString(R.string.channel) + "\n" + getString(R.string.mono));
        btnRecordRate.setText(getString(R.string.rate) + "\n" + getString(R.string.rate_16000));
        btnPlayRate.setText(getString(R.string.rate) + "\n" + getString(R.string.rate_16000));
        btnBufferSize.setText(getString(R.string.buffer_size) + "\n" + getString(R.string.buffer_size_1024));

        btn_record = findViewById(R.id.btn_record);
        btn_play = findViewById(R.id.btn_play);
        btn_play.setEnabled(false);

        final AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        int nCurrentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        btnVolume = findViewById(R.id.btn_play_volume);
        btnVolume.setText(getString(R.string.volume) + "\n" + nCurrentVolume);

        switchButton = findViewById(R.id.switchButton);
        switchButton.setOnSwitchListener(new SwitchMultiButton.OnSwitchListener() {
            @Override
            public void onSwitch(int position, String tabText) {

                if (position == 0) {  // for file drop on
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
        volumeObserver = new VolumeContentObserver(this, new Handler());
        this.getContentResolver().registerContentObserver(
                android.provider.Settings.System.CONTENT_URI,
                true,
                volumeObserver
        );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getApplicationContext().getContentResolver().unregisterContentObserver(volumeObserver);
    }

    public void permissionCheck() {
        int permission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        if (permission != PackageManager.PERMISSION_GRANTED) {  // 권한이 있을 때
            ActivityCompat.requestPermissions(this,
                    new String[] {Manifest.permission.RECORD_AUDIO},101);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_main_toolbar, menu);
        return true;
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_exit:
                dialogService.create("", getString(R.string.exit));
                break;

            case R.id.list_play:
                Intent intent = new Intent(this, ListActivity.class);
                intent.putExtra(getString(R.string.rate), playRate);
                intent.putExtra(getString(R.string.buffer_size), bufferSize);
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                break;
        }
        return true;
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_record:
                record();
                break;
            case R.id.btn_play:
                play();
                break;
            case R.id.btn_record_source:
                dialogService.create("", getString(R.string.source));
                break;
            case R.id.btn_record_channel:
                dialogService.create(getString(R.string.record), getString(R.string.channel));
                break;
            case R.id.btn_record_sampleRate:
                dialogService.create(getString(R.string.record), getString(R.string.rate));
                break;
            case R.id.btn_record_bufferSize:
                dialogService.create("", getString(R.string.buffer_size));
                break;
            case R.id.btn_play_type:
                dialogService.create("", getString(R.string.type));
                break;
            case R.id.btn_play_channel:
                dialogService.create(getString(R.string.play), getString(R.string.channel));
                break;
            case R.id.btn_play_sampleRate:
                dialogService.create(getString(R.string.play), getString(R.string.rate));
                break;
            case R.id.btn_play_volume:
                dialogService.create("", getString(R.string.volume));
                break;
        }
    }

    public void record() {
        if (isRecording) {  // if "STOP" button clicked,
            isRecording = false;  // check : 함수 안으로 집어 넣으면 AudioRecord로 isRecording이 가끔씩 전달되지 않음
            mAudioRecord.stop();
            mAudioRecord.release(this, fileDrop);
            stopRecording();
        } else {  // if "RECORD" button clicked,
            mAudioRecord = new AudioRecord();
            queue = new Queue();
            isRecording = true;
            mAudioRecord.init(bufferSize);
            mAudioRecord.start(source, recordChannel, recordRate, queue, fileDrop);
            mLog.i(TAG, String.valueOf(source));
            startRecording();
        }
    }

    public void stopRecording() {
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
        view_waveform.recreate();
        view_waveform.setChunkColor(getResources().getColor(R.color.record_red));

        startTime = SystemClock.elapsedRealtime();
//        Message recordMsg = recordHandler.obtainMessage();
////        Message recordMsg = new Message();
//        recordMsg.what = MESSAGE_RECORD;
        Message recordMsg = recordHandler.obtainMessage();
        recordMsg.what = MESSAGE_RECORD;
        recordHandler.sendMessage(recordMsg);

        btn_record.setText("Stop");
        btnBufferSize.setEnabled(true);
        img_recording.setVisibility(View.VISIBLE);
        text_timer.setVisibility(View.VISIBLE);

        setAnimation(img_recording, btn_record);
    }

    public void play() {
        if (isPlaying) {  // if "STOP" button clicked,
            isPlaying = false;
            mAudioTrack.stop();
            mAudioTrack.release();
            stopPlaying();
        } else {  // if "PLAY" button clicked,
            mAudioTrack = new AudioTrack();
            isPlaying = true;
            mAudioTrack.init(bufferSize);
            mAudioTrack.play(type, playChannel, playRate, queue);
            startPlaying();
        }
    }

    public void stopPlaying() {
        playHandler.removeMessages(0);

        img_playing.clearAnimation();
        img_playing.setVisibility(View.INVISIBLE);
        btn_record.setEnabled(true);
        btn_play.clearAnimation();
        btn_play.setText("Play");
    }

    public void startPlaying() {
        // use this emerging bug like delay 300 at first play
//        if (first_track) {
//            try {
//                Thread.sleep(300);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            first_track = false;
//        }

        view_waveform.recreate();
        view_waveform.setChunkColor(getResources().getColor(R.color.play_blue));

        startTime = SystemClock.elapsedRealtime();
        Message playMsg = playHandler.obtainMessage();
        playMsg.what = MESSAGE_PLAY;
        playHandler.sendMessage(playMsg);

        img_playing.setVisibility(View.VISIBLE);
        btn_record.setEnabled(false);
        btn_play.setText("Stop");

        setAnimation(img_playing, btn_play);
    }

    private void setAnimation(ImageView imageView, Button button) {
        Animation animation = new AlphaAnimation(1, 0);
        animation.setDuration(500);
        animation.setInterpolator(new LinearInterpolator());
        animation.setRepeatCount(Animation.INFINITE);
        animation.setRepeatMode(Animation.REVERSE);
        imageView.startAnimation(animation);
        button.startAnimation(animation);
    }

    public String getTime() {
        long nowTime = SystemClock.elapsedRealtime();
        long overTime = nowTime - startTime;

        long min = overTime / 1000 / 60;
        long sec = (overTime / 1000) % 60;
        long mSec = overTime % 1000 / 10;

        @SuppressLint("DefaultLocale") String timeText = String.format("%02d : %02d : %02d", min, sec, mSec);

        return timeText;
    }

    Handler recordHandler = new Handler() {
        @SuppressLint("HandlerLeak")
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
            if (!autoStop) {
                text_timer.setText(getTime());
                view_waveform.update(AudioTrack.dataMax);
                playHandler.sendEmptyMessage(0);
            } else {
//                myLog.d("autoStop 발생!");

                autoStop = false;
                isPlaying = false;
                mAudioTrack.stop();
                mAudioTrack.release();

                img_playing.clearAnimation();
                img_playing.setVisibility(View.INVISIBLE);
                btn_record.setEnabled(true);
                btn_play.clearAnimation();
                btn_play.setText("Play");

                playHandler.removeMessages(0);
            }
        }
    };
}
