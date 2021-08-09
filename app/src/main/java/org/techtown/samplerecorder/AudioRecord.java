package org.techtown.samplerecorder;

import android.content.Context;
import android.media.AudioFormat;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ShortBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AudioRecord {

    public static short lastData_1, lastData_2;
    public static int dataMax;

    private android.media.AudioRecord audioRecord = null;
    private Thread recordThread = null;
    private ShortBuffer shortBuffer = null;
    private FileOutputStream outputStream;
    private File file;
    private short[] audioData = null;
    private int capacity_buffer, record_bufferSize, len_audioData;

    public void init(int sampleRate, int bufferSize) {
        myLog.d("method activate");

        audioData = null;
        shortBuffer = null;

        capacity_buffer = sampleRate * 240;  // stored buffer size (240s)
        shortBuffer = ShortBuffer.allocate(capacity_buffer);

        record_bufferSize = bufferSize;
        audioData = new short[record_bufferSize];
    }

    public void start(int source, int channel, int sampleRate, Queue queue) {
        myLog.d("method activate");

        if(audioRecord == null) {
            audioRecord = new android.media.AudioRecord(
                    source,
                    sampleRate,
                    channel,
                    AudioFormat.ENCODING_PCM_16BIT,
                    audioData.length
            );
        }

        if (MainActivity.fileDrop) {
            file = new File("/mnt/sdcard/audioDrop/", raw_fileName(System.currentTimeMillis()));
            outputStream = null;

            try {
                outputStream = new FileOutputStream(file);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        audioRecord.startRecording();

        recordThread = new Thread(new Runnable() {
            @Override
            public void run() {

                shortBuffer.rewind();

                len_audioData = 0;
                while(MainActivity.isRecording) {
                    len_audioData = audioRecord.read(audioData, 0, record_bufferSize);  // audioRecord -> audioData
                    shortBuffer.put(audioData, 0, len_audioData);  // audioData -> shortBuffer
//                    queue.enqueue(shortBuffer);  // shortBuffer -> queue

                    dataMax = 0;
                    for (int i = 0; i < audioData.length; i++) {
                        if(Math.abs(audioData[i]) >= dataMax) {
                            dataMax = Math.abs(audioData[i]);
                        }
                    }

                    if (MainActivity.fileDrop) {
                        try {
                            outputStream.write(shortToByte_1(audioData), 0, len_audioData);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                lastData_1 = audioData[len_audioData - 1];
                lastData_2 = audioData[len_audioData - 2];

                queue.enqueue(shortBuffer);  // shortBuffer -> queue
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
    }

    public void release(Context context) {
        myLog.d("method activate");

        if (MainActivity.fileDrop) {
            try {
                outputStream.flush();
                outputStream.close();
            } catch (IOException e) {
                myLog.d("exception while closing output stream " + e.toString());
                e.printStackTrace();
            }

            Toast.makeText(context, file.getAbsolutePath() + " 저장 완료", Toast.LENGTH_LONG).show();
        }
    }

    public String raw_fileName(long realtime) {
        Date date = new Date(realtime);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd_HH_mm_ss");
        return dateFormat.format(date) + ".pcm";
    }

    // short[] -> byte[]
    private byte[] shortToByte_1(short[] sData) {

        int shortArrsize = sData.length;
        byte[] bytes = new byte[shortArrsize * 2];

        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            sData[i] = 0;
        }
        return bytes;
    }
}