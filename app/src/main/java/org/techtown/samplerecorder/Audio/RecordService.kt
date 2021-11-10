package org.techtown.samplerecorder.audio

import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import androidx.core.app.ActivityCompat.startActivityForResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.techtown.samplerecorder.activity.DialogActivity
import org.techtown.samplerecorder.activity.DialogActivity.Companion.KEY_MODE_DIALOG
import org.techtown.samplerecorder.activity.DialogActivity.Companion.MODE_FILE_NAME
import org.techtown.samplerecorder.home.HomeFragment.Companion.bufferSize
import org.techtown.samplerecorder.home.HomeFragment.Companion.isRecording
import org.techtown.samplerecorder.home.HomeFragment.Companion.recordChannel
import org.techtown.samplerecorder.home.HomeFragment.Companion.recordRate
import org.techtown.samplerecorder.home.HomeFragment.Companion.source
import org.techtown.samplerecorder.activity.MainActivity
import org.techtown.samplerecorder.activity.MainActivity.Companion.filePath
import org.techtown.samplerecorder.util.AppModule.currentTimeName
import org.techtown.samplerecorder.util.AppModule.dataToShort
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class RecordService(private val context: Context) {
    private var mainActivity = context as MainActivity
    private var audioRecord: AudioRecord? = null
    private var outputStream: FileOutputStream? = null
    private var job: Job? = null
    private var fileDrop: Boolean = false

    fun start(queue: Queue, fileDrop: Boolean) {
        this.fileDrop = fileDrop
        if (fileDrop) fileCreate()

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

        audioRecord!!.startRecording()
        job = CoroutineScope(Dispatchers.IO).launch {
            var dataSize: Int
            while (isRecording) {
                // Record write
                audioData = ByteArray(bufferSize)
                dataSize = audioRecord!!.read(audioData!!, 0, bufferSize)
                queue.enqueue(audioData!!)

                // Waveform
                recordWave = 0
                for (i in audioData!!.indices) recordWave = dataToShort(audioData)

                if (fileDrop) fileWrite(audioData!!, dataSize)  // File Write
            }
        }
    }

    fun stop() {
        if (audioRecord != null) {
            audioRecord!!.stop()
            audioRecord!!.release()
            audioRecord = null
            job = null
        }
        if (fileDrop) {
            fileSave()
            val intent = Intent(context, DialogActivity::class.java).apply {
                putExtra(KEY_MODE_DIALOG, MODE_FILE_NAME)
            }
            startActivityForResult(mainActivity, intent, CODE_FILE_NAME, null)
        }
    }

    private fun fileCreate() {
        fileCreateTime = currentTimeName()
        file = File(filePath, "$fileCreateTime.pcm")
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

    companion object {
        private const val TAG = "RecordService"
        fun record(context: Context) = RecordService(context)
        const val CODE_FILE_NAME = 1
        lateinit var file: File
        lateinit var fileCreateTime: String
        var recordWave = 0
    }
}