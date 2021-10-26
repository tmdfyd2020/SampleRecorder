package org.techtown.samplerecorder.Main

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.widget.Toast
import org.techtown.samplerecorder.MainActivity.Companion.bufferSize
import org.techtown.samplerecorder.MainActivity.Companion.filePath
import org.techtown.samplerecorder.MainActivity.Companion.isRecording
import org.techtown.samplerecorder.MainActivity.Companion.recordChannel
import org.techtown.samplerecorder.MainActivity.Companion.recordRate
import org.techtown.samplerecorder.MainActivity.Companion.source
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class AudioRecord {
    private val TAG = this.javaClass.simpleName

    private var audioRecord: AudioRecord? = null
    private var recordThread: Thread? = null
    private var outputStream: FileOutputStream? = null
    private var file: File? = null

    fun start(queue: Queue, fileDrop: Boolean) {  // TODO 함수로 다 쪼개기
        if (audioRecord == null) {
            audioRecord = AudioRecord(
                source,
                recordRate,
                recordChannel,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )
        }
        var audioData: ByteArray?

        if (fileDrop) {
            file = File(filePath, fileName())
            outputStream = null
            try { outputStream = FileOutputStream(file) }
            catch (e: FileNotFoundException) { e.printStackTrace() }
        }
        
        audioRecord!!.startRecording()
        
        recordThread = Thread {  // TODO Coroutine
            var dataSize: Int

            while (isRecording) {
                audioData = ByteArray(bufferSize) // prevent from overwritting data
                dataSize = audioRecord!!.read(
                    audioData!!,
                    0,
                    bufferSize
                ) // audioRecord -> audioData
                queue.enqueue(audioData)

                // using draw waveform in MainActivity
                dataMax = 0
                for (i in audioData!!.indices) {
                    val buffer = ByteBuffer.wrap(audioData)
                    buffer.order(ByteOrder.LITTLE_ENDIAN)
                    dataMax = 10 * abs(buffer.short.toInt())
                }
                
                if (fileDrop) {
                    try {
                        if (outputStream != null) {
                            outputStream!!.write(audioData, 0, dataSize)
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
        }
        recordThread!!.start()
    }

    fun stop() {  // TODO Merge
        if (audioRecord != null) {
            if (audioRecord!!.state != AudioRecord.RECORDSTATE_STOPPED) {
                try {
                    audioRecord!!.stop()
                } catch (e: IllegalStateException) {
                    e.printStackTrace()
                }
                audioRecord!!.release()
                audioRecord = null
                recordThread = null
            }
        }
    }

    fun release(context: Context?, fileDrop: Boolean) {
        if (fileDrop) {
            try {
                outputStream!!.flush() // TODO : null reference issue
                outputStream!!.close()
            } catch (e: IOException) {
                myLog.d("exception while closing output stream $e")
                e.printStackTrace()
            }
            Toast.makeText(context, file!!.absolutePath + " 저장 완료", Toast.LENGTH_LONG).show()
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun fileName(): String {
        val date = Date(System.currentTimeMillis())
        val dateFormat = SimpleDateFormat("yyyy.MM.dd HH:mm:ss")
        return dateFormat.format(date) + ".pcm"
    }

    companion object {
        var dataMax = 0
    }
}