package org.techtown.samplerecorder;

import android.content.Context;
import android.media.AudioFormat;
import android.media.MediaRecorder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AudioRecord {

    private final int AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;
    private final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    public static Queue queue;
    public static int dataMax;

    private android.media.AudioRecord audioRecord = null;
    public Thread recordThread = null;
    private ShortBuffer shortBuffer = null;
    private short[] audioData = null;
    private int capacity_buffer, record_bufferSize, len_audioData;

    FileOutputStream outputStream;

    public void init() {
        myLog.d("method activate");

        capacity_buffer = MainActivity.SampleRate * 60;  // stored buffer size (60s)
        shortBuffer = ShortBuffer.allocate(capacity_buffer);

        record_bufferSize = android.media.AudioRecord.getMinBufferSize(  // recorded buffer size
                MainActivity.SampleRate,
                CHANNEL_CONFIG,
                AUDIO_FORMAT
        ) * 2;

        audioData = new short[record_bufferSize];
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

        if(MainActivity.fileDrop) {
            File file = new File("/mnt/sdcard/audioDrop/", fileName(System.currentTimeMillis()));
            outputStream = null;

            try {
                outputStream = new FileOutputStream(file); // fileName is path to a file, where audio data should be written
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        audioRecord.startRecording();

        recordThread = new Thread(new Runnable() {
            @Override
            public void run() {

                shortBuffer.rewind();
                myLog.d("Recording isRecording >> " + String.valueOf(MainActivity.isRecording));

                len_audioData = 0;
                while(MainActivity.isRecording) {
                    len_audioData = audioRecord.read(audioData, 0, record_bufferSize);  // audioRecord -> audioData
                    shortBuffer.put(audioData, 0, len_audioData);  // audioData -> shortBuffer
                    queue.enqueue(shortBuffer);  // shortBuffer -> queue

                    if (MainActivity.fileDrop) {
                        try {
                            outputStream.write(short2byte(audioData), 0, len_audioData);
                        } catch (IOException e) {
                            myLog.d("exception while writing to file");
                            e.printStackTrace();
                        }
                    }

                    dataMax = 0;

                    for(int i = 0; i < audioData.length; i++){
                        if(Math.abs(audioData[i]) >= dataMax) {
                            dataMax = Math.abs(audioData[i]);
                        }
                    }
                    MainActivity.view.update(dataMax);  // 속도가 왜 느릴까?

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

        if (MainActivity.fileDrop) {
            try {
                outputStream.flush();
                outputStream.close();
            } catch (IOException e) {
                myLog.d("exception while closing output stream " + e.toString());
                e.printStackTrace();
            }
        }

        MainActivity.view.recreate();
    }

    public void release() {
        myLog.d("method activate");

        audioData = null;
        shortBuffer = null;

        queue = new Queue();
    }

    public String fileName(long realtime) {
        Date date = new Date(realtime);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd_HH_mm_ss");
        return dateFormat.format(date) + ".pcm";
    }

    private byte[] short2byte(short[] sData) {

        int shortArrsize = sData.length;
        byte[] bytes = new byte[shortArrsize * 2];

        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            sData[i] = 0;
        }
        return bytes;
    }

    /**
     * Short array to Byte array Functions
        private byte[] short2byte(short[] sData) {

            int shortArrsize = sData.length;
            byte[] bytes = new byte[shortArrsize * 2];

            for (int i = 0; i < shortArrsize; i++) {
                bytes[i * 2] = (byte) (sData[i] & 0x00FF);
                bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
                sData[i] = 0;
            }
            return bytes;
        }
    */

}