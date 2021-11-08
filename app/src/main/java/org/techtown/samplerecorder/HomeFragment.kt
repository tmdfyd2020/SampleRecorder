package org.techtown.samplerecorder

import android.annotation.SuppressLint
import android.content.Context.AUDIO_SERVICE
import android.content.Context.MODE_PRIVATE
import android.media.AudioManager
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.fragment.app.Fragment
import org.techtown.samplerecorder.Audio.Queue
import org.techtown.samplerecorder.Audio.RecordService
import org.techtown.samplerecorder.Audio.TrackService
import org.techtown.samplerecorder.Util.DialogService
import org.techtown.samplerecorder.databinding.FragmentHomeBinding

class HomeFragment : Fragment(), View.OnClickListener {

    private val context by lazy { activity }
    private lateinit var binding: FragmentHomeBinding
    private val audioRecord by lazy { RecordService(requireContext()) }
    private val audioTrack by lazy { TrackService() }
    private val switchButton by lazy { binding.switchButton }
    private val sharedPreferences by lazy { context?.getSharedPreferences(DATABASE, MODE_PRIVATE) }
    private val editor by lazy { sharedPreferences?.edit() }
    private var queue: Queue? = null
    private val dialogService by lazy { DialogService(requireContext()) }

    private var startTime: Long = 0
    private var fileDrop = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        setOnClickListener()
        return binding.root
    }

    override fun onStart() {
        initUi()
        initState()
//        setOnClickListener()
        super.onStart()
    }

    private fun initState() {
        // Switch Button 초기화
        val sharedPreferences = requireContext().getSharedPreferences(DATABASE, MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        fileDrop = sharedPreferences.getBoolean(FILE_DROP, false)
        if (fileDrop) switchButton.selectedTab = 0
        else switchButton.selectedTab = 1
        switchButton.setOnSwitchListener { position, _ ->
            fileDrop = position == 0
            with (editor) {
                putBoolean(FILE_DROP, fileDrop)
                apply()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun initUi() {
        val audioManager = context?.getSystemService(AUDIO_SERVICE) as AudioManager
        val nCurrentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        with (binding) {
            btnRecordSource.text = "${getString(R.string.source)}\n${getString(R.string.mic)}"
            btnPlayType.text = "${getString(R.string.type)}\n${getString(R.string.media)}"
            btnRecordChannel.text = "${getString(R.string.channel)}\n${getString(R.string.mono)}"
            btnPlayChannel.text = "${getString(R.string.channel)}\n${getString(R.string.mono)}"
            btnRecordSampleRate.text = "${getString(R.string.rate)}\n${getString(R.string.rate_16000)}"
            btnPlaySampleRate.text = "${getString(R.string.rate)}\n${getString(R.string.rate_16000)}"
            btnRecordBufferSize.text = "${getString(R.string.buffer_size)}\n${getString(R.string.buffer_size_1024)}"
            btnPlay.isEnabled = false
            btnPlayVolume.text = "${getString(R.string.volume)}\n${nCurrentVolume}"
        }
    }

    private fun setOnClickListener() {
        val btnSequence = binding.layoutBtnRecordSettings.child
        btnSequence.forEach { btn ->
            btn.setOnClickListener(this)
        }
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.btn_record -> record()
            R.id.btn_play -> play()
            R.id.btn_record_source -> dialogService.create(getString(R.string.source))
            R.id.btn_record_channel -> dialogService.create(getString(R.string.channel), getString(R.string.record))
            R.id.btn_record_sampleRate -> dialogService.create(getString(R.string.rate), getString(R.string.record))
            R.id.btn_record_bufferSize -> dialogService.create(getString(R.string.buffer_size))
            R.id.btn_play_type -> dialogService.create(getString(R.string.type))
            R.id.btn_play_channel -> dialogService.create(getString(R.string.channel), getString(R.string.play))
            R.id.btn_play_sampleRate -> dialogService.create(getString(R.string.rate), getString(R.string.play))
            R.id.btn_play_volume -> dialogService.create(getString(R.string.volume))
        }
    }

    companion object {
        private const val TAG = "HomeFragment"
        private const val DATABASE = "database"
        private const val FILE_DROP = "fileDrop"
    }
}