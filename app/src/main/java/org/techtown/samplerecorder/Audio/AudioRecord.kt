package org.techtown.samplerecorder.Audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.techtown.samplerecorder.AppModule.dataToShort
import org.techtown.samplerecorder.LogUtil
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
import java.text.SimpleDateFormat
import java.util.*

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class AudioRecord {
    private val TAG = this.javaClass.simpleName

    private var audioRecord: AudioRecord? = null
    private var outputStream: FileOutputStream? = null
    private var file: File? = null
    private var job: Job? = null

    fun start(queue: Queue, fileDrop: Boolean) {
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

        if (fileDrop) fileCreate()
        
        audioRecord!!.startRecording()

        job = CoroutineScope(Dispatchers.IO).launch {
            var dataSize: Int
            while (isRecording) {
                audioData = ByteArray(bufferSize)
                dataSize = audioRecord!!.read(audioData!!, 0, bufferSize)
                queue.enqueue(audioData!!)

                recordWave = 0  // Waveform
                for (i in audioData!!.indices) recordWave = dataToShort(audioData)

                if (fileDrop) fileWrite(audioData!!, dataSize)  // File Write
            }
        }
    }

    fun stop(context: Context?, fileDrop: Boolean) {
        if (audioRecord != null) {
            audioRecord!!.stop()
            audioRecord!!.release()
            audioRecord = null
            job = null
        }
        if (fileDrop) {
            fileSave()
            LogUtil.i(TAG, "file path : ${file!!.name}")
            Toast.makeText(context, file!!.name + " 저장 완료", Toast.LENGTH_LONG).show()
        }
    }

    private fun fileCreate() {
        file = File(filePath, fileName())
        outputStream = null
        try { outputStream = FileOutputStream(file) }
        catch (e: FileNotFoundException) { e.printStackTrace() }
    }

    private fun fileWrite(data: ByteArray, size: Int) {
        if (outputStream != null) {
            try { outputStream!!.write(data, 0, size) }
            catch (e: IOException) { e.printStackTrace() }
        }
    }

    private fun fileSave() {
        try {
            outputStream!!.flush()
            outputStream!!.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun fileName(): String {
        val date = Date(System.currentTimeMillis())
        val dateFormat = SimpleDateFormat("yyyy.MM.dd HH:mm:ss")
        return dateFormat.format(date) + ".pcm"
    }

    companion object {
        var recordWave = 0
    }
}