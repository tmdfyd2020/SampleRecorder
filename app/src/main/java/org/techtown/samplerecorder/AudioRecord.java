package org.techtown.samplerecorder;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class AudioRecord {

    private final int AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;
    private final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    public static Queue queue;

    private android.media.AudioRecord audioRecord = null;
    private Thread recordThread = null;
    private ByteBuffer byteBuffer = null;
    private byte[] audioData = null;
    private int capacity_buffer, record_bufferSize, len_audioData;

    ContentValues values;
    ContentResolver contentResolver;
    Uri item;
    FileOutputStream fos;
    ParcelFileDescriptor pdf;

    public void init() {
        myLog.d("method activate");

        capacity_buffer = MainActivity.SampleRate * 60;  // stored buffer size (60s)
        byteBuffer = ByteBuffer.allocate(capacity_buffer);

        record_bufferSize = android.media.AudioRecord.getMinBufferSize(  // recorded buffer size
                MainActivity.SampleRate,
                CHANNEL_CONFIG,
                AUDIO_FORMAT
        ) * 2;
        audioData = new byte[record_bufferSize];

        queue = new Queue();
    }

    public void start(Context context) {
        myLog.d("method activate");
        myLog.d("Recording Sample Rate : " + String.valueOf(MainActivity.SampleRate));

        if(audioRecord == null) {
            audioRecord = new android.media.AudioRecord(
                    AUDIO_SOURCE,
                    MainActivity.SampleRate,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    audioData.length
            );
        }

        audioRecord.startRecording();

        values = new ContentValues();
        // 파일 이름 저장할 때 현재 날짜, 시각 받아서 String 형식 받아서 출력
        values.put(MediaStore.Audio.Media.DISPLAY_NAME, "test1.wav");
        values.put(MediaStore.Audio.Media.MIME_TYPE, "audio/*");
        values.put(MediaStore.Audio.Media.IS_PENDING, 1);

        contentResolver = context.getContentResolver();
        item = contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values);

        try {
            pdf = contentResolver.openFileDescriptor(item, "w", null);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        recordThread = new Thread(new Runnable() {
            @Override
            public void run() {

                byteBuffer.rewind();
                myLog.d("Recording isRecording >> " + String.valueOf(MainActivity.isRecording));

                fos = new FileOutputStream(pdf.getFileDescriptor());

                len_audioData = 0;
                while(MainActivity.isRecording) {
                    len_audioData = audioRecord.read(audioData, 0, record_bufferSize);  // audioRecord -> audioData
                    byteBuffer.put(audioData, 0, len_audioData);  // audioData -> byteBuffer
                    queue.enqueue(byteBuffer);  // byteBuffer -> queue

                    try {
                        fos.write(audioData);  // 이대로 쓰니까 pcm 파일이 저장된다.
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                myLog.d("len_audioData Size >> " + String.valueOf(len_audioData));
            }
        });
        recordThread.start();
    }

    public void stop() {
        myLog.d("method activate");

        if (audioRecord != null) {
            if (audioRecord.getState() != android.media.AudioRecord.RECORDSTATE_STOPPED) {
                try {
                    audioRecord.stop();

                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }

                audioRecord.release();
                audioRecord = null;
                recordThread = null;
            }
        }

        try {
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        values.clear();
        values.put(MediaStore.Audio.Media.IS_PENDING, 0);
        contentResolver.update(item, values, null, null);
    }

    public void release() {
        myLog.d("method activate");

        audioData = null;
        byteBuffer = null;

        queue = new Queue();
    }

}