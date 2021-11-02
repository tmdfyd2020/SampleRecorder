package org.techtown.samplerecorder.List

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.text.Html
import android.view.Gravity
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import org.techtown.samplerecorder.Audio.TrackService
import org.techtown.samplerecorder.List.ItemListActivity.Companion.BUTTON_PAUSE
import org.techtown.samplerecorder.List.ItemListActivity.Companion.BUTTON_PLAY
import org.techtown.samplerecorder.LogUtil
import org.techtown.samplerecorder.R
import org.techtown.samplerecorder.databinding.ItemBinding
import java.io.File
import java.io.RandomAccessFile

class ItemListViewHolder(binding: ItemBinding, private val adapter: ItemListAdapter) : RecyclerView.ViewHolder(binding.root) {

    private val TAG = this.javaClass.simpleName

    private val playButton = binding.btnItemPlay
    private val fileName = binding.itemText
    val seekBar = binding.seekbarPlayState
    private val audioTrack by lazy { TrackService() }

    private var randomFile: RandomAccessFile? = null
    private lateinit var currentFile: File
    private var currentPosition: Int? = -1
    private lateinit var fileList: MutableList<File>

    fun setData(fileList: MutableList<File>, file: File, position: Int) {
        currentFile = file
        currentPosition = position
        this.fileList = fileList
        fileName.text = file.name
    }

    private val seekBarListener = object: SeekBar.OnSeekBarChangeListener {
        var seekPoint = 0
        // 시크바 드래그 시
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            if (fromUser) seekPoint = progress
        }
        // 시크바 thumb 터치 시
        override fun onStartTrackingTouch(seekBar: SeekBar) {
            FLAG_PAUSE_STATE = true
            FLAG_CAN_PLAY = true
            playButton.setImageDrawable(BUTTON_PLAY)
        }
        // 시크바 터치 해제 시
        override fun onStopTrackingTouch(seekBar: SeekBar) {
            FLAG_CAN_PLAY = false
            playButton.setImageDrawable(BUTTON_PAUSE)
            randomFile = RandomAccessFile(currentFile, "r")
            audioTrack.create()
            audioTrack.play(randomFile!!, seekBar, playButton, 0)  // TODO seekPoint 시 일정 확률로 노이즈
        }
    }

    init {
        seekBar.setOnSeekBarChangeListener(seekBarListener)

        playButton.setOnClickListener {
            when (FLAG_CAN_PLAY) {
                true -> playList()
                false -> pauseList()
            }
        }

        binding.root.setOnLongClickListener { v ->
            removeListDialog(v.context)
            false
        }
    }

    private fun playList() {
        randomFile = RandomAccessFile(currentFile, "rw")
        setState()
        if (PREVIOUS_FILE_POSITION != currentPosition) {
            adapter.notifyItemChanged(PREVIOUS_FILE_POSITION, "click")
            audioTrack.pausePoint = 0
            LogUtil.i(TAG, "Previous position : $PREVIOUS_FILE_POSITION")
            LogUtil.i(TAG, "Current position : $currentPosition")
        }
        PREVIOUS_FILE_POSITION = currentPosition!!

        audioTrack.create()
        audioTrack.play(randomFile!!, seekBar, playButton)
    }

    private fun pauseList() {
        setState()
    }

    private fun setState() {
        when (FLAG_CAN_PLAY) {
            true -> {  // 재생 버튼 클릭 시
                FLAG_CAN_PLAY = false
                playButton.setImageDrawable(BUTTON_PAUSE)
                with (seekBar) {
                    visibility = View.VISIBLE
                    max = randomFile!!.length().toInt()
                    min = 0
                }
            }
            false -> {  // 중지 버튼 클릭 시
                FLAG_PAUSE_STATE = true
                FLAG_CAN_PLAY = true
                playButton.setImageDrawable(BUTTON_PLAY)
            }
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun removeListDialog(context: Context) {
        val builder = AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar)
        builder.setTitle(context.getString(R.string.delete))
            .setMessage(context.getString(R.string.delete_message))
            .setIcon(context.getDrawable(R.drawable.png_delete))
            .setPositiveButton(Html.fromHtml("<font color='#3399FF'>${context.getString(R.string.yes)}</font>")) { _, _ ->
                currentFile.delete()
                fileList.removeAt(currentPosition!!)
                adapter.notifyItemRemoved(currentPosition!!)
                adapter.notifyItemRangeChanged(currentPosition!!, fileList.size)
                Toast.makeText(context, context.getString(R.string.toast_delete), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(
                Html.fromHtml("<font color='#F06292'>${context.getString(R.string.no)}</font>")
            ) { dialog, _ -> dialog.cancel() }
        val dialog = builder.create()
        dialog.window!!.setGravity(Gravity.CENTER)
        dialog.show()
    }

    companion object {
        var FLAG_PAUSE_STATE = false  // 멈춘 지점부터 시작할 지, 처음부터 시작할 지
        var FLAG_CAN_PLAY = true      // 재생 버튼 클릭 가능 | 중지 버튼 클릭 가능
        var PREVIOUS_FILE_POSITION = -1
    }
}