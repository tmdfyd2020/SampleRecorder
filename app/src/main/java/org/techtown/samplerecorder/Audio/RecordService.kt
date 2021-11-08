package org.techtown.samplerecorder.Audio

import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.widget.Toast
import androidx.core.app.ActivityCompat.startActivityForResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.techtown.samplerecorder.Database.RoomItem
import org.techtown.samplerecorder.FileNameActivity
import org.techtown.samplerecorder.HomeFragment.Companion.bufferSize
import org.techtown.samplerecorder.HomeFragment.Companion.isRecording
import org.techtown.samplerecorder.HomeFragment.Companion.recordChannel
import org.techtown.samplerecorder.HomeFragment.Companion.recordRate
import org.techtown.samplerecorder.HomeFragment.Companion.source
import org.techtown.samplerecorder.MainActivity
import org.techtown.samplerecorder.MainActivity.Companion.filePath
import org.techtown.samplerecorder.R
import org.techtown.samplerecorder.Util.AppModule.currentTimeName
import org.techtown.samplerecorder.Util.AppModule.dataToShort
import org.techtown.samplerecorder.Util.LogUtil
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class RecordService(context: Context) {
    private var mainActivity = context as MainActivity
    private var audioRecord: AudioRecord? = null
    private var outputStream: FileOutputStream? = null
    private var file: File? = null
    private lateinit var time: String
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
            val intent = Intent(context, FileNameActivity::class.java)
            startActivityForResult(context as MainActivity, intent, CODE_FILE_NAME, null)
            LogUtil.i(TAG, "file path : ${file!!.name}")
            Toast.makeText(context, file!!.name + " ${context.getString(R.string.toast_save)}", Toast.LENGTH_LONG).show()
        }
    }

    private fun fileCreate() {
        time = currentTimeName()
        file = File(filePath, "$time.pcm")
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

    fun addItem(name: String, context: Context) {
        val itemName : String = if (name == "") file!!.name else name
        val channel : String = if (recordChannel == AudioFormat.CHANNEL_IN_MONO) context.getString(R.string.mono) else context.getString(R.string.stereo)
        val item = RoomItem(itemName, file!!.name, time, channel, recordRate)
        mainActivity.insertItem(item)
    }

    fun removeFile() {
        file!!.delete()
    }

    companion object {
        private const val TAG = "RecordService"
        var recordWave = 0
        const val CODE_FILE_NAME = 1
        fun record(context: Context) = RecordService(context)
    }
}