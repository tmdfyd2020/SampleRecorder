package org.techtown.samplerecorder

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import org.techtown.samplerecorder.Main.AudioRecord
import org.techtown.samplerecorder.Main.AudioTrack
import org.techtown.samplerecorder.databinding.ActivityMainBinding

class MainActivity3 : AppCompatActivity() {

    private val TAG = this.javaClass.simpleName
    private lateinit var binding: ActivityMainBinding

    private lateinit var mAudioRecord: AudioRecord
    private lateinit var mAudioTrack: AudioTrack
    private val waveform by lazy { binding.viewWaveForm }
    private val switchButton by lazy { binding.switchButton }
    private val sharedPreferences by lazy { getSharedPreferences(FILE_DROP, MODE_PRIVATE) }
    private val editor by lazy { sharedPreferences.edit() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

    }

    companion object {
        private const val MESSAGE_RECORD = 1
        private const val MESSAGE_PLAY = 2
        private const val FILE_DROP = "fileDrop"
    }

}