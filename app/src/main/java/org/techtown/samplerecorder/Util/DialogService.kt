package org.techtown.samplerecorder.Util

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.AUDIO_SERVICE
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.MediaRecorder
import android.text.Html.fromHtml
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import org.techtown.samplerecorder.HomeFragment
import org.techtown.samplerecorder.HomeFragment.Companion.bufferSize
import org.techtown.samplerecorder.HomeFragment.Companion.playChannel
import org.techtown.samplerecorder.HomeFragment.Companion.playRate
import org.techtown.samplerecorder.HomeFragment.Companion.recordChannel
import org.techtown.samplerecorder.HomeFragment.Companion.recordRate
import org.techtown.samplerecorder.HomeFragment.Companion.source
import org.techtown.samplerecorder.HomeFragment.Companion.type
import org.techtown.samplerecorder.HomeFragment.Companion.volumeType
import org.techtown.samplerecorder.MainActivity
import org.techtown.samplerecorder.R


class DialogService private constructor(private val context: Context) {

    private val mainActivity = context as MainActivity

    fun create(setting: String, mode: String = "") {
        when (setting) {
            context.getString(R.string.volume) -> {
                volumeDialog()
            }
        }
    }

    private fun volumeDialog() {
        val viewSeekbar = mainActivity.layoutInflater.inflate(R.layout.seekbar_volume, null)
        val seekBar = viewSeekbar.findViewById<View>(R.id.seekbar_volume) as SeekBar
        val textView = viewSeekbar.findViewById<View>(R.id.text_seekbar) as TextView
        val imageView = viewSeekbar.findViewById<View>(R.id.img_seekbar) as ImageView

        val builder = dialogBuilder()
        builder.setTitle(context.getString(R.string.volume)).setView(viewSeekbar)
        seekbarSetting(seekBar, textView, imageView)
        showDialog(builder)
    }

    private fun seekbarSetting(seekBar: SeekBar, volumeText: TextView, image: ImageView) {
        val audioManager = mainActivity.getSystemService(AUDIO_SERVICE) as AudioManager
        val currentVolume = audioManager.getStreamVolume(volumeType)
        seekBar.min = 1
        seekBar.max = audioManager.getStreamMaxVolume(volumeType)
        seekBar.progress = currentVolume
        seekBarUi(currentVolume, volumeText, image)
        seekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                audioManager.setStreamVolume(volumeType, progress, 0)
                seekBarUi(progress, volumeText, image)
            }
            override fun onStopTrackingTouch(seekBar: SeekBar) { }
            override fun onStartTrackingTouch(seekBar: SeekBar) { }
        })
    }

    private fun seekBarUi(volume: Int, volumeText: TextView, image: ImageView) {
        when {
            volume >= 13 -> {
                volumeText.setTextColor(mainActivity.resources.getColor(R.color.red_record))
                image.setImageResource(R.drawable.ic_volume_loud)
            }
            volume in 10..12 -> {
                volumeText.setTextColor(mainActivity.resources.getColor(R.color.blue_play))
                image.setImageResource(R.drawable.ic_volume_loud)
            }
            else -> {
                volumeText.setTextColor(mainActivity.resources.getColor(R.color.blue_play))
                image.setImageResource(R.drawable.ic_volume_small)
            }
        }
        volumeText.text = volume.toString()
    }

    private fun dialogBuilder(): AlertDialog.Builder {
        return AlertDialog.Builder(
            context,
            android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar
        )
    }

    private fun showDialog(builder: AlertDialog.Builder) {
        val dialog = builder.create()
        dialog.window!!.setGravity(Gravity.CENTER)
        dialog.show()
    }

    companion object {
        private const val TAG = "DialogService"
        fun dialog(context: Context) = DialogService(context)
    }
}