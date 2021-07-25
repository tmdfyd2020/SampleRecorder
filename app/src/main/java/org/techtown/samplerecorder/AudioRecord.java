package org.techtown.samplerecorder;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

public class AudioRecord {

    private final int AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;
    private final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    public static Queue queue;

    private android.media.AudioRecord audioRecord = null;
    public Thread recordThread = null;
    private ByteBuffer byteBuffer = null;
    private ShortBuffer shortBuffer = null;
    private byte[] audioData = null;
    private short[] audioData2 = null;
    private int capacity_buffer, record_bufferSize, len_audioData;

    ContentValues contentValues;
    ContentResolver contentResolver;
    Uri audioUri;
    FileOutputStream fos;
    ParcelFileDescriptor pdf;

    WaveForm waveForm;

    public AudioRecord() {

    }

    public AudioRecord(WaveForm waveForm) {
        this.waveForm = waveForm;
    }

    public void setBoardManager(WaveForm waveForm){
        this.waveForm = waveForm;
    }

    public void init() {
        myLog.d("method activate");

        capacity_buffer = MainActivity.SampleRate * 60;  // stored buffer size (60s)
        // byteBuffer = ByteBuffer.allocate(capacity_buffer);
        shortBuffer = ShortBuffer.allocate(capacity_buffer);

        record_bufferSize = android.media.AudioRecord.getMinBufferSize(  // recorded buffer size
                MainActivity.SampleRate,
                CHANNEL_CONFIG,
                AUDIO_FORMAT
        ) * 2;
        // audioData = new byte[record_bufferSize];
        audioData2 = new short[record_bufferSize];
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
                    audioData2.length
            );
        }

        audioRecord.startRecording();

        contentValues = new ContentValues();
        // 파일 이름 저장할 때 현재 날짜, 시각 받아서 String 형식 받아서 출력
        contentValues.put(MediaStore.Audio.Media.DISPLAY_NAME, "test.pcm");
        contentValues.put(MediaStore.Audio.Media.MIME_TYPE, "audio/*");
        contentValues.put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/MZ/");
        contentValues.put(MediaStore.Audio.Media.IS_PENDING, 1);

        contentResolver = context.getContentResolver();
        audioUri = contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues);  // 파일 생성
        try {
            pdf = contentResolver.openFileDescriptor(audioUri, "w", null);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        recordThread = new Thread(new Runnable() {
            @Override
            public void run() {

                // byteBuffer.rewind();
                shortBuffer.rewind();
                myLog.d("Recording isRecording >> " + String.valueOf(MainActivity.isRecording));

                fos = new FileOutputStream(pdf.getFileDescriptor());

                len_audioData = 0;
                while(MainActivity.isRecording) {
                    //len_audioData = audioRecord.read(audioData, 0, record_bufferSize);  // audioRecord -> audioData
                    //byteBuffer.put(audioData, 0, len_audioData);  // audioData -> byteBuffer
                    //queue.enqueue(byteBuffer);  // byteBuffer -> queue
                    shortBuffer.position(0);
                    len_audioData = audioRecord.read(audioData2, 0, record_bufferSize);  // audioRecord -> audioData
                    shortBuffer.put(audioData2, 0, len_audioData);  // audioData -> byteBuffer
                    waveForm.setData(shortBuffer, len_audioData);  // Null Pointer

                    queue.enqueue(shortBuffer);  // byteBuffer -> queue

                    try {
                        fos.write(short2byte(audioData2));  // 이대로 쓰니까 pcm 파일이 저장된다.
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                myLog.d("len_audioData Size >> " + String.valueOf(len_audioData));
            }
        });
        recordThread.start();
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

    public void writeWaveFileHeader(FileOutputStream out, long totalAudioLen,
                                     long totalDataLen, long longSampleRate, int channels, long byteRate)
            throws IOException {
        byte[] header = new byte[44];
        // 4byte Chunk ID : WAV 파일에 대한 고정값인 RIFF 문자
        header[0] = 'R';
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        // 4byte Chunk Size :
        header[4] = (byte) (totalDataLen & 0xff);  // file의 크기? 녹음도 하기 전에 어떻게 알아?
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        //WAVE
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        // 'fmt ' chunk
        header[12] = 'f';
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        // 4 bytes: size of 'fmt ' chunk
        header[16] = 16;
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        // format = 1
        header[20] = 1;
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        // block align
        header[32] = (byte) (2 * 16 / 8);
        header[33] = 0;
        // bits per sample
        header[34] = 16;
        header[35] = 0;
        //data
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
        out.write(header, 0, 44);
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

        contentValues.clear();
        contentValues.put(MediaStore.Audio.Media.IS_PENDING, 0);
        contentResolver.update(audioUri, contentValues, null, null);

        // 여기에 저장된 파일을 다시 불러서 wav 파일로 바꾸는 것을 해보자.
        Cursor cursor = contentResolver.query(Uri.parse(String.valueOf(audioUri)), null, null, null, null);
        cursor.moveToNext();
        String absolutePath = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DATA));
        myLog.d("저장된 파일의 경로는 " + absolutePath);

    }

    public void release() {
        myLog.d("method activate");

        audioData2 = null;
        byteBuffer = null;

        queue = new Queue();
    }

}