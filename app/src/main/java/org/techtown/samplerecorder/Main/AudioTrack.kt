package org.techtown.samplerecorder.Main

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import org.techtown.samplerecorder.MainActivity.Companion.autoStop
import org.techtown.samplerecorder.MainActivity.Companion.isPlaying
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AudioTrack {
    private val TAG = this.javaClass.simpleName

    private var audioTrack: AudioTrack? = null
    private var playThread: Thread? = null
    private var audioData: ByteArray? = null
    private var track_bufferSize = 0

    fun init(bufferSize: Int) {
//        myLog.d("method activate");
        track_bufferSize = bufferSize
        audioData = ByteArray(track_bufferSize)
    }

    fun play(type: Int, channel: Int, sampleRate: Int, queue: Queue) {
//        myLog.d("method activate");
        myLog.d("play sample rate : $sampleRate")
        if (audioTrack == null) {
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(type)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(channel)
                        .build()
                )
                .setBufferSizeInBytes(track_bufferSize * 2)
                .build()
        }
        playThread = Thread {
            audioTrack!!.play()
            while (isPlaying) {
                audioData = queue.dequeue()
                audioTrack!!.write(audioData!!, 0, track_bufferSize)
                if (queue.isEmpty) {
                    autoStop = true
                    queue.copy()
                    break
                }

                // using draw waveform in MainActivity
                dataMax = 0
                for (i in audioData!!.indices) {
                    val buffer = ByteBuffer.wrap(audioData)
                    buffer.order(ByteOrder.LITTLE_ENDIAN)
                    dataMax = 10 * Math.abs(buffer.short.toInt())
                }
            }
        }
        playThread!!.start()
    }

    fun stop() {
//        myLog.d("method activate");
        if (audioTrack != null && audioTrack!!.state != AudioTrack.STATE_UNINITIALIZED) {
            if (audioTrack!!.playState != AudioTrack.PLAYSTATE_STOPPED) {
                try {
                    audioTrack!!.stop()
                } catch (e: IllegalStateException) {
                    e.printStackTrace()
                }
                audioTrack!!.release()
                audioTrack = null
                playThread = null
            }
        }
    }

    fun release() {
//        myLog.d("method activate");
    }

    companion object {
        var dataMax = 0
    }
}