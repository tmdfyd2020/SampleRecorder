package org.techtown.samplerecorder.util

import android.annotation.SuppressLint
import android.content.Context
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Handler
import org.techtown.samplerecorder.home.HomeFragment
import org.techtown.samplerecorder.home.HomeFragment.Companion.volumeType
import org.techtown.samplerecorder.R

class VolumeObserver(private val context: Context, handler: Handler?) : ContentObserver(handler) {

    private var audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    @SuppressLint("SetTextI18n")
    override fun onChange(selfChange: Boolean) {
        LogUtil.i(TAG, "volume : ${audioManager.getStreamVolume(volumeType)}")
        HomeFragment.instance().changeTextUi(context.getString(R.string.volume), "${audioManager.getStreamVolume(volumeType)}")
        super.onChange(selfChange)
    }

    companion object {
        private const val TAG = "VolumeObserver"
    }
}