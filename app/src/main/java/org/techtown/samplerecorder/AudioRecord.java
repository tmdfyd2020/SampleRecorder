package org.techtown.samplerecorder;

import android.media.AudioFormat;
import android.media.MediaRecorder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AudioRecord {

    private final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    public static Queue queue;
    public static short index;

    private android.media.AudioRecord audioRecord = null;
    private Thread recordThread = null;
    private ShortBuffer shortBuffer = null;
    private FileOutputStream outputStream;
    private File file;
    private short[] audioData = null;
    private int capacity_buffer, record_bufferSize, len_audioData, dataMax;

    public void init(int sampleRate, int bufferSize) {
//        myLog.d("method activate");

        audioData = null;
        shortBuffer = null;

        capacity_buffer = sampleRate * 60;  // stored buffer size (60s)
        shortBuffer = ShortBuffer.allocate(capacity_buffer);

//        record_bufferSize = android.media.AudioRecord.getMinBufferSize(  // recorded buffer size
//                sampleRate,
//                channel,
//                AUDIO_FORMAT ) * 2;
        record_bufferSize = bufferSize;
        audioData = new short[record_bufferSize];

        queue = new Queue();

        MainActivity.view_record.recreate();
    }

    public void start(int source, int channel, int sampleRate) {
//        myLog.d("method activate");
//        myLog.d("Recording Sample Rate : " + String.valueOf(sampleRate));

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

//            try {
//                writeWavHeader(outputStream, CHANNEL_CONFIG, sampleRate, AUDIO_FORMAT);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
        }

        audioRecord.startRecording();

        recordThread = new Thread(new Runnable() {
            @Override
            public void run() {

                shortBuffer.rewind();
                myLog.d("Recording isRecording >> " + String.valueOf(sampleRate));

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


                    if (MainActivity.fileDrop) {  // why? 해당 부분 dataMax 반복문 위로 가면 view 출력이 안됨
                        try {
                            outputStream.write(shortToByte_1(audioData), 0, len_audioData);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    MainActivity.view_record.update(dataMax);
                }
                queue.enqueue(shortBuffer);  // shortBuffer -> queue

                myLog.d("len_audioData Size >> " + String.valueOf(len_audioData));
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

    public void release() {
//        myLog.d("method activate");

        if (MainActivity.fileDrop) {
            try {
                outputStream.flush();
                outputStream.close();
            } catch (IOException e) {
                myLog.d("exception while closing output stream " + e.toString());
                e.printStackTrace();
            }

            // using 1) pcm file to wav file
            /*
            try {
                File wav_file = new File("/mnt/sdcard/audioDrop/", wav_fileName(System.currentTimeMillis()));
                rawToWave(file, wav_file);
                file.delete();
            } catch (IOException e) {
                e.printStackTrace();
            }
            */

            // using 2) raw + wav header
//            try {
//                updateWavHeader(file);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
        }
    }

    public String raw_fileName(long realtime) {
        Date date = new Date(realtime);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd_HH_mm_ss");
        return dateFormat.format(date) + ".pcm";
    }

    public String wav_fileName(long realtime) {
        Date date = new Date(realtime);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd_HH_mm_ss");
        return dateFormat.format(date) + ".wav";
    }

    // using 2) raw + wav header
    public static void writeWavHeader(OutputStream out, int channelMask, int sampleRate, int encoding) throws IOException {
        short channels;
        switch (channelMask) {
            case AudioFormat.CHANNEL_IN_MONO:
                channels = 1;
                break;
            case AudioFormat.CHANNEL_IN_STEREO:
                channels = 2;
                break;
            default:
                throw new IllegalArgumentException("Unacceptable channel mask");
        }

        short bitDepth;
        switch (encoding) {
            case AudioFormat.ENCODING_PCM_8BIT:
                bitDepth = 8;
                break;
            case AudioFormat.ENCODING_PCM_16BIT:
                bitDepth = 16;
                break;
            case AudioFormat.ENCODING_PCM_FLOAT:
                bitDepth = 32;
                break;
            default:
                throw new IllegalArgumentException("Unacceptable encoding");
        }

        writeWavHeader(out, channels, sampleRate, bitDepth);
    }

    public static void writeWavHeader(OutputStream out, short channels, int sampleRate, short bitDepth) throws IOException {
        // Convert the multi-byte integers to raw bytes in little endian format as required by the spec
        byte[] littleBytes = ByteBuffer
                .allocate(14)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putShort(channels)
                .putInt(sampleRate)
                .putInt(sampleRate * channels * (bitDepth / 8))
                .putShort((short) (channels * (bitDepth / 8)))
                .putShort(bitDepth)
                .array();

        // Not necessarily the best, but it's very easy to visualize this way
        out.write(new byte[]{
                // RIFF header
                'R', 'I', 'F', 'F', // Chunk ID
                0, 0, 0, 0, // Chunk Size (must be updated later)
                'W', 'A', 'V', 'E', // Format
                // fmt subchunk
                'f', 'm', 't', ' ', // Subchunk1 ID
                16, 0, 0, 0, // Subchunk1 Size
                1, 0, // AudioFormat
                littleBytes[0], littleBytes[1], // NumChannels
                littleBytes[2], littleBytes[3], littleBytes[4], littleBytes[5], // Sample Rate
                littleBytes[6], littleBytes[7], littleBytes[8], littleBytes[9], // Byte Rate
                littleBytes[10], littleBytes[11], // Block Align
                littleBytes[12], littleBytes[13], // Bits Per Sample
                // data sub chunk
                'd', 'a', 't', 'a', // Subchunk2 ID
                0, 0, 0, 0, // Subchunk2 Size (must be updated later)
        });
    }

    public static void updateWavHeader(File wav) throws IOException {
        byte[] sizes = ByteBuffer
                .allocate(8)
                .order(ByteOrder.LITTLE_ENDIAN)
                // There are probably a bunch of different/better ways to calculate
                // these two given your circumstances. Cast should be safe since if the WAV is
                // > 4 GB we've already made a terrible mistake.
                .putInt((int) (wav.length() - 8)) // ChunkSize
                .putInt((int) (wav.length() - 44)) // Subchunk2Size
                .array();

        RandomAccessFile accessWave = null;
        //noinspection CaughtExceptionImmediatelyRethrown
        try {
            accessWave = new RandomAccessFile(wav, "rw");
            // ChunkSize
            accessWave.seek(4);
            accessWave.write(sizes, 0, 4);

            // Subchunk2Size
            accessWave.seek(40);
            accessWave.write(sizes, 4, 4);
        } catch (IOException ex) {
            // Rethrow but we still close accessWave in our finally
            throw ex;
        } finally {
            if (accessWave != null) {
                try {
                    accessWave.close();
                } catch (IOException ex) {
                    //
                }
            }
        }
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

    // using 1) pcm file to wav file
    /*
    private void rawToWave(final File rawFile, final File waveFile) throws IOException {

        byte[] rawData = new byte[(int) rawFile.length()];
        DataInputStream input = null;
        try {
            input = new DataInputStream(new FileInputStream(rawFile));
            input.read(rawData);
        } finally {
            if (input != null) {
                input.close();
            }
        }

        DataOutputStream output = null;
        try {
            output = new DataOutputStream(new FileOutputStream(waveFile));
            // WAVE header
            writeString(output, "RIFF"); // chunk id
            writeInt(output, 36 + rawData.length); // chunk size
            writeString(output, "WAVE"); // format
            writeString(output, "fmt "); // subchunk 1 id
            writeInt(output, 16); // subchunk 1 size
            writeShort(output, (short) 1); // audio format (1 = PCM)
            writeShort(output, (short) 1); // number of channels
            writeInt(output, sampleRate); // sample rate
            writeInt(output, sampleRate * 2); // byte rate
            writeShort(output, (short) 2); // block align
            writeShort(output, (short) 16); // bits per sample
            writeString(output, "data"); // subchunk 2 id
            writeInt(output, rawData.length); // subchunk 2 size
            // Audio data (conversion big endian -> little endian)
            short[] shorts = new short[rawData.length / 2];
            ByteBuffer.wrap(rawData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
            ByteBuffer bytes = ByteBuffer.allocate(shorts.length * 2);
            for (short s : shorts) {
                bytes.putShort(s);
            }

            output.write(fullyReadFileToBytes(rawFile));
        } finally {
            if (output != null) {
                output.close();
            }
        }
    }

    byte[] fullyReadFileToBytes(File f) throws IOException {
        int size = (int) f.length();
        byte bytes[] = new byte[size];
        byte tmpBuff[] = new byte[size];
        FileInputStream fis= new FileInputStream(f);
        try {

            int read = fis.read(bytes, 0, size);
            if (read < size) {
                int remain = size - read;
                while (remain > 0) {
                    read = fis.read(tmpBuff, 0, remain);
                    System.arraycopy(tmpBuff, 0, bytes, size - remain, read);
                    remain -= read;
                }
            }
        }  catch (IOException e){
            throw e;
        } finally {
            fis.close();
        }

        return bytes;
    }

    private void writeInt(final DataOutputStream output, final int value) throws IOException {
        output.write(value >> 0);
        output.write(value >> 8);
        output.write(value >> 16);
        output.write(value >> 24);
    }

    private void writeShort(final DataOutputStream output, final short value) throws IOException {
        output.write(value >> 0);
        output.write(value >> 8);
    }

    private void writeString(final DataOutputStream output, final String value) throws IOException {
        for (int i = 0; i < value.length(); i++) {
            output.write(value.charAt(i));
        }
    }
    */
}