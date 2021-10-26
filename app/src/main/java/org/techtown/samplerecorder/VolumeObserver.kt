package org.techtown.samplerecorder

import android.annotation.SuppressLint
import android.content.Context
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Handler
import org.techtown.samplerecorder.MainActivity.Companion.volumeType

class VolumeObserver(private val context: Context, handler: Handler?) : ContentObserver(handler) {

    private var audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val mainUi = context as MainActivity

    @SuppressLint("SetTextI18n")
    override fun onChange(selfChange: Boolean) {
        super.onChange(selfChange)
        mainUi.changeTextUi(context.getString(R.string.volume), "${audioManager.getStreamVolume(volumeType)}")
    }
}