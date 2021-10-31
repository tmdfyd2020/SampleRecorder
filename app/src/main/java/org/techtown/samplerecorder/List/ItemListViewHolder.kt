package org.techtown.samplerecorder.List

import android.content.Context
import android.view.View
import android.widget.SeekBar
import androidx.recyclerview.widget.RecyclerView
import org.techtown.samplerecorder.Audio.TrackService
import org.techtown.samplerecorder.List.ItemListActivity.Companion.BUTTON_PAUSE
import org.techtown.samplerecorder.List.ItemListActivity.Companion.BUTTON_PLAY
import org.techtown.samplerecorder.LogUtil
import org.techtown.samplerecorder.databinding.ItemBinding
import java.io.File
import java.io.RandomAccessFile

class ItemListViewHolder (private val binding: ItemBinding, val context: Context, val adapter: ItemListAdapter) : RecyclerView.ViewHolder(binding.root) {

    private val TAG = this.javaClass.simpleName

    private val playButton = binding.btnItemPlay
    private val fileName = binding.itemText
    val seekBar = binding.seekbarPlayState
    private val audioTrack by lazy { TrackService() }


    private var randomFile: RandomAccessFile? = null
    private lateinit var currentFile: File
    private var currentPosition: Int? = -1

    fun setData(fileList: MutableList<File>, file: File, position: Int) {
        currentFile = file
        currentPosition = position
        currentPosition = position
        fileName.text = file.name
    }

    private val seekBarListener = object: SeekBar.OnSeekBarChangeListener {
        var seekPoint = 0
        // 시크바 드래그 시
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            seekPoint = progress
        }
        // 시크바 thumb 터치 시
        override fun onStartTrackingTouch(seekBar: SeekBar) {
            LogUtil.d(TAG, "시크바 터치 발생")
            FLAG_PAUSE_STATE = true
        }
        // 시크바 터치 해제 시
        override fun onStopTrackingTouch(seekBar: SeekBar) {
            LogUtil.d(TAG, "실행 포인트 : ${seekPoint.toLong()}")
            FLAG_CAN_PLAY = false
            playButton.setImageDrawable(BUTTON_PAUSE)
            randomFile = RandomAccessFile(currentFile, "rw")
            audioTrack.create()
            audioTrack.pausePoint = seekPoint.toLong()  // TODO 0으로 설정 시 소음없이 진행
            audioTrack.play(randomFile!!, seekBar, playButton)
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


//    init {
//        // if click itemView on LongClick, show delete dialog
//        itemView.setOnLongClickListener { v ->
//            val file = fileList[adapterPosition]
//            val builder = AlertDialog.Builder(
//                v.context,
//                R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar
//            )
//            builder.setTitle("Delete")
//                .setMessage("Are you sure you want to delete?")
//                .setIcon(listContext.getDrawable(R.drawable.png_delete))
//                .setPositiveButton(
//                    Html.fromHtml("<font color='#3399FF'>Yes</font>")
//                ) { dialog, which ->
//                    file.delete()
//                    fileList.removeAt(adapterPosition)
//                    notifyItemRemoved(adapterPosition)
//                    notifyItemRangeChanged(adapterPosition, fileList.size)
//                    Toast.makeText(listContext, "파일이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
//                }
//                .setNegativeButton(
//                    Html.fromHtml("<font color='#F06292'>No</font>")
//                ) { dialog, which -> dialog.cancel() }
//            val dialog = builder.create()
//            dialog.window!!.setGravity(Gravity.CENTER)
//            dialog.show()
//            false
//        }
//    }

    companion object {
        var FLAG_PAUSE_STATE = false  // 멈춘 지점부터 시작할 지, 처음부터 시작할 지
        var FLAG_CAN_PLAY = true      // 재생 버튼 클릭 가능 | 중지 버튼 클릭 가능
        var PREVIOUS_FILE_POSITION = -1
        var FLAG_SEEKBAR_PAUSE = false
    }
}