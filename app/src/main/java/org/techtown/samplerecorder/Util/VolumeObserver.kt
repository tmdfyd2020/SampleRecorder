package org.techtown.samplerecorder.util

import android.annotation.SuppressLint
import android.content.Context
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Handler
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import org.techtown.samplerecorder.R
import org.techtown.samplerecorder.activity.MainActivity.Companion.seekbarView
import org.techtown.samplerecorder.home.HomeFragment
import org.techtown.samplerecorder.home.HomeFragment.Companion.volumeType

class VolumeObserver(private val context: Context, handler: Handler?) : ContentObserver(handler) {

    private var audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    @SuppressLint("SetTextI18n", "InflateParams")
    override fun onChange(selfChange: Boolean) {
        val volume = audioManager.getStreamVolume(volumeType)
        LogUtil.i(TAG, "volume : $volume")
        HomeFragment.instance().changeTextUi(context.getString(R.string.volume), "$volume")
        val seekBar = seekbarView.findViewById<View>(R.id.seekbar_volume) as SeekBar
        val textView = seekbarView.findViewById<View>(R.id.text_seekbar) as TextView
        seekBar.progress = volume
        textView.text = volume.toString()
        super.onChange(selfChange)
    }

    companion object {
        private const val TAG = "VolumeObserver"
    }
}