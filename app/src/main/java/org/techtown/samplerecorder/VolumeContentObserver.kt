package org.techtown.samplerecorder

import android.annotation.SuppressLint
import android.content.Context
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Handler
import org.techtown.samplerecorder.Main.MainActivity
import org.techtown.samplerecorder.Main.MainActivity.volumeType

class VolumeContentObserver(private val context: Context, handler: Handler?) : ContentObserver(handler) {

    private var audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val mainUi = context as MainActivity

    @SuppressLint("SetTextI18n")
    override fun onChange(selfChange: Boolean) {
        super.onChange(selfChange)
        mainUi.btnVolume.text =
            "${context.getString(R.string.volume)}\n${audioManager.getStreamVolume(volumeType)}"
    }
}