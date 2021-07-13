package org.techtown.samplerecorder;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class SaveInFile extends AppCompatActivity {

    private static final String TAG = "[MAIN]";

    static final int AUDIO_SOURCE = MediaRecorder.AudioSource.MIC; // for raw audio, use MediaRecorder.AudioSource.UNPROCESSED, see note in MediaRecorder section
    static final int SAMPLE_RATE = 16000;
    static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    static final int BUFFER_SIZE_RECORDING = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
    static final int BUFFER_SIZE_PLAYING = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);

    protected AudioRecord audioRecord;
    protected AudioTrack audioTrack;

    Button buttonStart, buttonPlay;
    Thread recordThread = null;
    boolean isRecording = false;

    Thread playThread = null;
    boolean playing = false;

    String mFilePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Music";  // 경로가 계속 /0으로 들어가는데 휴대용 디바이스에서는 접근이 불가?
    String mFileName = "/test.pcm";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_save_in_file);

        // 1. TODO : permission 코드 리팩토링
        // 2. TODO : 안드로이드 TODO 사용법 찾기
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        int permissionCheck2 = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int permissionCheck3 = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);

        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(SaveInFile.this, "RECORD_AUDIO 권한 설정 완료", Toast.LENGTH_SHORT).show();
        } else {
            // 권한이 없을 때
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    101);
        }

        if (permissionCheck2 == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(SaveInFile.this, "WRITE_EXTERNAL_STORAGE 권한 설정 완료", Toast.LENGTH_SHORT).show();
        } else {
            // 권한이 없을 때
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    102);
        }

        if (permissionCheck3 == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(SaveInFile.this, "READ_EXTERNAL_STORAGE 권한 설정 완료", Toast.LENGTH_SHORT).show();
        } else {
            // 권한이 없을 때
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    103);
        }


        buttonStart = (Button) findViewById(R.id.buttonStart);
        buttonStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startRecoding();
            }
        });

        buttonPlay = (Button) findViewById(R.id.buttonPlay);
        buttonPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startPlaying();
            }
        });
    }

    private void startRecoding() {

        Log.d("[Main]", "startRecording() pass");

        if (isRecording == true) {
            Log.d("[Main]", "isRecording == true pass");

            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
            recordThread = null;

            isRecording = false;
            buttonStart.setText("Record");
            buttonPlay.setEnabled(true);

        } else {
            Log.d("[Main]", "isRecording == false pass");
            isRecording = true;

            buttonStart.setText("Stop");
            buttonPlay.setEnabled(false);

            audioRecord = new AudioRecord(AUDIO_SOURCE, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, BUFFER_SIZE_RECORDING);

            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) { // check for proper initialization
                Log.e(TAG, "error initializing ");
                return;
            }

            byte[] data = new byte[BUFFER_SIZE_RECORDING]; // assign size so that bytes are read in in chunks inferior to AudioRecord internal buffer size
            File file = new File(mFilePath);
            File audioFile = new File(mFileName);

            if (file.exists()) {
                Log.d("[Main]", "이미 폴더가 존재함!");
            }

            if (audioFile.exists()) {
                Log.d("[Main]", "이미 파일이 만들어짐.");  // 파일이 존재하지 않아서 이 곳에는 들어욎 않음

                audioFile.delete();
            }

            audioRecord.startRecording();

            recordThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    Log.d("[Main]", "recordThread pass");  // 여기에 들어오질 않는다..
                    writeAudioData(audioFile, data);
                }
            });

            recordThread.start();
        }

    }

    private void writeAudioData(File fileName, byte[] data) { // to be called in a Runnable for a Thread created after call to startRecording()

        Log.d("[Main]", "writeAudioData() pass");

        FileOutputStream outputStream = null;

        try {
            outputStream = new FileOutputStream(fileName); //fileName is path to a file, where audio data should be written
            Log.d("[Main]", "outputStream creator pass");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        while (isRecording) { // continueRecording can be toggled by a button press, handled by the main (UI) thread
            Log.d("[Main]", "in while pass");
            int read = audioRecord.read(data, 0, data.length);
            try {
                outputStream.write(data, 0, read);
            }
            catch (IOException e) {
                Log.d(TAG, "exception while writing to file");
                e.printStackTrace();
            }
        }

        try {
            outputStream.flush();
            outputStream.close();
        }
        catch (IOException e) {
            Log.d(TAG, "exception while closing output stream " + e.toString());
            e.printStackTrace();
        }

    }

    private void startPlaying() {

        Log.d("[Main]", "startPlaying() pass");

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH) // defines the type of content being played
                .setUsage(AudioAttributes.USAGE_MEDIA) // defines the purpose of why audio is being played in the app
                .build();

        AudioFormat audioFormat = new AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_8BIT) // we plan on reading byte arrays of data, so use the corresponding encoding
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build();

        audioTrack = new AudioTrack(audioAttributes, audioFormat, BUFFER_SIZE_PLAYING, AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE);

        playThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d("[Main]", "playThread pass");
                readAudioData(mFileName);  // filepath를 넣어야 되는데, 어떻게 넣지?
            }
        });

    }

    private void readAudioData(String fileName) { // fileName is the path to the file where the audio data is located

        Log.d("[Main]", "readAudioData() pass");

        byte[] data = new byte[BUFFER_SIZE_PLAYING/2]; // small buffer size to not overflow AudioTrack's internal buffer

        FileInputStream fileInputStream = null;

        try {
            fileInputStream = new FileInputStream(new File(fileName));
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        int i = 0;
        while (i != -1) { // run until file ends
            try {
                i = fileInputStream.read(data);
                audioTrack.write(data, 0, i);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            fileInputStream.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        audioTrack.stop();
        audioTrack.release();
        audioTrack = null;
    }
}