package org.techtown.samplerecorder;

import android.content.Context;
import android.media.AudioFormat;
import android.widget.Toast;

import com.visualizer.amplitude.AudioRecordView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AudioRecord {

    private final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    public static short lastData_1, lastData_2;
    public static short[] index;


    private android.media.AudioRecord audioRecord = null;
    private Thread recordThread = null;
    private ShortBuffer shortBuffer = null;
    private FileOutputStream outputStream;
    private File file;
    private short[] audioData = null;
    private int capacity_buffer, record_bufferSize, len_audioData, dataMax;

    public void init(int sampleRate, int bufferSize, AudioRecordView view_record) {
        myLog.d("method activate");

        audioData = null;
        shortBuffer = null;

        capacity_buffer = sampleRate * 60;  // stored buffer size (60s)
        shortBuffer = ShortBuffer.allocate(capacity_buffer);

        record_bufferSize = bufferSize;
        audioData = new short[record_bufferSize];

        view_record.recreate();
    }

    public void start(int source, int channel, int sampleRate, AudioRecordView view_record, Queue queue) {
        myLog.d("method activate");

        if(audioRecord == null) {
            audioRecord = new android.media.AudioRecord(
                    source,
                    sampleRate,
                    channel,
                    AUDIO_FORMAT,
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

                    // 여기도 고려 :: while 문 밖에서?
                    if (MainActivity.fileDrop) {  // why? 해당 부분 dataMax 반복문 위로 가면 view 출력이 안됨
                        try {
                            outputStream.write(shortToByte_1(audioData), 0, len_audioData);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    view_record.update(dataMax);
                }
                lastData_1 = audioData[len_audioData - 1];
                lastData_2 = audioData[len_audioData - 2];
                index = audioData;

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

    // short[] -> byte[] 후보 1
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

    // short[] -> byte[] 후보 2
    byte [] shortToByte_2(short [] input)
    {
        int short_index, byte_index;
        int iterations = input.length;

        byte [] buffer = new byte[input.length * 2];

        short_index = byte_index = 0;

        for(/*NOP*/; short_index != iterations; /*NOP*/)
        {
            buffer[byte_index]     = (byte) (input[short_index] & 0x00FF);
            buffer[byte_index + 1] = (byte) ((input[short_index] & 0xFF00) >> 8);

            ++short_index; byte_index += 2;
        }

        return buffer;
    }

    // short[] -> byte[] 후보 3
    byte[] shortToByte_3(short[] data) {
        ByteBuffer buffer = ByteBuffer.allocate(data.length * 2);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.asShortBuffer().put(data);
        byte[] bytes = buffer.array();

        return bytes;
    }
}