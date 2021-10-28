package org.techtown.samplerecorder.List

import android.R
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.recyclerview.widget.RecyclerView
import org.techtown.samplerecorder.MainActivity.Companion.bufferSize
import org.techtown.samplerecorder.MainActivity.Companion.playRate
import org.techtown.samplerecorder.databinding.ItemBinding
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.RandomAccessFile
import java.util.*

class ItemListAdapter(var context: Context, private val fileList: MutableList<File>) : RecyclerView.Adapter<ItemListViewHolder>() {

    private val TAG = this.javaClass.simpleName

    var audioTrack: AudioTrack? = null
    var seekbar_touch = false
    var previous_position = -1
    var move_pointer = 0
    var btn_type = "play_button"
    var playThread: Thread? = null
    var press_pause = false
    var resume = false
    var complete_play = false
    var pause_point: Long = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemListViewHolder {
        val binding = ItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemListViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemListViewHolder, position: Int) {
        val file = fileList[position]
        holder.fileName.text = file.name

        holder.seekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            // 시크바 드래그 시
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                move_pointer = progress
            }
            // 시크바 thumb 터치 시
            override fun onStartTrackingTouch(seekBar: SeekBar) {
                seekbar_touch = true
            }
            // if stop touch seekbar thumb
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                // Thread 실행 시키면 될 것 같다.
            }
        })

        holder.playButton.setOnClickListener {
            when (btn_type) {
                "play_button" -> {
                    btn_type = "pause_button"

                    complete_play = true
                    if (previous_position != -1 && previous_position != position) {
                        notifyItemChanged(previous_position,"click")  // hide previous position seekbar
                        pause_point = 0
                        resume = false
                    }
                    previous_position = position
                    holder.playButton.setImageResource(R.drawable.png_pause)
                    holder.seekBar.visibility = View.VISIBLE

                    playThread = Thread {
                        if (audioTrack == null) {
                            audioTrack = AudioTrack.Builder()
                                .setAudioAttributes(
                                    AudioAttributes.Builder()
                                        .setUsage(AudioAttributes.USAGE_MEDIA)
                                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                        .build()
                                )
                                .setAudioFormat(
                                    AudioFormat.Builder()
                                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                        .setSampleRate(playRate)
                                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                        .build()
                                )
                                .setBufferSizeInBytes(bufferSize * 2)
                                .build()
                        }
                        val data = ByteArray(bufferSize) // small buffer size to not overflow AudioTrack's internal buffer
                        var randomFile: RandomAccessFile? = null
                        try { randomFile = RandomAccessFile(file, "rw") }
                        catch (e: FileNotFoundException) { e.printStackTrace() }

                        holder.seekBar.min = 0
                        try { holder.seekBar.max = randomFile!!.length().toInt() }
                        catch (e: IOException) { e.printStackTrace() }

                        audioTrack!!.play()
                        if (!resume) {
                            var i = 0
                            while (i != -1) { // run until file ends
                                try {
                                    i = randomFile!!.read(data)
                                    audioTrack!!.write(data, 0, i)
                                    holder.seekBar.progress = randomFile.filePointer
                                        .toInt()
                                    if (press_pause) {
                                        pause_point = randomFile.filePointer
                                        complete_play = false
                                        press_pause = false
                                        break
                                    }
                                } catch (e: IOException) {
                                    e.printStackTrace()
                                }
                            }
                        } else {  // if click pause button and then resume play,
                            resume = false
                            try { randomFile!!.seek(pause_point as Int.toLong()) }
                            catch (e: IOException) { e.printStackTrace() }
                            var j = 0
                            while (j != -1) { // run until file ends
                                try {
                                    j = randomFile!!.read(data)
                                    audioTrack!!.write(data, 0, j)
                                    holder.seekBar.progress = randomFile.filePointer
                                        .toInt()
                                    if (press_pause) {
                                        pause_point = randomFile.filePointer
                                        complete_play = false
                                        press_pause = false
                                        break
                                    }
                                } catch (e: IOException) {
                                    e.printStackTrace()
                                }
                            }
                        }
                        audioTrack!!.stop()
                        audioTrack!!.release()
                        audioTrack = null
                        try {
                            randomFile!!.close()
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                        if (complete_play) {  // if complete to play audio file until end point,
                            holder.playButton.setImageResource(R.drawable.png_play)
                            btn_type = "play_button"
                        }
                    }
                    playThread!!.start()
                }
                "pause_button" -> {
                    btn_type = "play_button"
                    press_pause = true
                    resume = true
                    holder.playButton.setImageResource(R.drawable.png_play)
                }
            }
        }
    }

    override fun getItemCount() = fileList.size
}