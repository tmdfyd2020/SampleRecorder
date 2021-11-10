package org.techtown.samplerecorder.list

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.text.Html.fromHtml
import android.view.Gravity
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.techtown.samplerecorder.audio.TrackService
import org.techtown.samplerecorder.database.RoomItem
import org.techtown.samplerecorder.list.ListFragment.Companion.BUTTON_PAUSE
import org.techtown.samplerecorder.list.ListFragment.Companion.BUTTON_PLAY
import org.techtown.samplerecorder.util.LogUtil
import org.techtown.samplerecorder.activity.MainActivity.Companion.filePath
import org.techtown.samplerecorder.activity.MainActivity.Companion.itemDAO
import org.techtown.samplerecorder.activity.MainActivity.Companion.itemList
import org.techtown.samplerecorder.R
import org.techtown.samplerecorder.databinding.ItemListBinding
import java.io.File
import java.io.RandomAccessFile

class ListViewHolder(private val binding: ItemListBinding, private val adapter: ListAdapter) : RecyclerView.ViewHolder(binding.root) {

    private val playButton by lazy { binding.btnItemPlay }
    private val seekBar    by lazy { binding.seekbarItem }
    val layout             by lazy { binding.layoutItemBottom }

    private lateinit var currentItem: RoomItem
    private lateinit var currentFile: File
    private var currentPosition: Int? = -1
    private val audioTrack by lazy { TrackService() }
    private var randomFile: RandomAccessFile? = null

    fun setData(item: RoomItem, position: Int) {
        currentItem = item
        currentFile = File(filePath, item.fileName)
        with (binding) {
            tvItemTitle.text = item.title
            tvItemTime.text = item.time
            tvItemChannel.text = item.channel
            tvItemRate.text = item.rate.toString()
        }
        currentPosition = position
    }

    private val seekBarListener = object: SeekBar.OnSeekBarChangeListener {
        var seekPoint = 0
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            if (fromUser) seekPoint = progress
        }
        override fun onStartTrackingTouch(seekBar: SeekBar) {
            FLAG_PAUSE_STATE = true
            FLAG_CAN_PLAY = true
            playButton.setImageDrawable(BUTTON_PLAY)
        }
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
                layout.visibility = View.VISIBLE
                with (seekBar) {
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

    @Suppress("DEPRECATION")
    @SuppressLint("UseCompatLoadingForDrawables")
    private fun removeListDialog(context: Context) {
        val builder = AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar)
        builder.setTitle(context.getString(R.string.delete))
            .setMessage(context.getString(R.string.delete_message))
            .setIcon(context.getDrawable(R.drawable.ic_list_dialog_delete))
            .setPositiveButton(fromHtml("<font color='#3399FF'>${context.getString(R.string.yes)}</font>")) { _, _ ->
                deleteItem()
                Toast.makeText(context, context.getString(R.string.toast_delete), Toast.LENGTH_SHORT).apply {
                    setGravity(Gravity.TOP, 0, context.resources.getInteger(R.integer.toast_height))
                    show()
                }
            }
            .setNegativeButton(fromHtml("<font color='#F06292'>${context.getString(R.string.no)}</font>")) { dialog, _ -> dialog.cancel() }
        val dialog = builder.create()
        dialog.window!!.setGravity(Gravity.CENTER)
        dialog.show()
    }

    private fun deleteItem() {
        currentFile.delete()
        itemList.removeAt(currentPosition!!)
        CoroutineScope(Dispatchers.IO).launch {
            itemDAO.delete(currentItem)
        }
        adapter.notifyItemRemoved(currentPosition!!)
        adapter.notifyItemRangeChanged(currentPosition!!, itemList.size)
    }

    companion object {
        private const val TAG = "ItemListViewHolder"
        var FLAG_PAUSE_STATE = false     // 멈춘 지점부터 시작할 지, 처음부터 시작할 지
        var FLAG_CAN_PLAY = true         // 재생 버튼 클릭 가능 | 중지 버튼 클릭 가능
        var PREVIOUS_FILE_POSITION = -1  // 다른 아이템 클릭 시 layout 숨기기 위한 장치(이전에 클릭한 아이템 position)
    }
}