package org.techtown.samplerecorder.Main

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.techtown.samplerecorder.AppModule.dataToShort
import org.techtown.samplerecorder.MainActivity.Companion.autoStop
import org.techtown.samplerecorder.MainActivity.Companion.bufferSize
import org.techtown.samplerecorder.MainActivity.Companion.isPlaying
import org.techtown.samplerecorder.MainActivity.Companion.playChannel
import org.techtown.samplerecorder.MainActivity.Companion.playRate
import org.techtown.samplerecorder.MainActivity.Companion.type

class AudioTrack {
    private val TAG = this.javaClass.simpleName

    private var audioTrack: AudioTrack? = null
    private var job: Job? = null

    fun play(queue: Queue) {
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
                        .setSampleRate(playRate)
                        .setChannelMask(playChannel)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize * 2)
                .build()
        }
        var audioData: ByteArray?

        job = CoroutineScope(Dispatchers.IO).launch {
            audioTrack!!.play()
            while (isPlaying) {
                audioData = queue.dequeue()
                audioTrack!!.write(audioData!!, 0, bufferSize)

                if (queue.isEmpty) {
                    autoStop = true
                    queue.copy()
                    break
                }

                playWave = 0  // Waveform
                for (i in audioData!!.indices) playWave = dataToShort(audioData)
            }
        }
    }

    fun stop() {
        if (audioTrack != null) {
            audioTrack!!.stop()
            audioTrack!!.release()
            audioTrack = null
            job = null
        }
    }

    companion object {
        var playWave = 0
    }
}