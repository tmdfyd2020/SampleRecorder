package org.techtown.samplerecorder.Main;

import android.content.Context;
import android.media.AudioFormat;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AudioRecord {

    public static int dataMax;

    private android.media.AudioRecord audioRecord = null;
    private Thread recordThread = null;
    private FileOutputStream outputStream;
    private File file;
    private byte[] audioData = null;
    private int record_bufferSize, len_audioData;

    public void init(int bufferSize) {
//        myLog.d("method activate");

        audioData = null;
        record_bufferSize = bufferSize;
    }

    public void start(int source, int channel, int sampleRate, Queue queue) {
//        myLog.d("method activate");

        if(audioRecord == null) {
            audioRecord = new android.media.AudioRecord(
                    source,
                    sampleRate,
                    channel,
                    AudioFormat.ENCODING_PCM_16BIT,
                    record_bufferSize
            );
        }

        if (MainActivity.fileDrop) {
            file = new File("/mnt/sdcard/audioDrop/", fileName(System.currentTimeMillis()));
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

                len_audioData = 0;
                while(MainActivity.isRecording) {
                    audioData = new byte[record_bufferSize];  // prevent from overwritting data
                    len_audioData = audioRecord.read(audioData, 0, record_bufferSize);  // audioRecord -> audioData
                    queue.enqueue(audioData);

                    // using draw waveform in MainActivity
                    dataMax = 0;
                    for (int i = 0; i < audioData.length; i++) {
                        ByteBuffer buffer = ByteBuffer.wrap(audioData);
                        buffer.order(ByteOrder.LITTLE_ENDIAN);
                        dataMax = 10 * Math.abs(buffer.getShort());
                    }

                    if (MainActivity.fileDrop) {
                        try {
                            outputStream.write(audioData, 0, len_audioData);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                }
            }
        });

        recordThread.start();
    }

    public void stop() {
//        myLog.d("method activate");

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
//        myLog.d("method activate");

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

    public String fileName(long realtime) {
        Date date = new Date(realtime);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd_HH_mm_ss");
        return dateFormat.format(date) + ".pcm";
    }
}